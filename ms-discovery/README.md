# ms-discovery · Service Registry (Eureka)

| | |
|---|---|
| **Responsable** | Integrante 1 · Mathias |
| **Puerto** | 8761 |
| **Panel** | http://localhost:8761 |
| **Base de datos** | Ninguna |
| **Patrón** | Service Registry y Discovery |

## Para qué existe

Es la "guía telefónica" del sistema. Cada servicio, al arrancar, se registra aquí con su nombre (`ms-catalogo`, `ms-pedidos`…) y su IP. Cuando alguien necesita hablar con otro servicio, pregunta por el **nombre** y Eureka le devuelve dónde está. Por eso nadie escribe IPs en el código.

```
ms-pedidos ──"¿dónde está ms-catalogo?"──▶ ms-discovery ──"172.18.0.5:8082"──▶ ms-pedidos
```

Lo usan dos clientes:
- **ms-gateway**: las rutas `lb://ms-catalogo` se resuelven con Eureka.
- **ms-pedidos**: los clientes Feign `@FeignClient(name = "ms-catalogo")` se resuelven con Eureka.

## Estado actual ✅ (funcional)

| Archivo | Qué hace |
|---|---|
| `DiscoveryApplication.java` | `@EnableEurekaServer` |
| `application.yml` | No se registra a sí mismo; self-preservation apagado para desarrollo |
| `Dockerfile` | Imagen de dos etapas con healthcheck sobre `/actuator/health` |

Al levantar `docker compose up`, el panel muestra registrados a los 6 clientes: MS-AUTH, MS-CATALOGO, MS-INVENTARIO, MS-PEDIDOS, MS-COMPROBANTES y MS-GATEWAY.

## Pendientes

- [ ] **Semana 8**: en el perfil `cloud`, volver a activar `enable-self-preservation: true` (en producción es lo correcto).
- [ ] **Semana 8**: no exponer el puerto 8761 al mundo en la EC2: solo la red interna de Docker (quitar `ports` en el compose de nube o cerrarlo en el security group).
- [ ] (Opcional) Proteger el panel con usuario y contraseña.

## Cómo correrlo

```bash
# Solo este servicio, desde la raíz del repo
./mvnw -pl ms-discovery spring-boot:run

# O todo el sistema
docker compose up -d --build
```

## Problemas típicos

| Síntoma | Causa | Solución |
|---|---|---|
| Un servicio no aparece en el panel | Arrancó antes que Eureka o tiene mal `EUREKA_HOST` | En Docker es `ms-discovery`; en local, `localhost` (valor por defecto) |
| El gateway responde 503 justo después de arrancar | El gateway aún no refresca su caché de Eureka (cada 30 s) | Esperar unos 30–60 s |
| Instancias "fantasma" en el panel | Contenedores reiniciados | Se limpian solas en ~90 s (self-preservation está apagado) |

## Qué decir en la sustentación

> "Los servicios no se conocen por IP sino por nombre. ms-discovery es un servidor Eureka; cada servicio se registra al arrancar y el gateway y Feign le preguntan dónde está el otro. Si mañana levantamos dos instancias de catálogo, el gateway reparte la carga sin cambiar una línea."
