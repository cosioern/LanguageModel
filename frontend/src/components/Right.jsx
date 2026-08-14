import "./Right.css";
import { useNavigate } from "react-router-dom";
import ToggleTheme from "./ToggleTheme.jsx";

function Right({isLoggedIn}) {
    const navigate = useNavigate();

    async function handleLogout() {

        const res = await fetch(`http://localhost:8080/logout`, {
            method: "POST",
            credentials: "include",
        });

        if (res.ok) {
            navigate("/");
        } else {
            // toast error
        }

    }

    return (
        <div className="sideRight">
            {/*Button classifier*/}
            <div className="top-page">
                <div className="button-holder">
                    {isLoggedIn ? (
                        <><button onClick={handleLogout} className="logout">LogOut</button></>
                    ) : (
                        <><button onClick={() => navigate("/login")} className="login-button">Log In</button>
                        <button onClick={() => navigate("/register")} className="signin-button">Sign Up</button></>
                    )}
                </div>
                    <button className="share-button" onClick={() => window.open("https://www.instagram.com/ecosio3/")}>Share</button>
            </div>
                <ToggleTheme/>
        </div>
    );
}

export default Right;