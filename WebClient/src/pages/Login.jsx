import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Login() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const res = await login(email, password);
    if (res.ok) navigate('/', { replace: true });
    else setError(res.message);
  };

  return (
    <div className="auth-page">
      <h1>Prijava</h1>
      <form onSubmit={handleSubmit}>
        {error && <div className="alert alert-error">{error}</div>}
        <div className="form-group">
          <label>E-pošta</label>
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="ime@primer.si" required autoFocus />
        </div>
        <div className="form-group">
          <label>Geslo</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="••••••••" required />
        </div>
        <button type="submit" disabled={loading} style={{ width: '100%' }}>
          {loading ? 'Prijavljam…' : 'Prijava'}
        </button>
      </form>
      <p className="auth-footer">
        Še nimaš računa? <Link to="/register">Registracija</Link>
      </p>
    </div>
  );
}
