import { useEffect, useRef, useState } from "react"
import { Routes, Route } from "react-router-dom"
import { useNavigate } from "react-router-dom"
import "./App.css";
import LandingPage from "./components/LandingPage"
import ChatPage from "./components/ChatPage"
import Register from "./components/Register"
import Login from "./components/Login"
import Verify from "./components/Verify"

function App() {
    const [initialPrompt, setInitialPrompt] = useState("");
    const [chatHistory, setChatHistory] = useState([]);
    const navigate = useNavigate();

    // 
    function handleInitialPrompt(text) {
        setInitialPrompt(text);
        navigate("/chat");
        // setStarted(true);
    }
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

   return ( 
        <Routes>
            <Route path="/" element={<LandingPage onSubmit={handleInitialPrompt}/>} />
            <Route path="/chat" element={<ChatPage initialPrompt={initialPrompt} chatHistory={chatHistory} />} />
            <Route path="/register" element={<Register />}/>
            <Route path="/login" element={<Login />} />
            <Route path="/verify" element={<Verify />} />
        </Routes>
    );
}

export default App;