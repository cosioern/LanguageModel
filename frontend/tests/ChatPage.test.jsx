import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom/vitest";
import { vi, beforeEach, afterEach, test, expect} from "vitest";
import ChatPage from "../src/components/ChatPage";
 

vi.mock("../src/components/Left", () => ({
    default: ({isLoggedIn}) => (
        <div data-testid="left">
            {isLoggedIn ? "Left Logged In" : "Left Logged Out"}
        </div>
    ),
}));

vi.mock("../src/components/Right", () => ({
    default: ({isLoggedIn}) => (
        <div data-testid="right">
            {isLoggedIn ? "Right Logged In" : "Right Logged Out"}
        </div>
    )
}));

beforeEach(() => {
    vi.stubGlobal(
        "fetch",
        vi.fn((url) => { // false value is used by setLoggedIn
            if (url.includes("/authStatus")) {
                return Promise.resolve({ json: () => Promise.resolve(false) });
            }
            else if (url.includes("/load")) {
                return Promise.resolve({ json: () => Promise.resolve([]) });
            }
            else if (url.includes("/generate")) {
                return Promise.resolve({
                        body: {
                            getReader: () => ({
                                read: vi.fn().mockResolvedValue({ done: true, value: undefined})
                        })
                    }
                });
            }
            else if (url.includes("/embedDocument")) {
                return Promise.resolve({
                    ok: true,
                    status: 200,
                });
            }
            return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
        })
    );
});

// test that the correct useEffects run
test("renders the chat page", async () => {
    // ChatPage is normally accessed through a user submitting a prompt (initialPrompt) from LandingPage
    render(<ChatPage initialPrompt={"Nonempty prompt"} chatHistory={[]} />);

    expect(screen.getByTestId("left")).toBeInTheDocument;
    expect(screen.getByTestId("right")).toBeInTheDocument;
    expect(screen.getByPlaceholderText("Ask anything...")).toBeInTheDocument;

    expect(fetch).toHaveBeenCalledTimes(2);
    expect(fetch).toHaveBeenCalledWith("/api/authStatus", {credentials: "include"});
    expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/generate?prompt="), 
        {credentials: "include", method: "POST"}
    );
    expect(fetch).not.toHaveBeenCalledWith("/load");
});

test("updates the prompt when user types", async () => {
    const user = userEvent.setup();
    render(<ChatPage initialPrompt={""} chatHistory={[]} />);
    const textarea = screen.getByPlaceholderText("Ask anything...");
    await user.type(textarea, "What sort of AI assistant are you?");
    expect(textarea).toHaveValue("What sort of AI assistant are you?");
});

test("submits the prompt when the user hits Enter", async () => {
    const user = userEvent.setup();
    render(<ChatPage initialPrompt={""} chatHistory={[]}/>);

    const textarea = screen.getByPlaceholderText("Ask anything...");
    await user.type(textarea, "Generic question about real estate hee hee");
    await user.keyboard("{Enter}");
    
    expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/generate"),
        expect.objectContaining({credentials: "include"}, {method: "POST"}),
    );
    expect(fetch).toHaveBeenCalledTimes(3);
});

test("does nothing when the user hits Shift+Enter", async () => {
    const user = userEvent.setup();
    render(<ChatPage initialPrompt={""} chatHistory={[]}/>);

    const textarea = screen.getByPlaceholderText("Ask anything...");
    await user.type(textarea, "Generic question about real estate");
    await user.keyboard("{Shift>}{Enter}{/Shift}");

    expect(fetch).not.toHaveBeenCalledWith("/generate");
    expect(fetch).toHaveBeenCalledWith("/api/authStatus", {credentials: "include"});
    expect(fetch).toHaveBeenCalledWith("/api/load", {credentials: "include"});
    expect(fetch).toHaveBeenCalledTimes(2);
});

// isLoggedIn is set to false in beforeEach
test("passes authentication staus to Left and Right compoents", async () => {
    render(<ChatPage initialPrompt={""} chatHistory={[]}/>);
    expect(await screen.findByText("Left Logged Out")).toBeInTheDocument();
    expect(await screen.findByText("Right Logged Out")).toBeInTheDocument();
});

test("calls load on refresh", async () => {
    render(<ChatPage initialPrompt={""} chatHistory={[]}/>);
    expect(fetch).toHaveBeenCalledWith("/api/authStatus", {credentials: "include"});
    expect(fetch).toHaveBeenCalledWith("/api/load", {credentials: "include"});
    expect(fetch).not.toHaveBeenCalledWith("/api/embedDocument", {method: "POST", credentials: "include"});
    expect(fetch).toHaveBeenCalledTimes(2);
});

//test embedDocument
test("embed document feature", async () => {
    const {container } = render(<ChatPage initialPrompt={""} chatHistory={[]}/>);
    const user = userEvent.setup();
    const fileinput = container.querySelector('input[type="file"]');
    const file = new File(["content"], "test.txt", {type: "text/plain"});

    await user.upload(fileinput, file);
    expect(await screen.findByText("Uploaded \"test.txt\"")).toBeInTheDocument();
});