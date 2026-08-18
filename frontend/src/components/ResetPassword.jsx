import "./ResetPassword.css";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import ToggleTheme from "./ToggleTheme";

function ResetPassword() {
    const navigate = useNavigate();
    const [newPassword, setNewPassword] = useState("");
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [retype, setRetype] = useState("");
    const [showConfirmedPassword, setShowConfirmedPassword] = useState(false);
    const [currentPassword, setCurrentPassword] = useState("");
    const [showCurrentPassword, setShowCurrentPassword] = useState(false);
    const [status, setStatus] = useState("");
    const [searchParams] = useSearchParams();
    const token = searchParams.get("token");
    const [toast, setToast] = useState(null);

    useEffect(() => {
        if (!toast) return;

        const timer = setTimeout(() => {
            setToast(null);
        }, 3000);
        return () => {clearTimeout(timer); navigate("/chat");}
        }, [toast])

    async function handleSubmit(e) {
        e.preventDefault();
        let res;
        const formData = new URLSearchParams();
        console.log("token:", token);
        if (token) {
            if (!newPassword) {setStatus("Enter a password"); return;}
            if (!retype) {setStatus("Retype Your Password"); return;}
            if (newPassword !== retype) {setStatus("Passwords Must Match"); return;}
            formData.append("newPassword", newPassword);

            res = await fetch(`http://localhost:8080/resetPassword?token=${encodeURIComponent(token)}`, {
                method: "POST",
                credentials: "include",
                headers: {"Content-type" : "application/x-www-form-urlencoded"},
                body: formData,
            });
        } 
        else {
            if (!newPassword) {setStatus("Enter a password"); return;}
            if (!retype) {setStatus("Retype Your Password"); return;}
            if (newPassword !== retype) {setStatus("Passwords Must Match"); return;}
            if (!currentPassword) {setStatus("Enter your current password"); return;}

            formData.append("newPassword", newPassword);
            formData.append("currentPassword", currentPassword);

            res = await fetch(`http://localhost:8080/changePassword`, {
                method: "POST",
                credentials: "include",
                headers: {"Content-type" : "application/x-www-form-urlencoded"},
                body: formData,
            });
        }

        if (res.ok) {
            // navigate("/chat");
            setToast({message: "Success. Redirecting to chat page.", type: "success"});
        } else if (res.status === 403) {
            setStatus("Wrong password");
        } else {
            setStatus("Unable to update password");
        }
    }

    return (
        <div className="reset-page">
            <form className="reset-card" onSubmit={handleSubmit}>
                <h1>Set a New Password</h1>
                <div className="eye">
                    <input
                        type={showNewPassword ? "text" : "password"}
                        value={newPassword}
                        onChange={e => setNewPassword(e.target.value)}
                        placeholder="Select a New Password">
                    </input>
                    <button 
                        type="button" 
                        onClick={() => setShowNewPassword(!showNewPassword)}
                    >
                        {showNewPassword ? <EyeOff size={18}/> : <Eye size={18} />}
                    </button>
                </div>
                <div className="eye">
                    <input
                        type={showConfirmedPassword ? "text" : "password"}
                        value={retype}
                        onChange={e => setRetype(e.target.value)}
                        placeholder="Retype Your New Password">
                    </input>
                    <button
                        type="button"
                        onClick={() => setShowConfirmedPassword(!showConfirmedPassword)}
                    >
                        {showConfirmedPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                </div>
                {!token ? 
                (<div className="eye">
                    <input
                        type={showCurrentPassword ? "text" : "password"}
                        value={currentPassword}
                        onChange={e => setCurrentPassword(e.target.value)}
                        placeholder = "Enter Your Current Password">
                    </input>
                    <button
                        type="button"
                        onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    >
                        {showCurrentPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                </div>) : (<></>)}
                <button type="submit">Submit</button>
                {status && <p className="status">{status}</p>}
                {toast && (<div className={`toast toast-${toast.type}`}>
                    {toast.message}
                </div>)}
            </form>
            <ToggleTheme/>
        </div>

);
}

export default ResetPassword;