export const environment = {
  production: true,
  apiUrl: '/api'  // Use relative path in production - nginx will proxy to backend
  // For external production deployment, update apiUrl to your backend server
  // Example: apiUrl: 'https://api.yourhamradio.com/api'
};
