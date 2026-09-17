import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useTheme } from '../ui/ThemeContext';

const links = [
  { to: '/painel', label: 'Painel', icon: '📊' },
  { to: '/conversa', label: 'Lumi', icon: '✨' },
  { to: '/transacoes', label: 'Gastos', icon: '🧾' },
  { to: '/orcamentos', label: 'Orçamentos', icon: '🎯' },
  { to: '/whatsapp', label: 'WhatsApp', icon: '💬' },
];

const initials = (name?: string) =>
  (name ?? '?').split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase()).join('');

export function Layout() {
  const { user, logout, demo } = useAuth();
  const { theme, toggle } = useTheme();
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-dot" />
          <span>Controle Financeiro</span>
        </div>
        <nav className="nav" aria-label="principal">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              <span aria-hidden>{l.icon}</span> <span className="nav-label">{l.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="user-box">
          <button className="btn ghost icon" onClick={toggle} aria-label={theme === 'dark' ? 'tema claro' : 'tema escuro'} title="alternar tema">
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
          <span className="avatar" title={user?.email}>{initials(user?.name)}</span>
          <span className="user-name">{user?.name}</span>
          {demo ? (
            <span className="pill" title="conta de demonstração, com dados fictícios">modo demo</span>
          ) : (
            <button className="btn ghost small" onClick={logout}>Sair</button>
          )}
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
