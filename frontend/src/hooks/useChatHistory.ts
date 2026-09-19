import { useCallback, useEffect, useRef, useState } from 'react';

export interface ChatMessage {
  id: number;
  role: 'user' | 'lumi';
  text: string;
  at: string;
  kind?: 'text' | 'transcription' | 'audio' | 'error';
  audioUrl?: string;
}

const KEY = (conversation: string, userId: string) => `lumi.chat.${userId}.${conversation}`;

const welcome = (): ChatMessage => ({
  id: 1,
  role: 'lumi',
  text: 'Oi! Eu sou a Lumi. Fale, escreva ou envie um áudio (até os do WhatsApp) com um gasto ou uma receita, tipo "gastei 45 reais no mercado" ou "recebi 5200 de salário". Também dá para perguntar "quanto gastei este mês?" e "sobrou quanto?".',
  at: new Date().toISOString(),
});

/**
 * Historico da conversa guardado no navegador (localStorage), por usuario e por conversa.
 * URLs de audio (blob:) nao sobrevivem a recarga, entao sao descartadas ao salvar.
 */
export function useChatHistory(conversation: string, userId: string) {
  const key = KEY(conversation, userId);
  const [messages, setMessages] = useState<ChatMessage[]>(() => {
    try {
      const saved = localStorage.getItem(key);
      const parsed = saved ? (JSON.parse(saved) as ChatMessage[]) : [];
      return parsed.length ? parsed : [welcome()];
    } catch {
      return [welcome()];
    }
  });
  const nextId = useRef(Math.max(0, ...messages.map((m) => m.id)) + 1);

  useEffect(() => {
    try {
      localStorage.setItem(key, JSON.stringify(messages.slice(-200).map(({ audioUrl: _drop, ...m }) => m)));
    } catch {
      /* modo privado ou cota cheia: a conversa segue so em memoria */
    }
  }, [key, messages]);

  const push = useCallback((m: Omit<ChatMessage, 'id' | 'at'>) => {
    const id = nextId.current++;
    setMessages((prev) => [...prev, { ...m, id, at: new Date().toISOString() }]);
    return id;
  }, []);

  const patch = useCallback((id: number, changes: Partial<ChatMessage>) => {
    setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, ...changes } : m)));
  }, []);

  const reset = useCallback(() => {
    setMessages([{ ...welcome(), text: 'Conversa nova. O que você gastou?' }]);
  }, []);

  return { messages, push, patch, reset };
}
