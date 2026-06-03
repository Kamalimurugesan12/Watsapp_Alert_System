// src/pages/Dashboard.jsx
import { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, List, Tag, Typography, Spin } from 'antd';
import {
  WhatsAppOutlined, FileTextOutlined, ClockCircleOutlined,
  CheckCircleOutlined, DatabaseOutlined
} from '@ant-design/icons';
import api from '../api/axios';
import { useApp } from '../context/AppContext';
import { useNavigate } from 'react-router-dom';
const { Title, Text } = Typography;

const statColors = {
  messages: '#25D366',
  reports: '#1677ff',
  schedules: '#fa8c16',
  tables: '#722ed1',
};

export default function Dashboard() {
  const { user, dbConnected, connectedDb } = useApp();
  const [tables, setTables] = useState([]);
  const [loading, setLoading] = useState(false);
const navigate = useNavigate();
  useEffect(() => {
    if (dbConnected) {
      setLoading(true);
      api.get('/api/database/tables')
        .then(res => setTables(res.data.data || []))
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [dbConnected]);

  return (
    <div>
      <div className="page-header">
        <div>
          <Title level={4} style={{ margin: 0 }}>Welcome back, {user?.username} 👋</Title>
          <Text type="secondary">WhatsApp Database Alert & Reporting System</Text>
        </div>
      </div>

      {/* Stat Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        {[
          { title: 'DB Connected', value: dbConnected ? connectedDb : 'None', icon: <DatabaseOutlined />, color: dbConnected ? '#25D366' : '#999' },
          { title: 'Tables Available', value: tables.length, icon: <DatabaseOutlined />, color: statColors.tables },
          { title: 'Modules Active', value: 6, icon: <CheckCircleOutlined />, color: statColors.messages },
          { title: 'Scheduler Status', value: 'Active', icon: <ClockCircleOutlined />, color: statColors.schedules },
        ].map((stat, i) => (
          <Col xs={24} sm={12} lg={6} key={i}>
            <Card bordered={false} style={{ borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
                  width: 48, height: 48, borderRadius: 12,
                  background: stat.color + '15',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 20, color: stat.color
                }}>
                  {stat.icon}
                </div>
                <div>
                  <Text type="secondary" style={{ fontSize: 12 }}>{stat.title}</Text>
                  <div style={{ fontSize: 20, fontWeight: 700, color: stat.color }}>{stat.value}</div>
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Quick Access Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} md={12}>
          <Card title="📊 Quick Actions" bordered={false} style={{ borderRadius: 12 }}>
            {[
              { label: 'Execute SQL Query', path: '/query', color: '#1677ff' },
              { label: 'Send WhatsApp Message', path: '/whatsapp', color: '#25D366' },
              { label: 'Generate Report', path: '/reports', color: '#fa8c16' },
              { label: 'Manage Schedules', path: '/scheduler', color: '#722ed1' },
            ].map((item, i) => (
              <div key={i} style={{
                padding: '10px 14px', marginBottom: 8, borderRadius: 8,
                background: '#fafafa', border: '1px solid #f0f0f0',
                cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 10,
                transition: 'background 0.2s'
              }}
               onClick={() => navigate('/app' + item.path)}
                onMouseEnter={e => e.currentTarget.style.background = '#f0f9ff'}
                onMouseLeave={e => e.currentTarget.style.background = '#fafafa'}
              >
                <div style={{ width: 8, height: 8, borderRadius: '50%', background: item.color }} />
                <Text>{item.label}</Text>
              </div>
            ))}
          </Card>
        </Col>

        <Col xs={24} md={12}>
          <Card
            title={<span>🗄️ Database Tables {loading && <Spin size="small" style={{ marginLeft: 8 }} />}</span>}
            bordered={false} style={{ borderRadius: 12 }}
          >
            {!dbConnected ? (
              <Text type="secondary">Connect to a database from the login screen to see tables.</Text>
            ) : (
              <List
                size="small"
                dataSource={tables.slice(0, 8)}
                renderItem={(table) => (
                  <List.Item style={{ padding: '6px 0' }}>
                    <Tag color="geekblue" style={{ cursor: 'pointer' }}
                      onClick={() => window.location.href = '/query'}>
                      {table}
                    </Tag>
                  </List.Item>
                )}
                footer={tables.length > 8 && <Text type="secondary">+{tables.length - 8} more tables</Text>}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* System Info */}
      <Card bordered={false} style={{ borderRadius: 12 }}>
        <Row gutter={24}>
          <Col span={8}>
            <Statistic title="WhatsApp API" value="Meta Cloud API" />
          </Col>
          <Col span={8}>
            <Statistic title="Report Formats" value="Excel, CSV" />
          </Col>
          <Col span={8}>
            <Statistic title="Auth" value="JWT Secured" />
          </Col>
        </Row>
      </Card>
    </div>
  );
}