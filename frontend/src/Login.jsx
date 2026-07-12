import React, { useState } from 'react';
import { getLoginUrl, devLogin } from './api';
import { Mail, Briefcase, TrendingUp, Code } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function Login() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const handleLogin = () => {
    window.location.href = getLoginUrl();
  };

  const handleDevLogin = async () => {
    try {
      setLoading(true);
      const data = await devLogin();
      localStorage.setItem('jwt', data.access_token);
      navigate('/dashboard');
    } catch (err) {
      console.error(err);
      alert('Dev login failed');
      setLoading(false);
    }
  };

  return (
    <div className="flex-center" style={{ minHeight: '100vh', background: 'var(--bg-base)' }}>
      <div className="container" style={{ maxWidth: '1000px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '4rem' }}>
        
        {/* Left Side: Hero Text */}
        <div style={{ flex: 1 }} className="fade-in">
          <h1 style={{ fontSize: '3.5rem', lineHeight: 1.1, marginBottom: '1.5rem' }}>
            Track Your <br />
            <span style={{ color: 'var(--primary-accent)' }}>Dream Job.</span>
          </h1>
          <p style={{ fontSize: '1.125rem', marginBottom: '2.5rem', maxWidth: '400px' }}>
            Connect your Gmail to automatically detect job applications, interview invites, and offers. No manual data entry required.
          </p>
          <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
            <button className="btn-primary" onClick={handleLogin} style={{ padding: '1rem 2rem', fontSize: '1.125rem', borderRadius: '30px' }}>
              <Mail size={20} />
              Continue with Google
            </button>
          </div>
        </div>

        {/* Right Side: Glassmorphism Graphic */}
        <div style={{ flex: 1, position: 'relative', animationDelay: '0.2s' }} className="fade-in">
          {/* Decorative glowing orb */}
          <div style={{
            position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
            width: '300px', height: '300px', background: 'var(--primary-accent)',
            filter: 'blur(100px)', opacity: 0.3, zIndex: 0, borderRadius: '50%'
          }}></div>

          <div className="glass-panel" style={{ padding: '2rem', position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(59, 130, 246, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--primary-accent)' }}>
                <Briefcase size={24} />
              </div>
              <div>
                <h4 style={{ margin: 0 }}>Automated Tracking</h4>
                <p style={{ margin: 0, fontSize: '0.875rem' }}>Reads "Thank you for applying" emails</p>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--secondary-accent)' }}>
                <TrendingUp size={24} />
              </div>
              <div>
                <h4 style={{ margin: 0 }}>Smart Analytics</h4>
                <p style={{ margin: 0, fontSize: '0.875rem' }}>Visualize your funnel conversion</p>
              </div>
            </div>
            
          </div>
        </div>

      </div>
    </div>
  );
}
