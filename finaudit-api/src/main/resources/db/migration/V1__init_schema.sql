-- FinAudit-AI V1 Schema Initialization

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    status VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    audited_at TIMESTAMP WITH TIME ZONE,
    error_reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_reports_owner_id ON reports(owner_id);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);

CREATE TABLE IF NOT EXISTS report_line_items (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    invoice_id VARCHAR(100) NOT NULL,
    vendor VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    category VARCHAR(100),
    raw_text TEXT,
    line_number INTEGER,
    CONSTRAINT uq_line_item_report_vendor_invoice UNIQUE (report_id, vendor, invoice_id)
);

CREATE INDEX IF NOT EXISTS idx_report_line_items_report_id ON report_line_items(report_id);
CREATE INDEX IF NOT EXISTS idx_report_line_items_invoice_id ON report_line_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_report_line_items_vendor_invoice ON report_line_items(vendor, invoice_id);

CREATE TABLE IF NOT EXISTS compliance_policies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body_text TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    effective_date DATE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_compliance_policies_category ON compliance_policies(category);

CREATE TABLE IF NOT EXISTS audit_findings (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    line_item_id BIGINT REFERENCES report_line_items(id) ON DELETE SET NULL,
    rule_source VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    policy_reference VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_findings_report_id ON audit_findings(report_id);
CREATE INDEX IF NOT EXISTS idx_audit_findings_line_item_id ON audit_findings(line_item_id);

CREATE TABLE IF NOT EXISTS audit_runs (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    compliance_score INTEGER NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    raw_model_output_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_runs_report_id ON audit_runs(report_id);
