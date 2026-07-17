/**
 * @jest-environment jsdom
 *
 * Unit tests for the frontend logic in app.js. app.js exports its functions
 * via a module.exports shim that only activates under Node/Jest.
 */

const {
    gbp,
    formatDate,
    initials,
    fetchJson,
    renderSummary,
    renderAccounts,
    renderMortgages,
    renderTransactions,
} = require("./app.js");

describe("gbp() currency formatter", () => {
    test("formats a whole number as GBP", () => {
        expect(gbp(1000)).toBe("£1,000.00");
    });

    test("formats a decimal amount with two places", () => {
        expect(gbp(3245.6)).toBe("£3,245.60");
    });

    test("formats zero", () => {
        expect(gbp(0)).toBe("£0.00");
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

describe("renderSummary()", () => {
    test("writes formatted totals into the summary elements", () => {
        document.body.innerHTML = `
            <span id="summary-total-balance"></span>
            <span id="summary-account-count"></span>
            <span id="summary-mortgage-balance"></span>`;

        renderSummary({ totalBalance: 73936.09, accountCount: 3, totalMortgageOutstanding: 356500 });

        expect(document.getElementById("summary-total-balance").textContent).toBe("£73,936.09");
        expect(document.getElementById("summary-account-count").textContent).toBe("3");
        expect(document.getElementById("summary-mortgage-balance").textContent).toBe("£356,500.00");
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
        expect(initials("Everyday Current Account")).toBe("EC");
    });

    test("uses first two letters for a single word", () => {
        expect(initials("SAVINGS")).toBe("SA");
    });
});

describe("renderAccounts()", () => {
    test("renders one clickable row per account with nickname and balance", () => {
        document.body.innerHTML = `<div id="accounts-list"></div>`;

        renderAccounts([
            {
                id: "acc-001",
                nickname: "Everyday Current Account",
                type: "CURRENT",
                product: "Select Current Account",
                sortCode: "60-16-13",
                accountNumberMasked: "****4471",
                balance: 3245.67,
                availableBalance: 3745.67,
            },
        ]);

        const html = document.getElementById("accounts-list").innerHTML;
        expect(html).toContain("Everyday Current Account");
        expect(html).toContain("£3,245.67");
        const rows = document.querySelectorAll(".account-row");
        expect(rows).toHaveLength(1);
        expect(rows[0].getAttribute("data-account-id")).toBe("acc-001");
    });
});

describe("renderTransactions()", () => {
    test("renders a row per transaction with signed amounts", () => {
        document.body.innerHTML = `<div id="drawer-transactions"></div>`;

        renderTransactions([
            {
                id: "txn-1", description: "Salary - Harness Ltd", category: "Income",
                date: "2026-07-14", type: "CREDIT", amount: 2650, balanceAfter: 3303.71,
            },
            {
                id: "txn-2", description: "Sainsbury's Superstore", category: "Groceries",
                date: "2026-07-12", type: "DEBIT", amount: -61.83, balanceAfter: 749.91,
            },
        ]);

        const container = document.getElementById("drawer-transactions");
        expect(container.querySelectorAll(".txn-row")).toHaveLength(2);
        expect(container.querySelector(".txn-amount.credit").textContent).toContain("+£2,650.00");
        expect(container.querySelector(".txn-amount.debit").textContent).toContain("£61.83");
    });

    test("shows an empty state when there are no transactions", () => {
        document.body.innerHTML = `<div id="drawer-transactions"></div>`;
        renderTransactions([]);
        expect(document.getElementById("drawer-transactions").innerHTML).toContain("No recent transactions");
    });
});

describe("renderMortgages()", () => {
    test("renders one card per mortgage with product and outstanding balance", () => {
        document.body.innerHTML = `<div id="mortgages-list"></div>`;

        renderMortgages([
            {
                product: "2 Year Fixed 4.29%",
                outstandingBalance: 214500,
                originalAmount: 250000,
                interestRate: 4.29,
                rateType: "FIXED",
                rateEndDate: "2027-11-01",
                monthlyPayment: 1187.32,
                nextPaymentDate: "2026-08-01",
                termRemainingMonths: 264,
            },
        ]);

        const html = document.getElementById("mortgages-list").innerHTML;
        expect(html).toContain("2 Year Fixed 4.29%");
        expect(html).toContain("£214,500.00");
        expect(document.querySelectorAll(".mortgage-card")).toHaveLength(1);
    });
});
