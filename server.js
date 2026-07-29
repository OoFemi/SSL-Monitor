const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const https = require('https');
const { URL } = require('url');

const app = express();
const PORT = 7000;

app.use(cors());
app.use(bodyParser.json());

// In-Memory Data Store (Can be backed by PostgreSQL / SQLite)
let monitoredUrls = [
    {
        id: 1,
        url: "https://fiberone.ng",
        category: "Production",
        tags: ["Frontend"],
        isUp: true,
        responseTime: 120,
        sslDays: 45,
        lastChecked: Date.now(),
        renewalUrl: "https://www.digicert.com/account/login",
        lastAlertSentStage: null
    },
    {
        id: 2,
        url: "https://api.fiberone.ng",
        category: "Production",
        tags: ["API"],
        isUp: true,
        responseTime: 85,
        sslDays: 5, // Trigger renewal status
        lastChecked: Date.now(),
        renewalUrl: "https://cloudflare.com",
        lastAlertSentStage: "7days"
    }
];

let renewalPortals = [
    {
        id: 1,
        name: "DigiCert Central",
        domainPattern: "fiberone.ng",
        renewalUrl: "https://www.digicert.com/account/login"
    },
    {
        id: 2,
        name: "Cloudflare Cert Manager",
        domainPattern: "cloudflare.com",
        renewalUrl: "https://dash.cloudflare.com"
    }
];

let systemLogs = [
    { id: 1, timestamp: Date.now() - 3600000, type: "INFO", message: "FOB SSL Monitor Engine Initialized on port 7000." },
    { id: 2, timestamp: Date.now() - 1800000, type: "ALERT", message: "SSL Warning: target https://api.fiberone.ng expires in 5 days." }
];

// -------------------------------------------------------------------
// SSL & HTTP HEALTH CHECK INSPECTOR ENGINE
// -------------------------------------------------------------------
function checkTargetHealth(target) {
    return new Promise((resolve) => {
        try {
            const parsedUrl = new URL(target.url);
            const startTime = Date.now();

            const req = https.request({
                hostname: parsedUrl.hostname,
                port: parsedUrl.port || 443,
                method: 'GET',
                rejectUnauthorized: false
            }, (res) => {
                const responseTime = Date.now() - startTime;
                const cert = res.socket.getPeerCertificate();

                let sslDays = target.sslDays;
                if (cert && cert.valid_to) {
                    const validTo = new Date(cert.valid_to);
                    const now = new Date();
                    sslDays = Math.ceil((validTo - now) / (1000 * 60 * 60 * 24));
                }

                // Renewal Alert Trigger (#12)
                if (target.sslDays <= 0 && sslDays > 0) {
                    logEvent("RENEWAL", `SSL Certificate SUCCESSFULLY RENEWED for target: ${target.url}. New validity: ${sslDays} days.`);
                }

                // Scheduled Alert Threshold Rules (#11: 14d, 7d, 3d, 2d, 6h)
                evaluateAlertSchedules(target, sslDays);

                target.isUp = res.statusCode >= 200 && res.statusCode < 400;
                target.responseTime = responseTime;
                target.sslDays = sslDays;
                target.lastChecked = Date.now();

                resolve(target);
            });

            req.on('error', () => {
                target.isUp = false;
                target.responseTime = 0;
                target.lastChecked = Date.now();
                logEvent("ALERT", `CRITICAL: Target ${target.url} is DOWN.`);
                resolve(target);
            });

            req.setTimeout(5000, () => {
                req.destroy();
            });

            req.end();
        } catch (e) {
            target.isUp = false;
            resolve(target);
        }
    });
}

// Alert Threshold Scheduler Engine (#11)
function evaluateAlertSchedules(target, sslDays) {
    const hoursLeft = sslDays * 24;

    if (hoursLeft <= 6 && target.lastAlertSentStage !== '6hours') {
        logEvent("ALERT", `CRITICAL SSL ALERT: ${target.url} expires in under 6 HOURS!`);
        target.lastAlertSentStage = '6hours';
    } else if (sslDays <= 2 && target.lastAlertSentStage !== '2days') {
        logEvent("ALERT", `SSL Alert: ${target.url} expires in 2 Days.`);
        target.lastAlertSentStage = '2days';
    } else if (sslDays <= 3 && target.lastAlertSentStage !== '3days') {
        logEvent("ALERT", `SSL Alert: ${target.url} expires in 3 Days.`);
        target.lastAlertSentStage = '3days';
    } else if (sslDays <= 7 && target.lastAlertSentStage !== '7days') {
        logEvent("ALERT", `SSL Alert: ${target.url} expires in 7 Days.`);
        target.lastAlertSentStage = '7days';
    } else if (sslDays <= 14 && target.lastAlertSentStage !== '14days') {
        logEvent("ALERT", `SSL Notice: ${target.url} expires in 14 Days.`);
        target.lastAlertSentStage = '14days';
    }
}

function logEvent(type, message) {
    systemLogs.unshift({ id: Date.now(), timestamp: Date.now(), type, message });
}

// -------------------------------------------------------------------
// REST API ENDPOINTS
// -------------------------------------------------------------------

// Get Monitored URLs
app.get('/api/urls', async (req, res) => {
    res.json(monitoredUrls);
});

// Admin: Add Target URL (#3, #7, #8)
app.post('/api/admin/add-url', (req, res) => {
    const { url, category, tags, renewalUrl } = req.body;
    const newTarget = {
        id: Date.now(),
        url,
        category: category || "Production",
        tags: tags || ["API"],
        isUp: true,
        responseTime: 0,
        sslDays: 30,
        lastChecked: Date.now(),
        renewalUrl: renewalUrl || ""
    };
    monitoredUrls.push(newTarget);
    logEvent("INFO", `Admin added target URL: ${url}`);
    res.status(201).json(newTarget);
});

// Admin: Edit Target URL (#8)
app.put('/api/admin/update-url/:id', (req, res) => {
    const id = parseInt(req.params.id);
    const index = monitoredUrls.findIndex(u => u.id === id);
    if (index !== -1) {
        monitoredUrls[index] = { ...monitoredUrls[index], ...req.body };
        logEvent("INFO", `Admin updated target URL: ${monitoredUrls[index].url}`);
        return res.json(monitoredUrls[index]);
    }
    res.status(404).json({ error: "Target URL not found" });
});

// Admin: Delete Target URL (#8)
app.delete('/api/admin/delete-url/:id', (req, res) => {
    const id = parseInt(req.params.id);
    monitoredUrls = monitoredUrls.filter(u => u.id !== id);
    logEvent("INFO", `Admin deleted target ID: ${id}`);
    res.json({ message: "Target deleted" });
});

// Admin: SSL Renewal Websites Portals CRUD (#17)
app.get('/api/renewal-portals', (req, res) => res.json(renewalPortals));

app.post('/api/admin/add-portal', (req, res) => {
    const portal = { id: Date.now(), ...req.body };
    renewalPortals.push(portal);
    logEvent("INFO", `Admin registered Renewal Provider: ${portal.name}`);
    res.status(201).json(portal);
});

app.put('/api/admin/update-portal/:id', (req, res) => {
    const id = parseInt(req.params.id);
    const index = renewalPortals.findIndex(p => p.id === id);
    if (index !== -1) {
        renewalPortals[index] = { ...renewalPortals[index], ...req.body };
        return res.json(renewalPortals[index]);
    }
    res.status(404).json({ error: "Portal not found" });
});

app.delete('/api/admin/delete-portal/:id', (req, res) => {
    const id = parseInt(req.params.id);
    renewalPortals = renewalPortals.filter(p => p.id !== id);
    res.json({ message: "Portal deleted" });
});

// Logs Endpoint
app.get('/api/logs', (req, res) => res.json(systemLogs));

// Background Inspection Loop every 6 Hours (#11)
const SIX_HOURS = 6 * 60 * 60 * 1000;
setInterval(() => {
    monitoredUrls.forEach(target => checkTargetHealth(target));
}, SIX_HOURS);

app.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(` FOB SSL Monitor Server running on port ${PORT}`);
    console.log(`====================================================`);
});
