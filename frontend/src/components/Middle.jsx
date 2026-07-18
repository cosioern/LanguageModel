import "./Middle.css";

function Middle({messages, prompt, setPrompt, bottomRef, textareaRef, handleKeyDown, 
    resizeTextarea, fileInputRef, handleFileUpload, sendPrompt, isStreaming}) {
    return (
        <div className="container">
            <div className="output">
                {messages.map((m, i) => (
                    <div key={i} className={`message ${m.role}`}>
                    {m.content}
                    </div>
                ))}
                <div id="bottom" ref={bottomRef} style={{ height: "100px" }} />
            </div>

            <div className="input-wrapper">
                <button
                    type="button"
                    className="upload-button"
                    onClick={() => fileInputRef.current.click()}
                >
                    +
                </button>
                <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: "none" }}
                    onChange={handleFileUpload}
                />
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
                    // disabled={isStreaming}
                />
                <button
                    type="button"
                    className="submit"
                    onClick={sendPrompt}
                    disabled={isStreaming}
>
                    <svg width="40" height="40" viewBox="0 0 25 23" fill="none">
                        <circle cx="12" cy="12" r="12" fill="currentColor" />
                        <path d="M12 16V8M12 8L8 12M12 8L16 12" stroke="white" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                </button>
            </div>
        </div>
    );
}

export default Middle;