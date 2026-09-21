package dgtic.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.productos.upload-dir}")
    private String productosUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = Path.of(productosUploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }

        registry.addResourceHandler("/uploads/productos/**")
                .addResourceLocations(ubicacion);
    }
}
