import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const links = [
  { to: '/conversa', label: 'Lumi', icon: '✨' },
  { to: '/transacoes', label: 'Gastos', icon: '🧾' },
  { to: '/orcamentos', label: 'Orçamentos', icon: '🎯' },
];

export function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-dot" />
          <span>Controle Financeiro</span>
        </div>
        <nav className="nav">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              <span aria-hidden>{l.icon}</span> {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="user-box">
          <span className="user-name">{user?.name}</span>
          <button className="btn ghost" onClick={logout}>Sair</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
