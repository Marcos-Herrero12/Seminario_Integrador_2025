import { useState, useEffect, useMemo, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import FilterBar from "../components/FilterBar.jsx";
import "../styles/pasantias.css";

const API = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "");

function getStoredItem(key) {
  const persisted = localStorage.getItem(key);
  if (persisted !== null) return persisted;
  return sessionStorage.getItem(key);
}

// Helper para formatear fecha relativa
function formatRelativeDate(dateString) {
  if (!dateString) return "Fecha no disponible";
  
  const date = new Date(dateString);
  const now = new Date();
  const diffTime = now - date;
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
  
  if (diffDays === 0) return "Hoy";
  if (diffDays === 1) return "Ayer";
  if (diffDays < 7) return `Hace ${diffDays} días`;
  if (diffDays < 30) return `Hace ${Math.floor(diffDays / 7)} semana${Math.floor(diffDays / 7) !== 1 ? "s" : ""}`;
  if (diffDays < 365) return `Hace ${Math.floor(diffDays / 30)} mes${Math.floor(diffDays / 30) !== 1 ? "es" : ""}`;
  return `Hace ${Math.floor(diffDays / 365)} año${Math.floor(diffDays / 365) !== 1 ? "s" : ""}`;
}

export default function Internships() {
  const navigate = useNavigate();
  const location = useLocation();
  const [filters, setFilters] = useState({ texto: "", carrera: "", modalidad: "" });
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [user, setUser] = useState(null);
  const [approving, setApproving] = useState(new Set());
  const [finalizing, setFinalizing] = useState(new Set());
  const [careers, setCareers] = useState([]);
  const [appliedIds, setAppliedIds] = useState(new Set());

  const readUserFromStorage = useCallback(() => {
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

  const isAdmin = user?.rol === "ADMINISTRADOR";

  async function load() {
    try {
      setLoading(true);
      setError("");
      const res = await fetch(`${API}/pasantias`, {
        headers: { 
          Accept: "application/json;charset=UTF-8",
          "Content-Type": "application/json;charset=UTF-8"
        }
      });
      
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      
      // Asegurar que la respuesta se decodifique como UTF-8
      const text = await res.text();
      const data = JSON.parse(text);
      
      // Mapear los datos del backend al formato esperado por el frontend
      const mappedData = Array.isArray(data) ? data.map((p) => {
        const carreras = (() => {
          const fromArray = Array.isArray(p.carreras)
            ? p.carreras.map((c) => c?.nombre || c).filter(Boolean)
            : [];
          const extra = [p.carrera, p.carreraNombre, p.nombreCarrera].filter(Boolean);
          return Array.from(new Set([...fromArray, ...extra]));
        })();
        return {
          id: p.idPasantia,
          titulo: p.titulo || "",
          empresa: p.nombreEmpresa || "Empresa no especificada",
          ciudad: p.ciudad || "",
          modalidad: p.modalidad || "",
          carreras,
          carreraLabel: carreras.length ? carreras.join(", ") : "Varias",
          publicada: formatRelativeDate(p.fechaPublicacion),
          estado: p.estado,
          puestoACubrir: p.puestoACubrir,
          asignacionEstimulo: p.asignacionEstimulo,
          emailContacto: p.emailContacto,
          aceptaPostulaciones: p.aceptaPostulaciones,
          diasRestantes: p.diasRestantes
        };
      }) : [];
      
      setJobs(mappedData);
    } catch (err) {
      setError(err.message || "No se pudo obtener el listado de pasantías.");
      setJobs([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);


  // Prefill filters from query params (e.g. searches from Home)
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    setFilters((prev) => ({
      ...prev,
      texto: params.get("texto") || "",
      carrera: params.get("carrera") || "",
      modalidad: params.get("modalidad") || ""
    }));
  }, [location.search]);

  useEffect(() => {
    async function loadCareers() {
      try {
        const res = await fetch(`${API}/carreras/listarCarreras`, {
          headers: { Accept: "application/json" }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        setCareers(Array.isArray(data) ? data.map((c) => c.nombre).filter(Boolean) : []);
      } catch (err) {
        console.error("No se pudo obtener el listado de carreras para filtros:", err);
      }
    }
    loadCareers();
  }, []);

  const filtered = useMemo(() => {
    return jobs.filter(j => {
      const t = filters.texto.trim().toLowerCase();
      const byTexto = !t || 
        j.titulo.toLowerCase().includes(t) || 
        j.empresa.toLowerCase().includes(t) || 
        j.ciudad.toLowerCase().includes(t) ||
        (j.puestoACubrir && j.puestoACubrir.toLowerCase().includes(t));
      const byCarrera = (() => {
        if (!filters.carrera) return true;
        const carreras = Array.isArray(j.carreras) ? j.carreras : [];
        if (carreras.length === 0) return false; // si no hay info de carreras, no coincide
        const filterLower = filters.carrera.toLowerCase();
        return carreras.some((c) => String(c).toLowerCase() === filterLower);
      })();
      const byModalidad = !filters.modalidad || j.modalidad === filters.modalidad;
      return byTexto && byCarrera && byModalidad;
    });
  }, [jobs, filters]);

  const hasActiveFilters = Boolean(
    filters.texto.trim() || filters.carrera || filters.modalidad
  );

  // Verificar pasantías ya postuladas por el estudiante autenticado
  useEffect(() => {
    async function loadApplied() {
      if (!user || user.rol !== "ESTUDIANTE") {
        setAppliedIds(new Set());
        return;
      }

      const token = getStoredItem("authToken");
      if (!token) {
        setAppliedIds(new Set());
        return;
      }

      const ids = jobs.map((j) => j.id).filter(Boolean);
      if (ids.length === 0) {
        setAppliedIds(new Set());
        return;
      }

      const headers = {
        Accept: "application/json;charset=UTF-8",
        "Content-Type": "application/json;charset=UTF-8",
        Authorization: `Bearer ${token}`,
      };

      const applied = new Set();
      await Promise.all(ids.map(async (pasantiaId) => {
        try {
          const res = await fetch(`${API}/postulaciones/porPasantia/${pasantiaId}`, { headers });
          if (!res.ok) return;
          const text = await res.text();
          const data = JSON.parse(text);
          if (data?.codigo === 0 && data.data) {
            applied.add(pasantiaId);
          }
        } catch (err) {
          console.error("Error al verificar postulación del usuario:", err);
        }
      }));

      setAppliedIds(applied);
    }

    loadApplied();
  }, [user, jobs]);

  function handleFiltersChange(event) {
    const { name, value } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value
    }));
  }

  function handleResetFilters() {
    setFilters({ texto: "", carrera: "", modalidad: "" });
    if (location.search) {
      navigate("/pasantias", { replace: true });
    }
  }

  async function handleVerDetalles(id) {
    navigate(`/pasantias/${id}`);
  }

  async function handleAprobar(id, e) {
    e.preventDefault();
    e.stopPropagation();
    
    if (!isAdmin) {
      alert("Solo los administradores pueden aprobar pasantías");
      return;
    }

    const token = getStoredItem("authToken");
    if (!token) {
      alert("Debes iniciar sesión para realizar esta acción");
      return;
    }

    if (!confirm("¿Estás seguro de que deseas aprobar esta pasantía?")) {
      return;
    }

    setApproving(prev => new Set(prev).add(id));

    try {
      const res = await fetch(`${API}/pasantias/${id}/aprobar`, {
        method: "PUT",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.mensaje || `Error ${res.status}`);
      }

      // Recargar la lista de pasantías
      await load();
      alert("Pasantía aprobada exitosamente");
    } catch (err) {
      console.error("Error al aprobar pasantía:", err);
      alert(err.message || "Error al aprobar la pasantía");
    } finally {
      setApproving(prev => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  }

  async function handleFinalizar(id, e) {
    e.preventDefault();
    e.stopPropagation();
    
    if (!isAdmin) {
      alert("Solo los administradores pueden finalizar pasantías");
      return;
    }

    const token = getStoredItem("authToken");
    if (!token) {
      alert("Debes iniciar sesión para realizar esta acción");
      return;
    }

    if (!confirm("¿Estás seguro de que deseas finalizar esta pasantía?")) {
      return;
    }

    setFinalizing(prev => new Set(prev).add(id));

    try {
      const res = await fetch(`${API}/pasantias/${id}/finalizar`, {
        method: "PUT",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json"
        }
      });

      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.mensaje || `Error ${res.status}`);
      }

      // Recargar la lista de pasantías
      await load();
      alert("Pasantía finalizada exitosamente");
    } catch (err) {
      console.error("Error al finalizar pasantía:", err);
      alert(err.message || "Error al finalizar la pasantía");
    } finally {
      setFinalizing(prev => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  }

  return (
    <section className="pasantias-page">
      {/* Contenido / resultados */}
      <div className="container results-wrap">
        <header className="section-head results-head">
          <div>
            <p className="section-head__eyebrow">Oportunidades activas</p>
            <h1>Pasantias</h1>
            <p className="muted">
              Explora las pasantias disponibles y filtra por carrera y modalidad.
            </p>
          </div>
          <div className="section-head__meta" style={{ display: "flex", gap: "0.75rem", alignItems: "center", flexWrap: "wrap", justifyContent: "flex-end" }}>
            <span className="section-head__badge">
              {loading ? "Cargando..." :
                error ? "—" :
                `${filtered.length} resultado${filtered.length !== 1 ? "s" : ""}${(filters.texto || filters.carrera || filters.modalidad) ? " (filtrado)" : ""}`}
            </span>
            {user?.rol === "EMPRESA" && (
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => navigate("/registrar-pasantia")}
              >
                + Nueva pasantía
              </button>
            )}
          </div>
        </header>
        <FilterBar
          actions={
            <button
              type="button"
              className={`btn btn-outline sm clean-btn ${!hasActiveFilters ? "is-disabled" : ""}`}
              onClick={() => {
                if (!hasActiveFilters) return;
                handleResetFilters();
              }}
              aria-disabled={!hasActiveFilters}
            >
              Limpiar
            </button>
          }
        >
          <form
            className="pasantias-filter-form"
            onSubmit={(e) => e.preventDefault()}
          >
            <input
              type="text"
              name="texto"
              className="filter-bar-input"
              placeholder="Cargo, empresa o ciudad…"
              value={filters.texto}
              onChange={handleFiltersChange}
            />
            <select
              name="carrera"
              className="filter-bar-input"
              value={filters.carrera}
              onChange={handleFiltersChange}
            >
              <option value="">Todas las carreras</option>
              {careers.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
            <select
              name="modalidad"
              className="filter-bar-input"
              value={filters.modalidad}
              onChange={handleFiltersChange}
            >
              <option value="">Cualquier modalidad</option>
              <option>Presencial</option>
              <option>Híbrida</option>
              <option>Remota</option>
            </select>
          </form>
        </FilterBar>

        {error && (
          <div className="emp-alert error" style={{ marginBottom: "1rem" }}>
            Ocurrió un error: {error} —{" "}
            <button className="link" onClick={load}>Reintentar</button>
          </div>
        )}

        {loading && !error && (
          <div className="results-grid">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="job-card skeleton" style={{ minHeight: "150px" }} />
            ))}
          </div>
        )}

        {!loading && !error && (
          <>
            {filtered.length === 0 ? (
              <div className="no-results">
                {filters.texto || filters.carrera || filters.modalidad
                  ? "No se encontraron pasantías con esos filtros."
                  : "No hay pasantías disponibles en este momento."}
              </div>
            ) : (
              <div className="results-grid">
                {filtered.map(j => (
                    <article key={j.id} className="job-card">
                    <header>
                      <h3>{j.titulo}</h3>
                      <span className="badge">{j.modalidad}</span>
                    </header>
                    <p className="meta">
                      {j.empresa} · {j.ciudad} · {j.carreraLabel || "Varias"}
                    </p>
                    {j.puestoACubrir && (
                      <p className="meta" style={{ fontSize: "0.9em", color: "#666", marginTop: "0.25rem" }}>
                        {j.puestoACubrir}
                      </p>
                    )}
                    <footer>
                      <span className="time">{j.publicada}</span>
                      <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                        {j.aceptaPostulaciones && (!user || user?.rol === "ESTUDIANTE") && !appliedIds.has(j.id) && (
                          <a className="btn btn-postular" href={`/pasantias/${j.id}`}>
                            Postular
                          </a>
                        )}
                        <button 
                          className="btn btn-ghost" 
                          onClick={() => handleVerDetalles(j.id)}
                          style={{ fontSize: "0.9em" }}
                        >
                          Ver detalles
                        </button>
                        {isAdmin && j.estado === "PENDIENTE_DE_APROBACION" && (
                          <button
                            className="btn btn-approve"
                            onClick={(e) => handleAprobar(j.id, e)}
                            disabled={approving.has(j.id)}
                            style={{ fontSize: "0.9em" }}
                          >
                            {approving.has(j.id) ? "Aprobando..." : "Aprobar"}
                          </button>
                        )}
                        {isAdmin && j.estado === "PUBLICADA" && (
                          <button
                            className="btn btn-finish"
                            onClick={(e) => handleFinalizar(j.id, e)}
                            disabled={finalizing.has(j.id)}
                            style={{ 
                              fontSize: "0.9em",
                            }}
                          >
                            {finalizing.has(j.id) ? "Finalizando..." : "Finalizar"}
                          </button>
                        )}
                      </div>
                    </footer>
                  </article>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </section>
  );
}
