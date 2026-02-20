-- Initialize database for Execution Service
-- This script runs when the PostgreSQL container starts
-- It creates schema, tables and fake seed data for local testing.

CREATE TABLE IF NOT EXISTS execution_task (
    id BIGSERIAL PRIMARY KEY,
    service_order_id BIGINT NOT NULL,
    customer_id BIGINT,
    vehicle_id BIGINT,
    vehicle_license_plate VARCHAR(20),
    description TEXT,
    status VARCHAR(40) NOT NULL,
    assigned_technician VARCHAR(255),
    notes TEXT,
    failure_reason TEXT,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_execution_task_service_order_id ON execution_task (service_order_id);
CREATE INDEX IF NOT EXISTS idx_execution_task_status ON execution_task (status);
CREATE INDEX IF NOT EXISTS idx_execution_task_customer_id ON execution_task (customer_id);

-- Fake data: execution tasks
INSERT INTO execution_task (
    id, service_order_id, customer_id, vehicle_id, vehicle_license_plate,
    description, status, assigned_technician, notes, failure_reason,
    priority, created_at, updated_at, started_at, completed_at
) VALUES
    (1, 2, 1002, 2002, 'BRA2E45',
     'Troca de óleo e revisão de 10.000km', 'IN_PROGRESS', 'João Mecânico',
     'Iniciado diagnóstico do motor', NULL,
     1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NULL),
    (2, 3, 1003, 2003, 'XYZ9K88',
     'Falha na partida e bateria descarregando', 'COMPLETED', 'Maria Eletricista',
     'Bateria substituída com sucesso', NULL,
     2, NOW() - INTERVAL '3 days', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '2 days', NOW() - INTERVAL '6 hours'),
    (3, 5, 1005, 2005, 'MNO3P67',
     'Alinhamento e balanceamento', 'QUEUED', NULL,
     NULL, NULL,
     0, NOW() - INTERVAL '30 minutes', NULL, NULL, NULL),
    (4, 6, 1006, 2006, 'RST8U12',
     'Troca de correia dentada', 'FAILED', 'Pedro Técnico',
     'Peça incompatível com o modelo', 'Recurso indisponível: correia dentada modelo X não encontrada',
     1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day 12 hours', NULL)
ON CONFLICT (id) DO NOTHING;

-- Keep sequence in sync with inserted IDs
SELECT setval('execution_task_id_seq', COALESCE((SELECT MAX(id) FROM execution_task), 1), true);

SELECT 'Execution Service schema and fake data initialized' AS status;
