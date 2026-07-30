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
                sessionStorage.setItem("f-admin-auth", "true");
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
            // Fallback for default local credentials configuration
            if (u === "admin" && p === "admin123") {
                sessionStorage.setItem("adminToken", "fallback-token");
                sessionStorage.setItem("f-admin-auth", "true");
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
        sessionStorage.removeItem("f-admin-auth");
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
                <tr class="hover:bg-slate-800/40 transition">
                    <td class="py-3 px-6 font-mono text-brand-blue">${escapeHtml(row.url || row.targetUrl || '')}</td>
                    <td class="py-3 px-6">${escapeHtml(row.category || 'Production')}</td>
                    <td class="py-3 px-6"><span class="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px]">${escapeHtml(tags)}</span></td>
                    <td class="py-3 px-6">${badge}</td>
                    <td class="py-3 px-6 font-mono">${row.sslDays !== undefined && row.sslDays !== null ? row.sslDays + ' Days' : '--'}</td>
                    <td class="py-3 px-6 text-right space-x-2">
                        <button onclick="window.editUrl(${row.id}, '${escapeQuote(row.url || row.targetUrl)}', '${escapeQuote(row.category || 'Production')}', '${escapeQuote(tags)}', '${escapeQuote(row.renewalUrl || '')}')" class="text-brand-blue hover:text-brand-hoverBlue p-1 transition" title="Edit"><i class="fa-solid fa-pen-to-square"></i></button>
                        <button onclick="window.deleteUrl(${row.id})" class="text-brand-red hover:text-red-400 p-1 transition" title="Delete"><i class="fa-solid fa-trash"></i></button>
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

        const payload = { url, category, tags: [tag], tag, renewalUrl };
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
    document.getElementById("urlFormTitleinnerText") || (document.getElementById("urlFormTitle").innerText = "Edit Monitored Endpoint (ID: " + id + ")");
    document.getElementById("saveUrlBtnText").innerText = "Update Endpoint";
    document.getElementById("cancelUrlEditBtn").classList.remove("hidden");
    window.scrollTo({ top: 0, behavior: 'smooth' });
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
        const res = await fetch(`/api/admin/delete-url/${id}`, { method: "DELETE" });
        if (res.ok) {
            loadUrls();
        } else {
            alert("Failed to delete target endpoint.");
        }
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
            const portalName = p.name || p.providerName || '';
            const pattern = p.domainPattern || p.pattern || '';
            const rUrl = p.renewalUrl || p.url || '';

            tbody.innerHTML += `
                <tr class="hover:bg-slate-800/40 transition">
                    <td class="py-3 px-4 font-semibold text-slate-200">${escapeHtml(portalName)}</td>
                    <td class="py-3 px-4 font-mono text-purple-400">${escapeHtml(pattern)}</td>
                    <td class="py-3 px-4 font-mono text-brand-blue truncate max-w-xs"><a href="${escapeHtml(rUrl)}" target="_blank" class="hover:underline">${escapeHtml(rUrl)}</a></td>
                    <td class="py-3 px-4 text-right space-x-2">
                        <button onclick="window.editPortal(${p.id}, '${escapeQuote(portalName)}', '${escapeQuote(pattern)}', '${escapeQuote(rUrl)}')" class="text-brand-blue hover:text-brand-hoverBlue p-1 transition" title="Edit"><i class="fa-solid fa-pen-to-square"></i></button>
                        <button onclick="window.deletePortal(${p.id})" class="text-brand-red hover:text-red-400 p-1 transition" title="Delete"><i class="fa-solid fa-trash"></i></button>
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
            } else {
                alert("Failed to save renewal provider.");
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
    document.getElementById("portalFormTitle").innerText = "Edit SSL Renewal Provider Website (ID: " + id + ")";
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
    if (!confirm("Are you sure you want to delete this SSL renewal portal?")) return;
    try {
        const res = await fetch(`/api/admin/delete-portal/${id}`, { method: "DELETE" });
        if (res.ok) {
            loadPortals();
        } else {
            alert("Failed to delete renewal portal.");
        }
    } catch (err) {
        console.error("Error deleting portal:", err);
    }
};

// Utility Helpers for Safe String Output
function escapeHtml(str) {
    return String(str || "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function escapeQuote(str) {
    return String(str || "").replace(/'/g, "\\'").replace(/"/g, '&quot;');
}
