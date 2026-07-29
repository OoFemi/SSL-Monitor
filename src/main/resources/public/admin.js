document.addEventListener("DOMContentLoaded", () => {
    const token = sessionStorage.getItem("adminToken");
    if (token) {
        showAdminPanel();
    }
});

// Admin Login Form Handler
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const u = document.getElementById('loginUser').value.trim();
        const p = document.getElementById('loginPass').value.trim();

        fetch('/api/admin/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: u, password: p })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success || data.token) {
                sessionStorage.setItem("adminToken", data.token || "active-session");
                sessionStorage.setItem("fob-admin-auth", "true");
                showAdminPanel();
            } else {
                const errEl = document.getElementById('loginError');
                if (errEl) {
                    errEl.innerText = data.message || "Invalid credentials.";
                    errEl.classList.remove("hidden");
                }
            }
        })
        .catch(err => {
            console.error("Login network error:", err);
            // Fallback for default local credentials
            if (u === "admin" && p === "admin123") {
                sessionStorage.setItem("adminToken", "fallback-token");
                sessionStorage.setItem("fob-admin-auth", "true");
                showAdminPanel();
            } else {
                const errEl = document.getElementById('loginError');
                if (errEl) {
                    errEl.innerText = "Invalid credentials (default: admin / admin123)";
                    errEl.classList.remove("hidden");
                }
            }
        });
    });
}

// Logout Handler
const logoutBtn = document.getElementById('logoutBtn');
if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
        sessionStorage.removeItem("adminToken");
        sessionStorage.removeItem("fob-admin-auth");
        location.reload();
    });
}

function showAdminPanel() {
    const authOverlay = document.getElementById('authOverlay');
    if (authOverlay) authOverlay.classList.add('hidden');
    loadAdminData();
}

function loadAdminData() {
    loadUrls();
    loadPortals();
}

// Load Target URLs Safely
async function loadUrls() {
    try {
        const res = await fetch('/api/urls');
        const data = await res.json();
        const tbody = document.getElementById("adminUrlTableBody");
        if (!tbody) return;
        
        tbody.innerHTML = "";

        if (!Array.isArray(data)) {
            console.warn("API /api/urls did not return an array:", data);
            tbody.innerHTML = `<tr><td colspan="6" class="py-4 text-center text-brand-red font-mono">Server Error: Database or endpoint initializing... Check backend logs.</td></tr>`;
            return;
        }

        if (data.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="py-4 text-center text-slate-500">No active targets configured.</td></tr>`;
            return;
        }

        data.forEach(row => {
            const tags = Array.isArray(row.tags) ? row.tags.join(", ") : (row.tag || 'API');
            const badge = row.isUp 
                ? `<span class="px-2 py-0.5 rounded-full text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">UP</span>`
                : `<span class="px-2 py-0.5 rounded-full text-[10px] bg-brand-red/10 text-brand-red border border-brand-red/20">DOWN</span>`;

            tbody.innerHTML += `
                <tr class="hover:bg-slate-800/40">
                    <td class="py-3 px-6 font-mono text-brand-blue">${row.url || row.targetUrl}</td>
                    <td class="py-3 px-6">${row.category || 'Production'}</td>
                    <td class="py-3 px-6">${tags}</td>
                    <td class="py-3 px-6">${badge}</td>
                    <td class="py-3 px-6 font-mono">${row.sslDays ?? '--'} Days</td>
                    <td class="py-3 px-6 text-right space-x-2">
                        <button onclick="editUrl(${row.id}, '${row.url || row.targetUrl}', '${row.category || 'Production'}', '${tags}', '${row.renewalUrl || ''}')" class="bg-brand-blue/10 text-brand-blue px-2.5 py-1 rounded hover:bg-brand-blue/20">Edit</button>
                        <button onclick="deleteUrl(${row.id})" class="bg-brand-red/10 text-brand-red px-2.5 py-1 rounded hover:bg-brand-red/20">Delete</button>
                    </td>
                </tr>`;
        });
    } catch (e) {
        console.error("Error loading URLs:", e);
    }
}

let editUrlId = null;

const saveUrlBtn = document.getElementById("saveUrlBtn");
if (saveUrlBtn) {
    saveUrlBtn.addEventListener("click", async () => {
        const url = document.getElementById("urlInput").value.trim();
        const category = document.getElementById("categoryInput").value;
        const tag = document.getElementById("tagInput").value;
        const renewalUrl = document.getElementById("renewalUrlInput").value.trim();

        if (!url) {
            alert("Target URL is required.");
            return;
        }

        const payload = { url, category, tags: [tag], renewalUrl };
        const method = editUrlId ? "PUT" : "POST";
        const endpoint = editUrlId ? `/api/admin/update-url/${editUrlId}` : `/api/admin/add-url`;

        try {
            const res = await fetch(endpoint, {
                method,
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                resetUrlForm();
                loadUrls();
            } else {
                alert("Failed to save endpoint target.");
            }
        } catch (err) {
            console.error("Error saving URL target:", err);
        }
    });
}

window.editUrl = (id, url, category, tags, renewalUrl) => {
    editUrlId = id;
    document.getElementById("urlInput").value = url;
    document.getElementById("categoryInput").value = category;
    document.getElementById("renewalUrlInput").value = renewalUrl;
    document.getElementById("urlFormTitle").innerText = "Edit Monitored Endpoint";
    document.getElementById("saveUrlBtnText").innerText = "Update Endpoint";
    document.getElementById("cancelUrlEditBtn").classList.remove("hidden");
};

function resetUrlForm() {
    editUrlId = null;
    document.getElementById("urlInput").value = "";
    document.getElementById("renewalUrlInput").value = "";
    document.getElementById("urlFormTitle").innerText = "Add / Edit Target Endpoint";
    document.getElementById("saveUrlBtnText").innerText = "Save Target Endpoint";
    document.getElementById("cancelUrlEditBtn").classList.add("hidden");
}

const cancelUrlEditBtn = document.getElementById("cancelUrlEditBtn");
if (cancelUrlEditBtn) {
    cancelUrlEditBtn.addEventListener("click", resetUrlForm);
}

window.deleteUrl = async (id) => {
    if (!confirm("Are you sure you want to delete this target endpoint?")) return;
    try {
        await fetch(`/api/admin/delete-url/${id}`, { method: "DELETE" });
        loadUrls();
    } catch (err) {
        console.error("Error deleting URL:", err);
    }
};

// Load Renewal Portals Safely
async function loadPortals() {
    try {
        const res = await fetch('/api/renewal-portals');
        const data = await res.json();
        const tbody = document.getElementById("portalsTableBody");
        if (!tbody) return;
        
        tbody.innerHTML = "";

        if (!Array.isArray(data)) {
            console.warn("API /api/renewal-portals did not return an array:", data);
            return;
        }

        data.forEach(p => {
            tbody.innerHTML += `
                <tr>
                    <td class="py-2 px-4 font-semibold">${p.name}</td>
                    <td class="py-2 px-4 font-mono text-purple-400">${p.domainPattern}</td>
                    <td class="py-2 px-4 font-mono text-slate-400">${p.renewalUrl}</td>
                    <td class="py-2 px-4 text-right space-x-2">
                        <button onclick="editPortal(${p.id}, '${p.name}', '${p.domainPattern}', '${p.renewalUrl}')" class="text-brand-blue hover:underline">Edit</button>
                        <button onclick="deletePortal(${p.id})" class="text-brand-red hover:underline">Delete</button>
                    </td>
                </tr>`;
        });
    } catch (e) {
        console.error("Error loading renewal portals:", e);
    }
}

let editPortalId = null;
const savePortalBtn = document.getElementById("savePortalBtn");
if (savePortalBtn) {
    savePortalBtn.addEventListener("click", async () => {
        const name = document.getElementById("portalName").value.trim();
        const domainPattern = document.getElementById("portalPattern").value.trim();
        const renewalUrl = document.getElementById("portalUrl").value.trim();

        if (!name || !renewalUrl) {
            alert("Provider Name and Renewal URL are required.");
            return;
        }

        const payload = { name, domainPattern, renewalUrl };
        const method = editPortalId ? "PUT" : "POST";
        const endpoint = editPortalId ? `/api/admin/update-portal/${editPortalId}` : `/api/admin/add-portal`;

        try {
            const res = await fetch(endpoint, {
                method,
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                resetPortalForm();
                loadPortals();
            }
        } catch (err) {
            console.error("Error saving renewal portal:", err);
        }
    });
}

window.editPortal = (id, name, pattern, url) => {
    editPortalId = id;
    document.getElementById("portalName").value = name;
    document.getElementById("portalPattern").value = pattern;
    document.getElementById("portalUrl").value = url;
    document.getElementById("portalFormTitle").innerText = "Edit SSL Renewal Provider Website";
    document.getElementById("savePortalBtnText").innerText = "Update Provider";
    document.getElementById("cancelPortalEditBtn").classList.remove("hidden");
};

function resetPortalForm() {
    editPortalId = null;
    document.getElementById("portalName").value = "";
    document.getElementById("portalPattern").value = "";
    document.getElementById("portalUrl").value = "";
    document.getElementById("portalFormTitle").innerText = "SSL Renewal Provider Websites";
    document.getElementById("savePortalBtnText").innerText = "Save Renewal Provider";
    document.getElementById("cancelPortalEditBtn").classList.add("hidden");
}

const cancelPortalEditBtn = document.getElementById("cancelPortalEditBtn");
if (cancelPortalEditBtn) {
    cancelPortalEditBtn.addEventListener("click", resetPortalForm);
}

window.deletePortal = async (id) => {
    if (!confirm("Delete this SSL renewal portal?")) return;
    try {
        await fetch(`/api/admin/delete-portal/${id}`, { method: "DELETE" });
        loadPortals();
    } catch (err) {
        console.error("Error deleting portal:", err);
    }
};
