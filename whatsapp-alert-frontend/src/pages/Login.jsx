// // src/pages/Login.jsx
// // ─────────────────────────────────────────────────────────────
// // Connection Screen — replaces the old Login/Auth page.
// // Equivalent to the Python script's get_sql_connection() dialog.
// // No username/password for the APP — only for the TARGET DATABASE.
// // ─────────────────────────────────────────────────────────────
// import { useState } from 'react';
// import { useNavigate } from 'react-router-dom';
// import {
//   Form, Input, Button, Card, Typography, Alert, Space, Divider, Steps
// } from 'antd';
// import {
//   DatabaseOutlined, WhatsAppOutlined, CheckCircleOutlined,
//   LoadingOutlined, LinkOutlined
// } from '@ant-design/icons';
// import toast from 'react-hot-toast';
// import api from '../api/axios';
// import { useApp } from '../context/AppContext';

// const { Title, Text, Paragraph } = Typography;

// export default function ConnectionScreen() {
//   const [loading, setLoading]   = useState(false);
//   const [testOk, setTestOk]     = useState(false);
//   const navigate                = useNavigate();
//   const { connectDb }           = useApp();

//   const handleConnect = async (values) => {
//     setLoading(true);
//     setTestOk(false);
//     try {
//       const res = await api.post('/api/connector/connect', {
//         server:   values.server,
//         port:     values.port || '3306',
//         database: values.database,
//         username: values.username,
//         password: values.password,
//       });

//       if (res.data.success) {
//         setTestOk(true);
//         connectDb(values.database);
//         toast.success(`Connected to '${values.database}' successfully!`);
//         setTimeout(() => navigate('/app/dashboard'), 800);
//       } else {
//         toast.error(res.data.message);
//       }
//     } catch (err) {
//       const msg = err.response?.data?.message || 'Connection failed. Check credentials and host.';
//       toast.error(msg);
//     } finally {
//       setLoading(false);
//     }
//   };

//   return (
//     <div style={{
//       minHeight: '100vh',
//       display: 'flex',
//       alignItems: 'center',
//       justifyContent: 'center',
//       background: 'linear-gradient(135deg, #001529 0%, #003a1a 100%)',
//       padding: 24,
//     }}>
//       <Card style={{
//         width: '100%', maxWidth: 460,
//         borderRadius: 16,
//         boxShadow: '0 20px 60px rgba(0,0,0,0.4)',
//       }}>

//         {/* Header */}
//         <div style={{ textAlign: 'center', marginBottom: 28 }}>
//           <WhatsAppOutlined style={{ fontSize: 52, color: '#25D366' }} />
//           <Title level={3} style={{ margin: '10px 0 2px' }}>WA Alert System</Title>
//           <Text type="secondary">Connect to your database to get started</Text>
//         </div>

//         <Alert
//           message="No login required"
//           description="Enter your MySQL database credentials below. The app will connect directly — just like the Python pyodbc connector."
//           type="info"
//           showIcon
//           style={{ marginBottom: 24, borderRadius: 8 }}
//         />

//         <Form
//           layout="vertical"
//           onFinish={handleConnect}
//           initialValues={{ port: '3306', server: 'localhost' }}
//           size="large"
//         >
//           {/* Server + Port on the same row */}
//           <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px', gap: 12 }}>
//             <Form.Item
//               name="server"
//               label="Server / Host"
//               rules={[{ required: true, message: 'Enter server address' }]}
//               style={{ marginBottom: 12 }}
//             >
//               <Input
//                 prefix={<LinkOutlined />}
//                 placeholder="localhost or 192.168.1.10"
//               />
//             </Form.Item>
//             <Form.Item
//               name="port"
//               label="Port"
//               style={{ marginBottom: 12 }}
//             >
//               <Input placeholder="3306" />
//             </Form.Item>
//           </div>

//           <Form.Item
//             name="database"
//             label="Database Name"
//             rules={[{ required: true, message: 'Enter database name' }]}
//             style={{ marginBottom: 12 }}
//           >
//             <Input prefix={<DatabaseOutlined />} placeholder="my_database" />
//           </Form.Item>

//           <Form.Item
//             name="username"
//             label="Username"
//             rules={[{ required: true, message: 'Enter username' }]}
//             style={{ marginBottom: 12 }}
//           >
//             <Input placeholder="root" />
//           </Form.Item>

//           <Form.Item
//             name="password"
//             label="Password"
//             rules={[{ required: true, message: 'Enter password' }]}
//             style={{ marginBottom: 20 }}
//           >
//             <Input.Password placeholder="••••••••" />
//           </Form.Item>

//           <Form.Item style={{ marginBottom: 0 }}>
//             <Button
//               type="primary"
//               htmlType="submit"
//               block
//               loading={loading}
//               icon={testOk ? <CheckCircleOutlined /> : <DatabaseOutlined />}
//               style={{
//                 height: 46,
//                 background: testOk ? '#52c41a' : '#25D366',
//                 borderColor: testOk ? '#52c41a' : '#25D366',
//                 fontSize: 15,
//                 fontWeight: 600,
//               }}
//             >
//               {loading ? 'Connecting...' : testOk ? 'Connected!' : 'Connect to Database'}
//             </Button>
//           </Form.Item>
//         </Form>

//         <Divider style={{ margin: '20px 0 12px' }} />

//         {/* How it works hint */}
//         <div style={{ background: '#f9f9f9', borderRadius: 8, padding: '10px 14px' }}>
//           <Text style={{ fontSize: 12, color: '#888' }}>
//             Equivalent to Python's:{' '}
//             <code style={{ fontSize: 11, background: '#eee', padding: '2px 6px', borderRadius: 4 }}>
//               pyodbc.connect("SERVER=...;DATABASE=...;UID=...;PWD=...")
//             </code>
//           </Text>
//         </div>
//       </Card>
//     </div>
//   );
// }
// src/pages/Login.jsx  (Connection Screen)
// Upgraded: DB type selector, auto-port switching, connection history panel
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Form, Input, Button, Card, Typography, Alert, Divider,
  Select, List, Tag, Tooltip, Spin, Modal, Badge
} from 'antd';
import {
  DatabaseOutlined, WhatsAppOutlined, CheckCircleOutlined,
  LinkOutlined, HistoryOutlined, ThunderboltOutlined, ClockCircleOutlined
} from '@ant-design/icons';
import toast from 'react-hot-toast';
import api from '../api/axios';
import { useApp } from '../context/AppContext';

const { Title, Text } = Typography;
const { Option } = Select;

const DB_TYPES = {
  MYSQL:     { label: 'MySQL',               defaultPort: '3306', color: '#f97316' },
  SQLSERVER: { label: 'Microsoft SQL Server', defaultPort: '1433', color: '#0078d4' },
};

export default function ConnectionScreen() {
  const [form]            = Form.useForm();
  const [loading, setLoading]     = useState(false);
  const [testOk, setTestOk]       = useState(false);
  const [dbType, setDbType]       = useState('MYSQL');
  const [history, setHistory]     = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [reconnectId, setReconnectId]   = useState(null);
  const [reconnectPwd, setReconnectPwd] = useState('');
  const [reconnectModal, setReconnectModal] = useState(false);
  const navigate  = useNavigate();
  const { connectDb } = useApp();

  // Load connection history on mount
  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    setHistoryLoading(true);
    try {
      const res = await api.get('/api/connector/history');
      setHistory(res.data.data || []);
    } catch {
      // silent — history is optional
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleDbTypeChange = (type) => {
    setDbType(type);
    form.setFieldValue('port', DB_TYPES[type].defaultPort);
  };

  const handleConnect = async (values) => {
    setLoading(true);
    setTestOk(false);
    try {
      const res = await api.post('/api/connector/connect', {
        databaseType: values.databaseType || 'MYSQL',
        server:       values.server,
        port:         values.port || DB_TYPES[dbType].defaultPort,
        database:     values.database,
        username:     values.username,
        password:     values.password,
      });

      if (res.data.success) {
        setTestOk(true);
        connectDb(values.database);
        toast.success(`Connected to '${values.database}' successfully!`);
        setTimeout(() => navigate('/app/dashboard'), 800);
      } else {
        toast.error(res.data.message);
      }
    } catch (err) {
      const msg = err.response?.data?.message || 'Connection failed. Check credentials and host.';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  // One-click reconnect from history
  const openReconnect = (item) => {
    setReconnectId(item.id);
    setReconnectPwd('');
    setReconnectModal(true);
  };

  const handleReconnect = async () => {
    if (!reconnectPwd.trim()) { toast.error('Enter your password to reconnect'); return; }
    setLoading(true);
    try {
      const res = await api.post('/api/connector/reconnect', {
        id:       reconnectId,
        password: reconnectPwd,
      });
      if (res.data.success) {
        connectDb(res.data.data.database);
        toast.success('Reconnected successfully!');
        setReconnectModal(false);
        setTimeout(() => navigate('/app/dashboard'), 600);
      } else {
        toast.error(res.data.message);
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Reconnect failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #001529 0%, #003a1a 100%)',
      padding: 24,
      gap: 24,
      flexWrap: 'wrap',
    }}>

      {/* ── Main Connection Card ── */}
      <Card style={{ width: '100%', maxWidth: 480, borderRadius: 16, boxShadow: '0 20px 60px rgba(0,0,0,0.4)' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <WhatsAppOutlined style={{ fontSize: 52, color: '#25D366' }} />
          <Title level={3} style={{ margin: '10px 0 2px' }}>WA Alert System</Title>
          <Text type="secondary">Connect to your database to get started</Text>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleConnect}
          initialValues={{ databaseType: 'MYSQL', port: '3306', server: 'localhost' }}
          size="large"
        >
          {/* ✅ Database Type Dropdown */}
          <Form.Item name="databaseType" label="Database Type" style={{ marginBottom: 12 }}>
            <Select onChange={handleDbTypeChange}>
              {Object.entries(DB_TYPES).map(([key, val]) => (
                <Option key={key} value={key}>
                  <span style={{ color: val.color, fontWeight: 600 }}>● </span>{val.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          {/* Server + Port */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px', gap: 12 }}>
            <Form.Item
              name="server" label="Server / Host"
              rules={[{ required: true, message: 'Enter server address' }]}
              style={{ marginBottom: 12 }}
            >
              <Input prefix={<LinkOutlined />} placeholder="localhost or 192.168.1.10" />
            </Form.Item>
            <Form.Item name="port" label="Port" style={{ marginBottom: 12 }}>
              <Input placeholder={DB_TYPES[dbType].defaultPort} />
            </Form.Item>
          </div>

          <Form.Item
            name="database" label="Database Name"
            rules={[{ required: true, message: 'Enter database name' }]}
            style={{ marginBottom: 12 }}
          >
            <Input prefix={<DatabaseOutlined />} placeholder="my_database" />
          </Form.Item>

          <Form.Item
            name="username" label="Username"
            rules={[{ required: true, message: 'Enter username' }]}
            style={{ marginBottom: 12 }}
          >
            <Input placeholder={dbType === 'SQLSERVER' ? 'sa' : 'root'} />
          </Form.Item>

          <Form.Item
            name="password" label="Password"
            rules={[{ required: true, message: 'Enter password' }]}
            style={{ marginBottom: 20 }}
          >
            <Input.Password placeholder="••••••••" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={loading}
              icon={testOk ? <CheckCircleOutlined /> : <DatabaseOutlined />}
              style={{
                height: 46,
                background: testOk ? '#52c41a' : '#25D366',
                borderColor: testOk ? '#52c41a' : '#25D366',
                fontSize: 15,
                fontWeight: 600,
              }}
            >
              {loading ? 'Connecting...' : testOk ? 'Connected!' : 'Connect to Database'}
            </Button>
          </Form.Item>
        </Form>

        {/* JDBC URL preview */}
        <Divider style={{ margin: '16px 0 10px' }} />
        <div style={{ background: '#f9f9f9', borderRadius: 8, padding: '8px 12px' }}>
          <Text style={{ fontSize: 11, color: '#888' }}>
            {dbType === 'SQLSERVER'
              ? 'jdbc:sqlserver://host:1433;databaseName=db;encrypt=true'
              : 'jdbc:mysql://host:3306/db?useSSL=false'}
          </Text>
        </div>
      </Card>

      {/* ── Connection History Card ── */}
      <Card
        style={{ width: '100%', maxWidth: 340, borderRadius: 16, boxShadow: '0 20px 60px rgba(0,0,0,0.4)', alignSelf: 'flex-start' }}
        title={
          <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <HistoryOutlined style={{ color: '#25D366' }} />
            Recent Connections
          </span>
        }
        extra={
          historyLoading ? <Spin size="small" /> :
          <Button type="text" size="small" onClick={loadHistory}>↻</Button>
        }
      >
        {history.length === 0 ? (
          <Text type="secondary" style={{ fontSize: 13 }}>
            No connection history yet. Connect to a database to see it here.
          </Text>
        ) : (
          <List
            size="small"
            dataSource={history}
            renderItem={(item) => (
              <List.Item
                style={{ padding: '8px 0', cursor: 'pointer' }}
                extra={
                  <Tooltip title="Reconnect">
                    <Button
                      type="primary"
                      size="small"
                      icon={<ThunderboltOutlined />}
                      onClick={() => openReconnect(item)}
                      style={{ background: '#25D366', borderColor: '#25D366' }}
                    />
                  </Tooltip>
                }
              >
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                    <Tag
                      color={item.databaseType === 'SQLSERVER' ? 'blue' : 'orange'}
                      style={{ fontSize: 10, marginRight: 0 }}
                    >
                      {item.databaseType}
                    </Tag>
                    <Text strong style={{ fontSize: 13 }}>{item.databaseName}</Text>
                  </div>
                  <Text type="secondary" style={{ fontSize: 11 }}>
                    {item.username}@{item.host}:{item.port}
                  </Text>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 2 }}>
                    <ClockCircleOutlined style={{ fontSize: 10, color: '#999' }} />
                    <Text style={{ fontSize: 10, color: '#999' }}>
                      {new Date(item.connectedAt).toLocaleString()}
                    </Text>
                  </div>
                </div>
              </List.Item>
            )}
          />
        )}
      </Card>

      {/* ── Reconnect Modal ── */}
      <Modal
        title={<span><ThunderboltOutlined style={{ color: '#25D366' }} /> Quick Reconnect</span>}
        open={reconnectModal}
        onOk={handleReconnect}
        onCancel={() => setReconnectModal(false)}
        okText="Reconnect"
        confirmLoading={loading}
        okButtonProps={{ style: { background: '#25D366', borderColor: '#25D366' } }}
      >
        <Alert
          message="Enter your password to reconnect to the previously used database."
          type="info" showIcon style={{ marginBottom: 16 }}
        />
        <Input.Password
          size="large"
          placeholder="Database password"
          value={reconnectPwd}
          onChange={e => setReconnectPwd(e.target.value)}
          onPressEnter={handleReconnect}
          prefix={<DatabaseOutlined />}
        />
      </Modal>
    </div>
  );
}