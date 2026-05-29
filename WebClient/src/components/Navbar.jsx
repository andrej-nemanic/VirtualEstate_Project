import { useState, useEffect } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useTheme } from '../context/ThemeContext.jsx';

export default function Navbar() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  useEffect(() => { setOpen(false); }, [location.pathname]);

  const handleLogout = () => {
    logout();
    setOpen(false);
    navigate('/');
  };

  return (
    <nav className={`navbar ${open ? 'open' : ''}`}>
      <div className="navbar-inner">
        <div className="navbar-top">
          <NavLink to="/" className="brand" onClick={() => setOpen(false)}>
            <span className="brand-mark">VE</span>
            <span>VirtualEstate</span>
          </NavLink>
          <button
            className="nav-toggle"
            aria-label="Meni"
            aria-expanded={open}
            onClick={() => setOpen(v => !v)}
          >
            {open ? (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            ) : (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" />
              </svg>
            )}
          </button>
        </div>

        <div className="links">
          <NavLink to="/" end>Nepremičnine</NavLink>
          {user?.isAdmin && <NavLink to="/admin">Admin</NavLink>}
        </div>

        <div className="right">
          <button
            className="ghost icon-btn"
            onClick={toggle}
            aria-label={theme === 'light' ? 'Vklopi temni način' : 'Vklopi svetli način'}
            title={theme === 'light' ? 'Temni način' : 'Svetli način'}
          >
            {theme === 'light' ? (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
              </svg>
            ) : (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="4" />
                <line x1="12" y1="2" x2="12" y2="4" /><line x1="12" y1="20" x2="12" y2="22" />
                <line x1="4.93" y1="4.93" x2="6.34" y2="6.34" /><line x1="17.66" y1="17.66" x2="19.07" y2="19.07" />
                <line x1="2" y1="12" x2="4" y2="12" /><line x1="20" y1="12" x2="22" y2="12" />
                <line x1="4.93" y1="19.07" x2="6.34" y2="17.66" /><line x1="17.66" y1="6.34" x2="19.07" y2="4.93" />
              </svg>
            )}
          </button>
          {user ? (
            <>
              <span className="user-chip" title={user.email}>
                {user.isAdmin && <span className="admin-dot" />}
                {user.name}
              </span>
              <button className="secondary sm" onClick={handleLogout}>Odjava</button>
            </>
          ) : (
            <>
              <NavLink to="/login"><button className="secondary sm">Prijava</button></NavLink>
              <NavLink to="/register"><button className="sm">Registracija</button></NavLink>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
