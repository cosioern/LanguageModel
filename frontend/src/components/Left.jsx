import "./Left.css";

function Left() {
    return (
        <div className="sideLeft">
            <h3>Powered by Bernie</h3>
            <button className="profile" title="Profile">
            <img src="/profile.png" alt="Profile" />
            </button>
        </div>
    );
}

export default Left;