document.addEventListener("DOMContentLoaded", () => {
    const token = sessionStorage.getItem("adminToken");
    if (token) {
        showAdminPanel();
    }
});

document.getElementById('login-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const u = document.getElementById('username').value;
    const p = document.getElementById('password').value;

    fetch('/api/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: u, password: p })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            sessionStorage.setItem("adminToken", data.token);
            showAdminPanel();
        } else {
            document.getElementById('login-error').innerText = data.message;
        }
    })
    .catch(err => console.error(err));
});

function showAdminPanel() {
    document.getElementById('login-container').style.display = 'none';
    document.getElementById('admin-panel').style.display = 'block';
}

document.getElementById('add-url-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const url = document.getElementById('target-url').value;

    fetch('/api/endpoints/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: url })
    })
    .then(res => res.text())
    .then(msg => {
        document.getElementById('response-msg').innerText = "URL added successfully!";
        document.getElementById('target-url').value = '';
    })
    .catch(err => console.error(err));
});

document.getElementById('upload-logo-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const fileInput = document.getElementById('logo-file');
    if (fileInput.files.length === 0) return;

    const formData = new FormData();
    formData.append("logo", fileInput.files[0]);

    fetch('/api/admin/upload-logo', {
        method: 'POST',
        body: formData
    })
    .then(res => res.text())
    .then(msg => {
        document.getElementById('logo-msg').innerText = "Logo uploaded successfully! Refresh dashboard to see it.";
    })
    .catch(err => {
        document.getElementById('logo-msg').innerText = "Error uploading logo.";
        console.error(err);
    });
});
