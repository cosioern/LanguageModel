import "./Right.css"

function Right() {
    return (
        <div className="sideRight">
            {/*Button classifier*/}
            <div className="button-holder">
                <button className="login-button">Log In</button>
                <button className="signin-button">Sign Up</button>
            </div>
            <button className="share-button">Share</button>
        </div>
    );
}

export default Right;