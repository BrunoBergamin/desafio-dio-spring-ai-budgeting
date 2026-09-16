import type { RecorderState } from '../hooks/useRecorder';

interface Props {
  state: RecorderState;
  seconds: number;
  maxSeconds: number;
  disabled?: boolean;
  onStart: () => void;
  onStop: () => void;
}

export function MicButton({ state, seconds, maxSeconds, disabled, onStart, onStop }: Props) {
  if (state === 'unsupported') {
    return <p className="muted small">Seu navegador não grava áudio. Use o Chrome ou o Edge, em localhost ou https. Você ainda pode enviar um arquivo de áudio.</p>;
  }
  if (state === 'denied') {
    return <p className="error small">Permissão do microfone negada. Libere no cadeado da barra de endereço e tente de novo.</p>;
  }
  const recording = state === 'recording';
  return (
    <div className="mic-wrap">
      <button
        type="button"
        className={recording ? 'mic recording' : 'mic'}
        onClick={recording ? onStop : onStart}
        disabled={disabled}
        aria-pressed={recording}
        aria-label={recording ? 'parar gravação' : 'gravar áudio'}
      >
        {recording ? '■' : '🎙️'}
      </button>
      <span className="mic-hint">
        {recording ? (
          <>
            <span className="rec-dot" /> gravando… <b>{seconds}s</b> / {maxSeconds}s · toque para enviar
          </>
        ) : (
          'toque e fale, ex.: "gastei 30 reais na farmácia"'
        )}
      </span>
    </div>
  );
}
