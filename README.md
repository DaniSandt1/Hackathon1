# 🍪 Hackathon 1 — Oreo Insight Factory
### JWT Roles · Ventas CRUD · Summary Async · LLM Integration · Premium Report

---

## 👥 Equipo

| Integrante | Código UTEC |
|-------------|-------------|
| Daniel Guillermo Sandoval Toro | 202310533 |
| Ray Sebastian Bolaños Aedo  |             |
| Sebastian Cangalaya Martinez |             |
| Fabrizio Leandro Gonzales Nuñez  |             |
| Miguel Angel Champi Hinojosa |             |



---

## 🚀 Instrucciones de ejecución

### 1️⃣ Clonar el repositorio
```bash
git clone https://github.com/<usuario>/insight-factory.git
cd insight-factory
```

### 2️⃣ Configurar variables de entorno

Crea un archivo `.env` en el root del proyecto con:
```bash
MAIL_USERNAME=correo@gmail.com
MAIL_PASSWORD=contraseña_app
LLM_TOKEN=token_github_models
```

---

## ⚙️ Variables requeridas (para Canvas)

```
MAIL_USERNAME=xxxx@gmail.com
MAIL_PASSWORD=xxxxxx
LLM_TOKEN=ghp_xxxxxxxxxxxxxx
```

---

## 🧩 Cómo ejecutar el proyecto

### Opción 1 — Desde IntelliJ / IDE
1. Abre el proyecto en IntelliJ IDEA.  
2. Espera que se descarguen las dependencias Maven.  
3. Ejecuta la clase principal:  
   **`OreoHk1Application.java`**  
   (click derecho → `Run 'OreoHk1Application'`)

Luego abre en tu navegador:
```
http://localhost:8080/h2-console
```

Con:
- JDBC URL: `jdbc:h2:mem:oreo_db`
- User: `sa`
- Password: *(vacío)*

---

## 🧠 Instrucciones Postman (Workflow)

Importa el archivo:
```
postman_collection.json
```

### Secuencia validada:
1. **Registro CENTRAL**
2. **Login CENTRAL (guarda token)**
3. **Registro BRANCH (Miraflores)**
4. **Login BRANCH (guarda token_branch)**
5(.1 a .5). **Crear 5 ventas (CENTRAL)** → todas con `201`
6. **Listar todas las ventas (CENTRAL)** → `200`
7. **Listar ventas (BRANCH)** → solo Miraflores
8. **Resumen semanal (BRANCH)** → `202` (envía correo)
 ✉️ Importante — Configuración del correo de destino
⚠️ Antes de ejecutar el paso en Postman, editar el body del request y reemplaza el campo "emailTo" con correo propio.
9. **Intentar crear venta otra sucursal (BRANCH)** → `403`
10. **Eliminar venta (CENTRAL)** → `204`

---

## ⚡ Implementación Asíncrona

El procesamiento del resumen semanal se realiza con eventos y tareas asíncronas:

```java
@SpringBootApplication
@EnableAsync
public class OreoHk1Application {}

@Component
public class SummaryEventListener {
    @Async
    @EventListener
    public void handle(SummaryRequestedEvent event) {
        processor.handlePremiumReport(event.getRequest());
    }
}
```

Esto garantiza que el envío del reporte (PDF + HTML + LLM) se procese en segundo plano mientras la API responde con `202 Accepted`.

---

## 🧾 Endpoint Premium

**POST /sales/summary/weekly/premium**

### Request:
```json
{
  "from": "2025-09-01",
  "to": "2025-09-07",
  "branch": "Miraflores",
  "emailTo": "correo@empresa.com",
  "format": "PREMIUM",
  "includeCharts": true,
  "attachPdf": true
}
```

### Response:
```json
{
  "requestId": "req_01JABC...",
  "status": "PROCESSING",
  "message": "Su reporte premium está siendo generado. Incluirá gráficos y PDF adjunto.",
  "estimatedTime": "60-90 segundos"
}
```

---

## 🧩 Stack Tecnológico

| Componente | Tecnología |
|-------------|-------------|
| Backend | Java 21 + Spring Boot 3.4 |
| Seguridad | Spring Security + JWT |
| BD | H2 en memoria |
| Asincronía | @Async + ApplicationEventPublisher |
| LLM | GitHub Models (gpt-5-mini) |
| PDF | Apache PDFBox |
| Email | Spring Boot Mail (SMTP) |
| Testing | JUnit 5 + Mockito |
| CI/CD | Maven |

---

## 🧪 Testing Unitario

Los tests unitarios se encuentran en la clase:
```
src/test/java/com/oreo/insight_factory/service/SalesAggregationServiceTest.java
```

Ejecuta el test con:
- Click derecho → **Run 'SalesAggregationServiceTest'**  
  *(desde IntelliJ o Eclipse)*

### Cobertura:
- Total de unidades vendidas  
- Total de ingresos  
- SKU más vendido  
- Sucursal top  
- Caso sin ventas (retorna 0)

---

## 🏁 Estado Final

✅ Autenticación JWT  
✅ Roles CENTRAL/BRANCH  
✅ CRUD completo de ventas  
✅ Resumen semanal asíncrono  
✅ Email HTML + PDF  
✅ LLM prompt corto y validado  
✅ Postman Collection funcional  
✅ 5 tests unitarios mínimos  
✅ Ejecución directa desde `OreoHk1Application`

---

© 2025 · Hackathon #1 — Oreo Insight Factory 🍪
