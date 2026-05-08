import { useCallback, useEffect, useMemo, useState } from "react";
import { Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar.jsx";
import AdminSidebar from "./components/AdminSidebar.jsx";
import Home from "./pages/Home.jsx";
import Internships from "./pages/Internships.jsx";
import Login from "./pages/Login.jsx";
import Empresas from "./pages/Empresas.jsx";

// NUEVO:
import RegistrarEmpresa from "./pages/RegistrarEmpresa.jsx";
import RegistrarUsuario from "./pages/RegistrarUsuario.jsx";
import RegistrarEstudiante from "./pages/RegistrarEstudiante.jsx";
import CompletarPerfil from "./pages/CompletarPerfil.jsx";
import Carreras from "./pages/Carreras.jsx";
import RegistrarCarrera from "./pages/RegistrarCarrera.jsx";
import Perfil from "./pages/Perfil.jsx";
import ModificarPerfil from "./pages/ModificarPerfil.jsx";
import RegistrarPasantia from "./pages/RegistrarPasantia.jsx";
import PasantiaDetalle from "./pages/PasantiaDetalle.jsx";
import PasantiasPublicadas from "./pages/PasantiasPublicadas.jsx";
import PostulacionDetalle from "./pages/PostulacionDetalle.jsx";
import "./styles/admin-layout.css";
import Footer from "./components/Footer.jsx";
import Privacidad from "./pages/Privacidad.jsx";
import Condiciones from "./pages/Condiciones.jsx";

function getStoredItem(key) {
  const persisted = localStorage.getItem(key);
  if (persisted !== null) return persisted;
  return sessionStorage.getItem(key);
}

function useStoredUser() {
  const [user, setUser] = useState(null);

  const readUserFromStorage = useCallback(() => {
    const token = getStoredItem("authToken");
    const userInfoRaw = getStoredItem("userInfo");

    if (!token || !userInfoRaw) {
      setUser(null);
      return;
    }

    try {
      setUser(JSON.parse(userInfoRaw));
    } catch (err) {
      console.error("Error parsing user info:", err);
      localStorage.removeItem("authToken");
      localStorage.removeItem("userInfo");
      sessionStorage.removeItem("authToken");
      sessionStorage.removeItem("userInfo");
      setUser(null);
    }
  }, []);

  useEffect(() => {
    readUserFromStorage();

    const handleAuthChange = () => readUserFromStorage();
    window.addEventListener("storage", handleAuthChange);
    window.addEventListener("auth-change", handleAuthChange);

    return () => {
      window.removeEventListener("storage", handleAuthChange);
      window.removeEventListener("auth-change", handleAuthChange);
    };
  }, [readUserFromStorage]);

  return user;
}

export default function App() {
  const user = useStoredUser();
  const isAdmin = user?.rol === "ADMINISTRADOR";

  const routes = useMemo(() => (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/pasantias" element={<Internships />} />
      <Route path="/pasantias/publicadas" element={<PasantiasPublicadas />} />
      <Route path="/pasantias/:id" element={<PasantiaDetalle />} />
      <Route path="/postulaciones/pasantia/:pasantiaId" element={<PostulacionDetalle />} />
      <Route path="/empresas" element={<Empresas />} />
      <Route path="/administrar-usuarios" element={<RegistrarUsuario />} />
      <Route path="/registrarse" element={<RegistrarEstudiante />} />
      <Route path="/login" element={<Login />} />
      <Route path="/empresa-list" element={<Empresas />} />
      <Route path="/registrar-empresa" element={<RegistrarEmpresa />} />
      <Route path="/carreras" element={<Carreras />} />
      <Route path="/registrar-carrera" element={<RegistrarCarrera />} />
      <Route path="/registrar-pasantia" element={<RegistrarPasantia />} />
      <Route path="/perfil" element={<Perfil />} />
      <Route path="/perfil/completar" element={<CompletarPerfil />} />
      <Route path="/perfil/modificar" element={<ModificarPerfil />} />
      <Route path="/privacidad" element={<Privacidad />} />
      <Route path="/condiciones" element={<Condiciones />} />
    </Routes>
  ), []);

  return (
    <div className="app">
      {isAdmin ? (
        <>
          <div className="admin-layout">
            <AdminSidebar user={user} />
            <main className="main admin-main">{routes}</main>
          </div>
          <Footer />
        </>
      ) : (
        <>
          <Navbar />
          <main className="main">{routes}</main>
          <Footer />
        </>
      )}
    </div>
  );
}
