const API_BASE = "http://192.168.2.41:8080/api/status";

document.addEventListener("DOMContentLoaded", () => {
  loadUrls();

  const form = document.getElementById("addForm");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const url = document.getElementById("url").value.trim();
    const category = document.getElementById("category").value.trim();
    const tags = document.getElementById("tags").value.trim();

    try {
      const response = await fetch(`${API_BASE}/add`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url, category, tags })
      });

      if (!response.ok) throw new Error("Network error occurred while adding URL.");

      alert("✅ URL added successfully!");
      form.reset();
      loadUrls();
    } catch (err) {
      alert(err.message);
    }
  });
});

async function loadUrls() {
  try {
    const response = await fetch(`${API_BASE}/list`);
    if (!response.ok) throw new Error("Failed to load URLs.");

    const urls = await response.json();
    const tbody = document.querySelector("#urlTable tbody");
    tbody.innerHTML = "";

    urls.forEach((u) => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${u.url}</td>
        <td>${u.category}</td>
        <td>${u.tags || "-"}</td>
        <td>${u.up ? "UP" : "DOWN"}</td>
        <td>${u.sslDays || "-"}</td>
        <td><button class="remove-btn" onclick="removeUrl('${u.url}')">Remove</button></td>
      `;
      tbody.appendChild(row);
    });
  } catch (err) {
    console.error(err);
  }
}

async function removeUrl(url) {
  if (!confirm(`Remove ${url}?`)) return;

  try {
    const response = await fetch(`${API_BASE}/delete`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url })
    });

    if (!response.ok) throw new Error("Failed to remove URL.");

    alert("🗑️ URL removed successfully!");
    loadUrls();
  } catch (err) {
    alert(err.message);
  }
}

