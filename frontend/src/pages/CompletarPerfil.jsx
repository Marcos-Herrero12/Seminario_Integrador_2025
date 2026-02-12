import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/completar-perfil.css";

const API = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "");

function getStoredItem(key) {
  const persisted = localStorage.getItem(key);
  if (persisted !== null) return persisted;
  return sessionStorage.getItem(key);
}

async function safeJson(res) {
  try {
    return await res.json();
  } catch (err) {
    return null;
  }
}

export default function CompletarPerfil() {
  const navigate = useNavigate();
  const [userInfo, setUserInfo] = useState(null);
  const [form, setForm] = useState({
    especialidad: "",
    nroLegajo: "",
    calle: "",
    nroCalle: "",
    barrio: "",
    localidad: "",
    provincia: "",
    telFijo: ""
  });
  
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [success, setSuccess] = useState(false);
  const [photo, setPhoto] = useState(null);
  const [studentId, setStudentId] = useState(null);
  const [cvList, setCvList] = useState([]);
  const [cvLoading, setCvLoading] = useState(false);
  const [cvError, setCvError] = useState("");
  const [cvUploading, setCvUploading] = useState(false);

  // Verificar autenticación
  useEffect(() => {
    const token = getStoredItem("authToken");
    const userInfoStr = getStoredItem("userInfo");
    
    if (!token || !userInfoStr) {
      navigate("/login", { replace: true });
      return;
    }
    
    const userInfo = JSON.parse(userInfoStr);
    setUserInfo(userInfo);
    if (userInfo.rol !== "ESTUDIANTE") {
      navigate("/", { replace: true });
    }
  }, [navigate]);

  // Foto almacenada localmente
  useEffect(() => {
    if (userInfo?.email) {
      const stored = localStorage.getItem(`profilePhoto:${userInfo.email}`);
      if (stored) setPhoto(stored);
    }
  }, [userInfo]);

  // Cargar id estudiante y CVs
  useEffect(() => {
    async function loadCvData() {
      if (!userInfo) return;
      const token = getStoredItem("authToken");
      if (!token) return;
      try {
        setCvLoading(true);
        setCvError("");
        const resEst = await fetch(`${API}/estudiantes/perfil?email=${encodeURIComponent(userInfo.email)}`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "application/json;charset=UTF-8" }
        });
        const dataEst = await safeJson(resEst);
        const idEst = dataEst?.idEstudiante || dataEst?.id;
        setStudentId(idEst || null);
        if (!idEst) throw new Error("No se pudo obtener el id del estudiante");

        const resCv = await fetch(`${API}/cvs/getCV?idEstudiante=${idEst}`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "application/json;charset=UTF-8" }
        });
        if (!resCv.ok) throw new Error(`HTTP ${resCv.status}`);
        const cvs = await safeJson(resCv);
        setCvList(Array.isArray(cvs) ? cvs : []);
      } catch (err) {
        setCvError(err.message || "No se pudieron cargar los CVs");
        setCvList([]);
      } finally {
        setCvLoading(false);
      }
    }
    loadCvData();
  }, [userInfo]);

  const handlePhotoChange = (ev) => {
    const file = ev.target.files?.[0];
    if (!file || !userInfo?.email) return;
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result;
      setPhoto(dataUrl);
      localStorage.setItem(`profilePhoto:${userInfo.email}`, dataUrl);
    };
    reader.readAsDataURL(file);
  };

  const handleRemovePhoto = () => {
    if (!userInfo?.email) return;
    setPhoto(null);
    localStorage.removeItem(`profilePhoto:${userInfo.email}`);
  };

  const handleUploadCv = async (file, inputRef) => {
    if (!file || !studentId) {
      setCvError("Primero completa los datos básicos para obtener tu ID de estudiante");
      if (inputRef) inputRef.value = "";
      return;
    }
    const token = getStoredItem("authToken");
    if (!token) {
      setCvError("Necesitas iniciar sesión para subir un CV");
      if (inputRef) inputRef.value = "";
      return;
    }
    try {
      setCvUploading(true);
      setCvError("");
      const form = new FormData();
      form.append("file", file);
      form.append("idEstudiante", studentId);
      const resUpload = await fetch(`${API}/cvs/subirCV`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: form
      });
      const dataUpload = await safeJson(resUpload);
      if (!resUpload.ok || dataUpload?.code === -1) {
        throw new Error(dataUpload?.message || `HTTP ${resUpload.status}`);
      }
      const resCv = await fetch(`${API}/cvs/getCV?idEstudiante=${studentId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const cvs = await safeJson(resCv);
      setCvList(Array.isArray(cvs) ? cvs : []);
    } catch (err) {
      setCvError(err.message || "Error al subir el CV");
    } finally {
      setCvUploading(false);
      if (inputRef) inputRef.value = "";
    }
  };

  const handleDeleteCv = async (cvId) => {
    const token = getStoredItem("authToken");
    if (!token || !cvId) return;
    if (!confirm("¿Eliminar este CV?")) return;
    try {
      const res = await fetch(`${API}/cvs/eliminarCV/${cvId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` }
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok || data.code === -1) {
        throw new Error(data.message || `HTTP ${res.status}`);
      }
      setCvList((prev) => prev.filter((c) => c.idCv !== cvId));
    } catch (err) {
      setCvError(err.message || "No se pudo eliminar el CV");
    }
  };

  function validate(values) {
    const e = {};
    
    if (!values.especialidad) {
      e.especialidad = "La especialidad/carrera es obligatoria";
    }
    
    if (!values.nroLegajo) {
      e.nroLegajo = "El número de legajo es obligatorio";
    }
    
    if (!values.calle) {
      e.calle = "La calle es obligatoria";
    }
    
    if (!values.nroCalle) {
      e.nroCalle = "El número de calle es obligatorio";
    } else if (!/^\d+$/.test(values.nroCalle)) {
      e.nroCalle = "Debe ser un número";
    }
    
    if (!values.barrio) {
      e.barrio = "El barrio es obligatorio";
    }
    
    if (!values.localidad) {
      e.localidad = "La localidad es obligatoria";
    }
    
    if (!values.provincia) {
      e.provincia = "La provincia es obligatoria";
    }
    
    return e;
  }

  function handleChange(ev) {
    const { name, value } = ev.target;
    setForm((f) => ({ ...f, [name]: value }));
    
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  }

  async function handleSubmit(ev) {
    ev.preventDefault();
    const e = validate(form);
    setErrors(e);
    setSubmitError("");
    
    if (Object.keys(e).length) return;

    try {
      setLoading(true);
      
      const token = getStoredItem("authToken");
      const userInfoStr = getStoredItem("userInfo");
      const userInfo = JSON.parse(userInfoStr);

      const response = await fetch(`${API}/estudiantes/completarPerfil?email=${userInfo.email}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          especialidad: form.especialidad,
          nroLegajo: form.nroLegajo,
          calle: form.calle,
          nroCalle: parseInt(form.nroCalle),
          barrio: form.barrio,
          localidad: form.localidad,
          provincia: form.provincia,
          telFijo: form.telFijo || null
        })
      });

      const data = await response.json();

      if (!response.ok || data.code !== 0) {
        setSubmitError(data.message || "Error al completar el perfil");
        return;
      }

      setSuccess(true);
      
      // Redirigir a la página de perfil después de 2 segundos
      setTimeout(() => {
        navigate("/perfil", { replace: true });
      }, 2000);
      
    } catch (error) {
      console.error("Error al completar perfil:", error);
      setSubmitError("Error de conexión. Intentá nuevamente.");
    } finally {
      setLoading(false);
    }
  }

  function handleSkip() {
    // Permitir omitir por ahora, pero mostrar advertencia
    if (confirm("¿Estás seguro? Necesitarás completar tu perfil más adelante para acceder a todas las funcionalidades.")) {
      navigate("/perfil", { replace: true });
    }
  }

  if (success) {
    return (
      <section className="completar-perfil-hero">
        <div className="container">
          <div className="success-card">
            <div className="success-icon">✓</div>
            <h2>¡Perfil completado exitosamente!</h2>
            <p>Serás redirigido a tu perfil...</p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="completar-perfil-hero">
      <div className="container completar-perfil-grid">
        <div className="completar-perfil-card">
          <div className="perfil-header">
            <h1 className="perfil-title">Completá tu perfil</h1>
            <p className="perfil-subtitle">Necesitamos algunos datos adicionales para continuar</p>
          </div>

          <form onSubmit={handleSubmit} className="perfil-form" noValidate>
            {submitError && <div className="perfil-error">{submitError}</div>}

            <div className="form-section">
              <h3 className="section-title">Información Académica</h3>
              
              <div className="form-row">
                <div className={`field ${errors.especialidad ? "has-error" : ""}`}>
                  <label htmlFor="especialidad">Especialidad/Carrera *</label>
                  <input
                    id="especialidad"
                    type="text"
                    name="especialidad"
                    placeholder="Ingeniería en Sistemas"
                    value={form.especialidad}
                    onChange={handleChange}
                    disabled={loading}
                  />
                  {errors.especialidad && <div className="error">{errors.especialidad}</div>}
                </div>

                <div className={`field ${errors.nroLegajo ? "has-error" : ""}`}>
                  <label htmlFor="nroLegajo">Número de Legajo *</label>
                  <input
                    id="nroLegajo"
                    type="text"
                    name="nroLegajo"
                    placeholder="12345"
                    value={form.nroLegajo}
                    onChange={handleChange}
                    disabled={loading}
                  />
                  {errors.nroLegajo && <div className="error">{errors.nroLegajo}</div>}
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3 className="section-title">Dirección</h3>
              
              <div className="form-row">
                <div className={`field field-large ${errors.calle ? "has-error" : ""}`}>
                  <label htmlFor="calle">Calle *</label>
                  <input
                    id="calle"
                    type="text"
                    name="calle"
                    placeholder="Av. Vélez Sarsfield"
                    value={form.calle}
                    onChange={handleChange}
                    disabled={loading}
                  />
                  {errors.calle && <div className="error">{errors.calle}</div>}
                </div>

                <div className={`field field-small ${errors.nroCalle ? "has-error" : ""}`}>
                  <label htmlFor="nroCalle">Número *</label>
                  <input
                    id="nroCalle"
                    type="text"
                    name="nroCalle"
                    placeholder="1234"
                    value={form.nroCalle}
                    onChange={handleChange}
                    disabled={loading}
                  />
                  {errors.nroCalle && <div className="error">{errors.nroCalle}</div>}
                </div>
              </div>

              <div className={`field ${errors.barrio ? "has-error" : ""}`}>
                <label htmlFor="barrio">Barrio *</label>
                <input
                  id="barrio"
                  type="text"
                  name="barrio"
                  placeholder="Centro"
                  value={form.barrio}
                  onChange={handleChange}
                  disabled={loading}
                />
                {errors.barrio && <div className="error">{errors.barrio}</div>}
              </div>

              <div className="form-row">
                <div className={`field ${errors.localidad ? "has-error" : ""}`}>
                  <label htmlFor="localidad">Localidad *</label>
                  <input
                    id="localidad"
                    type="text"
                    name="localidad"
                    placeholder="Córdoba"
                    value={form.localidad}
                    onChange={handleChange}
                    disabled={loading}
                  />
                  {errors.localidad && <div className="error">{errors.localidad}</div>}
                </div>

                <div className={`field ${errors.provincia ? "has-error" : ""}`}>
                  <label htmlFor="provincia">Provincia *</label>
                  <input
                    id="provincia"
                    type="text"
                    name="provincia"
                    placeholder="Córdoba"
                    value={form.provincia}
                    onChange={handleChange}
                    disabled={loading}
                  />
                  {errors.provincia && <div className="error">{errors.provincia}</div>}
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3 className="section-title">Contacto Adicional (opcional)</h3>
              
              <div className={`field`}>
                <label htmlFor="telFijo">Teléfono Fijo</label>
                <input
                  id="telFijo"
                  type="tel"
                  name="telFijo"
                  placeholder="3514567890"
                  value={form.telFijo}
                  onChange={handleChange}
                  disabled={loading}
                />
              </div>
            </div>

            <div className="form-section">
              <h3 className="section-title">Foto de perfil</h3>
              <div className="perfil-photo-wrapper" style={{ display: "flex", alignItems: "center", gap: "16px" }}>
                <div className="perfil-photo" style={{ width: "72px", height: "72px", borderRadius: "50%", overflow: "hidden", background: "#f3f4f6", display: "grid", placeItems: "center", fontWeight: 700, color: "#475569" }}>
                  {photo ? <img src={photo} alt="Foto de perfil" style={{ width: "100%", height: "100%", objectFit: "cover" }} /> : (userInfo?.username || "?")?.charAt(0)?.toUpperCase() || "?"}
                </div>
                <div className="photo-actions">
                  <input
                    id="profile-photo-input"
                    type="file"
                    accept="image/*"
                    style={{ display: "none" }}
                    onChange={handlePhotoChange}
                  />
                  <button type="button" className="btn-secondary" onClick={() => document.getElementById("profile-photo-input")?.click()}>
                    Subir foto
                  </button>
                  {photo && (
                    <button type="button" className="btn-outline" onClick={handleRemovePhoto}>
                      Quitar foto
                    </button>
                  )}
                </div>
              </div>
              <p className="perfil-subtitle" style={{ marginTop: "8px" }}>Usa una imagen cuadrada para que se vea mejor en el perfil.</p>
            </div>

            <div className="form-section">
              <h3 className="section-title">Currículum (PDF)</h3>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "10px", alignItems: "center", marginBottom: "8px" }}>
                <input
                  id="cv-file-input"
                  type="file"
                  accept="application/pdf"
                  style={{ display: "none" }}
                  onChange={(e) => handleUploadCv(e.target.files?.[0], e.target)}
                />
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() => document.getElementById("cv-file-input")?.click()}
                  disabled={cvUploading}
                >
                  {cvUploading ? "Subiendo..." : "Subir CV"}
                </button>
                {cvError && <span className="error" style={{ marginLeft: "8px" }}>{cvError}</span>}
              </div>
              {cvLoading ? (
                <p className="muted">Cargando CVs...</p>
              ) : cvList.length === 0 ? (
                <p className="muted">Todavía no subiste CVs. Puedes agregar varios y elegirlos al postular.</p>
              ) : (
                <ul className="cv-list">
                  {cvList.map((cv) => (
                    <li key={cv.idCv} className="cv-item">
                      <div>
                        <strong>{cv.nombreArchivo || `CV ${cv.idCv}`}</strong>
                        {cv.fechaSubida && (
                          <span className="muted" style={{ marginLeft: "8px", fontSize: "12px" }}>
                            {new Date(cv.fechaSubida).toLocaleDateString()}
                          </span>
                        )}
                      </div>
                      <div className="cv-actions">
                        <a
                          className="btn btn-outline"
                          href={`${API}/cvs/descargarCV/${cv.idCv}`}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Descargar
                        </a>
                        <button
                          type="button"
                          className="btn btn-danger"
                          onClick={() => handleDeleteCv(cv.idCv)}
                        >
                          Eliminar
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <div className="form-actions">
              <button 
                type="button" 
                className="btn-secondary" 
                onClick={handleSkip}
                disabled={loading}
              >
                Completar más tarde
              </button>
              <button 
                type="submit" 
                className="btn-primary" 
                disabled={loading}
              >
                {loading ? "Guardando..." : "Completar perfil"}
              </button>
            </div>
          </form>
        </div>

        <div className="perfil-art">
          <div className="info-box">
            <h3>¿Por qué necesitamos estos datos?</h3>
            <ul>
              <li>✓ Para verificar tu identidad como estudiante</li>
              <li>✓ Para que las empresas puedan contactarte</li>
              <li>✓ Para personalizar tu experiencia</li>
              <li>✓ Para enviarte oportunidades relevantes</li>
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}
