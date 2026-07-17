import { useEffect, useRef, useState } from "react";
import Left from "./Left";
import Right from "./Right";
import Middle from "./Middle";


function ChatPage({initialPrompt, chatHistory}) {
    const [messages, setMessages] = useState(chatHistory || []);
    const [prompt, setPrompt] = useState("");
    const bottomRef = useRef(null);
    const textareaRef = useRef(null);
    const baseHeightRef = useRef(0);
    const hasSentInitial = useRef(false);
    const fileInputRef = useRef(null);
    const [toast, setToast] = useState(null);

    // initial  api call, seamless transition between LandingPage and ChatPage
    useEffect(() => {
        if (hasSentInitial.current) return;
        hasSentInitial.current = true;

        if (initialPrompt && initialPrompt.trim()) {
            setMessages(prev => [...prev, {role: "user", content: initialPrompt}]);

            fetch(`http://localhost:8080/generate?prompt=${encodeURI(initialPrompt)}`, {
                credentials:"include", 
                method:"POST"
            })
                .then(res => res.text())
                .then(data => {
                    setMessages(prev => [...prev, {role:"assistant", content:data}]);
                });
        }
    }, []);
    
    
    // scrolls down the page as new messages are added, but only when page is filled
    useEffect(() => {
        const bottomEl = bottomRef.current;
        if (!bottomEl) return;

        const rect = bottomEl.getBoundingClientRect();
        const isBelowFold = rect.bottom > window.innerHeight;

        if (isBelowFold) {
            bottomEl.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages]);

    // capture the natural single-line height once, on mount
    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = "auto";
            baseHeightRef.current = textareaRef.current.scrollHeight;
            textareaRef.current.style.height = `${baseHeightRef.current}px`;
        }
    }, []);

    useEffect(() => {
        if (!toast) return;

        const timer = setTimeout(() => {
            setToast(null);
        }, 3000);
        return () => clearTimeout(timer);
    }, [toast])

    // grows input bar
    function resizeTextarea(el) {
        el.style.height = "auto";
        const needed = el.scrollHeight;
        // only grow if content actually needs more than one line's worth of space
        el.style.height = `${Math.max(needed, baseHeightRef.current)}px`;
    }

    function handleFileUpload(e) {
        const file = e.target.files[0];
        if (!file) return;

        const formData = new FormData();
        formData.append("document", file);

        fetch(`http://localhost:8080/embedDocument`, {
            method: "POST",
            credentials: "include",
            body: formData
        })
        .then(res => {
            if (res.ok) {
                setToast({message: "Document Uploaded Successfully", type: "success"})
            } else {
                setToast({mesage: "Upload Failed", type: "Error"})
            }
        })
        .catch(() => {
            setToast({message: "Upload failed", type: "Error"});
        });

        e.target.value = "";
    }

    // send prompts, return messages
    function sendPrompt() {
        if (!prompt.trim()) return;

        const userMessage = prompt;
        setPrompt("");
        if (textareaRef.current) {
            textareaRef.current.style.height = `${baseHeightRef.current}px`;
        }

        setMessages(prev => [...prev, { role: "user", content: userMessage }]);

        fetch(`http://localhost:8080/generate?prompt=${encodeURIComponent(userMessage)}`, {
            credentials:"include",
            method:"POST"
        })
            .then(res => res.text())
            .then(data => {
            setMessages(prev => [...prev, { role: "assistant", content: data }]);
            });
        }

    function handleKeyDown(e) {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            sendPrompt();
        }
    }

    // assembled html page
    return (
        <div className="page">
            <Left />
            <Middle 
                messages={messages}
                prompt={prompt}
                setPrompt={setPrompt}
                bottomRef={bottomRef}
                textareaRef={textareaRef}
                handleKeyDown={handleKeyDown}
                resizeTextarea={resizeTextarea}

                fileInputRef={fileInputRef}
                handleFileUpload={handleFileUpload}
            />
            <Right />

            {toast && (
                <div className={`toast toast-${toast.type}`}>
                    {toast.message}
                </div>
            )}
        </div>
  );
}

export default ChatPage;