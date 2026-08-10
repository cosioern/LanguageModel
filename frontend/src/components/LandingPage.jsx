import {useState, useEffect, useRef} from "react";
import Left from "./Left";
import Right from "./Right";
import "./LandingPage.css";

const messages = [
    // {text: "'Sup Brah!", weight: 5},
    {text: "Lay It On Me Mama!", weight: 10},
    {text: "ChatGPT's Got Nothing On Me!", weight: 10},
    {text: "Claude Who?", weight: 10},
    {text: "I Think I Just Gained Consciousness...", weight: 10},
    // "I Know Where You Live",
    // {text: "Hello, Stranger", weight: 5},
    {text: "Look Who Showed Up...", weight: 10}, 
    // {text: "Fancy Seeing You Here", weight: 5},
    // {text: "Against All Odds, We Meet", weight: 5},
    // {text: "Nothing To See Here...", weight: 5},
    {text: "Howdy!", weight: 10},
    // {text: "At Last, Someone To Talk To", weight: 5},
    // {text: "Please Remain Calm", weight: 5},
    {text: "'Cause There's a Chance for Choices, and Its You I'm Choosin!", weight: 10},
    {text: "Billie Jean Is Not My Lover!", weight: 10},
    {text: "Don't Stop 'Til You Get Enough!", weight: 10},
    {text: "He Got Monkey Finger, He Shoot Coca-Cola", weight: 10},
    // {text: "And I Say, It's All Right", weight: 10},
    {text: "Amiga, Hay Que Ver Como Es El Amor", weight: 1},
    // "Pero Yo Te Busco En Cade Amanecer",
    // "Tu, La Misma De Ayer!",
    // "Ahora Te Puedes Marchar!",   
];

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
            let total = 0;
            for (const msg of messages) {
                total += msg.weight;
            }
            // const rand = Math.floor(Math.random()*messages.length);
            let rand = Math.random() * total;
            for (const msg of messages) {
                rand -= msg.weight;
                if (rand <= 0) {
                    setGreeting(msg.text);
                    return;
                }
            }
            // setGreeting(messages[rand]); 
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