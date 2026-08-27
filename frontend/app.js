// Harness demo landing zone — front-end behaviour for Captain Canary.
//
// This page has no backend. Its job is to look sharp while it sits deployed on
// the `landing` branch, then to put on a little show when a presenter clicks
// "Prime the canary": Captain Canary gets excited and the pipeline strip walks
// itself forward one stage at a time. During a live demo the *real* transform
// happens when a PR on an app branch is deployed over the top of this page.
//
// Everything interesting is factored into small pure-ish functions so the unit
// tests (app.test.js, run by jest under jsdom) can exercise them without a DOM
// event loop. A module.exports shim at the bottom activates only under Node.

// The ordered pipeline as it appears in the strip.
const STAGE_ORDER = ["build", "scan", "deploy", "verify", "promote"];

// What Captain Canary says at each moment of the show.
const SPEECH = {
    idle: "Ready to ship 🚀",
    build: "Build's green 📦",
    scan: "Security gate: clear 🛡️",
    deploy: "Rolling out the canary 🐦",
    verify: "Verifying health 🩺",
    promote: "Promoted! Full send ✅",
    done: "Shipped it 🎉",
};

// Presenter-facing hint under the button.
const HINT = {
    idle: "Give Captain Canary a nudge →",
    running: "Watch the pipeline advance…",
    done: "That's the demo loop — now raise a PR to see it for real.",
};

// Two-letter initials — kept from the old app because a couple of tests and the
// odd label still lean on it, and it's harmless.
const initials = (text) => {
    const words = String(text).trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) return "?";
    if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
    return (words[0][0] + words[1][0]).toUpperCase();
};

// The speech line for a given pipeline position. Unknown keys fall back to idle.
const speechFor = (key) => SPEECH[key] || SPEECH.idle;

// Pure state-machine step over the pipeline stage elements. Given the ordered
// NodeList/array of `.stage` divs, find the one marked `.active`, mark it
// `.done`, and activate the next one. Returns the data-stage of the stage that
// became active, or "done" when the pipeline is already complete.
function advancePipeline(stages) {
    const list = Array.from(stages);
    const activeIndex = list.findIndex((s) => s.classList.contains("active"));

    // Nothing active yet -> start at the first not-done stage.
    if (activeIndex === -1) {
        const next = list.find((s) => !s.classList.contains("done"));
        if (!next) return "done";
        next.classList.add("active");
        return next.getAttribute("data-stage");
    }

    const current = list[activeIndex];
    current.classList.remove("active");
    current.classList.add("done");

    const next = list[activeIndex + 1];
    if (!next) return "done";
    next.classList.add("active");
    return next.getAttribute("data-stage");
}

// Set the footer year.
function setYear(el, now = new Date()) {
    if (el) el.textContent = String(now.getFullYear());
}

// ----- Wiring (browser only) -----------------------------------------------
function runShow() {
    const pipeline = document.getElementById("pipeline");
    const speech = document.getElementById("canary-speech");
    const hint = document.getElementById("deploy-hint");
    const canary = document.querySelector(".canary");
    const btn = document.getElementById("deploy-btn");
    if (!pipeline) return;

    const stages = pipeline.querySelectorAll(".stage");
    if (btn) btn.disabled = true;
    if (canary) canary.classList.add("excited");
    if (hint) hint.textContent = HINT.running;

    // Walk forward one stage every beat until the pipeline is complete.
    const beat = 900;
    const tick = () => {
        const now = advancePipeline(stages);
        if (speech) speech.textContent = speechFor(now);
        if (now === "done") {
            if (hint) hint.textContent = HINT.done;
            if (canary) canary.classList.remove("excited");
            if (btn) btn.disabled = false;
            return;
        }
        setTimeout(tick, beat);
    };
    tick();
}

function init() {
    setYear(document.getElementById("year"));

    const btn = document.getElementById("deploy-btn");
    if (btn) btn.addEventListener("click", runShow);

    const speech = document.getElementById("canary-speech");
    if (speech) speech.textContent = speechFor("idle");
}

if (typeof document !== "undefined") {
    document.addEventListener("DOMContentLoaded", init);
}

// Export for unit tests when running under Node/Jest. No effect in the browser.
if (typeof module !== "undefined" && module.exports) {
    module.exports = {
        STAGE_ORDER,
        SPEECH,
        HINT,
        initials,
        speechFor,
        advancePipeline,
        setYear,
    };
}
