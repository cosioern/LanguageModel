import { useState } from "react"
import ToggleTheme from "./ToggleTheme";
import "./Register.css"

function Register() {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [name, setName] = useState("");
    const [birthDate, setBirthDate] = useState("");

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
        formData.append("name", name);
        formData.append("birthDate", birthDate);

        const res = await fetch(`http://localhost:8080/register`, {
            method: "POST",
            // credentials: "include", no auto-login until verified
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: formData
        });

        if (res.ok) {
            setStatus("Check your email to verify your account and close this tab.");
        } else {
            setStatus("Registration failed. Please try again.");
        }
    }


    return (
        <div className="register-page">
            <form className="register-card" onSubmit={handleSubmit}>
                <h1>Wanna chat?</h1>
                <p>Create an Account</p>

                <input 
                    value={username} 
                    onChange={e => setUsername(e.target.value)} 
                    placeholder="Username">

                </input>

                <input
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="Name">

                </input>

                <input
                    type="email"
                    value={email} 
                    onChange={e => setEmail(e.target.value)} 
                    placeholder="Email">
                </input>

                <input
                    type="date"
                    name="birthDate"
                    value={birthDate}
                    onChange={e => setBirthDate(e.target.value)}
                    autoComplete="bday"
                    // placeholder="birthDate"
                    title="Date of Birth"
                    >

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
            <ToggleTheme/>
        </div>
    );
}

export default Register;