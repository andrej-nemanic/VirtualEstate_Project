import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { validateEmail, validatePassword, PASSWORD_MIN_LENGTH } from '../constants.js';

export default function Register() {
  const { register, loading } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '', email: '', password: '', confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    if (fieldErrors[e.target.name]) {
      const next = { ...fieldErrors };
      delete next[e.target.name];
      setFieldErrors(next);
    }
  };

  const validate = () => {
    const errs = {};
    if (!form.name.trim()) errs.name = 'Ime je obvezno.';
    const emailErr = validateEmail(form.email);
    if (emailErr) errs.email = emailErr;
    const passErr = validatePassword(form.password);
    if (passErr) errs.password = passErr;
    if (form.password !== form.confirmPassword) errs.confirmPassword = 'Gesli se ne ujemata.';
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const errs = validate();
    if (Object.keys(errs).length) {
      setFieldErrors(errs);
      return;
    }
    const { confirmPassword, ...payload } = form;
    const res = await register({ ...payload, email: payload.email.trim().toLowerCase() });
    if (res.ok) navigate('/dashboard');
    else setError(res.message);
  };

  return (
    <div className="auth-page">
      <h1>Registracija</h1>
      <form onSubmit={handleSubmit} noValidate>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="form-group">
          <label>Ime</label>
          <input name="name" value={form.name} onChange={handleChange} required />
          {fieldErrors.name && <div style={{ color: '#dc2626', fontSize: 12, marginTop: 4 }}>{fieldErrors.name}</div>}
        </div>
        <div className="form-group">
          <label>E-pošta</label>
          <input name="email" type="email" value={form.email} onChange={handleChange} required />
          {fieldErrors.email && <div style={{ color: '#dc2626', fontSize: 12, marginTop: 4 }}>{fieldErrors.email}</div>}
        </div>
        <div className="form-group">
          <label>Geslo (najmanj {PASSWORD_MIN_LENGTH} znakov, vsaj 1 črka in 1 številka)</label>
          <input name="password" type="password" value={form.password} onChange={handleChange} required />
          {fieldErrors.password && <div style={{ color: '#dc2626', fontSize: 12, marginTop: 4 }}>{fieldErrors.password}</div>}
        </div>
        <div className="form-group">
          <label>Potrdi geslo</label>
          <input name="confirmPassword" type="password" value={form.confirmPassword} onChange={handleChange} required />
          {fieldErrors.confirmPassword && <div style={{ color: '#dc2626', fontSize: 12, marginTop: 4 }}>{fieldErrors.confirmPassword}</div>}
        </div>
        <button type="submit" disabled={loading} style={{ width: '100%' }}>
          {loading ? 'Registriram...' : 'Registracija'}
        </button>
      </form>
      <p style={{ marginTop: 16, fontSize: 14, textAlign: 'center' }}>
        Že imaš račun? <Link to="/login">Prijava</Link>
      </p>
    </div>
  );
}
