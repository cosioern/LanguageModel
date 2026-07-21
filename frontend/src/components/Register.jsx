import { useState } from "react"
import "./Register.css"

function Register() {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [status, setStatus] = useState(null);
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    async function handleSubmit(e) {
        e.preventDefault();
        if (!emailRegex.test(email)) {
            setStatus("Invalid Email");
            return; 
        }
        const formData = new URLSearchParams();
        formData.append("username", username);
        formData.append("email", email);
        formData.append("password", password);

        const res = await fetch(`http://localhost:8080/register`, {
            method: "POST",
            // credentials: "include", no auto-login until verified
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: formData
        });

        if (res.ok) {
            setStatus("Check your email to verify your account");
        } else {
            setStatus("Registration failed. Please try again.");
        }
    }


    return (
        <div className="register-page">
            <form className="register-card" onSubmit={handleSubmit}>
                <h1>First Time Here?</h1>
                <p>Create an Account</p>

                <input 
                    value={username} 
                    onChange={e => setUsername(e.target.value)} 
                    placeholder="Username">

                </input>
                
                <input
                    type="email"
                    value={email} 
                    onChange={e => setEmail(e.target.value)} 
                    placeholder="Email">
                </input>

                <input
                    type="password"
                    value={password} 
                    onChange={e => setPassword(e.target.value)} 
                    placeholder="Password">
                </input>

                <button type="submit">Register</button>

                {status && <p>{status}</p>}
            </form>
        </div>
    );
}

export default Register;