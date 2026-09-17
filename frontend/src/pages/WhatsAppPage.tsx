import { useEffect, useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { authApi, whatsappApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { keys } from '../lib/queryClient';
import { useToast } from '../ui/Toast';

const STATE_LABEL: Record<string, string> = {
  open: 'conectado',
  connecting: 'aguardando o QR code',
  close: 'desconectado',
  not_created: 'ainda não configurado',
  unknown: 'desconhecido',
};

export function WhatsAppPage() {
  const { user, demo } = useAuth();
  const { notify } = useToast();
  const qc = useQueryClient();
  const [phone, setPhone] = useState('');
  const [qr, setQr] = useState<{ image?: string | null; pairingCode?: string | null } | null>(null);

  const me = useQuery({ queryKey: keys.me, queryFn: authApi.me });
  const status = useQuery({
    queryKey: keys.whatsapp,
    queryFn: whatsappApi.status,
    retry: false,
    refetchInterval: (q) => (q.state.data?.state === 'connecting' ? 5000 : false),
  });
  const unavailable = axios.isAxiosError(status.error) && status.error.response?.status === 404;

  useEffect(() => {
    if (status.data?.connected && qr) {
      setQr(null);
      notify('success', 'WhatsApp conectado! Mande um "oi" para a Lumi.');
    }
  }, [status.data?.connected, qr, notify]);

  const connect = useMutation({
    mutationFn: whatsappApi.connect,
    onSuccess: (info) => {
      setQr({ image: info.qrCodeBase64, pairingCode: info.pairingCode });
      qc.setQueryData(keys.whatsapp, info);
    },
    onError: (e) => notify('error', errorMessage(e)),
  });

  const link = useMutation({
    mutationFn: (value: string) => authApi.linkPhone(value),
    onSuccess: (updated) => {
      qc.setQueryData(keys.me, updated);
      setPhone('');
      notify('success', `Número +${updated.phone} vinculado à sua conta.`);
    },
    onError: (e) => notify('error', errorMessage(e)),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    link.mutate(phone);
  };

  if (unavailable) {
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

  const state = status.data?.state ?? 'unknown';

  return (
    <div className="grid-two">
      <section className="card">
        <div className="card-head"><h2>1. Vincular o meu número</h2></div>
        {demo ? (
          <p className="muted small">
            No modo demo isso é automático: a primeira mensagem que você mandar <b>para você mesmo</b> no WhatsApp
            pareado já vincula o número a esta conta. Só preencha abaixo se quiser fazer à mão.
          </p>
        ) : (
          <p className="muted small">A Lumi só responde a números vinculados a uma conta. Informe o WhatsApp de {user?.name}.</p>
        )}
        {me.data?.phone && <p className="pill inline">vinculado: +{me.data.phone}</p>}
        <form className="form" onSubmit={submit}>
          <label>
            Número com DDD
            <input inputMode="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="(19) 99999-9999" required />
          </label>
          <button className="btn primary" disabled={link.isPending}>{me.data?.phone ? 'Trocar número' : 'Vincular'}</button>
        </form>
      </section>

      <section className="card">
        <div className="card-head">
          <h2>2. Conectar o WhatsApp da Lumi</h2>
          <span className={`pill ${state === 'open' ? 'ok' : ''}`}>{STATE_LABEL[state] ?? state}</span>
        </div>
        {status.data?.connected ? (
          <div className="stack">
            <p>Tudo pronto. No WhatsApp, abra o chat <b>"Você"</b> (mensagem para mim mesmo) e mande "gastei 30 reais na farmácia", em texto ou áudio. A Lumi responde ali.</p>
            <p className="muted small">Cada pessoa que vincular o próprio número recebe as respostas dos próprios gastos.</p>
          </div>
        ) : (
          <div className="stack">
            <p className="muted small">
              Use um chip que não seja o seu pessoal (a Evolution API não é oficial). No celular: WhatsApp →
              Aparelhos conectados → Conectar aparelho → escaneie o código. Ele expira em cerca de 40 segundos.
            </p>
            {qr?.image ? (
              <div className="qr-box">
                <img className="qr" src={qr.image} alt="QR code para conectar o WhatsApp" />
                <button className="btn ghost small" onClick={() => connect.mutate()} disabled={connect.isPending}>gerar outro</button>
              </div>
            ) : (
              <button className="btn primary" onClick={() => connect.mutate()} disabled={connect.isPending}>
                {connect.isPending ? 'Gerando…' : 'Gerar QR code'}
              </button>
            )}
            {qr?.pairingCode && <p className="muted small">Código de pareamento: <b>{qr.pairingCode}</b></p>}
          </div>
        )}
      </section>
    </div>
  );
}
