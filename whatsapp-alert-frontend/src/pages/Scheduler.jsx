// src/pages/Scheduler.jsx
import { useState, useEffect } from 'react';
import {
  Card, Form, Input, Select, Button, Table, Switch, Typography,
  Space, Tag, Popconfirm, Row, Col, Divider
} from 'antd';
import { PlusOutlined, DeleteOutlined, ClockCircleOutlined } from '@ant-design/icons';
import toast from 'react-hot-toast';
import api from '../api/axios';

const { Title, Text } = Typography;
const { TextArea } = Input;

export default function Scheduler() {
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [recipients, setRecipients] = useState([]);
  const [form] = Form.useForm();

  useEffect(() => {
    loadSchedules();
  }, []);

  const loadSchedules = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/scheduler');
      setSchedules(res.data.data || []);
    } catch {
      toast.error('Failed to load schedules');
    } finally {
      setLoading(false);
    }
  };

  const createSchedule = async (values) => {
    setCreating(true);
    try {
      await api.post('/api/scheduler', { ...values, recipients, enabled: true });
      toast.success('Schedule created!');
      form.resetFields();
      setRecipients([]);
      loadSchedules();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Create failed');
    } finally {
      setCreating(false);
    }
  };

  const toggleSchedule = async (id, enabled) => {
    try {
      await api.patch(`/api/scheduler/${id}/toggle`, { enabled });
      toast.success(enabled ? 'Schedule enabled' : 'Schedule disabled');
      loadSchedules();
    } catch {
      toast.error('Toggle failed');
    }
  };

  const deleteSchedule = async (id) => {
    try {
      await api.delete(`/api/scheduler/${id}`);
      toast.success('Schedule deleted');
      loadSchedules();
    } catch {
      toast.error('Delete failed');
    }
  };

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name', width: 180 },
    {
      title: 'Type', dataIndex: 'scheduleType', key: 'scheduleType', width: 100,
      render: t => <Tag color={t === 'DAILY' ? 'blue' : t === 'WEEKLY' ? 'green' : 'orange'}>{t}</Tag>
    },
    { title: 'Report Type', dataIndex: 'reportType', key: 'reportType', width: 130 },
    {
      title: 'Recipients', dataIndex: 'recipients', key: 'recipients',
      render: r => r ? r.split(',').map(p => <Tag key={p}>{p}</Tag>) : '-'
    },
    {
      title: 'Last Run', dataIndex: 'lastRunAt', key: 'lastRunAt', width: 150,
      render: v => v ? new Date(v).toLocaleString() : 'Never'
    },
    {
      title: 'Enabled', key: 'enabled', width: 90,
      render: (_, r) => (
        <Switch
          checked={r.isEnabled}
          onChange={checked => toggleSchedule(r.id, checked)}
          size="small"
        />
      )
    },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, r) => (
        <Popconfirm title="Delete this schedule?" onConfirm={() => deleteSchedule(r.id)} okText="Yes" cancelText="No">
          <Button type="text" danger icon={<DeleteOutlined />} size="small" />
        </Popconfirm>
      )
    }
  ];

  return (
    <div>
      <div className="page-header">
        <Title level={4} style={{ margin: 0 }}>
          <ClockCircleOutlined style={{ marginRight: 8 }} />Scheduler
        </Title>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card style={{ borderRadius: 12 }} title="➕ New Schedule">
            <Form form={form} layout="vertical" onFinish={createSchedule}>
              <Form.Item name="name" label="Schedule Name" rules={[{ required: true }]}>
                <Input placeholder="Daily Salary Report" />
              </Form.Item>

              <Form.Item name="scheduleType" label="Frequency" rules={[{ required: true }]}>
                <Select options={[
                  { label: '📅 Daily', value: 'DAILY' },
                  { label: '📆 Weekly', value: 'WEEKLY' },
                  { label: '⚙️ Custom (Cron)', value: 'CUSTOM' },
                ]} />
              </Form.Item>

              <Form.Item noStyle shouldUpdate={(p, c) => p.scheduleType !== c.scheduleType}>
                {({ getFieldValue }) => getFieldValue('scheduleType') === 'CUSTOM' && (
                  <Form.Item name="cronExpression" label="Cron Expression"
                    rules={[{ required: true }]}>
                    <Input placeholder="0 0 8 * * ? (8 AM daily)" />
                  </Form.Item>
                )}
              </Form.Item>

              <Form.Item name="reportType" label="Report Type">
                <Select options={[
                  { label: 'Basic Salary', value: 'SALARY' },
                  { label: 'HRA', value: 'HRA' },
                  { label: 'Employee Summary', value: 'SUMMARY' },
                  { label: 'Custom SQL', value: 'CUSTOM' },
                ]} />
              </Form.Item>

              <Form.Item name="sqlQuery" label="SQL Query">
                <TextArea rows={3} style={{ fontFamily: 'monospace', fontSize: 12 }}
                  placeholder="SELECT * FROM employee_salary" />
              </Form.Item>

              <div style={{ marginBottom: 12 }}>
                <Text strong style={{ fontSize: 13 }}>Recipients</Text>
                <Space.Compact style={{ width: '100%', marginTop: 4 }}>
                  <Input
                    id="schedRecipient"
                    placeholder="+91XXXXXXXXXX"
                    onPressEnter={e => {
                      const val = e.target.value.trim();
                      if (val && !recipients.includes(val)) {
                        setRecipients([...recipients, val]);
                        e.target.value = '';
                      }
                    }}
                  />
                  <Button onClick={() => {
                    const el = document.getElementById('schedRecipient');
                    const val = el.value.trim();
                    if (val && !recipients.includes(val)) {
                      setRecipients([...recipients, val]);
                      el.value = '';
                    }
                  }} icon={<PlusOutlined />} />
                </Space.Compact>
                <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                  {recipients.map(r => (
                    <Tag key={r} closable onClose={() => setRecipients(recipients.filter(x => x !== r))} color="green">{r}</Tag>
                  ))}
                </div>
              </div>

              <Form.Item name="messageTemplate" label="Message Template">
                <TextArea rows={3} placeholder="Scheduled report attached. Date: {date}" />
              </Form.Item>

              <Button type="primary" htmlType="submit" block loading={creating}
                style={{ background: '#25D366', borderColor: '#25D366' }}>
                Create Schedule
              </Button>
            </Form>
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card style={{ borderRadius: 12 }} title={`📋 Schedules (${schedules.length})`}
            extra={<Button size="small" onClick={loadSchedules}>Refresh</Button>}>
            <Table
              columns={columns}
              dataSource={schedules.map(s => ({ ...s, key: s.id }))}
              loading={loading}
              size="small"
              scroll={{ x: 800 }}
              pagination={{ pageSize: 10 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}