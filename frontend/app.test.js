/**
 * @jest-environment jsdom
 *
 * Unit tests for the My Vodafone front-end logic in app.js. app.js exports its
 * functions via a module.exports shim that only activates under Node/Jest.
 */

const {
    gbp,
    formatDate,
    initials,
    formatGB,
    pct,
    fetchJson,
    renderDashboard,
    renderUsage,
    renderBills,
    renderPayments,
    renderBroadband,
    DEMO_DATA,
} = require("./app.js");

describe("gbp() currency formatter", () => {
    test("formats a whole number as GBP", () => {
        expect(gbp(1000)).toBe("£1,000.00");
    });

    test("formats a decimal amount with two places", () => {
        expect(gbp(31.5)).toBe("£31.50");
    });

    test("formats zero", () => {
        expect(gbp(0)).toBe("£0.00");
    });
});

describe("formatDate()", () => {
    test("formats an ISO date as DD Mon YYYY", () => {
        expect(formatDate("2026-07-16")).toBe("16 Jul 2026");
    });

    test("returns the input unchanged when not a valid date", () => {
        expect(formatDate("not-a-date")).toBe("not-a-date");
    });
});

describe("initials()", () => {
    test("takes first letters of the first two words", () => {
        expect(initials("Alex Morgan")).toBe("AM");
    });

    test("uses first two letters for a single word", () => {
        expect(initials("VODAFONE")).toBe("VO");
    });
});

describe("formatGB()", () => {
    test("formats a decimal amount with one place and unit", () => {
        expect(formatGB(62.4)).toBe("62.4 GB");
    });

    test("formats a whole number without decimals", () => {
        expect(formatGB(100)).toBe("100 GB");
    });

    test("passes through Unlimited", () => {
        expect(formatGB("Unlimited")).toBe("Unlimited");
    });
});

describe("pct()", () => {
    test("computes a rounded percentage", () => {
        expect(pct(62.4, 100)).toBe(62);
    });

    test("clamps above 100", () => {
        expect(pct(150, 100)).toBe(100);
    });

    test("returns 0 for an unlimited/zero allowance", () => {
        expect(pct(20, "Unlimited")).toBe(0);
        expect(pct(20, 0)).toBe(0);
    });
});

describe("fetchJson()", () => {
    afterEach(() => {
        delete global.fetch;
    });

    test("returns parsed JSON on a 200 response", async () => {
        global.fetch = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => ({ hello: "world" }),
        });
        await expect(fetchJson("/api/summary")).resolves.toEqual({ hello: "world" });
    });

    test("throws on a non-ok response", async () => {
        global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 500, json: async () => ({}) });
        await expect(fetchJson("/api/summary")).rejects.toThrow("Request failed: /api/summary (500)");
    });
});

describe("renderDashboard()", () => {
    test("writes the bill, data and plan summaries into the DOM", () => {
        document.body.innerHTML = `
            <span id="summary-bill-amount"></span>
            <span id="summary-bill-foot"></span>
            <span id="summary-data-value"></span>
            <span id="summary-data-foot"></span>
            <span id="summary-plan-value"></span>
            <span id="summary-plan-foot"></span>
            <div id="dash-ring"></div>
            <span id="dash-ring-pct"></span>
            <span id="dash-ring-label"></span>
            <span id="dash-ring-sub"></span>
            <span id="dash-bill-amount"></span>
            <span id="dash-bill-due"></span>
            <span id="dash-plan-name"></span>
            <span id="dash-plan-mins"></span>
            <span id="dash-plan-texts"></span>`;

        renderDashboard({
            bills: [{ amount: 31.5, status: "DUE", due: "2026-08-01" }],
            usage: { dataUsedGB: 62.4, dataAllowanceGB: 100, daysLeft: 9 },
            plan: { name: "Red Entertainment 100GB", price: 24, network: "5G", minutes: "Unlimited", texts: "Unlimited" },
        });

        expect(document.getElementById("summary-bill-amount").textContent).toBe("£31.50");
        expect(document.getElementById("summary-data-value").textContent).toBe("62.4 GB");
        expect(document.getElementById("summary-plan-value").textContent).toBe("Red Entertainment 100GB");
        expect(document.getElementById("dash-ring-pct").textContent).toBe("62%");
        expect(document.getElementById("dash-ring-label").textContent).toBe("62.4 GB of 100 GB");
    });
});

describe("renderBills()", () => {
    test("renders one clickable row per bill with amount and status", () => {
        document.body.innerHTML = `<div id="bills-list"></div>`;

        renderBills([
            { id: "bill-2607", period: "July 2026", issued: "2026-07-22", due: "2026-08-01", amount: 31.5, status: "DUE" },
            { id: "bill-2606", period: "June 2026", issued: "2026-06-22", due: "2026-07-01", amount: 24, status: "PAID" },
        ]);

        const rows = document.querySelectorAll(".bill-row");
        expect(rows).toHaveLength(2);
        expect(rows[0].getAttribute("data-bill-id")).toBe("bill-2607");
        const html = document.getElementById("bills-list").innerHTML;
        expect(html).toContain("July 2026");
        expect(html).toContain("£31.50");
        expect(html).toContain("DUE");
    });

    test("shows an empty state when there are no bills", () => {
        document.body.innerHTML = `<div id="bills-list"></div>`;
        renderBills([]);
        expect(document.getElementById("bills-list").innerHTML).toContain("No bills");
    });
});

describe("renderPayments()", () => {
    test("renders a row per payment with signed amounts", () => {
        document.body.innerHTML = `<div id="payments-list"></div>`;

        renderPayments([
            { id: "p1", description: "Bill adjustment refund", method: "Visa", date: "2026-06-02", type: "CREDIT", amount: 3.2 },
            { id: "p2", description: "Monthly bill", method: "Direct Debit", date: "2026-06-01", type: "DEBIT", amount: -24 },
        ]);

        const container = document.getElementById("payments-list");
        expect(container.querySelectorAll(".payment-row")).toHaveLength(2);
        expect(container.querySelector(".payment-amount.credit").textContent).toContain("+£3.20");
        expect(container.querySelector(".payment-amount.debit").textContent).toContain("£24.00");
    });
});

describe("renderBroadband()", () => {
    test("renders the broadband package with price and status", () => {
        document.body.innerHTML = `<div id="broadband-content"></div>`;

        renderBroadband({
            package: "Pro II Full Fibre 900",
            speed: "910 Mbps average download",
            uploadSpeed: "104 Mbps average upload",
            price: 42,
            router: "Vodafone Wi-Fi 6E Hub",
            routerStatus: "Online",
            connectedDevices: 14,
            nextBillDate: "2026-08-03",
            contractEnd: "2027-01-20",
        });

        const html = document.getElementById("broadband-content").innerHTML;
        expect(html).toContain("Pro II Full Fibre 900");
        expect(html).toContain("£42.00");
        expect(html).toContain("Online");
    });
});

describe("DEMO_DATA", () => {
    test("exposes a coherent set of demo content", () => {
        expect(DEMO_DATA.customer.name).toBe("Alex Morgan");
        expect(DEMO_DATA.bills.length).toBeGreaterThan(0);
        expect(DEMO_DATA.plan.dataAllowanceGB).toBe(100);
    });

    test("renderUsage builds tiles from demo usage without throwing", () => {
        document.body.innerHTML = `<div id="usage-content"></div>`;
        renderUsage(DEMO_DATA.usage, DEMO_DATA.plan);
        const html = document.getElementById("usage-content").innerHTML;
        expect(html).toContain("62.4 GB");
        expect(html).toContain("Roaming data");
    });
});
