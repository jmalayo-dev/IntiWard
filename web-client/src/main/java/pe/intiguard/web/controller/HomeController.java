package pe.intiguard.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.intiguard.web.client.CatalogoApi;

/**
 * PENDIENTE por integrante (semana 6), cada uno en su propio controller y carpeta de templates:
 *  - 2: /login, /registro, /perfil, /admin/usuarios
 *  - 3: /catalogo, /catalogo/{id}, /admin/productos
 *  - 4: /carrito, /checkout, /mis-pedidos, /admin/ventas
 *  - 5: /admin/stock, /comprobantes, /comprobantes/{id}
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CatalogoApi catalogoApi;

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("productos", catalogoApi.listarProductos());
        return "index";
    }
}
