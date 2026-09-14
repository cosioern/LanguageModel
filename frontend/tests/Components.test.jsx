import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom/vitest";
import { MemoryRouter } from "react-router-dom";
import {vi, beforeEach, test, expect } from "vitest";
import Left from "../src/components/Left";
import Right from "../src/components/Right";
import Middle from "../src/components/Middle";


const mockNavigate = vi.fn();
let fetchResult = false;
const setToast = vi.fn();

beforeEach(() => {
    
    vi.mock("react-router-dom", async () => {
        const actual = await vi.importActual("react-router-dom");
        return { ...actual, useNavigate: () => mockNavigate };
    });

    vi.stubGlobal("fetch",
        vi.fn((url) => {
            if (fetchResult) {
                return Promise.resolve({ok: true, status:200});
            }
            else {
                return Promise.resolve({ok: false, status:501});
            }
        })
    );

});

// Left tests
test("renders the Left component", () => {
    render(<Left isLoggedIn={false}/>, { wrapper: MemoryRouter});

    expect(screen.getByText("Built for Big Brains"));
    expect(screen.getByTitle("Profile"));
});

test("profile button redirects to register page when logged out", async () => {
    render(<Left isLoggedIn={false}/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByTitle("Profile");
    await user.click(button);

    expect(mockNavigate).toHaveBeenCalledWith("/register");
});

test("profile button redircts to profile page when logged in", async () => {
    render(<Left isLoggedIn={true}/>, {wrapper: MemoryRouter});
    const user = userEvent.setup();
    const button = screen.getByTitle("Profile");
    await user.click(button);

    expect(mockNavigate).toHaveBeenCalledWith("/profile");
});


// Right tests
test("renders the Right component (logged out)", () => {
    render(<Right isLoggedIn={false} />, {wrapper: MemoryRouter});
    
    expect(screen.getByText("Log In"));
    expect(screen.getByText("Sign Up"));
    expect(screen.getByText("Share"));

});

test("renders the Right component (logged in)", () => {
    render(<Right isLoggedIn={true}/>, {wrapper: MemoryRouter});

    expect(screen.getByText("Log Out"));
    expect(screen.getByText("Share"));
});

test("tests Right component's handleLogout fucntion", async () => {
    render(<Right isLoggedIn={true} setToast={setToast} />);
    fetchResult = true;
    const user = userEvent.setup();
    const button = screen.getByText("Log Out");
    await user.click(button);

    expect(mockNavigate).toHaveBeenCalledWith("/");

    fetchResult = false;
    await user.click(button);
    expect(setToast).toHaveBeenCalledWith({
        message: "Log Out Failed",
        type: "error",
    });
});

test("verifies Right component buttons work", async () => {
    render(<Right isLoggedIn={false} />);

    const user = userEvent.setup();
    let button = screen.getByText("Log In");
    await user.click(button);
    expect(mockNavigate).toHaveBeenCalledWith("/login");

    button = screen.getByText("Sign Up");
    await user.click(button);
    expect(mockNavigate).toHaveBeenCalledWith("/register");
});


// Middle tests
test("renders the Middle component", () => {
    render(<Middle messages={[]} chatHistory={[]} />);
    expect(screen.getByText("+")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Ask anything...")).toBeInTheDocument();
    expect(document.querySelector("button.submit")).toBeInTheDocument();
});

test("stream response disables submit", () => {
    render(<Middle messages={[]} isStreaming={true}/>);
    expect(document.querySelector("button.submit")).toBeDisabled();
});

test("messages are rendered in Middle component", () => {
    render(<Middle 
        messages={[{role: "user", content: "Hi"}, {role: "assistant", content: "Hello, how are you?"}]}       
    />);
    expect(screen.getByText("Hi"));
    expect(screen.getByText("Hello, how are you?"));
});