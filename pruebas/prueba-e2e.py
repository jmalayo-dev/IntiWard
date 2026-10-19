"""Prueba de punta a punta contra el gateway y Aiven, solo con la biblioteca estándar de Python.

Uso (con los 7 servicios arriba y un administrador en Aiven):
  ADMIN_CORREO=... ADMIN_CLAVE=... python3 -I pruebas/prueba-e2e.py         # flujo completo (52 verificaciones)
  ... python3 -I pruebas/prueba-e2e.py caida   # con ms-comprobantes APAGADO: compensación del checkout
  ... python3 -I pruebas/prueba-e2e.py web     # llamadas del panel y limpieza de los datos de prueba
  GW=http://localhost:5173/api ...                    # lo mismo, pasando por el proxy de Vite de la web

Crea un producto y dos clientes de prueba (correo e2e.*@intiguard.pe); la fase "web" los desactiva.
"""
import json, os, sys, tempfile, time, urllib.request, urllib.error

GW = os.environ.get('GW', 'http://localhost:8080')
ok_count = 0
fallos = []
estado_path = os.path.join(tempfile.gettempdir(), 'intiguard_e2e_estado.json')


def req(metodo, ruta, cuerpo=None, token=None, crudo=False):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    r = urllib.request.Request(GW + ruta, data=datos, method=metodo)
    if datos is not None:
        r.add_header('Content-Type', 'application/json')
    if token:
        r.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(r, timeout=20) as resp:
            b = resp.read()
            return resp.status, (b if crudo else (json.loads(b) if b else None)), {k.title(): v for k, v in resp.headers.items()}
    except urllib.error.HTTPError as e:
        b = e.read()
        try:
            return e.code, json.loads(b), dict(e.headers)
        except Exception:
            return e.code, b, dict(e.headers)


def check(nombre, condicion, detalle=''):
    global ok_count
    if condicion:
        ok_count += 1
        print(f'  ✅ {nombre}')
    else:
        fallos.append(nombre)
        print(f'  ❌ {nombre}  {detalle}')


def login(correo, clave):
    s, b, _ = req('POST', '/auth/login', {'correo': correo, 'password': clave})
    assert s == 200, (s, b)
    return b['token']


ADMIN = login(os.environ['ADMIN_CORREO'], os.environ['ADMIN_CLAVE'])
fase = sys.argv[1] if len(sys.argv) > 1 else 'todo'

if fase == 'web':
    print('\n== Llamadas del panel con los datos reales (vía ' + GW + ')')
    s, b, _ = req('GET', '/productos?size=1000&inactivos=true', token=ADMIN)
    check(f"panel: todo el catálogo en una página ({b['page']['totalElements']} con inactivos)", s == 200 and len(b['content']) == b['page']['totalElements'], (s, b.get('page')))
    ids = ','.join(str(p['id']) for p in b['content'])
    s, st, _ = req('GET', '/stock?ids=' + ids)
    check(f'stock de toda la grilla en una llamada ({len(st)} productos)', s == 200 and len(st) == len(b['content']), s)
    s, st, _ = req('GET', '/stock')
    check('campana de agotados: GET /stock sin ids', s == 200 and len(st) > 100, s)
    s, mv, _ = req('GET', '/movimientos', token=ADMIN)
    check(f'movimientos ({len(mv)})', s == 200 and len(mv) > 0, s)
    s, vs, _ = req('GET', '/pedidos', token=ADMIN)
    check(f'ventas del panel ({len(vs)}) con detalles', s == 200 and all('detalles' in v for v in vs), s)
    s, cs, _ = req('GET', '/comprobantes', token=ADMIN)
    check(f'comprobantes del panel ({len(cs)})', s == 200, s)
    s, us, _ = req('GET', '/usuarios', token=ADMIN)
    check(f'usuarios ({len(us)}) con bloqueadoHasta y ultimoAcceso', s == 200 and all('ultimoAcceso' in u and 'bloqueadoHasta' in u for u in us), s)
    e = json.load(open(estado_path))
    s, b, _ = req('PUT', f"/productos/{e['idProducto']}", {'nombre': 'Producto de prueba E2E (editado)', 'descripcion': '', 'idMarca': b['content'][0]['idMarca'],
        'idCategoria': b['content'][0]['idCategoria'], 'precio': 30, 'imagenUrl': None, 'activo': True, 'especificaciones': []}, ADMIN)
    check('editar producto como lo manda el formulario 5.3', s == 200 and b['especificaciones'] == [] and b['imagenUrl'] is None, (s, b))
    s, _, _ = req('DELETE', f"/productos/{e['idProducto']}", token=ADMIN)
    check('desactivar producto de prueba (204)', s == 204, s)
    s, b, _ = req('GET', f"/productos/{e['idProducto']}")
    check('el detalle lo sigue mostrando con activo=false', s == 200 and b['activo'] is False, b)
    for clave in ('idUsuario', 'idUsuario2'):
        s, _, _ = req('DELETE', f"/usuarios/{e[clave]}", token=ADMIN)
        check(f'desactivar cliente de prueba {e[clave]} (204)', s == 204, s)
    s, b, _ = req('PUT', f"/usuarios/{e['idUsuario']}", {'activo': True}, ADMIN)
    check('reactivar (como el interruptor 5.10)', s == 200 and b['activo'] is True, b)
    req('DELETE', f"/usuarios/{e['idUsuario']}", token=ADMIN)
    s, b, _ = req('POST', '/auth/login', {'correo': e['correo'], 'password': 'ClaveE2e123'})
    check('un cliente desactivado no puede entrar (401)', s == 401, s)
    print(f'\nResultado: {ok_count} OK, {len(fallos)} fallos {fallos}')
    sys.exit(1 if fallos else 0)

if fase == 'caida':
    e = json.load(open(estado_path))
    print('\n== Checkout con ms-comprobantes apagado (compensación)')
    cli = login(e['correo'], 'ClaveE2e123')
    s0 = req('GET', f"/stock/{e['idProducto']}")[1]
    s, b, _ = req('POST', '/carrito/items', {'idProducto': e['idProducto'], 'cantidad': 1}, cli)
    check('agregar al carrito', s == 201, (s, b))
    t = time.time()
    s, b, _ = req('POST', '/pedidos/checkout', {'tipoComprobante': 'BOLETA', 'docCliente': None, 'nombreRazonSocial': 'Cliente E2E'}, cli)
    check(f'checkout responde 503 en {time.time()-t:.1f} s', s == 503, (s, b))
    check('mensaje claro para la web', isinstance(b, dict) and 'comprobantes' in b.get('mensaje', ''), b)
    s1 = req('GET', f"/stock/{e['idProducto']}")[1]
    check('el stock vuelve a disponible (liberado)', s1['disponible'] == s0['disponible'] and s1['reservado'] == 0, (s0, s1))
    pedidos = req('GET', '/pedidos', token=cli)[1]
    check('la venta quedó ANULADA', pedidos[0]['estado'] == 'ANULADA', pedidos[0])
    movs = req('GET', f"/movimientos?idProducto={e['idProducto']}", token=ADMIN)[1]
    check('movimientos RESERVA y LIBERACION de esa venta', [m['tipo'] for m in movs[:2]] == ['LIBERACION', 'RESERVA'], movs[:2])
    car = req('GET', '/carrito', token=cli)[1]
    check('el carrito se conserva para reintentar', len(car['items']) == 1, car)
    req('DELETE', f"/carrito/items/{e['idProducto']}", token=cli)
    print(f'\nResultado: {ok_count} OK, {len(fallos)} fallos {fallos}')
    sys.exit(1 if fallos else 0)

sello = time.strftime('%Y%m%d%H%M%S')

print('== Catálogo (público)')
s, b, _ = req('GET', '/productos?size=5&sort=precio,desc')
check('GET /productos paginado VIA_DTO', s == 200 and 'page' in b and len(b['content']) == 5, s)
total = b['page']['totalElements']
check(f'catálogo con {total} productos activos', total >= 100, total)
p0 = b['content'][0]
check('producto con activo, idMarca, idCategoria y especificaciones',
      all(k in p0 for k in ('activo', 'idMarca', 'idCategoria', 'especificaciones')), p0.keys())
precios = [p['precio'] for p in b['content']]
check('orden por precio descendente', precios == sorted(precios, reverse=True), precios)
s, b, _ = req('GET', '/productos?categoria=1&q=c%C3%A1mara&size=100')
check('filtro categoría + texto', s == 200 and all(p['idCategoria'] == 1 for p in b['content']), s)
s, b, _ = req('GET', '/productos?conStock=true&size=1')
check('filtro conStock (catálogo → inventario por Feign)', s == 200 and b['page']['totalElements'] > 0, (s, b))
s, marcas, _ = req('GET', '/marcas')
check('GET /marcas con contador', s == 200 and 'productos' in marcas[0], s)
s, cats, _ = req('GET', '/categorias')
check('GET /categorias', s == 200 and len(cats) >= 6, s)
s, b, _ = req('GET', '/productos/999999')
check('producto inexistente → 404', s == 404, s)

print('\n== Registro y perfil')
correo = f'e2e.{sello}@intiguard.pe'
s, nuevo, _ = req('POST', '/auth/registro', {'nombres': 'Cliente', 'apellidos': 'E2E', 'correo': correo,
                                            'password': 'ClaveE2e123', 'rol': 'ADMIN'})
check('registro → 201 siempre CLIENTE', s == 201 and nuevo['rol'] == 'CLIENTE', (s, nuevo))
s, b, _ = req('POST', '/auth/registro', {'nombres': 'X', 'apellidos': 'Y', 'correo': correo, 'password': 'ClaveE2e123'})
check('correo repetido → 409', s == 409 and b['mensaje'] == 'El correo ya está registrado', (s, b))
cli = login(correo, 'ClaveE2e123')
s, yo, _ = req('GET', '/usuarios/me', token=cli)
check('GET /usuarios/me con ultimoAcceso', s == 200 and yo['correo'] == correo and yo['ultimoAcceso'], (s, yo))
s, yo, _ = req('PUT', '/usuarios/me', {'telefono': '987654321', 'direccion': 'Av. Prueba 123', 'rol': 'ADMIN'}, cli)
check('PUT /usuarios/me (el rol no cambia)', s == 200 and yo['telefono'] == '987654321' and yo['rol'] == 'CLIENTE', (s, yo))
correo2 = f'e2e2.{sello}@intiguard.pe'
s, otro, _ = req('POST', '/auth/registro', {'nombres': 'Otro', 'apellidos': 'E2E', 'correo': correo2, 'password': 'ClaveE2e123'})
cli2 = login(correo2, 'ClaveE2e123')

print('\n== Seguridad en el gateway')
check('sin token → 401', req('GET', '/pedidos')[0] == 401)
check('token inválido → 401', req('GET', '/pedidos', token='abc.def.ghi')[0] == 401)
check('CLIENTE en ruta ADMIN → 403', req('GET', '/usuarios', token=cli)[0] == 403)
check('ruta interna POST /stock/reservar → 403', req('POST', '/stock/reservar', {'idVenta': 1, 'items': []}, ADMIN)[0] == 403)
check('ruta interna POST /comprobantes → 403', req('POST', '/comprobantes', {}, ADMIN)[0] == 403)
check('ADMIN no tiene carrito → 403', req('GET', '/carrito', token=ADMIN)[0] == 403)

print('\n== Panel: usuarios, marcas, producto de prueba y stock')
s, usuarios, _ = req('GET', '/usuarios', token=ADMIN)
check('GET /usuarios (ADMIN)', s == 200 and any(u['correo'] == correo for u in usuarios), s)
s, b, _ = req('POST', '/marcas', {'nombre': marcas[0]['nombre'].upper()}, ADMIN)
check('marca repetida (sin distinguir mayúsculas) → 409', s == 409, (s, b))
s, b, _ = req('PUT', f"/marcas/{marcas[0]['id']}", {'activo': True}, ADMIN)
check('PUT /marcas/{id}', s == 200 and b['activo'] is True, (s, b))
s, prod, _ = req('POST', '/productos', {'nombre': f'Producto de prueba E2E {sello}', 'descripcion': 'Creado por la prueba de punta a punta',
                                         'precio': 25.5, 'idMarca': marcas[0]['id'], 'idCategoria': cats[0]['id'],
                                         'especificaciones': [{'nombre': 'Color', 'valor': 'Blanco'}]}, ADMIN)
check('POST /productos → 201', s == 201 and prod['activo'] and prod['especificaciones'][0]['valor'] == 'Blanco', (s, prod))
idp = prod['id']
s, b, _ = req('POST', '/productos', {'nombre': '', 'precio': 0, 'idMarca': 999999, 'idCategoria': cats[0]['id']}, ADMIN)
check('producto inválido → 400 con detalles por campo', s == 400 and len(b['detalles']) == 3, (s, b))
s, b, _ = req('GET', f'/stock/{idp}')
check('producto nuevo: stock 0 sin fila (200)', s == 200 and b['disponible'] == 0 and b['actualizadoEn'] is None, (s, b))
s, b, _ = req('POST', '/stock/entrada', {'idProducto': idp, 'cantidad': 10, 'motivo': 'Prueba E2E'}, ADMIN)
check('entrada de 10 (crea la fila validando en catálogo)', s == 200 and b['disponible'] == 10, (s, b))
s, b, _ = req('PUT', f'/stock/{idp}', {'disponible': 8, 'motivo': 'Conteo E2E'}, ADMIN)
check('ajuste a 8', s == 200 and b['disponible'] == 8, (s, b))
s, b, _ = req('POST', '/stock/entrada', {'idProducto': 99999999, 'cantidad': 1}, ADMIN)
check('entrada de producto inexistente → 404', s == 404, (s, b))
s, b, _ = req('GET', f'/stock?ids={idp},99999999')
check('GET /stock?ids= (uno en 0)', s == 200 and [x['disponible'] for x in b] == [8, 0], b)

print('\n== Carrito')
s, car, _ = req('POST', '/carrito/items', {'idProducto': idp, 'cantidad': 2}, cli)
check('agregar 2', s == 201 and car['items'][0]['cantidad'] == 2, (s, car))
s, car, _ = req('POST', '/carrito/items', {'idProducto': idp}, cli)
check('agregar otra vez suma (3)', car['items'][0]['cantidad'] == 3, car)
s, car, _ = req('PUT', f'/carrito/items/{idp}', {'cantidad': 2}, cli)
check('cambiar a 2, total 51.0 y marca', s == 200 and car['total'] == 51.0 and car['items'][0]['marca'] == marcas[0]['nombre'], car)

print('\n== Checkout con boleta')
s, venta, _ = req('POST', '/pedidos/checkout', {'tipoComprobante': 'BOLETA', 'docCliente': '', 'nombreRazonSocial': 'Cliente E2E'}, cli)
check('checkout → 201 CONFIRMADA', s == 201 and venta['estado'] == 'CONFIRMADA', (s, venta))
check('montos: 51.00 = 43.22 + 7.78', (venta['total'], venta['subtotal'], venta['igv']) == (51.0, 43.22, 7.78), venta)
check('venta con detalles', venta['detalles'][0]['importe'] == 51.0, venta)
idv = venta['id']
s, b, _ = req('GET', f'/stock/{idp}')
check('stock: 6 disponibles, 0 reservados', (b['disponible'], b['reservado']) == (6, 0), b)
movs = req('GET', f'/movimientos?idProducto={idp}', token=ADMIN)[1]
check('movimientos CONFIRMACION, RESERVA, AJUSTE, ENTRADA', [m['tipo'] for m in movs] == ['CONFIRMACION', 'RESERVA', 'AJUSTE', 'ENTRADA'], [m['tipo'] for m in movs])
check('carrito vacío tras la compra', req('GET', '/carrito', token=cli)[1]['items'] == [])

print('\n== Pedidos y comprobantes')
s, ped, _ = req('GET', '/pedidos', token=cli)
check('mis pedidos (solo los míos, con detalles)', s == 200 and len(ped) == 1 and ped[0]['detalles'], (s, ped))
check('otro cliente no ve mi pedido → 403', req('GET', f'/pedidos/{idv}', token=cli2)[0] == 403)
s, comp, _ = req('GET', f'/comprobantes/venta/{idv}', token=cli)
check('comprobante de la venta (B001-…)', s == 200 and comp['numero'].startswith('B001-') and comp['total'] == 51.0, (s, comp))
check('pdfUrl armado', comp['pdfUrl'] == f"/comprobantes/{comp['id']}/pdf", comp)
check('otro cliente no ve mi comprobante → 403', req('GET', f"/comprobantes/{comp['id']}", token=cli2)[0] == 403)
s, pdf, h = req('GET', comp['pdfUrl'], token=cli, crudo=True)
check('PDF descargable', s == 200 and pdf[:5] == b'%PDF-' and comp['numero'] in h.get('Content-Disposition', ''), (s, h.get('Content-Disposition')))
open(os.path.join(os.path.dirname(estado_path), 'comprobante_e2e.pdf'), 'wb').write(pdf)
s, lista, _ = req('GET', '/comprobantes', token=cli)
check('mis comprobantes', s == 200 and len(lista) == 1, (s, lista))
hoy = time.strftime('%Y-%m-%d')
s, rep, _ = req('GET', f'/comprobantes/reporte?desde={hoy}&hasta={hoy}', token=ADMIN)
check('reporte del día (ADMIN) incluye el comprobante', s == 200 and any(c['id'] == comp['id'] for c in rep['comprobantes']), s)

print('\n== Checkout con factura y sin stock suficiente')
req('POST', '/carrito/items', {'idProducto': idp, 'cantidad': 1}, cli2)
s, v2, _ = req('POST', '/pedidos/checkout', {'tipoComprobante': 'FACTURA', 'docCliente': '20601234567', 'nombreRazonSocial': 'Empresa E2E S.A.C.'}, cli2)
check('factura → 201', s == 201 and v2['tipoComprobante'] == 'FACTURA', (s, v2))
c2 = req('GET', f"/comprobantes/venta/{v2['id']}", token=cli2)[1]
check('numeración F001', c2['numero'].startswith('F001-'), c2)
s, b, _ = req('POST', '/pedidos/checkout', {'tipoComprobante': 'FACTURA', 'docCliente': '123', 'nombreRazonSocial': 'X'}, cli)
check('RUC inválido → 400', s == 400, (s, b))
req('POST', '/carrito/items', {'idProducto': idp, 'cantidad': 50}, cli)
s, b, _ = req('POST', '/pedidos/checkout', {'tipoComprobante': 'BOLETA', 'docCliente': '45678912', 'nombreRazonSocial': 'Cliente E2E'}, cli)
check('sin stock → 409 con el producto', s == 409 and b['detalles'] == [f'idProducto: {idp}'] and 'Producto de prueba' in b['mensaje'], (s, b))
check('la venta fallida quedó ANULADA', req('GET', '/pedidos', token=cli)[1][0]['estado'] == 'ANULADA')
s, b, _ = req('GET', f'/stock/{idp}')
check('el stock no se movió (5, 0)', (b['disponible'], b['reservado']) == (5, 0), b)
req('DELETE', f'/carrito/items/{idp}', token=cli)

json.dump({'correo': correo, 'idProducto': idp, 'idUsuario': nuevo['id'], 'idUsuario2': otro['id']}, open(estado_path, 'w'))
print(f'\nResultado: {ok_count} OK, {len(fallos)} fallos {fallos}')
sys.exit(1 if fallos else 0)
