import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/client';

export function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/painel" replace />;

  const strength = password.length >= 12 ? 'forte' : password.length >= 8 ? 'ok' : 'curta';

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await register(name, email, password);
      navigate('/painel', { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Não foi possível criar a conta.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-screen">
      <div className="auth-hero">
        <span className="brand-dot big" />
        <h1>Criar conta</h1>
        <p className="muted">leva dez segundos</p>
      </div>
      <form className="card auth-card" onSubmit={submit}>
        <label>
          Nome
          <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={80} autoComplete="name" autoFocus />
        </label>
        <label>
          E-mail
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>
          Senha <span className="muted small">(mínimo 8 caracteres) {password && <span className={`pill ${strength === 'forte' ? 'ok' : ''}`}>{strength}</span>}</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} maxLength={72} autoComplete="new-password" />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn primary" disabled={busy}>{busy ? 'Criando…' : 'Criar conta'}</button>
        <p className="muted small center">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  );
}
