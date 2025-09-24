// src/api/empresaService.js
const API_URL = 'http://localhost:8080/empresas';

export const getEmpresaById = async (id) => {
  const res = await fetch(`${API_URL}/${id}`);
  if (!res.ok) throw new Error('Error al obtener la empresa');
  return res.json();
};

export const getEmpresas = async () => {
  const res = await fetch('http://localhost:8080/empresas');
  if (!res.ok) throw new Error('Error al obtener las empresas');
  return res.json();
};