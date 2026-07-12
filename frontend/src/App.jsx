import { useEffect, useRef, useState } from "react";
import "./App.css";

import LandingPage from "./components/LandingPage";
import ChatPage from "./components/ChatPage";

function App() {
    const [started, setStarted] = useState(false);
    const [initialPrompt, setInitialPrompt] = useState("");
    const [chatHistory, setChatHistory] = useState([]);

    function handleInitialPrompt(text) {
        setInitialPrompt(text);
        setStarted(true);
    }

    useEffect(() => {
        fetch(`http://localhost:8080/load`, {credentials:"include"})
            .then(res => res.json())
            .then(data => setChatHistory(data))
    }, []);

    // if cookie found send chat history to ChatPage
    if (chatHistory.length > 0)
        return <ChatPage chatHistory={chatHistory}/>;

    return started
        ? <ChatPage initialPrompt={initialPrompt}/>
        : <LandingPage onSubmit={handleInitialPrompt} />;
}

export default App;