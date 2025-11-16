# TFG System

Diseño e implementación de un sistema autónomo para la remediación automática de vulnerabilidades en entornos cloud. Este repositorio acompaña el Trabajo Final de Grado (TFG) de Santiago Pulido Manzano, titulado **“Diseño e Implementación de un Agente Auto-replicable para la Remediación Automática de Vulnerabilidades en Entornos Cloud”**. El objetivo es validar la viabilidad de un enfoque híbrido que combine agentes auto-replicables con una plataforma centralizada de monitoreo, documentación y orquestación.

---

## Visión General

- **Problema**: La gestión manual de vulnerabilidades en infraestructuras cloud incrementa tiempos de respuesta (MTTR) y riesgo de errores humanos.
- **Propuesta**: Un conjunto de agentes autónomos capaces de desplegarse, replicarse, ejecutar tareas de análisis y remediación, y autodestruirse cuando corresponde; todo coordinado por una plataforma central accesible vía API y dashboard web.
- **Metas**:
  - Reducir el tiempo de exposición frente a amenazas.
  - Mejorar la trazabilidad de acciones de seguridad mediante reportes estructurados.
  - Facilitar la adopción de automatización y prácticas DevSecOps en organizaciones de cualquier tamaño.

---

## Arquitectura del Sistema

```mermaid
graph TD
    UI[Dashboard web (Angular 17)] -->|HTTP/HTTPS| API[API Spring Boot]
    API -->|Persistencia| DB[(MongoDB)]
    Agent[Agente Standalone\nSpring Boot + GraalVM] -->|TLS + API Key| API
    API -->|Alertas/Reportes| UI
```

1. **Plataforma central** (carpeta `api/` y `ui/`):
   - Exposición de endpoints REST, autenticación JWT, gestión de organizaciones, targets, usuarios y políticas.
   - Dashboard web con métricas, alertas y administración del ecosistema.
   - Persistencia documental en MongoDB.
2. **Agente auto-replicable** (carpeta `agents/unix/`):
   - Aplicación Spring Boot en modo `WebApplicationType.NONE`, orientada a ejecución en entornos Linux/Unix.
   - Polling periódica al backend para obtener trabajos, ejecutar remediaciones y reportar resultados.
   - Preparada para compilar a imagen nativa con GraalVM para reducir tiempos de arranque y huella.

---

## Componentes Principales

### 1. API (`api/`)
- **Stack**: Java 17, Spring Boot 3.1, Spring Security, MongoDB, JWT.
- **Características**:
  - Autenticación vía `/authenticate` y registro inicial.
  - CRUD de organizaciones, usuarios, targets y plantillas.
  - Emisión de eventos y alertas, integración con correo electrónico (SMTP configurable).
- **Configuración**: Variables en `src/main/resources/application.properties` con respaldo a variables de entorno:
  - `MONGODB_URI`, `MONGODB_DATABASE_NAME`, `JWT_SECRET`, `ALLOWED_ORIGINS`, `APPLICATION_DOMAIN`, `MAIL_*`.

### 2. UI (`ui/`)
- **Stack**: Angular 17, PrimeNG, PrimeFlex, TailwindCSS, Chart.js.
- **Funcionalidad**: Dashboard administrativo para monitorear agentes, organizaciones, reportes y alertas. Incluye flujos de autenticación, selección de proyectos y gestión de plantillas.
- **Internacionalización**: Archivos `messages.json` / `messages.es.json`; el idioma base es inglés con soporte a español.

### 3. Agente Standalone (`agents/unix/`)
- **Stack**: Java 17, Spring Boot 3.5 (modo standalone), GraalVM Native Image.
- **Módulos**:
  - `WorkerCoordinator`: agenda y coordina trabajos recibidos de la plataforma central.
  - `AgentLifecycle`: controla el ciclo de vida y auto-detención del agente.
  - `WorkerPoolConfig`: gestiona el pool de hilos para ejecución concurrente.
- **Construcción nativa**: Script `package-macos.sh` y perfil Maven `-Pnative` para compilar a binario nativo.

---

## Requisitos Previos

| Herramienta | Versión recomendada | Uso |
|-------------|---------------------|-----|
| Java JDK    | 17 (Liberica, Temurin o GraalVM) | API y agente |
| Maven       | 3.9.x               | Construcción backend y agente |
| Node.js     | 18 LTS              | Construcción UI |
| npm         | 9.x                 | Gestión de dependencias UI |
| MongoDB     | 6.x (local o cloud) | Persistencia |
| GraalVM JDK | 21.0.x (opcional)   | Compilación nativa del agente |

> Nota: Para desarrollar sobre macOS, Linux o WSL. En Windows se recomienda WSL2 para compatibilidad con scripts de shell.

---

## Puesta en Marcha Rápida

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/locknet11/tfg-system.git
   cd tfg-system
   ```
2. **Configurar MongoDB**
   - Crear base de datos `tfg-system` o ajustar `MONGODB_DATABASE_NAME`.
   - Asegurar que la URI sea accesible desde API y agente.
3. **Definir variables de entorno** (ejemplo Unix/macOS):
   ```bash
   export MONGODB_URI="mongodb://localhost:27017"
   export JWT_SECRET="cambia-este-valor"
   export ALLOWED_ORIGINS="http://localhost:4200"
   export APPLICATION_DOMAIN="http://localhost:4200"
   export MAIL_HOST="smtp.gmail.com"
   export MAIL_USERNAME="usuario"
   export MAIL_PASSWORD="clave"
   ```
4. **Seleccionar entorno**
   - Desarrollo local: API + UI + agente en modo JVM.
   - Validación de rendimiento: Compilar agente con GraalVM y ejecutar binario.

---

## Construcción y Ejecución por Componente

### API (Spring Boot)

```bash
cd api
./mvnw clean package
./mvnw spring-boot:run
```

- Ejecuta en `http://localhost:8080`.
- El usuario administrador inicial puede configurarse vía scripts o directamente en la base de datos.
- Logs disponibles en consola. Se recomienda habilitar TLS y configurar dominios en despliegues productivos.

### UI (Angular)

```bash
cd ui
npm ci
npm run start
```

- Servida en `http://localhost:4200`.
- Usa proxy hacia `http://localhost:8080` (ajustar `environment.ts` si cambia el backend).
- Comandos adicionales:
  - `npm run build` genera artefactos de producción (`dist/`).
  - `npm run test` ejecuta unit tests (Karma + Jasmine).

### Agente Standalone (Spring Boot / GraalVM)

```bash
cd agents/unix
./mvnw clean package  # Ejecuta en modo JVM
java -jar target/agent-0.0.1-SNAPSHOT.jar --api.url=https://backend
```

- Para imagen nativa en macOS (requiere GraalVM instalado y variables en `package-macos.sh`):
  ```bash
  cd agents/unix
  sh package-macos.sh
  ./target/agent  --api.url=https://backend
  ```
- El agente realiza `polling` cada 10 segundos (`WorkerCoordinator`) y puede autodestruirse utilizando `AgentLifecycle` cuando la plataforma lo indique.

---

## Estructura del Repositorio

```
.
├── api/              # API REST Spring Boot
│   ├── src/main/java/com/spulido/tfg/...  # Controladores, servicios, seguridad
│   ├── src/main/resources/               # application.properties, scripts
│   └── src/test/java/                    # Pruebas unitarias
├── ui/               # Dashboard Angular 17
│   ├── src/app/      # Módulos, servicios y componentes
│   ├── src/assets/   # Estilos, imágenes, i18n
│   └── angular.json  # Configuración Angular CLI
├── agents/
│   └── unix/         # Agente standalone + configuración GraalVM
└── mockups/          # Prototipos de interfaz
```

---

## Entornos y Configuración

- **Variables sensibles**: No se almacenan en código fuente; usar `.env` locales o gestores de secretos.
- **CORS**: Configurar `ALLOWED_ORIGINS` para dominios válidos de la UI.
- **Correo electrónico**: Configurar SMTP para notificaciones (`MAIL_*`).
- **Logs y trazabilidad**: El agente registra acciones localmente y reporta al backend; la plataforma debe centralizar auditoría y backups (S3/Glacier recomendado en despliegue productivo).

---

## Metodología de Desarrollo y Validación

- Planificación Waterfall complementada con tableros Kanban.
- Pruebas en laboratorio AWS simulando despliegues multi-agente.
- Métricas evaluadas: tiempo de detección, éxito de remediaciones automáticas, estabilidad del agente, impacto en servicios críticos.

---

## Futuras Líneas de Trabajo

- Integrar priorización inteligente de vulnerabilidades con machine learning.
- Extender soporte multi-cloud (AWS, Azure, GCP) y ejecución en contenedores.
- Añadir integración con SIEM/SOAR y herramientas de ticketing para flujos de incidentes.
- Profundizar en hardening del agente (firmas, rotación de llaves, atestación remota).

---

## Referencias y Recursos

- [Repositorio del proyecto](https://github.com/locknet11/tfg-system)
- Reportes de referencia: NIST SP 800-53, OWASP Cloud-Native Application Security Top 10, IBM Cost of a Data Breach 2023.
- Documentación adicional en `ALERTS_IMPLEMENTATION_SUMMARY.md` y `ALERTS_MODULE.md`.

---

## Contacto

- Autor: **Santiago Pulido Manzano**
- Carrera: Licenciatura en Seguridad Informática — Universidad Siglo 21
- Año: 2025

Este README resume la arquitectura, componentes y procesos necesarios para experimentar con la solución propuesta. Para dudas o mejoras, abra un issue o contacte al autor.
