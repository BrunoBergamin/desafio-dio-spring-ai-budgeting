-- Transacao passa a ser gasto (EXPENSE) ou receita (INCOME). O que ja existe e gasto: o DEFAULT preenche
-- as linhas antigas e permite criar a coluna NOT NULL de forma portatil (H2 e MySQL).
ALTER TABLE transactions ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'EXPENSE';

-- O painel e o resumo sempre filtram por tipo dentro de um periodo
CREATE INDEX idx_transactions_user_type_date ON transactions (user_id, type, transaction_date);
