import { useEffect, useMemo, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "../styles/pasantias.css";

const API = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "");

function getStoredItem(key) {
  const persisted = localStorage.getItem(key);
  if (persisted !== null) return persisted;
  return sessionStorage.getItem(key);
}

export default function PostulacionDetalle() {
  const { pasantiaId } = useParams();
  const navigate = useNavigate();
  const [postulaciones, setPostulaciones] = useState([]);
  const [pasantia, setPasantia] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [estadoFilter, setEstadoFilter] = useState("todas");

  const storedUserInfo = getStoredItem("userInfo");
  const userInfo = storedUserInfo ? JSON.parse(storedUserInfo) : null;
  const isStudent = userInfo?.rol === "ESTUDIANTE";

  if (isStudent) {
    return (
      <section className="pasantias-page">
        <div className="container">
          <div className="job-card" style={{ padding: "1.5rem", textAlign: "center" }}>
            <h2 style={{ marginBottom: "8px" }}>Acceso restringido</h2>
            <p className="muted" style={{ marginBottom: "16px" }}>
              Solo administradores o empresas pueden ver las postulaciones.
            </p>
            <button className="btn btn-primary" onClick={() => navigate("/pasantias")}>
              Volver a pasantÃ­as
            </button>
          </div>
        </div>
      </section>
    );
  }

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        setError("");
        const token = localStorage.getItem("authToken") || sessionStorage.getItem("authToken");
        
        const headers = {
          Accept: "application/json;charset=UTF-8",
          "Content-Type": "application/json;charset=UTF-8"
        };
        
        if (token) {
          headers.Authorization = `Bearer ${token}`;
        }

        // Fetch all postulaciones for the pasantia
        const resPostulaciones = await fetch(`${API}/postulaciones/pasantia/${pasantiaId}`, {
          headers
        });

        if (!resPostulaciones.ok) {
          throw new Error(`HTTP ${resPostulaciones.status}`);
        }

        const textPostulaciones = await resPostulaciones.text();
        const dataPostulaciones = JSON.parse(textPostulaciones);
        
        if (dataPostulaciones.codigo === 0 && dataPostulaciones.data && Array.isArray(dataPostulaciones.data)) {
          setPostulaciones(dataPostulaciones.data);
          
          // Fetch pasantia details if we have postulaciones
          if (dataPostulaciones.data.length > 0) {
            const pasantiaIdFromPost = dataPostulaciones.data[0].idPasantia;
            const resPasantia = await fetch(`${API}/pasantias/${pasantiaIdFromPost}`, {
              headers: {
                Accept: "application/json;charset=UTF-8",
                "Content-Type": "application/json;charset=UTF-8"
              }
            });
            
            if (resPasantia.ok) {
              const textPasantia = await resPasantia.text();
              const dataPasantia = JSON.parse(textPasantia);
              setPasantia(dataPasantia);
            }
          }
        } else {
          setPostulaciones([]);
        }
      } catch (err) {
        setError(err.message || "No se pudieron cargar las postulaciones.");
      } finally {
        setLoading(false);
      }
    }

    if (pasantiaId) {
      load();
    }
  }, [pasantiaId]);

  const estadosDisponibles = useMemo(() => {
    const set = new Set(
      postulaciones
        .map((p) => (p.estado || "").trim())
        .filter(Boolean)
    );
    return ["todas", ...Array.from(set)];
  }, [postulaciones]);

  const filteredPostulaciones = useMemo(() => {
    const term = search.toLowerCase().trim();
    return postulaciones
      .filter((p) => {
        const matchEstado = estadoFilter === "todas" || (p.estado || "").trim() === estadoFilter;
        const matchTerm =
          term === "" ||
          [p.nombreEstudiante, p.apellidoEstudiante, p.dniEstudiante, p.legajoEstudiante]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(term));
        return matchEstado && matchTerm;
      })
      .sort((a, b) => {
        const fechaA = a.fechaPostulacion ? new Date(a.fechaPostulacion).getTime() : 0;
        const fechaB = b.fechaPostulacion ? new Date(b.fechaPostulacion).getTime() : 0;
        return fechaB - fechaA;
      });
  }, [postulaciones, search, estadoFilter]);

  if (loading) {
    return (
      <section className="pasantias-page">
        <div className="container">
          <div className="job-card" style={{ padding: "1.5rem" }}>
            <p className="muted">Cargando postulaciones...</p>
          </div>
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="pasantias-page">
        <div className="container">
          <div className="emp-alert error">
            {error}
            <button className="link" onClick={() => navigate("/pasantias")}>
              Volver a pasantÃ­as
            </button>
          </div>
        </div>
      </section>
    );
  }

  const getEstadoBadgeClass = (estado) => {
    const estadoLower = estado?.toLowerCase() || "";
    if (estadoLower.includes("pendiente")) return "badge warning";
    if (estadoLower.includes("publicada") || estadoLower.includes("cubierta")) return "badge success";
    if (estadoLower.includes("finalizada")) return "badge";
    if (estadoLower.includes("borrador")) return "badge";
    return "badge";
  };

  const formatDate = (value) => {
    if (!value) return "No informado";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "No informado";
    return date.toLocaleDateString();
  };

  return (
    <section className="pasantias-page">
      <div className="container">
        <header style={{ margin: "28px 0 2rem" }}>
          <button 
            className="back-link" 
            onClick={() => navigate(`/pasantias/${pasantiaId}`)}
            style={{ marginBottom: "1rem" }}
          >
            <span aria-hidden="true">&#8592;</span> Volver a Pasantia
          </button>
          <h1>Postulaciones</h1>
          {pasantia && (
            <p className="muted">
              {pasantia.titulo} Â· {postulaciones.length} {postulaciones.length === 1 ? "postulaciÃ³n" : "postulaciones"}
            </p>
          )}
        </header>

        <div className="postulaciones-toolbar">
          <div className="postulaciones-summary">
            <div className="summary-card">
              <div className="summary-label">Total</div>
              <div className="summary-value">{postulaciones.length}</div>
            </div>
            <div className="summary-card">
              <div className="summary-label">Mostrando</div>
              <div className="summary-value">{filteredPostulaciones.length}</div>
            </div>
            {pasantia?.estado && (
              <div className="summary-card">
                <div className="summary-label">Estado de pasantÃ­a</div>
                <span className={getEstadoBadgeClass(pasantia.estado)}>{pasantia.estado}</span>
              </div>
            )}
          </div>

          <div className="postulaciones-filters">
            <input
              type="search"
              className="filter-input"
              placeholder="Buscar por nombre, DNI o legajo..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="filter-input"
              value={estadoFilter}
              onChange={(e) => setEstadoFilter(e.target.value)}
            >
              {estadosDisponibles.map((estado) => (
                <option key={estado} value={estado}>
                  {estado === "todas" ? "Todos los estados" : estado}
                </option>
              ))}
            </select>
          </div>
        </div>

        {pasantia && (
          <div className="job-card" style={{ padding: "1.5rem", marginBottom: "1.5rem" }}>
            <div className="pasantia-head">
              <div>
                <p className="muted" style={{ marginBottom: "6px" }}>PasantÃ­a</p>
                <h2 style={{ fontSize: "1.1rem", margin: 0 }}>{pasantia.titulo}</h2>
              </div>
              {pasantia.estado && (
                <span className={getEstadoBadgeClass(pasantia.estado)}>{pasantia.estado}</span>
              )}
            </div>
            <dl className="pasantia-grid">
              {pasantia.empresa?.nombre && (
                <>
                  <dt>Empresa</dt>
                  <dd>{pasantia.empresa.nombre}</dd>
                </>
              )}
              {pasantia.ciudad && (
                <>
                  <dt>Ciudad</dt>
                  <dd>{pasantia.ciudad}</dd>
                </>
              )}
              {pasantia.modalidad && (
                <>
                  <dt>Modalidad</dt>
                  <dd>{pasantia.modalidad}</dd>
                </>
              )}
            </dl>
          </div>
        )}

        {filteredPostulaciones.length === 0 ? (
          <div className="job-card" style={{ padding: "2rem", textAlign: "center" }}>
            <p style={{ marginBottom: "0.3rem" }}>
              {postulaciones.length === 0
                ? "No hay postulaciones para esta pasantÃ­a."
                : "No se encontraron postulaciones con ese filtro."}
            </p>
            {postulaciones.length > 0 && (
              <button className="btn btn-outline sm" onClick={() => { setSearch(""); setEstadoFilter("todas"); }}>
                Limpiar filtros
              </button>
            )}
          </div>
        ) : (
          <div className="postulaciones-grid">
            {filteredPostulaciones.map((postulacion) => (
              <article key={postulacion.idPostulacion} className="postulacion-card">
                <div className="postulacion-card__head">
                  <div>
                    <h3 className="postulacion-card__title">
                      {postulacion.nombreEstudiante} {postulacion.apellidoEstudiante}
                    </h3>
                    <p className="muted">PostulaciÃ³n #{postulacion.idPostulacion}</p>
                  </div>
                  <span className={getEstadoBadgeClass(postulacion.estado)}>
                    {postulacion.estado || "Sin estado"}
                  </span>
                </div>

                <div className="postulacion-card__grid">
                  <section>
                    <h4>Datos de la postulaciÃ³n</h4>
                    <dl>
                      <dt>Fecha de postulaciÃ³n</dt>
                      <dd>{formatDate(postulacion.fechaPostulacion)}</dd>
                      {postulacion.fechaInicioContrato && (
                        <>
                          <dt>Fecha de inicio</dt>
                          <dd>{formatDate(postulacion.fechaInicioContrato)}</dd>
                        </>
                      )}
                      {postulacion.duracionMeses && (
                        <>
                          <dt>DuraciÃ³n</dt>
                          <dd>{postulacion.duracionMeses} meses</dd>
                        </>
                      )}
                      {postulacion.observaciones && (
                        <>
                          <dt>Observaciones</dt>
                          <dd style={{ whiteSpace: "pre-wrap" }}>{postulacion.observaciones}</dd>
                        </>
                      )}
                    </dl>
                  </section>

                  <section>
                    <h4>Datos del estudiante</h4>
                    <dl>
                      {postulacion.emailEstudiante && (
                        <>
                          <dt>Email</dt>
                          <dd><a href={`mailto:${postulacion.emailEstudiante}`}>{postulacion.emailEstudiante}</a></dd>
                        </>
                      )}
                      {postulacion.telefonoEstudiante && (
                        <>
                          <dt>TelÃ©fono celular</dt>
                          <dd><a href={`tel:${postulacion.telefonoEstudiante}`}>{postulacion.telefonoEstudiante}</a></dd>
                        </>
                      )}
                      {postulacion.telefonoFijoEstudiante && (
                        <>
                          <dt>TelÃ©fono fijo</dt>
                          <dd>{postulacion.telefonoFijoEstudiante}</dd>
                        </>
                      )}
                      {postulacion.dniEstudiante && (
                        <>
                          <dt>DNI</dt>
                          <dd>{postulacion.dniEstudiante}</dd>
                        </>
                      )}
                      {postulacion.legajoEstudiante && (
                        <>
                          <dt>Legajo</dt>
                          <dd>{postulacion.legajoEstudiante}</dd>
                        </>
                      )}
                      {postulacion.especialidadEstudiante && (
                        <>
                          <dt>Especialidad</dt>
                          <dd>{postulacion.especialidadEstudiante}</dd>
                        </>
                      )}
                      {(postulacion.calleEstudiante || postulacion.nroCalleEstudiante) && (
                        <>
                          <dt>DirecciÃ³n</dt>
                          <dd>
                            {postulacion.calleEstudiante} {postulacion.nroCalleEstudiante}
                            {postulacion.barrioEstudiante && `, ${postulacion.barrioEstudiante}`}
                            {postulacion.localidadEstudiante && `, ${postulacion.localidadEstudiante}`}
                            {postulacion.provinciaEstudiante && `, ${postulacion.provinciaEstudiante}`}
                          </dd>
                        </>
                      )}
                    </dl>
                  </section>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
