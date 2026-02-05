import { useEffect, useState } from "react";
import Login from "./pages/Login";
import Tasks from "./pages/Tasks";
import { apiFetch } from "./api";

export default function App() {
  const [authed, setAuthed] = useState(false);
  const [loading, setLoading] = useState(true);
  
  async function refreshMe() {
    setLoading(true);
  try {
    const res = await apiFetch("/api/me"); // credentials: "include" inside apiFetch
    setAuthed(res.ok);
  } catch (err) {
    // If CORS/network fails, treat as logged out so user can see Login page
    console.error("refreshAuth failed:", err);
    setAuthed(false);
  } finally {
    setLoading(false);
  }
}

  useEffect(() => { refreshMe(); }, []);

  if (loading) return <div style={{ padding: 24 }}>Loading...</div>;
  if (!authed) return <Login onLoggedIn={() => refreshMe()} />;
  return <Tasks onLogout={refreshMe} />;
}
