import { useNavigate } from "react-router-dom";
import { useEffect } from "react";


function Verify() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get("token");

    useEffect(() => {
        async function verify() {
            const res = await fetch(`http://localhost:8080/verify?token=${encodeURIComponent(token)}`, {
                method: "GET",
                credentials: "include",
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