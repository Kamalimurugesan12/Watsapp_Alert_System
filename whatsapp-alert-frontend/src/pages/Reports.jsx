// // src/pages/Reports.jsx
// import { useState } from 'react';
// import {
//   Card, Select, Button, Input, Table, Typography, Space, Row, Col,
//   Divider, Tag, Alert
// } from 'antd';
// import {
//   FileExcelOutlined, FilePdfOutlined, DownloadOutlined,
//   PlayCircleOutlined, SendOutlined
// } from '@ant-design/icons';
// import toast from 'react-hot-toast';
// import api from '../api/axios';

// const { TextArea } = Input;
// const { Title, Text } = Typography;

// const REPORT_TEMPLATES = [
//   {
//     label: 'Basic Salary Report',
//     value: 'salary',
//     sql: 'SELECT emp_id, emp_name, department, basic_salary, hra, gross_salary FROM employee_salary ORDER BY department',
//   },
//   {
//     label: 'HRA Report',
//     value: 'hra',
//     sql: 'SELECT department, COUNT(*) AS total_employees, SUM(hra) AS total_hra FROM employee_salary GROUP BY department',
//   },
//   {
//     label: 'Employee Summary',
//     value: 'summary',
//     sql: 'SELECT emp_id, emp_name, designation, join_date FROM employees ORDER BY join_date DESC',
//   },
//   { label: 'Custom SQL', value: 'custom', sql: '' },
// ];

// export default function Reports() {
//   const [selectedTemplate, setSelectedTemplate] = useState(null);
//   const [sql, setSql] = useState('');
//   const [reportName, setReportName] = useState('Report');
//   const [preview, setPreview] = useState(null);
//   const [previewLoading, setPreviewLoading] = useState(false);
//   const [downloading, setDownloading] = useState(false);

//   const selectTemplate = (value) => {
//     const tpl = REPORT_TEMPLATES.find(t => t.value === value);
//     setSelectedTemplate(value);
//     setSql(tpl?.sql || '');
//     setReportName(tpl?.label || 'Report');
//     setPreview(null);
//   };

//   const previewReport = async () => {
//     if (!sql.trim()) return toast.error('Enter or select a SQL query');
//     setPreviewLoading(true);
//     try {
//       const res = await api.post('/api/database/query', { sql, page: 0, pageSize: 50 });
//       setPreview(res.data.data);
//       toast.success(`Preview loaded: ${res.data.data.totalRows} rows`);
//     } catch (err) {
//       toast.error(err.response?.data?.message || 'Preview failed');
//     } finally {
//       setPreviewLoading(false);
//     }
//   };

//   const downloadExcel = async () => {
//     setDownloading(true);
//     try {
//       const res = await api.post('/api/reports/excel', { sql, reportName },
//         { responseType: 'blob' });
//       const url = URL.createObjectURL(res.data);
//       const a = document.createElement('a');
//       a.href = url;
//       a.download = `${reportName}.xlsx`;
//       a.click();
//       toast.success('Excel report downloaded');
//     } catch (err) {
//       toast.error('Excel export failed');
//     } finally {
//       setDownloading(false);
//     }
//   };

//   const downloadCsv = async () => {
//     setDownloading(true);
//     try {
//       const res = await api.post('/api/reports/csv', { sql, reportName },
//         { responseType: 'blob' });
//       const url = URL.createObjectURL(res.data);
//       const a = document.createElement('a');
//       a.href = url;
//       a.download = `${reportName}.csv`;
//       a.click();
//       toast.success('CSV report downloaded');
//     } catch {
//       toast.error('CSV export failed');
//     } finally {
//       setDownloading(false);
//     }
//   };

//   const antColumns = preview?.columns?.map(col => ({
//     title: col, dataIndex: col, key: col, ellipsis: true, width: 140,
//     render: val => val == null ? <Text type="secondary">NULL</Text> : String(val)
//   })) || [];

//   const antData = preview?.rows?.map((row, i) => {
//     const obj = { key: i };
//     preview.columns.forEach((c, j) => { obj[c] = row[j]; });
//     return obj;
//   }) || [];

//   return (
//     <div>
//       <div className="page-header">
//         <Title level={4} style={{ margin: 0 }}>📄 Report Generator</Title>
//       </div>

//       <Row gutter={[16, 16]}>
//         <Col xs={24} lg={8}>
//           <Card style={{ borderRadius: 12 }} title="⚙️ Report Configuration">
//             <Space direction="vertical" style={{ width: '100%' }} size="middle">
//               <div>
//                 <Text strong>Report Type</Text>
//                 <Select
//                   style={{ width: '100%', marginTop: 4 }}
//                   placeholder="Select a report template"
//                   options={REPORT_TEMPLATES.map(t => ({ label: t.label, value: t.value }))}
//                   onChange={selectTemplate}
//                 />
//               </div>

//               <div>
//                 <Text strong>Report Name</Text>
//                 <Input
//                   value={reportName}
//                   onChange={e => setReportName(e.target.value)}
//                   style={{ marginTop: 4 }}
//                 />
//               </div>

//               <div>
//                 <Text strong>SQL Query</Text>
//                 <TextArea
//                   value={sql}
//                   onChange={e => setSql(e.target.value)}
//                   rows={5}
//                   style={{ fontFamily: 'monospace', fontSize: 12, marginTop: 4 }}
//                   placeholder="SELECT * FROM your_table"
//                 />
//               </div>

//               <Button
//                 icon={<PlayCircleOutlined />}
//                 onClick={previewReport}
//                 loading={previewLoading}
//                 block
//               >
//                 Preview Report
//               </Button>

//               <Divider style={{ margin: '4px 0' }}>Download</Divider>

//               <Space style={{ width: '100%' }}>
//                 <Button
//                   icon={<FileExcelOutlined />}
//                   onClick={downloadExcel}
//                   loading={downloading}
//                   block
//                   style={{ color: '#217346', borderColor: '#217346' }}
//                 >
//                   Excel (.xlsx)
//                 </Button>
//                 <Button
//                   icon={<DownloadOutlined />}
//                   onClick={downloadCsv}
//                   loading={downloading}
//                   block
//                 >
//                   CSV
//                 </Button>
//               </Space>
//             </Space>
//           </Card>
//         </Col>

//         <Col xs={24} lg={16}>
//           <Card
//             style={{ borderRadius: 12 }}
//             title={
//               preview
//                 ? <Space>
//                     <span>Preview: {reportName}</span>
//                     <Tag color="green">{preview.totalRows} rows</Tag>
//                   </Space>
//                 : 'Report Preview'
//             }
//           >
//             {!preview ? (
//               <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
//                 <FileExcelOutlined style={{ fontSize: 48, marginBottom: 12 }} />
//                 <br />
//                 <Text type="secondary">Configure and preview a report on the left</Text>
//               </div>
//             ) : (
//               <Table
//                 columns={antColumns}
//                 dataSource={antData}
//                 scroll={{ x: 'max-content', y: 400 }}
//                 size="small"
//                 bordered
//                 pagination={{ pageSize: 20, showSizeChanger: false }}
//               />
//             )}
//           </Card>
//         </Col>
//       </Row>
//     </div>
//   );
// }
// src/pages/Reports.jsx
// ─────────────────────────────────────────────────────────────────────────────
// Enhanced Reports Page
//   • Report Type dropdown loads static + saved templates from DB
//   • "Save Query Template" modal persists queries for reuse
//   • Download as Excel / CSV / PDF
//   • Send PDF directly via WhatsApp
// ─────────────────────────────────────────────────────────────────────────────
import { useState, useEffect, useCallback } from 'react';
import {
  Card, Select, Button, Input, Table, Typography, Space, Row, Col,
  Divider, Tag, Modal, Form, Alert, Tooltip, Spin, Badge, Popconfirm
} from 'antd';
import {
  FileExcelOutlined, FilePdfOutlined, DownloadOutlined,
  PlayCircleOutlined, SendOutlined, SaveOutlined, DeleteOutlined,
  WhatsAppOutlined, PlusOutlined, ReloadOutlined, DatabaseOutlined
} from '@ant-design/icons';
import toast from 'react-hot-toast';
import api from '../api/axios';

const { TextArea } = Input;
const { Title, Text } = Typography;
const { Option, OptGroup } = Select;

// Static built-in templates (always shown even before DB loads)
const STATIC_OPTIONS = [
  { label: 'Employee Report',   value: '__employee__' },
  { label: 'Salary Report',     value: '__salary__' },
  { label: 'Attendance Report', value: '__attendance__' },
];

export default function Reports() {
  // ── State ──────────────────────────────────────────────────────────────────
  const [savedTemplates, setSavedTemplates] = useState([]);
  const [loadingTemplates, setLoadingTemplates] = useState(false);

  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [sql, setSql]             = useState('');
  const [reportName, setReportName] = useState('Report');
  const [dbType, setDbType]       = useState('MYSQL');

  const [preview, setPreview]           = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [downloading, setDownloading]   = useState(false);

  // Save template modal
  const [saveModal, setSaveModal]   = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [saveForm]                  = Form.useForm();

  // WhatsApp PDF modal
  const [waModal, setWaModal]         = useState(false);
  const [waLoading, setWaLoading]     = useState(false);
  const [waForm]                      = Form.useForm();

  // ── Load saved templates on mount ──────────────────────────────────────────
  const loadTemplates = useCallback(async () => {
    setLoadingTemplates(true);
    try {
      const res = await api.get('/api/report-templates');
      setSavedTemplates(res.data.data || []);
    } catch {
      // silent — templates panel is optional
    } finally {
      setLoadingTemplates(false);
    }
  }, []);

  useEffect(() => { loadTemplates(); }, [loadTemplates]);

  // ── Select a template ──────────────────────────────────────────────────────
  const selectTemplate = (value) => {
    setSelectedTemplate(value);
    setPreview(null);

    // Try saved templates first
    const saved = savedTemplates.find(t => String(t.id) === String(value));
    if (saved) {
      setSql(saved.sqlQuery);
      setReportName(saved.name);
      setDbType(saved.databaseType || 'MYSQL');
      return;
    }

    // Static fallbacks
    const staticMap = {
      '__employee__':   {
        name: 'Employee Report',
        sql:  'SELECT s_no, name, designation, department, mob_no, email FROM employee_details ORDER BY department, name',
        db:   'MYSQL',
      },
      '__salary__': {
        name: 'Salary Report',
        sql:  "SELECT e.name, e.mob_no, MAX(CASE WHEN s.component='basic' THEN s.amount END) AS basic, MAX(CASE WHEN s.component='HRA' THEN s.amount END) AS hra, MAX(CASE WHEN s.component='total' THEN s.amount END) AS total, s.month FROM employee_details e JOIN salary_details s ON e.s_no = s.det_s_no GROUP BY e.s_no, e.name, e.mob_no, s.month",
        db:   'MYSQL',
      },
      '__attendance__': {
        name: 'Attendance Report',
        sql:  'SELECT name, department, designation FROM employee_details ORDER BY department',
        db:   'MYSQL',
      },
    };
    const found = staticMap[value];
    if (found) {
      setSql(found.sql);
      setReportName(found.name);
      setDbType(found.db);
    }
  };

  // ── Preview ────────────────────────────────────────────────────────────────
  const previewReport = async () => {
    if (!sql.trim()) return toast.error('Enter or select a SQL query');
    setPreviewLoading(true);
    try {
      const res = await api.post('/api/database/query', { sql, page: 0, pageSize: 50 });
      setPreview(res.data.data);
      toast.success(`Preview loaded — ${res.data.data.totalRows} rows`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Preview failed');
    } finally {
      setPreviewLoading(false);
    }
  };

  // ── Downloads ──────────────────────────────────────────────────────────────
  const download = async (endpoint, ext, mime) => {
    if (!sql.trim()) return toast.error('Enter a SQL query first');
    setDownloading(true);
    try {
      const res = await api.post(endpoint, { sql, reportName }, { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([res.data], { type: mime }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `${reportName}.${ext}`;
      a.click();
      toast.success(`${ext.toUpperCase()} downloaded`);
    } catch {
      toast.error(`${ext.toUpperCase()} download failed`);
    } finally {
      setDownloading(false);
    }
  };

  // ── Save template ──────────────────────────────────────────────────────────
  const openSaveModal = () => {
    saveForm.setFieldsValue({
      name:         reportName !== 'Report' ? reportName : '',
      description:  '',
      databaseType: dbType || 'MYSQL',
    });
    setSaveModal(true);
  };

  const handleSaveTemplate = async (values) => {
    setSaveLoading(true);
    try {
      await api.post('/api/report-templates', {
        name:         values.name,
        description:  values.description || '',
        sqlQuery:     sql,
        databaseType: values.databaseType || 'MYSQL',
      });
      toast.success(`Template "${values.name}" saved!`);
      setSaveModal(false);
      saveForm.resetFields();
      loadTemplates();   // refresh dropdown
    } catch (err) {
      const msg = err.response?.data?.message || 'Save failed';
      toast.error(msg);
    } finally {
      setSaveLoading(false);
    }
  };

  // ── Delete template ────────────────────────────────────────────────────────
  const deleteTemplate = async (id, name) => {
    try {
      await api.delete(`/api/report-templates/${id}`);
      toast.success(`"${name}" deleted`);
      if (String(selectedTemplate) === String(id)) {
        setSelectedTemplate(null);
        setSql('');
        setReportName('Report');
      }
      loadTemplates();
    } catch {
      toast.error('Delete failed');
    }
  };

  // ── WhatsApp PDF send ─────────────────────────────────────────────────────
  const handleSendWhatsApp = async (values) => {
    if (!sql.trim()) { toast.error('No SQL query to generate PDF from'); return; }
    setWaLoading(true);
    try {
      const res = await api.post('/api/reports/send-whatsapp-pdf', {
        phoneNumber: values.phoneNumber,
        message:     values.message || 'Please find the report attached.',
        query:       sql,
        reportName,
      });
      if (res.data.success) {
        toast.success('PDF sent via WhatsApp!');
        setWaModal(false);
        waForm.resetFields();
      } else {
        toast.error(res.data.message);
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'WhatsApp send failed');
    } finally {
      setWaLoading(false);
    }
  };

  // ── Table columns / data ───────────────────────────────────────────────────
  const antColumns = preview?.columns?.map(col => ({
    title: col, dataIndex: col, key: col, ellipsis: true, width: 140,
    render: val => val == null
      ? <Text type="secondary" style={{ fontSize: 11 }}>NULL</Text>
      : String(val)
  })) || [];

  const antData = preview?.rows?.map((row, i) => {
    const obj = { key: i };
    preview.columns.forEach((c, j) => { obj[c] = row[j]; });
    return obj;
  }) || [];

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div>
      {/* Page header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>📄 Report Generator</Title>
          <Text type="secondary">Generate, download, and share reports</Text>
        </div>
        {preview && (
          <Tag color="green">{preview.totalRows} records loaded</Tag>
        )}
      </div>

      <Row gutter={[16, 16]}>

        {/* ── LEFT: Config panel ── */}
        <Col xs={24} lg={8}>

          {/* Report Type dropdown */}
          <Card
            bordered={false}
            style={{ borderRadius: 12, marginBottom: 16, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
            title={
              <Space>
                <span>⚙️ Configuration</span>
                {loadingTemplates && <Spin size="small" />}
              </Space>
            }
            extra={
              <Tooltip title="Reload saved templates">
                <Button type="text" size="small" icon={<ReloadOutlined />} onClick={loadTemplates} />
              </Tooltip>
            }
          >
            <Space direction="vertical" style={{ width: '100%' }} size="middle">

              {/* Report Type */}
              <div>
                <Text strong style={{ display: 'block', marginBottom: 4 }}>Report Type</Text>
                <Select
                  style={{ width: '100%' }}
                  placeholder="Select a report template"
                  value={selectedTemplate}
                  onChange={selectTemplate}
                  allowClear
                  onClear={() => { setSelectedTemplate(null); setSql(''); setReportName('Report'); }}
                >
                  <OptGroup label="Built-in Templates">
                    {STATIC_OPTIONS.map(o => (
                      <Option key={o.value} value={o.value}>{o.label}</Option>
                    ))}
                  </OptGroup>

                  {savedTemplates.length > 0 && (
                    <OptGroup label={`Saved Templates (${savedTemplates.length})`}>
                      {savedTemplates.map(t => (
                        <Option key={t.id} value={String(t.id)}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span>{t.name}</span>
                            <Tag
                              color={t.databaseType === 'SQLSERVER' ? 'blue' : 'orange'}
                              style={{ fontSize: 9, marginLeft: 6, padding: '0 4px' }}
                            >
                              {t.databaseType}
                            </Tag>
                          </div>
                        </Option>
                      ))}
                    </OptGroup>
                  )}
                </Select>
              </div>

              {/* Report Name */}
              <div>
                <Text strong style={{ display: 'block', marginBottom: 4 }}>Report Name</Text>
                <Input value={reportName} onChange={e => setReportName(e.target.value)} />
              </div>

              {/* SQL Query */}
              <div>
                <Text strong style={{ display: 'block', marginBottom: 4 }}>SQL Query</Text>
                <TextArea
                  value={sql}
                  onChange={e => setSql(e.target.value)}
                  rows={7}
                  style={{ fontFamily: 'monospace', fontSize: 12 }}
                  placeholder="SELECT * FROM your_table"
                />
              </div>

              {/* Action buttons */}
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={previewReport}
                loading={previewLoading}
                block
                style={{ background: '#25D366', borderColor: '#25D366' }}
              >
                Preview Report
              </Button>

              <Button
                icon={<SaveOutlined />}
                onClick={openSaveModal}
                block
                disabled={!sql.trim()}
              >
                Save as Template
              </Button>
            </Space>
          </Card>

          {/* Download / Send buttons */}
          <Card
            bordered={false}
            style={{ borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
            title="📥 Export & Share"
          >
            <Space direction="vertical" style={{ width: '100%' }} size={8}>
              <Row gutter={8}>
                <Col span={12}>
                  <Button
                    icon={<FileExcelOutlined />}
                    onClick={() => download('/api/reports/excel', 'xlsx',
                      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')}
                    loading={downloading}
                    block
                    style={{ color: '#217346', borderColor: '#217346' }}
                    disabled={!sql.trim()}
                  >
                    Excel
                  </Button>
                </Col>
                <Col span={12}>
                  <Button
                    icon={<DownloadOutlined />}
                    onClick={() => download('/api/reports/csv', 'csv', 'text/csv')}
                    loading={downloading}
                    block
                    disabled={!sql.trim()}
                  >
                    CSV
                  </Button>
                </Col>
              </Row>

              <Button
                icon={<FilePdfOutlined />}
                onClick={() => download('/api/reports/download-pdf', 'pdf', 'application/pdf')}
                loading={downloading}
                block
                type="primary"
                danger
                disabled={!sql.trim()}
              >
                Download PDF
              </Button>

              <Divider style={{ margin: '8px 0' }} />

              <Button
                icon={<WhatsAppOutlined />}
                onClick={() => {
                  if (!sql.trim()) { toast.error('Enter a SQL query first'); return; }
                  waForm.resetFields();
                  setWaModal(true);
                }}
                block
                style={{ background: '#25D366', borderColor: '#25D366', color: '#fff' }}
                disabled={!sql.trim()}
              >
                Send PDF via WhatsApp
              </Button>
            </Space>
          </Card>

          {/* Saved templates list with delete */}
          {savedTemplates.length > 0 && (
            <Card
              bordered={false}
              style={{ borderRadius: 12, marginTop: 16, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}
              title="🗂️ Saved Templates"
              size="small"
            >
              {savedTemplates.map(t => (
                <div
                  key={t.id}
                  style={{
                    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    padding: '6px 0', borderBottom: '1px solid #f0f0f0', cursor: 'pointer',
                  }}
                  onClick={() => selectTemplate(String(t.id))}
                >
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Text strong style={{ fontSize: 12 }}>{t.name}</Text>
                    {t.description && (
                      <Text type="secondary" style={{ fontSize: 11, display: 'block' }} ellipsis>
                        {t.description}
                      </Text>
                    )}
                  </div>
                  <Space size={4}>
                    <Tag color={t.databaseType === 'SQLSERVER' ? 'blue' : 'orange'} style={{ fontSize: 9 }}>
                      {t.databaseType}
                    </Tag>
                    <Popconfirm
                      title={`Delete "${t.name}"?`}
                      onConfirm={e => { e.stopPropagation(); deleteTemplate(t.id, t.name); }}
                      okButtonProps={{ danger: true }}
                    >
                      <Button
                        type="text" danger size="small"
                        icon={<DeleteOutlined />}
                        onClick={e => e.stopPropagation()}
                      />
                    </Popconfirm>
                  </Space>
                </div>
              ))}
            </Card>
          )}
        </Col>

        {/* ── RIGHT: Preview table ── */}
        <Col xs={24} lg={16}>
          <Card
            bordered={false}
            style={{ borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.08)', minHeight: 500 }}
            title={
              preview
                ? <Space>
                    <span>Preview: {reportName}</span>
                    <Tag color="green">{preview.totalRows} rows</Tag>
                    {preview.totalRows > 50 && (
                      <Tag color="orange">Showing first 50</Tag>
                    )}
                  </Space>
                : 'Report Preview'
            }
          >
            {!preview ? (
              <div style={{ textAlign: 'center', padding: '80px 0', color: '#bbb' }}>
                <FilePdfOutlined style={{ fontSize: 56, marginBottom: 12 }} />
                <br />
                <Text type="secondary">
                  Select a template or write a query, then click <b>Preview Report</b>
                </Text>
              </div>
            ) : (
              <Table
                columns={antColumns}
                dataSource={antData}
                scroll={{ x: 'max-content', y: 440 }}
                size="small"
                bordered
                pagination={{ pageSize: 25, showSizeChanger: false, size: 'small' }}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* ── Save Template Modal ── */}
      <Modal
        title={<span><SaveOutlined /> Save Query as Template</span>}
        open={saveModal}
        onCancel={() => { setSaveModal(false); saveForm.resetFields(); }}
        footer={null}
        width={500}
      >
        <Alert
          type="info"
          showIcon
          message="This query will appear in the Report Type dropdown for future use."
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
        <Form form={saveForm} layout="vertical" onFinish={handleSaveTemplate}>
          <Form.Item
            name="name"
            label="Template Name"
            rules={[
              { required: true, message: 'Enter a unique template name' },
              { max: 200, message: 'Max 200 characters' },
            ]}
          >
            <Input placeholder="e.g. Monthly Sales Report" />
          </Form.Item>

          <Form.Item name="description" label="Description (optional)">
            <Input placeholder="Brief description of what this report shows" />
          </Form.Item>

          <Form.Item name="databaseType" label="Database Type" initialValue="MYSQL">
            <Select>
              <Option value="MYSQL">MySQL</Option>
              <Option value="SQLSERVER">SQL Server</Option>
            </Select>
          </Form.Item>

          <div style={{
            background: '#f5f5f5', borderRadius: 8, padding: '8px 12px', marginBottom: 16
          }}>
            <Text type="secondary" style={{ fontSize: 11 }}>Query to save:</Text>
            <pre style={{ fontSize: 11, margin: '4px 0 0', whiteSpace: 'pre-wrap', maxHeight: 100, overflow: 'auto' }}>
              {sql}
            </pre>
          </div>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => { setSaveModal(false); saveForm.resetFields(); }}>Cancel</Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={saveLoading}
                icon={<SaveOutlined />}
                style={{ background: '#25D366', borderColor: '#25D366' }}
              >
                Save Template
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* ── WhatsApp Send Modal ── */}
      <Modal
        title={<span><WhatsAppOutlined style={{ color: '#25D366' }} /> Send PDF via WhatsApp</span>}
        open={waModal}
        onCancel={() => { setWaModal(false); waForm.resetFields(); }}
        footer={null}
        width={480}
      >
        <Alert
          type="info"
          showIcon
          message={`A PDF of "${reportName}" will be generated and sent as a document.`}
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
        <Form form={waForm} layout="vertical" onFinish={handleSendWhatsApp}>
          <Form.Item
            name="phoneNumber"
            label="Recipient WhatsApp Number"
            rules={[
              { required: true, message: 'Enter the recipient number' },
              { pattern: /^\+?[0-9]{10,15}$/, message: 'Enter a valid phone number' },
            ]}
          >
            <Input prefix="📱" placeholder="+919876543210" size="large" />
          </Form.Item>

          <Form.Item
            name="message"
            label="Caption Message"
            initialValue="Please find the report attached."
          >
            <TextArea
              rows={3}
              placeholder="This message is sent from Smart Alert System"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setWaModal(false)}>Cancel</Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={waLoading}
                icon={<SendOutlined />}
                style={{ background: '#25D366', borderColor: '#25D366' }}
              >
                Send PDF
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}