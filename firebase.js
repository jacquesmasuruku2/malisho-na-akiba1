/**
 * Firebase Configuration and Client Initializer (JavaScript)
 * MALISHO NA AKIBA (MALAKI) - Project: malisho-na-akiba
 */
import { initializeApp, getApps, getApp } from "firebase/app";

const getEnvVar = (key, defaultValue = '') => {
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    return import.meta.env[key] || defaultValue;
  }
  if (typeof process !== 'undefined' && process.env) {
    return process.env[key] || defaultValue;
  }
  return defaultValue;
};

export const firebaseConfig = {
  apiKey: getEnvVar('VITE_FIREBASE_API_KEY', 'AIzaSyCmpM018RSZ-88a2Dg_hgyIn-AUDekkDHM'),
  authDomain: getEnvVar('VITE_FIREBASE_AUTH_DOMAIN', 'malisho-na-akiba.firebaseapp.com'),
  projectId: getEnvVar('VITE_FIREBASE_PROJECT_ID', 'malisho-na-akiba'),
  storageBucket: getEnvVar('VITE_FIREBASE_STORAGE_BUCKET', 'malisho-na-akiba.firebasestorage.app'),
  messagingSenderId: getEnvVar('VITE_FIREBASE_MESSAGING_SENDER_ID', '82960996383'),
  appId: getEnvVar('VITE_FIREBASE_APP_ID', '1:82960996383:web:9dd08dd75d0a00fa00dd2a'),
  measurementId: getEnvVar('VITE_FIREBASE_MEASUREMENT_ID', 'G-0ZB8DCT7V6')
};

export const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();

export const FIRESTORE_COLLECTIONS = {
  USERS: 'users',
  LOANS: 'loans',
  CREDIT_CYCLES: 'credit_cycles',
  TRANSACTIONS: 'transactions',
  PAYMENTS: 'payments',
  CASH_MOVEMENTS: 'cash_movements',
  AUDIT_LOGS: 'audit_logs',
  CHAT_MESSAGES: 'chat_messages',
  CLIENTS: 'clients',
  SETTINGS: 'business_settings',
};

export default firebaseConfig;
