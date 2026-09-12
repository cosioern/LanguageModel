import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi, beforeEach, test, expect } from "vitest";
import { MemoryRouter } from "react-router-dom";
import "@testing-library/jest-dom";
import Login from "../src/components/Login";

const mockNavigate = vi.fn();
let loginSucceeds = false;

beforeEach(() => {

    vi.stubGlobal(
        "fetch",
        vi.fn((url) => {
            if (loginSucceeds) {
                return Promise.resolve({ok: true, status: 200});
            }
            return Promise.resolve({ ok: false, status: 401});
        })
    );

    vi.mock("react-router-dom", async () => {
        const actual = await vi.importActual("react-router-dom");
        return { ...actual, useNavigate: () => mockNavigate };
    });

});

test("renders the login page", () => {
    render(<Login/>, { wrapper: MemoryRouter });
    expect(screen.getByText("Welcome Back"));
    expect(screen.getByText("Login to continue"));
    expect(screen.getByPlaceholderText("Username"));
    expect(screen.getByPlaceholderText("Password"));
    expect(screen.getByText("Login"));
    expect(screen.getByText("Forgot Your Password?"));
});

test("displays input validation from backend", async () => {
    render(<Login/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByText("Login");
    loginSucceeds = false;

    await user.click(button);
    expect(screen.getByText("Wrong username or password"));
});

test("redirects to chat page on successful login", async () => {
    render(<Login/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByText("Login");
    loginSucceeds = true;
    await user.click(button);

    expect(mockNavigate).toHaveBeenCalledWith("/chat");
});


test("redirects to the password reset page", async () => {
    render(<Login/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByText("Forgot Your Password?");
    await user.click(button);

    expect(mockNavigate).toHaveBeenCalledWith("/forgot-password");
})