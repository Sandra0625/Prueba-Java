# Prueba-Java

Este repositorio contiene una aplicación de ejemplo (Spring Boot) que implementa la lógica mínima para gestionar tarjetas y transacciones (generar tarjeta, enroll, recargas, compras y anulaciones). Incluye API REST, una UI estática sencilla y pruebas unitarias/integación.

**Estado**: funcional — la aplicación arranca y sirve una UI en `http://localhost:8081/`.

**Resumen rápido**
- **Lenguaje / Framework**: Java 21, Spring Boot 3.3.1
- **Build**: Maven
- **DB**: MySQL en producción (XAMPP recomendado en local), H2 para pruebas (in-memory)
- **Puerto por defecto**: `8081` (configurado en `src/main/resources/application.properties`)

**Estructura principal**
- `src/main/java/com/bankinc/prueba/controller` : Controladores REST (`CardController`, `TransactionController`, `DebugController`).
- `src/main/java/com/bankinc/prueba/service` : Lógica de negocio (`CardService`, `TransactionService`).
- `src/main/java/com/bankinc/prueba/model` : Entidades JPA (`Card`, `Transaction`).
- `src/main/java/com/bankinc/prueba/repository` : Repositorios Spring Data JPA.
- `src/main/resources/static` : UI estática (`index.html`, `styles.css`, `app.js`).

**Objetivo del proyecto**
Proveer un ejemplo sencillo de backend que cubre:
- Generación y persistencia de tarjetas.
- Enroll (activación) de tarjetas.
- Recarga de saldo y consulta de saldo.
- Procesamiento de compras (validaciones + registro de transacción).
- Anulación de transacciones en ventana de 24 horas.

**Algoritmos y lógica principal**

1) Generación de número de tarjeta (en `CardService.generateCardNumber`)
- Entrada: `productId` (string de 6 caracteres). Requisito: longitud exactamente 6.
- Proceso: concatena `productId` + 10 dígitos aleatorios (0-9) para formar un `cardId` de 16 caracteres.
- Resultado: crea `Card` con campos iniciales:
	- `cardId`: el valor generado
	- `productId`: el proporcionado
	- `expirationDate`: `LocalDate.now().plusYears(3)`
	- `balance`: `BigDecimal.ZERO`
	- `active`: `false` (por defecto)
	- `blocked`: `false`

Pseudocódigo:

```
function generateCardNumber(productId):
		assert productId != null and len(productId) == 6
		randomPart = randomDigits(10)
		cardNumber = productId + randomPart
		card = new Card(cardId=cardNumber, productId=productId, expirationDate=now+3y, balance=0, active=false, blocked=false)
		save(card)
		return cardNumber
```

Complejidad: O(1) tiempo y espacio constante por operación.

2) Compra / `TransactionService.purchase`
- Validaciones (en orden): tarjeta existe, está activa, no está bloqueada, no está vencida, saldo suficiente.
- Acción: resta `price` del `Card.balance`, guarda la tarjeta, crea `Transaction` con `UUID` y `transactionDate = now`, `status = COMPLETED` y lo guarda.

Puntos importantes:
- Todas las operaciones que actualizan saldo o estado usan el repositorio JPA; `purchase` y `annulTransaction` están anotadas con `@Transactional` para consistencia.
- Anulación (`annulTransaction`): sólo dentro de las 24 horas siguientes a `transactionDate`; si procede, suma el `price` al `Card.balance` y marca `Transaction.status = ANNULLED`.

3) Serialización JSON y proxies Hibernate
- Los modelos `Card` y `Transaction` tienen `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` para evitar excepciones de Jackson al serializar proxies lazy.

**API (endpoints principales)**
- `POST /cards/generate?productId=PROD01` : genera tarjeta; devuelve `cardId` (texto plano).
- `POST /cards/{cardId}/enroll` : activa la tarjeta.
- `POST /cards/{cardId}/recharge?amount=100.50` : recarga el monto (BigDecimal) en la tarjeta.
- `GET  /cards/{cardId}/balance` : devuelve el saldo (texto/plano con valor numérico).
- `POST /transaction/purchase` : cuerpo JSON `{ "cardId":"...", "price": 12.34 }`; devuelve `{ "transactionId":"...","status":"Completed" }`.
- `GET  /transaction/{transactionId}` : obtiene la transacción (JSON).
- `POST /transaction/anulation` : cuerpo JSON `{ "transactionId":"..." }` — anula si cumple ventana de 24h.
- `GET /api/status` : endpoint de diagnóstico, devuelve `OK`.
- `GET /ui` y `GET /` : sirven la UI estática (`index.html`).

Ejemplos (consumo rápido con `curl`):

```powershell
# Generar tarjeta
curl -X POST "http://localhost:8081/cards/generate?productId=PROD01"

# Enroll
curl -X POST "http://localhost:8081/cards/PROD01XXXXXXXXXXXX/enroll"

# Recarga
curl -X POST "http://localhost:8081/cards/PROD01XXXXXXXXXXXX/recharge?amount=50.00"

# Compra (JSON)
curl -X POST "http://localhost:8081/transaction/purchase" -H "Content-Type: application/json" -d '{"cardId":"PROD01XXXXXXXXXXXX","price":12.34}'
```

**Cómo ejecutar localmente**

- Requisitos:
	- JDK 21 instalado (ej. Eclipse Temurin / Adoptium)
	- Maven (o usar el wrapper/ubicación absoluta si no está en PATH)
	- MySQL (si se quiere conectar a una base MySQL). Para pruebas automáticas se usa H2 en memoria.

- Opciones rápidas:

1) Usando el script incluido (PowerShell) `scripts/build-and-run.ps1`:

```powershell
cd C:\Users\sandra\Prueba-Java
.\scripts\build-and-run.ps1
```

El script compila (`mvn clean package`) y arranca el JAR; genera `app-start.log` y `app-start.err` en la raíz.

2) Manual (Maven + java -jar):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot'
& 'C:\Program Files\Apache\maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd' clean package -DskipTests
java -jar target\prueba-0.0.1-SNAPSHOT.jar
```

La aplicación arranca en `http://localhost:8081/`.

**Configurar base de datos**
- Archivo principal: `src/main/resources/application.properties`.
- Propiedades típicas para MySQL (ejemplo):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bankinc_db
spring.datasource.username=bankinc_user
spring.datasource.password=secret
spring.jpa.hibernate.ddl-auto=update
```

- Para ejecutar pruebas, el proyecto usa H2 en memoria (no requiere configuración adicional).

**Pruebas**
- Ejecutar todas las pruebas (unit + integración) con:

```powershell
& 'C:\Program Files\Apache\maven-mvnd-1.0.3-windows-amd64\mvn\bin\mvn.cmd' test
```

- Las pruebas unitarias para servicios se encuentran en `src/test/java/com/bankinc/prueba/service` (`CardServiceTest`, `TransactionServiceTest`) y usan **Mockito**.

**Problemas conocidos y soluciones**
- Maven no en PATH: el script y la documentación muestran cómo usar la ruta absoluta de Maven o ejecutar `scripts/update-maven-path.ps1`.
- Error de empaquetado por JAR bloqueado: si `mvn package` falla porque el JAR no puede ser renombrado, probablemente haya una instancia Java ejecutando el JAR. Localiza y deténla (PowerShell):

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Select-Object ProcessId, CommandLine | Format-List
Stop-Process -Id <PID> -Force
```

- Serialización Jackson + Hibernate proxies: las entidades usan `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` para evitar errores al devolver objetos con relaciones LAZY en respuestas JSON.

**Contribuciones**
- Pull requests bienvenidos. Antes de crear PR asegúrate de:
	- Ejecutar `mvn -DskipTests package` y comprobar que el JAR se genera.
	- Ejecutar `mvn test` y que las pruebas pasen (o documentar fallos esperados).

**Limpieza del repositorio**
- Recomiendo añadir un `.gitignore` que excluya `target/`, `*.log`, y archivos temporales. Ejemplo básico:

```
target/
*.log
.idea/
.vscode/
```

**Licencia**
- Añade aquí la licencia que prefieras (MIT es una opción común). Si prefieres, la puedo añadir por ti.

---
Si quieres, puedo:
- añadir un `.gitignore` y limpiar `target/` del historial en un commit separado; o
- generar un `README` traducido a inglés; o
- documentar endpoints con ejemplos `curl` más detallados y esquemas de request/response.

¿Qué prefieres que haga a continuación?
