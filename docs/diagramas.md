# Diagramas

Todos los diagramas están en Mermaid, así que GitHub los muestra directamente.

## 1. Arquitectura (capas y dependencias)

Las dependencias apuntan siempre hacia el dominio. El dominio no conoce Spring, JPA ni HTTP.

```mermaid
flowchart LR
    subgraph applications
        APP[app-service<br/>arranque y configuración]
    end
    subgraph infrastructure
        EP[entry-points/api-rest<br/>controladores REST]
        DA[driven-adapters/jpa-repository<br/>Spring Data JPA + Flyway]
    end
    subgraph domain
        UC[usecase<br/>casos de uso]
        M[model<br/>entidades, reglas y gateways]
    end
    DB[(PostgreSQL)]
    CLIENT([Cliente HTTP]) --> EP
    APP --> EP
    APP --> DA
    EP --> UC
    UC --> M
    DA -. implementa gateways .-> M
    DA --> DB
```

## 2. Casos de uso

```mermaid
flowchart LR
    COM([Analista comercial])
    APR([Aprobador])
    PRI([Analista de precios])

    subgraph Sistema de contratos y precios
        UC1(Crear contrato)
        UC2(Consultar contratos)
        UC3(Sumar cantidad a una posición)
        UC4(Solicitar cambio de vigencia)
        UC5(Aprobar o rechazar)
        UC6(Cancelar contrato)
        UC7(Registrar precio o descuento)
        UC8(Calcular precio neto)
    end

    COM --> UC1
    COM --> UC2
    COM --> UC3
    COM --> UC4
    COM --> UC6
    APR --> UC5
    PRI --> UC7
    PRI --> UC8
    COM --> UC8
```

## 3. Diagrama de clases del dominio

```mermaid
classDiagram
    class Contract {
        -Long id
        -String customerCode
        -Country country
        -ValidityPeriod validity
        -LocalDate pendingValidTo
        -ContractStatus status
        -String rejectionReason
        -List~ContractItem~ items
        +create(customerCode, country, validity, items)$ Contract
        +approve()
        +reject(reason)
        +addQuantity(materialCode, quantity)
        +requestValidityChange(newValidTo)
        +cancel()
        +isOpen() boolean
        +total() BigDecimal
    }
    class ContractItem {
        <<record>>
        String materialCode
        BigDecimal quantity
        BigDecimal unitPrice
        +addQuantity(quantity) ContractItem
        +total() BigDecimal
    }
    class ValidityPeriod {
        <<record>>
        LocalDate validFrom
        LocalDate validTo
        +contains(date) boolean
        +overlaps(other) boolean
    }
    class PriceCondition {
        <<record>>
        Long id
        ConditionType type
        Country country
        String customerCode
        String materialCode
        BigDecimal amount
        ValidityPeriod validity
        +appliesTo(customer, date) boolean
        +overlapsWith(other) boolean
    }
    class ContractStatus {
        <<enum>>
        PENDING_APPROVAL
        ACTIVE
        REJECTED
        CANCELLED
    }
    class ConditionType {
        <<enum>>
        PRICE
        DISCOUNT
    }
    class Country {
        <<enum>>
        AR
        CO
        CR
        EC
        MX
        SV
        NI
        GT
        PE
        +getCurrency() String
    }
    class ContractRepository {
        <<interface>>
        +save(contract) Contract
        +findById(id) Optional~Contract~
        +findAll(country, status) List~Contract~
        +findByCustomerAndCountry(customer, country) List~Contract~
    }
    class PriceConditionRepository {
        <<interface>>
        +save(condition) PriceCondition
        +findById(id) Optional~PriceCondition~
        +findAll(country, type, material) List~PriceCondition~
        +findByTypeCountryAndMaterial(type, country, material) List~PriceCondition~
        +findValidOn(country, material, date) List~PriceCondition~
    }
    class ContractUseCase
    class PriceConditionUseCase
    class NetPriceUseCase

    Contract "1" *-- "1..*" ContractItem
    Contract --> ValidityPeriod
    Contract --> ContractStatus
    Contract --> Country
    PriceCondition --> ValidityPeriod
    PriceCondition --> ConditionType
    PriceCondition --> Country
    ContractUseCase --> ContractRepository
    PriceConditionUseCase --> PriceConditionRepository
    NetPriceUseCase --> PriceConditionRepository
```

## 4. Modelo entidad-relación

```mermaid
erDiagram
    CONTRACTS ||--|{ CONTRACT_ITEMS : contiene
    CONTRACTS {
        bigint id PK
        varchar customer_code
        varchar country
        date valid_from
        date valid_to
        date pending_valid_to
        varchar status
        varchar rejection_reason
        timestamp created_at
        timestamp updated_at
    }
    CONTRACT_ITEMS {
        bigint id PK
        bigint contract_id FK
        varchar material_code
        numeric quantity
        numeric unit_price
    }
    PRICE_CONDITIONS {
        bigint id PK
        varchar condition_type
        varchar country
        varchar customer_code "nulo = condición general"
        varchar material_code
        numeric amount
        date valid_from
        date valid_to
        timestamp created_at
    }
```

## 5. Estados de un contrato

```mermaid
stateDiagram-v2
    [*] --> PENDING_APPROVAL: crear
    PENDING_APPROVAL --> ACTIVE: aprobar
    PENDING_APPROVAL --> REJECTED: rechazar contrato nuevo
    PENDING_APPROVAL --> ACTIVE: rechazar cambio de vigencia
    ACTIVE --> ACTIVE: sumar cantidad
    ACTIVE --> PENDING_APPROVAL: solicitar cambio de vigencia
    ACTIVE --> CANCELLED: cancelar
    PENDING_APPROVAL --> CANCELLED: cancelar
    REJECTED --> [*]
    CANCELLED --> [*]
```

Sumar cantidad no requiere aprobación. Si se rechaza un cambio de vigencia, el contrato sigue activo con la vigencia anterior.

## 6. Secuencia: cambio de vigencia con aprobación

```mermaid
sequenceDiagram
    actor Analista
    actor Aprobador
    participant API as ContractController
    participant UC as ContractUseCase
    participant C as Contract
    participant Repo as ContractRepository

    Analista->>API: POST /contracts/{id}/validity-change
    API->>UC: requestValidityChange(id, newValidTo)
    UC->>Repo: findById(id)
    Repo-->>UC: contrato ACTIVE
    UC->>C: requestValidityChange(newValidTo)
    C-->>UC: queda PENDING_APPROVAL con pendingValidTo
    UC->>Repo: save(contrato)
    API-->>Analista: 200 OK

    Aprobador->>API: POST /contracts/{id}/approve
    API->>UC: approve(id)
    UC->>C: approve()
    C-->>UC: aplica la nueva fecha y vuelve a ACTIVE
    UC->>Repo: save(contrato)
    API-->>Aprobador: 200 OK
```
