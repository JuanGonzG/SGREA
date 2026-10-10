package dgtic.core.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import dgtic.core.model.dto.reporte.ReporteHojaDetalleDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionHojaDTO;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportePdfService {
    // Formato de fecha y hora para el reporte
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-MX"));
    // Motor de plantillas Thymeleaf para renderizar HTML
    private final SpringTemplateEngine templateEngine;
    // Constructor que inyecta el motor de plantillas
    public ReportePdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    // Genera un PDF de la hoja de producción a partir del reporte y los datos proporcionados
    public byte[] generarHoja(ReporteHojaDetalleDTO reporte, String nombreBodega, String nombreUsuario) {
        return renderizar("reportes/pdf/hoja", reporte, nombreBodega, nombreUsuario,
                "SGREA - Hoja de producción " + reporte.getIdHoja());
    }
    // Genera un PDF del surtido a partir del reporte y los datos proporcionados
    public byte[] generarSurtido(ReporteOperacionHojaDTO reporte, String nombreBodega, String nombreUsuario) {
        return renderizar("reportes/pdf/surtido", reporte, nombreBodega, nombreUsuario,
                "SGREA - Surtido hoja " + reporte.getIdHoja());
    }
    // Genera un PDF de la recepción a partir del reporte y los datos proporcionados
    public byte[] generarRecepcion(ReporteOperacionHojaDTO reporte, String nombreBodega, String nombreUsuario) {
        return renderizar("reportes/pdf/recepcion", reporte, nombreBodega, nombreUsuario,
                "SGREA - Recepción hoja " + reporte.getIdHoja());
    }
    // Método privado que renderiza el contenido HTML a PDF utilizando la plantilla Thymeleaf y los datos proporcionados
    private byte[] renderizar(String plantilla, Object reporte, String nombreBodega,
                              String nombreUsuario, String titulo) {
        // Configura el contexto de Thymeleaf con las variables necesarias para la plantilla
        Context context = new Context(Locale.forLanguageTag("es-MX"));
        context.setVariables(Map.of(
                "reporte", reporte,
                "nombreBodega", valorSeguro(nombreBodega),
                "nombreUsuario", valorSeguro(nombreUsuario),
                "titulo", titulo,
                "generadoEn", FECHA_HORA.format(LocalDateTime.now())
        ));
        // Procesa la plantilla Thymeleaf para generar el contenido HTML
        String html = templateEngine.process(plantilla, context);
        // Renderiza el contenido HTML a PDF utilizando OpenHTMLToPDF y devuelve el resultado como un arreglo de bytes
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder()
                    .useFastMode()
                    .withHtmlContent(html, null)
                    .toStream(output)
                    .run();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar el PDF del reporte.", ex);
        }
    }

    // Devuelve un valor seguro para una cadena, reemplazando null o cadenas en blanco con "No disponible"
    private String valorSeguro(String valor) {
        return valor == null || valor.isBlank() ? "No disponible" : valor;
    }
}
