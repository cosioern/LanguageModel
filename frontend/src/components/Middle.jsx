import "./Middle.css";

function Middle({messages, prompt, setPrompt, bottomRef, textareaRef, handleKeyDown, resizeTextarea}) {
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

export default Middle;