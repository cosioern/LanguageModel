import { useEffect, useRef, useState } from "react";
import "./App.css";

function App() {
  const [messages, setMessages] = useState([]);
  const [prompt, setPrompt] = useState("");
  const bottomRef = useRef(null);
  const textareaRef = useRef(null);
  const baseHeightRef = useRef(0);

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

  function resizeTextarea(el) {
    el.style.height = "auto";
    const needed = el.scrollHeight;
    // only grow if content actually needs more than one line's worth of space
    el.style.height = `${Math.max(needed, baseHeightRef.current)}px`;
  }

  function sendPrompt() {
    if (!prompt.trim()) return;

    const userMessage = prompt;
    setPrompt("");
    if (textareaRef.current) {
      textareaRef.current.style.height = `${baseHeightRef.current}px`;
    }

    setMessages(prev => [...prev, { role: "user", text: userMessage }]);

    fetch(`http://localhost:8080/llm/generate?prompt=${encodeURIComponent(userMessage)}`)
      .then(res => res.text())
      .then(data => {
        setMessages(prev => [...prev, { role: "assistant", text: data }]);
      });
  }

  function handleKeyDown(e) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendPrompt();
    }
  }

  return (
    <div className="container">
      <div className="output">
        {messages.map((m, i) => (
          <div key={i} className={`message ${m.role}`}>
            {m.text}
          </div>
        ))}
        <div id="bottom" ref={bottomRef} style={{ height: "100px" }} />
      </div>

      <textarea
        ref={textareaRef}
        className="input"
        value={prompt}
        onChange={(e) => {
          setPrompt(e.target.value);
          resizeTextarea(e.target);
        }}
        onKeyDown={handleKeyDown}
        placeholder="Ask anything..."
        rows={1}
      />
    </div>
  );
}

export default App;