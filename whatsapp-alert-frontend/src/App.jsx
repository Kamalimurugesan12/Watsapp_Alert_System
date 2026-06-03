// // src/App.jsx
// import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
// import { ConfigProvider, theme as antTheme } from 'antd';
// import { Toaster } from 'react-hot-toast';
// import { AppProvider, useApp } from './context/AppContext';
// import ConnectionScreen from './pages/Login';   // renamed Login → ConnectionScreen internally
// import Dashboard        from './pages/Dashboard';
// import QueryExecutor    from './pages/QueryExecutor';
// import WhatsApp         from './pages/WhatsApp';
// import Reports          from './pages/Reports';
// import Scheduler        from './pages/Scheduler';
// import Logs             from './pages/Logs';
// import Layout           from './components/Layout';

// /**
//  * Route guard — if no DB is connected, send user to Connection Screen.
//  * Replaces the old JWT-based ProtectedRoute.
//  */
// function RequiresDb({ children }) {
//   const { dbConnected } = useApp();
//   // Also check sessionStorage for page-refresh persistence
//   const sessionConnected = sessionStorage.getItem('dbConnected') === 'true';
//   return (dbConnected || sessionConnected) ? children : <Navigate to="/" replace />;
// }

// function AppRoutes() {
//   const { theme } = useApp();

//   return (
//     <ConfigProvider
//       theme={{
//         algorithm: theme === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
//         token: {
//           colorPrimary: '#25D366',
//           colorInfo:    '#25D366',
//           borderRadius: 8,
//         },
//       }}
//     >
//       <Toaster position="top-right" toastOptions={{ duration: 3500 }} />
//       <BrowserRouter>
//         <Routes>
//           {/* Connection Screen — always public */}
//           <Route path="/" element={<ConnectionScreen />} />

//           {/* All other pages require an active DB connection */}
//           <Route
//             path="/app"
//             element={
//               <RequiresDb>
//                 <Layout />
//               </RequiresDb>
//             }
//           >
//             <Route index           element={<Navigate to="/app/dashboard" replace />} />
//             <Route path="dashboard" element={<Dashboard />} />
//             <Route path="query"     element={<QueryExecutor />} />
//             <Route path="whatsapp"  element={<WhatsApp />} />
//             <Route path="reports"   element={<Reports />} />
//             <Route path="scheduler" element={<Scheduler />} />
//             <Route path="logs"      element={<Logs />} />
//           </Route>

//           <Route path="*" element={<Navigate to="/" replace />} />
//         </Routes>
//       </BrowserRouter>
//     </ConfigProvider>
//   );
// }

// export default function App() {
//   return (
//     <AppProvider>
//       <AppRoutes />
//     </AppProvider>
//   );
// }
// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, theme as antTheme } from 'antd';
import { Toaster } from 'react-hot-toast';
import { AppProvider, useApp } from './context/AppContext';
import ConnectionScreen from './pages/Login';
import Dashboard        from './pages/Dashboard';
import QueryExecutor    from './pages/QueryExecutor';
import WhatsApp         from './pages/WhatsApp';
import Reports          from './pages/Reports';
import Scheduler        from './pages/Scheduler';
import Logs             from './pages/Logs';
import SmartSender      from './pages/SmartSender';   // ✅ NEW
import Analytics        from './pages/Analytics';     // ✅ NEW
import Layout           from './components/Layout';

function RequiresDb({ children }) {
  const { dbConnected } = useApp();
  const sessionConnected = sessionStorage.getItem('dbConnected') === 'true';
  return (dbConnected || sessionConnected) ? children : <Navigate to="/" replace />;
}

function AppRoutes() {
  const { theme } = useApp();

  return (
    <ConfigProvider
      theme={{
        algorithm: theme === 'dark' ? antTheme.darkAlgorithm : antTheme.defaultAlgorithm,
        token: {
          colorPrimary: '#25D366',
          colorInfo:    '#25D366',
          borderRadius: 8,
        },
      }}
    >
      <Toaster position="top-right" toastOptions={{ duration: 3500 }} />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<ConnectionScreen />} />

          <Route
            path="/app"
            element={
              <RequiresDb>
                <Layout />
              </RequiresDb>
            }
          >
            <Route index              element={<Navigate to="/app/dashboard" replace />} />
            <Route path="dashboard"   element={<Dashboard />} />
            <Route path="analytics"   element={<Analytics />} />        {/* ✅ NEW */}
            <Route path="smart-sender" element={<SmartSender />} />     {/* ✅ NEW */}
            <Route path="query"       element={<QueryExecutor />} />
            <Route path="whatsapp"    element={<WhatsApp />} />
            <Route path="reports"     element={<Reports />} />
            <Route path="scheduler"   element={<Scheduler />} />
            <Route path="logs"        element={<Logs />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}

export default function App() {
  return (
    <AppProvider>
      <AppRoutes />
    </AppProvider>
  );
}