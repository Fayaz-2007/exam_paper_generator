/**
 * auth.js
 * Handles JWT storage, role management, and redirect logic.
 * All Supabase OAuth integration points are marked with TODO comments.
 */

/**
 * Save authentication data to localStorage
 * @param {string} token - JWT token from Supabase
 * @param {{role: string, email: string, name: string}} user - User payload
 */
function saveAuth(token, user) {
  localStorage.setItem('sqpg_token', token);
  localStorage.setItem('sqpg_user', JSON.stringify(user));
}

/**
 * Clear authentication data from localStorage (logout)
 */
function clearAuth() {
  localStorage.removeItem('sqpg_token');
  localStorage.removeItem('sqpg_user');
}

/**
 * Get the current logged-in user object
 * @returns {{role: string, email: string, name: string} | null}
 */
function getUser() {
  try {
    return JSON.parse(localStorage.getItem('sqpg_user'));
  } catch {
    return null;
  }
}

/**
 * Get the stored JWT token
 * @returns {string | null}
 */
function getToken() {
  return localStorage.getItem('sqpg_token');
}

/**
 * Check if the user is authenticated
 * @returns {boolean}
 */
function isAuthenticated() {
  return !!getToken() && !!getUser();
}

/**
 * Redirect user to their role-appropriate page
 * @param {string} role - 'teacher' or 'hod'
 */
function redirectByRole(role) {
  if (role === 'teacher') window.location.href = 'teacher.html';
  else if (role === 'hod') window.location.href = 'hod.html';
}

/**
 * Guard function — call at top of protected pages.
 * Redirects to login if not authenticated, or if role doesn't match.
 * @param {string} requiredRole - Expected role for this page
 */
function requireAuth(requiredRole) {
  if (!isAuthenticated()) {
    window.location.href = 'login.html';
    return;
  }
  const user = getUser();
  if (requiredRole && user.role !== requiredRole) {
    // Wrong role — redirect to their correct page
    redirectByRole(user.role);
  }
}

/**
 * Logout the current user and return to login
 */
function logout() {
  clearAuth();
  window.location.href = 'login.html';
}

// TODO: Replace mock auth with real Supabase OAuth
// import { createClient } from '@supabase/supabase-js'
// const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY)
// async function signInWithGoogle() {
//   const { error } = await supabase.auth.signInWithOAuth({ provider: 'google' })
//   if (error) console.error(error)
// }
