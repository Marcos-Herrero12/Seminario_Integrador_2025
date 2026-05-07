import { Link } from "react-router-dom";
import "../styles/footer.css";

export default function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-row">
        <div className="footer-left">
          <p>
            Maestro M. Lopez esq. Cruz Roja Argentina - Ciudad Universitaria
            <br />
            C.P. (X5016ZAA). Tel: +54-0351-598-6016 / Conmutador 598-6000 o 598-6001
          </p>
          <p>Facultad Regional Córdoba ©</p>
        </div>

        <div className="footer-right">
          <nav className="footer-links" aria-label="Enlaces legales">
            <Link to="/privacidad">Política de Privacidad</Link>
            <Link to="/condiciones">Condiciones de Uso</Link>
          </nav>
        </div>
      </div>
    </footer>
  );
}
