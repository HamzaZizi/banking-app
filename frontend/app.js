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

// "2026-07" -> "July 2026"
const formatMonth = (period) => {
    const d = new Date(`${period}-01`);
    if (isNaN(d)) return period;
    return d.toLocaleDateString("en-GB", { month: "long", year: "numeric" });
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

async function postJson(path, body) {
    const res = await fetch(`${API_BASE}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
        const message = data && data.error ? data.error : `Request failed: ${path} (${res.status})`;
        throw new Error(message);
    }
    return data;
}

async function putJson(path, body) {
    const res = await fetch(`${API_BASE}${path}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
        const message = data && data.error ? data.error : `Request failed: ${path} (${res.status})`;
        throw new Error(message);
    }
    return data;
}

// Cached account list so several panels (payments, insights) can reuse it.
const state = { accounts: [] };
const accountName = (id) => {
    const a = state.accounts.find((x) => x.id === id);
    return a ? a.nickname : id;
};

// ----- Summary -------------------------------------------------------------
function renderSummary(summary) {
    document.getElementById("summary-total-balance").textContent = gbp(summary.totalBalance);
    document.getElementById("summary-account-count").textContent = summary.accountCount;
    document.getElementById("summary-mortgage-balance").textContent = gbp(summary.totalMortgageOutstanding);
}

// ----- Accounts ------------------------------------------------------------
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

// ----- Mortgages -----------------------------------------------------------
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

// ----- Transactions (shared renderer for drawer + search) ------------------
function renderTransactions(transactions, target) {
    const container = typeof target === "string"
        ? document.getElementById(target)
        : (target || document.getElementById("drawer-transactions"));
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

// ----- Transactions search panel -------------------------------------------
let txnSearchTimer = null;

function currentTxnQuery() {
    const params = new URLSearchParams();
    const q = document.getElementById("txn-search").value.trim();
    const account = document.getElementById("txn-account").value;
    const category = document.getElementById("txn-category").value;
    const type = document.getElementById("txn-type").value;
    const from = document.getElementById("txn-from").value;
    const to = document.getElementById("txn-to").value;
    if (q) params.set("query", q);
    if (account) params.set("accountId", account);
    if (category) params.set("category", category);
    if (type) params.set("type", type);
    if (from) params.set("from", from);
    if (to) params.set("to", to);
    const qs = params.toString();
    return qs ? `?${qs}` : "";
}

async function runTransactionSearch() {
    const container = document.getElementById("transactions-results");
    try {
        const txns = await fetchJson(`/api/transactions${currentTxnQuery()}`);
        renderTransactions(txns, "transactions-results");
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="error">Could not load transactions.</div>';
    }
}

async function setupTransactionsPanel() {
    // account filter options
    const accSel = document.getElementById("txn-account");
    state.accounts.forEach((a) => {
        const opt = document.createElement("option");
        opt.value = a.id;
        opt.textContent = a.nickname;
        accSel.appendChild(opt);
    });
    // category options
    try {
        const categories = await fetchJson("/api/transactions/categories");
        const catSel = document.getElementById("txn-category");
        categories.forEach((c) => {
            const opt = document.createElement("option");
            opt.value = c;
            opt.textContent = c;
            catSel.appendChild(opt);
        });
    } catch (err) {
        console.error(err);
    }

    const debounced = () => {
        clearTimeout(txnSearchTimer);
        txnSearchTimer = setTimeout(runTransactionSearch, 200);
    };
    document.getElementById("txn-search").addEventListener("input", debounced);
    ["txn-account", "txn-category", "txn-type", "txn-from", "txn-to"].forEach((id) => {
        document.getElementById(id).addEventListener("change", runTransactionSearch);
    });
    document.getElementById("txn-clear").addEventListener("click", () => {
        ["txn-search", "txn-account", "txn-category", "txn-type", "txn-from", "txn-to"].forEach((id) => {
            document.getElementById(id).value = "";
        });
        runTransactionSearch();
    });

    runTransactionSearch();
}

// ----- Payments panel ------------------------------------------------------
function renderPayees(payees) {
    const container = document.getElementById("payees-list");
    if (!payees || payees.length === 0) {
        container.innerHTML = '<div class="empty">No saved payees.</div>';
        return;
    }
    container.innerHTML = payees
        .map(
            (p) => `
        <div class="payee-row">
            <div class="payee-icon">${initials(p.name)}</div>
            <div class="payee-main">
                <div class="name">${p.name}</div>
                <div class="meta">${p.sortCode} &middot; ${p.accountNumberMasked}</div>
            </div>
            <div class="payee-last">${p.lastAmount != null ? gbp(p.lastAmount) : "&mdash;"}
                <div class="txn-after">${p.lastPaidDate ? "Last paid " + formatDate(p.lastPaidDate) : "No payments yet"}</div>
            </div>
        </div>`
        )
        .join("");
}

function renderScheduled(items) {
    const container = document.getElementById("scheduled-list");
    if (!items || items.length === 0) {
        container.innerHTML = '<div class="empty">No scheduled payments.</div>';
        return;
    }
    const kindLabel = { STANDING_ORDER: "Standing order", DIRECT_DEBIT: "Direct debit", SCHEDULED: "Scheduled" };
    container.innerHTML = items
        .map(
            (s) => `
        <div class="scheduled-row">
            <div class="sched-icon">${initials(s.payeeName)}</div>
            <div class="sched-main">
                <div class="name">${s.payeeName}</div>
                <div class="meta">
                    <span class="pill pill-${(s.kind || "").toLowerCase()}">${kindLabel[s.kind] || s.kind}</span>
                    ${s.frequency} &middot; from ${accountName(s.accountId)}
                </div>
            </div>
            <div class="sched-amounts">
                <div class="balance">${gbp(s.amount)}</div>
                <div class="available">Next ${formatDate(s.nextDate)}</div>
            </div>
            <div class="status status-${(s.status || "").toLowerCase()}">${s.status}</div>
        </div>`
        )
        .join("");
}

function populateAccountSelect(select, selectedId) {
    select.innerHTML = state.accounts
        .map((a) => `<option value="${a.id}"${a.id === selectedId ? " selected" : ""}>${a.nickname} · ${gbp(a.balance)}</option>`)
        .join("");
}

async function setupPaymentsPanel() {
    const fromSel = document.getElementById("tf-from");
    const toSel = document.getElementById("tf-to");
    populateAccountSelect(fromSel, state.accounts[0] && state.accounts[0].id);
    populateAccountSelect(toSel, state.accounts[1] && state.accounts[1].id);

    document.getElementById("transfer-form").addEventListener("submit", async (e) => {
        e.preventDefault();
        const result = document.getElementById("transfer-result");
        const amount = parseFloat(document.getElementById("tf-amount").value);
        const body = {
            fromAccountId: fromSel.value,
            toAccountId: toSel.value,
            amount: isNaN(amount) ? null : amount,
            reference: document.getElementById("tf-reference").value.trim(),
        };
        result.className = "transfer-result";
        result.textContent = "Sending…";
        try {
            const res = await postJson("/api/transfers", body);
            result.classList.add("ok");
            result.textContent = `Sent ${gbp(res.amount)} to ${accountName(res.toAccountId)}. New balance ${gbp(res.fromBalance)}.`;
            document.getElementById("tf-amount").value = "";
            document.getElementById("tf-reference").value = "";
            await refreshAccounts();
            populateAccountSelect(fromSel, fromSel.value);
            populateAccountSelect(toSel, toSel.value);
        } catch (err) {
            result.classList.add("error");
            result.textContent = err.message;
        }
    });

    try {
        const [payees, scheduled] = await Promise.all([
            fetchJson("/api/payees"),
            fetchJson("/api/payments/scheduled"),
        ]);
        renderPayees(payees);
        renderScheduled(scheduled);
    } catch (err) {
        console.error(err);
    }
}

// ----- Cards panel ---------------------------------------------------------
function renderCards(cards) {
    const container = document.getElementById("cards-list");
    if (!cards || cards.length === 0) {
        container.innerHTML = '<div class="empty">No cards.</div>';
        return;
    }
    container.innerHTML = cards
        .map((c) => {
            const frozen = String(c.status).toUpperCase() === "FROZEN";
            const limitLabel = c.type === "CREDIT" ? "Credit limit" : "Daily ATM limit";
            return `
        <div class="bank-card ${frozen ? "frozen" : ""} ${c.type === "CREDIT" ? "credit" : "debit"}" data-card-id="${c.id}">
            <div class="bank-card-top">
                <span class="bank-card-type">${c.type}</span>
                <span class="bank-card-net">${c.network}</span>
            </div>
            <div class="bank-card-pan">${c.panMasked}</div>
            <div class="bank-card-row">
                <div><div class="k">Cardholder</div><div class="v">${c.cardholder}</div></div>
                <div><div class="k">Expires</div><div class="v">${c.expiry}</div></div>
            </div>
            <div class="bank-card-row">
                <div><div class="k">This month</div><div class="v">${gbp(c.monthlySpend)}</div></div>
                <div><div class="k">${limitLabel}</div><div class="v">${gbp(c.limit)}</div></div>
            </div>
            <div class="bank-card-foot">
                <span class="status status-${frozen ? "paused" : "active"}">${frozen ? "FROZEN" : "ACTIVE"}</span>
                <span class="contactless">${c.contactless ? "Contactless" : "No contactless"}</span>
                <button class="btn-ghost freeze-btn" data-card-id="${c.id}">${frozen ? "Unfreeze" : "Freeze"}</button>
            </div>
        </div>`;
        })
        .join("");

    container.querySelectorAll(".freeze-btn").forEach((btn) => {
        btn.addEventListener("click", async () => {
            const id = btn.getAttribute("data-card-id");
            btn.disabled = true;
            try {
                await postJson(`/api/cards/${id}/freeze`);
                await refreshCards();
            } catch (err) {
                console.error(err);
                btn.disabled = false;
            }
        });
    });
}

async function refreshCards() {
    try {
        const cards = await fetchJson("/api/cards");
        renderCards(cards);
    } catch (err) {
        console.error(err);
        document.getElementById("cards-list").innerHTML = '<div class="error">Could not load cards.</div>';
    }
}

// ----- Insights panel ------------------------------------------------------
function renderInsights(insights) {
    const container = document.getElementById("insights-content");
    if (!insights) {
        container.innerHTML = '<div class="empty">No insights available.</div>';
        return;
    }
    const cats = insights.categories || [];
    const max = cats.reduce((m, c) => Math.max(m, Number(c.amount)), 0) || 1;
    const bars = cats
        .map(
            (c) => `
        <div class="insight-bar-row">
            <div class="insight-cat">${c.category}</div>
            <div class="insight-track"><div class="insight-fill" style="width:${Math.max(4, (Number(c.amount) / max) * 100)}%"></div></div>
            <div class="insight-amt">${gbp(c.amount)}</div>
        </div>`
        )
        .join("");

    container.innerHTML = `
        <div class="insight-tiles">
            <div class="card summary-card"><div class="summary-label">Money in</div><div class="summary-value credit">${gbp(insights.moneyIn)}</div></div>
            <div class="card summary-card"><div class="summary-label">Money out</div><div class="summary-value debit">${gbp(insights.moneyOut)}</div></div>
            <div class="card summary-card"><div class="summary-label">Net</div><div class="summary-value">${gbp(insights.net)}</div></div>
        </div>
        <div class="panel-subhead"><h3>Where your money went</h3></div>
        <div class="insight-bars">${bars || '<div class="empty">No spending in this period.</div>'}</div>`;
}

async function loadInsights(accountId) {
    const container = document.getElementById("insights-content");
    container.innerHTML = '<div class="empty">Loading…</div>';
    try {
        const insights = await fetchJson(`/api/accounts/${accountId}/insights`);
        renderInsights(insights);
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="error">Could not load insights.</div>';
    }
}

function setupInsightsPanel() {
    const sel = document.getElementById("insights-account");
    populateAccountSelect(sel, state.accounts[0] && state.accounts[0].id);
    sel.addEventListener("change", () => loadInsights(sel.value));
    if (state.accounts[0]) loadInsights(state.accounts[0].id);
}

// ----- Notifications inbox -------------------------------------------------
function renderNotifications(notifications) {
    const container = document.getElementById("notifications-list");
    if (!notifications || notifications.length === 0) {
        container.innerHTML = '<div class="empty">No messages.</div>';
        return;
    }
    container.innerHTML = notifications
        .map(
            (n) => `
        <div class="notif-row ${n.read ? "" : "unread"}">
            <span class="notif-tag tag-${(n.category || "").toLowerCase()}">${n.category}</span>
            <div class="notif-main">
                <div class="notif-title">${n.title}</div>
                <div class="notif-msg">${n.message}</div>
                <div class="notif-date">${formatDate(n.date)}</div>
            </div>
        </div>`
        )
        .join("");
}

async function loadNotifications() {
    try {
        const [notifications, count] = await Promise.all([
            fetchJson("/api/notifications"),
            fetchJson("/api/notifications/unread-count"),
        ]);
        renderNotifications(notifications);
        const badge = document.getElementById("bell-badge");
        if (count.unread > 0) {
            badge.textContent = count.unread;
            badge.hidden = false;
        } else {
            badge.hidden = true;
        }
    } catch (err) {
        console.error(err);
    }
}

function openNotifications() {
    const drawer = document.getElementById("notifications-drawer");
    drawer.classList.add("open");
    drawer.setAttribute("aria-hidden", "false");
}

function closeNotifications() {
    const drawer = document.getElementById("notifications-drawer");
    drawer.classList.remove("open");
    drawer.setAttribute("aria-hidden", "true");
}

// ----- Shared account refresh ----------------------------------------------
async function refreshAccounts() {
    try {
        const [summary, accounts] = await Promise.all([
            fetchJson("/api/summary"),
            fetchJson("/api/accounts"),
        ]);
        state.accounts = accounts;
        renderSummary(summary);
        renderAccounts(accounts);
    } catch (err) {
        console.error(err);
    }
}

// ----- Tabs / drawers ------------------------------------------------------
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
    document.querySelectorAll("[data-close-notifications]").forEach((el) => {
        el.addEventListener("click", closeNotifications);
    });
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeDrawer();
            closeNotifications();
        }
    });
    const bell = document.getElementById("bell-btn");
    if (bell) bell.addEventListener("click", openNotifications);
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
        state.accounts = accounts;
        renderSummary(summary);
        renderAccounts(accounts);
        renderMortgages(mortgages);

        // Wire up the enriched panels once we know the accounts.
        setupTransactionsPanel();
        setupPaymentsPanel();
        refreshCards();
        setupInsightsPanel();
        loadNotifications();
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
