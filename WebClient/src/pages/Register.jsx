import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Register() {
  const { register, loading } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '', email: '', password: '', type: 'buyer'
  });
  const [error, setError] = useState('');

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const res = await register(form);
    if (res.ok) navigate('/dashboard');
    else setError(res.message);
  };

  return (
    <div className="auth-page">
      <h1>Registracija</h1>
      <form onSubmit={handleSubmit}>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="form-group">
          <label>Ime</label>
          <input name="name" value={form.name} onChange={handleChange} required />
        </div>
        <div className="form-group">
          <label>E-pošta</label>
          <input name="email" type="email" value={form.email} onChange={handleChange} required />
        </div>
        <div className="form-group">
          <label>Geslo</label>
          <input name="password" type="password" value={form.password} onChange={handleChange} required minLength={4} />
        </div>
        <div className="form-group">
          <label>Tip uporabnika</label>
          <select name="type" value={form.type} onChange={handleChange}>
            <option value="buyer">Kupec</option>
            <option value="owner">Lastnik</option>
          </select>
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
