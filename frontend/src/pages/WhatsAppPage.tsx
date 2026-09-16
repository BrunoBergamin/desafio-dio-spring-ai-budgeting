import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { authApi, whatsappApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { WhatsAppConnection } from '../api/types';
import { useAuth } from '../auth/AuthContext';

const STATE_LABEL: Record<string, string> = {
  open: 'conectado',
  connecting: 'aguardando o QR code',
  close: 'desconectado',
  not_created: 'ainda não configurado',
  unknown: 'desconhecido',
};

export function WhatsAppPage() {
  const { user } = useAuth();
  const [info, setInfo] = useState<WhatsAppConnection | null>(null);
  const [available, setAvailable] = useState(true);
  const [phone, setPhone] = useState('');
  const [linked, setLinked] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(() => {
    whatsappApi
      .status()
      .then((s) => { setInfo(s); setAvailable(true); })
      .catch((e) => { if (e?.response?.status === 404) setAvailable(false); else setError(errorMessage(e)); });
  }, []);

  useEffect(() => {
    refresh();
    authApi.me().then((me) => setLinked(me.phone ?? null)).catch(() => undefined);
  }, [refresh]);

  // Enquanto espera o QR ser lido, consulta o estado a cada 5s
  useEffect(() => {
    if (info?.state !== 'connecting') return;
    const id = window.setInterval(refresh, 5000);
    return () => window.clearInterval(id);
  }, [info?.state, refresh]);

  const connect = async () => {
    setBusy(true);
    setError(null);
    try {
      setInfo(await whatsappApi.connect());
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const link = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const me = await authApi.linkPhone(phone);
      setLinked(me.phone ?? null);
      setPhone('');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (!available) {
    return (
      <section className="card">
        <div className="card-head"><h2>WhatsApp</h2></div>
        <p className="muted">
          A integração com o WhatsApp não está ligada neste servidor. Suba com o perfil <code>whatsapp</code>:
          <code> docker compose --profile whatsapp up --build</code>.
        </p>
      </section>
    );
  }

  return (
    <div className="grid-two">
      <section className="card">
        <div className="card-head"><h2>1. Vincular o meu número</h2></div>
        <p className="muted small">
          A Lumi só responde a números vinculados a uma conta. Informe o WhatsApp de {user?.name}.
        </p>
        {linked && <p className="pill" style={{ display: 'inline-block' }}>vinculado: +{linked}</p>}
        <form className="form" onSubmit={link}>
          <label>
            Número com DDD
            <input inputMode="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="(19) 99999-9999" required />
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <button className="btn primary" disabled={busy}>{linked ? 'Trocar número' : 'Vincular'}</button>
        </form>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>2. Conectar o WhatsApp da Lumi</h2>
          <span className="pill">{STATE_LABEL[info?.state ?? 'unknown'] ?? info?.state}</span>
        </div>
        {info?.connected ? (
          <p>Tudo pronto. Mande um "oi" para o número conectado e fale com a Lumi por lá — texto ou áudio.</p>
        ) : (
          <>
            <p className="muted small">
              Use um chip que não seja o seu pessoal (a Evolution API não é oficial). No celular: WhatsApp →
              Aparelhos conectados → Conectar aparelho → escaneie o código.
            </p>
            {info?.qrCodeBase64 ? (
              <img className="qr" src={info.qrCodeBase64} alt="QR code para conectar o WhatsApp" />
            ) : (
              <button className="btn primary" onClick={connect} disabled={busy}>
                {busy ? 'Gerando…' : 'Gerar QR code'}
              </button>
            )}
            {info?.pairingCode && <p className="muted small">Código de pareamento: <b>{info.pairingCode}</b></p>}
          </>
        )}
      </section>
    </div>
  );
}
