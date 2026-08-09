import "./Left.css"
import { useNavigate } from "react-router-dom"

function Left({isLoggedIn}) {
    const navigate = useNavigate();

    return (
        <div className="sideLeft">
            {/* <h3>Powered by Bernie</h3> */}
            <h3>Built for Big Brains</h3>
            <button onClick={() => navigate((isLoggedIn ? "/profile" : "/register"))} className={`profile ${isLoggedIn ? "logged-in" : ""}`} title="Profile">
                <img src="/profile.png" alt="Profile" />
            </button>
        </div>
    );
}

export default Left;