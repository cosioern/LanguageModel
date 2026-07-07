import { useEffect, useState } from "react";
import "./App.css";

function App() {
const [messages, setMessages] = useState([]);
  const [prompt, setPrompt] = useState("");

  function sendPrompt() {
  if (!prompt.trim()) return;

  const userMessage = prompt;
  setPrompt("");

  setMessages(prev => [
    ...prev,
    { role: "user", text: userMessage }
  ]);

  fetch(`http://localhost:8080/llm/generate?prompt=${encodeURIComponent(userMessage)}`)
    .then(res => res.text())
    .then(data => {
      setMessages(prev => [
        ...prev,
        { role: "assistant", text: data }
      ]);
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
      {/* OUTPUT */}
    <div className="output">
  {messages.map((m, i) => (
    <div
      key={i}
      style={{
        margin: "8px 0",
        padding: "8px",
        borderRadius: "8px",
        background: m.role === "user" ? "#dbeafe" : "#e5e7eb",
        textAlign: m.role === "user" ? "right" : "left"
      }}
    >
      {m.role}: {m.text}
    </div>
  ))}
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