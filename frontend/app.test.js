/**
 * @jest-environment jsdom
 *
 * Unit tests for the landing-zone logic in app.js. app.js exports its functions
 * via a module.exports shim that only activates under Node/Jest.
 */

const {
    STAGE_ORDER,
    SPEECH,
    initials,
    speechFor,
    advancePipeline,
    setYear,
} = require("./app.js");

// Build the pipeline strip exactly as index.html ships it: build + scan done,
// deploy active, verify + promote pending.
function buildPipeline() {
    document.body.innerHTML = `
        <div id="pipeline">
            <div class="stage done" data-stage="build"></div>
            <div class="stage done" data-stage="scan"></div>
            <div class="stage active" data-stage="deploy"></div>
            <div class="stage" data-stage="verify"></div>
            <div class="stage" data-stage="promote"></div>
        </div>`;
    return document.querySelectorAll(".stage");
}

describe("STAGE_ORDER", () => {
    test("is the five-stage Harness pipeline in order", () => {
        expect(STAGE_ORDER).toEqual(["build", "scan", "deploy", "verify", "promote"]);
    });
});

describe("speechFor()", () => {
    test("returns the line for a known stage", () => {
        expect(speechFor("deploy")).toBe(SPEECH.deploy);
        expect(speechFor("promote")).toBe(SPEECH.promote);
    });

    test("falls back to the idle line for an unknown key", () => {
        expect(speechFor("nope")).toBe(SPEECH.idle);
    });
});

describe("advancePipeline()", () => {
    test("moves the active marker to the next stage and marks the old one done", () => {
        const stages = buildPipeline();

        const next = advancePipeline(stages);

        expect(next).toBe("verify");
        const deploy = document.querySelector('[data-stage="deploy"]');
        const verify = document.querySelector('[data-stage="verify"]');
        expect(deploy.classList.contains("active")).toBe(false);
        expect(deploy.classList.contains("done")).toBe(true);
        expect(verify.classList.contains("active")).toBe(true);
    });

    test("returns 'done' once the final stage is completed", () => {
        const stages = buildPipeline();

        expect(advancePipeline(stages)).toBe("verify");
        expect(advancePipeline(stages)).toBe("promote");
        expect(advancePipeline(stages)).toBe("done");

        const promote = document.querySelector('[data-stage="promote"]');
        expect(promote.classList.contains("done")).toBe(true);
    });

    test("starts at the first pending stage when nothing is active", () => {
        document.body.innerHTML = `
            <div id="pipeline">
                <div class="stage done" data-stage="build"></div>
                <div class="stage" data-stage="scan"></div>
                <div class="stage" data-stage="deploy"></div>
            </div>`;
        const stages = document.querySelectorAll(".stage");

        expect(advancePipeline(stages)).toBe("scan");
        expect(document.querySelector('[data-stage="scan"]').classList.contains("active")).toBe(true);
    });
});

describe("setYear()", () => {
    test("writes the four-digit year into the element", () => {
        document.body.innerHTML = `<span id="year"></span>`;
        setYear(document.getElementById("year"), new Date("2026-08-27T00:00:00Z"));
        expect(document.getElementById("year").textContent).toBe("2026");
    });

    test("is a no-op when the element is missing", () => {
        expect(() => setYear(null)).not.toThrow();
    });
});

describe("initials()", () => {
    test("takes first letters of the first two words", () => {
        expect(initials("Captain Canary")).toBe("CC");
    });

    test("uses first two letters for a single word", () => {
        expect(initials("HARNESS")).toBe("HA");
    });
});
