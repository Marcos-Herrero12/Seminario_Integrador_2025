import { Link, NavLink } from "react-router-dom";
import { useState, useEffect, useCallback } from "react";
import "../styles/navbar.css";

function getStoredItem(key) {
  const persisted = localStorage.getItem(key);
  if (persisted !== null) return persisted;
  return sessionStorage.getItem(key);
}

export default function Navbar(){
  const [user, setUser] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [profilePhoto, setProfilePhoto] = useState(null);

  const readUserFromStorage = useCallback(() => {
    const token = getStoredItem("authToken");
    const userInfoRaw = getStoredItem("userInfo");

    if (!token || !userInfoRaw) {
      setUser(null);
      setProfilePhoto(null);
      return;
    }

    try {
      const parsed = JSON.parse(userInfoRaw);
      setUser(parsed);
      if (parsed?.email) {
        const storedPhoto = localStorage.getItem(`profilePhoto:${parsed.email}`);
        setProfilePhoto(storedPhoto || null);
      } else {
        setProfilePhoto(null);
      }
    } catch (e) {
      console.error("Error parsing user info:", e);
      localStorage.removeItem("authToken");
      localStorage.removeItem("userInfo");
      sessionStorage.removeItem("authToken");
      sessionStorage.removeItem("userInfo");
      setUser(null);
      setProfilePhoto(null);
    }
  }, []);

  useEffect(() => {
    readUserFromStorage();

    const handleAuthChange = () => readUserFromStorage();
    const handleResize = () => {
      if (window.innerWidth > 960) setMenuOpen(false);
    };

    window.addEventListener("storage", handleAuthChange);
    window.addEventListener("auth-change", handleAuthChange);
    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("storage", handleAuthChange);
      window.removeEventListener("auth-change", handleAuthChange);
      window.removeEventListener("resize", handleResize);
    };
  }, [readUserFromStorage]);

  return (
    <header className="nav">
      <div className="container nav-row">
        <Link to="/" className="brand">
          <img src="/logo-utn.png" alt="UTN Conecta" />
          <span>UTN CONECTA</span>
        </Link>
        <button
          className={`burger ${menuOpen ? "open" : ""}`}
          onClick={() => setMenuOpen((open) => !open)}
          aria-label="Abrir menú de navegación"
          aria-expanded={menuOpen}
          aria-controls="nav-menu"
        >
          <span />
          <span />
          <span />
        </button>

        <div id="nav-menu" className={`nav-menu ${menuOpen ? "open" : ""}`}>
          <nav className="links">
            <NavLink to="/" end onClick={() => setMenuOpen(false)}>Inicio</NavLink>
            <NavLink to="/pasantias" onClick={() => setMenuOpen(false)}>Pasantias</NavLink>
            <NavLink
              to={user?.rol === "ADMINISTRADOR" ? "/empresa-list" : "/empresas"}
              onClick={() => setMenuOpen(false)}
            >
              Empresas
            </NavLink>
            {user?.rol === "ADMINISTRADOR" && (
              <NavLink to="/carreras" onClick={() => setMenuOpen(false)}>Carreras</NavLink>
            )}
          </nav>
          <div className="actions">
            {user ? (
              <div className="user-menu">
                <span className="user-greeting">
                Hola, <strong>{user.username}</strong>
                </span>
                <Link
                  to="/perfil"
                  className="profile-link"
                  aria-label="Ver perfil"
                  onClick={() => setMenuOpen(false)}
                >
                  {profilePhoto ? (
                    <img src={profilePhoto} alt="" aria-hidden="true" className="profile-photo" />
                  ) : (
                    <img src="/icons/profile.svg" alt="" aria-hidden="true" />
                  )}
                </Link>
              </div>
            ) : (
              <>
                <Link to="/registrarse" className="btn-register" onClick={() => setMenuOpen(false)}>Registrarse</Link>
                <Link to="/login" className="btn login" onClick={() => setMenuOpen(false)}>Iniciar Sesion</Link>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
