package com.harness.demo.cibanking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Self warm-up on boot — removes cold-start artifacts before the pod reports
 * ready.
 *
 * WHY THIS EXISTS
 * A freshly-started JVM is slow for its first minute or two (class loading, JIT
 * compilation of the hot paths, cold Jackson/Spring-MVC caches). If a new pod
 * starts serving the moment it reports READY, that cold opening period shows an
 * elevated "Average Response Time" that has nothing to do with the build's
 * steady-state performance — it skews dashboards and any latency-based release
 * gate against an otherwise healthy build.
 *
 * HOW IT FIXES IT
 * As an ApplicationRunner this executes AFTER the embedded web server is
 * listening but BEFORE ApplicationReadyEvent fires. Spring Boot only flips the
 * readiness probe (/actuator/health/readiness) to UP once this returns, so the
 * pod stays NotReady — and Kubernetes keeps it out of the Service endpoints —
 * until the hot endpoints are warm. By the time it takes live traffic the pod
 * is already warm, so there is no opening spike. (Liveness is unaffected: it
 * goes CORRECT at ApplicationStartedEvent, before runners, so the pod is never
 * restarted while warming.)
 *
 * All calls hit localhost directly (not the k8s Service), so readiness state
 * does not gate them, and every error is swallowed — warm-up must NEVER fail
 * startup.
 */
@Component
public class WarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WarmupRunner.class);

    // Real seed identifiers from BankingService — cycled so per-id caches warm too.
    private static final String[] ACCOUNTS = {"acc-001", "acc-002", "acc-003"};
    private static final String[] MORTGAGES = {"mtg-001", "mtg-002"};

    @Value("${server.port:8080}")
    private int port;

    @Value("${cibanking.warmup.enabled:true}")
    private boolean enabled;

    /** Number of browse rounds. Each round = ~6 GETs across the read endpoints. */
    @Value("${cibanking.warmup.rounds:200}")
    private int rounds;

    /** Hard time cap so a slow/unresponsive boot can never hang readiness. */
    @Value("${cibanking.warmup.max-seconds:60}")
    private int maxSeconds;

    /** Hit /api/summary only once every N rounds (it is the heaviest read). */
    @Value("${cibanking.warmup.summary-every:4}")
    private int summaryEvery;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Warm-up disabled (cibanking.warmup.enabled=false) — skipping.");
            return;
        }

        String base = "http://localhost:" + port;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        long deadline = System.currentTimeMillis() + (maxSeconds * 1000L);
        long start = System.currentTimeMillis();
        int requests = 0;
        int errors = 0;

        log.info("Warm-up starting: up to {} rounds / {}s against {} (summary every {} rounds). "
                + "Readiness stays DOWN until this completes.", rounds, maxSeconds, base, summaryEvery);

        for (int i = 0; i < rounds; i++) {
            if (System.currentTimeMillis() > deadline) {
                log.info("Warm-up time cap ({}s) reached at round {} — stopping early.", maxSeconds, i);
                break;
            }
            String acc = ACCOUNTS[i % ACCOUNTS.length];
            String mtg = MORTGAGES[i % MORTGAGES.length];

            errors += hit(client, base + "/api/accounts");
            errors += hit(client, base + "/api/accounts/" + acc);
            errors += hit(client, base + "/api/accounts/" + acc + "/balance");
            errors += hit(client, base + "/api/accounts/" + acc + "/transactions");
            errors += hit(client, base + "/api/mortgages");
            errors += hit(client, base + "/api/mortgages/" + mtg);
            requests += 6;

            // Summary is the heaviest read (aggregates every account + mortgage),
            // so warm it less often to keep total warm-up within the time cap.
            if (summaryEvery > 0 && i % summaryEvery == 0) {
                errors += hit(client, base + "/api/summary");
                requests += 1;
            }
        }

        long ms = System.currentTimeMillis() - start;
        log.info("Warm-up complete: {} requests in {} ms ({} errors, swallowed). Reporting READY.",
                requests, ms, errors);
    }

    /** Fire one GET, discard the body, swallow everything. Returns 1 if it failed. */
    private int hit(HttpClient client, String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
            return res.statusCode() >= 400 ? 1 : 0;
        } catch (Exception e) {
            // Never fail startup on a warm-up hiccup.
            return 1;
        }
    }
}
