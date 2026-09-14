import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom/vitest";
import { vi, beforeEach, test, expect } from "vitest";
import Profile from "../src/components/Profile";
import { TruckElectric } from "lucide-react";

let profileResult = false;
let deleteResult = false;

beforeEach(() => {
    vi.stubGlobal(
        "fetch",
        vi.fn((url) => {
            if (url.includes("/api/profile")) {
                if (profileResult) {
                    return Promise.resolve({
                        status: 200,
                        json: () => Promise.resolve({username:"testUser", email: "test@email.com", birthday: "2000-01-01", name: "testName"})
                    });
                }
                return Promise.resolve({status: 401});
            }
            else if (url.includes("/api/documents")) {
                if (profileResult) {
                    return Promise.resolve({
                        status: 200,
                        json: () => Promise.resolve([
                            {documentID: 1, fileName: "File 1"},
                            {documentID: 2, fileName: "File 2"},
                            {documentID: 3, fileName: "File 3"},
                        ]),
                    });
                }
                return Promise.resolve({status: 401});
            }
            else if (url.includes("/api/deleteDocument")) {
                if (deleteResult) {
                    return Promise.resolve({ok: true, status: 200});
                }
                return Promise.resolve({status: 401, json: () => Promise.resolve([])});
            }
            return Promise.resolve({});
        })
    );
});


test("renders Profile page", () => {
    render(<Profile />, {wrapper: MemoryRouter});

    expect(screen.getByText("'s Profile")).toBeInTheDocument();
    expect(screen.getByText("Username")).toBeInTheDocument();
    expect(document.querySelector("span.value")).toBeInTheDocument();
    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.getByText("Birthday")).toBeInTheDocument();
    expect(screen.getByText("Change Password")).toBeInTheDocument();
    expect(screen.getByText("Uploaded Documents:")).toBeInTheDocument();
    expect(screen.getByText("No documents uploded yet")).toBeInTheDocument();
});

test("useEffect sets toast on failure repsonse from /api/profile", async () => {
    profileResult = false;
    render(<Profile />, {wrapper: MemoryRouter});
    
    expect(fetch).toHaveBeenCalledWith("/api/profile", {credentials: "include", headers:{"Content-type":"application/x-www-form-urlencoded"}});
    expect(await screen.findByText("Unauthorized"));
})


test("useEffect populates profile details", async () => {
    profileResult = true;
    render(<Profile />, {wrapper: MemoryRouter});
    
    expect(fetch).toHaveBeenCalledWith("/api/profile", {credentials: "include", headers:{"Content-type":"application/x-www-form-urlencoded"}});
    expect(await screen.findByText("testUser")).toBeInTheDocument();
    expect(await screen.findByText("test@email.com")).toBeInTheDocument();
    expect(await screen.findByText("2000-01-01")).toBeInTheDocument();
    expect(await screen.findByText("testName's Profile")).toBeInTheDocument();
});

test("useEffect sets toast when failing to populate document list", async () => {
    profileResult = false;
    render(<Profile />, {wrapper: MemoryRouter});

    expect(fetch).toHaveBeenCalledWith("/api/documents", {method: "GET", credentials: "include", headers:{"Content-type":"application/x-www-form-urlencoded"}});
    expect(await screen.findByText("Unauthorized"));
});

test("useEffect populates document list", async () => {
    profileResult = true;
    render(<Profile />, {wrapper: MemoryRouter});

    expect(fetch).toHaveBeenCalledWith("/api/documents", {method: "GET", credentials: "include", headers:{"Content-type":"application/x-www-form-urlencoded"}});
    expect(await screen.findByText("File 1")).toBeInTheDocument();
    expect(screen.getByText("File 2")).toBeInTheDocument();
    expect(screen.getByText('File 3')).toBeInTheDocument();
});

test("deleteDocument function sets toast on failure", async () => {
    profileResult = true;
    deleteResult = false;
    const { container } = render(<Profile />, {wrapper: MemoryRouter});
    const user = userEvent.setup();

    await screen.findByText("File 1");
    const button = container.querySelector(".trash");
    await user.click(button);

    expect(fetch).toHaveBeenCalledWith("/api/deleteDocument?documentID=1", {method: "DELETE", credentials: "include", headers:{"Content-type":"application/x-www-form-urlencoded"}});
    expect(await screen.findByText("Unauthorized"));
});

test("deleteDocument successfully removes document", async () => {
    profileResult = true;
    deleteResult = true;

    const { container } = render(<Profile />, {wrapper: MemoryRouter});
    const user = userEvent.setup();

    await screen.findByText("File 1");
    const button = container.querySelector(".trash");
    await user.click(button);

    expect(fetch).toHaveBeenCalledWith("/api/deleteDocument?documentID=1", {method: "DELETE", credentials: "include", headers: {"Content-type":"application/x-www-form-urlencoded"}});
    expect(await screen.findByText("Deleted \"File 1\""));
})