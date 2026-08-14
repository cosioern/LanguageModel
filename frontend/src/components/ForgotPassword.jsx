import "./ForgotPassword.css";
import {useState} from "react";
import ToggleTheme from "./ToggleTheme";

function ForgotPassword() {
    const [email, setEmail] = useState("");
    const [status, setStatus] = useState(null);

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

     async function handleSubmit(e) {
        e.preventDefault();
        if (!email)     {
            setStatus({type: "error", message:"Enter an Email"}); 
            return;
        }
        if (!emailRegex.test(email)) {
            setStatus({type: "error", message:"Invalid Email"});
            return;
        }

        const formData = new URLSearchParams();
        formData.append("email", email);

        // send
        const res = await fetch(`http://localhost:8080/forgotPassword`, {
            method: "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: formData,
        });

        if (res.ok) {
            setStatus({type: "success", message: "Check your email"});
        } else {
            setStatus({type: "error", message: "Email not found"});
        }

    }

    return (
        <div className="forgot-page">
            <form className="forgot-card" onSubmit={handleSubmit}> 
                <h1>Password Reset</h1>
                <input
                    type="email"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    placeholder="Email">
                </input>
                <button type="submit">Send Link</button>
                {status && 
                    <p className={`status ${status.type}`}>
                        {status.message}
                    </p>}
            </form>
            <ToggleTheme/>
        </div>
    );
}

export default ForgotPassword;