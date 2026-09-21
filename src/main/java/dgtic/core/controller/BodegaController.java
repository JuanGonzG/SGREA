package dgtic.core.controller;

import dgtic.core.model.dto.BodegaDTO;
import dgtic.core.service.BodegaService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BodegaController {
    @Autowired
    private BodegaService bodegaService;

    @Autowired
    private SesionService sesionService;

    // Mostrar la página de inicio de bodegas
    @GetMapping("/bodega")
    public String inicioBodega(HttpSession session, Model model) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return "redirect:/login";
        }
        model.addAttribute("bodegas", bodegaService.getBodegas());
        return "catalogos/bodegas";
    }

    // Endpoint para obtener una bodega por su ID
    @GetMapping("/bodega/get/{idBodega}")
    @ResponseBody
    public ResponseEntity<BodegaDTO> getBodega(HttpSession session, @PathVariable Integer idBodega) {
        // Verificar si la sesión está activa
        if (!sesionService.isSesionActiva(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return bodegaService.getBodegaById(idBodega)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint para guardar una bodega
    @PostMapping("/bodega/guardar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> guardarBodega(HttpSession session, @RequestBody BodegaDTO bodega) {
        Map<String, String> response = new HashMap<>();
        try {
            // Verificar si la sesión está activa
            if (!sesionService.isSesionActiva(session)) {
                response.put("mensaje", "La sesión ha expirado.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            bodegaService.save(bodega);
            response.put("mensaje", "Bodega guardada correctamente.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            response.put("mensaje", exception.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Endpoint para eliminar una bodega
    @DeleteMapping("/bodega/eliminar/{idBodega}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminarBodega(HttpSession session, @PathVariable Integer idBodega) {
        Map<String, String> response = new HashMap<>();
        try {
            // Verificar si la sesión está activa
            if (!sesionService.isSesionActiva(session)) {
                response.put("mensaje", "La sesión ha expirado.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            bodegaService.delete(idBodega);
            response.put("mensaje", "Bodega eliminada correctamente.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            response.put("mensaje", exception.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException exception) {
            response.put("mensaje", exception.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
}
