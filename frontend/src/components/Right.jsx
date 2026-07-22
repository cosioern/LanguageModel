import "./Right.css"
import { useNavigate } from "react-router-dom"

function Right() {
    const navigate = useNavigate();
    return (
        <div className="sideRight">
            {/*Button classifier*/}
            <div className="button-holder">
                <button onClick={() => navigate("/login")} className="login-button">Log In</button>
                <button onClick={() => navigate("/register")} className="signin-button">Sign Up</button>
            </div>
                <button className="share-button" onClick={() => window.open("https://www.instagram.com/ecosio3/")}>Share</button>
        </div>
    );
}

export default Right;