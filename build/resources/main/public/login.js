document.addEventListener("DOMContentLoaded", () => {
  const form = document.querySelector("#loginForm");
  const errorMsg = document.querySelector("#errorMsg");

  form.addEventListener("submit", e => {
    e.preventDefault();

    const username = document.querySelector("#username").value.trim();
    const password = document.querySelector("#password").value.trim();

    fetch("/api/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    })
      .then(res => {
        if (res.ok) {
          window.location.href = "/admin.html";
        } else if (res.status === 401) {
          errorMsg.textContent = "Invalid username or password.";
        } else {
          errorMsg.textContent = "Login failed. Please try again.";
        }
      })
      .catch(err => {
        console.error("Login error:", err);
        errorMsg.textContent = "Server error. Please check backend connection.";
      });
  });
});

// ---------- LOGOUT FUNCTION ----------
function logout() {
  fetch("/api/logout", { method: "POST" })
    .then(() => {
      // Clear session and stay on login page
      window.location.href = "/login.html";
    })
    .catch(err => console.error("Logout failed:", err));
}
