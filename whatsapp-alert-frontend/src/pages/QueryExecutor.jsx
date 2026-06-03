// src/pages/QueryExecutor.jsx
import { useState, useEffect } from 'react';
import {
  Row, Col, Card, Input, Button, Table, Typography, Tree, Tag,
  Space, Tooltip, Select, Pagination, Spin, Divider, Alert
} from 'antd';
import {
  PlayCircleOutlined, DownloadOutlined, DatabaseOutlined,
  TableOutlined, ReloadOutlined
} from '@ant-design/icons';
import toast from 'react-hot-toast';
import api from '../api/axios';

const { TextArea } = Input;
const { Text, Title } = Typography;

const SAMPLE_QUERIES = [
  { label: 'Show all tables', value: 'SHOW TABLES' },
  { label: 'Table structure', value: 'DESCRIBE your_table' },
  { label: 'Basic SELECT', value: 'SELECT * FROM your_table LIMIT 10' },
  { label: 'Count rows', value: 'SELECT COUNT(*) FROM your_table' },
];

export default function QueryExecutor() {
  const [sql, setSql] = useState('SELECT * FROM your_table LIMIT 10');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [tables, setTables] = useState([]);
  const [tablesLoading, setTablesLoading] = useState(false);
  const [selectedTable, setSelectedTable] = useState(null);
  const [tableColumns, setTableColumns] = useState([]);
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    loadTables();
  }, []);

  const loadTables = async () => {
    setTablesLoading(true);
    try {
      const res = await api.get('/api/database/tables');
      setTables(res.data.data || []);
    } catch {
      toast.error('Failed to load tables. Is the database connected?');
    } finally {
      setTablesLoading(false);
    }
  };

  const executeQuery = async (pageNum = 0) => {
    if (!sql.trim()) return toast.error('Enter a SQL query');
    setLoading(true);
    setPage(pageNum);
    try {
      const res = await api.post('/api/database/query', { sql, page: pageNum, pageSize: 50 });
      setResult(res.data.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Query failed');
    } finally {
      setLoading(false);
    }
  };

  const loadTableColumns = async (tableName) => {
    setSelectedTable(tableName);
    setSql(`SELECT * FROM \`${tableName}\` LIMIT 100`);
    try {
      const res = await api.get(`/api/database/tables/${tableName}/columns`);
      setTableColumns(res.data.data || []);
    } catch {
      toast.error('Failed to load columns');
    }
  };

  const exportCsv = async () => {
    setExporting(true);
    try {
      const res = await api.post('/api/reports/csv', { sql, reportName: 'QueryResult' },
        { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'query_result.csv';
      a.click();
      toast.success('CSV exported');
    } catch {
      toast.error('Export failed');
    } finally {
      setExporting(false);
    }
  };

  // Build Ant Design table columns from result
  const antColumns = result?.columns?.map((col) => ({
    title: col,
    dataIndex: col,
    key: col,
    ellipsis: true,
    width: 150,
    render: (val) => val == null ? <Text type="secondary">NULL</Text> : String(val),
  })) || [];

  const antData = result?.rows?.map((row, i) => {
    const obj = { key: i };
    result.columns.forEach((col, j) => { obj[col] = row[j]; });
    return obj;
  }) || [];

  return (
    <div>
      <div className="page-header">
        <Title level={4} style={{ margin: 0 }}>🔍 Query Executor</Title>
      </div>

      <Row gutter={[16, 16]}>
        {/* Left: table explorer */}
        <Col xs={24} md={6}>
          <Card
            title={
              <Space>
                <DatabaseOutlined />
                <span>Tables</span>
                {tablesLoading && <Spin size="small" />}
              </Space>
            }
            extra={<ReloadOutlined onClick={loadTables} style={{ cursor: 'pointer' }} />}
            size="small"
            style={{ height: 'calc(100vh - 180px)', overflowY: 'auto', borderRadius: 12 }}
          >
            {tables.map(t => (
              <div key={t}
                style={{
                  padding: '8px 10px', cursor: 'pointer', borderRadius: 6,
                  background: selectedTable === t ? '#e6f7ff' : 'transparent',
                  marginBottom: 2, display: 'flex', alignItems: 'center', gap: 8
                }}
                onClick={() => loadTableColumns(t)}
              >
                <TableOutlined style={{ color: '#1677ff', fontSize: 12 }} />
                <Text style={{ fontSize: 13 }}>{t}</Text>
              </div>
            ))}

            {selectedTable && tableColumns.length > 0 && (
              <>
                <Divider style={{ margin: '8px 0' }} />
                <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 6 }}>
                  COLUMNS ({selectedTable})
                </Text>
                {tableColumns.map(col => (
                  <div key={col.name} style={{ padding: '3px 10px', fontSize: 12 }}>
                    <Text>{col.name}</Text>
                    <Tag style={{ marginLeft: 6, fontSize: 10 }} color="default">{col.type}</Tag>
                  </div>
                ))}
              </>
            )}
          </Card>
        </Col>

        {/* Right: query editor + results */}
        <Col xs={24} md={18}>
          <Card style={{ borderRadius: 12, marginBottom: 16 }}>
            <Space style={{ marginBottom: 8 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>Quick:</Text>
              {SAMPLE_QUERIES.map(q => (
                <Tag key={q.label} color="blue" style={{ cursor: 'pointer' }}
                  onClick={() => setSql(q.value)}>{q.label}</Tag>
              ))}
            </Space>

            <TextArea
              value={sql}
              onChange={e => setSql(e.target.value)}
              rows={5}
              className="sql-editor"
              placeholder="Enter SQL query... (SELECT queries only)"
              style={{ fontFamily: 'monospace', fontSize: 13, marginBottom: 12 }}
              onKeyDown={e => { if (e.ctrlKey && e.key === 'Enter') executeQuery(); }}
            />

            <Space>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                loading={loading}
                onClick={() => executeQuery()}
                style={{ background: '#25D366', borderColor: '#25D366' }}
              >
                Execute (Ctrl+Enter)
              </Button>
              {result && (
                <Button icon={<DownloadOutlined />} onClick={exportCsv} loading={exporting}>
                  Export CSV
                </Button>
              )}
            </Space>
          </Card>

          {/* Results */}
          {result && (
            <Card
              title={
                <Space>
                  <span>Results</span>
                  <Tag color="green">{result.totalRows} rows</Tag>
                  <Tag>{result.columns?.length} columns</Tag>
                </Space>
              }
              style={{ borderRadius: 12 }}
            >
              <Table
                columns={antColumns}
                dataSource={antData}
                scroll={{ x: 'max-content', y: 400 }}
                pagination={false}
                size="small"
                bordered
              />
              {result.totalPages > 1 && (
                <div style={{ textAlign: 'right', marginTop: 12 }}>
                  <Pagination
                    current={page + 1}
                    total={result.totalRows}
                    pageSize={50}
                    onChange={(p) => executeQuery(p - 1)}
                    showSizeChanger={false}
                    showTotal={(total) => `Total ${total} rows`}
                  />
                </div>
              )}
            </Card>
          )}
        </Col>
      </Row>
    </div>
  );
}