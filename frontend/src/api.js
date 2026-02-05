const API_BASE = import.meta.env.VITE_API_BASE;

export async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: "include", // IMPORTANT for cookie-based auth
  });
  return res;
}
