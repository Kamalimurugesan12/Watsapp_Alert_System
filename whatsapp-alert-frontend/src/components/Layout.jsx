// // src/components/Layout.jsx
// import { useState } from 'react';
// import { Outlet, useNavigate, useLocation } from 'react-router-dom';
// import {
//   Layout as AntLayout, Menu, Button, Badge, Switch, Tooltip, Popconfirm, Tag
// } from 'antd';
// import {
//   DashboardOutlined, CodeOutlined, WhatsAppOutlined,
//   FileTextOutlined, ClockCircleOutlined, UnorderedListOutlined,
//   MenuFoldOutlined, MenuUnfoldOutlined, BulbOutlined,
//   DisconnectOutlined, DatabaseOutlined
// } from '@ant-design/icons';
// import toast from 'react-hot-toast';
// import { useApp } from '../context/AppContext';
// import api from '../api/axios';

// const { Header, Sider, Content } = AntLayout;

// const menuItems = [
//   { key: '/app/dashboard', icon: <DashboardOutlined />,    label: 'Dashboard' },
//   { key: '/app/query',     icon: <CodeOutlined />,          label: 'Query Executor' },
//   { key: '/app/whatsapp',  icon: <WhatsAppOutlined />,      label: 'WhatsApp' },
//   { key: '/app/reports',   icon: <FileTextOutlined />,      label: 'Reports' },
//   { key: '/app/scheduler', icon: <ClockCircleOutlined />,   label: 'Scheduler' },
//   { key: '/app/logs',      icon: <UnorderedListOutlined />, label: 'Logs' },
// ];

// export default function Layout() {
//   const [collapsed, setCollapsed] = useState(false);
//   const { theme, toggleTheme, connectedDb, disconnectDb } = useApp();
//   const navigate  = useNavigate();
//   const location  = useLocation();

//   const handleDisconnect = async () => {
//     try {
//       await api.post('/api/connector/disconnect');
//     } catch { /* ignore */ }
//     disconnectDb();
//     toast.success('Disconnected from database');
//     navigate('/');
//   };

//   return (
//     <AntLayout style={{ minHeight: '100vh' }}>
//       <Sider
//         collapsible collapsed={collapsed} trigger={null} width={220}
//         style={{ background: '#001529' }}
//       >
//         {/* Logo / brand */}
//         <div style={{
//           padding: collapsed ? '16px 8px' : '16px 20px',
//           display: 'flex', alignItems: 'center', gap: 10,
//           borderBottom: '1px solid rgba(255,255,255,0.08)'
//         }}>
//           <WhatsAppOutlined style={{ fontSize: 24, color: '#25D366', flexShrink: 0 }} />
//           {!collapsed && (
//             <span style={{ color: '#fff', fontWeight: 700, fontSize: 14, lineHeight: 1.3 }}>
//               WA Alert<br />
//               <span style={{ fontWeight: 400, fontSize: 11, color: 'rgba(255,255,255,0.5)' }}>
//                 Reporting System
//               </span>
//             </span>
//           )}
//         </div>

//         {/* DB status badge */}
//         {!collapsed && (
//           <div style={{ padding: '8px 16px' }}>
//             <Badge status="success" text={
//               <span style={{ color: 'rgba(255,255,255,0.7)', fontSize: 11 }}>
//                 <DatabaseOutlined style={{ marginRight: 4 }} />
//                 {connectedDb || sessionStorage.getItem('connectedDb') || 'Connected'}
//               </span>
//             } />
//           </div>
//         )}

//         <Menu
//           theme="dark" mode="inline"
//           selectedKeys={[location.pathname]}
//           items={menuItems}
//           onClick={({ key }) => navigate(key)}
//           style={{ border: 'none', marginTop: 4 }}
//         />
//       </Sider>

//       <AntLayout>
//         <Header style={{
//           background: theme === 'dark' ? '#141414' : '#fff',
//           padding: '0 20px',
//           display: 'flex', alignItems: 'center', justifyContent: 'space-between',
//           borderBottom: '1px solid #f0f0f0',
//           position: 'sticky', top: 0, zIndex: 100,
//         }}>
//           <Button
//             type="text"
//             icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
//             onClick={() => setCollapsed(!collapsed)}
//           />

//           <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
//             <Tag color="green" icon={<DatabaseOutlined />}>
//               {connectedDb || sessionStorage.getItem('connectedDb')}
//             </Tag>

//             <Tooltip title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
//               <Switch
//                 checkedChildren={<BulbOutlined />}
//                 unCheckedChildren={<BulbOutlined />}
//                 checked={theme === 'dark'}
//                 onChange={toggleTheme}
//                 size="small"
//               />
//             </Tooltip>

//             <Popconfirm
//               title="Disconnect from database?"
//               description="You will be returned to the Connection Screen."
//               onConfirm={handleDisconnect}
//               okText="Disconnect"
//               okButtonProps={{ danger: true }}
//               cancelText="Cancel"
//             >
//               <Button danger icon={<DisconnectOutlined />} size="small">
//                 Disconnect
//               </Button>
//             </Popconfirm>
//           </div>
//         </Header>

//         <Content style={{ margin: 24, minHeight: 'calc(100vh - 112px)' }}>
//           <Outlet />
//         </Content>
//       </AntLayout>
//     </AntLayout>
//   );
// }
// src/components/Layout.jsx
import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Layout as AntLayout, Menu, Button, Badge, Switch, Tooltip, Popconfirm, Tag
} from 'antd';
import {
  DashboardOutlined, CodeOutlined, WhatsAppOutlined,
  FileTextOutlined, ClockCircleOutlined, UnorderedListOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, BulbOutlined,
  DisconnectOutlined, DatabaseOutlined, SendOutlined, BarChartOutlined
} from '@ant-design/icons';
import toast from 'react-hot-toast';
import { useApp } from '../context/AppContext';
import api from '../api/axios';

const { Header, Sider, Content } = AntLayout;

const menuItems = [
  { key: '/app/dashboard',    icon: <DashboardOutlined />,      label: 'Dashboard' },
  { key: '/app/analytics',    icon: <BarChartOutlined />,       label: 'Analytics' },         // ✅ NEW
  { key: '/app/smart-sender', icon: <SendOutlined />,           label: 'Smart WA Sender' },   // ✅ NEW
  { key: '/app/query',        icon: <CodeOutlined />,           label: 'Query Executor' },
  { key: '/app/whatsapp',     icon: <WhatsAppOutlined />,       label: 'WhatsApp' },
  { key: '/app/reports',      icon: <FileTextOutlined />,       label: 'Reports' },
  { key: '/app/scheduler',    icon: <ClockCircleOutlined />,    label: 'Scheduler' },
  { key: '/app/logs',         icon: <UnorderedListOutlined />,  label: 'Logs' },
];

export default function Layout() {
  const [collapsed, setCollapsed] = useState(false);
  const { theme, toggleTheme, connectedDb, disconnectDb } = useApp();
  const navigate  = useNavigate();
  const location  = useLocation();

  const handleDisconnect = async () => {
    try { await api.post('/api/connector/disconnect'); } catch { /* ignore */ }
    disconnectDb();
    toast.success('Disconnected from database');
    navigate('/');
  };

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible collapsed={collapsed} trigger={null} width={220}
        style={{ background: '#001529' }}
      >
        {/* Logo / brand */}
        <div style={{
          padding: collapsed ? '16px 8px' : '16px 20px',
          display: 'flex', alignItems: 'center', gap: 10,
          borderBottom: '1px solid rgba(255,255,255,0.08)'
        }}>
          <WhatsAppOutlined style={{ fontSize: 24, color: '#25D366', flexShrink: 0 }} />
          {!collapsed && (
            <span style={{ color: '#fff', fontWeight: 700, fontSize: 14, lineHeight: 1.3 }}>
              WA Alert<br />
              <span style={{ fontWeight: 400, fontSize: 11, color: 'rgba(255,255,255,0.5)' }}>
                Reporting System
              </span>
            </span>
          )}
        </div>

        {/* DB status badge */}
        {!collapsed && (
          <div style={{ padding: '8px 16px' }}>
            <Badge status="success" text={
              <span style={{ color: 'rgba(255,255,255,0.7)', fontSize: 11 }}>
                <DatabaseOutlined style={{ marginRight: 4 }} />
                {connectedDb || sessionStorage.getItem('connectedDb') || 'Connected'}
              </span>
            } />
          </div>
        )}

        <Menu
          theme="dark" mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ border: 'none', marginTop: 4 }}
        />
      </Sider>

      <AntLayout>
        <Header style={{
          background: theme === 'dark' ? '#141414' : '#fff',
          padding: '0 20px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          borderBottom: '1px solid #f0f0f0',
          position: 'sticky', top: 0, zIndex: 100,
        }}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Tag color="green" icon={<DatabaseOutlined />}>
              {connectedDb || sessionStorage.getItem('connectedDb')}
            </Tag>

            <Tooltip title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
              <Switch
                checkedChildren={<BulbOutlined />}
                unCheckedChildren={<BulbOutlined />}
                checked={theme === 'dark'}
                onChange={toggleTheme}
                size="small"
              />
            </Tooltip>

            <Popconfirm
              title="Disconnect from database?"
              description="You will be returned to the Connection Screen."
              onConfirm={handleDisconnect}
              okText="Disconnect"
              okButtonProps={{ danger: true }}
              cancelText="Cancel"
            >
              <Button danger icon={<DisconnectOutlined />} size="small">
                Disconnect
              </Button>
            </Popconfirm>
          </div>
        </Header>

        <Content style={{ margin: 24, minHeight: 'calc(100vh - 112px)' }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
}