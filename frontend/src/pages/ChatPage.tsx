import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import axios from 'axios';
import { assistantApi, budgetsApi, transactionsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { BudgetStatusResponse, SpendingSummary } from '../api/types';
import { useRecorder, type Recording } from '../hooks/useRecorder';
import { MicButton } from '../components/MicButton';
import { CategoryChart } from '../components/CategoryChart';
import { BudgetBar } from '../components/BudgetBar';
import { money } from '../utils/format';

interface Message {
  id: number;
  role: 'user' | 'lumi';
  text: string;
  transcription?: boolean;
  audioUrl?: string;
}

const CONVERSATION = 'web';
let nextId = 1;

export function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    { id: nextId++, role: 'lumi', text: 'Oi! Eu sou a Lumi. Fale ou escreva um gasto, tipo "gastei 45 reais no mercado", ou pergunte "quanto gastei este mês?".' },
  ]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [wantAudio, setWantAudio] = useState(true);
  const [summary, setSummary] = useState<SpendingSummary | null>(null);
  const [alerts, setAlerts] = useState<BudgetStatusResponse[]>([]);
  const listRef = useRef<HTMLDivElement>(null);

  const refresh = useCallback(() => {
    transactionsApi.summary().then(setSummary).catch(() => undefined);
    budgetsApi.alerts().then(setAlerts).catch(() => undefined);
  }, []);

  useEffect(refresh, [refresh]);
  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages]);

  const push = (m: Omit<Message, 'id'>) => setMessages((prev) => [...prev, { ...m, id: nextId++ }]);

  const sendText = async (e: FormEvent) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || busy) return;
    setInput('');
    push({ role: 'user', text });
    setBusy(true);
    try {
      const res = await assistantApi.chat(text, CONVERSATION);
      push({ role: 'lumi', text: res.answer });
      refresh();
    } catch (err) {
      push({ role: 'lumi', text: `⚠️ ${errorMessage(err)}` });
    } finally {
      setBusy(false);
    }
  };

  const sendAudio = useCallback(async ({ blob, filename }: Recording) => {
    setBusy(true);
    push({ role: 'user', text: '🎙️ áudio enviado…' });
    try {
      if (wantAudio) {
        // Tenta a resposta falada; se o perfil nao tiver TTS (503), cai para texto
        try {
          const mp3 = await assistantApi.voiceToVoice(blob, filename, CONVERSATION);
          const url = URL.createObjectURL(mp3);
          push({ role: 'lumi', text: 'Resposta em áudio:', audioUrl: url });
          refresh();
          return;
        } catch (err) {
          if (!(axios.isAxiosError(err) && err.response?.status === 503)) throw err;
          setWantAudio(false);
        }
      }
      const res = await assistantApi.voiceToText(blob, filename, CONVERSATION);
      setMessages((prev) => prev.map((m) => (m.text === '🎙️ áudio enviado…' ? { ...m, text: res.transcription ?? '', transcription: true } : m)));
      push({ role: 'lumi', text: res.answer });
      refresh();
    } catch (err) {
      push({ role: 'lumi', text: `⚠️ ${errorMessage(err)}` });
    } finally {
      setBusy(false);
    }
  }, [wantAudio, refresh]);

  const recorder = useRecorder(sendAudio);

  const newConversation = async () => {
    await assistantApi.forget(CONVERSATION).catch(() => undefined);
    setMessages([{ id: nextId++, role: 'lumi', text: 'Conversa nova. O que você gastou?' }]);
  };

  useEffect(() => () => messages.forEach((m) => m.audioUrl && URL.revokeObjectURL(m.audioUrl)), []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="grid-chat">
      <section className="card chat">
        <div className="card-head">
          <h2>Conversa com a Lumi</h2>
          <button className="btn ghost small" onClick={newConversation}>nova conversa</button>
        </div>
        <div className="messages" ref={listRef}>
          {messages.map((m) => (
            <div key={m.id} className={`bubble ${m.role}`}>
              {m.transcription && <span className="bubble-tag">você disse</span>}
              <p>{m.text}</p>
              {m.audioUrl && <audio controls autoPlay src={m.audioUrl} />}
            </div>
          ))}
          {busy && <div className="bubble lumi typing"><span /><span /><span /></div>}
        </div>
        <div className="chat-tools">
          <MicButton {...recorder} disabled={busy} onStart={recorder.start} onStop={recorder.stop} />
          <form className="chat-form" onSubmit={sendText}>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="ou escreva aqui…"
              maxLength={500}
              disabled={busy}
              aria-label="mensagem para a Lumi"
            />
            <button className="btn primary" disabled={busy || !input.trim()}>Enviar</button>
          </form>
          {!wantAudio && <p className="muted small">Este servidor responde só em texto (perfil sem text-to-speech).</p>}
        </div>
      </section>

      <aside className="side">
        <section className="card">
          <div className="card-head">
            <h3>Este mês</h3>
            {summary && <span className="total">{money(summary.total)}</span>}
          </div>
          <CategoryChart data={summary?.categories ?? []} />
        </section>
        {alerts.length > 0 && (
          <section className="card">
            <div className="card-head"><h3>Alertas de orçamento</h3></div>
            <div className="stack">
              {alerts.map((b) => <BudgetBar key={b.id} budget={b} />)}
            </div>
          </section>
        )}
      </aside>
    </div>
  );
}
