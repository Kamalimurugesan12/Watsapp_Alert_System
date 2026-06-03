// src/pages/Logs.jsx
import { useState, useEffect } from 'react';
import {
  Card, Table, Tabs, Tag, Typography, Button, Space,
  Badge, Select, DatePicker, Row, Col
} from 'antd';
import { ReloadOutlined, WhatsAppOutlined, FileTextOutlined } from '@ant-design/icons';
import toast from 'react-hot-toast';
import api from '../api/axios';

const { Title, Text } = Typography;

const STATUS_COLOR = {
  SENT:      'success',
  DELIVERED: 'success',
  FAILED:    'error',
  PENDING:   'warning',
  GENERATED: 'blue',
};

export default function Logs() {
  const [activeTab, setActiveTab] = useState('whatsapp');
  const [waLogs, setWaLogs]       = useState([]);
  const [reportLogs, setReportLogs] = useState([]);
  const [loading, setLoading]     = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  useEffect(() => {
    if (activeTab === 'whatsapp') fetchWaLogs(1);
    else fetchReportLogs(1);
  }, [activeTab]);

  const fetchWaLogs = async (page = 1) => {
    setLoading(true);
    try {
      const res = await api.get('/api/logs/whatsapp', {
        params: { page: page - 1, size: 20 }
      });
      const data = res.data.data;
      setWaLogs(data.content || []);
      setPagination(p => ({ ...p, current: page, total: data.totalElements || 0 }));
    } catch {
      // API may not exist yet — show empty state gracefully
      setWaLogs([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchReportLogs = async (page = 1) => {
    setLoading(true);
    try {
      const res = await api.get('/api/logs/reports', {
        params: { page: page - 1, size: 20 }
      });
      const data = res.data.data;
      setReportLogs(data.content || []);
      setPagination(p => ({ ...p, current: page, total: data.totalElements || 0 }));
    } catch {
      setReportLogs([]);
    } finally {
      setLoading(false);
    }
  };

  const waColumns = [
    {
      title: 'Recipient', dataIndex: 'recipient', key: 'recipient', width: 150,
      render: v => <Text code>{v}</Text>
    },
    {
      title: 'Message', dataIndex: 'messageBody', key: 'messageBody', ellipsis: true,
      render: v => <Text style={{ fontSize: 12 }}>{v}</Text>
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 110,
      render: v => <Badge status={STATUS_COLOR[v] || 'default'} text={v} />
    },
    {
      title: 'Message ID', dataIndex: 'waMessageId', key: 'waMessageId', width: 160,
      render: v => v ? <Text code style={{ fontSize: 11 }}>{v}</Text> : '-'
    },
    {
      title: 'Error', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true, width: 200,
      render: v => v ? <Text type="danger" style={{ fontSize: 12 }}>{v}</Text> : '-'
    },
    {
      title: 'Sent At', dataIndex: 'sentAt', key: 'sentAt', width: 160,
      render: v => v ? new Date(v).toLocaleString() : '-'
    },
  ];

  const reportColumns = [
    { title: 'Report Name', dataIndex: 'reportName', key: 'reportName', width: 200 },
    {
      title: 'Type', dataIndex: 'reportType', key: 'reportType', width: 120,
      render: v => <Tag color="blue">{v}</Tag>
    },
    {
      title: 'Format', dataIndex: 'format', key: 'format', width: 90,
      render: v => <Tag color={v === 'EXCEL' ? 'green' : 'orange'}>{v}</Tag>
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 110,
      render: v => <Badge status={STATUS_COLOR[v] || 'default'} text={v} />
    },
    {
      title: 'WhatsApp', dataIndex: 'sentViaWhatsapp', key: 'sentViaWhatsapp', width: 100,
      render: v => v
        ? <Tag color="green">Sent</Tag>
        : <Tag color="default">No</Tag>
    },
    {
      title: 'Generated At', dataIndex: 'generatedAt', key: 'generatedAt', width: 160,
      render: v => v ? new Date(v).toLocaleString() : '-'
    },
  ];

  const tabItems = [
    {
      key: 'whatsapp',
      label: <span><WhatsAppOutlined /> WhatsApp Logs</span>,
      children: (
        <Table
          columns={waColumns}
          dataSource={waLogs.map((r, i) => ({ ...r, key: r.id || i }))}
          loading={loading}
          size="small"
          scroll={{ x: 900 }}
          pagination={{
            ...pagination,
            showSizeChanger: false,
            showTotal: total => `Total ${total} entries`,
            onChange: fetchWaLogs,
          }}
          locale={{ emptyText: 'No WhatsApp logs found. Logs appear after you send messages.' }}
        />
      )
    },
    {
      key: 'reports',
      label: <span><FileTextOutlined /> Report Logs</span>,
      children: (
        <Table
          columns={reportColumns}
          dataSource={reportLogs.map((r, i) => ({ ...r, key: r.id || i }))}
          loading={loading}
          size="small"
          scroll={{ x: 900 }}
          pagination={{
            ...pagination,
            showSizeChanger: false,
            showTotal: total => `Total ${total} entries`,
            onChange: fetchReportLogs,
          }}
          locale={{ emptyText: 'No report logs found. Logs appear after reports are generated.' }}
        />
      )
    }
  ];

  return (
    <div>
      <div className="page-header">
        <Title level={4} style={{ margin: 0 }}>📋 Activity Logs</Title>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => activeTab === 'whatsapp' ? fetchWaLogs(1) : fetchReportLogs(1)}
          loading={loading}
        >
          Refresh
        </Button>
      </div>

      <Card style={{ borderRadius: 12 }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={tabItems}
        />
      </Card>
    </div>
  );
}
