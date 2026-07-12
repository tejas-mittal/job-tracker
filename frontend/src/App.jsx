import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import Login from './Login';
import Dashboard from './Dashboard';

// A simple component to handle the OAuth2 redirect callback
function OAuth2Callback() {
  const navigate = useNavigate();

  useEffect(() => {
    // In our backend setup, auth-service sets a cookie, or we might pass the token in the URL params.
    // For local dev with API gateway, if auth-service redirects here with a token, we grab it.
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    
    if (token) {
      localStorage.setItem('jwt', token);
    }
    
    // Redirect to dashboard whether token is present or we assume cookie session is active
    navigate('/dashboard');
  }, [navigate]);

  return <div style={{ color: 'white', padding: '2rem', textAlign: 'center' }}>Authenticating...</div>;
}

// Protected route component
function RequireAuth({ children }) {
  // In a real app we'd check if JWT exists or session is valid. 
  // For now, if there's no JWT and no cookie, API calls will just 401 and redirect to Login.
  const hasToken = localStorage.getItem('jwt');
  // Optional: if (!hasToken) return <Navigate to="/" />;
  return children;
}

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/oauth2/callback" element={<OAuth2Callback />} />
        <Route 
          path="/dashboard" 
          element={
            <RequireAuth>
              <Dashboard />
            </RequireAuth>
          } 
        />
      </Routes>
    </Router>
  );
}

export default App;
