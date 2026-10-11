package dgtic.core.controller;

import dgtic.core.service.ConjuntoService;
import dgtic.core.service.ProductoService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConjuntoControllerTest {
    private ConjuntoService conjuntoService;
    private SesionService sesionService;
    private MockMvc mockMvc;
    private org.springframework.mock.web.MockHttpSession session;

    @BeforeEach
    void setUp() {
        conjuntoService = mock(ConjuntoService.class);
        sesionService = mock(SesionService.class);
        ConjuntoController controller = new ConjuntoController();
        ReflectionTestUtils.setField(controller, "conjuntoService", conjuntoService);
        ReflectionTestUtils.setField(controller, "productoService", mock(ProductoService.class));
        ReflectionTestUtils.setField(controller, "sesionService", sesionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("idBodega", 1);
        when(sesionService.isSesionActiva(any(HttpSession.class))).thenReturn(true);
    }

    @Test
    void eliminaConjuntoSinHistorial() throws Exception {
        doNothing().when(conjuntoService).deleteConjunto("CON-000001", 1);

        mockMvc.perform(delete("/conjunto/eliminar/CON-000001").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Conjunto eliminado correctamente."));
    }

    @Test
    void conjuntoConHistorialResponde409ConMensaje() throws Exception {
        doThrow(new IllegalStateException("No se puede eliminar el conjunto porque tiene historial de movimientos."))
                .when(conjuntoService).deleteConjunto("CON-000001", 1);

        mockMvc.perform(delete("/conjunto/eliminar/CON-000001").session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar el conjunto porque tiene historial de movimientos."));
    }
}
