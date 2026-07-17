import "./Middle.css";

function Middle({messages, prompt, setPrompt, bottomRef, textareaRef, handleKeyDown, resizeTextarea, fileInputRef, handleFileUpload}) {
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

                <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: "none" }}
                    onChange={handleFileUpload}
                />
            </div>
        </div>
    );
}

export default Middle;