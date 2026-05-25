import { io } from 'socket.io-client';

const explicit = import.meta.env.VITE_SOCKET_URL;
const SOCKET_URL = explicit && explicit.trim() !== '' ? explicit : undefined;

export const socket = io(SOCKET_URL, {
  autoConnect: true,
  transports: ['websocket', 'polling']
});

socket.on('connect', () => console.log('Socket connected:', socket.id));
socket.on('disconnect', () => console.log('Socket disconnected'));
