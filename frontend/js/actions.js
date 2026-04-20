/**
 * actions.js
 * Page-specific UI event handlers and shared utilities.
 * Import this after auth.js and api.js on each page.
 */

/**
 * Generic debounce utility
 * @param {Function} fn
 * @param {number} delay
 */
function debounce(fn, delay) {
  let timer;
  return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay); };
}

/**
 * Show a toast notification (shared across pages)
 * Expects a #toast element on the page.
 * @param {string} msg
 * @param {'success'|'error'|'info'} type
 */
function showToast(msg, type = 'success') {
  const t = document.getElementById('toast');
  if (!t) return;
  t.className = `toast ${type} show`;
  t.textContent = msg;
  setTimeout(() => t.classList.remove('show'), 3500);
}

/**
 * Format bytes to human-readable string
 * @param {number} bytes
 * @returns {string}
 */
function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
}

/**
 * Save generated paper data to localStorage for preview page
 * @param {Object} data - { partA, partB, subject, exam }
 */
function savePaperToLocalStorage(data) {
  localStorage.setItem('sqpg_paper', JSON.stringify(data));
}

/**
 * Read generated paper data from localStorage
 * @returns {Object|null}
 */
function getPaperFromLocalStorage() {
  try {
    return JSON.parse(localStorage.getItem('sqpg_paper'));
  } catch {
    return null;
  }
}
