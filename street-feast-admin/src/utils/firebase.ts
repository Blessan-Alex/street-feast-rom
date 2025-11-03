import { initializeApp } from 'firebase/app';
import { initializeFirestore } from 'firebase/firestore';
import { getAuth, signInAnonymously } from 'firebase/auth';

// Build config from env; fall back to known values if projectId missing
const envConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

// Temporary hardcoded config as fallback if envs are not loaded
const fallbackConfig = {
  apiKey: 'AIzaSyCNWWfigndmxD13tJLM5w9hBG6Y_SSZvwU',
  authDomain: 'street-feast-22c58.firebaseapp.com',
  projectId: 'street-feast-22c58',
  storageBucket: 'street-feast-22c58.appspot.com',
  messagingSenderId: '539846043685',
  appId: '1:539846043685:web:0b25d7e1ecc2a4d653f7cc'
};

// Debug log to verify envs are present at runtime
// eslint-disable-next-line no-console
console.log('Firebase env check', {
  hasApiKey: !!envConfig.apiKey,
  projectId: envConfig.projectId || '(missing)'
});

const firebaseConfig = envConfig.projectId ? envConfig : fallbackConfig;

export const app = initializeApp(firebaseConfig);
// Initialize Firestore with long-polling to avoid GRPC errors in Electron renderer
export const db = initializeFirestore(app, { 
  experimentalAutoDetectLongPolling: true
});

// Store-scoped configuration
export const storeId: string = (import.meta as any).env?.VITE_STORE_ID || 'default';

export const getOrdersCollectionPath = (sid: string = storeId): string => `stores/${sid}/orders`;

// Ensure the admin app is authenticated (Anonymous) so Firestore rules permitting
// authenticated access will allow writes during development.
try {
  const auth = getAuth(app);
  // Avoid duplicate calls if already signed in
  if (!auth.currentUser) {
    signInAnonymously(auth).catch((err) => {
      // eslint-disable-next-line no-console
      console.error('Anonymous auth failed', err);
    });
  }
} catch (err) {
  // eslint-disable-next-line no-console
  console.error('Auth init error', err);
}
