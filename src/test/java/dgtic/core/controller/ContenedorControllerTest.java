package dgtic.core.controller;

import dgtic.core.service.ContenedorService;
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

class ContenedorControllerTest {
    private ContenedorService contenedorService;
    private SesionService sesionService;
    private MockMvc mockMvc;
    private org.springframework.mock.web.MockHttpSession session;

    @BeforeEach
    void setUp() {
        contenedorService = mock(ContenedorService.class);
        sesionService = mock(SesionService.class);
        ContenedorController controller = new ContenedorController();
        ReflectionTestUtils.setField(controller, "contenedorService", contenedorService);
        ReflectionTestUtils.setField(controller, "sesionService", sesionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        session = new org.springframework.mock.web.MockHttpSession();
        when(sesionService.isSesionActiva(any(HttpSession.class))).thenReturn(true);
    }

    @Test
    void eliminaContenedorSinHistorial() throws Exception {
        doNothing().when(contenedorService).deleteContenedor(1);

        mockMvc.perform(delete("/contenedor/eliminar/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contenedor eliminado correctamente."));
    }

    @Test
    void contenedorConHistorialResponde409ConMensaje() throws Exception {
        doThrow(new IllegalStateException("No se puede eliminar el contenedor porque tiene historial de utilizaciones."))
                .when(contenedorService).deleteContenedor(1);

        mockMvc.perform(delete("/contenedor/eliminar/1").session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar el contenedor porque tiene historial de utilizaciones."));
    }
}
