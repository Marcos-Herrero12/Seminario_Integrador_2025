import { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "../styles/pasantias.css";

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

export default function PasantiaDetalle() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [pasantia, setPasantia] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [postulacion, setPostulacion] = useState(null);
  const [loadingPostulacion, setLoadingPostulacion] = useState(false);
  const [postulaciones, setPostulaciones] = useState([]);
  const [loadingPostulaciones, setLoadingPostulaciones] = useState(false);
  const [user, setUser] = useState(null);
  const [userLoaded, setUserLoaded] = useState(false);
  const [registering, setRegistering] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [studentId, setStudentId] = useState(null);
  const [cvList, setCvList] = useState([]);
  const [cvLoading, setCvLoading] = useState(false);
  const [selectedCv, setSelectedCv] = useState("");

  const pushToast = useCallback((message, type = "success") => {
    const toastId = crypto.randomUUID();
    setToasts(curr => [...curr, { id: toastId, message, type }]);
    setTimeout(() => setToasts(curr => curr.filter(t => t.id !== toastId)), 4500);
  }, []);

  // cargar id estudiante y CVs cuando hay user estudiante
  useEffect(() => {
    async function loadStudentData() {
      if (!user || user.rol !== "ESTUDIANTE") {
        setStudentId(null);
        setCvList([]);
        return;
      }
      const token = getStoredItem("authToken");
      if (!token) return;
      try {
        setCvLoading(true);
        const resEst = await fetch(`${API}/estudiantes/perfil?email=${encodeURIComponent(user.email)}`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "application/json;charset=UTF-8" }
        });
        const dataEst = await safeJson(resEst);
        const idEst = dataEst?.idEstudiante || dataEst?.id;
        if (idEst) setStudentId(idEst);
        const resCv = await fetch(`${API}/cvs/getCV?idEstudiante=${idEst}`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "application/json;charset=UTF-8" }
        });
        if (resCv.ok) {
          const cvs = await safeJson(resCv);
          const list = Array.isArray(cvs) ? cvs : [];
          setCvList(list);
          if (list.length > 0) setSelectedCv(String(list[0].idCv));
        } else {
          setCvList([]);
        }
      } catch (e) {
        setCvList([]);
      } finally {
        setCvLoading(false);
      }
    }
    if (userLoaded) loadStudentData();
  }, [user, userLoaded]);

  useEffect(() => {
    if (cvList.length > 0 && !selectedCv) {
      setSelectedCv(String(cvList[0].idCv));
    }
  }, [cvList, selectedCv]);

  const closeToast = (id) => setToasts(curr => curr.filter(t => t.id !== id));

  useEffect(() => {
    const readUserFromStorage = () => {
      const token = getStoredItem("authToken");
      const userInfoRaw = getStoredItem("userInfo");

      if (!token || !userInfoRaw) {
        setUser(null);
        return;
      }

      try {
        setUser(JSON.parse(userInfoRaw));
      } catch (e) {
        console.error("Error parsing user info:", e);
        setUser(null);
      }
      setUserLoaded(true);
    };

    readUserFromStorage();
    setUserLoaded(true);
    const handleAuthChange = () => readUserFromStorage();
    window.addEventListener("storage", handleAuthChange);
    window.addEventListener("auth-change", handleAuthChange);
    return () => {
      window.removeEventListener("storage", handleAuthChange);
      window.removeEventListener("auth-change", handleAuthChange);
    };
  }, []);

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        setError("");
        const res = await fetch(`${API}/pasantias/${id}`, {
          headers: { 
            Accept: "application/json;charset=UTF-8",
            "Content-Type": "application/json;charset=UTF-8"
          }
        });

        if (!res.ok) {
          const text = await res.text();
          let errorData;
          try {
            errorData = JSON.parse(text);
          } catch (parseError) {
            errorData = { mensaje: text || `HTTP ${res.status}` };
          }
          throw new Error(errorData.mensaje || `HTTP ${res.status}`);
        }

        const text = await res.text();
        const data = JSON.parse(text);
        setPasantia(data);
      } catch (err) {
        setError(err.message || "No se pudo cargar la pasantía.");
      } finally {
        setLoading(false);
      }
    }

    if (id) {
      load();
    }
  }, [id]);

  useEffect(() => {
    async function loadPostulacion() {
      if (!id || !pasantia) return;

      try {
        setLoadingPostulacion(true);
        const token = getStoredItem("authToken");
        
        if (!token) {
          setLoadingPostulacion(false);
          return;
        }

        const res = await fetch(`${API}/postulaciones/porPasantia/${id}`, {
          headers: { 
            Accept: "application/json;charset=UTF-8",
            "Content-Type": "application/json;charset=UTF-8",
            Authorization: `Bearer ${token}`
          }
        });

        if (res.ok) {
          const text = await res.text();
          try {
            const data = JSON.parse(text);
            if (data.codigo === 0 && data.data) {
              setPostulacion(data.data);
            }
          } catch (e) {
            console.error("Error parsing postulacion response:", e);
          }
        }
      } catch (err) {
        console.error("Error al cargar postulación:", err);
      } finally {
        setLoadingPostulacion(false);
      }
    }

    loadPostulacion();
  }, [id, pasantia]);

  useEffect(() => {
    async function loadPostulaciones() {
      if (!id || !pasantia) return;

      const token = getStoredItem("authToken");
      if (!token) {
        return;
      }

      try {
        setLoadingPostulaciones(true);

        const res = await fetch(`${API}/postulaciones/pasantia/${id}`, {
          headers: {
            Accept: "application/json;charset=UTF-8",
            "Content-Type": "application/json;charset=UTF-8",
            Authorization: `Bearer ${token}`
          }
        });

        if (res.ok) {
          const text = await res.text();
          try {
            const data = JSON.parse(text);
            if (data.codigo === 0 && data.data && Array.isArray(data.data)) {
              setPostulaciones(data.data);
            }
          } catch (e) {
            console.error("Error parsing postulaciones list response:", e);
          }
        }
      } catch (err) {
        console.error("Error al cargar postulaciones:", err);
      } finally {
        setLoadingPostulaciones(false);
      }
    }

    loadPostulaciones();
  }, [id, pasantia]);

  const canViewPostulaciones = userLoaded && user?.rol !== "ESTUDIANTE";

  
  const handleConfirmarPostulacion = useCallback(async () => {
    if (!user || user.rol !== "ESTUDIANTE") {
      pushToast("Debes estar autenticado como estudiante para postular", "error");
      return;
    }

    if (!pasantia || pasantia.estado !== "PUBLICADA") {
      pushToast("Esta pasantia no esta disponible para postulaciones", "error");
      return;
    }

    if (postulacion) {
      pushToast("Ya tienes una postulacion para esta pasantia", "error");
      return;
    }

    if (!cvLoading && cvList.length === 0) {
      pushToast("Necesitas subir un CV en tu perfil antes de postularte", "error");
      return;
    }

    if (!selectedCv) {
      pushToast("Selecciona un CV para postularte", "error");
      return;
    }

    try {
      setRegistering(true);
      const token = getStoredItem("authToken");
      
      if (!token) {
        pushToast("No estas autenticado. Por favor inicia sesion", "error");
        return;
      }

      const resEstudiante = await fetch(`${API}/estudiantes/perfil?email=${encodeURIComponent(user.email)}`, {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "application/json;charset=UTF-8",
          "Content-Type": "application/json;charset=UTF-8"
        }
      });

      const textEstudiante = await resEstudiante.text();
      let estudianteData;
      
      try {
        estudianteData = JSON.parse(textEstudiante);
      } catch (e) {
        throw new Error(textEstudiante || "No se pudo obtener el perfil de estudiante");
      }
      
      if (typeof estudianteData === "string") {
        if (estudianteData.includes("Expected one result") || estudianteData.includes("but found")) {
          throw new Error("Error en la base de datos: se encontraron multiples perfiles de estudiante. Por favor contacta al administrador.");
        }
        throw new Error(estudianteData);
      }
      
      if (!estudianteData || typeof estudianteData !== "object" || !estudianteData.idEstudiante) {
        throw new Error("Perfil de estudiante no encontrado. Por favor completa tu perfil primero.");
      }

      const cvToSend = selectedCv ? parseInt(selectedCv, 10) : null;
      const postulacionRequest = {
        idPasantia: parseInt(id, 10),
        idEstudiante: estudianteData.idEstudiante,
        fechaPostulacion: new Date().toISOString().split("T")[0],
        estado: "BORRADOR",
        idCv: cvToSend
      };

      const resPostulacion = await fetch(`${API}/postulaciones/registrarPostulacion`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "application/json;charset=UTF-8",
          "Content-Type": "application/json;charset=UTF-8"
        },
        body: JSON.stringify(postulacionRequest)
      });

      const text = await resPostulacion.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch (parseError) {
        throw new Error(text || "Error al registrar la postulacion");
      }

      if (resPostulacion.ok && data.codigo === 0) {
        pushToast("Postulacion registrada exitosamente", "success");
        
        if (data.data) {
          setPostulacion(data.data);
        }
        
        try {
          const resUserPost = await fetch(`${API}/postulaciones/porPasantia/${id}`, {
            headers: {
              Accept: "application/json;charset=UTF-8",
              "Content-Type": "application/json;charset=UTF-8",
              Authorization: `Bearer ${token}`
            }
          });
          if (resUserPost.ok) {
            const textUser = await resUserPost.text();
            try {
              const dataUser = JSON.parse(textUser);
              if (dataUser.codigo === 0 && dataUser.data) {
                setPostulacion(dataUser.data);
              }
            } catch (e) {
              console.error("Error parsing user postulacion response:", e);
            }
          }
        } catch (e) {
          console.error("Error fetching user postulacion:", e);
        }
        
        try {
          const resPostulaciones = await fetch(`${API}/postulaciones/pasantia/${id}`, {
            headers: {
              Accept: "application/json;charset=UTF-8",
              "Content-Type": "application/json;charset=UTF-8",
              Authorization: `Bearer ${token}`
            }
          });
          if (resPostulaciones.ok) {
            const textPost = await resPostulaciones.text();
            try {
              const dataPost = JSON.parse(textPost);
              if (dataPost.codigo === 0 && dataPost.data && Array.isArray(dataPost.data)) {
                setPostulaciones(dataPost.data);
              } else if (dataPost.codigo === 0 && !dataPost.data) {
                setPostulaciones([]);
              }
            } catch (e) {
              console.error("Error parsing postulaciones list response:", e);
            }
          } else if (data.data) {
            setPostulaciones(prev => {
              const exists = prev.some(p => p.idPostulacion === data.data.idPostulacion);
              if (!exists) {
                return [...prev, data.data];
              }
              return prev;
            });
          }
        } catch (e) {
          console.error("Error fetching postulaciones list:", e);
          if (data.data) {
            setPostulaciones(prev => {
              const exists = prev.some(p => p.idPostulacion === data.data.idPostulacion);
              if (!exists) {
                return [...prev, data.data];
              }
              return prev;
            });
          }
        }
      } else {
        throw new Error(data?.mensaje || "Error al registrar la postulacion");
      }
    } catch (err) {
      console.error("Error al registrar postulacion:", err);
      pushToast(err.message || "Error al registrar la postulacion", "error");
    } finally {
      setRegistering(false);
    }
  }, [user, pasantia, postulacion, id, selectedCv, cvList, cvLoading, pushToast]);


  if (loading) {
    return (
      <section className="pasantias-page">
        <div className="container">
          <p>Cargando detalles...</p>
        </div>
      </section>
    );
  }

  if (error || !pasantia) {
    return (
      <section className="pasantias-page">
        <div className="container">
          <div className="emp-alert error">
            {error || "Pasantía no encontrada"}
            <button className="link" onClick={() => navigate("/pasantias")}>
              Volver a pasantías
            </button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="pasantias-page">
      <div className="container">
        <header style={{ margin: "28px 0 2rem" }}>
          <button
            className="back-link"
            onClick={() => navigate("/pasantias")}
            style={{ marginBottom: "1rem" }}
          >
            <span aria-hidden="true">&larr;</span> Volver a Pasantia
          </button>
          <h1>{pasantia.titulo}</h1>
          <p className="muted">
            {pasantia.empresa?.nombre || "Empresa no especificada"} · {pasantia.ciudad} · {pasantia.modalidad}
          </p>
        </header>

        <div className="pasantia-detail-layout">
          <article className="job-card" style={{ padding: "2rem" }}>
            <h2>Descripción</h2>
            {pasantia.puestoACubrir && (
              <div style={{ marginBottom: "1.5rem" }}>
                <h3 style={{ fontSize: "1.1em", marginBottom: "0.5rem" }}>Puesto a cubrir</h3>
                <p>{pasantia.puestoACubrir}</p>
              </div>
            )}

            {pasantia.conocimientos && (
              <div style={{ marginBottom: "1.5rem" }}>
                <h3 style={{ fontSize: "1.1em", marginBottom: "0.5rem" }}>Conocimientos requeridos</h3>
                <p style={{ whiteSpace: "pre-wrap" }}>{pasantia.conocimientos}</p>
              </div>
            )}

            {pasantia.otrosRequisitos && (
              <div style={{ marginBottom: "1.5rem" }}>
                <h3 style={{ fontSize: "1.1em", marginBottom: "0.5rem" }}>Otros requisitos</h3>
                <p style={{ whiteSpace: "pre-wrap" }}>{pasantia.otrosRequisitos}</p>
              </div>
            )}

            {pasantia.beneficios && (
              <div style={{ marginBottom: "1.5rem" }}>
                <h3 style={{ fontSize: "1.1em", marginBottom: "0.5rem" }}>Beneficios</h3>
                <p style={{ whiteSpace: "pre-wrap" }}>{pasantia.beneficios}</p>
              </div>
            )}
          </article>

          <aside>
              {/* Button to confirm postulacion - only for ESTUDIANTE users without existing postulacion */}
              {user && user.rol === "ESTUDIANTE" && !postulacion && pasantia?.estado === "PUBLICADA" && (
                <div className="job-card" style={{ padding: "1.5rem", marginBottom: "1rem" }}>
                  <h3 style={{ fontSize: "1.1em", marginBottom: "1rem" }}>Postularse</h3>
                  <p style={{ marginBottom: "1rem", fontSize: "0.9em" }}>
                    ¿Deseas postular a esta pasantía?
                  </p>
                  <button
                    className="btn"
                    onClick={() => setConfirmOpen(true)}
                    disabled={registering}
                    style={{ width: "100%" }}
                  >
                    {registering ? "Registrando..." : "Postularse"}
                  </button>
                </div>
              )}

            {/* Button to view all postulaciones - shows if there are ANY postulaciones */}
            {postulaciones.length > 0 && canViewPostulaciones && (
              <div className="job-card" style={{ padding: "1.5rem", marginBottom: "1rem" }}>
                <h3 style={{ fontSize: "1.1em", marginBottom: "1rem" }}>Postulaciones</h3>
                <p style={{ marginBottom: "1rem", fontSize: "0.9em" }}>
                  {postulaciones.length} {postulaciones.length === 1 ? "postulación" : "postulaciones"} para esta pasantía
                </p>
                <button
                  className="btn"
                  onClick={() => navigate(`/postulaciones/pasantia/${id}`)}
                  style={{ width: "100%" }}
                >
                  Ver Postulaciones
                </button>
              </div>
            )}

            <div className="job-card" style={{ padding: "1.5rem", marginBottom: "1rem" }}>
              <h3 style={{ fontSize: "1.1em", marginBottom: "1rem" }}>Información</h3>
              <dl style={{ display: "grid", gap: "0.75rem" }}>
                {user?.rol === "ADMINISTRADOR" && (
                  <div>
                    <dt style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>Estado</dt>
                    <dd>
                      <span className="badge">{pasantia.estado}</span>
                    </dd>
                  </div>
                )}
                {pasantia.cantidadDePasantes && (
                  <div>
                    <dt style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>Cantidad de pasantes</dt>
                    <dd>{pasantia.cantidadDePasantes}</dd>
                  </div>
                )}
                {pasantia.asignacionEstimulo && (
                  <div>
                    <dt style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>Asignación estimulo</dt>
                    <dd>${pasantia.asignacionEstimulo.toLocaleString()}</dd>
                  </div>
                )}
                {pasantia.fechaPublicacion && (
                  <div>
                    <dt style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>Fecha de publicación</dt>
                    <dd>{new Date(pasantia.fechaPublicacion).toLocaleDateString()}</dd>
                  </div>
                )}
                {pasantia.fechaCaducidad && (
                  <div>
                    <dt style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>Fecha de caducidad</dt>
                    <dd>{new Date(pasantia.fechaCaducidad).toLocaleDateString()}</dd>
                  </div>
                )}
                {pasantia.diasRestantes !== null && pasantia.diasRestantes !== undefined && (
                  <div>
                    <dt style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>Días restantes</dt>
                    <dd>{pasantia.diasRestantes} días</dd>
                  </div>
                )}
                {pasantia.emailContacto && (
                  <div>
                    <dt style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>Email de contacto</dt>
                    <dd>
                      <a href={`mailto:${pasantia.emailContacto}`}>{pasantia.emailContacto}</a>
                    </dd>
                  </div>
                )}
              </dl>
            </div>

            {pasantia.carreras && pasantia.carreras.length > 0 && (
              <div className="job-card" style={{ padding: "1.5rem" }}>
                <h3 style={{ fontSize: "1.1em", marginBottom: "1rem" }}>Carreras</h3>
                <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                  {pasantia.carreras.map((carrera) => (
                    <li key={carrera.idCarrera} style={{ marginBottom: "0.5rem" }}>
                      {carrera.nombre}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </aside>
        </div>
      </div>

      {/* Toast Notifications */}
      <div style={{
        position: "fixed",
        top: "20px",
        right: "20px",
        zIndex: 10000,
        display: "flex",
        flexDirection: "column",
        gap: "0.5rem"
      }}>
        {toasts.map(toast => (
          <div
            key={toast.id}
            style={{
              padding: "1rem 1.5rem",
              backgroundColor: toast.type === "error" ? "#dc3545" : "#28a745",
              color: "white",
              borderRadius: "4px",
              boxShadow: "0 2px 8px rgba(0,0,0,0.2)",
              minWidth: "300px",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: "1rem"
            }}
          >
            <span>{toast.message}</span>
            <button
              onClick={() => closeToast(toast.id)}
              style={{
                background: "transparent",
                border: "none",
                color: "white",
                cursor: "pointer",
                fontSize: "1.2em",
                padding: "0",
                width: "24px",
                height: "24px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center"
              }}
            >
              ×
            </button>
          </div>
        ))}
      </div>

      {/* Confirmación de postulación */}
      {confirmOpen && (
        <div
          className="modal-overlay"
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.45)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000
          }}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="modal"
            style={{
              background: "#fff",
              borderRadius: "12px",
              padding: "1.5rem",
              maxWidth: "420px",
              width: "calc(100% - 32px)",
              boxShadow: "0 12px 32px rgba(0,0,0,0.18)"
            }}
          >
            <h3 style={{ marginTop: 0, marginBottom: "0.75rem" }}>Confirmar postulacion</h3>
            <p style={{ marginBottom: "1.25rem", color: "#475569" }}>
              Vas a enviar tu postulacion a esta pasantia. Deseas continuar?
            </p>

            {user?.rol === "ESTUDIANTE" && (
              <div style={{ marginBottom: "1rem" }}>
                <label htmlFor="cvSelect" style={{ display: "block", fontWeight: 600, marginBottom: "0.4rem" }}>
                  Selecciona un CV para postularte
                </label>
                {cvLoading ? (
                  <p style={{ color: "#64748b", fontSize: "0.9rem" }}>Cargando CVs...</p>
                ) : cvList.length === 0 ? (
                  <p style={{ color: "#dc2626", fontSize: "0.9rem" }}>
                    No tienes CVs cargados. Sube uno desde tu perfil antes de postularte.
                  </p>
                ) : (
                  <select
                    id="cvSelect"
                    value={selectedCv}
                    onChange={(e) => setSelectedCv(e.target.value)}
                    style={{ width: "100%", padding: "0.65rem 0.75rem", borderRadius: "8px", border: "1px solid #cbd5e1" }}
                  >
                    {cvList.map((cv) => (
                      <option key={cv.idCv} value={cv.idCv}>
                        {cv.nombreArchivo || `CV ${cv.idCv}`}
                      </option>
                    ))}
                  </select>
                )}
              </div>
            )}

            <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end", flexWrap: "wrap" }}>
              <button
                className="btn btn-outline"
                onClick={() => setConfirmOpen(false)}
                disabled={registering}
              >
                Cancelar
              </button>
              <button
                className="btn btn-primary"
                onClick={() => {
                  setConfirmOpen(false);
                  handleConfirmarPostulacion();
                }}
                disabled={
                  registering ||
                  (user?.rol === "ESTUDIANTE" && (cvLoading || cvList.length === 0 || !selectedCv))
                }
              >
                {registering ? "Enviando..." : "Confirmar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
