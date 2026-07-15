import React, { useEffect, useState } from 'react';
import { fetchApplications, fetchAnalytics, fetchLinkGmailUrl, syncGmail, fetchLinkedAccounts, unlinkAccount, createApplication, updateApplication, deleteApplication } from './api';
import { LogOut, RefreshCw, Briefcase, TrendingUp, Filter, Mail, Plus, Edit2, Trash2, Archive } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import ApplicationModal from './components/ApplicationModal';

export default function Dashboard() {
  const [apps, setApps] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [linking, setLinking] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [linkedAccounts, setLinkedAccounts] = useState([]);
  const [unlinkingId, setUnlinkingId] = useState(null);
  const [activeTab, setActiveTab] = useState('active');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedApp, setSelectedApp] = useState(null);
  const navigate = useNavigate();

  const getDaysAgo = (dateStr) => {
    if (!dateStr) return '';
    const diff = new Date() - new Date(dateStr);
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days === 0) return 'Today';
    if (days === 1) return '1 day ago';
    return `${days} days ago`;
  };

  const getCompanyLogo = (companyName) => {
    // Basic formatting for clearbit logo (removes symbols, spaces, lowercase)
    const cleanName = companyName.toLowerCase().replace(/[^a-z0-9]/g, '');
    return `https://logo.clearbit.com/${cleanName}.com`;
  };

  const handleLinkGmail = async () => {
    try {
      setLinking(true);
      const res = await fetchLinkGmailUrl();
      window.location.href = res.authorizationUrl;
    } catch (err) {
      console.error(err);
      alert('Failed to get Gmail linking URL.');
      setLinking(false);
    }
  };

  const handleSync = async () => {
    try {
      setSyncing(true);
      await syncGmail();
      await loadData();
    } catch (err) {
      console.error(err);
      alert('Failed to sync with Gmail.');
    } finally {
      setSyncing(false);
    }
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const authHeaders = await import('./api').then(m => m.fetchAuthHeaders()).catch(e => ({error: e.message}));
      console.log('DEBUG HEADERS REACHING BACKEND:', authHeaders);

      const [appsData, analyticsData, accountsData] = await Promise.all([
        fetchApplications(),
        fetchAnalytics(),
        fetchLinkedAccounts()
      ]);
      
      const sixMonthsAgo = new Date();
      sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
      
      const processedApps = appsData.map(app => ({
        ...app,
        isArchived: new Date(app.lastUpdatedAt || app.appliedDate) < sixMonthsAgo
      }));
      
      setApps(processedApps);
      setAnalytics(analyticsData);
      if (accountsData && accountsData.length > 0) {
        setLinkedAccounts(accountsData);
      } else {
        setLinkedAccounts([]);
      }
    } catch (err) {
      console.error('loadData error:', err);
      // Only redirect to login for authentication errors
      if (err.message && (err.message.includes('401') || err.message.includes('Unauthorized') || err.message.includes('403'))) {
        navigate('/');
      } else {
        // For other errors (network, CORS, etc.) show in console but don't redirect
        // This prevents getting kicked out for temporary backend errors
        console.warn('Non-auth error in loadData, staying on dashboard:', err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('jwt');
    navigate('/');
  };

  const handleUnlink = async (accountId, email) => {
    if (!window.confirm(`Are you sure you want to unlink ${email}?`)) return;
    try {
      setUnlinkingId(accountId);
      await unlinkAccount(accountId);
      await loadData();
    } catch (err) {
      console.error(err);
      alert('Failed to unlink account.');
    } finally {
      setUnlinkingId(null);
    }
  };

  const handleOpenModal = (app = null) => {
    setSelectedApp(app);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setSelectedApp(null);
    setIsModalOpen(false);
  };

  const handleSaveApplication = async (formData) => {
    try {
      if (selectedApp) {
        // Optimistic update
        setApps(prev => prev.map(a => a.id === selectedApp.id ? { ...a, ...formData, isArchived: formData.status === 'OFFER' ? true : a.isArchived } : a));
        handleCloseModal();
        await updateApplication(selectedApp.id, formData);
      } else {
        handleCloseModal();
        const newApp = await createApplication(formData);
        if (formData.status === 'OFFER') newApp.isArchived = true;
        setApps(prev => [newApp, ...prev]);
      }
      // Give async microservices a moment, then fetch fresh analytics in background (no loading screen)
      await new Promise(r => setTimeout(r, 800));
      fetchAnalytics().then(data => setAnalytics(data)).catch(console.error);
    } catch (err) {
      console.error(err);
      alert('Failed to save application.');
      loadData(); // Revert to source of truth on failure
    }
  };

  const handleDeleteApplication = async (e, id) => {
    e.stopPropagation();
    if (!window.confirm('Are you sure you want to delete this application? This action cannot be undone.')) return;
    try {
      setApps(prev => prev.filter(a => a.id !== id));
      await deleteApplication(id);
      
      await new Promise(r => setTimeout(r, 800));
      fetchAnalytics().then(data => setAnalytics(data)).catch(console.error);
    } catch (err) {
      console.error(err);
      alert('Failed to delete application.');
      loadData();
    }
  };

  const handleArchiveApplication = async (e, app) => {
    e.stopPropagation();
    try {
      setApps(prev => prev.map(a => a.id === app.id ? { ...a, isArchived: true } : a));
      await updateApplication(app.id, { isArchived: true });
      
      await new Promise(r => setTimeout(r, 800));
      fetchAnalytics().then(data => setAnalytics(data)).catch(console.error);
    } catch (err) {
      console.error(err);
      alert('Failed to archive application.');
      loadData();
    }
  };

  return (
    <div style={{ padding: '2rem 0' }}>
      <div className="container">
        
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
          <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Briefcase color="var(--primary-accent)" /> 
            Job Tracker
          </h2>
          <div style={{ display: 'flex', gap: '1rem' }}>
            <div style={{ display: 'flex', gap: '0.25rem', alignItems: 'center' }}>
              <button className="btn-primary" onClick={() => handleOpenModal()} style={{ padding: '0.5rem 1rem', background: 'var(--bg-surface-light)', border: '1px solid var(--primary-accent)', color: 'var(--primary-accent)', boxShadow: 'none', borderRadius: 'var(--border-radius)' }}>
                <Plus size={16} /> Manual Add
              </button>
              <button className="btn-primary" onClick={handleLinkGmail} disabled={linking} style={{ padding: '0.5rem 1rem', background: 'var(--primary-accent)', border: 'none', color: 'white', boxShadow: '0 4px 12px rgba(99, 102, 241, 0.3)', borderRadius: 'var(--border-radius)', marginLeft: '0.5rem' }}>
                <Mail size={16} /> {linking ? 'Redirecting...' : 'Link Gmail'}
              </button>
            </div>
            <button className="btn-primary" onClick={handleSync} disabled={syncing} style={{ padding: '0.5rem 1rem', background: 'var(--bg-surface-light)', border: '1px solid var(--border-color)', color: 'var(--text-main)', boxShadow: 'none' }}>
              <RefreshCw size={16} className={syncing ? 'spin' : ''} /> {syncing ? 'Syncing...' : 'Sync'}
            </button>
            <button className="btn-primary" onClick={handleLogout} style={{ padding: '0.5rem 1rem', background: 'transparent', border: '1px solid var(--danger)', color: 'var(--danger)', boxShadow: 'none' }}>
              <LogOut size={16} /> Logout
            </button>
          </div>
        </div>

        {/* Linked Accounts List */}
        {linkedAccounts.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '2rem', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontWeight: 600 }}>Linked Accounts:</span>
            {linkedAccounts.map(acc => (
              <div key={acc.id} style={{ display: 'flex', alignItems: 'center', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)', padding: '0.25rem 0.75rem', borderRadius: '16px', gap: '0.5rem' }}>
                <Mail size={12} color="var(--primary-accent)" />
                <span style={{ fontSize: '0.85rem' }}>{acc.gmailAddress}</span>
                <button 
                  onClick={() => handleUnlink(acc.id, acc.gmailAddress)} 
                  disabled={unlinkingId === acc.id}
                  style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: 0, display: 'flex', alignItems: 'center', opacity: unlinkingId === acc.id ? 0.5 : 1 }} 
                  title="Unlink Account">
                   <LogOut size={12} /> 
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Analytics Bar */}
        {analytics && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem', marginBottom: '3rem' }}>
            
            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: 600, textTransform: 'uppercase' }}>Total Applications</span>
              <span style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--text-main)' }}>{apps.length}</span>
            </div>

            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: 600, textTransform: 'uppercase' }}>Online Assessments</span>
              <span style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--primary-accent)' }}>{apps.filter(a => a.status === 'ASSESSMENT' || a.assessmentDate || (a.notes && a.notes.toLowerCase().includes('assessment'))).length}</span>
            </div>

            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: 600, textTransform: 'uppercase' }}>Interviews</span>
              <span style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--warning)' }}>{apps.filter(a => a.status === 'INTERVIEW' && !(a.assessmentDate || (a.notes && a.notes.toLowerCase().includes('assessment')))).length}</span>
            </div>

            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem', fontWeight: 600, textTransform: 'uppercase' }}>Offers</span>
              <span style={{ fontSize: '2.5rem', fontWeight: 700, color: 'var(--secondary-accent)' }}>{apps.filter(a => a.status === 'OFFER').length}</span>
            </div>

          </div>
        )}

        {/* Applications List Header with Tabs */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h3 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Filter size={20} color="var(--text-muted)" />
            Applications
          </h3>
          <div style={{ display: 'flex', gap: '0.5rem', background: 'rgba(0,0,0,0.2)', padding: '0.25rem', borderRadius: '8px' }}>
            <button 
              onClick={() => setActiveTab('active')}
              style={{ 
                padding: '0.5rem 1rem', 
                background: activeTab === 'active' ? 'var(--primary-accent)' : 'transparent',
                color: activeTab === 'active' ? 'white' : 'var(--text-muted)',
                border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, transition: 'all 0.2s'
              }}>
              Active ({apps.filter(a => a.status !== 'REJECTED' && !a.isArchived).length})
            </button>
            <button 
              onClick={() => setActiveTab('rejected')}
              style={{ 
                padding: '0.5rem 1rem', 
                background: activeTab === 'rejected' ? 'var(--danger)' : 'transparent',
                color: activeTab === 'rejected' ? 'white' : 'var(--text-muted)',
                border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, transition: 'all 0.2s'
              }}>
              Rejected ({apps.filter(a => a.status === 'REJECTED' && !a.isArchived).length})
            </button>
            <button 
              onClick={() => setActiveTab('archived')}
              style={{ 
                padding: '0.5rem 1rem', 
                background: activeTab === 'archived' ? 'var(--warning)' : 'transparent',
                color: activeTab === 'archived' ? 'white' : 'var(--text-muted)',
                border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, transition: 'all 0.2s'
              }}>
              Archived ({apps.filter(a => a.isArchived).length})
            </button>
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-muted)' }}>Loading...</div>
        ) : apps.length === 0 ? (
          <div className="glass-panel" style={{ textAlign: 'center', padding: '4rem 2rem', color: 'var(--text-muted)' }}>
            <Briefcase size={48} style={{ opacity: 0.3, marginBottom: '1rem' }} />
            <p>No applications found yet. Sync your Gmail to start tracking!</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {(activeTab === 'active' ? apps.filter(a => a.status !== 'REJECTED' && !a.isArchived) : 
              (activeTab === 'rejected' ? apps.filter(a => a.status === 'REJECTED' && !a.isArchived) : 
                apps.filter(a => a.isArchived)
              )
            ).map(app => (
              <div key={app.id} className="glass-panel fade-in" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem', transition: 'transform 0.2s ease', opacity: app.isArchived ? 0.7 : 1 }} onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.01)'} onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}>
                
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    {app.company && (
                      <img 
                        src={getCompanyLogo(app.company)} 
                        alt={app.company} 
                        onError={(e) => { e.target.style.display = 'none'; }} 
                        style={{ width: '40px', height: '40px', borderRadius: '8px', objectFit: 'cover', background: 'white' }} 
                      />
                    )}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                      <h4 style={{ margin: 0, fontSize: '1.25rem' }}>{app.company || app.role}</h4>
                      <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        {app.role && app.role !== 'Unknown Role' ? `${app.role} • ` : ''}Applied on {new Date(app.appliedDate).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' })}
                        {app.sourceEmailAddress && (
                          <span style={{ background: 'rgba(255,255,255,0.1)', padding: '0.15rem 0.5rem', borderRadius: '12px', fontSize: '0.75rem', color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                            <Mail size={10} /> {app.sourceEmailAddress}
                          </span>
                        )}
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                    <span className={`badge ${(app.status === 'ASSESSMENT' || app.assessmentDate || (app.notes && app.notes.toLowerCase().includes('assessment'))) ? 'assessment' : app.status.toLowerCase()}`}>
                      {(app.status === 'ASSESSMENT' || app.assessmentDate || (app.notes && app.notes.toLowerCase().includes('assessment'))) ? 'ASSESSMENT' : app.status}
                    </span>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button onClick={() => handleOpenModal(app)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-main)', padding: '4px' }} title="Edit">
                        <Edit2 size={16} />
                      </button>
                      {!app.isArchived && (
                        <button onClick={(e) => handleArchiveApplication(e, app)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--warning)', padding: '4px' }} title="Archive">
                          <Archive size={16} />
                        </button>
                      )}
                      <button onClick={(e) => handleDeleteApplication(e, app.id)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--danger)', padding: '4px' }} title="Delete">
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                </div>

                {/* Progress Bar (Visual enhancement) */}
                <div style={{ width: '100%', height: '4px', background: 'rgba(255,255,255,0.1)', borderRadius: '2px', overflow: 'hidden', margin: '0.5rem 0' }}>
                  <div style={{ 
                    height: '100%', 
                    background: app.status === 'REJECTED' ? 'var(--danger)' : app.status === 'OFFER' ? 'var(--secondary-accent)' : 'var(--primary-accent)', 
                    width: app.status === 'APPLIED' ? '0%' : ((app.status === 'ASSESSMENT' || app.assessmentDate || (app.notes && app.notes.toLowerCase().includes('assessment'))) ? '50%' : (app.status === 'INTERVIEW' ? '75%' : '100%')),
                    transition: 'width 0.5s ease'
                  }}></div>
                </div>
                  
                {/* Notes and Actionables */}
                {(app.interviewTime || app.assessmentDate || app.interviewLink || app.notes || app.sourceMessageId) && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', background: 'rgba(0,0,0,0.2)', padding: '1rem', borderRadius: '8px' }}>
                    {app.interviewTime && (
                      <span style={{ color: 'var(--warning)', fontSize: '0.9rem', fontWeight: 500 }}>
                        {(app.status === 'ASSESSMENT' || app.assessmentDate || (app.notes && app.notes.toLowerCase().includes('assessment'))) ? '🕒 Time' : '📅 Interview'}: {app.interviewTime}
                      </span>
                    )}
                    {app.assessmentDate && (
                      <span style={{ color: 'var(--warning)', fontSize: '0.9rem', fontWeight: 500 }}>
                        📝 Assessment Date: {app.assessmentDate}
                      </span>
                    )}
                    
                    {app.notes && (
                      <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)', fontStyle: 'italic', borderLeft: '2px solid var(--border-color)', paddingLeft: '0.5rem', margin: '0.5rem 0', whiteSpace: 'pre-wrap' }}>
                        {app.notes}
                      </div>
                    )}

                    <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginTop: '0.25rem' }}>
                      {app.interviewLink && (
                        <a href={app.interviewLink.startsWith('http') ? app.interviewLink : `https://${app.interviewLink}`} 
                           target="_blank" rel="noreferrer" 
                           style={{ display: 'inline-block', padding: '0.5rem 1rem', background: 'var(--primary-accent)', color: 'white', textDecoration: 'none', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 600 }}>
                          {(app.status === 'ASSESSMENT' || app.assessmentDate || (app.notes && app.notes.toLowerCase().includes('assessment'))) ? 'Take Assessment' : 'Join Interview'}
                        </a>
                      )}
                      
                      {app.sourceMessageId && (
                        <a href={`https://mail.google.com/mail/u/0/#all/${app.sourceMessageId}`}
                           target="_blank" rel="noreferrer"
                           style={{ display: 'inline-block', padding: '0.5rem 1rem', background: 'transparent', color: 'var(--text-main)', border: '1px solid var(--border-color)', textDecoration: 'none', borderRadius: '6px', fontSize: '0.85rem', fontWeight: 600 }}>
                          <Mail size={14} style={{ marginRight: '0.25rem', verticalAlign: 'text-bottom' }} /> View Email
                        </a>
                      )}
                    </div>
                  </div>
                )}

              </div>
            ))}
          </div>
        )}

        <ApplicationModal 
          isOpen={isModalOpen} 
          onClose={handleCloseModal} 
          onSave={handleSaveApplication} 
          application={selectedApp} 
        />

      </div>
    </div>
  );
}
