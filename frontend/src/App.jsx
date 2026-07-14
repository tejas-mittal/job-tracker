import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import Login from './Login';
import Dashboard from './Dashboard';

// A simple component to handle the OAuth2 redirect callback
function OAuth2Callback() {
  const navigate = useNavigate();

  useEffect(() => {
    const search = window.location.search;
    console.log('OAuth2Callback search:', search);
    const urlParams = new URLSearchParams(search);
    const token = urlParams.get('token');
    
    if (token) {
      console.log('Token found, saving to localStorage');
      localStorage.setItem('jwt', token);
      navigate('/dashboard');
    } else {
      console.error('No token found in URL! URL was:', window.location.href);
      // Wait 3 seconds so we can see the console error before redirecting
      setTimeout(() => navigate('/'), 3000);
    }
  }, [navigate]);

  return <div style={{ color: 'white', padding: '2rem', textAlign: 'center' }}>Processing login... Please check browser console if stuck.</div>;
}

// Protected route component
function RequireAuth({ children }) {
  const hasToken = localStorage.getItem('jwt');
  if (!hasToken) {
    console.warn("RequireAuth: No JWT found, redirecting to login");
    return <Navigate to="/" replace />;
  }
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
