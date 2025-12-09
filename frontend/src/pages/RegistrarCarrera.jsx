import { useState, useCallback } from "react";
import "../styles/registrar-carrera.css";

const API = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/,"");

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

export default function RegistrarCarrera() {
  // Verificar si el usuario es administrador
  const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");
  const isAdmin = userInfo.rol === "ADMINISTRADOR";

  // Si no es administrador, mostrar mensaje de acceso denegado
  if (!isAdmin) {
    return (
      <div className="container" style={{ padding: "50px 20px", textAlign: "center" }}>
        <h2 style={{ color: "#dc3545", marginBottom: "20px" }}>Acceso Denegado</h2>
        <p style={{ fontSize: "18px", color: "#666", marginBottom: "30px" }}>
          No tienes permisos para registrar carreras. Solo los administradores pueden acceder a esta funcionalidad.
        </p>
        <button 
          className="btn btn-primary"
          onClick={() => window.location.href = "/"}
        >
          Volver al Inicio
        </button>
      </div>
    );
  }

  const [formData, setFormData] = useState({
    nombre: ""
  });
  
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // toasts
  const [toasts, setToasts] = useState([]);
  const pushToast = useCallback((message, type = "success") => {
    const id = crypto.randomUUID();
    setToasts(curr => [...curr, { id, message, type }]);
    // autodestruir a los 4.5s
    setTimeout(() => setToasts(curr => curr.filter(t => t.id !== id)), 4500);
  }, []);
  const closeToast = (id) => setToasts(curr => curr.filter(t => t.id !== id));

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    
    // limpiar error del campo al escribir
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    // validar nombre
    if (!formData.nombre.trim()) {
      newErrors.nombre = "El nombre de la carrera es obligatorio";
    } else if (formData.nombre.trim().length < 3) {
      newErrors.nombre = "El nombre debe tener al menos 3 caracteres";
    } else if (formData.nombre.trim().length > 100) {
      newErrors.nombre = "El nombre no puede exceder los 100 caracteres";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      pushToast("Por favor, corregí los errores en el formulario.", "error");
      return;
    }

    try {
      setIsSubmitting(true);
      
      // Obtener token de autenticación
      const token = localStorage.getItem("authToken");
      if (!token) {
        throw new Error("No hay token de autenticación. Por favor, inicia sesión.");
      }
      
      const carreraData = {
        nombre: formData.nombre.trim()
      };

      const response = await fetch(`${API}/carreras/registrarCarrera`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Accept": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(carreraData)
      });

      const data = await response.json();

      if (response.ok && data.code === 0) {
        pushToast("Carrera registrada exitosamente", "success");
        // limpiar formulario
        setFormData({ nombre: "" });
        setErrors({});
      } else {
        const errorMessage = data.message || "Error al registrar la carrera";
        pushToast(errorMessage, "error");
      }
      
    } catch (error) {
      console.error("Error al registrar carrera:", error);
      pushToast("Error de conexión. Intentá nuevamente.", "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setFormData({ nombre: "" });
    setErrors({});
  };

  return (
    <div className="registrar-carrera-container">
      <div className="registrar-carrera-header">
        <h1>Registrar Nueva Carrera</h1>
        <p className="registrar-carrera-subtitle">
          Agregá una nueva carrera al sistema
        </p>
      </div>

      <form onSubmit={handleSubmit} className="registrar-carrera-form" noValidate>
        <div className="form-group">
          <label htmlFor="nombre" className="form-label">
            Nombre de la Carrera <span className="required">*</span>
          </label>
          <input
            type="text"
            id="nombre"
            name="nombre"
            value={formData.nombre}
            onChange={handleChange}
            className={`form-input ${errors.nombre ? "error" : ""}`}
            placeholder="Ej: Ingeniería en Sistemas"
            disabled={isSubmitting}
            maxLength={100}
          />
          {errors.nombre && (
            <span className="error-message">{errors.nombre}</span>
          )}
          <span className="form-help">
            {formData.nombre.length}/100 caracteres
          </span>
        </div>

        <div className="form-actions">
          <button
            type="button"
            onClick={resetForm}
            className="btn btn-outline"
            disabled={isSubmitting}
          >
            Limpiar
          </button>
          <button
            type="submit"
            className={`btn btn-primary ${isSubmitting ? "is-loading" : ""}`}
            disabled={isSubmitting || !formData.nombre.trim()}
          >
            {isSubmitting ? "Registrando..." : "Registrar Carrera"}
          </button>
        </div>

        <div className="form-footer">
          <a href="/carreras" className="link">
            ← Volver al listado de carreras
          </a>
        </div>
      </form>

      {/* Toasts */}
      <Toast toasts={toasts} onClose={closeToast} />
    </div>
  );
}
