-- Drop unique constraint on (report_id, vendor, invoice_id)
-- Allows multi-line itemized invoices with the same vendor/invoice ID,
-- and allows duplicate invoice submissions within a report to be stored and audited.
ALTER TABLE report_line_items DROP CONSTRAINT IF EXISTS uq_line_item_report_vendor_invoice;
