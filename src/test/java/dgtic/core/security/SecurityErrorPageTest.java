package dgtic.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityErrorPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pagina403PersonalizadaConservaCodigoHttp() throws Exception {
        mockMvc.perform(get("/error/403"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Acceso denegado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("403")));
    }

    @Test
    void pagina404PersonalizadaConservaCodigoHttp() throws Exception {
        mockMvc.perform(get("/error/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Página no encontrada")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("404")));
    }
}
