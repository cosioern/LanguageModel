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
    const [isStreaming, setIsStreaming] = useState(false);

    // initial  api call, seamless transition between LandingPage and ChatPage
    useEffect(() => {
        async function init() {
            if (isStreaming) return;
            if (hasSentInitial.current) return;
            hasSentInitial.current = true;

            if (initialPrompt && initialPrompt.trim()) {
                setMessages(prev => [...prev, {role: "user", content: initialPrompt}]);

                let assistantMessage = "";
                setIsStreaming(true);
                const res = await fetch(`http://localhost:8080/generate?prompt=${encodeURI(initialPrompt)}`, {
                    credentials:"include", 
                    method:"POST"
                });
                const reader = res.body.getReader();
                const decoder = new TextDecoder;
                let tokens = await reader.read();
                setMessages(prev => [...prev, {role: "assistant", content: ""}]);
                
                // stream tokens, append/update only the latest assistant message
                while(!tokens.done) {
                    assistantMessage += decoder.decode(tokens.value);
                    setMessages(prev => {
                        const updated = [...prev];
                        updated[updated.length-1] = {role: "assistant", content: assistantMessage};
                        return updated;
                    });
                    tokens = await reader.read();
                }
                setIsStreaming(false);
            }
        }
        init();
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

    // calls Spring endpoint /embeDocument
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
                setToast({message: `Uploaded \"${file.name}\"`, type: "success"})
            } else {
                setToast({message: "Upload Failed", type: "error"})
            }
        })
        .catch(() => {
            setToast({message: "Upload failed", type: "error"});
        });

        e.target.value = "";
    }

    // send prompts, return messages
    async function sendPrompt() {
        if (isStreaming) return;
        if (!prompt.trim()) return;

        const userMessage = prompt;
        setPrompt("");
        if (textareaRef.current) {
            textareaRef.current.style.height = `${baseHeightRef.current}px`;
        }

        setMessages(prev => [...prev, { role: "user", content: userMessage }]);

        let assistantMessage = "";
        setIsStreaming(true);
        const res = await fetch(`http://localhost:8080/generate?prompt=${encodeURIComponent(userMessage)}`, {
            credentials:"include",
            method:"POST"
        });
        const reader = res.body.getReader()
        const decoder = new TextDecoder;
        let tokens = await reader.read();
        setMessages(prev => [...prev, {role: "assistant", content: ""}]);
        // stream tokens, append/update only the latest assistant message
        while(!tokens.done) {
            assistantMessage += decoder.decode(tokens.value);
            setMessages(prev => {
                const updated = [...prev];
                updated[updated.length - 1] = {role: "assistant", content: assistantMessage}
                return updated;
            });
            tokens = await reader.read()
        }
        setIsStreaming(false);
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
                sendPrompt={sendPrompt}
                fileInputRef={fileInputRef}
                handleFileUpload={handleFileUpload}
                isStreaming={isStreaming}
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