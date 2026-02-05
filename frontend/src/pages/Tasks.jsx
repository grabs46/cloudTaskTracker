import { useState, useEffect } from "react";
import { apiFetch } from "../api";

const STATUS_OPTIONS = ["", "TODO", "IN_PROGRESS", "DONE"];
const SORT_OPTIONS = [
  { value: "createdAt", label: "Created" },
  { value: "title", label: "Title" },
  { value: "updatedAt", label: "Updated" },
];

export default function Tasks({ onLogout }) {
  const [tasks, setTasks] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDir, setSortDir] = useState("desc");

  const [newTitle, setNewTitle] = useState("");
  const [newDescription, setNewDescription] = useState("");

  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editStatus, setEditStatus] = useState("TODO");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const triggerRefresh = () => setRefreshTrigger((n) => n + 1);

  // Debounce the search query - wait 300ms after user stops typing
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const abortController = new AbortController();

    async function fetchTasks() {
      setLoading(true);
      setError(null);
      try {
        const params = new URLSearchParams({
          page: page.toString(),
          size: "10",
          sortBy,
          sortDir,
        });
        if (debouncedQuery) params.set("query", debouncedQuery);
        if (statusFilter) params.set("status", statusFilter);

        const res = await apiFetch(`/api/tasks?${params}`, {
          signal: abortController.signal,
        });
        if (!res.ok) throw new Error("Failed to fetch tasks");

        const data = await res.json();
        setTasks(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      } catch (err) {
        // Ignore abort errors - they're expected during cleanup
        if (err.name !== "AbortError") {
          setError(err.message);
        }
      } finally {
        if (!abortController.signal.aborted) {
          setLoading(false);
        }
      }
    }

    fetchTasks();

    return () => abortController.abort();
  }, [page, debouncedQuery, statusFilter, sortBy, sortDir, refreshTrigger]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    triggerRefresh();
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    try {
      const res = await apiFetch("/api/tasks", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: newTitle.trim(),
          description: newDescription.trim() || null,
        }),
      });
      if (!res.ok) throw new Error("Failed to create task");

      setNewTitle("");
      setNewDescription("");
      setPage(0);
      triggerRefresh();
    } catch (err) {
      setError(err.message);
    }
  };

  const startEdit = (task) => {
    setEditingId(task.id);
    setEditTitle(task.title);
    setEditDescription(task.description || "");
    setEditStatus(task.status);
  };

  const cancelEdit = () => {
    setEditingId(null);
  };

  const saveEdit = async (taskId) => {
    try {
      const res = await apiFetch(`/api/tasks/${taskId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: editTitle.trim(),
          description: editDescription.trim() || null,
          status: editStatus,
        }),
      });
      if (!res.ok) throw new Error("Failed to update task");

      setEditingId(null);
      triggerRefresh();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleDelete = async (taskId) => {
    if (!window.confirm("Delete this task?")) return;

    try {
      const res = await apiFetch(`/api/tasks/${taskId}`, {
        method: "DELETE",
      });
      if (!res.ok) throw new Error("Failed to delete task");

      triggerRefresh();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleLogout = async () => {
    await apiFetch("/api/auth/logout", { method: "POST" });
    onLogout();
  };

  return (
    <div className="tasks-layout" style={{ display: "flex", gap: 32, padding: "24px 48px", maxWidth: 1600, margin: "0 auto" }}>
      {/* Sidebar - New Task Form */}
      <aside className="tasks-sidebar" style={{
        width: 280,
        flexShrink: 0,
        position: "sticky",
        top: 24,
        alignSelf: "flex-start"
      }}>
        <form onSubmit={handleCreate} style={{ padding: 20, background: "var(--bg-card)", borderRadius: 8 }}>
          <h3 style={{ marginTop: 0, marginBottom: 16, fontSize: 18 }}>New Task</h3>
          <div style={{ marginBottom: 12 }}>
            <input
              type="text"
              placeholder="Title"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              style={{ width: "100%", padding: 10, boxSizing: "border-box", borderRadius: 6 }}
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <textarea
              placeholder="Description (optional)"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              style={{ width: "100%", padding: 10, boxSizing: "border-box", minHeight: 80, borderRadius: 6, resize: "vertical" }}
            />
          </div>
          <button type="submit" className="btn-primary" style={{ width: "100%", padding: "10px 16px" }}>
            Create Task
          </button>
        </form>
      </aside>

      {/* Main Content */}
      <main style={{ flex: 1, minWidth: 0 }}>
        {/* Header */}
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 }}>
          <h1 style={{ margin: 0, fontSize: 32, fontWeight: 700 }}>Tasks</h1>
          <button onClick={handleLogout} style={{ padding: "8px 16px" }}>Logout</button>
        </div>

        {error && (
          <div style={{ background: "var(--error-bg)", color: "var(--error-text)", padding: 12, marginBottom: 16, borderRadius: 6 }}>
            {error}
          </div>
        )}

        {/* Search and Filters */}
        <form onSubmit={handleSearch} style={{ marginBottom: 20, display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input
            type="text"
            placeholder="Search..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            style={{ padding: 10, flex: 1, minWidth: 150, borderRadius: 6 }}
          />
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            style={{ padding: 10, borderRadius: 6 }}
          >
            <option value="">All Status</option>
            {STATUS_OPTIONS.filter(s => s).map((s) => (
              <option key={s} value={s}>{s.replace("_", " ")}</option>
            ))}
          </select>
          <select
            value={sortBy}
            onChange={(e) => { setSortBy(e.target.value); setPage(0); }}
            style={{ padding: 10, borderRadius: 6 }}
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
          <select
            value={sortDir}
            onChange={(e) => { setSortDir(e.target.value); setPage(0); }}
            style={{ padding: 10, borderRadius: 6 }}
          >
            <option value="desc">Desc</option>
            <option value="asc">Asc</option>
          </select>
        </form>

        {/* Task List */}
        {loading ? (
          <p>Loading...</p>
        ) : tasks.length === 0 ? (
          <p style={{ color: "var(--text-muted)" }}>No tasks found.</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {tasks.map((task) => (
              <div
                key={task.id}
                className="task-card"
                style={{
                  borderRadius: 8,
                  padding: 16,
                  background: "var(--bg-surface)",
                }}
              >
                {editingId === task.id ? (
                  /* Inline Edit Mode */
                  <div>
                    <input
                      type="text"
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      style={{ width: "100%", padding: 10, marginBottom: 8, boxSizing: "border-box", borderRadius: 6 }}
                    />
                    <textarea
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      style={{ width: "100%", padding: 10, marginBottom: 8, boxSizing: "border-box", minHeight: 60, borderRadius: 6 }}
                    />
                    <select
                      value={editStatus}
                      onChange={(e) => setEditStatus(e.target.value)}
                      style={{ padding: 10, marginBottom: 8, borderRadius: 6 }}
                    >
                      {STATUS_OPTIONS.filter(s => s).map((s) => (
                        <option key={s} value={s}>{s.replace("_", " ")}</option>
                      ))}
                    </select>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button onClick={() => saveEdit(task.id)} className="btn-primary" style={{ padding: "8px 16px" }}>Save</button>
                      <button onClick={cancelEdit} style={{ padding: "8px 16px" }}>Cancel</button>
                    </div>
                  </div>
                ) : (
                  /* View Mode */
                  <div>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                      <div>
                        <span style={{ fontSize: 18, fontWeight: 600 }}>{task.title}</span>
                        <span
                          style={{
                            marginLeft: 10,
                            padding: "3px 8px",
                            fontSize: 11,
                            fontWeight: 500,
                            borderRadius: 4,
                            background: task.status === "DONE" ? "var(--status-done)" : task.status === "IN_PROGRESS" ? "var(--status-progress)" : "var(--status-todo)",
                          }}
                        >
                          {task.status.replace("_", " ")}
                        </span>
                      </div>
                      <div style={{ display: "flex", gap: 8 }}>
                        <button onClick={() => startEdit(task)} style={{ padding: "6px 12px", fontSize: 12 }}>Edit</button>
                        <button onClick={() => handleDelete(task.id)} style={{ padding: "6px 12px", fontSize: 12, color: "var(--delete-color)" }}>Delete</button>
                      </div>
                    </div>
                    {task.description && (
                      <p style={{ margin: "10px 0 0", color: "var(--text-secondary)" }}>{task.description}</p>
                    )}
                    <p style={{ margin: "10px 0 0", fontSize: 12, color: "var(--text-muted)" }}>
                      Created: {new Date(task.createdAt).toLocaleString()}
                    </p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ marginTop: 24, display: "flex", justifyContent: "center", alignItems: "center", gap: 16 }}>
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              style={{ padding: "8px 16px" }}
            >
              Previous
            </button>
            <span style={{ color: "var(--text-muted)" }}>
              Page {page + 1} of {totalPages} ({totalElements} total)
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              style={{ padding: "8px 16px" }}
            >
              Next
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
