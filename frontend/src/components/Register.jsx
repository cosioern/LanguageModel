import { useState } from "react"
import { Eye, EyeOff } from "lucide-react";
import ToggleTheme from "./ToggleTheme";
import "./Register.css"

function Register() {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [name, setName] = useState("");
    const [birthDate, setBirthDate] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    const [status, setStatus] = useState(null);
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    async function handleSubmit(e) {
        e.preventDefault();
        if (!username)  {setStatus("Enter a Username"); return;}
        if (!name)      {setStatus("Enter a Name"); return;}
        if (!email)     {setStatus("Enter an Email"); return;}
        if (!birthDate) {setStatus("Enter a Birhdate"); return;}
        if (!password)  {setStatus("Enter a Password"); return;}
        if (!emailRegex.test(email)) {setStatus("Invalid Email"); return;}
        if (new Date(birthDate) >= new Date()) {setStatus("Enter a Valid Birthdate"); return;}

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
            // set exception message
            setStatus(await res.text());
        }
    }


    return (
        <div className="register-page">
            <form className="register-card" onSubmit={handleSubmit}>
                <h1>Wanna Chat?</h1>
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
                <div className="eye">
                    <input
                        type={showPassword ? "text" : "password"}
                        value={password} 
                        onChange={e => setPassword(e.target.value)} 
                        placeholder="Password">
                    </input>
                    <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                    >
                        {showPassword ? <EyeOff size={18}/> : <Eye size={18} /> }
                    </button>
                </div>
                <button type="submit">Register</button>

                {status && <p className="status">{status}</p>}
            </form>
            <ToggleTheme/>
        </div>
    );
}

export default Register;