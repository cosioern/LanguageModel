import { useEffect, useRef, useState } from "react"
import { Routes, Route } from "react-router-dom"
import { useNavigate } from "react-router-dom"
import { createContext } from "react"
import "./App.css";
import LandingPage from "./components/LandingPage"
import ChatPage from "./components/ChatPage"
import Register from "./components/Register"
import Login from "./components/Login"
import Verify from "./components/Verify"
import Profile from "./components/Profile"

export const ThemeContext = createContext("light");


function App() {
    const [initialPrompt, setInitialPrompt] = useState("");
    const [chatHistory, setChatHistory] = useState([]);
    const navigate = useNavigate();
    const [theme, setTheme] = useState(() => {
        const savedTheme = localStorage.getItem("theme");
        if (savedTheme) {
            return savedTheme;
        }
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    });

    function toggleTheme() {
        const newTheme = theme === "light" ? "dark" : "light";
        setTheme(newTheme);
        localStorage.setItem("theme", newTheme);
    }

    function handleInitialPrompt(text) {
        setInitialPrompt(text);
        navigate("/chat");
        // setStarted(true);
    }

    // set theme for the entire app
    useEffect(() => {
        document.body.className = theme;
    }, [theme]);

    // check for chat history
    useEffect(() => {
        fetch(`http://localhost:8080/load`, {credentials:"include"})
            .then(res => res.json())
            .then(data => setChatHistory(data))
    }, []);
    // if cookie found send chat history to ChatPage
    useEffect(() => {
        if (chatHistory.length > 0) {
            navigate("/chat");
        }
    }, [chatHistory]);

    // dark mode toggle


   return (
        <ThemeContext.Provider value={{theme, toggleTheme}}>
            <Routes>
                <Route path="/" element={<LandingPage onSubmit={handleInitialPrompt}/>} />
                <Route path="/chat" element={<ChatPage initialPrompt={initialPrompt} chatHistory={chatHistory} />} />
                <Route path="/register" element={<Register />}/>
                <Route path="/login" element={<Login />} />
                <Route path="/verify" element={<Verify />} />
                <Route path ="/profile" element={<Profile />} />
            </Routes>
        </ThemeContext.Provider>
    );
}

export default App;