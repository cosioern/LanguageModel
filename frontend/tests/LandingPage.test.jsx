import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom/vitest";
import { vi, beforeEach, afterEach, test, expect } from "vitest";
import LandingPage from "../src/components/LandingPage";
import { messages } from "../src/components/LandingPage";


vi.mock("../src/components/Left", () => ({
    default: ({ isLoggedIn }) => (
        <div data-testid="left">
            {isLoggedIn ? "Left Logged In" : "Right Logged Out"}
        </div>
    ),
}));

vi.mock("../src/components/Right", () => ({
    default: ({ isLoggedIn }) => (
        <div data-testid="right">
            {isLoggedIn ? "Right Logged In" : "Right Logged Out"}
        </div>
    ),
}));


// vi.mock("../src/components/Right", () => ({
//     default: () => <div data-testid="right">Right</div>,
// }));

beforeEach(() => {
    vi.stubGlobal(
        "fetch",
        vi.fn((url) => {
            return Promise.resolve({json: () => Promise.resolve(true)});
        })
    );

    vi.spyOn(Math, "random").mockReturnValue(0);
});

afterEach(() => {
    vi.restoreAllMocks();
});

test("renders the landing page", () => {
    render(<LandingPage onSubmit={vi.fn()} />);

    expect(screen.getByPlaceholderText("Ask anything...")).toBeInTheDocument();
    expect(screen.getByTestId("left")).toBeInTheDocument();
    expect(screen.getByTestId("right")).toBeInTheDocument();
});

test("updates the prompt when the user types", async () => {
    const user = userEvent.setup();
    render(<LandingPage onSubmit={vi.fn()} />);
    const textarea = screen.getByPlaceholderText("Ask anything...");
    await user.type(textarea, "Hello world");
    expect(textarea).toHaveValue("Hello world");
});

test("submits the prompt when Enter is pressed", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(<LandingPage onSubmit={onSubmit} />);

    const textarea = screen.getByPlaceholderText("Ask anything...");

    await user.type(textarea, "Hello world");
    await user.keyboard("{Enter}");

    expect(onSubmit).toHaveBeenCalledWith("Hello world");
    expect(onSubmit).toHaveBeenCalledTimes(1);
});

test("does not submit when Shift+Enter is pressed", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();

    render(<LandingPage onSubmit={onSubmit} />);

    const textarea = screen.getByPlaceholderText("Ask anything...");

    await user.type(textarea, "Hello world");
    await user.keyboard("{Shift>}{Enter}{/Shift}");

    expect(onSubmit).not.toHaveBeenCalled();
});

test("passes the authentication status to Left & Right", async () => {
    render(<LandingPage onSubmit={vi.fn()} />);
    expect(await screen.findByText("Left Logged In")).toBeInTheDocument();
    expect(await screen.findByText("Right Logged In")).toBeInTheDocument();
});

test("displays a greeting", () => {
    render(<LandingPage onSubmit={vi.fn()} />);
    expect(screen.getByText(messages[0].text)).toBeInTheDocument();
});