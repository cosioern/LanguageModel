import { useEffect, useState } from "react";
import "./App.css";

function App() {
  const [msg, setMsg] = useState("");
  const [prompt, setPrompt] = useState("");

  function sendPrompt() {
    if (!prompt.trim()) return;

    fetch(`http://localhost:8080/llm/generate?prompt=${encodeURIComponent(prompt)}`)
    .then(res => res.text())
    .then(data => setMsg(data))

    setPrompt("");
  }

  function handleKeyDown(e) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendPrompt();
    }
  }

  return (
    <div className="container">
      {/* OUTPUT */}
      <div className="output">
        {msg}
      </div>

      {/* INPUT */}
      <textarea
        className="input"
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Ask anything..."
        rows={3}
      />
    </div>
  );

}

export default App;