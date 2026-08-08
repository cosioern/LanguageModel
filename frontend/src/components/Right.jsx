import "./Right.css";
import { useNavigate } from "react-router-dom";
import { useContext } from "react";
import { ThemeContext } from "../App.jsx";
import { Moon, Sun} from "lucide-react";

function Right() {
    const navigate = useNavigate();
    const { theme, toggleTheme } = useContext(ThemeContext);

    return (
        <div className="sideRight">
            {/*Button classifier*/}
            <div className="top-page">
                <div className="button-holder">
                    <button onClick={() => navigate("/login")} className="login-button">Log In</button>
                    <button onClick={() => navigate("/register")} className="signin-button">Sign Up</button>
                </div>
                    <button className="share-button" onClick={() => window.open("https://www.instagram.com/ecosio3/")}>Share</button>
            </div>
                <button className={`theme-button ${theme === "dark" ? "theme-dark" : "theme-light"}`} onClick={() => toggleTheme()}>
                    {theme === "dark" ? <Sun size={20} /> : <Moon size={20} />}
                </button>
        </div>
    );
}

export default Right;