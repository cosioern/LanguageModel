import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi, beforeEach, afterEach, test, expect } from "vitest";
import "@testing-library/jest-dom";
import Register from "../src/components/Register";

let registerSucceeds = false;

beforeEach(() => {
    vi.stubGlobal(
        "fetch",
        vi.fn((url) => {
            if (registerSucceeds) {
                return Promise.resolve({ ok: true, status: 200});
            }
            else {
                return Promise.resolve({ ok: false, text: () => ("Generic Backend Error Message") })
                // { ok: false, status: 401, value: "Generic Backend Error Message"});
            }
        })
    );
});

test("renders the register page", () => {
    render(<Register />);
    expect(screen.getByText("Wanna Chat?")).toBeInTheDocument();
    expect(screen.getByText("Create an Account")).toBeInTheDocument();

    expect(screen.getByPlaceholderText("Username")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Email")).toBeInTheDocument();
    expect(screen.getByTitle("Date of Birth")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Password")).toBeInTheDocument();
    expect(screen.getByText("Register")).toBeInTheDocument();
});

test("correct input validation", async () => {
    render(<Register />);
    const user = userEvent.setup();
    const button = screen.getByText("Register");

    await user.click(button);
    expect(screen.getByText("Enter a Username"));

    let textarea = screen.getByPlaceholderText("Username");
    await user.type(textarea, "generic username");
    await user.click(button);
    expect(screen.getByText("Enter a Name"));
    
    textarea = screen.getByPlaceholderText("Name");
    await user.type(textarea, "generic name");
    await user.click(button);
    expect(screen.getByText("Enter an Email"));

    textarea = screen.getByPlaceholderText("Email");
    await user.type(textarea, "email@email");
    await user.click(button);
    expect(screen.getByText("Enter a Birthdate"));

    textarea = screen.getByTitle("Date of Birth");
    fireEvent.change(textarea, { target: { value: "2030-12-12" } });
    await user.click(button);
    expect(screen.getByText("Enter a Password"));
    
    textarea = screen.getByPlaceholderText("Password");
    await user.type(textarea, "generic password");
    await user.click(button);
    expect(screen.getByText("Invalid Email"));

    textarea = screen.getByPlaceholderText("Email");
    await user.clear(textarea);
    await user.type(textarea, "email@email.com");
    await user.click(button);
    expect(screen.getByText("Invalid Birthdate"));
});

test("successful api call", async () => {
    render(<Register/>);
    const user = userEvent.setup();
    const button = screen.getByText("Register");

    let textarea = screen.getByPlaceholderText("Username");
    await user.type(textarea, "generic username");
    textarea = screen.getByPlaceholderText("Name");
    await user.type(textarea, "generic name");
    textarea = screen.getByPlaceholderText("Password");
    await user.type(textarea, "generic password");
    textarea = screen.getByPlaceholderText("Email");
    await user.type(textarea, "email@email.com");
    textarea = screen.getByTitle("Date of Birth");
    fireEvent.change(textarea, { target: { value: "2026-09-11" } } );
    registerSucceeds = true;

    await user.click(button);
    expect(fetch).toHaveBeenCalledWith("/api/register", {method: "POST", headers: {"Content-Type":"application/x-www-form-urlencoded"}, body: expect.any(URLSearchParams),});
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(screen.findByText("Check your email to verify your account and close this tab."));
});

test("failed api call", async () => {
    render(<Register/>);
    const user = userEvent.setup();
    const button = screen.getByText("Register");

    let textarea = screen.getByPlaceholderText("Username");
    await user.type(textarea, "generic username");
    textarea = screen.getByPlaceholderText("Name");
    await user.type(textarea, "generic name");
    textarea = screen.getByPlaceholderText("Password");
    await user.type(textarea, "generic password");
    textarea = screen.getByPlaceholderText("Email");
    await user.type(textarea, "email@email.com");
    textarea = screen.getByTitle("Date of Birth");
    fireEvent.change(textarea, { target: { value: "2026-09-11" } } );
    registerSucceeds = false;

    await user.click(button);
    expect(fetch).toHaveBeenCalledWith("/api/register", {method: "POST", headers: {"Content-Type":"application/x-www-form-urlencoded"}, body: expect.any(URLSearchParams),});
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("Generic Backend Error Message"));
});

