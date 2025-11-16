AGENTS Guide (Angular + Spring Boot)

- Modules: ui/ (Angular 17), api/ (Spring Boot 3), agents/unix/ (Spring Boot + GraalVM native).
- Build: ui → `cd ui && npm ci && npm run build`; api → `cd api && ./mvnw clean package`; agents/unix → `cd agents/unix && ./mvnw clean package`.
- Native agent (macOS/GraalVM): `cd agents/unix && sh package-macos.sh` or `./mvnw -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication`.
- Test (Java): run in api/ or agents/unix → `./mvnw test`; single test → `./mvnw -Dtest=ClassName#method test` (method optional).
- Test (UI): `cd ui && npm test`; single spec file → `npm test -- --include src/app/.../foo.spec.ts` (or use `fit`/`fdescribe`).
- Lint/format (UI): Prettier 3.2.5 present; check `npx prettier --check .`, write `npx prettier --write .` (see ui/.prettierrc.json).
- Lint (UI): No ESLint configured; rely on Angular strict TS and Prettier.
- Java style: no formatter plugin; follow standard Spring/Java conventions; use Lombok where present; avoid wildcard imports; order imports: `java.*`, `jakarta.*`, `org.*`, project packages, static last.
- TS style: strict mode enabled; avoid `any`; use explicit types/interfaces from `ui/src/app/**/models`; prefer `readonly` and `const`.
- Naming: TS files kebab-case; classes/components/services/pipes PascalCase; variables and functions camelCase; Observables suffix `$`.
- Imports: group as framework (Angular), third‑party, project local; prefer relative within feature; do not use wildcard imports.
- Error handling (UI): centralize HTTP errors in `src/app/shared/interceptors/request.interceptor.ts`; use `catchError` and rethrow with context; avoid `console.log` in production code.
- Error handling (API): validate inputs with `jakarta.validation` annotations; map exceptions via `@ControllerAdvice`; return meaningful HTTP statuses/bodies.
- Testing tips: make unit tests deterministic; avoid network/filesystem in unit scope; for Angular, use TestBed + spies, not real HTTP.
- i18n/UI: keep text in `ui/src/i18n/*.json`; do not hardcode user‑visible strings.
- Security: never commit secrets; use environment/config files only.
- Tools: No Cursor (.cursor/rules, .cursorrules) or Copilot instructions detected.
- Scope: This file applies to the entire repository rooted here.

---

# Diseño y Prototipado de un Sistema Autónomo de Ciberseguridad para Infraestructuras Cloud

---

## Índice

1. [Introducción](#introducción)
2. [Justificación y Problema](#justificación-y-problema)
3. [Objetivos](#objetivos)
4. [Estado del Arte y Marco Teórico](#estado-del-arte-y-marco-teórico)
5. [Descripción del Sistema Propuesto](#descripción-del-sistema-propuesto)
   - [Arquitectura General](#arquitectura-general)
   - [Agentes Auto-replicables](#agentes-auto-replicables)
   - [Plataforma Centralizada](#plataforma-centralizada)
6. [Requisitos Funcionales y No Funcionales](#requisitos-funcionales-y-no-funcionales)
7. [Metodología y Entorno de Validación](#metodología-y-entorno-de-validación)
8. [Documentos y Reportes Generados](#documentos-y-reportes-generados)
9. [Conclusiones y Futuras Líneas de Trabajo](#conclusiones-y-futuras-líneas-de-trabajo)
10. [Bibliografía](#bibliografía)
11. [Anexos](#anexos)

---

## Introducción

El auge de la computación en la nube ha facilitado la gestión y escalabilidad de infraestructuras TI. Sin embargo, también ha introducido desafíos considerables relacionados con la seguridad, especialmente en la detección y remediación temprana de vulnerabilidades. El presente proyecto propone el diseño e implementación de un sistema autónomo, auto-replicable y centralizado que permita fortalecer la ciberseguridad en entornos cloud, minimizando la intervención humana y reduciendo el tiempo de exposición ante amenazas.

---

## Justificación y Problema

### Contexto

La gestión manual de vulnerabilidades en infraestructuras cloud conlleva tiempos prolongados de respuesta y una elevada exposición ante posibles ataques. Diferentes incidentes recientes han demostrado la importancia de la automatización y orquestación para mitigar riesgos (Ver: [IBM Cost of a Data Breach Report 2023](https://www.ibm.com/reports/data-breach)).

### Necesidad

- Reducción del tiempo de detección y remediación de vulnerabilidades.
- Minimización de los errores humanos y omisiones en la gestión de seguridad.
- Escalabilidad para adaptarse a la elasticidad inherente del cloud computing.

---

## Objetivos

### Objetivo General

Desarrollar un sistema autónomo y distribuido capaz de detectar y remediar de manera automática vulnerabilidades en entornos cloud, centralizando la documentación y facilitando la administración del proceso.

### Objetivos Específicos

- Diseñar una arquitectura robusta de agentes auto-replicables y plataforma centralizada.
- Implementar mecanismos automatizados para escaneo, identificación y resolución de debilidades.
- Documentar y reportar, de forma estandarizada, todas las acciones realizadas para análisis y auditoría.

---

## Estado del Arte y Marco Teórico

#### Ciberseguridad en Cloud

- Referencia a marcos como NIST SP 800-53 y OWASP Cloud-Native Application Security Top 10.
- Herramientas existentes: Nessus, Qualys, OpenVAS (límites de intervención humana).

#### Agentes autónomos y auto-replicación

- Definición de agentes autónomos (Jennings, 2001; Wooldridge, 2009).
- Orquestación de agentes en arquitecturas cloud (Kubernetes, Docker Swarm).

#### Automatización y reporting en ciberseguridad

- Prácticas DevSecOps.
- Centralización de logs y reportes: ELK Stack, MongoDB, CloudWatch.

---

## Descripción del Sistema Propuesto

### Arquitectura General

```mermaid
flowchart TD
    subgraph Plataforma Centralizada
        A[API Backend]
        B[Base de Datos (MongoDB)]
        C[Dashboard Web]
    end

    subgraph Cloud AWS/Infraestructura Escalable
        D1[Agente Auto-Replicable 1]
        D2[Agente Auto-Replicable 2]
        D3[Agente Auto-Replicable N]
    end

    D1 -- Reportes y Log --> A
    D2 -- Reportes y Log --> A
    D3 -- Reportes y Log --> A
    A <--> B
    C <--> A
```

---

### Agentes Auto-replicables

#### Descripción

Los agentes son unidades autónomas que se despliegan automáticamente en instancias cloud de acuerdo a reglas de escalabilidad. Sus tareas principales:

- **Escaneo programado** de sistemas asignados (máquinas, contenedores, servicios).
- **Identificación de vulnerabilidades** (referencias a CVE/CVSS).
- **Validación y remediación automática** basada en catálogos de actualizaciones o parches.
- **Reporting estructurado**: envío de logs y reportes de actividades a la plataforma centralizada.

#### Funcionalidades

- Auto-deployment vía scripts Terraform/CloudFormation.
- Registro de logs locales temporales.
- Healthchecks y auto-destrucción al completar la tarea o ante fallos.

---

### Plataforma Centralizada

#### Descripción

Componente núcleo para administración, monitoreo y presentación de información.

#### Funcionalidades

- Recepción y almacenamiento de reportes generados por los agentes.
- Visualización de métricas clave y manejo de alertas.
- Gestión de usuarios (creación, autenticación, gestión de permisos).
- Exportación e histórico de reportes para auditoría.

#### Stack tecnológico sugerido

- **Frontend**: React.js / Angular.
- **Backend**: Node.js / Python (FastAPI, Flask).
- **Base de datos**: MongoDB (ventajas para almacenamiento de documentos estructurados).
- **Infraestructura cloud**: AWS (EC2, Lambda, S3) o Azure/GCP.

---

## Requisitos Funcionales y No Funcionales

### Requisitos Funcionales

- **RF-01:** Crear usuario administrador inicial (validación de datos, unicidad).
- **RF-02:** Generar, modificar y eliminar organizaciones clientes en el sistema.
- **RF-03:** Desplegar automáticamente agentes según políticas de escalabilidad.
- **RF-04:** Ejecutar escaneos programados y bajo demanda.
- **RF-05:** Aplicar remediaciones automáticas válidas detectadas y reportar acciones.
- **RF-06:** Generar y almacenar reportes estructurados (especificando timestamps, recursos afectados, severidad de hallazgos).
- **RF-07:** Visualizar métricas y reportes históricos en dashboards.

### Requisitos No Funcionales

- **RNF-01:** Escalabilidad para soportar múltiples agentes y organizaciones.
- **RNF-02:** Seguridad en el transporte y almacenamiento de datos (TLS/HTTPS, cifrado en MongoDB).
- **RNF-03:** Integridad y trazabilidad completa de logs.
- **RNF-04:** Disponibilidad ≥ 99.5% en despliegues de producción.
- **RNF-05:** Usabilidad en dashboards (accesibilidad web).

---

## Metodología y Entorno de Validación

- **Fase 1:** Desarrollo, testeo unitario e integración en ambientes locales.
- **Fase 2:** Despliegue y validación sobre entorno cloud controlado (ejemplo: sandbox de AWS).
- **Fase 3:** Análisis de resultados (comparativas de tiempo de detección y remediación en escenarios controlados).
- **Métricas clave:** Cantidad de vulnerabilidades detectadas, tiempos de respuesta, éxito en remediación automática y precisión de reportes.

#### Herramientas

- **Docker Desktop**: Simulación de entornos.
- **MongoDB Compass**: Monitorización y validación de datos.
- **AWS Console**: Despliegue de agentes y paneles de administración.

---

## Documentos y Reportes Generados

Los reportes generados quedarán almacenados en formato digital/documental en la base MongoDB y serán accesibles por web. Ejemplo de documentos:

```json
{
  "organizacion": "Org1",
  "timestamp": "2025-09-11T08:00:00Z",
  "agente_id": "AGT-001",
  "ip_objetivo": "172.31.128.4",
  "vulnerabilidades": [
    {
      "cve_id": "CVE-2025-12345",
      "descripcion": "Desbordamiento de búfer en sshd",
      "severidad": "Alta",
      "remediacion": {
        "accion": "Aplicación de parche",
        "estado": "Exitoso",
        "timestamp": "2025-09-11T08:15:00Z"
      }
    }
  ]
}
```

#### Accesibles vía dashboards con filtrado por organización, período, severidad, estado, etc.

---

## Conclusiones y Futuras Líneas de Trabajo

- **Beneficios del prototipo:** Automatización de tareas críticas de ciberseguridad y reducción significativa del tiempo de reacción ante amenazas.
- **Limitaciones:** Dependencias de conectividad a internet/cloud, posibles falsos positivos/negativos en escaneos.
- **Futuro:** Integración de aprendizaje automático para priorización dinámica de vulnerabilidades, soporte multiplataforma (AWS, Azure, GCP), integración con SIEM, ampliación de reportes con análisis forense.

---

## Bibliografía

1. NIST Special Publication 800-53 Revision 5 (2020)
2. Jennings, N. R. (2001). An Introduction to Agent-Based Systems.
3. Wooldridge, M. (2009). An Introduction to MultiAgent Systems.
4. OWASP Cloud-Native Application Security Top 10 [2023]
5. IBM. (2023). Cost of a Data Breach Report.
6. [Agregar aquí otras fuentes y referencias empleadas.]

---

## Anexos

- **A1:** Diagramas de flujo de procesos de escaneo y remediación.
- **A2:** Scripts de despliegue automatizado (ejemplo: Terraform).
- **A3:** Ejemplos de reportes históricos y dashboards.
- **A4:** Código de ejemplo de agente auto-replicable y API centralizada.

---
