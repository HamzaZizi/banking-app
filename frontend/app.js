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

// "2026-07-16" -> "16 Jul 2026"
const formatDate = (iso) => {
    const d = new Date(iso);
    if (isNaN(d)) return iso;
    return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
};

// Two initials for an account/description, used in the little round icons.
const initials = (text) => {
    const words = String(text).trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) return "?";
    if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
    return (words[0][0] + words[1][0]).toUpperCase();
};

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
        <div class="account-row" data-account-id="${a.id}" role="button" tabindex="0">
            <div class="account-icon">${initials(a.type || a.nickname)}</div>
            <div class="account-main">
                <div class="name">${a.nickname}</div>
                <div class="meta">${a.product || a.type} &middot; ${a.sortCode} &middot; ${a.accountNumberMasked}</div>
            </div>
            <div class="account-amounts">
                <div class="balance">${gbp(a.balance)}</div>
                <div class="available">${gbp(a.availableBalance)} available</div>
            </div>
            <div class="chevron">&rsaquo;</div>
        </div>`
        )
        .join("");

    container.querySelectorAll(".account-row").forEach((row) => {
        const id = row.getAttribute("data-account-id");
        const account = accounts.find((a) => a.id === id);
        row.addEventListener("click", () => openAccount(account));
        row.addEventListener("keydown", (e) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                openAccount(account);
            }
        });
    });
}

function renderMortgages(mortgages) {
    const container = document.getElementById("mortgages-list");
    container.innerHTML = mortgages
        .map(
            (m) => `
        <div class="mortgage-card">
            <div class="name">${m.product}</div>
            <div class="meta">Rate ends ${formatDate(m.rateEndDate)}</div>
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
            <div class="row"><span class="label">Next payment</span><span class="value">${formatDate(m.nextPaymentDate)}</span></div>
            <div class="row"><span class="label">Term remaining</span><span class="value">${m.termRemainingMonths} months</span></div>
        </div>`
        )
        .join("");
}

function renderTransactions(transactions) {
    const container = document.getElementById("drawer-transactions");
    if (!transactions || transactions.length === 0) {
        container.innerHTML = '<div class="empty">No recent transactions.</div>';
        return;
    }
    container.innerHTML = transactions
        .map((t) => {
            const isCredit = t.type === "CREDIT";
            const sign = isCredit ? "+" : "";
            return `
        <div class="txn-row">
            <div class="txn-icon">${initials(t.description)}</div>
            <div class="txn-main">
                <div class="txn-desc">${t.description}</div>
                <div class="txn-meta">${formatDate(t.date)}<span class="txn-cat">${t.category}</span></div>
            </div>
            <div class="txn-amount ${isCredit ? "credit" : "debit"}">
                ${sign}${gbp(Math.abs(t.amount))}
                <div class="txn-after">Balance ${gbp(t.balanceAfter)}</div>
            </div>
        </div>`;
        })
        .join("");
}

function renderAccountDetail(account) {
    document.getElementById("drawer-title").textContent = account.nickname;
    document.getElementById("drawer-sub").textContent =
        `${account.product || account.type} · ${account.sortCode} · ${account.accountNumberMasked}`;

    const tiles = [
        { label: "Balance", value: gbp(account.balance) },
        { label: "Available", value: gbp(account.availableBalance) },
    ];
    if (account.overdraftLimit && Number(account.overdraftLimit) > 0) {
        tiles.push({ label: "Arranged overdraft", value: gbp(account.overdraftLimit) });
    }
    tiles.push({ label: "Currency", value: account.currency });

    document.getElementById("drawer-balances").innerHTML = tiles
        .map((t) => `<div class="balance-tile"><div class="label">${t.label}</div><div class="value">${t.value}</div></div>`)
        .join("");
}

async function openAccount(account) {
    renderAccountDetail(account);
    const container = document.getElementById("drawer-transactions");
    container.innerHTML = '<div class="empty">Loading transactions…</div>';
    openDrawer();
    try {
        const transactions = await fetchJson(`/api/accounts/${account.id}/transactions`);
        renderTransactions(transactions);
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="error">Could not load transactions. Please try again.</div>';
    }
}

function openDrawer() {
    const drawer = document.getElementById("account-drawer");
    drawer.classList.add("open");
    drawer.setAttribute("aria-hidden", "false");
}

function closeDrawer() {
    const drawer = document.getElementById("account-drawer");
    drawer.classList.remove("open");
    drawer.setAttribute("aria-hidden", "true");
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

function setupDrawer() {
    document.querySelectorAll("[data-close-drawer]").forEach((el) => {
        el.addEventListener("click", closeDrawer);
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") closeDrawer();
    });
}

async function init() {
    setupTabs();
    setupDrawer();
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
            '<p class="error">Could not reach the C&amp;I Banking API. Check the backend is running.</p>';
    }
}

document.addEventListener("DOMContentLoaded", init);

// Export for unit tests when running under Node/Jest. No effect in the browser
// (there is no `module` global there), so runtime behaviour is unchanged.
if (typeof module !== "undefined" && module.exports) {
    module.exports = { gbp, formatDate, initials, fetchJson, renderSummary, renderAccounts, renderMortgages, renderTransactions };
}
