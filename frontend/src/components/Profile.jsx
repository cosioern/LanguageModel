import {useState, useEffect} from "react"
import ToggleTheme from "./ToggleTheme";
import { Trash2 } from "lucide-react";
import "./Profile.css"


function Profile() {

    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [birthday, setBirthday] = useState("");
    const [name, setName] = useState("");
    const [documents, setDocuments] = useState([]);
    const [toast, setToast] = useState(null);

    // set toast for brief moment
    useEffect(() => {
        if(!toast) return;

        const timer = setTimeout(() => {
            setToast(null);
        }, 3000);
        return () => clearTimeout(timer);
    }, [toast])

    // populate User's profile details
    useEffect(() => {
        fetch(`http://localhost:8080/profile`, {
            credentials: "include",
            headers: {"Content-type": "application/x-www-form-urlencoded"},
        })
        .then (res => {
            if (res.status == 401) {
                setToast({message: "Unauthorized", type: "error"});
            }
            else if (res.status == 403) {
                setToast({message: "Forbidden", type: "error"});
            }
            return res.json();
        })
        .then(data => {
            setUsername(data.username); 
            setEmail(data.email);
            setBirthday(data.birthday);
            setName(data.name);
        })
        .catch(err => {
            console.error("Failed to fetch account details: ", err);
            setUsername("TestUserName");
            setEmail("test@mail.com");
            setBirthday("01-01-2001");
            setName("TestName");
        })
    }, []);

    // populate with User's documents
    useEffect(() => {

        fetch(`http://localhost:8080/documents`, {
            method: "GET",
            credentials: "include",
            headers: {"Content-type": "application/x-www-form-urlencoded"},
        })
        .then(res => {
            if (res.status == 401) {
                setToast({message: "Unauthorized", type: "error"});
            }
            return res.json();
        })
        .then(data => {
            console.log("documents: " + data);
            console.log(Array.isArray(data));
            setDocuments(data);
        })
        .catch(err => {
            setDocuments(
                [{documentID: 1, fileName: "Placeholder File 1"}, 
                {documentID: 2, fileName: "Placeholder File 2"}, 
                {documentID: 3, fileName: "Placeholder File 3"}]
            );
            console.error("Failed to fetch documents: ", err);
        });
    }, []);

    /**
     * Contact endpoint for document deletion and remove said document from state.
     * Set toast with success or failure message on fetch request return.
     * @param {string} fileName     is the name of file, used in the toast message
     * @param {string} documentID   is the UUID of the file used in backend call
     */
    async function deleteDocument(fileName, documentID) {

        const res = await fetch(`http://localhost:8080/deleteDocument?documentID=${documentID}`, {
            method: "DELETE",
            credentials: "include",
            headers: {"Content-type": "application/x-www-form-urlencoded"},
        })
        if (res.ok) {
            setToast({message: `Deleted \"${fileName}\"`, type: "success"});
            setDocuments(prev => prev.filter(doc => doc.documentID !== documentID));
        } 
        else if (res.status == 401) {
            setToast({message: "Unauthorized", type: "error"});
            }
        else if (res.status == 403) {
            setToast({message: "Forbidden", type: "error"});
        }
        else {
            console.log("Failed to delete document.");
        }
    }

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
            <div className="doc-card">
                <h1>Uploaded Documents:</h1>
                {documents.length === 0 ? (
                    <p className="empty">No documents uploded yet</p>
                ) : (
                    documents.map((doc) => 
                        <div className="doc-info"  key={doc.documentID}>
                            <p className="label">{doc.fileName}</p>
                            <Trash2 size={22} className="trash" onClick={() => deleteDocument(doc.fileName, doc.documentID)}></Trash2>
                        </div>)
                    )}
            </div>
            {toast && (
                <div className={`toast toast-${toast.type}`}>
                    {toast.message}
                </div>
            )}
            <ToggleTheme/>
        </div>
    );
}

export default Profile;