import { useNavigate, useSearchParams } from "react-router-dom";
import { useEffect } from "react";


function Verify() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get("token");
    // const token = useSearchParams().searchParams.get("token");

    useEffect(() => {
        async function verify() {
            const res = await fetch(`http://localhost:8080/verify?token=${encodeURIComponent(token)}`, {
                method: "GET",
                // headers: {"Content-Type": "application/x-www-form-urlencoded"},
                credentials: "include",
                // body: `token=${encodeURIComponent(token)}`,
            });

            if (res.ok) {
                navigate("/chat");
            } else {
                // error message
            }
        }
        verify();
    }, []);
    return null;
}

export default Verify;