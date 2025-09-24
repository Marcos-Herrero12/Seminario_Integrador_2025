import React, { useState, useEffect } from 'react';
        import { Link, useNavigate } from 'react-router-dom';
        import { getEmpresas } from '../api/empresaService';

        const HomePage = () => {
          const [inputValue, setInputValue] = useState('');
          const [mensaje, setMensaje] = useState('');
          const [empresas, setEmpresas] = useState([]);
          const navigate = useNavigate();

          useEffect(() => {
            getEmpresas().then(setEmpresas).catch(console.error);
          }, []);

          const handleClick = () => {
            setMensaje(`¡Hola, ${inputValue || 'usuario'}!`);
            if (inputValue) {
              navigate(`/empresas/${inputValue}`);
            }
          };

          return (
            <div>
              <h1>Bienvenido a la App de Empresas</h1>
              <p>Selecciona una empresa para ver su detalle.</p>
              <input
                type="text"
                placeholder="Ingresa el ID de la empresa"
                value={inputValue}
                onChange={e => setInputValue(e.target.value)}
              />
              <button onClick={handleClick}>Ver Empresa</button>
              {mensaje && <p>{mensaje}</p>}
              <ul>
                  {empresas.map((empresa, idx) => (
                      <li key={empresa.id ?? idx}>
                          <Link to={`/empresas/${empresa.id}`}>{empresa.nombre}</Link>
                      </li>
                  ))}
              </ul>
            </div>
          );
        };

        export default HomePage;