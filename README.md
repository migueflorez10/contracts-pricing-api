# Contracts & Pricing API

API REST para gestionar contratos de venta y condiciones de precio en nueve países de Latinoamérica (AR, CO, CR, EC, MX, SV, NI, GT, PE). Está construida con **Java 17, Spring Boot 3 y Arquitectura Limpia**.

El dominio se basa en un proceso real de Order to Cash: los contratos pasan por aprobación, las cantidades se pueden ampliar sin aprobación, los cambios de vigencia sí la requieren, y el precio neto de un material depende de condiciones generales del país o propias de cada cliente.

![CI](https://github.com/migueflorez10/contracts-pricing-api/actions/workflows/ci.yml/badge.svg)

## Funcionalidades

**Contratos**
- Crear un contrato con una o varias posiciones (material, cantidad y precio unitario). Todo contrato nuevo queda pendiente de aprobación.
- Aprobar o rechazar un contrato.
- Sumar cantidad a una posición. La cantidad se suma a la existente (10.000 + 10.000 = 20.000) y no requiere aprobación.
- Solicitar un cambio de la fecha de fin. Queda pendiente hasta que se apruebe; si se rechaza, el contrato conserva la vigencia anterior.
- Cancelar un contrato.
- Un cliente no puede tener dos contratos abiertos (activos o pendientes) que se crucen en el mismo país.

**Precios**
- Registrar precios base y descuentos porcentuales, generales del país o propios de un cliente.
- No se permiten dos condiciones del mismo tipo que se crucen en fechas para el mismo país, material y cliente.
- Calcular el precio neto de un material para un cliente en una fecha: `precio base × (1 − descuento / 100)`. La condición propia del cliente tiene prioridad sobre la general.
- La moneda se toma del país (COP, USD, MXN, GTQ, etc.).

## Arquitectura

El proyecto sigue la estructura de Arquitectura Limpia del [scaffold de Bancolombia](https://github.com/bancolombia/scaffold-clean-architecture), en un proyecto Maven multimódulo:

```
contracts-pricing-api
├── domain
│   ├── model                  Entidades, objetos de valor, reglas de negocio y gateways (puertos). Java puro.
│   └── usecase                Casos de uso que orquestan el dominio. Java puro.
├── infrastructure
│   ├── driven-adapters
│   │   └── jpa-repository     Implementa los gateways con Spring Data JPA, PostgreSQL y Flyway.
│   └── entry-points
│       └── api-rest           Controladores REST, DTOs, validaciones, manejo de errores y OpenAPI.
├── applications
│   └── app-service            Punto de arranque: une los módulos e inyecta las dependencias.
├── deployment                 Dockerfile
└── docs                       Diagramas
```

Reglas que se respetan:
- `model` y `usecase` no dependen de Spring ni de ninguna librería externa. Las reglas de negocio se prueban sin levantar el framework.
- Los casos de uso solo conocen las interfaces (`ContractRepository`, `PriceConditionRepository`). La implementación con JPA se inyecta desde `app-service`.
- Los errores de negocio se clasifican en el dominio (no encontrado, regla de negocio, conflicto) y el punto de entrada los traduce a HTTP (404, 422, 409).

Los diagramas de arquitectura, casos de uso, clases, entidad-relación, estados y secuencia están en [docs/diagramas.md](docs/diagramas.md).

## Tecnologías

| Área | Herramientas |
| --- | --- |
| Lenguaje y framework | Java 17, Spring Boot 3.4 (Web, Validation, Data JPA, Actuator) |
| Persistencia | PostgreSQL 16, Flyway, H2 (pruebas y perfil local) |
| Documentación de la API | springdoc-openapi (Swagger UI) |
| Pruebas | JUnit 5, Mockito, AssertJ, MockMvc, `@DataJpaTest`, JaCoCo |
| Contenedores | Docker (build multietapa), Docker Compose |
| Integración continua | GitHub Actions |

## Cómo ejecutarlo

### Requisitos
- JDK 17 o 21
- Maven 3.9 o superior
- Docker (solo para la opción con PostgreSQL)

### Opción 1: con Docker Compose (PostgreSQL)

```bash
docker compose up --build
```

La API queda en `http://localhost:8080`.

### Opción 2: local con base de datos en memoria (H2)

```bash
mvn clean package
java -jar applications/app-service/target/app-service.jar --spring.profiles.active=local
```

### Documentación interactiva

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI (JSON): http://localhost:8080/v3/api-docs
- Salud del servicio: http://localhost:8080/actuator/health

## Pruebas

```bash
mvn verify
```

Se ejecutan:
- **Pruebas unitarias del dominio**: reglas de contratos, vigencias y condiciones de precio.
- **Pruebas de casos de uso** con Mockito, aisladas de la base de datos.
- **Pruebas de la capa web** con `@WebMvcTest`: validaciones, códigos HTTP y formato de errores.
- **Pruebas del adaptador JPA** con `@DataJpaTest` sobre H2 y las migraciones de Flyway.
- **Prueba de integración** de punta a punta con `@SpringBootTest`.

El reporte de cobertura de JaCoCo de cada módulo queda en `target/site/jacoco/index.html`.

## Endpoints

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/v1/contracts` | Crear contrato |
| GET | `/api/v1/contracts?country=&status=` | Listar contratos |
| GET | `/api/v1/contracts/{id}` | Consultar contrato |
| POST | `/api/v1/contracts/{id}/approve` | Aprobar contrato o cambio pendiente |
| POST | `/api/v1/contracts/{id}/reject` | Rechazar contrato o cambio pendiente |
| PATCH | `/api/v1/contracts/{id}/items/{materialCode}` | Sumar cantidad a una posición |
| POST | `/api/v1/contracts/{id}/validity-change` | Solicitar cambio de vigencia |
| POST | `/api/v1/contracts/{id}/cancel` | Cancelar contrato |
| POST | `/api/v1/price-conditions` | Registrar precio o descuento |
| GET | `/api/v1/price-conditions?country=&type=&materialCode=` | Listar condiciones |
| GET | `/api/v1/price-conditions/{id}` | Consultar condición |
| GET | `/api/v1/prices/net?country=&materialCode=&customerCode=&date=` | Calcular precio neto |

### Ejemplo de uso

```bash
# 1. Crear un contrato
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Content-Type: application/json" \
  -d '{
    "customerCode": "C-1001",
    "country": "SV",
    "validFrom": "2026-01-01",
    "validTo": "2026-06-30",
    "items": [
      {"materialCode": "CEM-50KG", "quantity": 10000, "unitPrice": 9.50}
    ]
  }'

# 2. Aprobarlo
curl -X POST http://localhost:8080/api/v1/contracts/1/approve

# 3. Sumar 2.500 unidades a la posición (no requiere aprobación)
curl -X PATCH http://localhost:8080/api/v1/contracts/1/items/CEM-50KG \
  -H "Content-Type: application/json" \
  -d '{"additionalQuantity": 2500}'

# 4. Registrar el precio general y un descuento del cliente
curl -X POST http://localhost:8080/api/v1/price-conditions \
  -H "Content-Type: application/json" \
  -d '{"type": "PRICE", "country": "SV", "materialCode": "CEM-50KG", "amount": 9.80, "validFrom": "2026-01-01", "validTo": "2026-12-31"}'

curl -X POST http://localhost:8080/api/v1/price-conditions \
  -H "Content-Type: application/json" \
  -d '{"type": "DISCOUNT", "country": "SV", "customerCode": "C-1001", "materialCode": "CEM-50KG", "amount": 5, "validFrom": "2026-01-01", "validTo": "2026-12-31"}'

# 5. Calcular el precio neto: 9.80 x (1 - 0.05) = 9.31 USD
curl "http://localhost:8080/api/v1/prices/net?country=SV&customerCode=C-1001&materialCode=CEM-50KG&date=2026-05-15"
```

### Formato de error

```json
{
  "code": "CONTRACT_OVERLAP",
  "message": "El cliente ya tiene un contrato vigente o pendiente en ese periodo: C-1001",
  "details": [],
  "path": "/api/v1/contracts",
  "timestamp": "2026-11-20T10:15:30"
}
```

| Código HTTP | Cuándo |
| --- | --- |
| 400 | Campos faltantes o con formato inválido |
| 404 | El contrato, la condición o el precio no existen |
| 409 | Conflicto con un contrato o condición existente |
| 422 | Se incumple una regla de negocio (fechas invertidas, transición de estado no permitida, etc.) |

## Autor

Miguel Ángel Martínez Flórez · [LinkedIn](https://www.linkedin.com/in/miguelmartinez-dataanalyst) · [GitHub](https://github.com/migueflorez10)
