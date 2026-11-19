import api from './api';

export const registrarUsuario = async (datos) => {
  const response = await api.post('/auth/register', datos);
  return response.data;
};

export const iniciarSesion = async (credenciales) => {
  const response = await api.post('/auth/login', credenciales);
  if (response.data.token) {
    localStorage.setItem('token', response.data.token);
    localStorage.setItem('rol', response.data.rol);
    localStorage.setItem('nombre', response.data.nombre);
  }
  return response.data;
};

export const cerrarSesion = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('rol');
  localStorage.removeItem('nombre');
};