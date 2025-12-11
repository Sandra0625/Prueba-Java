# Prueba-Java

Este repositorio contiene una aplicación de ejemplo (Spring Boot) que implementa la lógica mínima para gestionar tarjetas y transacciones (generar tarjeta, enroll, recargas, compras y anulaciones). Incluye API REST, una UI estática sencilla y pruebas unitarias/integación.

**Estado**: funcional — la aplicación arranca y sirve una UI en `http://localhost:8081/`.

 # Prueba-Java

Aplicación de ejemplo full-stack (backend Java + frontend estático) para gestionar tarjetas y transacciones. Contiene:

- Backend: Spring Boot 3.3.1, Java 21
- Autenticación: usuarios + JWT
- Persistencia: Spring Data JPA (MySQL en producción / H2 para pruebas)
- Frontend estático: `src/main/resources/static` (HTML/CSS/JS)

Estado actual: la aplicación se puede empaquetar y ejecutar localmente; la UI está disponible en `http://localhost:8081/`.

**Contenido del repositorio (ubicación de archivos importantes)**
- `pom.xml` — configuración y dependencias Maven.
- `src/main/java/com/bankinc/prueba/controller` — controladores REST: `AuthController`, `CardController`, `TransactionController`.
- `src/main/java/com/bankinc/prueba/service` — servicios: `AuthService`, `CardService`, `TransactionService`.
- `src/main/java/com/bankinc/prueba/model` — entidades JPA: `User`, `Card`, `Transaction`.
- `src/main/java/com/bankinc/prueba/repository` — interfaces Spring Data JPA.
- `src/main/java/com/bankinc/prueba/security` — JWT provider y filtro (`JwtTokenProvider`, `JwtAuthenticationFilter`).
- `src/main/java/com/bankinc/prueba/config` — `SecurityConfig` (configuración de Spring Security).
- `src/main/java/com/bankinc/prueba/migration` — `UserMigrationRunner` (migración en tiempo de ejecución desde `cards.holderName` a `users`).
- `src/main/resources/static` — `index.html`, `styles.css`, `app.js` (UI y lógica cliente).
- `src/main/resources/application.properties` — configuración de puerto, datasource, JWT y flags (`app.migrate-users`, `spring.flyway.enabled`).

**Resumen técnico y dependencias**
- Java 21
- Spring Boot 3.3.1
- Spring Data JPA + Hibernate 6.x
- Spring Security + JWT (`jjwt`)
- Maven para build

## Algoritmos y lógica (detalle ampliado)

**1) Generación de número de tarjeta (`CardService.generateCardNumber`)**

- Entradas: `productId` (string, típicamente 6 caracteres).
- Salida: `cardId` de 16 caracteres. Implementación usada:
  - `cardId = productId + randomDigits(10)` (10 dígitos aleatorios 0-9).
  - `expirationDate = LocalDate.now().plusYears(3)`.
  - `balance = BigDecimal.ZERO`, `active = false`, `blocked = false`.

Puntos a considerar (mejoras posibles): Luhn check digit, bloqueo por duplicado en colisiones muy raras, formato BIN más completo.

Complejidad: O(1).

**2) Compra (`TransactionService.purchase`)**

- Validaciones en orden:
  1. Existe la tarjeta.
  2. Está activa (`active == true`).
  3. No está bloqueada.
  4. No está vencida (`expirationDate` vs fecha actual).
  5. `balance >= price`.
- Si todo pasa, resta `price` de `Card.balance`, guarda `Card` y crea `Transaction` con `UUID` como `transactionId` y `status = COMPLETED`.
- Operación envuelta en `@Transactional` para consistencia.

**3) Anulación de transacciones (`TransactionService.annulTransaction`)**

- Solo permitido dentro de 24 horas de `transactionDate`.
- Suma `price` al `Card.balance` y marca la transacción como `ANNULLED`.

**4) Migración de usuarios (`UserMigrationRunner`)**

- Escanea todas las `cards` y por cada tarjeta sin `owner` y con `holderName` no vacío crea un `User`.
- Normaliza `holderName` a `username` (minúsculas, caracteres no alfanuméricos → `_`, longitud límite 30) y asegura unicidad añadiendo sufijos numéricos si hace falta.
- Genera contraseña aleatoria (codificada con BCrypt) y enlaza la tarjeta al usuario.

## Endpoints principales (rápida guía de uso)

- `POST /auth/register` — registro: recibe `{ "username":"...", "password":"..." }`. Devuelve `{ "token":"..." }`.
- `POST /auth/login` — login: `{ "username":"...", "password":"..." }` → `{ "token":"..." }`.
- `POST /cards/generate` — `{ "productId":"PROD01", "holderName":"Nombre" }` → devuelve `cardId`.
- `POST /cards/{cardId}/enroll` — activa la tarjeta.
- `POST /cards/{cardId}/recharge?amount=100.50` — recarga.
- `GET /cards/{cardId}/balance` — obtiene saldo.
- `GET /cards/me` — obtiene tarjetas del usuario autenticado (JWT required).
- `POST /transaction/purchase` — `{ "cardId":"...", "price":12.34 }` → crea transacción.
- `POST /transaction/anulation` — `{ "transactionId":"..." }` → anula (si cumple condiciones).

Para llamadas protegidas, incluya header: `Authorization: Bearer <token>`.

## Cómo construir y ejecutar (Windows)

1) Asegúrate de tener JDK 21 y Maven instalados. En PowerShell:

```powershell
$Env:JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.9.10-hotspot'
mvn -DskipTests package
java -jar target\\prueba-0.0.1-SNAPSHOT.jar
```

2) Flags útiles al arrancar:

```powershell
java -Dspring.flyway.enabled=false -Dapp.migrate-users=true -jar target\\prueba-0.0.1-SNAPSHOT.jar
```

- `spring.flyway.enabled=false` desactiva la ejecución automática de Flyway (útil si la DB objetivo es incompatible con la versión de Flyway incluida).
- `app.migrate-users=true` activa el `UserMigrationRunner` para crear usuarios desde `cards.holderName` al arrancar.

## Desarrollo y tests

- Ejecutar tests: `mvn test`.
- Ejecutar con logs a fichero: usar scripts o redirección de salida.

## Problemas conocidos y consejos de debugging

- Paquete JAR bloqueado al repackage: sucede si hay un proceso Java ejecutando el JAR. Detener proceso antes de `mvn package`:

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Select-Object ProcessId, CommandLine
Stop-Process -Id <PID> -Force
```

- PathPattern / Resource matching: si hay errores 500/PatternParseException en tiempo de ejecución por matchers en `SecurityConfig`, corrige los patrones a rutas explícitas (ej. `/styles.css`, `/app.js`, `/index.html`) o usa el DSL actualizado.

- Flyway: si la base de datos remota es demasiado antigua (ej. MySQL 5.5), Flyway puede fallar al iniciar. En ese caso desactiva `spring.flyway.enabled` y usa `UserMigrationRunner` u otro mecanismo de migración manual.

## Seguridad

- Autenticación basada en JWT (`JwtTokenProvider`): el backend genera tokens firmados con un secret definido en `application.properties` (`jwt.secret`) y los valida en cada petición mediante `JwtAuthenticationFilter`.
- Las rutas estáticas y `/auth/**` están permitidas sin autenticación; el resto requiere token.

## Buenas prácticas y próximos pasos recomendados

- Considerar usar Luhn o esquema BIN para tarjetas en vez de simple concatenación.
- Añadir controles y auditoría para cambios de saldo (event sourcing / logs de auditoría).
- Añadir validaciones más estrictas en el frontend y manejar errores del backend con mensajes de usuario.
- Migrar APIs de Spring Security que están marcadas como deprecated (ver `SecurityConfig`) para evitar warnings futuros.

---

Si quieres, puedo:

- Añadir un `.gitignore` y limpiarlo del repositorio.
- Aplicar la migración automática de `SecurityConfig` para eliminar las APIs deprecated.
- Generar documentación OpenAPI (Swagger) con ejemplos de request/response.

Dime cuál de las opciones quieres que haga a continuación y lo implemento.
- Error de empaquetado por JAR bloqueado: si `mvn package` falla porque el JAR no puede ser renombrado, probablemente haya una instancia Java ejecutando el JAR. Localiza y deténla (PowerShell):
