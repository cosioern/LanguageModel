import {useState} from "react";
import { useNavigate } from "react-router-dom";
import ToggleTheme from "./ToggleTheme.jsx";
import "./Login.css";

function Login() {
    const[username, setUsername] = useState("");
    const[password, setPassword] = useState("");
    const[status, setStatus] = useState(null);
    const navigate = useNavigate();    

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
            setStatus("Login Failed. Bad Credentials");
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

                <input
                    type="password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    placeholder="Password"
                />

                <button type="submit">Login</button>

                {status && <p className="status">{status}</p>}
            </form>
            <ToggleTheme/>
        </div>
);
}

export default Login;