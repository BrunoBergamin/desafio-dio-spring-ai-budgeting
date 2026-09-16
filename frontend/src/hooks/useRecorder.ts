import { useCallback, useEffect, useRef, useState } from 'react';

const PREFERRED_TYPES = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];
const MAX_SECONDS = 60;

export type RecorderState = 'idle' | 'recording' | 'unsupported' | 'denied';

export interface Recording {
  blob: Blob;
  filename: string;
}

/**
 * Grava audio pelo microfone com MediaRecorder. O Chrome grava webm/opus, que o Whisper aceita
 * direto: isso elimina o problema do Gravador do Windows, que salva AAC cru dentro de um .m4a.
 */
export function useRecorder(onReady: (recording: Recording) => void) {
  const [state, setState] = useState<RecorderState>(
    typeof MediaRecorder === 'undefined' || !navigator.mediaDevices ? 'unsupported' : 'idle',
  );
  const [seconds, setSeconds] = useState(0);
  const recorderRef = useRef<MediaRecorder | null>(null);
  const timerRef = useRef<number | null>(null);

  const stop = useCallback(() => {
    recorderRef.current?.state === 'recording' && recorderRef.current.stop();
  }, []);

  const start = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeType = PREFERRED_TYPES.find((t) => MediaRecorder.isTypeSupported(t)) ?? '';
      const recorder = new MediaRecorder(stream, mimeType ? { mimeType, audioBitsPerSecond: 64000 } : undefined);
      const chunks: Blob[] = [];

      recorder.ondataavailable = (e) => e.data.size > 0 && chunks.push(e.data);
      recorder.onstop = () => {
        stream.getTracks().forEach((t) => t.stop()); // apaga o indicador de microfone
        if (timerRef.current) window.clearInterval(timerRef.current);
        setState('idle');
        setSeconds(0);
        const type = recorder.mimeType || mimeType || 'audio/webm';
        const blob = new Blob(chunks, { type });
        // A extensao importa: o servico de transcricao decide o formato por ela
        const filename = type.startsWith('audio/mp4') ? 'gravacao.mp4' : 'gravacao.webm';
        if (blob.size > 0) onReady({ blob, filename });
      };

      recorderRef.current = recorder;
      recorder.start();
      setState('recording');
      setSeconds(0);
      timerRef.current = window.setInterval(() => {
        setSeconds((s) => {
          if (s + 1 >= MAX_SECONDS) stop();
          return s + 1;
        });
      }, 1000);
    } catch {
      setState('denied');
    }
  }, [onReady, stop]);

  useEffect(() => () => {
    if (timerRef.current) window.clearInterval(timerRef.current);
    recorderRef.current?.stream.getTracks().forEach((t) => t.stop());
  }, []);

  return { state, seconds, maxSeconds: MAX_SECONDS, start, stop };
}
