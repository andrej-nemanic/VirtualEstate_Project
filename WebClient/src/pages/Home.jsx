import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Home() {
  const { user } = useAuth();
  return (
    <div className="container">
      <div className="card" style={{ padding: 40, textAlign: 'center' }}>
        <h1 style={{ margin: 0 }}>Virtual Estate</h1>
        <p style={{ color: '#6b7280', maxWidth: 600, margin: '12px auto' }}>
          Digitalni dvojček nepremičnin. Pregled, vizualizacija in upravljanje podatkov o
          nepremičninah z grafom, zemljevidom in realnočasovnim posodabljanjem.
        </p>
        <div style={{ marginTop: 20, display: 'flex', gap: 12, justifyContent: 'center' }}>
          {user ? (
            <Link to="/dashboard"><button>Pojdi na nadzorno ploščo</button></Link>
          ) : (
            <>
              <Link to="/login"><button>Prijava</button></Link>
              <Link to="/register"><button className="secondary">Registracija</button></Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
