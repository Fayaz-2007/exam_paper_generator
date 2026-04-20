/**
 * api.js
 * Centralised fetch wrapper for all backend API calls.
 */

const API_BASE = 'http://localhost:8080';

/**
 * Returns Authorization header with the stored Supabase JWT.
 */
function getAuthHeaders() {
  const token = localStorage.getItem('sqpg_token');
  return { 'Authorization': `Bearer ${token}` };
}

/**
 * Upload a question bank PDF.
 * POST /api/upload
 * @param {FormData} formData - contains file, subject, exam
 * @returns {Promise<string>} summary text from backend
 */
async function apiUpload(formData) {
  const res = await fetch(`${API_BASE}/api/upload`, {
    method: 'POST',
    body: formData,
    headers: getAuthHeaders()
  });
  if (!res.ok) throw new Error(await res.text());
  return res.text();
}

/**
 * Generate a question paper.
 * GET /api/generate?subject=&exam=&difficulty=
 * @param {string} subject
 * @param {string} exam
 * @param {string} [difficulty='medium'] - easy | medium | hard
 * @returns {Promise<Object>} { subject, exam, partA:[{q,unit}], partB:[{a,b,unit}] }
 */
async function apiGenerate(subject, exam, difficulty = 'medium') {
  const res = await fetch(
    `${API_BASE}/api/generate?subject=${encodeURIComponent(subject)}&exam=${encodeURIComponent(exam)}&difficulty=${encodeURIComponent(difficulty)}`,
    { headers: getAuthHeaders() }
  );
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

/**
 * Export a generated question paper as a PDF blob.
 * GET /api/export?subject=&exam=
 * @param {string} subject
 * @param {string} exam
 * @returns {Promise<Blob>}
 */
async function apiExport(subject, exam) {
  const res = await fetch(
    `${API_BASE}/api/export?subject=${encodeURIComponent(subject)}&exam=${encodeURIComponent(exam)}`,
    { headers: getAuthHeaders() }
  );
  if (!res.ok) throw new Error(await res.text());
  return res.blob();
}
