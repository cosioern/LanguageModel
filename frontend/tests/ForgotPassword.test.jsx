import { screen, render } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi, beforeEach, test, expect, } from "vitest";
import "@testing-library/jest-dom/vitest";
import ForgotPassword from "../src/components/ForgotPassword";

let fetchResult = false;

beforeEach(() => {

    vi.stubGlobal(
        "fetch",
        vi.fn(() => {
            if (fetchResult) {
                return Promise.resolve({ok: true, status: 200});
            }
            return Promise.resolve({ok: false, status: 501});
        })
    );

});


test("renders ForgotPassword page", () => {
    render(<ForgotPassword/>);

    expect(screen.getByText("Password Reset")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Email")).toBeInTheDocument();
    expect(screen.getByText("Send Link")).toBeInTheDocument();
});

test("handleSubmit input validation", async () => {
    render(<ForgotPassword/>);
    const user = userEvent.setup();
    const button = screen.getByText("Send Link");
    
    await user.click(button);
    expect(screen.getByText("Enter an Email"));

    const textarea = screen.getByPlaceholderText("Email");
    await user.type(textarea, "email@emali");
    await user.click(button);
    expect(screen.getByText("Invalid Email"));
});

test("handleSubmit presents correct mesage on failure", async () => {
    render(<ForgotPassword/>);
    fetchResult = false;
    const user = userEvent.setup();
    const button = screen.getByText("Send Link");
    const textarea = screen.getByPlaceholderText("Email");

    await user.type(textarea, "email@email.com");
    await user.click(button);

    expect(fetch).toHaveBeenCalledWith("/api/forgotPassword", {
        method: "POST", 
        headers: {"Content-Type": "application/x-www-form-urlencoded"}, 
        body: new URLSearchParams({email: "email@email.com"}),
    });
    expect(screen.getByText("Email not found"));
});

test("handleSubmit presents correct message on success", async () => {
    render(<ForgotPassword/>);
    fetchResult = true;
    const user = userEvent.setup();
    const button = screen.getByText("Send Link");
    const textarea = screen.getByPlaceholderText("Email");

    await user.type(textarea, "email@email.com");
    await user.click(button);
    expect(fetch).toHaveBeenCalledWith("/api/forgotPassword", {
        method: "POST", 
        headers: {"Content-Type": "application/x-www-form-urlencoded"}, 
        body: new URLSearchParams({email: "email@email.com"}),
    });
    expect(screen.getByText("Check your email"));
});