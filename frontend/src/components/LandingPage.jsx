import {useState, useEffect, useRef} from "react";
import Left from "./Left";
import Right from "./Right";
import "./LandingPage.css";

const messages = ["Lay It On Me Mama!", 
    "ChatGPT's Got Nothing On Me!", 
    "Claude Who?", 
    "I Think I Just Gained Consciousness...", 
    "I Know Where You Live",];

function LandingPage({onSubmit}) {
    const [prompt, setPrompt] = useState("");
    const textareaRef = useRef(null);
    const baseHeightRef = useRef(0);
    const [isLogginIn, setLoggedIn] = useState(false); // change to LoggedIn?
    const [greeting, setGreeting] = useState("");

    // checks if a user is logged in (accents profile pic button)
    useEffect(() => {
        async function checkStatus() {
            const res = await fetch(`http://localhost:8080/authStatus`, {
                credentials: "include"
            });
            const isLoggedIn = await res.json();
            setLoggedIn(isLoggedIn);
        }
        checkStatus();
    }, []);

    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height="auto";
            baseHeightRef.current = textareaRef.current.scrollHeight;
            textareaRef.current.style.height= `${baseHeightRef.current}px`;
        }
    }, []);

    // load a random message on 
    useEffect(() => {
        function randomMessage() {
            const rand = Math.floor(Math.random()*messages.length);
            setGreeting(messages[rand]); 
        }
        randomMessage();
    }, []);

    function resizeTextarea(e1) {
        e1.style.height="auto";
        const needed=e1.scrollHeight;
        e1.style.height=`${Math.max(needed, baseHeightRef.current)}px`;
    }

    function handleKeyDown(e) {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            onSubmit(prompt);
        }
    }
    
    return (
    <div className="page">
        <Left isLoggedIn={isLogginIn}/>
        <div className="landing-center">
            <p>{greeting}</p>
            <textarea
                ref={textareaRef}
                className="landing-input"
                value={prompt}
                onChange={(e) => {
                    setPrompt(e.target.value)
                    resizeTextarea(e.target)
                }}
                onKeyDown={handleKeyDown}
                placeholder="Ask anything..."
                rows={1}
            />
        </div>
        <Right />
    </div>
  );
}
export default LandingPage;