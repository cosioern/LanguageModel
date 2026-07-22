import {useState, useEffect, useRef} from "react";
import Left from "./Left";
import Right from "./Right";
import "./LandingPage.css";

function LandingPage({onSubmit}) {
    const [prompt, setPrompt] = useState("");
    const textareaRef = useRef(null);
    const baseHeightRef = useRef(0);
    const [isLogginIn, setLoggedIn] = useState(false);


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
            <p>Lay It On Me Mama!</p>
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