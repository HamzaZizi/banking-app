// If APP_CONFIG.apiBaseUrl is a string (including ""), use it verbatim.
// "" = same origin: the browser calls /api/... on whatever host served this
// page (the shared ALB), so there's no cross-origin request and no CORS.
// Only when there's no config.js at all (pure local static serving) do we
// fall back to the local backend on :8080.
const API_BASE = (window.APP_CONFIG && typeof window.APP_CONFIG.apiBaseUrl === "string")
    ? window.APP_CONFIG.apiBaseUrl
    : "http://localhost:8080";

const gbp = (value) =>
    new Intl.NumberFormat("en-GB", { style: "currency", currency: "GBP" }).format(value);

async function fetchJson(path) {
    const res = await fetch(`${API_BASE}${path}`);
    if (!res.ok) {
        throw new Error(`Request failed: ${path} (${res.status})`);
    }
    return res.json();
}

function renderSummary(summary) {
    document.getElementById("summary-total-balance").textContent = gbp(summary.totalBalance);
    document.getElementById("summary-account-count").textContent = summary.accountCount;
    document.getElementById("summary-mortgage-balance").textContent = gbp(summary.totalMortgageOutstanding);
}

function renderAccounts(accounts) {
    const container = document.getElementById("accounts-list");
    container.innerHTML = accounts
        .map(
            (a) => `
        <div class="account-card">
            <div class="name">${a.nickname}</div>
            <div class="meta">${a.type} &middot; Sort code ${a.sortCode} &middot; ${a.accountNumberMasked}</div>
            <div class="balance">${gbp(a.balance)}</div>
            <div class="available">Available: ${gbp(a.availableBalance)}</div>
        </div>`
        )
        .join("");
}

function renderMortgages(mortgages) {
    const container = document.getElementById("mortgages-list");
    container.innerHTML = mortgages
        .map(
            (m) => `
        <div class="mortgage-card">
            <div class="name">${m.product}</div>
            <div class="meta">Rate ends ${m.rateEndDate}</div>
            <div class="row"><span class="label">Outstanding balance</span><span class="value">${gbp(
                m.outstandingBalance
            )}</span></div>
            <div class="row"><span class="label">Original amount</span><span class="value">${gbp(
                m.originalAmount
            )}</span></div>
            <div class="row"><span class="label">Interest rate</span><span class="value">${m.interestRate}% (${m.rateType})</span></div>
            <div class="row"><span class="label">Monthly payment</span><span class="value">${gbp(
                m.monthlyPayment
            )}</span></div>
            <div class="row"><span class="label">Next payment</span><span class="value">${m.nextPaymentDate}</span></div>
            <div class="row"><span class="label">Term remaining</span><span class="value">${m.termRemainingMonths} months</span></div>
        </div>`
        )
        .join("");
}

function setupTabs() {
    const buttons = document.querySelectorAll(".tab-btn");
    buttons.forEach((btn) => {
        btn.addEventListener("click", () => {
            buttons.forEach((b) => b.classList.remove("active"));
            btn.classList.add("active");
            document.querySelectorAll(".panel").forEach((p) => p.classList.remove("active"));
            document.getElementById(`${btn.dataset.tab}-panel`).classList.add("active");
        });
    });
}

async function init() {
    setupTabs();
    try {
        const [summary, accounts, mortgages] = await Promise.all([
            fetchJson("/api/summary"),
            fetchJson("/api/accounts"),
            fetchJson("/api/mortgages"),
        ]);
        renderSummary(summary);
        renderAccounts(accounts);
        renderMortgages(mortgages);
    } catch (err) {
        console.error(err);
        document.getElementById("accounts-list").innerHTML =
            '<p style="color:#B00020;">Could not reach the C&amp;I Banking API. Check the backend is running.</p>';
    }
}

document.addEventListener("DOMContentLoaded", init);

// Export for unit tests when running under Node/Jest. No effect in the browser
// (there is no `module` global there), so runtime behaviour is unchanged.
if (typeof module !== "undefined" && module.exports) {
    module.exports = { gbp, fetchJson, renderSummary, renderAccounts, renderMortgages };
}
