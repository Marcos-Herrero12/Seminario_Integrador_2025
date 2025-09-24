// src/pages/EmpresaDetailPage.js
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getEmpresaById } from '../api/empresaService';

const EmpresaDetailPage = () => {
  const { id } = useParams();
  const [empresa, setEmpresa] = useState(null);

  useEffect(() => {
    getEmpresaById(id).then(setEmpresa);
  }, [id]);

  if (!empresa) return <div>Cargando...</div>;

  return (
    <div>
      <h2>Detalle de Empresa</h2>
      <p><strong>Nombre:</strong> {empresa.nombre}</p>
      <p><strong>Ciudad:</strong> {empresa.ciudad}</p>
      <p><strong>Dirección:</strong> {empresa.direccion}</p>
      <p><strong>Email:</strong> {empresa.emailContacto}</p>
      <p><strong>Modalidad:</strong> {empresa.modalidad}</p>
    </div>
  );
};

export default EmpresaDetailPage;