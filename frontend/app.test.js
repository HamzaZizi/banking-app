/**
 * @jest-environment jsdom
 *
 * Unit tests for the frontend logic in app.js. app.js exports its functions
 * via a module.exports shim that only activates under Node/Jest.
 */

const { gbp, fetchJson, renderSummary, renderAccounts, renderMortgages } = require("./app.js");

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

describe("renderAccounts()", () => {
    test("renders one card per account with nickname and balance", () => {
        document.body.innerHTML = `<div id="accounts-list"></div>`;

        renderAccounts([
            {
                nickname: "Everyday Current Account",
                type: "CURRENT",
                sortCode: "60-16-13",
                accountNumberMasked: "****4471",
                balance: 3245.67,
                availableBalance: 3245.67,
            },
        ]);

        const html = document.getElementById("accounts-list").innerHTML;
        expect(html).toContain("Everyday Current Account");
        expect(html).toContain("£3,245.67");
        expect(document.querySelectorAll(".account-card")).toHaveLength(1);
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
