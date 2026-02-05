import { useEffect, useRef } from "react";

export default function Login({ onLoggedIn }) {
  const buttonDiv = useRef(null);

  useEffect(() => {
    if (!window.google || !buttonDiv.current) return;

    window.google.accounts.id.initialize({
      client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
      callback: async (response) => {
        // response.credential is the Google ID token (JWT)
        const res = await fetch(`${import.meta.env.VITE_API_BASE}/api/auth/google`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "include", // IMPORTANT so browser accepts httpOnly cookie
          body: JSON.stringify({ idToken: response.credential }),
        });

        if (res.ok) onLoggedIn();
        else console.error(await res.text());
      },
    });

    window.google.accounts.id.renderButton(buttonDiv.current, {
      theme: "outline",
      size: "large",
      text: "signin_with",
    });
  }, [onLoggedIn]);

  return (
    <div style={{
      minHeight: "100vh",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: 24,
    }}>
      <div style={{
        background: "var(--bg-card)",
        borderRadius: 12,
        padding: "48px 40px",
        textAlign: "center",
        maxWidth: 360,
        width: "100%",
      }}>
        {/* App name with accent */}
        <h1 style={{
          margin: "0 0 8px 0",
          fontSize: 32,
          fontWeight: 700,
          color: "var(--accent)",
        }}>
          TaskTracker
        </h1>

        {/* Tagline */}
        <p style={{
          margin: "0 0 32px 0",
          color: "var(--text-muted)",
          fontSize: 14,
        }}>
          Track your tasks, stay organized
        </p>

        {/* Google sign-in button */}
        <div style={{ display: "flex", justifyContent: "center" }}>
          <div ref={buttonDiv} />
        </div>

        {/* Footer text */}
        <p style={{
          margin: "24px 0 0 0",
          color: "var(--text-muted)",
          fontSize: 12,
        }}>
          Sign in with your Google account to get started
        </p>
      </div>
    </div>
  );
}
