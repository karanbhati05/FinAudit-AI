import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  UploadCloud,
  FileText,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  RefreshCw,
  Sparkles,
} from 'lucide-react';

interface ReportStatusResponse {
  id: number;
  status: 'UPLOADED' | 'PARSING' | 'AUDITING' | 'COMPLETE' | 'FAILED';
  lineItemCount?: number;
  errorReason?: string;
}

const STAGES = [
  { key: 'UPLOADED', label: 'Uploaded', desc: 'Securely saved to storage' },
  { key: 'PARSING', label: 'Parsing', desc: 'Gemini structured extraction' },
  { key: 'AUDITING', label: 'Auditing', desc: 'RAG retrieval & duplicate tool' },
  { key: 'COMPLETE', label: 'Complete', desc: 'Audit findings finalized' },
] as const;

export const UploadPage: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [reportId, setReportId] = useState<number | null>(null);
  const [auditStatus, setAuditStatus] = useState<ReportStatusResponse | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const pollingTimerRef = useRef<number | null>(null);
  const navigate = useNavigate();

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const validateAndSetFile = (file: File) => {
    setErrorMessage(null);
    if (file.size > 10 * 1024 * 1024) {
      setErrorMessage('File size exceeds the 10MB limit.');
      return;
    }
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension !== 'pdf' && extension !== 'txt') {
      setErrorMessage('Only PDF and plain text (.txt) files are supported.');
      return;
    }
    setSelectedFile(file);
    setAuditStatus(null);
    setReportId(null);
  };

  const handleLoadSample = () => {
    const sampleText = `CORPORATE EXPENSE REPORT - Q1 2026
Employee: Jane Doe
Department: Engineering

Line Items:
1. Invoice #INV-1011 | Vendor: United Airlines | Amount: $1850.00 USD | Category: Airfare | Description: SFO to NYC Flight Business Class
2. Invoice #INV-1012 | Vendor: Hilton Hotels | Amount: $680.00 USD | Category: Lodging | Description: 2 nights at Hilton Midtown Manhattan
3. Invoice #INV-1013 | Vendor: Uber Technologies | Amount: $92.50 USD | Category: Ground Transportation | Description: Airport rides to hotel
4. Invoice #INV-1014 | Vendor: Client Dinner Bistro | Amount: $420.00 USD | Category: Meals & Entertainment | Description: Dinner with 3 attendees
5. Invoice #INV-9402 | Vendor: TechSupplies Inc | Amount: $240.00 USD | Category: Office Supplies | Description: Ergonomic accessories
`;
    const sampleFile = new File([sampleText], 'sample-q1-expense-report.txt', {
      type: 'text/plain',
    });
    validateAndSetFile(sampleFile);
  };

  const handleStartAudit = async () => {
    if (!selectedFile) return;

    try {
      setIsUploading(true);
      setErrorMessage(null);

      const formData = new FormData();
      formData.append('file', selectedFile);

      const response = await api.post('/reports/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      const repId = response.data.reportId || response.data.id || response.data;
      setReportId(repId);
      setAuditStatus({
        id: repId,
        status: 'UPLOADED',
        lineItemCount: 0,
      });
    } catch (err: any) {
      setErrorMessage(
        err.response?.data?.message || 'Failed to upload report. Check backend connectivity.'
      );
      setIsUploading(false);
    }
  };

  // Status Polling Effect
  useEffect(() => {
    if (!reportId || !auditStatus) return;

    if (auditStatus.status === 'COMPLETE' || auditStatus.status === 'FAILED') {
      setIsUploading(false);
      return;
    }

    const pollStatus = async () => {
      try {
        const response = await api.get<ReportStatusResponse>(`/reports/${reportId}/status`);
        setAuditStatus(response.data);
      } catch (err) {
        console.error('Error polling report status', err);
      }
    };

    const intervalId = window.setInterval(pollStatus, 1500);
    pollingTimerRef.current = intervalId;

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [reportId, auditStatus?.status]);

  // Stage calculations
  const getStageIndex = (status?: string): number => {
    switch (status) {
      case 'UPLOADED':
        return 0;
      case 'PARSING':
        return 1;
      case 'AUDITING':
        return 2;
      case 'COMPLETE':
        return 3;
      case 'FAILED':
        return -1;
      default:
        return -1;
    }
  };

  const currentStageIndex = getStageIndex(auditStatus?.status);

  return (
    <div className="max-w-4xl mx-auto px-6 py-12">
      {/* Header */}
      <div className="mb-10">
        <span className="text-caption font-mono uppercase tracking-widest text-accent font-semibold">
          Autonomous Audit Pipeline
        </span>
        <h1 className="text-headline font-bold text-primary mt-2">Upload Expense Report</h1>
        <p className="text-body text-secondary mt-1">
          Upload corporate expense PDF statements or raw receipt texts to initiate automated RAG policy verification.
        </p>
      </div>

      {/* Main Upload / Progress Card */}
      <Card padding="lg" className="border-subtle bg-surface">
        {!auditStatus ? (
          <div>
            {/* Drag & Drop Zone */}
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`border-2 border-dashed rounded-xl p-10 md:p-14 text-center cursor-pointer transition-all duration-200 ${
                isDragging
                  ? 'border-accent bg-accent-subtle/50 scale-[0.99]'
                  : selectedFile
                  ? 'border-accent/40 bg-surface-subtle/60'
                  : 'border-subtle bg-surface-subtle/20 hover:border-strong hover:bg-surface-subtle/40'
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.txt"
                onChange={handleFileInputChange}
                className="hidden"
              />

              <div className="flex flex-col items-center justify-center">
                <div className="h-14 w-14 rounded-2xl bg-accent-subtle border border-accent/20 flex items-center justify-center text-accent mb-4">
                  <UploadCloud className="h-7 w-7" />
                </div>

                {selectedFile ? (
                  <div className="space-y-1">
                    <p className="text-title font-semibold text-primary">{selectedFile.name}</p>
                    <p className="text-caption text-secondary">
                      {(selectedFile.size / 1024).toFixed(1)} KB • Click or drop another file to replace
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <p className="text-subhead font-medium text-primary">
                      Drag and drop your expense report PDF or text file
                    </p>
                    <p className="text-caption text-secondary">
                      Supports PDF and TXT up to 10MB
                    </p>
                  </div>
                )}
              </div>
            </div>

            {errorMessage && (
              <div className="mt-4 p-3.5 rounded-lg bg-red-500/10 border border-red-500/20 text-caption text-red-600 dark:text-red-400 flex items-center gap-2">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Actions Bar */}
            <div className="mt-8 flex flex-col sm:flex-row items-center justify-between gap-4 pt-6 border-t border-subtle">
              <button
                type="button"
                onClick={handleLoadSample}
                className="inline-flex items-center gap-2 text-caption text-secondary hover:text-accent font-medium transition-colors cursor-pointer"
              >
                <Sparkles className="h-4 w-4 text-accent" />
                <span>Load Sample Corporate Expense Statement</span>
              </button>

              <Button
                variant="primary"
                size="lg"
                onClick={handleStartAudit}
                disabled={!selectedFile || isUploading}
                isLoading={isUploading}
                className="w-full sm:w-auto"
              >
                <span>Initiate Autonomous Audit</span>
                <ArrowRight className="h-4 w-4 ml-2" />
              </Button>
            </div>
          </div>
        ) : (
          /* Multi-Step Progress Tracker */
          <div className="py-4 space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-subtle">
              <div>
                <span className="text-caption font-mono text-muted uppercase">Audit Session #{auditStatus.id}</span>
                <h3 className="text-title font-semibold text-primary mt-0.5">
                  {selectedFile?.name || `Report #${auditStatus.id}`}
                </h3>
              </div>
              <div className="flex items-center gap-3">
                <Badge status={auditStatus.status}>{auditStatus.status}</Badge>
                {auditStatus.status === 'COMPLETE' && (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => navigate(`/reports/${auditStatus.id}`)}
                  >
                    <span>View Audit Findings</span>
                    <ArrowRight className="h-4 w-4 ml-1.5" />
                  </Button>
                )}
              </div>
            </div>

            {/* Step Pipeline Visualization */}
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              {STAGES.map((stage, idx) => {
                const isPassed = currentStageIndex > idx || auditStatus.status === 'COMPLETE';
                const isCurrent = currentStageIndex === idx && auditStatus.status !== 'COMPLETE';

                return (
                  <div
                    key={stage.key}
                    className={`p-4 rounded-xl border transition-all duration-200 ${
                      isPassed
                        ? 'border-accent/40 bg-accent-subtle/30 text-primary'
                        : isCurrent
                        ? 'border-accent bg-surface shadow-sm ring-1 ring-accent/30'
                        : 'border-subtle bg-surface-subtle/40 opacity-60 text-muted'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-caption font-mono text-secondary">0{idx + 1}</span>
                      {isPassed ? (
                        <CheckCircle2 className="h-4 w-4 text-accent" />
                      ) : isCurrent ? (
                        <span className="h-2 w-2 rounded-full bg-accent animate-ping" />
                      ) : null}
                    </div>
                    <div className="text-body font-semibold text-primary">{stage.label}</div>
                    <p className="text-caption text-secondary mt-1">{stage.desc}</p>
                  </div>
                );
              })}
            </div>

            {/* Line Item Counter Highlight */}
            {auditStatus.lineItemCount !== undefined && auditStatus.lineItemCount > 0 && (
              <div className="p-4 rounded-xl bg-surface-subtle border border-subtle flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <FileText className="h-5 w-5 text-accent" />
                  <span className="text-body text-primary">
                    <span className="font-semibold">{auditStatus.lineItemCount}</span> line items successfully extracted from invoice.
                  </span>
                </div>
                <span className="text-caption font-mono text-secondary">Gemini 2.5 Flash</span>
              </div>
            )}

            {/* Error or Completion Banner */}
            {auditStatus.status === 'FAILED' && (
              <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 space-y-2">
                <div className="flex items-center gap-2 font-medium">
                  <AlertCircle className="h-5 w-5" />
                  <span>Audit processing encountered a failure.</span>
                </div>
                {auditStatus.errorReason && (
                  <p className="text-caption">{auditStatus.errorReason}</p>
                )}
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setAuditStatus(null);
                    setReportId(null);
                  }}
                  className="mt-2"
                >
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Try Again
                </Button>
              </div>
            )}

            {auditStatus.status === 'COMPLETE' && (
              <div className="p-6 rounded-xl bg-surface-subtle border border-accent/30 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-accent text-white flex items-center justify-center">
                    <CheckCircle2 className="h-5 w-5" />
                  </div>
                  <div>
                    <h4 className="text-subhead font-semibold text-primary">
                      Audit successfully finalized
                    </h4>
                    <p className="text-caption text-secondary">
                      Findings, compliance scores, and deterministic checks have been computed.
                    </p>
                  </div>
                </div>

                <div className="pt-2 flex flex-col sm:flex-row gap-3">
                  <Button
                    variant="primary"
                    size="md"
                    onClick={() => navigate(`/reports/${auditStatus.id}`)}
                  >
                    <span>Inspect Detailed Findings</span>
                    <ArrowRight className="h-4 w-4 ml-2" />
                  </Button>
                  <Button
                    variant="secondary"
                    size="md"
                    onClick={() => {
                      setSelectedFile(null);
                      setAuditStatus(null);
                      setReportId(null);
                    }}
                  >
                    Upload Another Report
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </Card>
    </div>
  );
};
