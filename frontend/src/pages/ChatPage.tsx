import { useCallback, useEffect, useRef, useState, type ChangeEvent, type DragEvent, type FormEvent, type KeyboardEvent } from 'react';
import axios from 'axios';
import { assistantApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useRecorder, type Recording } from '../hooks/useRecorder';
import { useChatHistory } from '../hooks/useChatHistory';
import { useAlerts, useSummary } from '../hooks/useFinance';
import { invalidateFinancial } from '../lib/queryClient';
import { MicButton } from '../components/MicButton';
import { CategoryChart } from '../components/CategoryChart';
import { BudgetBar } from '../components/BudgetBar';
import { Confirm, Skeleton } from '../ui/primitives';
import { useToast } from '../ui/Toast';
import { AUDIO_ACCEPT, money } from '../utils/format';

const CONVERSATION = 'web';
const MAX_UPLOAD = 10 * 1024 * 1024;

const hhmm = (iso: string) => new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

export function ChatPage() {
  const { user } = useAuth();
  const { notify } = useToast();
  const { messages, push, patch, reset } = useChatHistory(CONVERSATION, user?.id ?? 'anon');
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [wantAudio, setWantAudio] = useState(true);
  const [dragging, setDragging] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const textRef = useRef<HTMLTextAreaElement>(null);
  const summary = useSummary();
  const alerts = useAlerts();

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, busy]);

  const ask = async (text: string) => {
    if (!text.trim() || busy) return;
    setInput('');
    if (textRef.current) textRef.current.style.height = 'auto';
    push({ role: 'user', text });
    setBusy(true);
    try {
      const res = await assistantApi.chat(text, CONVERSATION);
      push({ role: 'lumi', text: res.answer });
      invalidateFinancial();
    } catch (err) {
      push({ role: 'lumi', text: errorMessage(err), kind: 'error' });
    } finally {
      setBusy(false);
    }
  };

  const sendText = (e: FormEvent) => {
    e.preventDefault();
    ask(input);
  };

  const onKey = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      ask(input);
    }
  };

  const autoGrow = (el: HTMLTextAreaElement) => {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 140)}px`;
  };

  /** Audio gravado ou arquivo enviado: mesmo caminho. */
  const sendAudio = useCallback(async ({ blob, filename }: Recording) => {
    if (blob.size > MAX_UPLOAD) {
      notify('error', 'O áudio precisa ter no máximo 10 MB.');
      return;
    }
    setBusy(true);
    const url = URL.createObjectURL(blob);
    const id = push({ role: 'user', text: 'áudio enviado', kind: 'audio', audioUrl: url });
    try {
      if (wantAudio) {
        try {
          const mp3 = await assistantApi.voiceToVoice(blob, filename, CONVERSATION);
          push({ role: 'lumi', text: 'Resposta em áudio:', kind: 'audio', audioUrl: URL.createObjectURL(mp3) });
          invalidateFinancial();
          return;
        } catch (err) {
          if (!(axios.isAxiosError(err) && err.response?.status === 503)) throw err;
          setWantAudio(false); // perfil sem text-to-speech: segue so em texto daqui em diante
        }
      }
      const res = await assistantApi.voiceToText(blob, filename, CONVERSATION);
      patch(id, { text: res.transcription ?? 'áudio enviado', kind: 'transcription' });
      push({ role: 'lumi', text: res.answer });
      invalidateFinancial();
    } catch (err) {
      push({ role: 'lumi', text: errorMessage(err), kind: 'error' });
    } finally {
      setBusy(false);
    }
  }, [wantAudio, push, patch, notify]);

  const recorder = useRecorder(sendAudio);

  const onFile = (file: File | undefined) => {
    if (!file) return;
    if (!/\.(ogg|opus|oga|m4a|mp3|wav|webm|mp4|flac|aac)$/i.test(file.name) && !file.type.startsWith('audio/')) {
      notify('error', 'Envie um arquivo de áudio (ogg, mp3, m4a, wav, webm...).');
      return;
    }
    sendAudio({ blob: file, filename: file.name });
  };

  const onFileInput = (e: ChangeEvent<HTMLInputElement>) => {
    onFile(e.target.files?.[0]);
    e.target.value = '';
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDragging(false);
    onFile(e.dataTransfer.files?.[0]);
  };

  const newConversation = async () => {
    setConfirmReset(false);
    await assistantApi.forget(CONVERSATION).catch(() => undefined);
    reset();
    notify('info', 'Conversa nova. A Lumi esqueceu o contexto anterior.');
  };

  return (
    <div className="grid-chat">
      <section
        className={`card chat ${dragging ? 'dragging' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
      >
        <div className="card-head">
          <div>
            <h2>Conversa com a Lumi</h2>
            <p className="muted small">fale, escreva ou arraste um áudio aqui, inclusive as notas de voz do WhatsApp</p>
          </div>
          <button className="btn ghost small" onClick={() => setConfirmReset(true)}>nova conversa</button>
        </div>

        <div className="messages" ref={listRef} aria-live="polite">
          {messages.map((m) => (
            <div key={m.id} className={`bubble ${m.role} ${m.kind ?? ''}`}>
              {m.role === 'lumi' && <span className="bubble-avatar" aria-hidden>✨</span>}
              <div className="bubble-body">
                {m.kind === 'transcription' && <span className="bubble-tag">você disse</span>}
                {m.kind === 'error' && <span className="bubble-tag">algo deu errado</span>}
                <p>{m.text}</p>
                {m.audioUrl && <audio controls autoPlay={m.role === 'lumi'} src={m.audioUrl} />}
                <span className="bubble-time">{hhmm(m.at)}</span>
              </div>
            </div>
          ))}
          {busy && (
            <div className="bubble lumi">
              <span className="bubble-avatar" aria-hidden>✨</span>
              <div className="bubble-body typing"><span /><span /><span /></div>
            </div>
          )}
        </div>

        <div className="chat-tools">
          <div className="chat-audio">
            <MicButton {...recorder} disabled={busy} onStart={recorder.start} onStop={recorder.stop} />
            <button type="button" className="btn ghost" onClick={() => fileRef.current?.click()} disabled={busy} title="enviar um arquivo de áudio">
              📎 enviar áudio
            </button>
            <input ref={fileRef} type="file" accept={AUDIO_ACCEPT} hidden onChange={onFileInput} aria-label="arquivo de áudio" />
          </div>
          <form className="chat-form" onSubmit={sendText}>
            <textarea
              ref={textRef}
              rows={1}
              value={input}
              onChange={(e) => { setInput(e.target.value); autoGrow(e.target); }}
              onKeyDown={onKey}
              placeholder="ou escreva aqui… (Enter envia, Shift+Enter quebra linha)"
              maxLength={500}
              disabled={busy}
              aria-label="mensagem para a Lumi"
            />
            <button className="btn primary" disabled={busy || !input.trim()}>Enviar</button>
          </form>
          {!wantAudio && <p className="muted small">Este servidor responde só em texto (perfil sem text-to-speech).</p>}
        </div>
        {dragging && <div className="drop-hint">solte o áudio para enviar</div>}
      </section>

      <aside className="side">
        <section className="card">
          <div className="card-head">
            <h3>Este mês</h3>
            {summary.data && <span className="total">{money(summary.data.total)}</span>}
          </div>
          {summary.isLoading ? <Skeleton lines={4} height={18} /> : <CategoryChart data={summary.data?.categories ?? []} />}
        </section>
        {(alerts.data?.length ?? 0) > 0 && (
          <section className="card">
            <div className="card-head"><h3>Alertas de orçamento</h3></div>
            <div className="stack">
              {alerts.data!.map((b) => <BudgetBar key={b.id} budget={b} />)}
            </div>
          </section>
        )}
      </aside>

      <Confirm open={confirmReset} title="Começar uma conversa nova?" text="A Lumi vai esquecer o contexto desta conversa. Os gastos registrados continuam salvos."
               onCancel={() => setConfirmReset(false)} onConfirm={newConversation} />
    </div>
  );
}
