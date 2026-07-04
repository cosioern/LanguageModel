import { useEffect, useState } from "react";

function App() {
  const [msg, setMsg] = useState("");
  const [prompt, setPrompt] = useState("");

  function sendPrompt() {
    fetch(`http://localhost:8080/llm/generate?prompt=${encodeURIComponent(prompt)}`)
    .then(res => res.text())
    .then(data => setMsg(data))
  }

  return (
    <div>
      <input
        value={prompt}
        onChange={(e) => setPrompt(e.target.value)}
        placeholder="Enter Prompt"
      />

      <button onClick={sendPrompt}>
        Send
      </button>
      <h1>{msg}</h1>
    </div>
  );
}
export default App;