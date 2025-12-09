import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/perfil.css";

function getStoredItem(key) {
  const persisted = localStorage.getItem(key);
  if (persisted !== null) return persisted;
  return sessionStorage.getItem(key);
}

export default function Perfil() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [photo, setPhoto] = useState(null);
  const navigate = useNavigate();

  const loadUser = useCallback(() => {
    const raw = getStoredItem("userInfo");
    if (!raw) {
      setUser(null);
      setLoading(false);
      return;
    }

    try {
      const parsed = JSON.parse(raw);
      setUser(parsed);
    } catch (err) {
      console.error("Error al parsear userInfo", err);
      localStorage.removeItem("userInfo");
      sessionStorage.removeItem("userInfo");
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  // Cargar foto guardada en localStorage segun el email
  useEffect(() => {
    if (user?.email) {
      const stored = localStorage.getItem(`profilePhoto:${user.email}`);
      if (stored) setPhoto(stored);
    }
  }, [user]);

  const handlePhotoChange = (ev) => {
    const file = ev.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result;
      setPhoto(dataUrl);
      if (user?.email) {
        localStorage.setItem(`profilePhoto:${user.email}`, dataUrl);
      }
    };
    reader.readAsDataURL(file);
  };

  const triggerPhoto = () => {
    const input = document.getElementById("profile-photo-input");
    if (input) input.click();
  };

  const handleRemovePhoto = () => {
    setPhoto(null);
    if (user?.email) {
      localStorage.removeItem(`profilePhoto:${user.email}`);
    }
  };

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const handleLogout = async () => {
    try {
      const token = getStoredItem("authToken");
      if (token) {
        await fetch("http://localhost:8080/auth/cerrarSesion", {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
          }
        });
      }
    } catch (error) {
      console.error("Error al cerrar sesión:", error);
    } finally {
      localStorage.removeItem("authToken");
      localStorage.removeItem("userInfo");
      sessionStorage.removeItem("authToken");
      sessionStorage.removeItem("userInfo");
      window.dispatchEvent(new Event("auth-change"));
      navigate("/login", { replace: true });
    }
  };

  if (loading) {
    return (
      <section className="perfil-page">
        <div className="perfil-card">
          <p>Cargando datos…</p>
        </div>
      </section>
    );
  }

  if (!user) {
    return (
      <section className="perfil-page">
        <div className="perfil-card">
          <h1>No has iniciado sesión</h1>
          <p>Para ver tu perfil necesitás iniciar sesión.</p>
          <button className="btn btn-primary" onClick={() => navigate("/login")}>Iniciar sesión</button>
        </div>
      </section>
    );
  }

  return (
    <section className="perfil-page">
      <div className="perfil-card">
        <div className="perfil-header">
          <div className="perfil-avatar" aria-hidden="true">
            {photo ? (
              <img src={photo} alt="" />
            ) : (
              (user.username || "?").substring(0, 1).toUpperCase()
            )}
          </div>
          <div>
            <h1>{user.username}</h1>
            <p className="perfil-role">{user.rol}</p>
          </div>
        </div>

        <div className="perfil-info">
          <div className="perfil-row">
            <span className="perfil-label">Usuario</span>
            <span>{user.username}</span>
          </div>
          <div className="perfil-row">
            <span className="perfil-label">Email</span>
            <span>{user.email}</span>
          </div>
          <div className="perfil-row">
            <span className="perfil-label">Rol</span>
            <span>{user.rol}</span>
          </div>
        </div>

        <div className="perfil-actions">
          {(user.rol === "ESTUDIANTE" || user.rol === "EMPRESA") && (
            <>
              <input
                id="profile-photo-input"
                type="file"
                accept="image/*"
                style={{ display: "none" }}
                onChange={handlePhotoChange}
              />
              <button className="btn btn-outline" onClick={triggerPhoto}>
                Subir foto
              </button>
              {photo && (
                <button className="btn btn-secondary" onClick={handleRemovePhoto}>
                  Quitar foto
                </button>
              )}
            </>
          )}
          {user.rol === "ESTUDIANTE" && (
            <>
              <button 
                className="btn btn-secondary" 
                onClick={() => navigate("/perfil/completar")}
              >
                Completar perfil
              </button>
              <button 
                className="btn btn-primary" 
                onClick={() => navigate("/perfil/modificar")}
              >
                Modificar perfil
              </button>
            </>
          )}
          <button className="btn btn-danger" onClick={handleLogout}>
            Cerrar sesión
          </button>
        </div>
      </div>
    </section>
  );
}
