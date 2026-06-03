// src/context/AppContext.jsx
// No JWT token. No user object. Just DB connection state + theme.
import { createContext, useContext, useState } from 'react';

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [theme,       setTheme]       = useState(() => localStorage.getItem('theme') || 'light');
  const [dbConnected, setDbConnected] = useState(false);
  const [connectedDb, setConnectedDb] = useState('');

  const connectDb = (dbName) => {
    setDbConnected(true);
    setConnectedDb(dbName);
    // Persist so page refresh doesn't force re-connect
    sessionStorage.setItem('dbConnected', 'true');
    sessionStorage.setItem('connectedDb',  dbName);
  };

  const disconnectDb = () => {
    setDbConnected(false);
    setConnectedDb('');
    sessionStorage.removeItem('dbConnected');
    sessionStorage.removeItem('connectedDb');
  };

  const toggleTheme = () => {
    const next = theme === 'light' ? 'dark' : 'light';
    setTheme(next);
    localStorage.setItem('theme', next);
  };

  return (
    <AppContext.Provider value={{
      theme, toggleTheme,
      dbConnected, connectedDb,
      connectDb, disconnectDb,
    }}>
      {children}
    </AppContext.Provider>
  );
}

export const useApp = () => {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be inside AppProvider');
  return ctx;
};
