CREATE TABLE saga_instances (
    id UUID PRIMARY KEY,
    order_id TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPENSATING', 'COMPLETED', 'COMPENSATED', 'FAILED')),
    next_step INTEGER NOT NULL DEFAULT 0 CHECK (next_step BETWEEN 0 AND 3),
    compensation_step INTEGER,
    failure_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX saga_instances_recovery_idx
    ON saga_instances (status, updated_at)
    WHERE status IN ('PENDING', 'RUNNING', 'COMPENSATING', 'FAILED');

CREATE TABLE saga_transitions (
    sequence BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    saga_id UUID NOT NULL REFERENCES saga_instances(id),
    event_type TEXT NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    aggregate_id TEXT NOT NULL,
    correlation_id UUID NOT NULL,
    causation_id UUID,
    step_name TEXT,
    action TEXT NOT NULL,
    from_status TEXT,
    to_status TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (saga_id, step_name, action)
);

CREATE INDEX saga_transitions_saga_sequence_idx ON saga_transitions (saga_id, sequence);

CREATE TABLE inventory_reservations (
    order_id TEXT PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('RESERVED', 'RELEASED')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payment_authorizations (
    order_id TEXT PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('AUTHORIZED', 'REFUNDED')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE shipments (
    order_id TEXT PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL CHECK (status IN ('CREATED', 'CANCELLED')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
