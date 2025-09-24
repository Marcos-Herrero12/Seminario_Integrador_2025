// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import EmpresaDetailPage from './pages/EmpresaDetailPage';
import HomePage from './pages/HomePage';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/empresas/:id" element={<EmpresaDetailPage />} />
      </Routes>
    </Router>
  );
}

export default App;