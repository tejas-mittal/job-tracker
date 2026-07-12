// api.js
// Handles all communication with the api-gateway (http://localhost:8080)

const API_BASE = 'http://localhost:8080';

// Helper to get auth headers if we have a token
function getHeaders() {
  const token = localStorage.getItem('jwt');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

export async function fetchApplications() {
  const res = await fetch(`${API_BASE}/api/applications`, {
    headers: getHeaders()
  });
  if (!res.ok) throw new Error('Failed to fetch applications');
  return res.json();
}

export async function fetchAnalytics() {
  const res = await fetch(`${API_BASE}/analytics`, {
    headers: getHeaders()
  });
  if (!res.ok) throw new Error('Failed to fetch analytics');
  return res.json();
}

export function getLoginUrl() {
  // Hit auth-service directly for OAuth2 because Spring Security's default endpoints
  // don't have the /auth/ prefix mapped in the gateway.
  return `http://localhost:8081/oauth2/authorization/google`;
}

export async function devLogin() {
  const res = await fetch(`${API_BASE}/auth/dev-token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      googleId: "114397273750381720799",
      email: "mittal.tejas.29@gmail.com",
      displayName: "Tejas Mittal"
    })
  });
  if (!res.ok) throw new Error("Dev login failed");
  return res.json();
}

export async function fetchLinkGmailUrl() {
  const res = await fetch(`${API_BASE}/api/email/accounts/link`, {
    headers: getHeaders()
  });
  if (!res.ok) throw new Error('Failed to fetch link URL');
  return res.json();
}

export async function fetchLinkedAccounts() {
  const res = await fetch(`${API_BASE}/api/email/accounts`, {
    headers: getHeaders()
  });
  if (!res.ok) return [];
  return res.json();
}

export async function syncGmail() {
  const res = await fetch(`${API_BASE}/api/email/accounts/sync`, {
    method: 'POST',
    headers: getHeaders()
  });
  if (!res.ok) throw new Error('Failed to trigger manual sync');
}

export async function unlinkAccount(id) {
  const res = await fetch(`${API_BASE}/api/email/accounts/${id}`, {
    method: 'DELETE',
    headers: getHeaders()
  });
}

export async function createApplication(data) {
  const res = await fetch(`${API_BASE}/api/applications`, {
    method: 'POST',
    headers: getHeaders(),
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error('Failed to create application');
  return res.json();
}

export async function updateApplication(id, data) {
  const res = await fetch(`${API_BASE}/api/applications/${id}`, {
    method: 'PATCH',
    headers: getHeaders(),
    body: JSON.stringify(data)
  });
  if (!res.ok) throw new Error('Failed to update application');
  return res.json();
}

export async function deleteApplication(id) {
  const res = await fetch(`${API_BASE}/api/applications/${id}`, {
    method: 'DELETE',
    headers: getHeaders()
  });
  if (!res.ok) throw new Error('Failed to delete application');
}
