import {useState, useEffect} from "react"
import "./Profile.css"

function Profile() {

    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [birthday, setBirthday] = useState("");
    const [name, setName] = useState("");

    useEffect(() => {
        fetch(`http://localhost:8080/account`, {
            // method: "GET",
            credentials: "include",
            headers: {"Content-type": "application/x-www-form-urlencoded"},
        })
        .then(res => res.json())
        .then(data => {
            setUsername(data.username); 
            setEmail(data.email);
            setBirthday(data.birthday);
            setName(data.name);
        })
        .catch(err => {
            console.error("Failed to fetch account details: ", err);
            setUsername("TestName");
            setEmail("test@mail.com");
            setBirthday("01-01-2001");
            setName("Bernie");
        })
    }, []);

    return (
        <div className="profile-page">
            <div className="profile-card">
                <h1>{name}'s Profile</h1>

                <div className="profile-info">
                    <span className="label">Username</span>
                    <span className="value">{username}</span>
                </div>

                <div className="profile-info">
                    <span className="label">Email</span>
                    <span className="value">{email}</span>
                </div>

                <div className="profile-info">
                    <span className="label">Birthday</span>
                    <span className="value">{birthday}</span>
                </div>
            </div>
        </div>
    );
}

export default Profile;