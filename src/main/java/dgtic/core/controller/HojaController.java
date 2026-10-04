package dgtic.core.controller;

import dgtic.core.model.dto.*;
import dgtic.core.service.DetalleHojaService;
import dgtic.core.service.HojaProduccionService;
import dgtic.core.service.ProductoService;
import dgtic.core.service.SesionService;
import dgtic.core.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "hojas-produccion")
public class HojaController {
    @Autowired
    private SesionService sesionService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private HojaProduccionService hojaProduccionService;
    @Autowired
    private DetalleHojaService detalleHojaService;
    @Autowired
    private ProductoService productoService;

    // Cargar el catálogo de estados de hoja y agregarlo al modelo
    private List<EstadoHojaDTO> cargarCatalogoEstadoHoja(Model model) {
        List<EstadoHojaDTO> catalogoEstadoHoja = hojaProduccionService.getCatalogoEstadoHoja();
        if (catalogoEstadoHoja.isEmpty()) {
            return null;
        }
        model.addAttribute("estadosHoja", catalogoEstadoHoja);
        return catalogoEstadoHoja;
    }

    // Verificar si la hoja de producción está en un estado que permite solo lectura
    private boolean esSoloLectura(HojaProduccionDTO hojaProduccion) {
        return hojaProduccion != null
                && hojaProduccion.getIdHoja() != null
                && hojaProduccion.getEstadoHoja() != null
                && hojaProduccion.getEstadoHoja().getIdEstadoHoja() != null
                && hojaProduccion.getEstadoHoja().getIdEstadoHoja() >= 2;
    }

    // Cargar los detalles de la hoja de producción y agregarlos al modelo
    private void cargarDetallesHoja(Model model, Integer idHoja) {
        List<DetalleHojaDTO> detallesHoja = idHoja == null
                ? List.of()
                : detalleHojaService.getDetallesByHojaId(idHoja);
        model.addAttribute("detallesHoja", detallesHoja);
    }

    // Construir una respuesta de error reutilizable con un mensaje específico
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String mensaje) {
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(response);
    }

    // Obtener el ID de la bodega asociada a la sesión
    private Integer obtenerIdBodegaSesion(HttpSession session) {
        return (Integer) session.getAttribute("idBodega");
    }

    // Verificar si la hoja de producción pertenece a la bodega de la sesión
    private boolean perteneceBodegaSesion(HojaProduccionDTO hojaProduccion, HttpSession session) {
        Integer idBodegaSesion = obtenerIdBodegaSesion(session);
        return idBodegaSesion != null
                && hojaProduccion != null
                && hojaProduccion.getBodega() != null
                && idBodegaSesion.equals(hojaProduccion.getBodega().getIdBodega());
    }

    // Iniciar la vista de la hoja
    @GetMapping()
    public String inicioHoja(HttpSession session, Model model) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return "redirect:/login";
        }

        // Obtener todas las hojas de producción de la bodega asociada a la sesión y agregarlas al modelo
        Integer idBodega = (Integer) session.getAttribute("idBodega");
        List<HojaProduccionDTO> hojasProduccion = hojaProduccionService.getHojasProduccionByBodegaId(idBodega);

        // Agregar las hojas de producción al modelo para que puedan ser accedidas en la vista
        model.addAttribute("hojasProduccion", hojasProduccion);
        return "operacion/hojas";
    }

    // Iniciar la vista de creación de hoja
    @GetMapping("/crear")
    public String crearHoja(HttpSession session, Model model) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return "redirect:/login";
        }
        // Cargar el catálogo de estados de hoja y agregarlo al modelo
        List<EstadoHojaDTO> catalogoEstadoHoja = cargarCatalogoEstadoHoja(model);
        if (catalogoEstadoHoja == null) {
            // Manejar el caso en que no se encontró el catálogo de estado de hoja
            return "error/error"; // Vista genérica de error
        }

        // Agregar un objeto vacío de HojaProduccionDTO al modelo para el formulario de creación
        EstadoHojaDTO estadoInicial = hojaProduccionService.getEstadoHojaById(1);
        if (estadoInicial == null) {
            return "error/error";
        }
        // Crear un objeto HojaProduccionDTO con el estado inicial y agregarlo al modelo
        HojaProduccionDTO hojaProduccion = HojaProduccionDTO.builder()
                .estadoHoja(estadoInicial)
                .build();
        model.addAttribute("hojaProduccion", hojaProduccion);
        model.addAttribute("soloLectura", esSoloLectura(hojaProduccion));
        return "operacion/hoja-form";
    }

    // Iniciar la vista de visualización de hoja
    @GetMapping("/{id}")
    public String verHoja(HttpSession session, Model model, @PathVariable("id") Integer id) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return "redirect:/login";
        }

        // Obtener la hoja de producción por su ID
        HojaProduccionDTO hojaProduccion = hojaProduccionService.getById(id);
        if (hojaProduccion == null) {
            // Manejar el caso en que no se encontró la hoja de producción
            return "error/error"; // Vista genérica de error
        }
        // Verificar si la hoja de producción pertenece a la bodega de la sesión
        if (!perteneceBodegaSesion(hojaProduccion, session)) {
            return "error/error";
        }
        // Cargar el catálogo de estados de hoja y agregarlo al modelo
        List<EstadoHojaDTO> catalogoEstadoHoja = cargarCatalogoEstadoHoja(model);
        if (catalogoEstadoHoja == null) {
            return "error/error";
        }

        // Agregar la hoja de producción al modelo para que pueda ser accedida en la vista
        model.addAttribute("hojaProduccion", hojaProduccion);
        model.addAttribute("soloLectura", esSoloLectura(hojaProduccion));
        cargarDetallesHoja(model, hojaProduccion.getIdHoja());
        return "operacion/hoja-form";
    }
    // Bloquear una hoja de producción para indicar que está lista para surtido
    @PostMapping("/{idHoja}/bloquear")
    @ResponseBody
    public ResponseEntity<Map<String, String>> bloquearHoja(HttpSession session, @PathVariable Integer idHoja) {
        // Verificar si hay una sesión activa en el servidor
        if (!sesionService.isSesionActiva(session)) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "La sesion ha expirado.");
        }
        // Obtener el ID de la bodega asociada a la sesión
        Integer idBodega = obtenerIdBodegaSesion(session);
        if (idBodega == null) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST,
                    "La sesion no tiene una bodega seleccionada.");
        }
        try {
            // Bloquear la hoja de producción para indicar que está lista para surtido
            hojaProduccionService.bloquearHoja(idHoja, idBodega);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "La hoja quedó lista para surtido.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (IllegalStateException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible bloquear la hoja de producción.");
        }
    }

    // Obtener los productos disponibles en la bodega asociada a la sesión y enviarlos como respuesta JSON
    @GetMapping("/productos-bodega")
    @ResponseBody
    public ResponseEntity<List<ProductoDTO>> getProductosBodega(HttpSession session) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Obtener el ID de la bodega asociada a la sesión
        Integer idBodega = (Integer) session.getAttribute("idBodega");
        if (idBodega == null) {
            return ResponseEntity.badRequest().build();
        }
        // Obtener los productos disponibles en la bodega y enviarlos como respuesta JSON
        return ResponseEntity.ok(productoService.getProductosActivosByBodega(idBodega));
    }

    // Obtener los detalles de una hoja de producción específica y enviarlos como respuesta JSON
    @GetMapping("/{idHoja}/detalles")
    @ResponseBody
    public ResponseEntity<List<DetalleHojaDTO>> getDetallesHoja(HttpSession session, @PathVariable Integer idHoja) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Obtener la hoja de producción por su ID
        HojaProduccionDTO hojaProduccion = hojaProduccionService.getById(idHoja);
        if (hojaProduccion == null) {
            return ResponseEntity.notFound().build();
        }
        // Verificar si la hoja de producción pertenece a la bodega de la sesión
        if (!perteneceBodegaSesion(hojaProduccion, session)) {
            return ResponseEntity.notFound().build();
        }
        // Obtener los detalles de la hoja de producción y enviarlos como respuesta JSON
        return ResponseEntity.ok(detalleHojaService.getDetallesByHojaId(idHoja));
    }

    // Obtener un detalle de hoja de producción específico por su ID y enviarlo como respuesta JSON
    @GetMapping("/detalle/{idDetalleHoja}")
    @ResponseBody
    public ResponseEntity<DetalleHojaDTO> getDetalleHoja(HttpSession session, @PathVariable Integer idDetalleHoja) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Obtener el detalle de la hoja de producción por su ID
        DetalleHojaDTO detalleHoja = detalleHojaService.getById(idDetalleHoja);
        if (detalleHoja == null) {
            return ResponseEntity.notFound().build();
        }
        // Verificar si la hoja de producción pertenece a la bodega de la sesión
            if (!perteneceBodegaSesion(detalleHoja.getHojaProduccion(), session)) {
            return ResponseEntity.notFound().build();
        }
        // Retornar el detalle de la hoja de producción como respuesta JSON
        return ResponseEntity.ok(detalleHoja);
    }

    // Guardar o actualizar un detalle de hoja de producción
    @PostMapping("/detalle/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarDetalleHoja(HttpSession session, @RequestBody DetalleHojaDTO detalleHoja) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "La sesion ha expirado.");
        }

        try {
            // Validar que la hoja de producción esté presente en el detalle
            if (detalleHoja.getHojaProduccion() == null || detalleHoja.getHojaProduccion().getIdHoja() == null) {
                throw new IllegalArgumentException("La hoja de produccion es requerida.");
            }

            // Obtener la hoja de producción por su ID
            HojaProduccionDTO hojaProduccion = hojaProduccionService.getById(detalleHoja.getHojaProduccion().getIdHoja());
            if (hojaProduccion == null) {
                throw new IllegalArgumentException("La hoja de produccion no existe.");
            }
            // Verificar si la hoja de producción pertenece a la bodega de la sesión
            if (!perteneceBodegaSesion(hojaProduccion, session)) {
                throw new IllegalArgumentException("La hoja no pertenece a la bodega de la sesion.");
            }
            // Verificar si la hoja de producción permite cambios en sus detalles
            if (esSoloLectura(hojaProduccion)) {
                throw new IllegalArgumentException("La hoja ya no permite cambios en sus detalles.");
            }
            // Validar que el producto esté presente en el detalle
            if (detalleHoja.getProducto() == null || detalleHoja.getProducto().getIdProducto() == null) {
                throw new IllegalArgumentException("Selecciona un producto.");
            }
            // Validar que la cantidad solicitada sea mayor a cero
            if (detalleHoja.getCantidadSolicitada() == null || detalleHoja.getCantidadSolicitada() < 1) {
                throw new IllegalArgumentException("La cantidad solicitada debe ser mayor a cero.");
            }

            // Obtener el producto por su ID
            ProductoDTO producto = productoService.getProductoById(detalleHoja.getProducto().getIdProducto())
                    .orElseThrow(() -> new IllegalArgumentException("El producto seleccionado no existe."));

            // Verificar si el producto pertenece a la bodega de la sesión
            Integer idBodega = (Integer) session.getAttribute("idBodega");
            if (idBodega == null || producto.getBodega() == null || !idBodega.equals(producto.getBodega().getIdBodega())) {
                throw new IllegalArgumentException("El producto no pertenece a la bodega de la sesion.");
            }
            // Verificar si el producto está activo
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new IllegalArgumentException("El producto seleccionado esta inactivo.");
            }

            // Verificar si se está actualizando un detalle existente
            if (detalleHoja.getIdDetalleHoja() != null) {
                DetalleHojaDTO detalleActual = detalleHojaService.getById(detalleHoja.getIdDetalleHoja());
                if (detalleActual == null) {
                    throw new IllegalArgumentException("El detalle de hoja no existe.");
                }
                // Verificar si el detalle pertenece a la hoja de producción indicada
                if (detalleActual.getHojaProduccion() == null
                        || !detalleHoja.getHojaProduccion().getIdHoja().equals(detalleActual.getHojaProduccion().getIdHoja())) {
                    throw new IllegalArgumentException("El detalle no pertenece a la hoja indicada.");
                }
                // Mantener la cantidad surtida actual si ya existe
                detalleHoja.setCantidadSurtida(detalleActual.getCantidadSurtida() == null ? 0 : detalleActual.getCantidadSurtida());
            } else {
                detalleHoja.setCantidadSurtida(0);
            }
            // Verificar si el producto ya existe en los detalles de la hoja
            if (detalleHojaService.existeProductoEnHoja(
                    detalleHoja.getHojaProduccion().getIdHoja(),
                    producto.getIdProducto(),
                    detalleHoja.getIdDetalleHoja())) {
                throw new IllegalArgumentException("El producto ya existe en los detalles de esta hoja.");
            }
            // Asignar la hoja de producción y el producto al detalle antes de guardarlo
            detalleHoja.setHojaProduccion(hojaProduccion);
            detalleHoja.setProducto(producto);
            // Guardar o actualizar el detalle de hoja de producción
            detalleHojaService.addDetalleHoja(detalleHoja);

            // Construir la respuesta con un mensaje de éxito
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", detalleHoja.getIdDetalleHoja() == null
                    ? "Detalle guardado correctamente."
                    : "Detalle actualizado correctamente.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            // Construir la respuesta con un mensaje de error específico
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            // Construir la respuesta con un mensaje de error específico en caso de violación de integridad de datos
            return buildErrorResponse(HttpStatus.CONFLICT, "El producto ya existe en los detalles de esta hoja.");
        } catch (DataAccessException exception) {
            // Construir la respuesta con un mensaje de error genérico en caso de fallo en la base de datos
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible guardar el detalle.");
        }
    }

    // Eliminar un detalle de hoja de producción por su ID
    @DeleteMapping("/detalle/{idDetalleHoja}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarDetalleHoja(HttpSession session, @PathVariable Integer idDetalleHoja) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "La sesion ha expirado.");
        }

        try {
            // Obtener el detalle de hoja de producción por su ID
            DetalleHojaDTO detalleHoja = detalleHojaService.getById(idDetalleHoja);
            if (detalleHoja == null) {
                throw new IllegalArgumentException("El detalle de hoja no existe.");
            }
            // Validar que el detalle tenga una hoja de producción válida
            if (detalleHoja.getHojaProduccion() == null || detalleHoja.getHojaProduccion().getIdHoja() == null) {
                throw new IllegalArgumentException("El detalle no tiene una hoja de producción válida.");
            }
            // Obtener la hoja de producción asociada al detalle
            HojaProduccionDTO hojaProduccion = hojaProduccionService.getById(detalleHoja.getHojaProduccion().getIdHoja());
            if (hojaProduccion == null) {
                throw new IllegalArgumentException("La hoja de producción no existe.");
            }
            // Verificar si la hoja de producción pertenece a la bodega de la sesión
            if (!perteneceBodegaSesion(hojaProduccion, session)) {
                throw new IllegalArgumentException("La hoja no pertenece a la bodega de la sesion.");
            }
            // Verificar si la hoja de producción está en modo solo lectura
            if (esSoloLectura(hojaProduccion)) {
                throw new IllegalArgumentException("La hoja ya no permite cambios en sus detalles.");
            }
            // Eliminar el detalle de hoja de producción
            detalleHojaService.deleteDetalleHojaById(idDetalleHoja);

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Detalle eliminado correctamente.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible eliminar el detalle.");
        }
    }

    // Eliminar una hoja de producción por su ID
    @DeleteMapping("/{idHoja}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarHoja(HttpSession session, @PathVariable Integer idHoja) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "La sesion ha expirado.");
        }

        try {
            // Obtener la hoja de producción por su ID
            HojaProduccionDTO hojaProduccion = hojaProduccionService.getById(idHoja);
            if (hojaProduccion == null) {
                throw new IllegalArgumentException("La hoja de produccion no existe.");
            }
            // Verificar si la hoja de producción pertenece a la bodega de la sesión
            if (!perteneceBodegaSesion(hojaProduccion, session)) {
                throw new IllegalArgumentException("La hoja no pertenece a la bodega de la sesion.");
            }
            // Verificar si la hoja de producción está en un estado que permite su eliminación
            if (hojaProduccion.getEstadoHoja() == null
                    || hojaProduccion.getEstadoHoja().getIdEstadoHoja() == null
                    || hojaProduccion.getEstadoHoja().getIdEstadoHoja() != 1) {
                throw new IllegalArgumentException("Solo se pueden eliminar hojas con información procesada.");
            }

            // Eliminar la hoja de producción junto con sus detalles
            hojaProduccionService.deleteHojaProduccionConDetalles(idHoja);

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Hoja de produccion eliminada correctamente.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataAccessException exception) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible eliminar la hoja de produccion.");
        }
    }

    // Guardar o actualizar una hoja de producción
    @PostMapping()
    public String guardarHoja(HttpSession session, @ModelAttribute HojaProduccionDTO hojaProduccion, Model model, RedirectAttributes redirectAttributes) {
        // Verificar si hay una sesión activa en el servidor
        Boolean sesionActiva = sesionService.isSesionActiva(session);
        if (!sesionActiva) {
            return "redirect:/login";
        }
        // Cargar el catálogo de estados de hoja y agregarlo al modelo
        List<EstadoHojaDTO> catalogoEstadoHoja = cargarCatalogoEstadoHoja(model);
        if (catalogoEstadoHoja == null) {
            return "error/error";
        }
        // Determinar si la hoja de producción es nueva (sin ID) o existente
        boolean esNuevaHoja = hojaProduccion.getIdHoja() == null;

        try {
            Integer idBodegaSesion = obtenerIdBodegaSesion(session);
            if (idBodegaSesion == null) {
                throw new IllegalArgumentException("La sesion no tiene una bodega seleccionada.");
            }
            // Si la hoja es nueva, asignar el estado inicial; si es existente, verificar su estado actual
            if (esNuevaHoja) {
                // Asignar el estado inicial a la hoja de producción
                EstadoHojaDTO estadoInicial = hojaProduccionService.getEstadoHojaById(1);
                if (estadoInicial == null) {
                    throw new IllegalArgumentException("No existe el estado inicial de la hoja.");
                }
                hojaProduccion.setEstadoHoja(estadoInicial);
            } else {
                HojaProduccionDTO hojaActual = hojaProduccionService.getById(hojaProduccion.getIdHoja());
                if (hojaActual == null) {
                    throw new IllegalArgumentException("La hoja de producción no existe.");
                }
                // Verificar si la hoja de producción pertenece a la bodega de la sesión
                if (!perteneceBodegaSesion(hojaActual, session)) {
                    throw new IllegalArgumentException("La hoja no pertenece a la bodega de la sesion.");
                }
                // Verificar si la hoja de producción existente está en un estado que permite edición
                if (esSoloLectura(hojaActual)) {
                    redirectAttributes.addFlashAttribute("tipoAlerta", "error");
                    redirectAttributes.addFlashAttribute("mensajeAlerta", "La hoja ya no se puede editar por su estado actual.");
                    return "redirect:/hojas-produccion/" + hojaActual.getIdHoja();
                }
                hojaProduccion.setEstadoHoja(hojaActual.getEstadoHoja());
            }
            // Asignar la bodega de la sesión a la hoja de producción
            BodegaDTO bodega = BodegaDTO.builder()
                    .idBodega(idBodegaSesion)
                    .build();
            hojaProduccion.setBodega(bodega);
            // Guardar o actualizar la hoja de producción
            HojaProduccionDTO hojaGuardada = hojaProduccionService.addHojaProduccion(hojaProduccion);
            redirectAttributes.addFlashAttribute("tipoAlerta", "success");
            redirectAttributes.addFlashAttribute("mensajeAlerta",
                    esNuevaHoja ? "La hoja de producción se guardó correctamente."
                            : "La hoja de producción se actualizó correctamente.");
            return "redirect:/hojas-produccion/" + hojaGuardada.getIdHoja();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("tipoAlerta", "error");
            model.addAttribute("mensajeAlerta", exception.getMessage());
            model.addAttribute("hojaProduccion", hojaProduccion);
            model.addAttribute("soloLectura", esSoloLectura(hojaProduccion));
            cargarDetallesHoja(model, hojaProduccion.getIdHoja());
            return "operacion/hoja-form";
        } catch (DataAccessException exception) {
            model.addAttribute("tipoAlerta", "error");
            model.addAttribute("mensajeAlerta", "No fue posible guardar la hoja de producción.");
            model.addAttribute("hojaProduccion", hojaProduccion);
            model.addAttribute("soloLectura", esSoloLectura(hojaProduccion));
            // Cargar los detalles de la hoja de producción
            cargarDetallesHoja(model, hojaProduccion.getIdHoja());
            return "operacion/hoja-form";
        }
    }
}
