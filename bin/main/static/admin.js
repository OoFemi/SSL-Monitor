const BASE_URL = window.location.origin;

// ---------- LOAD ADMIN DATA ----------
function fetchAdmin() {
  fetch(`${BASE_URL}/api/admin/urls`)
    .then(res => {
      if (res.status === 401) {
        // Not logged in — redirect to login page
        window.location.href = "/login.html";
        return;
      }
      if (!res.ok) throw new Error("Failed to load admin data");
      return res.json();
    })
    .then(data => {
      renderAdminTable(data.items || []);
    })
    .catch(err => {
      console.error(err);
      alert("Failed to load admin data (check server or login session).");
    });
}

// ---------- RENDER TABLE ----------
function renderAdminTable(items) {
  const tbody = document.querySelector("#adminTable tbody");
  tbody.innerHTML = "";

  if (!items || items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;">No URLs found</td></tr>`;
    return;
  }

  items.forEach(u => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${u.id}</td>
      <td>${u.url}</td>
      <td>${u.category || ""}</td>
      <td>${(u.tags || []).join(", ")}</td>
      <td>
        <button onclick="startEdit(${u.id}, '${encodeURIComponent(u.url)}', '${encodeURIComponent(u.category || "")}', '${encodeURIComponent((u.tags || []).join(","))}')">Edit</button>
        <button onclick="deleteUrl(${u.id})" style="background:#dc2626;">Delete</button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

// ---------- EDIT FORM ----------
function startEdit(id, url, category, tags) {
  document.querySelector("#urlId").value = id;
  document.querySelector("#urlInput").value = decodeURIComponent(url);
  document.querySelector("#categoryInput").value = decodeURIComponent(category);
  document.querySelector("#tagsInput").value = decodeURIComponent(tags);
  document.querySelector("#cancelEdit").style.display = "inline-block";
}

function resetForm() {
  document.querySelector("#urlId").value = "";
  document.querySelector("#urlInput").value = "";
  document.querySelector("#categoryInput").value = "";
  document.querySelector("#tagsInput").value = "";
  document.querySelector("#cancelEdit").style.display = "none";
}

// ---------- DELETE URL ----------
function deleteUrl(id) {
  if (!confirm("Delete this URL?")) return;

  fetch(`${BASE_URL}/api/admin/urls/${id}`, {
    method: "DELETE"
  })
    .then(res => {
      if (res.status === 401) window.location.href = "/login.html";
      fetchAdmin();
    })
    .catch(err => {
      console.error(err);
      alert("Failed to delete URL.");
    });
}

// ---------- SAVE (ADD/EDIT) ----------
document.addEventListener("DOMContentLoaded", () => {
  document.querySelector("#urlForm").addEventListener("submit", e => {
    e.preventDefault();

    const id = document.querySelector("#urlId").value;
    const payload = {
      url: document.querySelector("#urlInput").value.trim(),
      category: document.querySelector("#categoryInput").value.trim(),
      tags: document.querySelector("#tagsInput").value
        .split(",")
        .map(t => t.trim())
        .filter(t => t.length > 0)
    };

    const method = id ? "PUT" : "POST";
    const endpoint = id
      ? `${BASE_URL}/api/admin/urls/${id}`
      : `${BASE_URL}/api/admin/urls`;

    fetch(endpoint, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    })
      .then(res => {
        if (res.status === 401) window.location.href = "/login.html";
        resetForm();
        fetchAdmin();
      })
      .catch(err => {
        console.error(err);
        alert("Failed to save URL.");
      });
  });

  document.querySelector("#cancelEdit").addEventListener("click", resetForm);
  fetchAdmin();
});

// ---------- LOGOUT ----------
function logout() {
  fetch(`${BASE_URL}/api/logout`, { method: "POST" })
    .then(() => window.location.href = "/login.html");
}

