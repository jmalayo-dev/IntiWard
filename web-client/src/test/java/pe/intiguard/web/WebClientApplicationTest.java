package pe.intiguard.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** El gateway no está levantado en la prueba: la página igual debe cargar el contexto. */
@SpringBootTest
class WebClientApplicationTest {

    @Test
    void contextLoads() {
    }
}
