import { useContext } from "react";
import { ThemeContext } from "../App";
import { Moon, Sun } from "lucide-react";
import "./ToggleTheme.css";

function ToggleTheme() {

    const {theme, toggleTheme } = useContext(ThemeContext);

    return (
        <button className={`theme-button ${theme === "dark" ? "theme-dark" : "theme-light"}`} onClick={() => toggleTheme()}>
                    {theme === "dark" ? <Sun size={20} /> : <Moon size={20} />}
        </button>
    );
}

export default ToggleTheme;