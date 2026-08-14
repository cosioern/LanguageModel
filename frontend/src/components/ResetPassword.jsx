import "./ResetPassword.css";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import ToggleTheme from "./ToggleTheme";

function ResetPassword() {
    const navigate = useNavigate();
    const [password, setPassword] = useState("");
    const [retype, setRetype] = useState("");
    const [status, setStatus] = useState("");
    const [searchParams] = useSearchParams();
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmedPassword, setShowConfirmedPassword] = useState(false);
    const token = searchParams.get("token");

    async function handleSubmit(e) {
        e.preventDefault();
        if (!password) {setStatus("Enter a password"); return;}
        if (!retype) {setStatus("Retype Your Password"); return;}
        if (password !== retype) {setStatus("Passwords Must Match"); return;}
        const formData = new URLSearchParams();

        formData.append("password", password);

        const res = await fetch(`http://localhost:8080/resetPassword?token=${encodeURIComponent(token)}`, {
            method: "POST",
            credentials: "include",
            headers: {"Content-type" : "application/x-www-form-urlencoded"},
            body: formData,
        });

        if (res.ok) {
            navigate("/chat")
        } else {
            setStatus("Unable To Reset Password")
        }
    }

    return (
        <div className="reset-page">
            <form className="reset-card" onSubmit={handleSubmit}>
                <h1>Set a New Password</h1>
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
                        {showPassword ? <EyeOff size={18}/> : <Eye size={18} />}
                    </button>
                </div>
                <div className="eye">
                <input
                    type={showConfirmedPassword ? "text" : "password"}
                    value={retype}
                    onChange={e => setRetype(e.target.value)}
                    placeholder="Retype Your Password">
                </input>
                <button
                    type="button"
                    onClick={() => setShowConfirmedPassword(!showConfirmedPassword)}
                >
                    {showConfirmedPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
                </div>
                <button type="submit">Submit</button>
                {status && <p className="status">{status}</p>}
            </form>
            <ToggleTheme/>
        </div>

);
}

export default ResetPassword;