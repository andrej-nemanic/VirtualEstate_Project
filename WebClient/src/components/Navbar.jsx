import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="brand">🏠 Virtual Estate</div>
      <div className="links">
        <NavLink to="/" end>Domov</NavLink>
        <NavLink to="/dashboard">Nadzorna plošča</NavLink>
        {user && <NavLink to="/admin">Admin</NavLink>}
      </div>
      {user ? (
        <>
          <span className="user">{user.name} ({user.type})</span>
          <button className="secondary" onClick={handleLogout}>Odjava</button>
        </>
      ) : (
        <>
          <NavLink to="/login">Prijava</NavLink>
          <NavLink to="/register">Registracija</NavLink>
        </>
      )}
    </nav>
  );
}
