import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, vi, expect } from "vitest";
import Verify from "../src/components/Verify";

const mockNavigate = vi.fn();
let successfulCall = false;

beforeEach(() => {

    vi.mock("react-router-dom", async () => {
        const actual = await vi.importActual("react-router-dom");
        return { ...actual, useNavigate: () => mockNavigate };
    });

    vi.stubGlobal("fetch",
        vi.fn(() => {
            if (successfulCall) {
                return Promise.resolve({ok: true, status: 200});
            }
            return Promise.resolve({ok: false, status: 501});
        })
    );

})

test("Verify fails to redirect to chat", async () => {
    successfulCall = false;
    render(<Verify />, { wrapper: MemoryRouter });
    await Promise.resolve();

    expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/verify"),
        expect.objectContaining({credentials: "include"}, {method: "GET"}),
    );

    expect(mockNavigate).not.toHaveBeenCalledWith("/chat");
});

test("Verify redirects to chat", async () => {
    successfulCall = true;
    render(<Verify />, { wrapper: MemoryRouter });
    await Promise.resolve();

    expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/verify"),
        expect.objectContaining({credentials: "include"}, {method: "GET"}),
    );

    expect(mockNavigate).toHaveBeenCalledWith("/chat");
    expect(mockNavigate).toHaveBeenCalledTimes(1);
});