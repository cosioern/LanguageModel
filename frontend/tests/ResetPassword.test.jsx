import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom/vitest";
import { MemoryRouter, useNavigate } from "react-router-dom";
import { vi, beforeEach, test, expect } from "vitest";
import ResetPassword from "../src/components/ResetPassword";

let fetchResult = false;
const mockNavigate = vi.fn();

beforeEach(() => {
    vi.stubGlobal("fetch",
        vi.fn((url) => {
            if (fetchResult) {
                return Promise.resolve({ok: true, status: 200});
            }
            return Promise.resolve({ok: false, status: 403});
        })
    );

    vi.mock("react-router-dom", async () => {
        const actual = await vi.importActual("react-router-dom");
        return { ...actual, useNavigate: () => mockNavigate };
    });
});

test("renders RestPassword page", async () => {
    render(<ResetPassword/>, {wrapper: MemoryRouter});
    expect(await screen.findByText("Set a New Password")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Select a New Password"));
    expect(screen.getByPlaceholderText("Retype Your New Password"));
    // expect(screen.getByText("Enter Your Current Password"));
    expect(screen.getByText("Submit")).toBeInTheDocument();
});

// no token input validation
test("handleSubmit input validation (no token)", async () => {
    render(<ResetPassword/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByText("Submit");

    await user.click(button);
    expect(screen.getByText("Enter a password")).toBeInTheDocument();

    let textarea = screen.getByPlaceholderText("Select a New Password");
    await user.type(textarea, "new password");
    await user.click(button);
    expect(screen.getByText("Retype Your Password")).toBeInTheDocument();

    textarea = screen.getByPlaceholderText("Retype Your New Password");
    await user.type(textarea, "mismatched password");
    await user.click(button);
    expect(screen.getByText("Passwords Must Match")).toBeInTheDocument();

    await user.clear(textarea);
    await user.type(textarea, "new password");
    await user.click(button);
    expect(screen.getByText("Enter your current password")).toBeInTheDocument();
});

test("handleSubmit sets toast on failed fetch (no token)", async () => {
    render(<ResetPassword/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByText("Submit");

    fetchResult = false;
    let textarea = screen.getByPlaceholderText("Select a New Password");
    await user.type(textarea, "new password");
    textarea = screen.getByPlaceholderText("Retype Your New Password");
    await user.type(textarea, "new password");
    textarea = screen.getByPlaceholderText("Enter Your Current Password");
    await user.type(textarea, "current password");
    await user.click(button);

    expect(fetch).toHaveBeenCalledWith("/api/changePassword", {
        method: "POST", 
        credentials: "include", 
        headers: {"Content-type":"application/x-www-form-urlencoded"}, 
        body: new URLSearchParams({newPassword: "new password", currentPassword: "current password"}),
    });

    expect(await screen.findByText("Wrong password"));
});

test("handleSubmit redirects page on successful fetch (no token)", async () => {
    render(<ResetPassword/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByText("Submit");

    fetchResult = true;
    let textarea = screen.getByPlaceholderText("Select a New Password");
    await user.type(textarea, "new password");
    textarea = screen.getByPlaceholderText("Retype Your New Password");
    await user.type(textarea, "new password");
    textarea = screen.getByPlaceholderText("Enter Your Current Password");
    await user.type(textarea, "current password");
    await user.click(button);


    expect(fetch).toHaveBeenCalledWith("/api/changePassword", {
        method: "POST", 
        credentials: "include", 
        headers: {"Content-type":"application/x-www-form-urlencoded"}, 
        body: new URLSearchParams({newPassword: "new password", currentPassword: "current password"}),
    });

    expect(await screen.findByText("Success. Redirecting to chat page."));
    expect(mockNavigate);
})

test("handleSubmit input validation (token)", async() => {
    render(<ResetPassword/>, {
        wrapper: ({children}) => (
            <MemoryRouter initialEntries={["/reset-password?token=abc123"]}>
                {children}
            </MemoryRouter>
        )
    });
    const user = userEvent.setup();
    const button = screen.getByText("Submit");

    await user.click(button);
    expect(screen.getByText("Enter a password"));

    let textarea = screen.getByPlaceholderText("Select a New Password");
    await user.type(textarea, "new password");
    await user.click(button);
    expect(screen.getByText("Retype Your Password"));

    textarea = screen.getByPlaceholderText("Retype Your New Password");
    await user.type(textarea, "mismatched password");
    await user.click(button);
    expect(screen.getByText("Passwords Must Match"));

    // user.clear(textarea);
    // await user.type(textarea, "new password");
    // await user.click(button);
    
});

test("handleSubmit sets toast on failed fetch (token)", async () => {
    render(<ResetPassword/>, {
        wrapper: ({children}) => (
            <MemoryRouter initialEntries={["/reset-password?token=abc123"]}>
                {children}
            </MemoryRouter>
        )
    });

    const user = userEvent.setup();
    const button = screen.getByText("Submit");

    fetchResult = false;
    let textarea = screen.getByPlaceholderText("Select a New Password");
    await user.type(textarea, "new password");
    textarea = screen.getByPlaceholderText("Retype Your New Password");
    await user.type(textarea, "new password");
    // textarea = screen.getByPlaceholderText("Enter Your Current Password");
    // await user.type(textarea, "current password");
    await user.click(button);

    expect(fetch).toHaveBeenCalledWith("/api/resetPassword?token=abc123", {
        method: "POST", 
        credentials: "include", 
        headers: {"Content-type":"application/x-www-form-urlencoded"}, 
        body: new URLSearchParams({newPassword: "new password"}),
    });

    expect(await screen.findByText("Wrong password"));
});


test("handleSubmit redirects page on successful fetch (token)", async () => {
    render(<ResetPassword/>, {
        wrapper: ({children}) => (
            <MemoryRouter initialEntries={["reset-password?token=abc123"]}>
                {children}
            </MemoryRouter>
        )
    })

    const user = userEvent.setup();
    const button = screen.getByText("Submit");

    fetchResult = false;
    let textarea = screen.getByPlaceholderText("Select a New Password");
    await user.type(textarea, "new password");
    textarea = screen.getByPlaceholderText("Retype Your New Password");
    await user.type(textarea, "new password");
    await user.click(button);

    fetchResult = true;
    await user.click(button);
    expect(fetch).toHaveBeenCalledWith("/api/resetPassword?token=abc123", {
        method: "POST", 
        credentials: "include", 
        headers: {"Content-type":"application/x-www-form-urlencoded"}, 
        body: new URLSearchParams({newPassword: "new password"}),
    });
    expect(await screen.findByText("Success. Redirecting to chat page."));
    expect(mockNavigate);
});