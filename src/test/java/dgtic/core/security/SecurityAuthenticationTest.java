package dgtic.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void refreshSinCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshSinTokenConCsrfRespondeNoAutorizado() throws Exception {
        mockMvc.perform(post("/refresh").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Refresh Token inválido"));
    }
}
