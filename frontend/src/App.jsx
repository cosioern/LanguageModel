import { useEffect, useRef, useState } from "react";
import "./App.css";

import LandingPage from "./components/LandingPage";
import ChatPage from "./components/ChatPage";

function App() {
    const [started, setStarted] = useState(false);
    const [initialPrompt, setInitialPrompt] = useState("");

    function handleInitialPrompt(text) {
        setInitialPrompt(text);
        setStarted(true);
    }

    return started
        ? <ChatPage initialPrompt={initialPrompt}/>
        : <LandingPage onSubmit={handleInitialPrompt} />;
}

export default App;