# Chavez Store — cómo correrlo

Sistema interno de control de **stock, compras, ventas, clientes y ganancias** para una
botillería. Monorepo con dos piezas independientes:

| Ruta | Qué es | Stack |
|------|--------|-------|
| `api-rest/` | Backend REST | Java 21 · Spring Boot 4.1.1 · MySQL 8 |
| `frontend/` | Aplicación web | Angular 20 · Angular Material · Chart.js |

> La venta es un **asiento interno**: documenta la salida de stock y la utilidad generada.
> No registra cobro de mostrador, no emite comprobante ni métodos de pago por venta.
> El único dinero que entra al sistema son los **abonos** de clientes con crédito.
> Ver `LOGICA_NEGOCIO.md` §1.

Documentación del proyecto:

| Documento | Contenido |
|-----------|-----------|
| `LOGICA_NEGOCIO.md` | Reglas de negocio (`R-A-xx`, `R-C-xx`, `R-I-xx`, `R-CO-xx`, `R-V-xx`, `R-CL-xx`, `R-R-xx`) |
| `MODELO_DATOS.md` | Tablas, ENUMs, DDL e invariantes |
| `ARQUITECTURA.md` | Stack, estructura, capas y flujo JWT |
| `ESQUEMA_API.md` | Endpoints REST, DTOs y códigos de error |

---

## 1. Requisitos previos

| Herramienta | Versión | Verificar |
|-------------|---------|-----------|
| JDK | 21 | `java -version` |
| MySQL | 8.x | `mysql --version` |
| Node.js | 20 o superior | `node --version` |
| npm | 10 o superior | `npm --version` |

No hace falta instalar MySQL si usas Docker:

```bash
docker run -d --name chavez-mysql -p 3306:3306 ^
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=chavez_store ^
  mysql:8
```

---

## 2. Base de datos

El DDL completo está en `api-rest/src/main/resources/db/schema.sql` e incluye los catálogos
iniciales (roles, categorías, marcas) y un usuario administrador.

```bash
mysql -u root -p < api-rest/src/main/resources/db/schema.sql
```

Si la base ya existe y solo quieres recargar los catálogos:

```bash
mysql -u root -p chavez_store < api-rest/src/main/resources/db/schema.sql
```

**Usuario inicial** (creado por `schema.sql`):

| Campo | Valor |
|-------|-------|
| usuario | `admin` |
| contraseña | `Admin123` |

> Cambia esa contraseña desde **Configuración → Cambiar contraseña** apenas ingreses.
> El hash está en el repositorio: no sirve para producción.

---

## 3. Backend

### 3.1 Configurar la conexión

`api-rest/src/main/resources/application.properties` ya apunta a `localhost:3306` con usuario
`root` y contraseña `root`. Si tu MySQL usa otra contraseña, edítalo ahí o crea un
`application-local.properties` en `api-rest/src/main/resources/` (está en `.gitignore`):

```properties
spring.datasource.username=root
spring.datasource.password=TU_CONTRASENA
```

Para overriding por variables de entorno:

```bash
set DB_PASSWORD=root
```

### 3.2 Correr

```bash
cd api-rest
./mvnw spring-boot:run
```

En Windows:

```bat
cd api-rest
mvnw.cmd spring-boot:run
```

Verifica que respondió:

```bash
curl http://localhost:8080/api/auth/login
```

El backend queda en `http://localhost:8080`.

### 3.3 Compilar el JAR

```bash
cd api-rest
./mvnw clean package
java -jar target/api-rest-0.0.1-SNAPSHOT.jar
```

### 3.4 Tests

```bash
cd api-rest
./mvnw test
```

---

## 4. Frontend

```bash
cd frontend
npm install
npm start
```

Queda en `http://localhost:4200`.

El `npm start` ya usa `proxy.conf.json`, que redirige `/api` a `http://localhost:8080`.
**Por eso no necesitas CORS ni configurar nada del backend**: el navegador habla siempre
con el puerto 4200 y el proxy reenvía las llamadas.

Si cambiaste el puerto del backend, ajusta el destino en `frontend/proxy.conf.json`.

### 4.1 Otros comandos

```bash
npm run build     # build de producción en frontend/dist
npm run watch     # build en modo watch
npm test          # tests unitarios (Karma + Jasmine)
```

### 4.2 Rutas principales

| Ruta | Pantalla | Rol mínimo |
|------|----------|------------|
| `/login` | Iniciar sesión | Público |
| `/dashboard` | Resumen del día | Autenticado |
| `/productos` | Catálogo con presentaciones | Autenticado |
| `/ventas/nueva` | Registro de venta interna | ADMIN, CAJERO |
| `/ventas` | Listado y detalle de ventas | Autenticado |
| `/inventario` | Stock, mermas y ajustes | Autenticado |
| `/inventario/kardex/:id` | Kardex de un producto | Autenticado |
| `/compras` | Compras y recepción en almacén | Autenticado |
| `/clientes` | Clientes, crédito y abonos | Autenticado |
| `/reportes/ganancias` | Ganancia por período | ADMIN, SUPERVISOR |
| `/reportes/stock` | Stock crítico y vencimientos | ADMIN, SUPERVISOR, ALMACENERO |
| `/reportes/cuentas-por-cobrar` | Antigüedad de la deuda | ADMIN |

> El menú lateral ya oculta las secciones no permitidas para tu rol. La autorización real
> la aplica el backend, que responde `403 AUTH_006` cuando corresponde.

---

## 5. Orden sugerido para la primera vez

1. Levanta MySQL y ejecuta `schema.sql`.
2. Levanta el backend (`./mvnw spring-boot:run`).
3. Levanta el frontend (`npm start`) e ingresa con `admin` / `Admin123`.
4. **Cambia la contraseña** del administrador.
5. Crea categorías y marcas (si necesitas otras que las del DDL).
6. Crea un proveedor.
7. Crea un producto con sus presentaciones: al menos una de tipo `COMPRA` (para recibir compras)
   y al menos una de tipo `VENTA` (con precio, es la que se usa al vender).
8. Carga el **stock inicial** del producto desde su ficha.
9. Registra una compra y recíbela en almacén: verás cómo se recalcula el costo promedio.
10. Registra una venta de mostrador.
11. Revisa los reportes de ganancias, mermas y cuentas por cobrar.

---

## 6. Problemas frecuentes

### El login responde 401

La contraseña `Admin123` solo existe si ejecutaste `schema.sql` completo. Si la base ya
existía, inserta el usuario manualmente o crea uno con un hash BCrypt generado.

### `Port 4200 is already in use`

```bash
npm start -- --port 4300
```

### `Port 8080 is already in use`

Algo más está en el 8080 (MySQL X Plugin, otra app). Cambia `server.port` en
`application.properties`, y actualiza el `target` de `frontend/proxy.conf.json`.

### `Table ... doesn't exist`

`spring.jpa.hibernate.ddl-auto=validate` no crea tablas: el esquema debe existir. Ejecuta
`schema.sql`. Además, `spring.sql.init.mode=never` desactiva la inicialización automática
a propósito, para que nadie borre datos por accidente.

### Un reporte sale vacío

Los reportes respetan el rango de fechas elegido. Usa los atajos `Hoy`, `Mes actual` o
`7 días`. Si no hay ventas, `Margen %` es 0 por diseño (R-R-05).

### `STK_003` al verificar inventario

El stock cacheado de `products.stock` no coincide con la suma de `stock_movements`.
No es un error de la UI: es una alerta de auditoría (R-I-04). Corrige con un ajuste
manual con motivo, nunca editando el caché a mano.

---

## 7. Estructura del frontend

```
frontend/src/app/
├── core/                    # modelos, enums, servicios, guards, interceptors
│   ├── enums/               # catálogos ENUM del dominio
│   ├── guards/              # auth.guard, role.guard
│   ├── interceptors/        # jwt.interceptor, error.interceptor
│   ├── models/              # espejo de los DTO de ESQUEMA_API.md
│   ├── services/            # 11 servicios REST
│   └── utils/               # api-base, jwt.util, error-mapeo
├── features/                # una carpeta por módulo, lazy-loaded
│   ├── auth/                # login, cambio de contraseña
│   ├── dashboard/
│   ├── categorias/ marcas/ productos/
│   ├── ventas/              # registro interno (NO es un POS)
│   ├── clientes/            # CRUD, abonos, límite de crédito
│   ├── inventario/          # stock, kardex, mermas, ajustes
│   ├── proveedores/ compras/
│   └── reportes/            # los 8 reportes de ESQUEMA_API.md §12
├── layout/shell/            # toolbar, menú por rol
├── shared/                  # componentes, pipes, directivas reutilizables
├── app.config.ts
└── app.routes.ts
```

Todas las rutas de negocio son lazy (`loadComponent`), así que el bundle inicial
solo carga login + shell.

---

## 8. Convenciones

| Aspecto | Convención |
|---------|-----------|
| Clases y métodos | PascalCase / camelCase, **en español** |
| Archivos | kebab-case con sufijo: `producto-form-dialog.component.ts` |
| Interfaces de servicio | Prefijo `I` en el backend (`IVentaService`) |
| Enums | Sufijo de tipo (`TipoVenta`, `EstadoVenta`) |
| Errores | Formato único `{ code, message, timestamp, path }` |
| Commits | Conventional Commits (`feat:`, `fix:`, `chore:`) |