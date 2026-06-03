// // src/pages/SmartSender.jsx
// // Smart WhatsApp Query Sender — unified SQL editor + template sender
// import { useState, useCallback } from 'react';
// import {
//   Row, Col, Card, Input, Button, Table, Typography, Tag, Space,
//   Divider, Alert, Spin, Progress, Badge, Tooltip, List, Tabs
// } from 'antd';
// import {
//   PlayCircleOutlined, SendOutlined, WhatsAppOutlined, DownloadOutlined,
//   ThunderboltOutlined, UserOutlined, EyeOutlined, ReloadOutlined,
//   CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined
// } from '@ant-design/icons';
// import toast from 'react-hot-toast';
// import api from '../api/axios';

// const { TextArea } = Input;
// const { Text, Title, Paragraph } = Typography;

// const SAMPLE_QUERY = `SELECT
//   e.name,
//   e.mob_no,
//   s.month,
//   s.amount
// FROM employee_details e
// JOIN salary_details s
//   ON e.s_no = s.det_s_no
// WHERE s.component = 'total'`;

// const DEFAULT_TEMPLATE = `Dear {{name}},
// Your salary for {{month}} of ₹{{amount}} has been credited to your account.

// Thank you! 🙏`;

// // Colour-code send result status
// const statusTag = (status) => {
//   if (status === 'SENT')    return <Tag color="green"  icon={<CheckCircleOutlined />}>SENT</Tag>;
//   if (status === 'FAILED')  return <Tag color="red"    icon={<CloseCircleOutlined />}>FAILED</Tag>;
//   if (status === 'SKIPPED') return <Tag color="orange">SKIPPED</Tag>;
//   return <Tag>{status}</Tag>;
// };

// export default function SmartSender() {
//   // ── Query state
//   const [sql, setSql]           = useState(SAMPLE_QUERY);
//   const [queryResult, setQueryResult] = useState(null);
//   const [queryLoading, setQueryLoading] = useState(false);
//   const [execMs, setExecMs]     = useState(null);
//   const [phoneColumn, setPhoneColumn] = useState(null);
//   const [recipients, setRecipients]   = useState([]);

//   // ── Template state
//   const [template, setTemplate] = useState(DEFAULT_TEMPLATE);
//   const [previewTab, setPreviewTab] = useState('template');
//   const [sendLoading, setSendLoading] = useState(false);
//   const [sendProgress, setSendProgress] = useState(0);
//   const [sendResults, setSendResults]   = useState([]);

//   // ─────────────────────────────────────────────────────────
//   // Execute query
//   // ─────────────────────────────────────────────────────────
//   const executeQuery = async () => {
//     if (!sql.trim()) return toast.error('Enter a SQL query');
//     setQueryLoading(true);
//     setSendResults([]);
//     try {
//       const res = await api.post('/api/query/execute-and-detect', { sql, page: 0, pageSize: 200 });
//       const d = res.data.data;
//       setQueryResult(d);
//       setPhoneColumn(d.phoneColumn);
//       setRecipients(d.recipients || []);
//       setExecMs(d.executionMs);

//       if (d.phoneColumn) {
//         toast.success(`Found ${d.recipients?.length} recipients via "${d.phoneColumn}" column`);
//       } else {
//         toast('Query ran successfully. No phone column detected automatically.', { icon: 'ℹ️' });
//       }
//     } catch (err) {
//       toast.error(err.response?.data?.message || 'Query failed');
//       setQueryResult(null);
//     } finally {
//       setQueryLoading(false);
//     }
//   };

//   // ─────────────────────────────────────────────────────────
//   // Export CSV
//   // ─────────────────────────────────────────────────────────
//   const exportCsv = () => {
//     if (!queryResult) return;
//     const { columns, rows } = queryResult;
//     const csvContent = [
//       columns.join(','),
//       ...rows.map(r => r.map(v => `"${v ?? ''}"`).join(','))
//     ].join('\n');
//     const blob = new Blob([csvContent], { type: 'text/csv' });
//     const url  = URL.createObjectURL(blob);
//     const a    = document.createElement('a');
//     a.href = url; a.download = 'query_result.csv'; a.click();
//   };

//   // ─────────────────────────────────────────────────────────
//   // Build row maps from query result
//   // ─────────────────────────────────────────────────────────
//   const buildRowMaps = useCallback(() => {
//     if (!queryResult) return [];
//     const { columns, rows } = queryResult;
//     return rows.map(row => {
//       const map = {};
//       columns.forEach((col, i) => { map[col] = row[i]; });
//       return map;
//     });
//   }, [queryResult]);

//   // Preview first message
//   const previewMessage = () => {
//     const rows = buildRowMaps();
//     if (!rows.length) return 'No data — run a query first.';
//     const row = rows[0];
//     return template.replace(/\{\{(\w+)\}\}/g, (_, key) => {
//       const val = Object.entries(row).find(([k]) => k.toLowerCase() === key.toLowerCase());
//       return val ? val[1] ?? key : `{{${key}}}`;
//     });
//   };

//   // ─────────────────────────────────────────────────────────
//   // Send template
//   // ─────────────────────────────────────────────────────────
//   const sendTemplate = async () => {
//     const rows = buildRowMaps();
//     if (!rows.length) return toast.error('Run a query first to get recipients');
//     if (!template.trim()) return toast.error('Enter a message template');
//     if (!phoneColumn && recipients.length === 0) {
//       return toast.error('No phone column detected. Ensure your query returns mob_no, mobile, or phone.');
//     }

//     setSendLoading(true);
//     setSendProgress(0);
//     setSendResults([]);

//     try {
//       // Simulate progress while waiting
//       const progressInterval = setInterval(() => {
//         setSendProgress(p => Math.min(p + 5, 90));
//       }, 300);

//       const res = await api.post('/api/whatsapp/send-template', {
//         template,
//         rows,
//         phoneKey: phoneColumn,
//       });

//       clearInterval(progressInterval);
//       setSendProgress(100);
//       setSendResults(res.data.data || []);
//       toast.success(res.data.message);
//     } catch (err) {
//       toast.error(err.response?.data?.message || 'Send failed');
//     } finally {
//       setSendLoading(false);
//     }
//   };

//   // ─────────────────────────────────────────────────────────
//   // Build Ant Design table columns from query result
//   // ─────────────────────────────────────────────────────────
//   const tableColumns = queryResult?.columns.map(col => ({
//     title: (
//       <span>
//         {col}
//         {col === phoneColumn && (
//           <Tag color="green" style={{ marginLeft: 4, fontSize: 10 }}>📱</Tag>
//         )}
//       </span>
//     ),
//     dataIndex: col,
//     key: col,
//     ellipsis: true,
//     width: 140,
//   }));

//   const tableData = queryResult?.rows.map((row, i) => {
//     const obj = { key: i };
//     queryResult.columns.forEach((col, j) => { obj[col] = row[j] != null ? String(row[j]) : ''; });
//     return obj;
//   });

//   return (
//     <div>
//       {/* Header */}
//       <div className="page-header" style={{ marginBottom: 16 }}>
//         <div>
//           <Title level={4} style={{ margin: 0 }}>
//             <ThunderboltOutlined style={{ color: '#25D366', marginRight: 8 }} />
//             Smart WhatsApp Query Sender
//           </Title>
//           <Text type="secondary">Execute SQL → Auto-detect recipients → Send personalised messages</Text>
//         </div>
//       </div>

//       <Row gutter={16} style={{ alignItems: 'flex-start' }}>

//         {/* ══════════════ LEFT — SQL + Results (60%) ══════════════ */}
//         <Col xs={24} lg={14}>

//           {/* SQL Editor */}
//           <Card
//             bordered={false}
//             style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
//             title={<span>🗄️ SQL Editor</span>}
//             extra={
//               <Space>
//                 {execMs != null && (
//                   <Tag icon={<ClockCircleOutlined />} color="blue">
//                     {execMs} ms
//                   </Tag>
//                 )}
//                 {queryResult && (
//                   <Tooltip title="Export CSV">
//                     <Button size="small" icon={<DownloadOutlined />} onClick={exportCsv}>CSV</Button>
//                   </Tooltip>
//                 )}
//               </Space>
//             }
//           >
//             <TextArea
//               value={sql}
//               onChange={e => setSql(e.target.value)}
//               rows={8}
//               style={{ fontFamily: 'monospace', fontSize: 13, marginBottom: 12 }}
//               placeholder="Enter your SELECT query here..."
//             />
//             <Button
//               type="primary"
//               icon={<PlayCircleOutlined />}
//               loading={queryLoading}
//               onClick={executeQuery}
//               style={{ background: '#25D366', borderColor: '#25D366', fontWeight: 600 }}
//               block
//             >
//               Execute Query
//             </Button>

//             {/* Security notice */}
//             <Alert
//               message="Only SELECT, SHOW, and DESCRIBE queries are permitted."
//               type="info" showIcon style={{ marginTop: 10, borderRadius: 8 }}
//             />
//           </Card>

//           {/* Query Results Table */}
//           {queryResult && (
//             <Card
//               bordered={false}
//               style={{ borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
//               title={
//                 <Space>
//                   <span>📊 Results</span>
//                   <Tag color="blue">{queryResult.totalRows} rows</Tag>
//                   {phoneColumn && (
//                     <Tag color="green" icon={<UserOutlined />}>
//                       Recipients: {recipients.length} via "{phoneColumn}"
//                     </Tag>
//                   )}
//                 </Space>
//               }
//             >
//               <Table
//                 size="small"
//                 columns={tableColumns}
//                 dataSource={tableData}
//                 scroll={{ x: 'max-content', y: 300 }}
//                 pagination={{ pageSize: 20, size: 'small' }}
//                 bordered
//               />
//             </Card>
//           )}
//         </Col>

//         {/* ══════════════ RIGHT — WhatsApp Panel (40%) ══════════════ */}
//         <Col xs={24} lg={10}>
//           <Card
//             bordered={false}
//             style={{ borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
//             title={<span><WhatsAppOutlined style={{ color: '#25D366' }} /> WhatsApp Panel</span>}
//           >

//             {/* Recipients summary */}
//             <div style={{
//               background: phoneColumn ? '#f6ffed' : '#fffbe6',
//               border: `1px solid ${phoneColumn ? '#b7eb8f' : '#ffe58f'}`,
//               borderRadius: 8, padding: '10px 14px', marginBottom: 16
//             }}>
//               <Text strong style={{ fontSize: 13 }}>
//                 {phoneColumn
//                   ? `✅ ${recipients.length} recipients detected (column: "${phoneColumn}")`
//                   : '⚠️ No phone column detected yet. Run a query first.'}
//               </Text>
//               {recipients.length > 0 && (
//                 <div style={{ marginTop: 6, maxHeight: 80, overflowY: 'auto' }}>
//                   {recipients.slice(0, 8).map((r, i) => (
//                     <Tag key={i} style={{ marginBottom: 4 }}>{r}</Tag>
//                   ))}
//                   {recipients.length > 8 && (
//                     <Tag color="default">+{recipients.length - 8} more</Tag>
//                   )}
//                 </div>
//               )}
//             </div>

//             <Divider style={{ margin: '12px 0' }} />

//             {/* Template editor + preview */}
//             <Tabs
//               activeKey={previewTab}
//               onChange={setPreviewTab}
//               size="small"
//               items={[
//                 {
//                   key: 'template',
//                   label: '✏️ Template',
//                   children: (
//                     <>
//                       <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
//                         Use <code>{'{{column_name}}'}</code> placeholders — e.g. <code>{'{{name}}'}</code>, <code>{'{{amount}}'}</code>
//                       </Text>
//                       <TextArea
//                         value={template}
//                         onChange={e => setTemplate(e.target.value)}
//                         rows={8}
//                         style={{ fontFamily: 'sans-serif', fontSize: 13 }}
//                         placeholder="Dear {{name}}, your salary for {{month}} is ₹{{amount}}"
//                       />
//                     </>
//                   ),
//                 },
//                 {
//                   key: 'preview',
//                   label: <span><EyeOutlined /> Preview</span>,
//                   children: (
//                     <div style={{
//                       background: '#dcf8c6',
//                       borderRadius: 12,
//                       padding: '14px 16px',
//                       minHeight: 160,
//                       fontFamily: 'sans-serif',
//                       fontSize: 14,
//                       whiteSpace: 'pre-wrap',
//                       boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
//                       position: 'relative'
//                     }}>
//                       <Text style={{ fontSize: 10, color: '#666', display: 'block', marginBottom: 4 }}>
//                         Preview (first row data)
//                       </Text>
//                       {previewMessage()}
//                       <div style={{ textAlign: 'right', marginTop: 8 }}>
//                         <Text style={{ fontSize: 10, color: '#999' }}>
//                           {new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
//                         </Text>
//                       </div>
//                     </div>
//                   ),
//                 },
//               ]}
//             />

//             <Divider style={{ margin: '16px 0' }} />

//             {/* Send progress */}
//             {sendLoading && (
//               <Progress
//                 percent={sendProgress}
//                 strokeColor={{ '0%': '#25D366', '100%': '#128C7E' }}
//                 style={{ marginBottom: 12 }}
//               />
//             )}

//             {/* Send button */}
//             <Button
//               type="primary"
//               icon={<SendOutlined />}
//               size="large"
//               loading={sendLoading}
//               disabled={recipients.length === 0}
//               onClick={sendTemplate}
//               block
//               style={{
//                 background: '#25D366', borderColor: '#25D366',
//                 fontWeight: 700, height: 48, fontSize: 15,
//               }}
//             >
//               {sendLoading
//                 ? 'Sending...'
//                 : `Send to ${recipients.length} Recipients`}
//             </Button>

//             {/* Send results log */}
//             {sendResults.length > 0 && (
//               <>
//                 <Divider style={{ margin: '16px 0' }}>
//                   <Text type="secondary" style={{ fontSize: 12 }}>Send Log</Text>
//                 </Divider>
//                 <div style={{ maxHeight: 200, overflowY: 'auto' }}>
//                   {sendResults.map((r, i) => (
//                     <div key={i} style={{
//                       display: 'flex', justifyContent: 'space-between',
//                       alignItems: 'center', padding: '4px 0',
//                       borderBottom: '1px solid #f0f0f0'
//                     }}>
//                       <Text style={{ fontSize: 12 }}>{r.recipient || r.row?.mob_no || 'unknown'}</Text>
//                       {statusTag(r.status)}
//                     </div>
//                   ))}
//                 </div>

//                 {/* Summary */}
//                 <Row gutter={8} style={{ marginTop: 12 }}>
//                   {[
//                     { label: 'Sent',    count: sendResults.filter(r => r.status === 'SENT').length,    color: '#52c41a' },
//                     { label: 'Failed',  count: sendResults.filter(r => r.status === 'FAILED').length,  color: '#ff4d4f' },
//                     { label: 'Skipped', count: sendResults.filter(r => r.status === 'SKIPPED').length, color: '#fa8c16' },
//                   ].map(s => (
//                     <Col span={8} key={s.label}>
//                       <div style={{ textAlign: 'center', background: s.color + '15', borderRadius: 8, padding: '6px 0' }}>
//                         <div style={{ fontSize: 18, fontWeight: 700, color: s.color }}>{s.count}</div>
//                         <Text style={{ fontSize: 11, color: s.color }}>{s.label}</Text>
//                       </div>
//                     </Col>
//                   ))}
//                 </Row>
//               </>
//             )}
//           </Card>
//         </Col>
//       </Row>
//     </div>
//   );
// }
// src/pages/SmartSender.jsx
// Smart WhatsApp Query Sender — SQL editor + template sender
// FIX: Default query fetches basic, HRA, and total separately (one row per employee)
//      Template now shows full salary breakdown
import { useState, useCallback } from 'react';
import {
  Row, Col, Card, Input, Button, Table, Typography, Tag, Space,
  Divider, Alert, Progress, Tooltip, Tabs, Select
} from 'antd';
import {
  PlayCircleOutlined, SendOutlined, WhatsAppOutlined, DownloadOutlined,
  ThunderboltOutlined, UserOutlined, EyeOutlined, ClockCircleOutlined,
  CheckCircleOutlined, CloseCircleOutlined, CodeOutlined
} from '@ant-design/icons';
import toast from 'react-hot-toast';
import api from '../api/axios';

const { TextArea } = Input;
const { Text, Title } = Typography;
const { Option } = Select;

// ── Sample Queries ──────────────────────────────────────────────────────────

const SAMPLE_QUERIES = [
  {
    label: '💰 Full Salary Breakdown (MySQL)',
    value: `SELECT
  e.name,
  e.mob_no,
  MAX(CASE WHEN s.component = 'basic' THEN s.amount END) AS basic,
  MAX(CASE WHEN s.component = 'HRA'   THEN s.amount END) AS hra,
  MAX(CASE WHEN s.component = 'total' THEN s.amount END) AS total,
  s.month
FROM employee_details e
JOIN salary_details s ON e.s_no = s.det_s_no
GROUP BY e.s_no, e.name, e.mob_no, s.month`,
  },
  {
    label: '💰 Full Salary Breakdown (SQL Server)',
    value: `SELECT
  e.name,
  e.mob_no,
  MAX(CASE WHEN s.component = 'basic' THEN s.amount END) AS basic,
  MAX(CASE WHEN s.component = 'HRA'   THEN s.amount END) AS hra,
  MAX(CASE WHEN s.component = 'total' THEN s.amount END) AS total,
  s.month
FROM employee_details e
JOIN salary_details s ON e.s_no = s.det_s_no
GROUP BY e.s_no, e.name, e.mob_no, s.month`,
  },
  {
    label: '📋 Total Salary Only',
    value: `SELECT
  e.name,
  e.mob_no,
  s.month,
  s.amount AS total
FROM employee_details e
JOIN salary_details s ON e.s_no = s.det_s_no
WHERE s.component = 'total'`,
  },
  {
    label: '👥 All Employees',
    value: `SELECT name, mob_no, designation, department
FROM employee_details`,
  },
];

// ── Default Template — full salary breakdown ────────────────────────────────

const DEFAULT_TEMPLATE = `Dear {{name}},

Your salary details for *{{month}}* are as follows:

💵 Basic Salary : ₹{{basic}}
🏠 HRA          : ₹{{hra}}
━━━━━━━━━━━━━━━━━
💰 Total Salary : ₹{{total}}

Your salary has been credited to your account.
Thank you! 🙏

— HR Department`;

// ── Status tag helper ───────────────────────────────────────────────────────

const statusTag = (status) => {
  if (status === 'SENT')    return <Tag color="green"  icon={<CheckCircleOutlined />}>SENT</Tag>;
  if (status === 'FAILED')  return <Tag color="red"    icon={<CloseCircleOutlined />}>FAILED</Tag>;
  if (status === 'SKIPPED') return <Tag color="orange">SKIPPED</Tag>;
  return <Tag>{status}</Tag>;
};

// ────────────────────────────────────────────────────────────────────────────

export default function SmartSender() {
  // Query state
  const [sql, setSql]                   = useState(SAMPLE_QUERIES[0].value);
  const [queryResult, setQueryResult]   = useState(null);
  const [queryLoading, setQueryLoading] = useState(false);
  const [execMs, setExecMs]             = useState(null);
  const [phoneColumn, setPhoneColumn]   = useState(null);
  const [recipients, setRecipients]     = useState([]);

  // Template state
  const [template, setTemplate]         = useState(DEFAULT_TEMPLATE);
  const [previewTab, setPreviewTab]     = useState('template');
  const [sendLoading, setSendLoading]   = useState(false);
  const [sendProgress, setSendProgress] = useState(0);
  const [sendResults, setSendResults]   = useState([]);

  // ── Execute query ────────────────────────────────────────────────────────

  const executeQuery = async () => {
    if (!sql.trim()) return toast.error('Enter a SQL query');
    setQueryLoading(true);
    setSendResults([]);
    try {
      const res = await api.post('/api/query/execute-and-detect', {
        sql, page: 0, pageSize: 500,
      });
      const d = res.data.data;
      setQueryResult(d);
      setPhoneColumn(d.phoneColumn);
      setRecipients(d.recipients || []);
      setExecMs(d.executionMs);

      if (d.phoneColumn) {
        toast.success(
          `✅ ${d.recipients?.length} recipient${d.recipients?.length === 1 ? '' : 's'} found via "${d.phoneColumn}"`
        );
      } else {
        toast('Query ran. No phone column detected automatically.', { icon: 'ℹ️' });
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Query failed');
      setQueryResult(null);
    } finally {
      setQueryLoading(false);
    }
  };

  // ── Export CSV ────────────────────────────────────────────────────────────

  const exportCsv = () => {
    if (!queryResult) return;
    const { columns, rows } = queryResult;
    const csv = [
      columns.join(','),
      ...rows.map(r => r.map(v => `"${v ?? ''}"`).join(',')),
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href = url; a.download = 'salary_report.csv'; a.click();
  };

  // ── Build row maps ────────────────────────────────────────────────────────

  const buildRowMaps = useCallback(() => {
    if (!queryResult) return [];
    const { columns, rows } = queryResult;
    return rows.map(row => {
      const map = {};
      columns.forEach((col, i) => { map[col] = row[i] != null ? String(row[i]) : ''; });
      return map;
    });
  }, [queryResult]);

  // ── Preview first message ─────────────────────────────────────────────────

  const previewMessage = () => {
    const rows = buildRowMaps();
    if (!rows.length) return 'No data — run a query first.';
    const row = rows[0];
    return template.replace(/\{\{(\w+)\}\}/g, (_, key) => {
      const entry = Object.entries(row).find(
        ([k]) => k.toLowerCase() === key.toLowerCase()
      );
      return entry ? entry[1] : `{{${key}}}`;
    });
  };

  // ── Send template ─────────────────────────────────────────────────────────

  const sendTemplate = async () => {
    const rows = buildRowMaps();
    if (!rows.length)    return toast.error('Run a query first to get recipients');
    if (!template.trim()) return toast.error('Enter a message template');
    if (!phoneColumn && recipients.length === 0) {
      return toast.error(
        'No phone column detected. Ensure your query returns mob_no, mobile, or phone.'
      );
    }

    setSendLoading(true);
    setSendProgress(10);
    setSendResults([]);

    const interval = setInterval(() =>
      setSendProgress(p => Math.min(p + 8, 88)), 400
    );

    try {
      const res = await api.post('/api/whatsapp/send-template', {
        template,
        rows,
        phoneKey: phoneColumn,
      });
      clearInterval(interval);
      setSendProgress(100);
      setSendResults(res.data.data || []);
      toast.success(res.data.message);
    } catch (err) {
      clearInterval(interval);
      toast.error(err.response?.data?.message || 'Send failed');
    } finally {
      setSendLoading(false);
    }
  };

  // ── Table columns ─────────────────────────────────────────────────────────

  const tableColumns = queryResult?.columns.map(col => ({
    title: (
      <span>
        {col}
        {col === phoneColumn && (
          <Tag color="green" style={{ marginLeft: 4, fontSize: 10, padding: '0 4px' }}>📱</Tag>
        )}
      </span>
    ),
    dataIndex: col,
    key: col,
    ellipsis: true,
    width: 130,
  }));

  const tableData = queryResult?.rows.map((row, i) => {
    const obj = { key: i };
    queryResult.columns.forEach((col, j) => {
      obj[col] = row[j] != null ? String(row[j]) : '';
    });
    return obj;
  });

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div>
      {/* Page header */}
      <div style={{ marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>
          <ThunderboltOutlined style={{ color: '#25D366', marginRight: 8 }} />
          Smart WhatsApp Query Sender
        </Title>
        <Text type="secondary">
          Execute SQL → Auto-detect recipients → Send personalised messages (Basic + HRA + Total)
        </Text>
      </div>

      <Row gutter={16} style={{ alignItems: 'flex-start' }}>

        {/* ═══════════════ LEFT — SQL + Results (60%) ═══════════════ */}
        <Col xs={24} lg={14}>

          {/* SQL Editor */}
          <Card
            bordered={false}
            style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
            title={<span><CodeOutlined /> SQL Editor</span>}
            extra={
              <Space>
                {execMs != null && (
                  <Tag icon={<ClockCircleOutlined />} color="blue">{execMs} ms</Tag>
                )}
                {queryResult && (
                  <Tooltip title="Export CSV">
                    <Button size="small" icon={<DownloadOutlined />} onClick={exportCsv}>CSV</Button>
                  </Tooltip>
                )}
              </Space>
            }
          >
            {/* Sample query picker */}
            <div style={{ marginBottom: 10 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>Load sample query: </Text>
              <Select
                size="small"
                style={{ width: '100%', marginTop: 4 }}
                placeholder="— select a sample query —"
                onChange={val => setSql(val)}
              >
                {SAMPLE_QUERIES.map((q, i) => (
                  <Option key={i} value={q.value}>{q.label}</Option>
                ))}
              </Select>
            </div>

            <TextArea
              value={sql}
              onChange={e => setSql(e.target.value)}
              rows={9}
              style={{ fontFamily: 'monospace', fontSize: 13, marginBottom: 12 }}
              placeholder="Enter your SELECT query..."
            />

            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              loading={queryLoading}
              onClick={executeQuery}
              block
              style={{ background: '#25D366', borderColor: '#25D366', fontWeight: 600 }}
            >
              Execute Query
            </Button>

            <Alert
              message="Only SELECT, SHOW, and DESCRIBE queries are permitted."
              type="info" showIcon style={{ marginTop: 10, borderRadius: 8 }}
            />
          </Card>

          {/* Results Table */}
          {queryResult && (
            <Card
              bordered={false}
              style={{ borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
              title={
                <Space>
                  <span>📊 Results</span>
                  <Tag color="blue">{queryResult.totalRows} rows</Tag>
                  {phoneColumn && (
                    <Tag color="green" icon={<UserOutlined />}>
                      {recipients.length} recipients via "{phoneColumn}"
                    </Tag>
                  )}
                </Space>
              }
            >
              <Table
                size="small"
                columns={tableColumns}
                dataSource={tableData}
                scroll={{ x: 'max-content', y: 280 }}
                pagination={{ pageSize: 20, size: 'small' }}
                bordered
              />
            </Card>
          )}
        </Col>

        {/* ═══════════════ RIGHT — WhatsApp Panel (40%) ═══════════════ */}
        <Col xs={24} lg={10}>
          <Card
            bordered={false}
            style={{ borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
            title={
              <span>
                <WhatsAppOutlined style={{ color: '#25D366', marginRight: 6 }} />
                WhatsApp Panel
              </span>
            }
          >

            {/* Recipients status */}
            <div style={{
              background: phoneColumn ? '#f6ffed' : '#fffbe6',
              border: `1px solid ${phoneColumn ? '#b7eb8f' : '#ffe58f'}`,
              borderRadius: 8,
              padding: '10px 14px',
              marginBottom: 14,
            }}>
              <Text strong style={{ fontSize: 13 }}>
                {phoneColumn
                  ? `✅ ${recipients.length} recipients detected (column: "${phoneColumn}")`
                  : '⚠️ No phone column detected yet. Run a query first.'}
              </Text>
              {recipients.length > 0 && (
                <div style={{ marginTop: 6, maxHeight: 72, overflowY: 'auto' }}>
                  {recipients.slice(0, 6).map((r, i) => (
                    <Tag key={i} style={{ marginBottom: 4 }}>{r}</Tag>
                  ))}
                  {recipients.length > 6 && (
                    <Tag color="default">+{recipients.length - 6} more</Tag>
                  )}
                </div>
              )}
            </div>

            {/* Template / Preview tabs */}
            <Tabs
              activeKey={previewTab}
              onChange={setPreviewTab}
              size="small"
              items={[
                {
                  key: 'template',
                  label: '✏️ Template',
                  children: (
                    <>
                      <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
                        Placeholders: <code>{'{{name}}'}</code> <code>{'{{month}}'}</code>{' '}
                        <code>{'{{basic}}'}</code> <code>{'{{hra}}'}</code> <code>{'{{total}}'}</code>
                        {' '}(must match column names exactly)
                      </Text>
                      <TextArea
                        value={template}
                        onChange={e => setTemplate(e.target.value)}
                        rows={10}
                        style={{ fontFamily: 'sans-serif', fontSize: 13 }}
                      />
                    </>
                  ),
                },
                {
                  key: 'preview',
                  label: <span><EyeOutlined /> Preview</span>,
                  children: (
                    <div style={{
                      background: '#dcf8c6',
                      borderRadius: 12,
                      padding: '14px 16px',
                      minHeight: 220,
                      fontFamily: 'sans-serif',
                      fontSize: 14,
                      whiteSpace: 'pre-wrap',
                      lineHeight: 1.7,
                      boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
                    }}>
                      <Text style={{ fontSize: 10, color: '#666', display: 'block', marginBottom: 6 }}>
                        Preview using first row of data
                      </Text>
                      {previewMessage()}
                      <div style={{ textAlign: 'right', marginTop: 10 }}>
                        <Text style={{ fontSize: 10, color: '#999' }}>
                          {new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} ✓✓
                        </Text>
                      </div>
                    </div>
                  ),
                },
              ]}
            />

            <Divider style={{ margin: '14px 0' }} />

            {/* Send progress bar */}
            {sendLoading && (
              <Progress
                percent={sendProgress}
                strokeColor={{ '0%': '#25D366', '100%': '#128C7E' }}
                style={{ marginBottom: 12 }}
              />
            )}

            {/* Send button */}
            <Button
              type="primary"
              icon={<SendOutlined />}
              size="large"
              loading={sendLoading}
              disabled={recipients.length === 0}
              onClick={sendTemplate}
              block
              style={{
                background: '#25D366', borderColor: '#25D366',
                fontWeight: 700, height: 48, fontSize: 15,
              }}
            >
              {sendLoading
                ? `Sending... ${sendProgress}%`
                : `Send to ${recipients.length} Recipients`}
            </Button>

            {/* Send results log */}
            {sendResults.length > 0 && (
              <>
                <Divider style={{ margin: '14px 0' }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>Send Log</Text>
                </Divider>

                <div style={{ maxHeight: 180, overflowY: 'auto' }}>
                  {sendResults.map((r, i) => (
                    <div key={i} style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '4px 0',
                      borderBottom: '1px solid #f0f0f0',
                    }}>
                      <Text style={{ fontSize: 12 }} ellipsis>
                        {r.recipient || 'unknown'}
                      </Text>
                      {statusTag(r.status)}
                    </div>
                  ))}
                </div>

                {/* Summary counts */}
                <Row gutter={8} style={{ marginTop: 12 }}>
                  {[
                    { label: 'Sent',    count: sendResults.filter(r => r.status === 'SENT').length,    color: '#52c41a' },
                    { label: 'Failed',  count: sendResults.filter(r => r.status === 'FAILED').length,  color: '#ff4d4f' },
                    { label: 'Skipped', count: sendResults.filter(r => r.status === 'SKIPPED').length, color: '#fa8c16' },
                  ].map(s => (
                    <Col span={8} key={s.label}>
                      <div style={{
                        textAlign: 'center',
                        background: s.color + '15',
                        borderRadius: 8,
                        padding: '6px 0',
                        border: `1px solid ${s.color}30`,
                      }}>
                        <div style={{ fontSize: 20, fontWeight: 800, color: s.color }}>{s.count}</div>
                        <Text style={{ fontSize: 11, color: s.color }}>{s.label}</Text>
                      </div>
                    </Col>
                  ))}
                </Row>
              </>
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}
