package dgtic.core.controller;

import dgtic.core.exception.ReporteNoAutorizadoException;
import dgtic.core.exception.ReporteRecursoNoEncontradoException;
import dgtic.core.model.dto.reporte.ReporteConjuntoFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteContenedorFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteHojaFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteMovimientoFiltroDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionHojaDTO;
import dgtic.core.service.ReporteExcelService;
import dgtic.core.service.ReporteService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/reportes")
public class ReporteExcelController {
    // MediaType para archivos Excel (.xlsx)
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    // Formato de fecha para los nombres de archivo
    private static final DateTimeFormatter ARCHIVO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReporteService reporteService;
    private final ReporteExcelService reporteExcelService;
    private final SesionService sesionService;

    public ReporteExcelController(
            ReporteService reporteService,
            ReporteExcelService reporteExcelService,
            SesionService sesionService) {
        this.reporteService = reporteService;
        this.reporteExcelService = reporteExcelService;
        this.sesionService = sesionService;
    }
    // Endpoint para exportar hojas de producción a Excel
    @GetMapping("/hojas/excel")
    @ResponseBody
    public ResponseEntity<?> exportarHojas(@ModelAttribute ReporteHojaFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar generar y devolver el archivo Excel
        try {
            // Buscar hojas de producción según el filtro y contexto de sesión
            var reportes = reporteService.buscarHojas(idUsuario(session), idBodega(session), filtro);
            // Generar el archivo Excel con los reportes obtenidos
            byte[] archivo = reporteExcelService.generarHojas(reportes, metadata(
                    "Reporte de Hojas de Producción", filtro, session));
            // Devolver el archivo Excel como respuesta
            return archivo(archivo, "SGREA_Hojas_" + fechaArchivo() + ".xlsx");
        } catch (ReporteNoAutorizadoException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException | IllegalStateException exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible exportar las hojas.");
        }
    }
    // Endpoint para exportar movimientos a Excel
    @GetMapping("/movimientos/excel")
    @ResponseBody
    public ResponseEntity<?> exportarMovimientos(@ModelAttribute ReporteMovimientoFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar generar y devolver el archivo Excel
        try {
            // Buscar movimientos según el filtro y contexto de sesión
            var reportes = reporteService.buscarMovimientos(idUsuario(session), idBodega(session), filtro);
            // Generar el archivo Excel con los reportes obtenidos
            byte[] archivo = reporteExcelService.generarMovimientos(reportes, metadata(
                    "Reporte de Movimientos", filtro, session));
            // Devolver el archivo Excel como respuesta
                return archivo(archivo, "SGREA_Movimientos_" + fechaArchivo() + ".xlsx");
        } catch (ReporteNoAutorizadoException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException | IllegalStateException exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible exportar los movimientos.");
        }
    }
    // Endpoint para exportar contenedores a Excel
    @GetMapping("/contenedores/excel")
    @ResponseBody
    public ResponseEntity<?> exportarContenedores(@ModelAttribute ReporteContenedorFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar generar y devolver el archivo Excel
        try {
            // Buscar contenedores según el filtro y contexto de sesión
            var reportes = reporteService.buscarContenedores(idUsuario(session), idBodega(session), filtro);
            // Generar el archivo Excel con los reportes obtenidos
            byte[] archivo = reporteExcelService.generarContenedores(reportes, metadata(
                    "Reporte de Utilización de Contenedores", filtro, session));
            // Devolver el archivo Excel como respuesta
            return archivo(archivo, "SGREA_Contenedores_" + fechaArchivo() + ".xlsx");
        } catch (ReporteNoAutorizadoException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException | IllegalStateException exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible exportar los contenedores.");
        }
    }
    // Endpoint para exportar inventario de conjuntos a Excel
    @GetMapping("/conjuntos/excel")
    @ResponseBody
    public ResponseEntity<?> exportarInventario(@ModelAttribute ReporteConjuntoFiltroDTO filtro, HttpSession session) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar generar y devolver el archivo Excel
        try {
            // Buscar inventario de conjuntos según el filtro y contexto de sesión
            var reportes = reporteService.buscarInventario(idUsuario(session), idBodega(session), filtro);
            // Generar el archivo Excel con los reportes obtenidos
            byte[] archivo = reporteExcelService.generarInventario(reportes, metadata(
                    "Reporte de Inventario de Conjuntos", filtro, session));
            // Devolver el archivo Excel como respuesta
            return archivo(archivo, "SGREA_Inventario_" + fechaArchivo() + ".xlsx");
        } catch (ReporteNoAutorizadoException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException | IllegalStateException exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible exportar el inventario.");
        }
    }
    // Endpoint para exportar surtido histórico a Excel
    @GetMapping("/hojas/{idHoja}/surtido/excel")
    @ResponseBody
    public ResponseEntity<?> exportarSurtido(@PathVariable Integer idHoja, HttpSession session) {
        // Llamar al método privado para exportar la operación de surtido
        return exportarOperacion(idHoja, session, true);
    }
    // Endpoint para exportar recepción histórica a Excel
    @GetMapping("/hojas/{idHoja}/recepcion/excel")
    @ResponseBody
    public ResponseEntity<?> exportarRecepcion(@PathVariable Integer idHoja, HttpSession session) {
        // Llamar al método privado para exportar la operación de recepción
        return exportarOperacion(idHoja, session, false);
    }
    // Método privado para exportar operaciones (surtido o recepción) a Excel
    private ResponseEntity<?> exportarOperacion(Integer idHoja, HttpSession session, boolean surtido) {
        // Validar contexto de sesión
        ResponseEntity<Map<String, String>> error = validarContexto(session);
        if (error != null) {
            return error;
        }
        // Intentar generar y devolver el archivo Excel
        try {
            // Obtener el reporte de operación (surtido o recepción) según el parámetro 'surtido'
            ReporteOperacionHojaDTO reporte = surtido
                    ? reporteService.obtenerSurtido(idUsuario(session), idBodega(session), idHoja)
                    : reporteService.obtenerRecepcion(idUsuario(session), idBodega(session), idHoja);
            // Crear metadatos para el reporte
            ReporteExcelService.Metadata metadata = metadata(
                    surtido ? "Reporte de Surtido por Hoja" : "Reporte de Recepción por Hoja",
                    "Hoja: " + idHoja,
                    session);
            // Generar el archivo Excel según el tipo de operación
            byte[] archivo = surtido
                    ? reporteExcelService.generarSurtido(reporte, metadata)
                    : reporteExcelService.generarRecepcion(reporte, metadata);
            // Determinar el tipo de operación para el nombre del archivo
            String tipo = surtido ? "Surtido" : "Recepcion";
            // Devolver el archivo Excel como respuesta
            return archivo(archivo, "SGREA_Hoja_" + idHoja + "_" + tipo + ".xlsx");
        } catch (ReporteNoAutorizadoException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (ReporteRecursoNoEncontradoException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException | IllegalStateException exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    surtido ? "No fue posible exportar el surtido histórico."
                            : "No fue posible exportar la recepción histórica.");
        }
    }
    // Método privado para crear metadatos del reporte
    private ReporteExcelService.Metadata metadata(String titulo, Object filtros, HttpSession session) {
        // Crear metadatos del reporte utilizando el título, filtros y contexto de sesión
        return metadata(titulo, String.valueOf(filtros), session);
    }
    // Método privado para crear metadatos del reporte
    private ReporteExcelService.Metadata metadata(String titulo, String filtros, HttpSession session) {
        // Obtener el nombre de la bodega y del usuario desde la sesión
        Object nombreBodega = session.getAttribute("nombreBodega");
        Object nombreUsuario = session.getAttribute("nombreCompletoUsuario");
        // Si no se encuentra el nombre de la bodega, intentar obtenerlo desde otro atributo
        if (nombreUsuario == null) {
            nombreUsuario = session.getAttribute("nombreUsuario");
        }
        // Crear y devolver un objeto Metadata con la información del reporte
        return new ReporteExcelService.Metadata(
                titulo,
                nombreBodega == null ? String.valueOf(idBodega(session)) : String.valueOf(nombreBodega),
                nombreUsuario == null ? String.valueOf(idUsuario(session)) : String.valueOf(nombreUsuario),
                LocalDateTime.now(),
                filtros);
    }
    // Método privado para construir la respuesta con el archivo Excel
    private ResponseEntity<byte[]> archivo(byte[] contenido, String nombre) {
        // Construir y devolver la respuesta HTTP con el archivo Excel adjunto
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombre + "\"")
                .body(contenido);
    }
    // Método privado para validar el contexto de la sesión
    private ResponseEntity<Map<String, String>> validarContexto(HttpSession session) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return error(HttpStatus.UNAUTHORIZED, "La sesión ha expirado.");
        }
        // Verificar si la sesión tiene contexto operativo
        if (idUsuario(session) == null || idBodega(session) == null) {
            return error(HttpStatus.UNAUTHORIZED, "La sesión no tiene contexto operativo.");
        }
        return null;
    }
    // Método privado para obtener el ID del usuario desde la sesión
    private Integer idUsuario(HttpSession session) {
        return (Integer) session.getAttribute("idUsuario");
    }

    // Método privado para obtener el ID de la bodega desde la sesión
    private Integer idBodega(HttpSession session) {
        return (Integer) session.getAttribute("idBodega");
    }

    // Método privado para obtener la fecha y hora actual formateada para el nombre del archivo
    private String fechaArchivo() {
        return LocalDateTime.now().format(ARCHIVO_FECHA);
    }

    // Método privado para construir una respuesta de error con un mensaje específico
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        // Construir un mapa con el mensaje de error
        Map<String, String> cuerpo = new HashMap<>();
        // Si el mensaje es nulo, usar un mensaje por defecto
        cuerpo.put("mensaje", mensaje == null ? "La operación no es válida." : mensaje);
        // Devolver la respuesta HTTP con el código de estado y el cuerpo del mensaje
        return ResponseEntity.status(status).body(cuerpo);
    }
}
