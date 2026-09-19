-- Metas de economia: guardar um valor ate uma data. O quanto ja foi guardado fica aqui e nao vira
-- lancamento: separar dinheiro nao e gastar, entao nao entra no total de gastos nem consome orcamento.
CREATE TABLE savings_goals (
    id            VARCHAR(36)   NOT NULL,
    user_id       VARCHAR(36)   NOT NULL,
    name          VARCHAR(80)   NOT NULL,
    target_amount DECIMAL(12,2) NOT NULL,
    saved_amount  DECIMAL(12,2) NOT NULL DEFAULT 0,
    deadline      DATE,
    completed_at  TIMESTAMP(6),
    created_at    TIMESTAMP(6)  NOT NULL,
    updated_at    TIMESTAMP(6),
    CONSTRAINT pk_savings_goals PRIMARY KEY (id),
    -- Nome unico por pessoa: e assim que a Lumi acha a meta pelo nome falado
    CONSTRAINT uk_goals_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_goals_user_deadline ON savings_goals (user_id, deadline);
