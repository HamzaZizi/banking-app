// =============================================================================
// My Vodafone — customer self-service account portal (demo)
//
// This is a static, self-contained front end. All content is rendered from the
// DEMO_DATA object below so the page looks complete when deployed to any
// environment, with no dependency on a live backend. The small set of pure
// utility functions (gbp, formatDate, initials, fetchJson) and the render
// functions are exported at the bottom for the Jest test suite.
// =============================================================================

// If a backend ever is wired up, APP_CONFIG.apiBaseUrl points at it. Kept for
// parity with the deployment tooling; the UI does not require it.
const API_BASE = (typeof window !== "undefined" && window.APP_CONFIG && typeof window.APP_CONFIG.apiBaseUrl === "string")
    ? window.APP_CONFIG.apiBaseUrl
    : "http://localhost:8080";

// ---- Formatting utilities ---------------------------------------------------

const gbp = (value) =>
    new Intl.NumberFormat("en-GB", { style: "currency", currency: "GBP" }).format(value);

// "2026-07-16" -> "16 Jul 2026"
const formatDate = (iso) => {
    const d = new Date(iso);
    if (isNaN(d)) return iso;
    return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
};

// Two initials for a name/label, used in the little round icons.
const initials = (text) => {
    const words = String(text).trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) return "?";
    if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
    return (words[0][0] + words[1][0]).toUpperCase();
};

// 62.4 -> "62.4 GB"; 100 -> "100 GB"; "Unlimited"/Infinity -> "Unlimited"
const formatGB = (gb) => {
    if (gb === "Unlimited" || gb === Infinity) return "Unlimited";
    const n = Number(gb);
    if (!isFinite(n)) return String(gb);
    return `${n % 1 === 0 ? n.toFixed(0) : n.toFixed(1)} GB`;
};

// Clamped 0-100 percentage. Unlimited allowances report 0 (never "full").
const pct = (used, total) => {
    if (!total || total === "Unlimited" || total === Infinity) return 0;
    return Math.max(0, Math.min(100, Math.round((Number(used) / Number(total)) * 100)));
};

async function fetchJson(path) {
    const res = await fetch(`${API_BASE}${path}`);
    if (!res.ok) {
        throw new Error(`Request failed: ${path} (${res.status})`);
    }
    return res.json();
}

// ---- Demo data --------------------------------------------------------------

const DEMO_DATA = {
    customer: {
        name: "Alex Morgan",
        accountNumber: "20 8412 6673",
        mobile: "07700 900482",
        email: "alex.morgan@example.co.uk",
        memberSince: "March 2019",
    },
    plan: {
        name: "Red Entertainment 100GB",
        network: "5G",
        price: 24.0,
        dataAllowanceGB: 100,
        minutes: "Unlimited",
        texts: "Unlimited",
        contractEnd: "2026-03-14",
        addOns: [
            { name: "Global Roaming Plus", detail: "80+ destinations", price: "£6 / day" },
            { name: "Spotify Premium", detail: "Included with your plan", price: "Included" },
        ],
    },
    usage: {
        cycleStart: "2026-07-01",
        cycleEnd: "2026-07-31",
        daysLeft: 9,
        dataUsedGB: 62.4,
        dataAllowanceGB: 100,
        minutesUsed: 214,
        textsUsed: 96,
        roamingUsedGB: 1.2,
        roamingAllowanceGB: 25,
        dailyGB: [1.8, 2.4, 3.1, 1.2, 2.9, 4.2, 2.6],
    },
    bills: [
        {
            id: "bill-2607", period: "July 2026", issued: "2026-07-22", due: "2026-08-01",
            amount: 31.5, status: "DUE",
            breakdown: [
                { label: "Airtime plan — Red Entertainment 100GB", amount: 24.0 },
                { label: "Global Roaming Plus (5 days)", amount: 4.0 },
                { label: "Out-of-plan usage", amount: 1.25 },
                { label: "VAT", amount: 2.25 },
            ],
        },
        {
            id: "bill-2606", period: "June 2026", issued: "2026-06-22", due: "2026-07-01",
            amount: 24.0, status: "PAID",
            breakdown: [
                { label: "Airtime plan — Red Entertainment 100GB", amount: 20.0 },
                { label: "VAT", amount: 4.0 },
            ],
        },
        {
            id: "bill-2605", period: "May 2026", issued: "2026-05-22", due: "2026-06-01",
            amount: 30.5, status: "PAID",
            breakdown: [
                { label: "Airtime plan — Red Entertainment 100GB", amount: 20.0 },
                { label: "Data XL Booster — 20GB", amount: 5.42 },
                { label: "VAT", amount: 5.08 },
            ],
        },
        {
            id: "bill-2604", period: "April 2026", issued: "2026-04-22", due: "2026-05-01",
            amount: 24.0, status: "PAID",
            breakdown: [
                { label: "Airtime plan — Red Entertainment 100GB", amount: 20.0 },
                { label: "VAT", amount: 4.0 },
            ],
        },
        {
            id: "bill-2603", period: "March 2026", issued: "2026-03-22", due: "2026-04-01",
            amount: 24.0, status: "PAID",
            breakdown: [
                { label: "Airtime plan — Red Entertainment 100GB", amount: 20.0 },
                { label: "VAT", amount: 4.0 },
            ],
        },
    ],
    broadband: {
        package: "Pro II Full Fibre 900",
        speed: "910 Mbps average download",
        uploadSpeed: "104 Mbps average upload",
        price: 42.0,
        router: "Vodafone Wi-Fi 6E Hub",
        routerStatus: "Online",
        connectedDevices: 14,
        nextBillDate: "2026-08-03",
        contractEnd: "2027-01-20",
    },
    devices: [
        {
            model: "iPhone 15 Pro",
            spec: "256GB · Blue Titanium",
            imei: "35 682910 442017 3",
            upgradeEligible: "2026-03-14",
            condition: "On airtime plan",
        },
    ],
    paymentMethod: {
        type: "Direct Debit",
        detail: "Visa Debit ending 4471",
        nextDate: "2026-08-01",
    },
    payments: [
        { id: "pay-1", date: "2026-07-01", description: "Monthly bill", method: "Direct Debit · Visa ••4471", amount: -24.0, type: "DEBIT" },
        { id: "pay-2", date: "2026-06-14", description: "Data add-on — 20GB Booster", method: "Visa Debit ••4471", amount: -6.5, type: "DEBIT" },
        { id: "pay-3", date: "2026-06-02", description: "Bill adjustment refund", method: "Visa Debit ••4471", amount: 3.2, type: "CREDIT" },
        { id: "pay-4", date: "2026-06-01", description: "Monthly bill", method: "Direct Debit · Visa ••4471", amount: -24.0, type: "DEBIT" },
        { id: "pay-5", date: "2026-05-18", description: "Pay as you go top-up", method: "Apple Pay", amount: -10.0, type: "DEBIT" },
        { id: "pay-6", date: "2026-05-01", description: "Monthly bill", method: "Direct Debit · Visa ••4471", amount: -30.5, type: "DEBIT" },
    ],
    addonsAvailable: [
        { name: "Data XL Booster — 20GB", detail: "Extra data until your next bill", price: "£10 / month" },
        { name: "Global Roaming Plus", detail: "Use your plan in 80+ destinations", price: "£6 / day" },
        { name: "Vodafone Secure Net", detail: "Protect every device on your network", price: "£2 / month" },
        { name: "VeryMe Rewards", detail: "Weekly treats and prize draws", price: "Free" },
    ],
    notifications: [
        { id: "n1", tag: "payment", title: "Your July bill is ready", message: "Your bill of £31.50 is due on 1 Aug 2026. It'll be taken by Direct Debit.", date: "2026-07-22", unread: true },
        { id: "n2", tag: "info", title: "You've used 62% of your data", message: "62.4 GB of 100 GB used with 9 days left in this cycle.", date: "2026-07-20", unread: true },
        { id: "n3", tag: "offer", title: "You're eligible for an upgrade soon", message: "Your upgrade window opens on 14 Mar 2026. Browse the latest devices.", date: "2026-07-15", unread: false },
        { id: "n4", tag: "security", title: "New login to My Vodafone", message: "We noticed a sign-in from a new device. If this wasn't you, secure your account.", date: "2026-07-11", unread: false },
    ],
};

// ---- Small DOM helpers ------------------------------------------------------

const $ = (id) => (typeof document !== "undefined" ? document.getElementById(id) : null);
const setText = (id, text) => { const el = $(id); if (el) el.textContent = text; };

// ---- Render: dashboard ------------------------------------------------------

function renderDashboard(data) {
    const bill = data.bills && data.bills[0];
    const usage = data.usage || {};
    const plan = data.plan || {};

    if (bill) {
        setText("summary-bill-amount", gbp(bill.amount));
        setText("summary-bill-foot", `${bill.status === "DUE" ? "Due" : "Paid"} ${formatDate(bill.due)}`);
    }

    const usedPct = pct(usage.dataUsedGB, usage.dataAllowanceGB);
    setText("summary-data-value", formatGB(usage.dataUsedGB));
    setText("summary-data-foot", `of ${formatGB(usage.dataAllowanceGB)} · ${usage.daysLeft} days left`);
    setText("summary-plan-value", plan.name || "—");
    setText("summary-plan-foot", `${gbp(plan.price)} a month · ${plan.network || ""}`);

    // Data usage ring on the dashboard.
    const ring = $("dash-ring");
    if (ring) {
        ring.style.background =
            `conic-gradient(var(--brand-red) ${usedPct * 3.6}deg, var(--border) 0deg)`;
    }
    setText("dash-ring-pct", `${usedPct}%`);
    setText("dash-ring-label", `${formatGB(usage.dataUsedGB)} of ${formatGB(usage.dataAllowanceGB)}`);
    setText("dash-ring-sub", `${usage.daysLeft} days left in this cycle`);

    if (bill) {
        setText("dash-bill-amount", gbp(bill.amount));
        setText("dash-bill-due", `${bill.status === "DUE" ? "Due" : "Paid"} ${formatDate(bill.due)}`);
    }
    setText("dash-plan-name", plan.name || "—");
    setText("dash-plan-mins", `${plan.minutes} minutes`);
    setText("dash-plan-texts", `${plan.texts} texts`);
}

// ---- Render: usage ----------------------------------------------------------

function usageTile({ title, valueHtml, sub, ringPct }) {
    const hasRing = typeof ringPct === "number";
    return `
        <div class="usage-tile">
            ${hasRing
                ? `<div class="usage-ring" style="background: conic-gradient(var(--brand-red) ${ringPct * 3.6}deg, var(--border) 0deg);">
                       <span>${ringPct}%</span>
                   </div>`
                : `<div class="usage-ring unlimited"><span>&#8734;</span></div>`}
            <div class="usage-tile-body">
                <div class="usage-tile-title">${title}</div>
                <div class="usage-tile-value">${valueHtml}</div>
                <div class="usage-tile-sub">${sub}</div>
            </div>
        </div>`;
}

function renderUsage(usage, plan) {
    const container = $("usage-content");
    if (!container) return;
    plan = plan || {};

    const dataPct = pct(usage.dataUsedGB, usage.dataAllowanceGB);
    const roamPct = pct(usage.roamingUsedGB, usage.roamingAllowanceGB);
    const maxDaily = Math.max(...(usage.dailyGB || [1]));

    const dailyBars = (usage.dailyGB || [])
        .map((gb, i) => `
            <div class="daily-bar">
                <div class="daily-bar-track">
                    <div class="daily-bar-fill" style="height: ${Math.round((gb / maxDaily) * 100)}%"></div>
                </div>
                <div class="daily-bar-label">D${i + 1}</div>
            </div>`)
        .join("");

    container.innerHTML = `
        <div class="usage-grid">
            ${usageTile({
                title: "Data",
                valueHtml: `${formatGB(usage.dataUsedGB)} <span class="muted">/ ${formatGB(usage.dataAllowanceGB)}</span>`,
                sub: `${usage.daysLeft} days left in this cycle`,
                ringPct: dataPct,
            })}
            ${usageTile({
                title: "Minutes",
                valueHtml: `${usage.minutesUsed} used`,
                sub: `${plan.minutes} allowance`,
            })}
            ${usageTile({
                title: "Texts",
                valueHtml: `${usage.textsUsed} used`,
                sub: `${plan.texts} allowance`,
            })}
            ${usageTile({
                title: "Roaming data",
                valueHtml: `${formatGB(usage.roamingUsedGB)} <span class="muted">/ ${formatGB(usage.roamingAllowanceGB)}</span>`,
                sub: "Global Roaming Plus",
                ringPct: roamPct,
            })}
        </div>
        <div class="card usage-daily">
            <h3>Daily data · last 7 days</h3>
            <div class="daily-bars">${dailyBars}</div>
            <div class="usage-cycle">Billing cycle ${formatDate(usage.cycleStart)} – ${formatDate(usage.cycleEnd)}</div>
        </div>`;
}

// ---- Render: bills ----------------------------------------------------------

function renderBills(bills) {
    const container = $("bills-list");
    if (!container) return;

    if (!bills || bills.length === 0) {
        container.innerHTML = `<div class="empty">No bills to show yet.</div>`;
        return;
    }

    container.innerHTML = bills
        .map((b) => {
            const paid = b.status !== "DUE";
            return `
                <div class="bill-row" data-bill-id="${b.id}" role="button" tabindex="0">
                    <div class="bill-icon">${paid ? "&#10003;" : "&#163;"}</div>
                    <div class="bill-main">
                        <div class="bill-period">${b.period}</div>
                        <div class="bill-meta">Issued ${formatDate(b.issued)} · ${paid ? "Paid" : "Due"} ${formatDate(b.due)}</div>
                    </div>
                    <div class="bill-right">
                        <div class="bill-amount">${gbp(b.amount)}</div>
                        <span class="status ${paid ? "status-active" : "status-paused"}">${paid ? "PAID" : "DUE"}</span>
                    </div>
                    <span class="chevron">&rsaquo;</span>
                </div>`;
        })
        .join("");
}

function renderBillDetail(bill) {
    setText("bill-drawer-title", bill.period);
    setText("bill-drawer-sub", `${bill.status === "DUE" ? "Due" : "Paid"} ${formatDate(bill.due)} · Issued ${formatDate(bill.issued)}`);
    const body = $("bill-drawer-body");
    if (!body) return;
    const rows = bill.breakdown
        .map((line) => `
            <div class="breakdown-row">
                <span class="label">${line.label}</span>
                <span class="value">${gbp(line.amount)}</span>
            </div>`)
        .join("");
    body.innerHTML = `
        ${rows}
        <div class="breakdown-row total">
            <span class="label">Total</span>
            <span class="value">${gbp(bill.amount)}</span>
        </div>
        <button class="btn-primary btn-block" style="margin-top:16px">Download PDF</button>`;
}

// ---- Render: plan & devices -------------------------------------------------

function renderPlan(plan, devices, addons) {
    const container = $("plan-content");
    if (!container) return;
    plan = plan || {};
    devices = devices || [];
    addons = addons || [];

    const addOnRows = (plan.addOns || [])
        .map((a) => `
            <div class="line-row">
                <div>
                    <div class="line-title">${a.name}</div>
                    <div class="line-sub">${a.detail}</div>
                </div>
                <div class="line-price">${a.price}</div>
            </div>`)
        .join("");

    const deviceCards = devices
        .map((d) => `
            <div class="device-row">
                <div class="device-icon">&#128241;</div>
                <div class="device-main">
                    <div class="line-title">${d.model}</div>
                    <div class="line-sub">${d.spec}</div>
                    <div class="line-sub">IMEI ${d.imei}</div>
                </div>
                <div class="device-side">
                    <div class="line-sub">Upgrade from</div>
                    <div class="line-title">${formatDate(d.upgradeEligible)}</div>
                </div>
            </div>`)
        .join("");

    const availableAddons = addons
        .map((a) => `
            <div class="addon-card">
                <div class="addon-main">
                    <div class="line-title">${a.name}</div>
                    <div class="line-sub">${a.detail}</div>
                </div>
                <div class="addon-foot">
                    <span class="line-price">${a.price}</span>
                    <button class="btn-ghost">Add</button>
                </div>
            </div>`)
        .join("");

    container.innerHTML = `
        <div class="card plan-hero">
            <div class="plan-hero-top">
                <div>
                    <div class="plan-hero-name">${plan.name}</div>
                    <div class="line-sub">${plan.network} · SIM only · Airtime plan</div>
                </div>
                <div class="plan-hero-price">${gbp(plan.price)}<span>/mo</span></div>
            </div>
            <div class="plan-allowances">
                <div class="allowance"><div class="allowance-val">${formatGB(plan.dataAllowanceGB)}</div><div class="line-sub">Data</div></div>
                <div class="allowance"><div class="allowance-val">${plan.minutes}</div><div class="line-sub">Minutes</div></div>
                <div class="allowance"><div class="allowance-val">${plan.texts}</div><div class="line-sub">Texts</div></div>
            </div>
            <div class="line-sub" style="margin-top:14px">Minimum term ends ${formatDate(plan.contractEnd)}</div>
        </div>

        <div class="panel-subhead"><h3>Extras on your plan</h3></div>
        <div class="card">${addOnRows}</div>

        <div class="panel-subhead"><h3>Your device</h3></div>
        <div class="card">${deviceCards}</div>

        <div class="panel-subhead"><h3>Add to your plan</h3></div>
        <div class="addon-grid">${availableAddons}</div>`;
}

// ---- Render: broadband ------------------------------------------------------

function renderBroadband(broadband) {
    const container = $("broadband-content");
    if (!container) return;
    const b = broadband || {};

    container.innerHTML = `
        <div class="card bb-hero">
            <div class="bb-hero-top">
                <div>
                    <div class="plan-hero-name">${b.package}</div>
                    <div class="line-sub">${b.speed}</div>
                    <div class="line-sub">${b.uploadSpeed}</div>
                </div>
                <div class="plan-hero-price">${gbp(b.price)}<span>/mo</span></div>
            </div>
            <div class="bb-status">
                <span class="status status-active">${b.routerStatus}</span>
                <span class="line-sub">${b.router} · ${b.connectedDevices} devices connected</span>
            </div>
        </div>
        <div class="card-grid" style="margin-top:16px">
            <div class="card bb-fact"><div class="line-sub">Next bill</div><div class="bb-fact-val">${formatDate(b.nextBillDate)}</div></div>
            <div class="card bb-fact"><div class="line-sub">Minimum term ends</div><div class="bb-fact-val">${formatDate(b.contractEnd)}</div></div>
            <div class="card bb-fact"><div class="line-sub">Connection</div><div class="bb-fact-val">Full Fibre</div></div>
        </div>`;
}

// ---- Render: payments -------------------------------------------------------

function renderPayments(payments) {
    const container = $("payments-list");
    if (!container) return;

    if (!payments || payments.length === 0) {
        container.innerHTML = `<div class="empty">No payments to show yet.</div>`;
        return;
    }

    container.innerHTML = payments
        .map((p) => {
            const credit = p.type === "CREDIT";
            const amountText = credit
                ? `+${gbp(Math.abs(p.amount))}`
                : gbp(Math.abs(p.amount));
            return `
                <div class="payment-row">
                    <div class="payment-icon">${initials(p.description)}</div>
                    <div class="payment-main">
                        <div class="payment-desc">${p.description}</div>
                        <div class="payment-meta">${formatDate(p.date)} · ${p.method}</div>
                    </div>
                    <div class="payment-amount ${credit ? "credit" : "debit"}">${amountText}</div>
                </div>`;
        })
        .join("");
}

// ---- Render: notifications --------------------------------------------------

function renderNotifications(notifications) {
    const list = $("notifications-list");
    const unread = (notifications || []).filter((n) => n.unread).length;
    const badge = $("bell-badge");
    if (badge) {
        badge.textContent = String(unread);
        badge.hidden = unread === 0;
    }
    if (!list) return;
    list.innerHTML = (notifications || [])
        .map((n) => `
            <div class="notif-row ${n.unread ? "unread" : ""}">
                <span class="notif-tag tag-${n.tag}">${n.tag}</span>
                <div>
                    <div class="notif-title">${n.title}</div>
                    <div class="notif-msg">${n.message}</div>
                    <div class="notif-date">${formatDate(n.date)}</div>
                </div>
            </div>`)
        .join("");
}

// ---- Page wiring ------------------------------------------------------------

const PAGE_TITLES = {
    dashboard: ["Hi Alex, welcome back", "Here's everything on your account"],
    usage: ["Your usage", "Data, minutes and texts this cycle"],
    bills: ["Bills & payments", "View and download your monthly bills"],
    plan: ["Your plan", "Manage your tariff, extras and device"],
    broadband: ["Home broadband", "Your Full Fibre connection"],
    payments: ["Payment history", "Your recent charges and top-ups"],
};

function switchTab(tab) {
    document.querySelectorAll(".tab-btn").forEach((b) =>
        b.classList.toggle("active", b.dataset.tab === tab));
    document.querySelectorAll(".panel").forEach((p) =>
        p.classList.toggle("active", p.id === `${tab}-panel`));
    const titles = PAGE_TITLES[tab];
    if (titles) {
        setText("page-title", titles[0]);
        setText("page-sub", titles[1]);
    }
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function openDrawer(id) {
    const d = $(id);
    if (d) { d.classList.add("open"); d.setAttribute("aria-hidden", "false"); }
}
function closeDrawer(id) {
    const d = $(id);
    if (d) { d.classList.remove("open"); d.setAttribute("aria-hidden", "true"); }
}

function renderAll(data) {
    renderDashboard(data);
    renderUsage(data.usage, data.plan);
    renderBills(data.bills);
    renderPlan(data.plan, data.devices, data.addonsAvailable);
    renderBroadband(data.broadband);
    renderPayments(data.payments);
    renderNotifications(data.notifications);
    setText("user-greeting", data.customer.name);
    const avatar = $("user-avatar");
    if (avatar) avatar.textContent = initials(data.customer.name);
}

function showApp() {
    const login = $("login-screen");
    const shell = $("app-shell");
    if (login) login.hidden = true;
    if (shell) shell.hidden = false;
}

function showLogin() {
    const login = $("login-screen");
    const shell = $("app-shell");
    if (shell) shell.hidden = true;
    if (login) login.hidden = false;
}

function init() {
    // Login
    const loginForm = $("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", (e) => {
            e.preventDefault();
            showApp();
            renderAll(DEMO_DATA);
            switchTab("dashboard");
        });
    }
    const logout = $("logout-btn");
    if (logout) logout.addEventListener("click", showLogin);

    // Tabs (nav buttons and any in-page shortcut with data-tab)
    document.querySelectorAll("[data-tab]").forEach((btn) =>
        btn.addEventListener("click", () => switchTab(btn.dataset.tab)));

    // Bills drawer
    const billsList = $("bills-list");
    if (billsList) {
        billsList.addEventListener("click", (e) => {
            const row = e.target.closest(".bill-row");
            if (!row) return;
            const bill = DEMO_DATA.bills.find((b) => b.id === row.dataset.billId);
            if (bill) { renderBillDetail(bill); openDrawer("bill-drawer"); }
        });
    }

    // Notifications
    const bell = $("bell-btn");
    if (bell) bell.addEventListener("click", () => openDrawer("notifications-drawer"));

    document.querySelectorAll("[data-close-drawer]").forEach((el) =>
        el.addEventListener("click", () => closeDrawer("bill-drawer")));
    document.querySelectorAll("[data-close-notifications]").forEach((el) =>
        el.addEventListener("click", () => closeDrawer("notifications-drawer")));
}

if (typeof document !== "undefined") {
    document.addEventListener("DOMContentLoaded", init);
}

// ---- Jest export shim -------------------------------------------------------
if (typeof module !== "undefined" && module.exports) {
    module.exports = {
        gbp,
        formatDate,
        initials,
        formatGB,
        pct,
        fetchJson,
        renderDashboard,
        renderUsage,
        renderBills,
        renderPlan,
        renderBroadband,
        renderPayments,
        renderNotifications,
        DEMO_DATA,
    };
}
