import { beforeEach, describe, expect, it } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useChatHistory } from './useChatHistory';

describe('useChatHistory', () => {
  beforeEach(() => localStorage.clear());

  it('começa com a boas-vindas da Lumi e persiste as mensagens por usuário', () => {
    const { result } = renderHook(() => useChatHistory('web', 'u1'));
    expect(result.current.messages[0].role).toBe('lumi');

    act(() => { result.current.push({ role: 'user', text: 'gastei 10' }); });
    expect(result.current.messages).toHaveLength(2);

    const saved = JSON.parse(localStorage.getItem('lumi.chat.u1.web')!);
    expect(saved).toHaveLength(2);
    expect(saved[1].text).toBe('gastei 10');
  });

  it('não mistura o histórico de usuários diferentes e descarta URLs de áudio ao salvar', () => {
    const a = renderHook(() => useChatHistory('web', 'u1'));
    act(() => { a.result.current.push({ role: 'user', text: 'áudio', kind: 'audio', audioUrl: 'blob:x' }); });

    const b = renderHook(() => useChatHistory('web', 'u2'));
    expect(b.result.current.messages).toHaveLength(1);

    const saved = JSON.parse(localStorage.getItem('lumi.chat.u1.web')!);
    expect(saved[1].audioUrl).toBeUndefined();
  });

  it('reset volta para uma conversa nova', () => {
    const { result } = renderHook(() => useChatHistory('web', 'u1'));
    act(() => { result.current.push({ role: 'user', text: 'oi' }); });
    act(() => { result.current.reset(); });
    expect(result.current.messages).toHaveLength(1);
    expect(result.current.messages[0].text).toMatch(/Conversa nova/);
  });
});
