package dgtic.core.controller;

import dgtic.core.exception.ReporteNoAutorizadoException;
import dgtic.core.exception.ReporteRecursoNoEncontradoException;
import dgtic.core.model.dto.reporte.ReporteConjuntoDTO;
import dgtic.core.model.dto.reporte.ReporteContenedorDTO;
import dgtic.core.model.dto.reporte.ReporteHojaDTO;
import dgtic.core.model.dto.reporte.ReporteMovimientoDTO;
import dgtic.core.model.dto.reporte.ReporteOperacionHojaDTO;
import dgtic.core.service.ReporteService;
import dgtic.core.service.SesionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReporteControllerTest {
    private ReporteService reporteService;
    private SesionService sesionService;
    private MockMvc mockMvc;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        reporteService = org.mockito.Mockito.mock(ReporteService.class);
        sesionService = org.mockito.Mockito.mock(SesionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ReporteController(reporteService, sesionService)).build();
        session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("idUsuario", 1);
        session.setAttribute("idBodega", 2);
        when(sesionService.isSesionActiva(any(HttpSession.class))).thenReturn(true);
    }

    @Test
    void hojasRespondeConOk() throws Exception {
        when(reporteService.buscarHojas(eq(1), eq(2), any())).thenReturn(List.of(ReporteHojaDTO.builder().build()));

        mockMvc.perform(get("/reportes/api/hojas").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void movimientosRespondeConOk() throws Exception {
        when(reporteService.buscarMovimientos(eq(1), eq(2), any())).thenReturn(List.of(ReporteMovimientoDTO.builder().build()));

        mockMvc.perform(get("/reportes/api/movimientos").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void contenedoresRespondeConOk() throws Exception {
        when(reporteService.buscarContenedores(eq(1), eq(2), any())).thenReturn(List.of(ReporteContenedorDTO.builder().build()));

        mockMvc.perform(get("/reportes/api/contenedores").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void conjuntosRespondeConOk() throws Exception {
        when(reporteService.buscarInventario(eq(1), eq(2), any())).thenReturn(List.of(ReporteConjuntoDTO.builder().build()));

        mockMvc.perform(get("/reportes/api/conjuntos").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void surtidoRespondeConOk() throws Exception {
        when(reporteService.obtenerSurtido(1, 2, 7)).thenReturn(ReporteOperacionHojaDTO.builder().build());

        mockMvc.perform(get("/reportes/api/hojas/7/surtido").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void recepcionRespondeConOk() throws Exception {
        when(reporteService.obtenerRecepcion(1, 2, 7)).thenReturn(ReporteOperacionHojaDTO.builder().build());

        mockMvc.perform(get("/reportes/api/hojas/7/recepcion").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk());
    }

    @Test
    void sesionAusenteResponde401YSinConsultarServicio() throws Exception {
        when(sesionService.isSesionActiva(any(HttpSession.class))).thenReturn(false);

        mockMvc.perform(get("/reportes/api/hojas"))
                .andExpect(status().isUnauthorized());

        verify(reporteService, never()).buscarHojas(any(), any(), any());
    }

    @Test
    void accesoDenegadoResponde403() throws Exception {
        when(reporteService.buscarHojas(eq(1), eq(2), any()))
                .thenThrow(new ReporteNoAutorizadoException("denegado"));

        mockMvc.perform(get("/reportes/api/hojas").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isForbidden());
    }

    @Test
    void hojaNoEncontradaResponde404() throws Exception {
        when(reporteService.obtenerSurtido(1, 2, 99))
                .thenThrow(new ReporteRecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/reportes/api/hojas/99/surtido")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtroInvalidoResponde400() throws Exception {
        when(reporteService.buscarHojas(eq(1), eq(2), any()))
                .thenThrow(new IllegalArgumentException("rango inválido"));

        mockMvc.perform(get("/reportes/api/hojas")
                        .param("fechaDesde", "2026-02-10")
                        .param("fechaHasta", "2026-02-01")
                        .session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isBadRequest());
    }
}
