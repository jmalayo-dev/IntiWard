# Plan de ejecución — IntiGuard v2

Oct 9, 2026 · @Mathias Alayo

IntiGuard pasa de un monolito ASP.NET MVC a cinco microservicios de negocio en Spring Boot, más gateway y service discovery: cada uno de los cinco integrantes es dueño de un servicio, todo vive en un mono-repo de GitHub y se despliega en AWS. El plan cubre ocho semanas y se ajusta a la fecha de entrega real del curso.

## Decisiones

El giro de negocio no cambia: e-commerce de equipos de seguridad electrónica (cámaras, alarmas, sensores de humo, cerraduras digitales y control de acceso). Lo que cambia es la tecnología y el corte en servicios.

| Pregunta | Decisión | Por qué |
| --- | --- | --- |
| ¿Cuántos microservicios? | 5 de negocio — auth, catálogo, inventario, pedidos, comprobantes — más gateway y service discovery | El mínimo es 4. Con 5, cada integrante es dueño de uno y el corte sigue al modelo de datos del informe: usuario/rol, producto, stock, venta/detalle\_venta, comprobante |
| ¿Qué tecnologías? | Java 21, Spring Boot 3.x, Spring Cloud (Gateway, Eureka, OpenFeign), Spring Data JPA, Spring Security con JWT, Lombok, MySQL 8, JUnit 5 con Mockito, Postman, Docker | Todo sale de la ficha obligatoria. Spring Cloud es lo que convierte “microservicios separados” en algo que arranca y se descubre solo |
| ¿Qué arquitectura? | Microservicios con API Gateway y registro de servicios; cada servicio en capas (controller → service → repository) y con su propio schema MySQL | Cumple “arquitectura en capas (MVC)”, “cliente-servidor” y “microservicios separados” al mismo tiempo, sin inventar nada fuera de la ficha |
| ¿Cómo repartimos la carga? | Mono-repo, una carpeta por servicio, un dueño por carpeta declarado en CODEOWNERS | La entrega exige “código y desarrollos en una carpeta”. El dueño por carpeta hace que los conflictos de merge casi desaparezcan entre 5 personas |
| ¿Qué nube? | EC2 con Docker Compose, RDS MySQL y S3 para las imágenes de producto | Es la ruta más barata que aún demuestra despliegue y configuración en la nube. ECS o EKS multiplican el costo sin sumar nota |
| ¿Cliente que consume la API? | Sí: una aplicación web aparte en Spring Boot con Thymeleaf | La ficha lo marca “en duda”, pero el informe original es un e-commerce con pantallas y la demo necesita verse. Además deja explícito el punto “cliente-servidor” |

## Arquitectura

&#91;embedded content: Arquitectura IntiGuard v2 · 8 servicios y el orden del checkout\]

El cliente web y Postman conocen una sola URL, la del gateway. El gateway valida el JWT una vez y reenvía la petición al servicio que corresponde. De ahí en adelante ningún servicio entra a la base de datos de otro: le pregunta por HTTP, y Eureka le dice dónde está.

El checkout es el único punto donde se encadenan cuatro servicios, y por eso es donde va el circuit breaker de Resilience4j. Si ms-comprobantes no responde en dos segundos, ms-pedidos libera la reserva de stock y devuelve un error claro, en vez de dejar una venta a medias con el stock descontado.

## Los servicios

Ocho aplicaciones Spring Boot: cinco de negocio, dos de infraestructura y el cliente web. Cada servicio de negocio tiene su propio schema y nadie entra al schema de otro.

| Servicio | Puerto | Entidades | Schema MySQL | Endpoints clave |
| --- | --- | --- | --- | --- |
| ms-discovery | 8761 | — | — | Servidor Eureka. No expone API de negocio |
| ms-gateway | 8080 | — | — | Única puerta de entrada. Enruta por prefijo, valida el JWT y propaga usuario y rol |
| ms-auth | 8081 | usuario, rol | db\_auth | `POST /auth/registro` · `POST /auth/login` · `GET POST PUT DELETE /usuarios` |
| ms-catalogo | 8082 | producto, marca, categoria | db\_catalogo | `GET /productos` paginado y con filtros · `GET /productos/{id}` · `POST PUT DELETE /productos` |
| ms-inventario | 8083 | stock, movimiento\_stock | db\_inventario | `GET /stock/{idProducto}` · `POST /stock/reservar` · `POST /stock/confirmar` · `POST /stock/liberar` · `GET /movimientos` |
| ms-pedidos | 8084 | carrito, item\_carrito, venta, detalle\_venta | db\_pedidos | `GET POST PUT DELETE /carrito/items` · `POST /pedidos/checkout` · `GET /pedidos` · `GET /pedidos/{id}` |
| ms-comprobantes | 8085 | comprobante, serie\_correlativo | db\_comprobantes | `POST /comprobantes` interno · `GET /comprobantes` · `GET /comprobantes/{id}` · `GET /comprobantes/{id}/pdf` |
| web-client | 8090 | — | — | Pantallas Thymeleaf. Consume la API solo a través del gateway |

### Qué cambia respecto al modelo del informe

El diagrama entidad-relación del informe tiene seis tablas: producto, venta, detalle\_venta, comprobante, usuario y rol. Al partir en servicios aparecen tres cosas nuevas.

- **`producto.stock` se parte en dos.** Catálogo guarda los datos comerciales (nombre, marca, precio, imagen) e inventario guarda la cantidad y su historial de movimientos. Esa separación es lo que hace medible el Objetivo 2 del informe, reducir errores logísticos en 30 %: sin tabla de movimientos no hay nada que auditar.
- **`producto.marca` deja de ser texto libre** y pasa a tabla `marca`, más `categoria` para filtrar el catálogo. En el informe la marca está escrita a mano en cada fila (Dahua, Honeywell, Samsung, Nest, Sony).
- **Aparece el carrito en base de datos.** En el informe el carrito vive en la sesión del servidor; con servicios separados y sin estado, tiene que persistirse para que sobreviva a un reinicio o a un cambio de instancia.
- **`comprobante.numero` pasa a serie más correlativo** (`B001-00000001` para boleta, `F001-00000001` para factura). En las capturas del informe conviven números como `B0001` y `C-8618141792`, señal de que la numeración se generaba de dos maneras distintas.

## Cómo se cubre el stack obligatorio

Cada fila de la ficha técnica tiene un lugar concreto donde se implementa. Si una fila no tiene dueño, no está cubierta.

| Requisito de la ficha | Cómo se cumple | Dónde |
| --- | --- | --- |
| Servicios REST GET, POST, PUT y DELETE | CRUD completo en productos, usuarios, ítems del carrito y stock | Los 5 servicios de negocio |
| Spring Boot API RESTful | Spring Boot 3.x con `@RestController` y respuestas `ResponseEntity` | Los 5 servicios |
| Spring Data | Interfaces `JpaRepository`, métodos derivados y `@Query` donde haga falta | Los 5 servicios |
| Spring MVC | `@RestController` y `@RequestMapping` en la API; `@Controller` más Thymeleaf en el cliente | Servicios y web-client |
| Spring Security | Filtro JWT por servicio y `@PreAuthorize` por rol en los endpoints de administración | ms-auth, ms-gateway y los demás |
| Lombok | `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` | Todos |
| Arquitectura en capas (MVC) | controller → service → repository → entity, con DTO y mapper entre capas | Todos |
| Arquitectura cliente-servidor | El cliente web no toca ningún servicio directamente: todo pasa por el gateway | web-client y ms-gateway |
| Pruebas con Postman | Una colección por servicio más una de flujo completo, con environment local y environment de nube | Carpeta `/postman` |
| Buenas prácticas y patrones de diseño | Ver la lista de abajo; cada patrón se nombra en el README con el archivo donde está | Todos |
| Despliegue en la nube | EC2 con Docker Compose, RDS MySQL y S3 | Sección de despliegue |
| Configuración para servicios y consumo en la nube | Perfiles `local` y `cloud`, cero credenciales en el código, secretos en Parameter Store | Todos |
| Base de datos MySQL | MySQL 8, un schema por servicio en una sola instancia | RDS |
| Código y desarrollos en una carpeta | Mono-repo con una carpeta por servicio | GitHub |
| Servicio REST de login | `POST /auth/login` devuelve un JWT con el rol dentro | ms-auth |
| Password cifrado con BCrypt | `BCryptPasswordEncoder` con fuerza 10; la contraseña nunca viaja de vuelta ni se registra en el log | ms-auth |
| Pruebas de la capa de datos (JUnit) | `@DataJpaTest` sobre los repositorios, más JUnit 5 y Mockito en la capa de servicio | Todos |
| Validaciones en el login | Ver el detalle de abajo | ms-auth |
| Microservicios separados | Cinco procesos independientes, con base de datos propia y despliegue propio | Todos |
| Consumo de la API por un cliente | Cliente web Thymeleaf más las colecciones de Postman | web-client |

### Patrones que vamos a poder nombrar en la sustentación

La ficha pide “buenas prácticas y patrones de diseño” sin decir cuáles. Estos salen solos de la arquitectura elegida, así que conviene tenerlos escritos con el archivo donde viven.

| Patrón | Dónde se ve |
| --- | --- |
| API Gateway | ms-gateway: una sola URL pública para ocho servicios |
| Service Registry y Discovery | ms-discovery: los servicios se encuentran por nombre, no por IP |
| Database per Service | Un schema por servicio, sin joins entre ellos |
| DTO y Mapper | La entidad JPA nunca sale en una respuesta HTTP |
| Repository | Interfaces de Spring Data JPA |
| Builder | `@Builder` de Lombok al construir entidades y DTO |
| Circuit Breaker | Resilience4j en los clientes Feign del checkout |
| Externalized Configuration | Perfiles de Spring y variables de entorno |
| Saga por compensación | El checkout reserva stock, y si falla después, lo libera |
| Manejo global de errores | `@RestControllerAdvice` con un formato único de error |

### Validaciones en el login

Esta fila está marcada “obligatorio para nota máxima”, así que vale la pena pasarse de prolijo.

- Formato de correo y longitud mínima de contraseña con Bean Validation: `@Email`, `@NotBlank`, `@Size(min = 8)`.
- Mensaje genérico “Credenciales inválidas” tanto si el correo no existe como si la contraseña está mal. Decir cuál de los dos falló le regala al atacante la lista de correos registrados.
- Contador de intentos fallidos y bloqueo temporal: cinco intentos seguidos bloquean la cuenta quince minutos.
- Rechazo del login si la cuenta está inactiva. La columna `activo` ya existe en el modelo del informe y hoy no se usa para nada.
- JWT con expiración corta, una hora, y el rol dentro del claim para que el gateway no tenga que consultar a ms-auth en cada petición.
- Códigos de respuesta sin pistas: `401` para credenciales inválidas, `423` para cuenta bloqueada.

## Reparto entre los 5 integrantes

Un servicio por persona, de punta a punta. Nadie comparte un servicio con otro, porque un servicio compartido es un servicio que nadie termina.

| Integrante | Servicio propio | Tarea transversal | Pantallas del cliente web |
| --- | --- | --- | --- |
| 1 — Mathias | ms-gateway y ms-discovery | Dueño del repo: ramas, CODEOWNERS, CI, `docker-compose.yml`, POM padre, integración semanal y despliegue | Layout base, menú, manejo de sesión |
| 2 | ms-auth | Librería común de seguridad (filtro JWT) que los otros cuatro importan | Registro, login, perfil, gestión de usuarios |
| 3 | ms-catalogo | Datos semilla: 200 productos con marcas y categorías, y la carga de imágenes a S3 | Inicio, catálogo, detalle de producto, CRUD de productos |
| 4 | ms-pedidos | Clientes Feign y Resilience4j; pruebas de integración del checkout | Carrito, confirmación de compra, mis pedidos, listado de ventas |
| 5 | ms-inventario y ms-comprobantes | Generación del PDF del comprobante y reporte de ventas | Stock, listado y detalle de comprobantes |

El integrante 5 lleva dos servicios porque ms-inventario es el más pequeño del conjunto: dos tablas y cinco endpoints, sin lógica de negocio complicada. El integrante 1 lleva los dos servicios con menos código, pero carga con la integración y el despliegue, que es donde se pierde más tiempo cuando algo no encaja.

### Reglas de reparto

- **De punta a punta.** Cada integrante entrega su servicio completo: entidad, repositorio, servicio, controlador, DTO, validaciones, pruebas JUnit, colección de Postman y Dockerfile.
- **El cliente web se reparte por servicio.** Quien hace ms-catalogo hace las pantallas del catálogo. Así nadie queda de cuello de botella haciendo todo el front solo.
- **Los archivos compartidos los toca una sola persona.** POM padre, `docker-compose.yml`, el workflow de CI y el README son del integrante 1. Si otro los necesita, lo pide por el grupo en vez de editarlos.
- **Un PR necesita la aprobación de otro integrante** antes de entrar. Revisar el código de un compañero toma diez minutos y es la única forma de que los cinco entiendan el sistema completo para la sustentación.

## Organización en GitHub

Un solo repositorio con una carpeta por servicio. La entrega pide “código y desarrollos en una carpeta”, y para cinco personas un mono-repo es además mucho más fácil de levantar que ocho repositorios sueltos.

```plain
intiguard/
├─ pom.xml                      POM padre: versiones de Spring Boot y Spring Cloud en un solo lugar
├─ docker-compose.yml           Levanta los 8 servicios + MySQL con un comando
├─ .gitignore
├─ .github/
│  ├─ workflows/ci.yml
│  ├─ CODEOWNERS
│  └─ pull_request_template.md
├─ infra/                       init.sql, nginx.conf, scripts de despliegue
├─ ms-discovery/
├─ ms-gateway/
├─ ms-auth/
├─ ms-catalogo/
├─ ms-inventario/
├─ ms-pedidos/
├─ ms-comprobantes/
├─ web-client/
└─ postman/                     Colecciones y environments
```

### Ramas

| Rama | Qué es | Regla |
| --- | --- | --- |
| `main` | Lo que se entrega y lo que está desplegado | Protegida. Solo entra por PR desde `develop`, con CI en verde y una aprobación |
| `develop` | Integración del equipo | Protegida. Solo entra por PR desde una rama de trabajo |
| `feature/<servicio>/<tarea>` | Trabajo diario | Se borra al mergear. Ejemplo: `feature/catalogo/crud-productos` |
| `fix/<servicio>/<tarea>` | Correcciones | Igual |

Proteger `main` se configura en Settings → Branches → Add rule: exigir pull request, exigir que el check de CI pase, y exigir una aprobación. Sin esa regla, alguien va a hacer push directo a `main` la noche antes de la entrega.

### Commits

Conventional Commits con el servicio como ámbito, para poder leer el historial y saber de qué parte se habla.

```text
feat(catalogo): endpoint de búsqueda por categoría
fix(pedidos): liberar stock cuando falla la emisión del comprobante
test(auth): pruebas de la capa de datos del repositorio de usuarios
chore(infra): fijar la versión de la imagen de MySQL
```

### CODEOWNERS

Este archivo hace que GitHub pida automáticamente la revisión del dueño de cada carpeta. Es la pieza que convierte el reparto en algo que se cumple solo.

```text
/ms-gateway/          @mathias
/ms-discovery/        @mathias
/ms-auth/             @integrante2
/ms-catalogo/         @integrante3
/ms-pedidos/          @integrante4
/ms-inventario/       @integrante5
/ms-comprobantes/     @integrante5
/pom.xml              @mathias
/docker-compose.yml   @mathias
/.github/             @mathias
```

Como cada quien escribe dentro de su propia carpeta, los conflictos de merge quedan reducidos a los tres o cuatro archivos compartidos, que tienen un solo dueño.

### Integración continua

```yaml
# .github/workflows/ci.yml (resumen)
on:
  pull_request:
    branches: [develop, main]
jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        servicio: [ms-auth, ms-catalogo, ms-inventario, ms-pedidos,
                   ms-comprobantes, ms-gateway, ms-discovery, web-client]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: mvn -B -pl ${{ matrix.servicio }} -am verify
```

Compilar los ocho en cada PR cuesta unos minutos y detecta cuando un cambio en un DTO compartido rompe a otro servicio. Más adelante se puede filtrar por carpeta modificada, pero al inicio conviene lo contrario.

### Tablero y seguimiento

- Un issue por grupo de endpoints o por pantalla, con etiqueta del servicio (`ms-catalogo`) y milestone igual a la semana del cronograma.
- Tablero de GitHub Projects con cuatro columnas: Backlog, En curso, En revisión, Hecho.
- Plantilla de PR con tres preguntas: qué cambia, cómo se prueba, a quién afecta.
- `.gitignore` con `target/`, `*.env` y cualquier `application-*-local.yml`. Un secreto subido al repo queda en el historial aunque después se borre el archivo.

## Despliegue en la nube

Una instancia EC2 corriendo los ocho contenedores con Docker Compose, con la base de datos en RDS y las imágenes en S3. Es la ruta más barata que aún demuestra despliegue real y configuración para la nube, que son dos filas distintas de la ficha.

Cuatro puntos del informe aterrizan aquí: el alcance habla de 200 productos y 500 pedidos simultáneos, las recomendaciones piden “implementar servidores en la nube”, el glosario menciona SSL/TLS, y la bibliografía cita la guía de OWASP. Hoy ninguno de los cuatro tiene implementación.

### Qué va en cada capa

| Capa | Servicio | Para qué | Costo aproximado |
| --- | --- | --- | --- |
| Cómputo | EC2 `t3.medium` con Docker Compose | Corre los 8 contenedores. Ocho JVM piden alrededor de 4 GB, así que `t3.small` con 2 GB queda muy justo | \~30 USD/mes |
| Base de datos | RDS MySQL 8, `db.t4g.micro`, 20 GB gp3 | Los cinco schemas en una sola instancia. Trae backups automáticos y restauración a un punto en el tiempo | \~15 USD/mes |
| Imágenes de producto | S3 más CloudFront | Reemplaza las `imagen_url` del informe, que hoy apuntan a Pixabay y a rutas locales como `/assets/img/portfolio/1.jpg` | Centavos |
| HTTPS | Nginx en la misma EC2 con certificado de Let's Encrypt | Cubre el punto SSL/TLS del glosario. ACM no sirve directo sobre EC2: necesitaría un balanceador, que añade unos 16 USD/mes | Gratis |
| DNS | Route 53 | Un dominio en vez de una IP para la sustentación | \~0,50 USD/mes más el dominio |
| Secretos | SSM Parameter Store, nivel estándar | Clave del JWT y contraseña de RDS fuera del repositorio | Gratis |
| Logs | CloudWatch Logs con el driver `awslogs` de Docker | Ver por qué se cayó un servicio sin entrar por SSH | Centavos |
| Control de costo | AWS Budgets con alerta por correo | Avisa antes de que la tarjeta duela | Gratis |

Los precios son de referencia y hay que confirmarlos en la calculadora de AWS: cambian, y varían bastante por región. Si la cuenta es nueva, parte de esto puede caer en el nivel gratuito, pero AWS cambió las condiciones del free tier en los últimos años, así que conviénelo verificar al crear la cuenta en vez de darlo por hecho. Sobre la región: São Paulo es la más cercana a Lima y también la más cara; Virginia del Norte sale bastante más barata y la diferencia de latencia no se nota en una demo.

### Configuración para la nube

Esta es la fila “configuración para servicios y consumo en la nube” de la ficha. La idea es que el mismo código y el mismo `docker-compose.yml` sirvan en la laptop y en la EC2, y que lo único que cambie sea de dónde salen las variables.

```yaml
# ms-catalogo/src/main/resources/application.yml
spring:
  application:
    name: ms-catalogo
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_HOST:ms-discovery}:8761/eureka/
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- **Ninguna credencial en el código.** Todo entra por variable de entorno. En local el `docker-compose.yml` las define en texto plano contra una base de prueba; en la nube las lee de Parameter Store al arrancar.
- **`ddl-auto: validate`, no `update`.** El schema se crea desde `infra/init.sql`, que está versionado y se revisa en un PR. Dejar que Hibernate altere tablas solo en producción es cómo se pierden datos sin saber cuándo.
- **Los servicios se llaman por nombre, no por IP.** `ms-pedidos` pide `http://ms-catalogo/productos/7` y Eureka resuelve. Dentro de la red de Docker funciona igual en ambos entornos.
- **Actuator expone `/actuator/health`** en los ocho. Es lo que usan Docker, Eureka y ustedes mismos para saber qué está vivo.

### Pasos del despliegue

1. El CI compila y prueba los ocho módulos en cada PR con `mvn -B verify`.
2. Cada servicio tiene un Dockerfile de dos etapas: `maven:3.9-eclipse-temurin-21` para compilar y `eclipse-temurin:21-jre-alpine` para correr. La imagen final queda alrededor de 200 MB en vez de pasar de 800 MB.
3. En cada merge a `main`, el CI construye las ocho imágenes y las publica en Docker Hub etiquetadas con el commit.
4. En la EC2: `docker compose pull && docker compose up -d`. Al inicio háganlo a mano por SSH; automatizarlo con SSM Run Command es trabajo de la última semana, no de la primera.
5. Verificación: `/actuator/health` de los ocho servicios y la colección de Postman apuntando al environment de nube.

El orden de arranque importa: MySQL, luego ms-discovery, luego auth, catálogo, inventario y comprobantes, luego pedidos, luego gateway y por último el cliente web. Se resuelve con `healthcheck` sobre `/actuator/health` y `depends_on` con `condition: service_healthy`. Sin eso, los servicios arrancan antes que Eureka, fallan el registro y hay que reiniciar a mano.

### Por qué no ECS, EKS ni Lambda

- **ECS Fargate** cobra CPU y memoria reservadas por tarea. Ocho tareas salen varias veces más caras que una EC2 que las corre todas juntas.
- **EKS** tiene costo fijo por hora del plano de control, aparte de los nodos. No se justifica para un proyecto de curso.
- **Lambda** no encaja: una JVM de Spring Boot tarda segundos en arrancar, los arranques en frío harían lenta la sustentación y un registro de servicios no tiene sentido ahí.

La ventaja real de la EC2 es que corre el mismo `docker-compose.yml` que ustedes tienen en la laptop, solo que con otro perfil. Eso reduce el “en mi máquina funciona” casi a cero, que con cinco personas es el riesgo más caro del proyecto.

### Si el costo es un problema

- **Oracle Cloud Always Free** da una instancia ARM de hasta 4 OCPU y 24 GB de forma permanente, que aguanta los ocho contenedores de sobra. El inconveniente conocido es que la capacidad ARM suele estar agotada y hay que insistir varios días para conseguirla. Conviénelo verificar antes de apostar el despliegue a eso.
- **Azure for Students** entrega crédito sin pedir tarjeta si alguno del equipo tiene correo institucional. Vale la pena revisar si Cibertec lo habilita.
- **Render o Railway** tienen nivel gratuito, pero los servicios duermen por inactividad. Con ocho servicios que se buscan entre sí, es inviable.

### Seguridad mínima del despliegue

El informe cita OWASP en la bibliografía y pone “ciberseguridad” entre las recomendaciones. Esto es lo mínimo para que esa mención tenga respaldo.

- Security group con 80 y 443 abiertos al mundo, y 22 solo desde las IP del equipo.
- RDS en subred privada, sin acceso público, alcanzable solo desde el security group de la EC2.
- Un usuario de MySQL por servicio, con permisos solo sobre su schema. Nada de `root` compartido.
- Los contenedores corren con usuario no root por dentro.
- La clave del JWT se genera al azar y vive en Parameter Store, no en `application.yml`.

## Cronograma

&#91;embedded content: Cronograma de 8 semanas · un carril por integrante\]

Las semanas son relativas, no fechas: anclen la semana 1 al lunes en que arranquen y ajusten el final a la fecha real de entrega del curso.

| Semana | Qué se construye | Qué tiene que existir al cerrarla |
| --- | --- | --- |
| 1 | Sprint 0 entre los cinco: POM padre, ms-discovery, ms-gateway, `docker-compose.yml`, MySQL con los cinco schemas, repo con ramas protegidas y CI | `docker compose up` levanta Eureka y el gateway, y los dos se ven en el panel de Eureka |
| 2 | Cada uno arranca su servicio: entidad, repositorio, servicio, controlador y un CRUD que responda | Cada servicio responde un `GET` y aparece registrado en Eureka |
| 3 | CRUD completo con DTO, validaciones y manejo de errores. ms-auth entrega el login con JWT | El gateway enruta a los cinco y el login devuelve un token que los demás aceptan |
| 4 | ms-pedidos arma el checkout y llama a catálogo, inventario y comprobantes con Feign | Una compra completa de punta a punta desde Postman |
| 5 | Compensación y circuit breaker; semillas de 200 productos; PDF del comprobante | El checkout falla bien: si no hay stock o no se emite el comprobante, no queda venta a medias |
| 6 | Cliente web: cada uno hace las pantallas de su servicio sobre el layout del integrante 1 | Se puede comprar desde el navegador, no solo desde Postman |
| 7 | Pruebas JUnit de la capa de datos y de servicio, colecciones de Postman, endurecer el login | CI en verde con pruebas reales y una colección de Postman que corre el flujo completo |
| 8 | Despliegue en AWS, HTTPS, imágenes en S3 y ensayo de la sustentación | URL pública funcionando y los cinco capaces de explicar el sistema completo |

La semana 4 es la bisagra. Si el checkout no está funcionando al cerrarla, lo que se recorta es el cliente web de la semana 6, no las pruebas ni el despliegue.

## Riesgos y decisiones abiertas

### Riesgos

- **La fecha de entrega.** El plan asume ocho semanas y nadie me dijo cuántas tienen. Si son seis, lo primero que se recorta es el cliente web y se sustenta con Postman; si hace falta recortar más, ms-inventario se fusiona con ms-catalogo y quedan cuatro servicios, que sigue cumpliendo el mínimo de la ficha.
- **El checkout es el único punto donde se tocan cuatro servicios.** Si se atrasa, se atrasa todo lo demás. Por eso va en la semana 4 y no al final: así queda margen para arreglarlo sin sacrificar la entrega.
- **Spring Cloud es nuevo para casi todos.** Eureka y Gateway son un par de archivos de configuración y una anotación cada uno, pero la primera vez que un servicio no se registra se pierde una tarde entera. Por eso el Sprint 0 lo hacen los cinco juntos en vez de repartirlo.
- **La cuenta de AWS necesita una tarjeta.** Resúelvanlo en la semana 1, no en la 8. Si nadie del equipo puede poner una, la alternativa gratuita hay que probarla temprano, porque la capacidad no siempre está disponible.
- **Un integrante que se cae deja un servicio huérfano.** Es el costo de tener un dueño por carpeta. Se compensa con dos cosas: el integrante 1 revisa todos los PR y entiende los ocho servicios, y cada servicio tiene su colección de Postman, así que un reemplazo sabe qué debería responder cada endpoint.

### Decisiones que hay que cerrar antes de arrancar

| Decisión | Opciones | Recomendación |
| --- | --- | --- |
| Cliente que consume la API | Thymeleaf dentro del equipo, React aparte, o solo Postman | Thymeleaf: un solo lenguaje para los cinco y nadie del equipo es front |
| Comunicación entre servicios | Solo REST síncrono con Feign, o agregar RabbitMQ para eventos | Solo Feign. RabbitMQ suma un contenedor y un concepto nuevo sin sumar nota |
| Base de datos | Una instancia RDS con 5 schemas, o 5 instancias | Una instancia con 5 schemas. Cinco instancias cuestan cinco veces más y no demuestran nada extra |
| Pruebas de la capa de datos | H2 en memoria o Testcontainers con MySQL real | H2 si quieren un CI rápido; Testcontainers si quieren que la prueba valga de verdad |
| Nube | AWS con tarjeta u Oracle Always Free | AWS si alguien puede poner tarjeta; es la que van a nombrar en una entrevista |

Hay una fila de la ficha que conviene aclarar con el docente antes de empezar: **“consumo de la API por un cliente” está marcada “en duda”**. Pregunten si una colección de Postman cuenta como cliente o si exige una aplicación aparte. Este plan asume lo segundo, que es lo más exigente; si la respuesta es la primera, se ahorran una semana completa.

También hacen falta los nombres de usuario de GitHub de los cinco para poder escribir el archivo CODEOWNERS.
