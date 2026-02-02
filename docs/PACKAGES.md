# Informe jerárquico de paquetes para jumibot/MVCGameEngine — rama: develop — generado el 2026-02-02.

## Micro-resumen

El repositorio presenta un engine MVC concurrente con separación clara entre simulación (Model), coordinación (Controller) y presentación (View). La lógica de entidades, físicas y eventos se concentra en el paquete `engine`, mientras que los paquetes de primer nivel (`ai`, `level`, `rules`, `world`, `assets`) aportan configuración y reglas específicas para un juego concreto.

Los patrones dominantes son MVC, DTO, Strategy (físicas/armas), Factory/Provider (definiciones de mundo y assets) y Template Method (generadores y bases abstractas). Las interfaces en paquetes `*.ports` se usan como contratos de desacoplamiento entre reglas, modelo y definición de mundo, mientras que las implementaciones concretas viven en `*.impl` o `*.implementations`.

El acoplamiento entre paquetes de primer nivel se concentra en `engine`: `ai`, `level` y `rules` dependen de `engine.*` (principalmente `engine.controller.ports` y `engine.worlddef.ports`) para evitar acoplamiento directo a implementaciones. `assets` y `world` aportan datos y definiciones que el `engine` consume vía DTOs y puertos, mientras `Main` (paquete por defecto) orquesta el wiring inicial.

**Nota**: existe un paquete por defecto con el entrypoint [src/Main.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/Main.java).

## Paquetes de primer nivel

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| ai | Spawner de entidades dinámicas mediante IA básica. | Crear entidades según definiciones; Iterar con delay configurable |
| assets | Catálogo y definiciones de assets visuales usados por el engine y los escenarios. | Registro y lookup de assets; Tipos e intensidades de assets; Bootstrap de catálogos concretos |
| engine | Núcleo MVC del engine con lógica de simulación, control, render y utilidades. | Coordinación MVC; Modelo físico y eventos; Render loop y HUD; Definición de mundos y generadores |
| level | Nivel básico basado en AbstractLevelGenerator. | Crear decoradores y estáticos; Crear jugador con armas y emisores |
| rules | Reglas concretas de eventos/acciones. | Resolución de colisiones; Rebotes y límites |
| world | Proveedores concretos de WorldDefinition. | Generar presets de mundo; Configurar assets y tipos |

## ai (`src/ai`)

Spawner de entidades dinámicas mediante IA básica.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| ai | Spawner de entidades dinámicas mediante IA básica. | Crear entidades según definiciones; Iterar con delay configurable |

**Análisis detallado**

- **Propósito:** Spawner de entidades dinámicas mediante IA básica.
- **Responsabilidades principales:** Crear entidades según definiciones, Iterar con delay configurable.
- **Interacción con otras capas/paquetes:** usa → engine.worlddef.ports (3, via ports), engine.controller.ports (1, via ports), engine.generators (1, acoplamiento directo); usado por ← default (Main) (1, acoplamiento directo).
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/ai/AIBasicSpawner.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/ai/AIBasicSpawner.java) — Clase/enum
- **Patrones de diseño relevantes:** Template Method
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

## assets (`src/assets`)

Catálogo y definiciones de assets visuales usados por el engine y los escenarios.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| assets.core | Catálogo de assets y lógica de registro/selección. | Registrar assets; Resolver IDs aleatorios por tipo e intensidad |
| assets.impl | Catálogo concreto del proyecto con registros de assets. | Carga de IDs y nombres de archivos; Define path base de assets |
| assets.ports | Tipos y DTOs base de assets. | Definir enums de tipo/intensidad; DTO con metadata |

**Análisis detallado**

- **Propósito:** Catálogo y definiciones de assets visuales usados por el engine y los escenarios.
- **Responsabilidades principales:** Registro y lookup de assets, Tipos e intensidades de assets, Bootstrap de catálogos concretos.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** DTO, Registry
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- El catálogo define la ruta base de imágenes y es un punto único de verdad para IDs.

### assets.core (`src/assets/core`)

Catálogo de assets y lógica de registro/selección.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| assets.core | Catálogo de assets y lógica de registro/selección. | Registrar assets; Resolver IDs aleatorios por tipo e intensidad |

**Análisis detallado**

- **Propósito:** Catálogo de assets y lógica de registro/selección.
- **Responsabilidades principales:** Registrar assets, Resolver IDs aleatorios por tipo e intensidad.
- **Interacción con otras capas/paquetes:** usa → assets.ports (3, via ports); usado por ← engine.worlddef.core (2, acoplamiento directo), assets.impl (1, acoplamiento directo), engine.controller.impl (1, acoplamiento directo), engine.controller.ports (1, via ports), engine.view.core (1, acoplamiento directo), engine.worlddef.ports (1, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/assets/core/AssetCatalog.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/assets/core/AssetCatalog.java) — Assets
- **Patrones de diseño relevantes:** Registry, DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### assets.impl (`src/assets/impl`)

Catálogo concreto del proyecto con registros de assets.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| assets.impl | Catálogo concreto del proyecto con registros de assets. | Carga de IDs y nombres de archivos; Define path base de assets |

**Análisis detallado**

- **Propósito:** Catálogo concreto del proyecto con registros de assets.
- **Responsabilidades principales:** Carga de IDs y nombres de archivos, Define path base de assets.
- **Interacción con otras capas/paquetes:** usa → assets.ports (2, via ports), assets.core (1, acoplamiento directo); usado por ← engine.worlddef.core (2, acoplamiento directo), world (2, acoplamiento directo), default (Main) (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/assets/impl/ProjectAssets.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/assets/impl/ProjectAssets.java) — Assets
- **Patrones de diseño relevantes:** Configuration Object
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### assets.ports (`src/assets/ports`)

Tipos y DTOs base de assets.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| assets.ports | Tipos y DTOs base de assets. | Definir enums de tipo/intensidad; DTO con metadata |

**Análisis detallado**

- **Propósito:** Tipos y DTOs base de assets.
- **Responsabilidades principales:** Definir enums de tipo/intensidad, DTO con metadata.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← assets.core (3, acoplamiento directo), engine.worlddef.core (3, acoplamiento directo), assets.impl (2, acoplamiento directo), world (2, acoplamiento directo), engine.view.core (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/assets/ports/AssetInfoDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/assets/ports/AssetInfoDTO.java) — DTO de datos
  - [src/assets/ports/AssetIntensity.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/assets/ports/AssetIntensity.java) — Assets
  - [src/assets/ports/AssetType.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/assets/ports/AssetType.java) — Assets
- **Patrones de diseño relevantes:** DTO, Enum
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

## engine (`src/engine`)

Núcleo MVC del engine con lógica de simulación, control, render y utilidades.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.actions | DTOs de acciones ejecutables por el engine. | Enumerar acciones; Transportar payloads de acción |
| engine.controller | Subdominio de coordinación MVC y mapeo a render. | Orquestar flujo Model/View; Exponer contratos del controller |
| engine.events | Subdominio de eventos de dominio del engine. | Modelar eventos y payloads; Exponer contratos de eventos |
| engine.generators | Generadores base para IA y niveles. | Esqueleto de generación; Materializar definiciones a DTOs |
| engine.model | Subdominio del modelo de simulación. | Gestión de entidades; Físicas, armas y emisores |
| engine.utils | Utilidades transversales del engine. | Imágenes, helpers, FX y spatial grid |
| engine.view | Subdominio de presentación y render. | Render loop; Renderables y HUD |
| engine.worlddef | Subdominio de definiciones de mundo. | Factories de definiciones; DTOs y providers |

**Análisis detallado**

- **Propósito:** Núcleo MVC del engine con lógica de simulación, control, render y utilidades.
- **Responsabilidades principales:** Coordinación MVC, Modelo físico y eventos, Render loop y HUD, Definición de mundos y generadores.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** MVC, DTO, Ports & Adapters, Strategy, Factory
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Contiene hot paths de física, render y generación de eventos.

### engine.actions (`src/engine/actions`)

DTOs de acciones ejecutables por el engine.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.actions | DTOs de acciones ejecutables por el engine. | Enumerar acciones; Transportar payloads de acción |

**Análisis detallado**

- **Propósito:** DTOs de acciones ejecutables por el engine.
- **Responsabilidades principales:** Enumerar acciones, Transportar payloads de acción.
- **Interacción con otras capas/paquetes:** usa → engine.events.domain.ports.eventtype (1, via ports), engine.model.bodies.ports (1, via ports); usado por ← rules (12, acoplamiento directo), engine.model.impl (2, acoplamiento directo), engine.controller.impl (1, acoplamiento directo), engine.controller.ports (1, via ports), engine.model.bodies.core (1, acoplamiento directo), engine.model.ports (1, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/actions/Action.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/actions/Action.java) — Acción
  - [src/engine/actions/ActionDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/actions/ActionDTO.java) — DTO de datos
- **Patrones de diseño relevantes:** DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### engine.controller (`src/engine/controller`)

Subdominio de coordinación MVC y mapeo a render.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.controller.impl | Implementación del coordinador MVC. | Orquestar Model/View; Aplicar reglas y ciclo de vida |
| engine.controller.mappers | Mappers de entidades del modelo a DTOs de render. | Convertir DTOs de bodies a renderables; Mapeo de armas/emisores |
| engine.controller.ports | Puertos del controlador para reglas y gestión del mundo. | Contratos para reglas (ActionsGenerator); Operaciones de gestión del mundo |

**Análisis detallado**

- **Propósito:** Subdominio de coordinación MVC y mapeo a render.
- **Responsabilidades principales:** Orquestar flujo Model/View, Exponer contratos del controller.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** MVC Controller
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.controller.impl (`src/engine/controller/impl`)

Implementación del coordinador MVC.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.controller.impl | Implementación del coordinador MVC. | Orquestar Model/View; Aplicar reglas y ciclo de vida |

**Análisis detallado**

- **Propósito:** Implementación del coordinador MVC.
- **Responsabilidades principales:** Orquestar Model/View, Aplicar reglas y ciclo de vida.
- **Interacción con otras capas/paquetes:** usa → engine.controller.mappers (6, acoplamiento directo), engine.view.renderables.ports (4, via ports), engine.controller.ports (3, via ports), engine.worlddef.ports (2, via ports), assets.core (1, acoplamiento directo), engine.actions (1, acoplamiento directo), engine.events.domain.ports.eventtype (1, via ports), engine.model.bodies.ports (1, via ports), engine.model.emitter.ports (1, via ports), engine.model.impl (1, acoplamiento directo), engine.model.ports (1, via ports), engine.model.weapons.ports (1, via ports), engine.utils.helpers (1, acoplamiento directo), engine.view.core (1, acoplamiento directo); usado por ← default (Main) (1, acoplamiento directo), engine.view.core (1, acoplamiento directo).
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/controller/impl/Controller.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/impl/Controller.java) — Coordinador MVC
- **Patrones de diseño relevantes:** MVC Controller, Facade
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Coordina el flujo principal del engine.

#### engine.controller.mappers (`src/engine/controller/mappers`)

Mappers de entidades del modelo a DTOs de render.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.controller.mappers | Mappers de entidades del modelo a DTOs de render. | Convertir DTOs de bodies a renderables; Mapeo de armas/emisores |

**Análisis detallado**

- **Propósito:** Mappers de entidades del modelo a DTOs de render.
- **Responsabilidades principales:** Convertir DTOs de bodies a renderables, Mapeo de armas/emisores.
- **Interacción con otras capas/paquetes:** usa → engine.view.renderables.ports (4, via ports), engine.model.bodies.ports (3, via ports), engine.worlddef.ports (3, via ports), engine.model.weapons.ports (2, via ports), engine.model.emitter.ports (1, via ports), engine.utils.spatial.ports (1, via ports); usado por ← engine.controller.impl (6, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/controller/mappers/DynamicRenderableMapper.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/mappers/DynamicRenderableMapper.java) — Mapper
  - [src/engine/controller/mappers/EmitterMapper.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/mappers/EmitterMapper.java) — Mapper
  - [src/engine/controller/mappers/PlayerRenderableMapper.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/mappers/PlayerRenderableMapper.java) — Mapper
  - [src/engine/controller/mappers/RenderableMapper.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/mappers/RenderableMapper.java) — Mapper
  - [src/engine/controller/mappers/SpatialGridStatisticsMapper.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/mappers/SpatialGridStatisticsMapper.java) — Mapper
  - [src/engine/controller/mappers/WeaponMapper.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/mappers/WeaponMapper.java) — Mapper
  - [src/engine/controller/mappers/WeaponTypeMapper.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/mappers/WeaponTypeMapper.java) — Mapper
- **Patrones de diseño relevantes:** Mapper/Adapter
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.controller.ports (`src/engine/controller/ports`)

Puertos del controlador para reglas y gestión del mundo.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.controller.ports | Puertos del controlador para reglas y gestión del mundo. | Contratos para reglas (ActionsGenerator); Operaciones de gestión del mundo |

**Análisis detallado**

- **Propósito:** Puertos del controlador para reglas y gestión del mundo.
- **Responsabilidades principales:** Contratos para reglas (ActionsGenerator), Operaciones de gestión del mundo.
- **Interacción con otras capas/paquetes:** usa → engine.worlddef.ports (2, via ports), assets.core (1, acoplamiento directo), engine.actions (1, acoplamiento directo), engine.events.domain.ports.eventtype (1, via ports), engine.utils.helpers (1, acoplamiento directo); usado por ← rules (6, acoplamiento directo), engine.controller.impl (3, acoplamiento directo), engine.generators (3, acoplamiento directo), engine.view.core (2, acoplamiento directo), default (Main) (1, acoplamiento directo), ai (1, acoplamiento directo), level (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/controller/ports/ActionsGenerator.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/ports/ActionsGenerator.java) — Acción
  - [src/engine/controller/ports/EngineState.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/ports/EngineState.java) — Clase/enum
  - [src/engine/controller/ports/WorldManager.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/controller/ports/WorldManager.java) — Definición de mundo
- **Patrones de diseño relevantes:** Ports & Adapters
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### engine.events (`src/engine/events`)

Subdominio de eventos de dominio del engine.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.events.domain | Eventos de dominio agrupados por core y ports. | Definir base de eventos; DTOs y tipos de eventos |

**Análisis detallado**

- **Propósito:** Subdominio de eventos de dominio del engine.
- **Responsabilidades principales:** Modelar eventos y payloads, Exponer contratos de eventos.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Event-Action
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.events.domain (`src/engine/events/domain`)

Eventos de dominio agrupados por core y ports.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.events.domain.core | Base de eventos de dominio. | Clases base de eventos; Normalizar acceso a payloads |
| engine.events.domain.ports | DTOs base para eventos de dominio. | Tipos y referencias de body; Contratos de eventos |

**Análisis detallado**

- **Propósito:** Eventos de dominio agrupados por core y ports.
- **Responsabilidades principales:** Definir base de eventos, DTOs y tipos de eventos.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Event-Action, DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.events.domain.core (`src/engine/events/domain/core`)

Base de eventos de dominio.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.events.domain.core | Base de eventos de dominio. | Clases base de eventos; Normalizar acceso a payloads |

**Análisis detallado**

- **Propósito:** Base de eventos de dominio.
- **Responsabilidades principales:** Clases base de eventos, Normalizar acceso a payloads.
- **Interacción con otras capas/paquetes:** usa → engine.events.domain.ports (2, via ports), engine.events.domain.ports.payloads (1, via ports); usado por ← engine.events.domain.ports.eventtype (4, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/events/domain/core/AbstractDomainEvent.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/core/AbstractDomainEvent.java) — Base abstracta
- **Patrones de diseño relevantes:** Event-Action
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.events.domain.ports (`src/engine/events/domain/ports`)

DTOs base para eventos de dominio.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.events.domain.ports.eventtype | Tipos concretos de eventos del dominio. | Modelar eventos de colisión, límites, emisión |
| engine.events.domain.ports.payloads | Payloads tipados para eventos. | Transportar datos específicos por evento |

**Análisis detallado**

- **Propósito:** DTOs base para eventos de dominio.
- **Responsabilidades principales:** Tipos y referencias de body, Contratos de eventos.
- **Interacción con otras capas/paquetes:** usa → engine.model.bodies.ports (2, via ports); usado por ← engine.events.domain.ports.eventtype (8, via ports), rules (6, acoplamiento directo), engine.model.impl (3, acoplamiento directo), engine.events.domain.core (2, acoplamiento directo), engine.events.domain.ports.payloads (2, via ports), engine.model.emitter.ports (2, via ports), engine.model.weapons.ports (2, via ports), engine.model.bodies.core (1, acoplamiento directo), engine.model.bodies.impl (1, acoplamiento directo), engine.model.emitter.core (1, acoplamiento directo), engine.model.weapons.core (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/events/domain/ports/BodyRefDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/BodyRefDTO.java) — DTO de datos
  - [src/engine/events/domain/ports/BodyToEmitDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/BodyToEmitDTO.java) — DTO de datos
  - [src/engine/events/domain/ports/DomainEventType.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/DomainEventType.java) — Clase/enum
- **Patrones de diseño relevantes:** DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

###### engine.events.domain.ports.eventtype (`src/engine/events/domain/ports/eventtype`)

Tipos concretos de eventos del dominio.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.events.domain.ports.eventtype | Tipos concretos de eventos del dominio. | Modelar eventos de colisión, límites, emisión |

**Análisis detallado**

- **Propósito:** Tipos concretos de eventos del dominio.
- **Responsabilidades principales:** Modelar eventos de colisión, límites, emisión.
- **Interacción con otras capas/paquetes:** usa → engine.events.domain.ports (8, via ports), engine.events.domain.core (4, acoplamiento directo), engine.events.domain.ports.payloads (4, via ports); usado por ← rules (30, acoplamiento directo), engine.model.impl (5, acoplamiento directo), engine.actions (1, acoplamiento directo), engine.controller.impl (1, acoplamiento directo), engine.controller.ports (1, via ports), engine.model.bodies.core (1, acoplamiento directo), engine.model.ports (1, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/events/domain/ports/eventtype/CollisionEvent.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/eventtype/CollisionEvent.java) — Clase/enum
  - [src/engine/events/domain/ports/eventtype/DomainEvent.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/eventtype/DomainEvent.java) — Clase/enum
  - [src/engine/events/domain/ports/eventtype/EmitEvent.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/eventtype/EmitEvent.java) — Clase/enum
  - [src/engine/events/domain/ports/eventtype/LifeOver.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/eventtype/LifeOver.java) — Clase/enum
  - [src/engine/events/domain/ports/eventtype/LimitEvent.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/eventtype/LimitEvent.java) — Clase/enum
- **Patrones de diseño relevantes:** Event-Action
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

###### engine.events.domain.ports.payloads (`src/engine/events/domain/ports/payloads`)

Payloads tipados para eventos.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.events.domain.ports.payloads | Payloads tipados para eventos. | Transportar datos específicos por evento |

**Análisis detallado**

- **Propósito:** Payloads tipados para eventos.
- **Responsabilidades principales:** Transportar datos específicos por evento.
- **Interacción con otras capas/paquetes:** usa → engine.events.domain.ports (2, via ports); usado por ← engine.events.domain.ports.eventtype (4, via ports), engine.model.impl (2, acoplamiento directo), engine.events.domain.core (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/events/domain/ports/payloads/CollisionPayload.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/payloads/CollisionPayload.java) — Clase/enum
  - [src/engine/events/domain/ports/payloads/DomainEventPayload.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/payloads/DomainEventPayload.java) — Clase/enum
  - [src/engine/events/domain/ports/payloads/EmitPayloadDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/payloads/EmitPayloadDTO.java) — DTO de datos
  - [src/engine/events/domain/ports/payloads/NoPayload.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/events/domain/ports/payloads/NoPayload.java) — Clase/enum
- **Patrones de diseño relevantes:** DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### engine.generators (`src/engine/generators`)

Generadores base para IA y niveles.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.generators | Generadores base para IA y niveles. | Esqueleto de generación; Materializar definiciones a DTOs |

**Análisis detallado**

- **Propósito:** Generadores base para IA y niveles.
- **Responsabilidades principales:** Esqueleto de generación, Materializar definiciones a DTOs.
- **Interacción con otras capas/paquetes:** usa → engine.worlddef.ports (13, via ports), engine.controller.ports (3, via ports), engine.utils.helpers (1, acoplamiento directo); usado por ← ai (1, acoplamiento directo), level (1, acoplamiento directo).
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/generators/AbstractIAGenerator.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/generators/AbstractIAGenerator.java) — Base abstracta
  - [src/engine/generators/AbstractLevelGenerator.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/generators/AbstractLevelGenerator.java) — Base abstracta
  - [src/engine/generators/DefItemMaterializer.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/generators/DefItemMaterializer.java) — Clase/enum
- **Patrones de diseño relevantes:** Template Method
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Coordina la creación de entidades a partir de definiciones.

### engine.model (`src/engine/model`)

Subdominio del modelo de simulación.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.bodies | Entidades físicas del modelo. | Definición de bodies; DTOs y factories |
| engine.model.emitter | Subdominio de emisores de efectos. | DTOs y contratos de emisores; Implementaciones concretas |
| engine.model.impl | Implementación principal del modelo y simulación. | Gestión de entidades; Procesamiento de eventos y acciones; Snapshots de estado |
| engine.model.physics | Subdominio de físicas. | Motores y DTOs de física |
| engine.model.ports | Puertos del modelo para procesar eventos y exponer estado. | Contrato para procesar eventos; DTO de estado del modelo |
| engine.model.weapons | Subdominio de armas y disparo. | DTOs y factories de armas; Implementaciones concretas |

**Análisis detallado**

- **Propósito:** Subdominio del modelo de simulación.
- **Responsabilidades principales:** Gestión de entidades, Físicas, armas y emisores.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** MVC Model
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.model.bodies (`src/engine/model/bodies`)

Entidades físicas del modelo.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.bodies.core | Clase base de entidades físicas. | Estado común y utilidades de bodies |
| engine.model.bodies.impl | Implementaciones concretas de cuerpos (dinámicos, estáticos, proyectiles). | Simulación de bodies; Gestión de threads por entidad |
| engine.model.bodies.ports | DTOs y contratos de bodies. | DTOs de bodies y jugador; Interfaces de factories/procesadores |

**Análisis detallado**

- **Propósito:** Entidades físicas del modelo.
- **Responsabilidades principales:** Definición de bodies, DTOs y factories.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.bodies.core (`src/engine/model/bodies/core`)

Clase base de entidades físicas.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.bodies.core | Clase base de entidades físicas. | Estado común y utilidades de bodies |

**Análisis detallado**

- **Propósito:** Clase base de entidades físicas.
- **Responsabilidades principales:** Estado común y utilidades de bodies.
- **Interacción con otras capas/paquetes:** usa → engine.model.bodies.ports (3, via ports), engine.model.physics.ports (2, via ports), engine.actions (1, acoplamiento directo), engine.events.domain.ports (1, via ports), engine.events.domain.ports.eventtype (1, via ports), engine.model.emitter.ports (1, via ports), engine.utils.spatial.core (1, acoplamiento directo); usado por ← engine.model.bodies.impl (3, acoplamiento directo), engine.model.bodies.ports (2, via ports), engine.model.impl (1, acoplamiento directo).
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/bodies/core/AbstractBody.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/core/AbstractBody.java) — Base abstracta
- **Patrones de diseño relevantes:** Template Method
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.bodies.impl (`src/engine/model/bodies/impl`)

Implementaciones concretas de cuerpos (dinámicos, estáticos, proyectiles).

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.bodies.impl | Implementaciones concretas de cuerpos (dinámicos, estáticos, proyectiles). | Simulación de bodies; Gestión de threads por entidad |

**Análisis detallado**

- **Propósito:** Implementaciones concretas de cuerpos (dinámicos, estáticos, proyectiles).
- **Responsabilidades principales:** Simulación de bodies, Gestión de threads por entidad.
- **Interacción con otras capas/paquetes:** usa → engine.model.bodies.ports (12, via ports), engine.model.physics.ports (6, via ports), engine.utils.spatial.core (4, acoplamiento directo), engine.model.bodies.core (3, acoplamiento directo), engine.model.weapons.ports (2, via ports), engine.events.domain.ports (1, via ports), engine.model.emitter.ports (1, via ports), engine.model.physics.implementations (1, acoplamiento directo); usado por ← engine.model.bodies.ports (4, via ports), engine.model.impl (3, acoplamiento directo).
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/bodies/impl/DynamicBody.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/impl/DynamicBody.java) — Entidad
  - [src/engine/model/bodies/impl/PlayerBody.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/impl/PlayerBody.java) — Entidad
  - [src/engine/model/bodies/impl/ProjectileBody.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/impl/ProjectileBody.java) — Entidad
  - [src/engine/model/bodies/impl/StaticBody.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/impl/StaticBody.java) — Entidad
- **Patrones de diseño relevantes:** Strategy (con PhysicsEngine), Runnable
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Implementa el threading por entidad y el loop de actualización.

##### engine.model.bodies.ports (`src/engine/model/bodies/ports`)

DTOs y contratos de bodies.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.bodies.ports | DTOs y contratos de bodies. | DTOs de bodies y jugador; Interfaces de factories/procesadores |

**Análisis detallado**

- **Propósito:** DTOs y contratos de bodies.
- **Responsabilidades principales:** DTOs de bodies y jugador, Interfaces de factories/procesadores.
- **Interacción con otras capas/paquetes:** usa → engine.model.bodies.impl (4, acoplamiento directo), engine.model.physics.ports (4, via ports), engine.model.bodies.core (2, acoplamiento directo), engine.model.physics.implementations (1, acoplamiento directo), engine.utils.spatial.core (1, acoplamiento directo); usado por ← engine.model.bodies.impl (12, acoplamiento directo), engine.model.impl (6, acoplamiento directo), engine.controller.mappers (3, acoplamiento directo), engine.model.bodies.core (3, acoplamiento directo), rules (3, acoplamiento directo), engine.events.domain.ports (2, via ports), world (2, acoplamiento directo), engine.actions (1, acoplamiento directo), engine.controller.impl (1, acoplamiento directo), engine.model.emitter.ports (1, via ports), engine.model.weapons.ports (1, via ports), engine.worlddef.core (1, acoplamiento directo), engine.worlddef.ports (1, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/bodies/ports/BodyDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/ports/BodyDTO.java) — DTO de datos
  - [src/engine/model/bodies/ports/BodyEventProcessor.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/ports/BodyEventProcessor.java) — Entidad
  - [src/engine/model/bodies/ports/BodyFactory.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/ports/BodyFactory.java) — Factory
  - [src/engine/model/bodies/ports/BodyState.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/ports/BodyState.java) — Entidad
  - [src/engine/model/bodies/ports/BodyType.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/ports/BodyType.java) — Entidad
  - [src/engine/model/bodies/ports/PlayerDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/bodies/ports/PlayerDTO.java) — DTO de datos
- **Patrones de diseño relevantes:** DTO, Ports & Adapters
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.model.emitter (`src/engine/model/emitter`)

Subdominio de emisores de efectos.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.emitter.core | Base abstracta de emisores. | Comportamiento base de emisión |
| engine.model.emitter.impl | Emisores concretos para trails o efectos. | Lógica concreta de emisión |
| engine.model.emitter.ports | Contratos y DTOs de emisores. | Interfaces de emisores; DTOs de configuración |

**Análisis detallado**

- **Propósito:** Subdominio de emisores de efectos.
- **Responsabilidades principales:** DTOs y contratos de emisores, Implementaciones concretas.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.emitter.core (`src/engine/model/emitter/core`)

Base abstracta de emisores.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.emitter.core | Base abstracta de emisores. | Comportamiento base de emisión |

**Análisis detallado**

- **Propósito:** Base abstracta de emisores.
- **Responsabilidades principales:** Comportamiento base de emisión.
- **Interacción con otras capas/paquetes:** usa → engine.model.emitter.ports (2, via ports), engine.events.domain.ports (1, via ports); usado por ← engine.model.emitter.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/emitter/core/AbstractEmitter.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/emitter/core/AbstractEmitter.java) — Base abstracta
- **Patrones de diseño relevantes:** Template Method
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.emitter.impl (`src/engine/model/emitter/impl`)

Emisores concretos para trails o efectos.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.emitter.impl | Emisores concretos para trails o efectos. | Lógica concreta de emisión |

**Análisis detallado**

- **Propósito:** Emisores concretos para trails o efectos.
- **Responsabilidades principales:** Lógica concreta de emisión.
- **Interacción con otras capas/paquetes:** usa → engine.model.emitter.ports (2, via ports), engine.model.weapons.ports (2, via ports), engine.model.emitter.core (1, acoplamiento directo); usado por ← engine.model.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/emitter/impl/BasicEmitter.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/emitter/impl/BasicEmitter.java) — Emisor
  - [src/engine/model/emitter/impl/BurstEmitter.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/emitter/impl/BurstEmitter.java) — Emisor
- **Patrones de diseño relevantes:** Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.emitter.ports (`src/engine/model/emitter/ports`)

Contratos y DTOs de emisores.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.emitter.ports | Contratos y DTOs de emisores. | Interfaces de emisores; DTOs de configuración |

**Análisis detallado**

- **Propósito:** Contratos y DTOs de emisores.
- **Responsabilidades principales:** Interfaces de emisores, DTOs de configuración.
- **Interacción con otras capas/paquetes:** usa → engine.events.domain.ports (2, via ports), engine.model.bodies.ports (1, via ports); usado por ← engine.model.emitter.core (2, acoplamiento directo), engine.model.emitter.impl (2, acoplamiento directo), engine.model.impl (2, acoplamiento directo), engine.controller.impl (1, acoplamiento directo), engine.controller.mappers (1, acoplamiento directo), engine.model.bodies.core (1, acoplamiento directo), engine.model.bodies.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/emitter/ports/Emitter.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/emitter/ports/Emitter.java) — Emisor
  - [src/engine/model/emitter/ports/EmitterConfigDto.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/emitter/ports/EmitterConfigDto.java) — Emisor
- **Patrones de diseño relevantes:** DTO, Ports & Adapters
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.model.impl (`src/engine/model/impl`)

Implementación principal del modelo y simulación.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.impl | Implementación principal del modelo y simulación. | Gestión de entidades; Procesamiento de eventos y acciones; Snapshots de estado |

**Análisis detallado**

- **Propósito:** Implementación principal del modelo y simulación.
- **Responsabilidades principales:** Gestión de entidades, Procesamiento de eventos y acciones, Snapshots de estado.
- **Interacción con otras capas/paquetes:** usa → engine.model.bodies.ports (6, via ports), engine.events.domain.ports.eventtype (5, via ports), engine.events.domain.ports (3, via ports), engine.model.bodies.impl (3, acoplamiento directo), engine.model.weapons.ports (3, via ports), engine.actions (2, acoplamiento directo), engine.events.domain.ports.payloads (2, via ports), engine.model.emitter.ports (2, via ports), engine.model.ports (2, via ports), engine.model.bodies.core (1, acoplamiento directo), engine.model.emitter.impl (1, acoplamiento directo), engine.model.physics.ports (1, via ports), engine.utils.helpers (1, acoplamiento directo), engine.utils.spatial.core (1, acoplamiento directo), engine.utils.spatial.ports (1, via ports); usado por ← default (Main) (1, acoplamiento directo), engine.controller.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/impl/Model.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/impl/Model.java) — Clase/enum
- **Patrones de diseño relevantes:** MVC Model, Event-Action
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Contiene el loop de simulación y el estado concurrente.

#### engine.model.physics (`src/engine/model/physics`)

Subdominio de físicas.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.physics.core | Base de motores de física. | Funciones comunes de físicas |
| engine.model.physics.implementations | Motores de físicas concretos. | Implementar comportamiento físico |
| engine.model.physics.ports | Contratos y DTOs de física. | DTO de valores físicos; Interfaz de motor de físicas |

**Análisis detallado**

- **Propósito:** Subdominio de físicas.
- **Responsabilidades principales:** Motores y DTOs de física.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.physics.core (`src/engine/model/physics/core`)

Base de motores de física.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.physics.core | Base de motores de física. | Funciones comunes de físicas |

**Análisis detallado**

- **Propósito:** Base de motores de física.
- **Responsabilidades principales:** Funciones comunes de físicas.
- **Interacción con otras capas/paquetes:** usa → engine.model.physics.ports (2, via ports); usado por ← engine.model.physics.implementations (2, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/physics/core/AbstractPhysicsEngine.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/physics/core/AbstractPhysicsEngine.java) — Base abstracta
- **Patrones de diseño relevantes:** Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.physics.implementations (`src/engine/model/physics/implementations`)

Motores de físicas concretos.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.physics.implementations | Motores de físicas concretos. | Implementar comportamiento físico |

**Análisis detallado**

- **Propósito:** Motores de físicas concretos.
- **Responsabilidades principales:** Implementar comportamiento físico.
- **Interacción con otras capas/paquetes:** usa → engine.model.physics.core (2, acoplamiento directo), engine.model.physics.ports (2, via ports); usado por ← engine.model.bodies.impl (1, acoplamiento directo), engine.model.bodies.ports (1, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/physics/implementations/BasicPhysicsEngine.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/physics/implementations/BasicPhysicsEngine.java) — Físicas
  - [src/engine/model/physics/implementations/NullPhysicsEngine.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/physics/implementations/NullPhysicsEngine.java) — Físicas
- **Patrones de diseño relevantes:** Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Afecta directamente la estabilidad y performance de simulación.

##### engine.model.physics.ports (`src/engine/model/physics/ports`)

Contratos y DTOs de física.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.physics.ports | Contratos y DTOs de física. | DTO de valores físicos; Interfaz de motor de físicas |

**Análisis detallado**

- **Propósito:** Contratos y DTOs de física.
- **Responsabilidades principales:** DTO de valores físicos, Interfaz de motor de físicas.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← engine.model.bodies.impl (6, acoplamiento directo), engine.model.bodies.ports (4, via ports), engine.model.bodies.core (2, acoplamiento directo), engine.model.physics.core (2, acoplamiento directo), engine.model.physics.implementations (2, acoplamiento directo), engine.model.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/physics/ports/PhysicsEngine.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/physics/ports/PhysicsEngine.java) — Físicas
  - [src/engine/model/physics/ports/PhysicsValuesDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/physics/ports/PhysicsValuesDTO.java) — DTO de datos
- **Patrones de diseño relevantes:** DTO, Ports & Adapters
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.model.ports (`src/engine/model/ports`)

Puertos del modelo para procesar eventos y exponer estado.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.ports | Puertos del modelo para procesar eventos y exponer estado. | Contrato para procesar eventos; DTO de estado del modelo |

**Análisis detallado**

- **Propósito:** Puertos del modelo para procesar eventos y exponer estado.
- **Responsabilidades principales:** Contrato para procesar eventos, DTO de estado del modelo.
- **Interacción con otras capas/paquetes:** usa → engine.actions (1, acoplamiento directo), engine.events.domain.ports.eventtype (1, via ports); usado por ← engine.model.impl (2, acoplamiento directo), engine.controller.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/ports/DomainEventProcessor.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/ports/DomainEventProcessor.java) — Clase/enum
  - [src/engine/model/ports/ModelState.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/ports/ModelState.java) — Clase/enum
- **Patrones de diseño relevantes:** Ports & Adapters, DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.model.weapons (`src/engine/model/weapons`)

Subdominio de armas y disparo.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.weapons.core | Base abstracta de armas. | Comportamiento base de armas |
| engine.model.weapons.implementations | Implementaciones concretas de armas. | Lógicas de disparo específicas |
| engine.model.weapons.ports | Contratos y DTOs de armas. | Factory de armas; DTOs y enums de tipos |

**Análisis detallado**

- **Propósito:** Subdominio de armas y disparo.
- **Responsabilidades principales:** DTOs y factories de armas, Implementaciones concretas.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Factory, Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.weapons.core (`src/engine/model/weapons/core`)

Base abstracta de armas.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.weapons.core | Base abstracta de armas. | Comportamiento base de armas |

**Análisis detallado**

- **Propósito:** Base abstracta de armas.
- **Responsabilidades principales:** Comportamiento base de armas.
- **Interacción con otras capas/paquetes:** usa → engine.model.weapons.ports (3, via ports), engine.events.domain.ports (1, via ports); usado por ← engine.model.weapons.implementations (4, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/weapons/core/AbstractWeapon.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/core/AbstractWeapon.java) — Base abstracta
- **Patrones de diseño relevantes:** Template Method
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.model.weapons.implementations (`src/engine/model/weapons/implementations`)

Implementaciones concretas de armas.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.weapons.implementations | Implementaciones concretas de armas. | Lógicas de disparo específicas |

**Análisis detallado**

- **Propósito:** Implementaciones concretas de armas.
- **Responsabilidades principales:** Lógicas de disparo específicas.
- **Interacción con otras capas/paquetes:** usa → engine.model.weapons.ports (8, via ports), engine.model.weapons.core (4, acoplamiento directo); usado por ← engine.model.weapons.ports (4, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/weapons/implementations/BasicWeapon.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/implementations/BasicWeapon.java) — Arma/armas
  - [src/engine/model/weapons/implementations/BurstWeapon.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/implementations/BurstWeapon.java) — Arma/armas
  - [src/engine/model/weapons/implementations/MineLauncher.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/implementations/MineLauncher.java) — Clase/enum
  - [src/engine/model/weapons/implementations/MissileLauncher.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/implementations/MissileLauncher.java) — Clase/enum
- **Patrones de diseño relevantes:** Strategy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Impacto directo en gameplay y balance.

##### engine.model.weapons.ports (`src/engine/model/weapons/ports`)

Contratos y DTOs de armas.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.model.weapons.ports | Contratos y DTOs de armas. | Factory de armas; DTOs y enums de tipos |

**Análisis detallado**

- **Propósito:** Contratos y DTOs de armas.
- **Responsabilidades principales:** Factory de armas, DTOs y enums de tipos.
- **Interacción con otras capas/paquetes:** usa → engine.model.weapons.implementations (4, acoplamiento directo), engine.events.domain.ports (2, via ports), engine.model.bodies.ports (1, via ports); usado por ← engine.model.weapons.implementations (8, acoplamiento directo), engine.model.impl (3, acoplamiento directo), engine.model.weapons.core (3, acoplamiento directo), engine.controller.mappers (2, acoplamiento directo), engine.model.bodies.impl (2, acoplamiento directo), engine.model.emitter.impl (2, acoplamiento directo), engine.controller.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/model/weapons/ports/Weapon.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/ports/Weapon.java) — Arma/armas
  - [src/engine/model/weapons/ports/WeaponDto.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/ports/WeaponDto.java) — Arma/armas
  - [src/engine/model/weapons/ports/WeaponFactory.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/ports/WeaponFactory.java) — Factory
  - [src/engine/model/weapons/ports/WeaponState.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/ports/WeaponState.java) — Arma/armas
  - [src/engine/model/weapons/ports/WeaponType.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/model/weapons/ports/WeaponType.java) — Arma/armas
- **Patrones de diseño relevantes:** Factory, DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### engine.utils (`src/engine/utils`)

Utilidades transversales del engine.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.utils.fx | Tipos y DTOs para efectos visuales. | Definir tipos de FX; DTOs de efectos |
| engine.utils.helpers | Utilidades generales (vectores, listas aleatorias). | Funciones helper; Estructuras simples |
| engine.utils.images | Cache y DTOs de imágenes. | Cache de imágenes; Metadatos de recursos |
| engine.utils.spatial | Subdominio de spatial grid. | Estructuras de particionado; DTOs de estadísticas |

**Análisis detallado**

- **Propósito:** Utilidades transversales del engine.
- **Responsabilidades principales:** Imágenes, helpers, FX y spatial grid.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Utility
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.utils.fx (`src/engine/utils/fx`)

Tipos y DTOs para efectos visuales.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.utils.fx | Tipos y DTOs para efectos visuales. | Definir tipos de FX; DTOs de efectos |

**Análisis detallado**

- **Propósito:** Tipos y DTOs para efectos visuales.
- **Responsabilidades principales:** Definir tipos de FX, DTOs de efectos.
- **Interacción con otras capas/paquetes:** usa → engine.view.renderables.impl (2, acoplamiento directo); usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/utils/fx/Fx.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/fx/Fx.java) — Clase/enum
  - [src/engine/utils/fx/FxImage.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/fx/FxImage.java) — Imágenes
  - [src/engine/utils/fx/FxType.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/fx/FxType.java) — Clase/enum
  - [src/engine/utils/fx/Spin.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/fx/Spin.java) — Clase/enum
- **Patrones de diseño relevantes:** DTO, Enum
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.utils.helpers (`src/engine/utils/helpers`)

Utilidades generales (vectores, listas aleatorias).

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.utils.helpers | Utilidades generales (vectores, listas aleatorias). | Funciones helper; Estructuras simples |

**Análisis detallado**

- **Propósito:** Utilidades generales (vectores, listas aleatorias).
- **Responsabilidades principales:** Funciones helper, Estructuras simples.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← engine.view.core (2, acoplamiento directo), world (2, acoplamiento directo), default (Main) (1, acoplamiento directo), engine.controller.impl (1, acoplamiento directo), engine.controller.ports (1, via ports), engine.generators (1, acoplamiento directo), engine.model.impl (1, acoplamiento directo), engine.worlddef.core (1, acoplamiento directo).
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/utils/helpers/DoubleVector.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/helpers/DoubleVector.java) — Clase/enum
  - [src/engine/utils/helpers/RandomArrayList.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/helpers/RandomArrayList.java) — Clase/enum
- **Patrones de diseño relevantes:** Utility
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.utils.images (`src/engine/utils/images`)

Cache y DTOs de imágenes.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.utils.images | Cache y DTOs de imágenes. | Cache de imágenes; Metadatos de recursos |

**Análisis detallado**

- **Propósito:** Cache y DTOs de imágenes.
- **Responsabilidades principales:** Cache de imágenes, Metadatos de recursos.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← engine.view.core (3, acoplamiento directo), engine.view.renderables.impl (2, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/utils/images/ImageCache.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/images/ImageCache.java) — Imágenes
  - [src/engine/utils/images/ImageCacheKeyDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/images/ImageCacheKeyDTO.java) — DTO de datos
  - [src/engine/utils/images/ImageDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/images/ImageDTO.java) — DTO de datos
  - [src/engine/utils/images/Images.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/images/Images.java) — Imágenes
- **Patrones de diseño relevantes:** Cache, DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.utils.spatial (`src/engine/utils/spatial`)

Subdominio de spatial grid.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.utils.spatial.core | Grid espacial para colisiones. | Particionado espacial; Celdas y estadísticas |
| engine.utils.spatial.ports | DTOs de métricas del grid espacial. | Exponer estadísticas para HUD o debug |

**Análisis detallado**

- **Propósito:** Subdominio de spatial grid.
- **Responsabilidades principales:** Estructuras de particionado, DTOs de estadísticas.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Spatial Hash/Grid
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.utils.spatial.core (`src/engine/utils/spatial/core`)

Grid espacial para colisiones.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.utils.spatial.core | Grid espacial para colisiones. | Particionado espacial; Celdas y estadísticas |

**Análisis detallado**

- **Propósito:** Grid espacial para colisiones.
- **Responsabilidades principales:** Particionado espacial, Celdas y estadísticas.
- **Interacción con otras capas/paquetes:** usa → engine.utils.spatial.ports (1, via ports); usado por ← engine.model.bodies.impl (4, acoplamiento directo), engine.model.bodies.core (1, acoplamiento directo), engine.model.bodies.ports (1, via ports), engine.model.impl (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/utils/spatial/core/Cells.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/spatial/core/Cells.java) — Clase/enum
  - [src/engine/utils/spatial/core/SpatialGrid.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/spatial/core/SpatialGrid.java) — Clase/enum
- **Patrones de diseño relevantes:** Spatial Hash/Grid
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Optimiza colisiones en el hot path del modelo.

##### engine.utils.spatial.ports (`src/engine/utils/spatial/ports`)

DTOs de métricas del grid espacial.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.utils.spatial.ports | DTOs de métricas del grid espacial. | Exponer estadísticas para HUD o debug |

**Análisis detallado**

- **Propósito:** DTOs de métricas del grid espacial.
- **Responsabilidades principales:** Exponer estadísticas para HUD o debug.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← engine.controller.mappers (1, acoplamiento directo), engine.model.impl (1, acoplamiento directo), engine.utils.spatial.core (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/utils/spatial/ports/SpatialGridStatisticsDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/utils/spatial/ports/SpatialGridStatisticsDTO.java) — DTO de datos
- **Patrones de diseño relevantes:** DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### engine.view (`src/engine/view`)

Subdominio de presentación y render.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.core | Vista principal, renderer y panel de control. | Render loop; Gestión de input; Consumo de DTOs del controller |
| engine.view.hud | Subdominio del HUD. | Componentes base; HUDs concretos |
| engine.view.renderables | Subdominio de renderables. | DTOs de render; Implementaciones de renderables |

**Análisis detallado**

- **Propósito:** Subdominio de presentación y render.
- **Responsabilidades principales:** Render loop, Renderables y HUD.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** MVC View
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.view.core (`src/engine/view/core`)

Vista principal, renderer y panel de control.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.core | Vista principal, renderer y panel de control. | Render loop; Gestión de input; Consumo de DTOs del controller |

**Análisis detallado**

- **Propósito:** Vista principal, renderer y panel de control.
- **Responsabilidades principales:** Render loop, Gestión de input, Consumo de DTOs del controller.
- **Interacción con otras capas/paquetes:** usa → engine.view.renderables.ports (8, via ports), engine.utils.images (3, acoplamiento directo), engine.view.hud.impl (3, acoplamiento directo), engine.controller.ports (2, via ports), engine.utils.helpers (2, acoplamiento directo), engine.view.renderables.impl (2, acoplamiento directo), assets.core (1, acoplamiento directo), assets.ports (1, via ports), engine.controller.impl (1, acoplamiento directo); usado por ← default (Main) (1, acoplamiento directo), engine.controller.impl (1, acoplamiento directo).
- **Concurrencia / threading:** Se observan constructs de concurrencia (Thread/Runnable).
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/view/core/ControlPanel.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/core/ControlPanel.java) — Clase/enum
  - [src/engine/view/core/Renderer.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/core/Renderer.java) — Render loop
  - [src/engine/view/core/SystemDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/core/SystemDTO.java) — DTO de datos
  - [src/engine/view/core/View.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/core/View.java) — Vista principal
- **Patrones de diseño relevantes:** MVC View
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Render loop y sincronización con snapshots.

#### engine.view.hud (`src/engine/view/hud`)

Subdominio del HUD.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.hud.core | Componentes base del HUD. | Items y layout del HUD; Componentes de barras y texto |
| engine.view.hud.impl | HUDs concretos del sistema y métricas. | HUD de sistema y player; HUD del grid espacial |

**Análisis detallado**

- **Propósito:** Subdominio del HUD.
- **Responsabilidades principales:** Componentes base, HUDs concretos.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Composite
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.view.hud.core (`src/engine/view/hud/core`)

Componentes base del HUD.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.hud.core | Componentes base del HUD. | Items y layout del HUD; Componentes de barras y texto |

**Análisis detallado**

- **Propósito:** Componentes base del HUD.
- **Responsabilidades principales:** Items y layout del HUD, Componentes de barras y texto.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← engine.view.hud.impl (3, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/view/hud/core/BarItem.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/BarItem.java) — Clase/enum
  - [src/engine/view/hud/core/DataHUD.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/DataHUD.java) — HUD
  - [src/engine/view/hud/core/GridHUD.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/GridHUD.java) — HUD
  - [src/engine/view/hud/core/IconItem.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/IconItem.java) — Clase/enum
  - [src/engine/view/hud/core/Item.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/Item.java) — Clase/enum
  - [src/engine/view/hud/core/SeparatorItem.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/SeparatorItem.java) — Clase/enum
  - [src/engine/view/hud/core/SkipItem.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/SkipItem.java) — Clase/enum
  - [src/engine/view/hud/core/TextItem.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/TextItem.java) — Clase/enum
  - [src/engine/view/hud/core/TitleItem.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/core/TitleItem.java) — Clase/enum
- **Patrones de diseño relevantes:** Composite
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.view.hud.impl (`src/engine/view/hud/impl`)

HUDs concretos del sistema y métricas.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.hud.impl | HUDs concretos del sistema y métricas. | HUD de sistema y player; HUD del grid espacial |

**Análisis detallado**

- **Propósito:** HUDs concretos del sistema y métricas.
- **Responsabilidades principales:** HUD de sistema y player, HUD del grid espacial.
- **Interacción con otras capas/paquetes:** usa → engine.view.hud.core (3, acoplamiento directo); usado por ← engine.view.core (3, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/view/hud/impl/PlayerHUD.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/impl/PlayerHUD.java) — HUD
  - [src/engine/view/hud/impl/SpatialGridHUD.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/impl/SpatialGridHUD.java) — HUD
  - [src/engine/view/hud/impl/SystemHUD.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/hud/impl/SystemHUD.java) — HUD
- **Patrones de diseño relevantes:** Template Method
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.view.renderables (`src/engine/view/renderables`)

Subdominio de renderables.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.renderables.impl | Renderables concretos. | Modelar sprites renderizables; Actualizar estado visual |
| engine.view.renderables.ports | DTOs de renderables para la vista. | DTOs de render dinámico y jugador; Métricas de grid |

**Análisis detallado**

- **Propósito:** Subdominio de renderables.
- **Responsabilidades principales:** DTOs de render, Implementaciones de renderables.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** DTO, Adapter
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.view.renderables.impl (`src/engine/view/renderables/impl`)

Renderables concretos.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.renderables.impl | Renderables concretos. | Modelar sprites renderizables; Actualizar estado visual |

**Análisis detallado**

- **Propósito:** Renderables concretos.
- **Responsabilidades principales:** Modelar sprites renderizables, Actualizar estado visual.
- **Interacción con otras capas/paquetes:** usa → engine.utils.images (2, acoplamiento directo), engine.view.renderables.ports (2, via ports); usado por ← engine.utils.fx (2, acoplamiento directo), engine.view.core (2, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/view/renderables/impl/DynamicRenderable.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/renderables/impl/DynamicRenderable.java) — Clase/enum
  - [src/engine/view/renderables/impl/Renderable.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/renderables/impl/Renderable.java) — Clase/enum
- **Patrones de diseño relevantes:** Adapter
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

##### engine.view.renderables.ports (`src/engine/view/renderables/ports`)

DTOs de renderables para la vista.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.view.renderables.ports | DTOs de renderables para la vista. | DTOs de render dinámico y jugador; Métricas de grid |

**Análisis detallado**

- **Propósito:** DTOs de renderables para la vista.
- **Responsabilidades principales:** DTOs de render dinámico y jugador, Métricas de grid.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← engine.view.core (8, acoplamiento directo), engine.controller.impl (4, acoplamiento directo), engine.controller.mappers (4, acoplamiento directo), engine.view.renderables.impl (2, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/view/renderables/ports/DynamicRenderDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/renderables/ports/DynamicRenderDTO.java) — DTO de datos
  - [src/engine/view/renderables/ports/PlayerRenderDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/renderables/ports/PlayerRenderDTO.java) — DTO de datos
  - [src/engine/view/renderables/ports/RenderDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/renderables/ports/RenderDTO.java) — DTO de datos
  - [src/engine/view/renderables/ports/SpatialGridStatisticsRenderDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/view/renderables/ports/SpatialGridStatisticsRenderDTO.java) — DTO de datos
- **Patrones de diseño relevantes:** DTO
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

### engine.worlddef (`src/engine/worlddef`)

Subdominio de definiciones de mundo.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.worlddef.core | Base de proveedores de definiciones de mundo. | Factory de definiciones; Registro de assets y armas |
| engine.worlddef.ports | Contratos y DTOs de definiciones de mundo. | DTOs de items, armas, emisores; Interfaces de provider |

**Análisis detallado**

- **Propósito:** Subdominio de definiciones de mundo.
- **Responsabilidades principales:** Factories de definiciones, DTOs y providers.
- **Interacción con otras capas/paquetes:** usa → Sin dependencias internas detectadas.; usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:** sin archivos Java directos.
- **Patrones de diseño relevantes:** Provider, Factory
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

#### engine.worlddef.core (`src/engine/worlddef/core`)

Base de proveedores de definiciones de mundo.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.worlddef.core | Base de proveedores de definiciones de mundo. | Factory de definiciones; Registro de assets y armas |

**Análisis detallado**

- **Propósito:** Base de proveedores de definiciones de mundo.
- **Responsabilidades principales:** Factory de definiciones, Registro de assets y armas.
- **Interacción con otras capas/paquetes:** usa → engine.worlddef.ports (10, via ports), assets.ports (3, via ports), assets.core (2, acoplamiento directo), assets.impl (2, acoplamiento directo), engine.model.bodies.ports (1, via ports), engine.utils.helpers (1, acoplamiento directo); usado por ← world (2, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/worlddef/core/AbstractWorldDefinitionProvider.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/core/AbstractWorldDefinitionProvider.java) — Base abstracta
  - [src/engine/worlddef/core/WeaponDefFactory.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/core/WeaponDefFactory.java) — Factory
  - [src/engine/worlddef/core/WorldAssetsRegister.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/core/WorldAssetsRegister.java) — Definición de mundo
- **Patrones de diseño relevantes:** Factory, Provider
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

**Detalles críticos**
- Define el contenido inicial y su wiring.

#### engine.worlddef.ports (`src/engine/worlddef/ports`)

Contratos y DTOs de definiciones de mundo.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| engine.worlddef.ports | Contratos y DTOs de definiciones de mundo. | DTOs de items, armas, emisores; Interfaces de provider |

**Análisis detallado**

- **Propósito:** Contratos y DTOs de definiciones de mundo.
- **Responsabilidades principales:** DTOs de items, armas, emisores, Interfaces de provider.
- **Interacción con otras capas/paquetes:** usa → assets.core (1, acoplamiento directo), engine.model.bodies.ports (1, via ports); usado por ← engine.generators (13, acoplamiento directo), engine.worlddef.core (10, acoplamiento directo), level (5, acoplamiento directo), ai (3, acoplamiento directo), engine.controller.mappers (3, acoplamiento directo), default (Main) (2, acoplamiento directo), engine.controller.impl (2, acoplamiento directo), engine.controller.ports (2, via ports).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/engine/worlddef/ports/DefBackgroundDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/DefBackgroundDTO.java) — DTO de datos
  - [src/engine/worlddef/ports/DefEmitterDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/DefEmitterDTO.java) — DTO de datos
  - [src/engine/worlddef/ports/DefItem.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/DefItem.java) — Clase/enum
  - [src/engine/worlddef/ports/DefItemDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/DefItemDTO.java) — DTO de datos
  - [src/engine/worlddef/ports/DefItemPrototypeDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/DefItemPrototypeDTO.java) — DTO de datos
  - [src/engine/worlddef/ports/DefWeaponDTO.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/DefWeaponDTO.java) — DTO de datos
  - [src/engine/worlddef/ports/DefWeaponType.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/DefWeaponType.java) — Arma/armas
  - [src/engine/worlddef/ports/WorldDefinition.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/WorldDefinition.java) — Definición de mundo
  - [src/engine/worlddef/ports/WorldDefinitionProvider.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/engine/worlddef/ports/WorldDefinitionProvider.java) — Definición de mundo
- **Patrones de diseño relevantes:** DTO, Ports & Adapters
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

## level (`src/level`)

Nivel básico basado en AbstractLevelGenerator.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| level | Nivel básico basado en AbstractLevelGenerator. | Crear decoradores y estáticos; Crear jugador con armas y emisores |

**Análisis detallado**

- **Propósito:** Nivel básico basado en AbstractLevelGenerator.
- **Responsabilidades principales:** Crear decoradores y estáticos, Crear jugador con armas y emisores.
- **Interacción con otras capas/paquetes:** usa → engine.worlddef.ports (5, via ports), engine.controller.ports (1, via ports), engine.generators (1, acoplamiento directo); usado por ← default (Main) (1, acoplamiento directo).
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/level/LevelBasic.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/level/LevelBasic.java) — Nivel
- **Patrones de diseño relevantes:** Template Method
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

## rules (`src/rules`)

Reglas concretas de eventos/acciones.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| rules | Reglas concretas de eventos/acciones. | Resolución de colisiones; Rebotes y límites |

**Análisis detallado**

- **Propósito:** Reglas concretas de eventos/acciones.
- **Responsabilidades principales:** Resolución de colisiones, Rebotes y límites.
- **Interacción con otras capas/paquetes:** usa → engine.events.domain.ports.eventtype (30, via ports), engine.actions (12, acoplamiento directo), engine.controller.ports (6, via ports), engine.events.domain.ports (6, via ports), engine.model.bodies.ports (3, via ports); usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/rules/DeadInLimits.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/rules/DeadInLimits.java) — Clase/enum
  - [src/rules/DeadInLimitsPlayerImmunity.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/rules/DeadInLimitsPlayerImmunity.java) — Clase/enum
  - [src/rules/InLimitsGoToCenter.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/rules/InLimitsGoToCenter.java) — Clase/enum
  - [src/rules/LimitRebound.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/rules/LimitRebound.java) — Clase/enum
  - [src/rules/ReboundAndCollision.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/rules/ReboundAndCollision.java) — Clase/enum
  - [src/rules/ReboundCollisionPlayerImmunity.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/rules/ReboundCollisionPlayerImmunity.java) — Clase/enum
- **Patrones de diseño relevantes:** Strategy/Policy
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.

## world (`src/world`)

Proveedores concretos de WorldDefinition.

| Paquete | Descripción breve | Responsabilidades clave |
|---|---|---|
| world | Proveedores concretos de WorldDefinition. | Generar presets de mundo; Configurar assets y tipos |

**Análisis detallado**

- **Propósito:** Proveedores concretos de WorldDefinition.
- **Responsabilidades principales:** Generar presets de mundo, Configurar assets y tipos.
- **Interacción con otras capas/paquetes:** usa → assets.impl (2, acoplamiento directo), assets.ports (2, via ports), engine.model.bodies.ports (2, via ports), engine.utils.helpers (2, acoplamiento directo), engine.worlddef.core (2, acoplamiento directo); usado por ← Sin dependencias internas detectadas..
- **Concurrencia / threading:** No se observa threading explícito en este paquete.
- **Principales clases/interfaces/DTOs y su rol:**
  - [src/world/EarthInCenterWorldDefinitionProvider.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/world/EarthInCenterWorldDefinitionProvider.java) — Definición de mundo
  - [src/world/RandomWorldDefinitionProvider.java](https://github.com/jumibot/MVCGameEngine/blob/develop/src/world/RandomWorldDefinitionProvider.java) — Definición de mundo
- **Patrones de diseño relevantes:** Provider
- **Puntos de atención:**
  - Revisar dependencias hacia implementaciones concretas y mantener contratos en `*.ports` cuando aplique.
  - Priorizar pruebas unitarias en rutas críticas o con alta concurrencia.
