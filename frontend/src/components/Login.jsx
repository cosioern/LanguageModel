import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import ToggleTheme from "./ToggleTheme.jsx";
import "./Login.css";

function Login() {
    const[username, setUsername] = useState("");
    const[password, setPassword] = useState("");
    const[status, setStatus] = useState(null);
    const navigate = useNavigate();
    const [showPassword, setShowPassword] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        const formData = new URLSearchParams();
        formData.append("username", username);
        formData.append("password", password);

        const res = await fetch(`http://localhost:8080/login`, {
            method: "POST",
            credentials: "include",
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: formData,
        });

        if (res.ok) {
            navigate("/chat");
        } else {
            setStatus("Wrong username or password");
        }
    }

    return (
        <div className="login-page">
            <form className="login-card" onSubmit={handleSubmit}>
                <h1>Welcome Back</h1>
                <p>Login to continue</p>

                <input
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    placeholder="Username"
                />
                <div className="eye">
                    <input
                        type={showPassword ? "text" : "password"}
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        placeholder="Password"
                    />
                    <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                    >
                        {showPassword ? <EyeOff size={18}/> : <Eye size={18} /> }
                    </button>
                </div>
                <button type="submit">Login</button>

                {status && <p className="status">{status}</p>}
                <button className="reset-link" onClick={() => navigate("/forgot-password")}>Forgot Your Password?</button>
            </form>
            <ToggleTheme/>
        </div>
);
}

export default Login;