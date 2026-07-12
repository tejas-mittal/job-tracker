import React, { useState, useEffect } from 'react';
import './ApplicationModal.css';

function ApplicationModal({ isOpen, onClose, onSave, application }) {
  const [formData, setFormData] = useState({
    company: '',
    role: '',
    status: 'APPLIED',
    appliedDate: '',
    interviewTime: '',
    assessmentDate: '',
    interviewLink: '',
    notes: ''
  });

  useEffect(() => {
    if (application) {
      setFormData({
        company: application.company || '',
        role: application.role || '',
        status: application.status || 'APPLIED',
        appliedDate: application.appliedDate || '',
        interviewTime: application.interviewTime || '',
        assessmentDate: application.assessmentDate || '',
        interviewLink: application.interviewLink || '',
        notes: application.notes || ''
      });
    } else {
      setFormData({
        company: '',
        role: '',
        status: 'APPLIED',
        appliedDate: new Date().toISOString().split('T')[0],
        interviewTime: '',
        assessmentDate: '',
        interviewLink: '',
        notes: ''
      });
    }
  }, [application, isOpen]);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <h2>{application ? 'Edit Application' : 'Add Application'}</h2>
        <form onSubmit={handleSubmit} className="application-form">
          <div className="form-group">
            <label>Company*</label>
            <input type="text" name="company" value={formData.company} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Role</label>
            <input type="text" name="role" value={formData.role} onChange={handleChange} />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Status</label>
              <select name="status" value={formData.status} onChange={handleChange}>
                <option value="APPLIED">Applied</option>
                <option value="ASSESSMENT">Assessment</option>
                <option value="INTERVIEW">Interview</option>
                <option value="OFFER">Offer</option>
                <option value="REJECTED">Rejected</option>
                <option value="WITHDRAWN">Withdrawn</option>
              </select>
            </div>
            <div className="form-group">
              <label>Applied Date</label>
              <input type="date" name="appliedDate" value={formData.appliedDate} onChange={handleChange} />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Interview Time</label>
              <input type="datetime-local" name="interviewTime" value={formData.interviewTime} onChange={handleChange} />
            </div>
            <div className="form-group">
              <label>Assessment Date</label>
              <input type="datetime-local" name="assessmentDate" value={formData.assessmentDate} onChange={handleChange} />
            </div>
          </div>
          <div className="form-group">
            <label>Interview Link</label>
            <input type="url" name="interviewLink" value={formData.interviewLink} onChange={handleChange} placeholder="https://..." />
          </div>
          <div className="form-group">
            <label>Notes</label>
            <textarea name="notes" value={formData.notes} onChange={handleChange} rows="4" placeholder="Add personal notes here..."></textarea>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary">Save</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ApplicationModal;
