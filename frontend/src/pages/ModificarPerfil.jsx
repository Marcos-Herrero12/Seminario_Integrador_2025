import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/modificar-perfil.css";

const API = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "");

function getStoredItem(key) {
  const persisted = localStorage.getItem(key);
  if (persisted !== null) return persisted;
  return sessionStorage.getItem(key);
}

function Toast({ toasts, onClose }) {
  return (
    <div className="toast-stack" role="status" aria-live="polite">
      {toasts.map(t => (
        <div
          key={t.id}
          className={`toast ${t.type === "error" ? "toast-error" : "toast-success"}`}
          onClick={() => onClose(t.id)}
        >
          {t.message}
          <button className="toast-close" aria-label="Cerrar" onClick={() => onClose(t.id)}>×</button>
        </div>
      ))}
    </div>
  );
}

export default function ModificarPerfil() {
  const [user, setUser] = useState(null);
  const [perfilData, setPerfilData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const navigate = useNavigate();

  // Estados para el formulario de datos de cuenta
  const [accountForm, setAccountForm] = useState({
    email: "",
    password: "",
    confirmPassword: ""
  });

  // Estados para el formulario de datos del estudiante
  const [studentForm, setStudentForm] = useState({
    dni: "",
    apellido: "",
    nombre: "",
    especialidad: "",
    nroLegajo: "",
    calle: "",
    nroCalle: "",
    barrio: "",
    localidad: "",
    provincia: "",
    telCelular: "",
    telFijo: ""
  });

  const [errors, setErrors] = useState({});
  const [activeTab, setActiveTab] = useState("cuenta"); // cuenta, datos

  // Toasts
  const [toasts, setToasts] = useState([]);
  const pushToast = useCallback((message, type = "success") => {
    const id = crypto.randomUUID();
    setToasts(curr => [...curr, { id, message, type }]);
    setTimeout(() => setToasts(curr => curr.filter(t => t.id !== id)), 4500);
  }, []);
  const closeToast = (id) => setToasts(curr => curr.filter(t => t.id !== id));

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
      setAccountForm(prev => ({ ...prev, email: parsed.email }));
    } catch (err) {
      console.error("Error al parsear userInfo", err);
      localStorage.removeItem("userInfo");
      sessionStorage.removeItem("userInfo");
      setUser(null);
    }
  }, []);

  const loadPerfilEstudiante = useCallback(async () => {
    if (!user) return;

    try {
      const token = getStoredItem("authToken");
      if (!token) {
        pushToast("No hay token de autenticación", "error");
        return;
      }

      const response = await fetch(`${API}/estudiantes/perfil?email=${encodeURIComponent(user.email)}`, {
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });

      if (response.ok) {
        const data = await response.json();
        setPerfilData(data);
        setStudentForm({
          dni: data.dni || "",
          apellido: data.apellido || "",
          nombre: data.nombre || "",
          especialidad: data.especialidad || "",
          nroLegajo: data.nroLegajo || "",
          calle: data.calle || "",
          nroCalle: data.nroCalle || "",
          barrio: data.barrio || "",
          localidad: data.localidad || "",
          provincia: data.provincia || "",
          telCelular: data.telCelular || "",
          telFijo: data.telFijo || ""
        });
      } else {
        const errorText = await response.text();
        pushToast(errorText || "Error al cargar perfil", "error");
      }
    } catch (error) {
      console.error("Error:", error);
      pushToast("Error al cargar perfil", "error");
    }
  }, [user, pushToast]);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  useEffect(() => {
    if (user) {
      loadPerfilEstudiante();
    }
    setLoading(false);
  }, [user, loadPerfilEstudiante]);

  const handleAccountSubmit = async (e) => {
    e.preventDefault();
    setUpdating(true);
    setErrors({});

    try {
      const newErrors = {};

      // Validaciones
      if (accountForm.password && accountForm.password !== accountForm.confirmPassword) {
        newErrors.confirmPassword = "Las contraseñas no coinciden";
      }

      if (accountForm.password && !accountForm.password.match(/^(?=.*[a-z])(?=.*\d).{8,}$/)) {
        newErrors.password = "La contraseña debe tener al menos 8 caracteres, una letra minúscula y un número";
      }

      if (!accountForm.email) {
        newErrors.email = "El email es obligatorio";
      }

      if (Object.keys(newErrors).length > 0) {
        setErrors(newErrors);
        setUpdating(false);
        return;
      }

      const token = getStoredItem("authToken");
      if (!token) {
        pushToast("No hay token de autenticación", "error");
        setUpdating(false);
        return;
      }

      const requestData = {
        email: accountForm.email !== user.email ? accountForm.email : null,
        password: accountForm.password || null
      };

      // Filtrar campos null
      Object.keys(requestData).forEach(key => {
        if (requestData[key] === null || requestData[key] === "") {
          delete requestData[key];
        }
      });

      if (Object.keys(requestData).length === 0) {
        pushToast("No hay cambios para guardar", "error");
        setUpdating(false);
        return;
      }

      const response = await fetch(`${API}/estudiantes/updateEstudiante?currentEmail=${encodeURIComponent(user.email)}`, {
        method: "PUT",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(requestData)
      });

      const data = await response.json();

      if (response.ok && data.code === 0) {
        pushToast("Datos de cuenta actualizados exitosamente", "success");
        
        // Si cambió el email, actualizar localStorage y redirigir al login
        if (requestData.email) {
          pushToast("Email actualizado. Por favor, inicia sesión nuevamente.", "success");
          setTimeout(() => {
            localStorage.removeItem("authToken");
            localStorage.removeItem("userInfo");
            sessionStorage.removeItem("authToken");
            sessionStorage.removeItem("userInfo");
            navigate("/login", { replace: true });
          }, 2000);
        }

        // Limpiar formulario
        setAccountForm(prev => ({ ...prev, password: "", confirmPassword: "" }));
      } else {
        pushToast(data.message || "Error al actualizar datos", "error");
      }
    } catch (error) {
      console.error("Error:", error);
      pushToast("Error de conexión", "error");
    } finally {
      setUpdating(false);
    }
  };

  const handleStudentSubmit = async (e) => {
    e.preventDefault();
    setUpdating(true);

    try {
      const token = getStoredItem("authToken");
      if (!token) {
        pushToast("No hay token de autenticación", "error");
        setUpdating(false);
        return;
      }

      const response = await fetch(`${API}/estudiantes/completarPerfil?email=${encodeURIComponent(user.email)}`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(studentForm)
      });

      const data = await response.json();

      if (response.ok && data.code === 0) {
        pushToast("Datos del estudiante actualizados exitosamente", "success");
        loadPerfilEstudiante(); // Recargar datos
      } else {
        pushToast(data.message || "Error al actualizar datos", "error");
      }
    } catch (error) {
      console.error("Error:", error);
      pushToast("Error de conexión", "error");
    } finally {
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <section className="modificar-perfil-page">
        <div className="container">
          <p>Cargando datos…</p>
        </div>
      </section>
    );
  }

  if (!user) {
    return (
      <section className="modificar-perfil-page">
        <div className="container">
          <h1>No has iniciado sesión</h1>
          <p>Para modificar tu perfil necesitás iniciar sesión.</p>
          <button className="btn btn-primary" onClick={() => navigate("/login")}>Iniciar sesión</button>
        </div>
      </section>
    );
  }

  if (user.rol !== "ESTUDIANTE") {
    return (
      <section className="modificar-perfil-page">
        <div className="container">
          <h1>Acceso denegado</h1>
          <p>Solo los estudiantes pueden modificar su perfil.</p>
          <button className="btn btn-primary" onClick={() => navigate("/")}>Volver al inicio</button>
        </div>
      </section>
    );
  }

  return (
    <section className="modificar-perfil-page">
      <div className="container">
        <div className="modificar-perfil-header">
          <h1>Modificar perfil</h1>
          <button 
            className="back-link" 
            onClick={() => navigate("/perfil")}
          >
            ← Volver al perfil
          </button>
        </div>

        <div className="modificar-perfil-tabs">
          <button 
            className={`tab-button ${activeTab === "cuenta" ? "active" : ""}`}
            onClick={() => setActiveTab("cuenta")}
          >
            Datos de cuenta
          </button>
          <button 
            className={`tab-button ${activeTab === "datos" ? "active" : ""}`}
            onClick={() => setActiveTab("datos")}
          >
            Datos del estudiante
          </button>
        </div>

        {activeTab === "cuenta" && (
          <div className="modificar-perfil-form">
            <h2>Datos de cuenta</h2>
            <form onSubmit={handleAccountSubmit}>
              <div className="form-group">
                <label htmlFor="email">Email *</label>
                <input
                  type="email"
                  id="email"
                  value={accountForm.email}
                  onChange={(e) => setAccountForm(prev => ({ ...prev, email: e.target.value }))}
                  className={errors.email ? "error" : ""}
                  required
                />
                {errors.email && <span className="error-text">{errors.email}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="password">Nueva contraseña</label>
                <input
                  type="password"
                  id="password"
                  value={accountForm.password}
                  onChange={(e) => setAccountForm(prev => ({ ...prev, password: e.target.value }))}
                  className={errors.password ? "error" : ""}
                  placeholder="Dejar en blanco para mantener la actual"
                />
                {errors.password && <span className="error-text">{errors.password}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="confirmPassword">Confirmar contraseña</label>
                <input
                  type="password"
                  id="confirmPassword"
                  value={accountForm.confirmPassword}
                  onChange={(e) => setAccountForm(prev => ({ ...prev, confirmPassword: e.target.value }))}
                  className={errors.confirmPassword ? "error" : ""}
                  placeholder="Confirmar nueva contraseña"
                />
                {errors.confirmPassword && <span className="error-text">{errors.confirmPassword}</span>}
              </div>

              <button 
                type="submit" 
                className="btn btn-primary" 
                disabled={updating}
              >
                {updating ? "Actualizando..." : "Actualizar datos de cuenta"}
              </button>
            </form>
          </div>
        )}

        {activeTab === "datos" && (
          <div className="modificar-perfil-form">
            <h2>Datos del estudiante</h2>
            <form onSubmit={handleStudentSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="dni">DNI</label>
                  <input
                    type="text"
                    id="dni"
                    value={studentForm.dni}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, dni: e.target.value }))}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="nroLegajo">Número de legajo</label>
                  <input
                    type="text"
                    id="nroLegajo"
                    value={studentForm.nroLegajo}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, nroLegajo: e.target.value }))}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="nombre">Nombre</label>
                  <input
                    type="text"
                    id="nombre"
                    value={studentForm.nombre}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, nombre: e.target.value }))}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="apellido">Apellido</label>
                  <input
                    type="text"
                    id="apellido"
                    value={studentForm.apellido}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, apellido: e.target.value }))}
                  />
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="especialidad">Especialidad</label>
                <input
                  type="text"
                  id="especialidad"
                  value={studentForm.especialidad}
                  onChange={(e) => setStudentForm(prev => ({ ...prev, especialidad: e.target.value }))}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="calle">Calle</label>
                  <input
                    type="text"
                    id="calle"
                    value={studentForm.calle}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, calle: e.target.value }))}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="nroCalle">Número</label>
                  <input
                    type="number"
                    id="nroCalle"
                    value={studentForm.nroCalle}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, nroCalle: e.target.value }))}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="barrio">Barrio</label>
                  <input
                    type="text"
                    id="barrio"
                    value={studentForm.barrio}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, barrio: e.target.value }))}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="localidad">Localidad</label>
                  <input
                    type="text"
                    id="localidad"
                    value={studentForm.localidad}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, localidad: e.target.value }))}
                  />
                </div>
              </div>

              <div className="form-group">
                <label htmlFor="provincia">Provincia</label>
                <input
                  type="text"
                  id="provincia"
                  value={studentForm.provincia}
                  onChange={(e) => setStudentForm(prev => ({ ...prev, provincia: e.target.value }))}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="telCelular">Teléfono celular</label>
                  <input
                    type="tel"
                    id="telCelular"
                    value={studentForm.telCelular}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, telCelular: e.target.value }))}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="telFijo">Teléfono fijo</label>
                  <input
                    type="tel"
                    id="telFijo"
                    value={studentForm.telFijo}
                    onChange={(e) => setStudentForm(prev => ({ ...prev, telFijo: e.target.value }))}
                  />
                </div>
              </div>

              <button 
                type="submit" 
                className="btn btn-primary" 
                disabled={updating}
              >
                {updating ? "Actualizando..." : "Actualizar datos del estudiante"}
              </button>
            </form>
          </div>
        )}

        <Toast toasts={toasts} onClose={closeToast} />
      </div>
    </section>
  );
}