Informe jerárquico de paquetes para {repoOwner}/{repoName} — rama: {branch} — generado el 2026-01-28.

## Micro-resumen arquitectural
El engine se organiza bajo un MVC explícito: `model` concentra la simulación y eventos, `view` ejecuta el render loop activo y la captura de input, y `controller` orquesta el ciclo de vida del engine y traduce eventos entre capas. Sobre esa base, `game` aporta reglas concretas (IA, niveles y acciones de dominio), `world` centraliza definiciones y DTOs del mundo, `utils` aporta infraestructura transversal (assets, eventos, helpers) y `resources` guarda assets no Java.

Se observan patrones como MVC, DTO/Mapper, Factory, Strategy y Template Method. El acoplamiento más sensible se da entre `controller`, `model` y `view`: el controlador mantiene referencias concretas a `Model` y `View`, pero el desacoplamiento funcional se apoya en interfaces ubicadas en paquetes `ports` (por ejemplo `controller.ports`, `model.ports`, `world.ports`) y en DTOs inmutables que evitan compartir estado mutable entre threads.

**Paquetes de primer nivel detectados (orden alfabético):** `controller`, `game`, `model`, `resources`, `utils`, `view`, `world`.

## Paquetes de primer nivel (grid resumen)

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `controller` | Orquestación MVC y mapeo de DTOs. | Coordinar activación del engine, mapear DTOs, conectar `model` ↔ `view`. |
| `game` | Reglas concretas (IA, niveles, acciones). | Generar mundo, spawns, reglas de eventos, bootstrapping. |
| `model` | Simulación, física y eventos de dominio. | Estado del juego, cuerpos, colisiones, armas, eventos. |
| `resources` | Assets no Java. | Imágenes y recursos de soporte. |
| `utils` | Infraestructura transversal. | Assets, eventos, helpers, imágenes, FX, acciones. |
| `view` | Capa de presentación Swing. | Render loop activo, HUDs, DTOs visuales, input. |
| `world` | Definiciones/DTOs del mundo. | Definición de items, armas, assets, world providers. |

---

## Paquete: `controller` (`src/controller`)
**Descripción:** Coordina el flujo del engine y actúa como fachada entre simulación (`model`) y presentación (`view`), usando DTOs y mappers.

### Resumen del paquete
| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `controller.implementations` | Implementación concreta del controlador. | Activación del engine, gestión de estado, orquestación MVC. |
| `controller.mappers` | Transformación de DTOs entre capas. | Mapear DTOs de dominio a render y definiciones a DTOs runtime. |
| `controller.ports` | Contratos para la orquestación. | Interfaces para inicialización/evolución del mundo y estado del engine. |

### Análisis detallado
- **Propósito:** Ser el punto central de coordinación del MVC.
- **Responsabilidades principales:** activar el engine, conectar `model` y `view`, traducir eventos a acciones y exponer snapshots de render.
- **Interacción con otras capas/paquetes:**
  - Usa directamente `model.implementations.Model` y `view.core.View` (acoplamiento concreto por inyección).
  - Se desacopla funcionalmente mediante `controller.ports` y DTOs (`BodyDTO`, `RenderDTO`).
- **Concurrencia / threading:** El render thread consulta snapshots del controlador; se devuelven DTOs nuevos para evitar estado compartido.
- **Principales clases/interfaces/DTOs:** [`Controller`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/implementations/Controller.java), [`ActionsGenerator`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/ActionsGenerator.java), [`EngineState`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/EngineState.java), [`WorldInitializer`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/WorldInitializer.java), [`WorldEvolver`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/WorldEvolver.java).
- **Patrones de diseño relevantes:** Facade, DTO Mapper, MVC, Ports & Adapters.
- **Puntos de atención:** cambios en DTOs o en el ciclo de vida del engine impactan `view` y `model`.

### Subpaquete: `controller.implementations`
**Descripción:** Implementación concreta del controlador.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Implementación MVC. | Activación del engine, orquestación de entidades, estado del engine. |

**Análisis detallado**
- **Propósito:** Ejecutar el flujo operativo del engine.
- **Responsabilidades principales:** cargar assets, inicializar mundo, sincronizar estado entre `model` y `view`.
- **Interacción con otras capas/paquetes:** referencia directa a `Model` y `View`; usa `controller.ports` para contratos de mundo.
- **Concurrencia / threading:** expone DTOs nuevos por frame; `engineState` es `volatile`.
- **Principales clases/interfaces/DTOs:** [`Controller`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/implementations/Controller.java).
- **Patrones de diseño relevantes:** Facade, DTO Mapper.
- **Puntos de atención:** cambios en el orden de activación pueden dejar al render loop sin estado válido.

### Subpaquete: `controller.mappers`
**Descripción:** Mappers para transformar DTOs de dominio en DTOs de render o runtime.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Transformación de DTOs. | Mapping de cuerpos, jugadores, armas, estadísticas de grid. |

**Análisis detallado**
- **Propósito:** Aislar la transformación de datos entre capas.
- **Responsabilidades principales:** convertir `BodyDTO`/`PlayerDTO`/`SpatialGridStatisticsDTO` a DTOs visuales o runtime.
- **Interacción con otras capas/paquetes:** utilizados por `controller` para servir datos a `view`.
- **Concurrencia / threading:** DTOs creados por copia para evitar sharing.
- **Principales clases/interfaces/DTOs:** [`RenderableMapper`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/mappers/RenderableMapper.java), [`DynamicRenderableMapper`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/mappers/DynamicRenderableMapper.java), [`PlayerRenderableMapper`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/mappers/PlayerRenderableMapper.java), [`WeaponMapper`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/mappers/WeaponMapper.java), [`WeaponTypeMapper`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/mappers/WeaponTypeMapper.java), [`EmitterMapper`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/mappers/EmitterMapper.java), [`SpatialGridStatisticsMapper`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/mappers/SpatialGridStatisticsMapper.java).
- **Patrones de diseño relevantes:** Mapper, DTO.
- **Puntos de atención:** sincronizar cambios de campos en DTOs entre capas.

### Subpaquete: `controller.ports`
**Descripción:** Interfaces y enums que definen el contrato entre capas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Contratos MVC. | Estado del engine, inicialización y evolución del mundo. |

**Análisis detallado**
- **Propósito:** Definir contratos estables de interacción.
- **Responsabilidades principales:** inicializar y evolucionar el mundo, exponer estado del engine.
- **Interacción con otras capas/paquetes:** usados por `game` y `controller` para desacoplar.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`ActionsGenerator`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/ActionsGenerator.java), [`EngineState`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/EngineState.java), [`WorldInitializer`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/WorldInitializer.java), [`WorldEvolver`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/controller/ports/WorldEvolver.java).
- **Patrones de diseño relevantes:** Ports & Adapters.
- **Puntos de atención:** cambios en interfaces afectan `game` y `controller`.

---

## Paquete: `game` (`src/game`)
**Descripción:** Define reglas concretas del juego (IA, niveles, acciones de dominio) y provee el entry point.

### Resumen del paquete
| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `game.core` | Generadores base de mundo y IA. | Pipeline de creación de mundo y spawns. |
| `game.implementations` | Implementaciones concretas de reglas. | Acciones de dominio, niveles y definición del mundo. |

### Análisis detallado
- **Propósito:** Aportar reglas y configuraciones concretas sobre la infraestructura MVC.
- **Responsabilidades principales:** crear niveles, generar spawns y conectar eventos con acciones.
- **Interacción con otras capas/paquetes:** usa `controller.ports` para world init/evolution y `world.ports` para definiciones; no toca `view` directamente.
- **Concurrencia / threading:** IA usa threads dedicados (`AbstractIAGenerator`).
- **Principales clases/interfaces/DTOs:** [`Main`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/Main.java), [`AbstractLevelGenerator`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/core/AbstractLevelGenerator.java), [`AbstractIAGenerator`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/core/AbstractIAGenerator.java).
- **Patrones de diseño relevantes:** Template Method, Strategy, Factory.
- **Puntos de atención:** el timing de spawns/acciones puede alterar el balance del juego.

### Subpaquete: `game.core`
**Descripción:** Base abstracta para crear mundos y spawners de IA.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Generadores base. | Creación de mundo, materialización de prototipos, spawn. |

**Análisis detallado**
- **Propósito:** Proveer un esqueleto de creación de mundo y spawns.
- **Responsabilidades principales:** materializar prototipos (`DefItemPrototypeDTO`) y aplicar pipeline de world creation.
- **Interacción con otras capas/paquetes:** depende de `controller.ports` para inyectar entidades; consume `world.ports`.
- **Concurrencia / threading:** `AbstractIAGenerator` gestiona su propio thread (`Runnable`).
- **Principales clases/interfaces/DTOs:** [`AbstractLevelGenerator`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/core/AbstractLevelGenerator.java), [`AbstractIAGenerator`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/core/AbstractIAGenerator.java).
- **Patrones de diseño relevantes:** Template Method.
- **Puntos de atención:** uso de randoms afecta reproducibilidad de niveles.

### Subpaquete: `game.implementations`
**Descripción:** Implementaciones concretas de IA, niveles, acciones y mundos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `game.implementations.actions` | Acciones ante eventos. | Reacciones a límites/colisiones. |
| `game.implementations.ai` | Spawner concreto. | Generación de entidades AI. |
| `game.implementations.level` | Niveles concretos. | Configuración de nivel/escenario. |
| `game.implementations.world` | Proveedores de definiciones. | Proveer definiciones de mundo concretas. |

**Análisis detallado**
- **Propósito:** Implementar reglas concretas sobre la infraestructura base.
- **Responsabilidades principales:** definir acciones ante eventos, spawners de IA y world providers.
- **Interacción con otras capas/paquetes:** consume `world.ports` y `controller.ports` para inicializar o evolucionar el mundo.
- **Concurrencia / threading:** hereda del comportamiento de `AbstractIAGenerator` en subpaquetes de IA.
- **Patrones de diseño relevantes:** Strategy (acciones), Factory (world providers).
- **Puntos de atención:** cambios de reglas deben reflejarse en tests de equilibrio.

#### Subpaquete: `game.implementations.actions`
**Descripción:** Acciones de dominio que responden a eventos del modelo.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Acciones de evento. | Reacciones a límites/colisiones, inmunidades, rebotes. |

**Análisis detallado**
- **Propósito:** Definir estrategias de acción ante eventos.
- **Responsabilidades principales:** generar `ActionDTO` según eventos (`Collision`, `Limit`, etc.).
- **Interacción con otras capas/paquetes:** integrado con `controller.ports.ActionsGenerator` y eventos de `model`.
- **Concurrencia / threading:** N/A (ejecutadas en pipeline de eventos del modelo).
- **Principales clases/interfaces/DTOs:** [`ActionsInLimitsGoToCenter`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/actions/ActionsInLimitsGoToCenter.java), [`ActionsReboundCollisionPlayerImmunity`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/actions/ActionsReboundCollisionPlayerImmunity.java), [`ActionsDeadInLimits`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/actions/ActionsDeadInLimits.java), [`ActionsDeadInLimitsPlayerImmunity`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/actions/ActionsDeadInLimitsPlayerImmunity.java), [`ActionsLimitRebound`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/actions/ActionsLimitRebound.java), [`ActionsReboundAndCollision`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/actions/ActionsReboundAndCollision.java).
- **Patrones de diseño relevantes:** Strategy.
- **Puntos de atención:** reglas deben alinearse con `DomainEventType` y `ActionDTO`.

#### Subpaquete: `game.implementations.ai`
**Descripción:** Spawner concreto de IA.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | IA básica. | Crear jugadores y asteroides en background. |

**Análisis detallado**
- **Propósito:** Spawning automático de entidades.
- **Responsabilidades principales:** crear jugador y spawnear asteroides periódicamente.
- **Interacción con otras capas/paquetes:** usa `WorldEvolver` para inyectar entidades; consume `WorldDefinition`.
- **Concurrencia / threading:** thread dedicado que ejecuta `tickAlive()`.
- **Principales clases/interfaces/DTOs:** [`AIBasicSpawner`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/ai/AIBasicSpawner.java).
- **Patrones de diseño relevantes:** Template Method (heredado), Strategy.
- **Puntos de atención:** cuidado con delays máximos en entornos de baja CPU.

#### Subpaquete: `game.implementations.level`
**Descripción:** Generador de nivel concreto.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Nivel básico. | Poblado del mundo (decoradores/cuerpos). |

**Análisis detallado**
- **Propósito:** Definir un conjunto básico de elementos del nivel.
- **Responsabilidades principales:** crear decoradores y cuerpos estáticos.
- **Interacción con otras capas/paquetes:** usa `WorldInitializer` y definiciones de `world.ports`.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`LevelBasic`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/level/LevelBasic.java).
- **Patrones de diseño relevantes:** Template Method.
- **Puntos de atención:** parámetros de posición pueden afectar jugabilidad.

#### Subpaquete: `game.implementations.world`
**Descripción:** Proveedores concretos de definiciones de mundo.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Definiciones concretas. | Proveer mundos aleatorios o centrados. |

**Análisis detallado**
- **Propósito:** Proveer instancias de `WorldDefinition` con assets y prototipos.
- **Responsabilidades principales:** construir catálogos de assets y listas de definiciones.
- **Interacción con otras capas/paquetes:** consumido por `game` y `controller` al iniciar el mundo.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`EarthInCenterWorldDefinitionProvider`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/world/EarthInCenterWorldDefinitionProvider.java), [`RandomWorldDefinitionProvider`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/game/implementations/world/RandomWorldDefinitionProvider.java).
- **Patrones de diseño relevantes:** Factory.
- **Puntos de atención:** asegurar consistencia de assets referenciados en `resources`.

---

## Paquete: `model` (`src/model`)
**Descripción:** Núcleo de simulación y física; gestiona entidades, eventos de dominio y snapshots.

### Resumen del paquete
| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `model.bodies` | Entidades físicas. | Cuerpos dinámicos/estáticos, DTOs, fábrica. |
| `model.emitter` | Emisores de partículas/trails. | Emitters y DTOs de configuración. |
| `model.implementations` | Modelo concreto. | Estado de simulación y ciclo de vida. |
| `model.physics` | Motor físico. | Actualización de física y DTOs de valores. |
| `model.ports` | Interfaces del modelo. | Estado del modelo y procesador de eventos. |
| `model.spatial` | Partitioning espacial. | Spatial grid y estadísticas. |
| `model.weapons` | Armas y proyectiles. | Modelado de armas y factory. |

> Nota: `model` contiene más de 15 clases; se listan las más relevantes. Otros archivos no listados — ver enlace al directorio.

### Análisis detallado
- **Propósito:** Ejecutar la simulación del mundo, detectar eventos y producir snapshots de estado.
- **Responsabilidades principales:** gestión de cuerpos, colisiones, física, armas y eventos de dominio.
- **Interacción con otras capas/paquetes:** expone DTOs al `controller` y usa `DomainEventProcessor` para reglas.
- **Concurrencia / threading:** cuerpos dinámicos pueden correr en threads propios; colecciones concurrentes para entidades.
- **Principales clases/interfaces/DTOs:** [`Model`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/implementations/Model.java), [`AbstractBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/core/AbstractBody.java), [`DynamicBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/implementations/DynamicBody.java), [`SpatialGrid`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/spatial/core/SpatialGrid.java), [`AbstractPhysicsEngine`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/core/AbstractPhysicsEngine.java), [`AbstractWeapon`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/core/AbstractWeapon.java).
- **Patrones de diseño relevantes:** Strategy (física/armas), Factory (cuerpos/armas), Observer (eventos), DTO.
- **Puntos de atención:** cambios en threads o grid espacial pueden introducir condiciones de carrera.

**Detalles críticos**
- `SpatialGrid` es un hot path para colisiones; cambios en tamaño de celda impactan rendimiento. ([`SpatialGrid`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/spatial/core/SpatialGrid.java))
- Cada entidad dinámica puede ejecutar su propio thread; la sincronización con el modelo debe mantenerse consistente. ([`DynamicBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/implementations/DynamicBody.java))
- La entrega de snapshots al controlador depende de DTOs inmutables. ([`Model`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/implementations/Model.java))

### Subpaquete: `model.bodies`
**Descripción:** Abstracciones y DTOs para entidades físicas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `model.bodies.core` | Base de cuerpos. | Estado común y comportamiento base. |
| `model.bodies.implementations` | Implementaciones concretas. | Cuerpos dinámicos, jugadores, proyectiles. |
| `model.bodies.ports` | Contratos/DTOs. | Tipos, estados, factory y DTOs. |

**Análisis detallado**
- **Propósito:** Centralizar el modelado de cuerpos físicos.
- **Responsabilidades principales:** definir tipos de cuerpos, DTOs y estado asociado.
- **Interacción con otras capas/paquetes:** usado por `model.implementations.Model` y `controller` vía DTOs.
- **Concurrencia / threading:** cuerpos dinámicos pueden ser actualizados desde threads de simulación.
- **Principales clases/interfaces/DTOs:** [`AbstractBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/core/AbstractBody.java), [`BodyDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/BodyDTO.java), [`PlayerDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/PlayerDTO.java).
- **Patrones de diseño relevantes:** Factory, DTO.
- **Puntos de atención:** coherencia entre estados físicos y DTOs de salida.

#### Subpaquete: `model.bodies.core`
**Descripción:** Implementación base para entidades físicas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Clase base. | Estado y utilidades de cuerpos. |

**Análisis detallado**
- **Propósito:** Encapsular comportamiento común de cuerpos.
- **Responsabilidades principales:** estado base y utilidades de actualización.
- **Interacción con otras capas/paquetes:** extendido por implementaciones concretas.
- **Concurrencia / threading:** puede ser accedido desde threads de simulación.
- **Principales clases/interfaces/DTOs:** [`AbstractBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/core/AbstractBody.java).
- **Patrones de diseño relevantes:** Template Method (si aplica en subclases).
- **Puntos de atención:** asegurar consistencia de campos compartidos.

#### Subpaquete: `model.bodies.implementations`
**Descripción:** Cuerpos concretos del juego.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Cuerpos concretos. | Player, dinámicos, proyectiles, estáticos. |

**Análisis detallado**
- **Propósito:** Implementar comportamiento específico por tipo de cuerpo.
- **Responsabilidades principales:** lógica de movimiento, vida, y comportamiento particular.
- **Interacción con otras capas/paquetes:** instanciados por `Model` y factories.
- **Concurrencia / threading:** threads por entidad dinámica (`DynamicBody`).
- **Principales clases/interfaces/DTOs:** [`DynamicBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/implementations/DynamicBody.java), [`PlayerBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/implementations/PlayerBody.java), [`ProjectileBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/implementations/ProjectileBody.java), [`StaticBody`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/implementations/StaticBody.java).
- **Patrones de diseño relevantes:** Strategy (diferentes comportamientos), Factory.
- **Puntos de atención:** sincronización de estado entre threads y model.

#### Subpaquete: `model.bodies.ports`
**Descripción:** Contratos y DTOs de cuerpos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | DTOs y tipos. | DTOs, estados, factory, tipos de cuerpo. |

**Análisis detallado**
- **Propósito:** Definir la interfaz estable para cuerpos.
- **Responsabilidades principales:** DTOs, enums y factories de cuerpos.
- **Interacción con otras capas/paquetes:** consumido por `controller` y `model`.
- **Concurrencia / threading:** DTOs inmutables para intercambio.
- **Principales clases/interfaces/DTOs:** [`BodyDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/BodyDTO.java), [`PlayerDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/PlayerDTO.java), [`BodyFactory`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/BodyFactory.java), [`BodyType`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/BodyType.java), [`BodyState`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/BodyState.java), [`BodyEventProcessor`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/bodies/ports/BodyEventProcessor.java).
- **Patrones de diseño relevantes:** Factory, DTO.
- **Puntos de atención:** mantener compatibilidad de DTOs con mappers.

### Subpaquete: `model.emitter`
**Descripción:** Emisores de partículas/trails asociados a entidades.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `model.emitter.core` | Base de emisores. | Lógica común de emisión. |
| `model.emitter.implementations` | Emisores concretos. | Burst y basic emitters. |
| `model.emitter.ports` | Interfaces/DTOs. | Configuración de emitters. |

**Análisis detallado**
- **Propósito:** Gestionar emisores visuales asociados a cuerpos.
- **Responsabilidades principales:** instanciación y configuración de emitters.
- **Interacción con otras capas/paquetes:** `controller` los configura vía DTOs de `world`.
- **Concurrencia / threading:** emitters actualizados dentro del ciclo de simulación.
- **Principales clases/interfaces/DTOs:** [`AbstractEmitter`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/emitter/core/AbstractEmitter.java), [`EmitterConfigDto`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/emitter/ports/EmitterConfigDto.java).
- **Patrones de diseño relevantes:** Factory, Strategy.
- **Puntos de atención:** coherencia de lifetime de partículas.

#### Subpaquete: `model.emitter.core`
**Descripción:** Base abstracta de emisores.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Emisor base. | Control de emisión y estado. |

**Análisis detallado**
- **Propósito:** Encapsular comportamiento común de emitters.
- **Responsabilidades principales:** timing de emisión, estado interno.
- **Interacción con otras capas/paquetes:** heredado por implementaciones.
- **Concurrencia / threading:** ejecutado dentro del loop de simulación.
- **Principales clases/interfaces/DTOs:** [`AbstractEmitter`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/emitter/core/AbstractEmitter.java).
- **Patrones de diseño relevantes:** Template Method.
- **Puntos de atención:** asegurar limpieza de emitters finalizados.

#### Subpaquete: `model.emitter.implementations`
**Descripción:** Implementaciones concretas de emitters.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Emitters concretos. | Emisión básica y en ráfaga. |

**Análisis detallado**
- **Propósito:** Definir comportamiento concreto de emisión.
- **Responsabilidades principales:** tipos de emisión (basic/burst).
- **Interacción con otras capas/paquetes:** instanciados por `Model` o factories.
- **Concurrencia / threading:** ejecutados en loop de simulación.
- **Principales clases/interfaces/DTOs:** [`BasicEmitter`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/emitter/implementations/BasicEmitter.java), [`BurstEmitter`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/emitter/implementations/BurstEmitter.java).
- **Patrones de diseño relevantes:** Strategy.
- **Puntos de atención:** performance de emisión masiva.

#### Subpaquete: `model.emitter.ports`
**Descripción:** Contratos y DTOs de emitters.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Interfaces/DTOs. | API de emitters y configuración. |

**Análisis detallado**
- **Propósito:** Definir interfaz estable para configuración de emitters.
- **Responsabilidades principales:** representar configuración y contratos.
- **Interacción con otras capas/paquetes:** consumido por `controller` y `game`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`Emitter`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/emitter/ports/Emitter.java), [`EmitterConfigDto`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/emitter/ports/EmitterConfigDto.java).
- **Patrones de diseño relevantes:** DTO.
- **Puntos de atención:** mantener compatibilidad de DTOs con world definitions.

### Subpaquete: `model.implementations`
**Descripción:** Implementación principal del modelo.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Modelo concreto. | Ciclo de vida del engine, snapshots, eventos. |

**Análisis detallado**
- **Propósito:** Orquestar la simulación completa.
- **Responsabilidades principales:** administrar entidades, snapshots, eventos y estados.
- **Interacción con otras capas/paquetes:** expone DTOs al `controller` y usa `DomainEventProcessor`.
- **Concurrencia / threading:** administra colecciones concurrentes de entidades.
- **Principales clases/interfaces/DTOs:** [`Model`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/implementations/Model.java).
- **Patrones de diseño relevantes:** Facade, Observer.
- **Puntos de atención:** mantener consistencia de snapshots entre threads.

### Subpaquete: `model.physics`
**Descripción:** Motores y DTOs de física.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `model.physics.core` | Implementación base. | Comportamiento general del motor físico. |
| `model.physics.implementations` | Motores concretos. | Física básica o nula. |
| `model.physics.ports` | Interfaces/DTOs. | Contrato y valores físicos. |

**Análisis detallado**
- **Propósito:** Centralizar la actualización física de entidades.
- **Responsabilidades principales:** cálculos de velocidad, aceleración, rebotes.
- **Interacción con otras capas/paquetes:** usado por cuerpos (`model.bodies`).
- **Concurrencia / threading:** ejecutado dentro del thread de cada cuerpo.
- **Principales clases/interfaces/DTOs:** [`AbstractPhysicsEngine`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/core/AbstractPhysicsEngine.java), [`PhysicsEngine`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/ports/PhysicsEngine.java).
- **Patrones de diseño relevantes:** Strategy.
- **Puntos de atención:** evitar cálculos costosos por frame.

#### Subpaquete: `model.physics.core`
**Descripción:** Base abstracta para motores físicos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Motor base. | Lógica común de física. |

**Análisis detallado**
- **Propósito:** Implementar comportamiento común para motores.
- **Responsabilidades principales:** cálculos base y utilidades.
- **Interacción con otras capas/paquetes:** extendido por motores concretos.
- **Concurrencia / threading:** ejecutado en threads de cuerpos.
- **Principales clases/interfaces/DTOs:** [`AbstractPhysicsEngine`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/core/AbstractPhysicsEngine.java).
- **Patrones de diseño relevantes:** Template Method.
- **Puntos de atención:** mantener coherencia numérica en cálculos.

#### Subpaquete: `model.physics.implementations`
**Descripción:** Motores físicos concretos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Motores concretos. | Física básica y placeholder nulo. |

**Análisis detallado**
- **Propósito:** Proveer estrategias físicas concretas.
- **Responsabilidades principales:** definir cálculos de física básica o nula.
- **Interacción con otras capas/paquetes:** instanciado por cuerpos o factories.
- **Concurrencia / threading:** ejecutado en threads de cuerpos.
- **Principales clases/interfaces/DTOs:** [`BasicPhysicsEngine`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/implementations/BasicPhysicsEngine.java), [`NullPhysicsEngine`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/implementations/NullPhysicsEngine.java).
- **Patrones de diseño relevantes:** Strategy.
- **Puntos de atención:** mantener performance estable.

#### Subpaquete: `model.physics.ports`
**Descripción:** Interfaces y DTOs de física.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Contratos físicos. | API de motores y valores físicos. |

**Análisis detallado**
- **Propósito:** Definir contratos para el motor de física.
- **Responsabilidades principales:** interfaces y DTOs de valores físicos.
- **Interacción con otras capas/paquetes:** usado por cuerpos y model.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`PhysicsEngine`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/ports/PhysicsEngine.java), [`PhysicsValuesDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/physics/ports/PhysicsValuesDTO.java).
- **Patrones de diseño relevantes:** DTO.
- **Puntos de atención:** compatibilidad con mappers/DTOs de render.

### Subpaquete: `model.ports`
**Descripción:** Contratos del modelo.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Interfaces. | Estado del modelo y procesamiento de eventos. |

**Análisis detallado**
- **Propósito:** Definir contratos estables del modelo.
- **Responsabilidades principales:** estado del modelo y procesador de eventos.
- **Interacción con otras capas/paquetes:** usado por `controller` y `model`.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`ModelState`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/ports/ModelState.java), [`DomainEventProcessor`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/ports/DomainEventProcessor.java).
- **Patrones de diseño relevantes:** Ports & Adapters.
- **Puntos de atención:** cambios pueden romper `controller`.

### Subpaquete: `model.spatial`
**Descripción:** Partitioning espacial y métricas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `model.spatial.core` | Grid espacial. | Celdas, inserción, consultas. |
| `model.spatial.ports` | DTOs de métricas. | Estadísticas de grid. |

**Análisis detallado**
- **Propósito:** Optimizar colisiones por particionado espacial.
- **Responsabilidades principales:** insertar entidades y calcular estadísticas.
- **Interacción con otras capas/paquetes:** usado por `model` y `controller` (stats).
- **Concurrencia / threading:** actualizado desde threads de simulación.
- **Principales clases/interfaces/DTOs:** [`SpatialGrid`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/spatial/core/SpatialGrid.java), [`SpatialGridStatisticsDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/spatial/ports/SpatialGridStatisticsDTO.java).
- **Patrones de diseño relevantes:** Spatial Partitioning.
- **Puntos de atención:** tuning de tamaño de celda.

#### Subpaquete: `model.spatial.core`
**Descripción:** Implementación del grid espacial.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Grid espacial. | Partición espacial y colisiones. |

**Análisis detallado**
- **Propósito:** Implementar el grid de colisiones.
- **Responsabilidades principales:** manejo de celdas y consultas.
- **Interacción con otras capas/paquetes:** usado por el modelo.
- **Concurrencia / threading:** compartido por threads de cuerpos.
- **Principales clases/interfaces/DTOs:** [`SpatialGrid`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/spatial/core/SpatialGrid.java), [`Cells`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/spatial/core/Cells.java).
- **Patrones de diseño relevantes:** Spatial Hash/Grid.
- **Puntos de atención:** evitar locking excesivo.

#### Subpaquete: `model.spatial.ports`
**Descripción:** DTOs de estadísticas del grid.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | DTOs de métricas. | Datos estadísticos para debug. |

**Análisis detallado**
- **Propósito:** Proveer métricas de la partición espacial.
- **Responsabilidades principales:** encapsular estadísticas para la vista.
- **Interacción con otras capas/paquetes:** consumido por `controller` → `view`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`SpatialGridStatisticsDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/spatial/ports/SpatialGridStatisticsDTO.java).
- **Patrones de diseño relevantes:** DTO.
- **Puntos de atención:** mantener consistencia con render stats.

### Subpaquete: `model.weapons`
**Descripción:** Armas, estados y factories.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `model.weapons.core` | Base de armas. | Comportamiento y estado general. |
| `model.weapons.implementations` | Armas concretas. | Basic, burst, misiles, minas. |
| `model.weapons.ports` | Contratos/DTOs. | Tipos, factory, DTOs. |

**Análisis detallado**
- **Propósito:** Modelar armas del jugador y proyectiles.
- **Responsabilidades principales:** crear armas, estados y DTOs para render.
- **Interacción con otras capas/paquetes:** creado por `Model` y configurado por `controller`.
- **Concurrencia / threading:** ejecutado en threads de entidades que disparan.
- **Principales clases/interfaces/DTOs:** [`AbstractWeapon`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/core/AbstractWeapon.java), [`WeaponFactory`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/ports/WeaponFactory.java).
- **Patrones de diseño relevantes:** Strategy, Factory.
- **Puntos de atención:** balance de cadencias y ammo.

#### Subpaquete: `model.weapons.core`
**Descripción:** Base abstracta de armas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Arma base. | Comportamiento común. |

**Análisis detallado**
- **Propósito:** Encapsular comportamiento común de armas.
- **Responsabilidades principales:** estado base y utilidades.
- **Interacción con otras capas/paquetes:** extendido por implementaciones concretas.
- **Concurrencia / threading:** ejecutado por threads de entidades.
- **Principales clases/interfaces/DTOs:** [`AbstractWeapon`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/core/AbstractWeapon.java).
- **Patrones de diseño relevantes:** Template Method.
- **Puntos de atención:** mantener consistencia de cooldown.

#### Subpaquete: `model.weapons.implementations`
**Descripción:** Implementaciones concretas de armas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Armas concretas. | Weapon básica, burst, misiles, minas. |

**Análisis detallado**
- **Propósito:** Definir estrategias de disparo concretas.
- **Responsabilidades principales:** lógica de firing por tipo de arma.
- **Interacción con otras capas/paquetes:** instanciadas por factory del modelo.
- **Concurrencia / threading:** ejecutadas dentro de threads de jugadores.
- **Principales clases/interfaces/DTOs:** [`BasicWeapon`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/implementations/BasicWeapon.java), [`BurstWeapon`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/implementations/BurstWeapon.java), [`MissileLauncher`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/implementations/MissileLauncher.java), [`MineLauncher`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/implementations/MineLauncher.java).
- **Patrones de diseño relevantes:** Strategy.
- **Puntos de atención:** ajustes de cooldown pueden romper balance.

#### Subpaquete: `model.weapons.ports`
**Descripción:** Contratos y DTOs de armas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Interfaces/DTOs. | Tipos, estados, factory, DTOs. |

**Análisis detallado**
- **Propósito:** Definir contratos estables para armas.
- **Responsabilidades principales:** DTOs, enums y factory.
- **Interacción con otras capas/paquetes:** usado por `controller` y `model`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`Weapon`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/ports/Weapon.java), [`WeaponFactory`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/ports/WeaponFactory.java), [`WeaponType`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/ports/WeaponType.java), [`WeaponState`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/ports/WeaponState.java), [`WeaponDto`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/model/weapons/ports/WeaponDto.java).
- **Patrones de diseño relevantes:** Factory, DTO.
- **Puntos de atención:** mantener compatibilidad con definiciones de `world`.

---

## Paquete: `resources` (`src/resources`)
**Descripción:** Contiene assets no Java utilizados por el engine.

### Resumen del paquete
| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `resources.images` | Imágenes. | Sprites y fondos. |

### Análisis detallado
- **Propósito:** Proveer assets estáticos fuera del classpath Java.
- **Responsabilidades principales:** almacenar imágenes usadas por `utils.images` y `view`.
- **Interacción con otras capas/paquetes:** consumido indirectamente por `utils.assets` y `view`.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** N/A (no Java).
- **Patrones de diseño relevantes:** N/A.
- **Puntos de atención:** mantener naming consistente con `AssetCatalog`.

---

## Paquete: `utils` (`src/utils`)
**Descripción:** Infraestructura transversal (assets, eventos, helpers, imágenes, efectos).

### Resumen del paquete
| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `utils.actions` | DTOs de acción. | Representar acciones de dominio. |
| `utils.assets` | Assets del juego. | Catalogación y tipos de assets. |
| `utils.events` | Eventos de dominio. | Tipos, payloads y referencias. |
| `utils.fx` | FX visuales. | Modelado de efectos. |
| `utils.helpers` | Utilidades. | Vectores, listas aleatorias. |
| `utils.images` | Carga de imágenes. | Cache y DTOs de imágenes. |

> Nota: `utils` contiene más de 15 clases; se listan las más relevantes. Otros archivos no listados — ver enlace al directorio.

### Análisis detallado
- **Propósito:** Proveer componentes transversales reutilizables.
- **Responsabilidades principales:** catálogos de assets, eventos de dominio y helpers utilitarios.
- **Interacción con otras capas/paquetes:** usado por `model`, `view`, `controller` y `game`.
- **Concurrencia / threading:** algunos DTOs viajan entre threads, pero no hay threads propios.
- **Principales clases/interfaces/DTOs:** [`AssetCatalog`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/core/AssetCatalog.java), [`Images`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/images/Images.java).
- **Patrones de diseño relevantes:** DTO, Value Object.
- **Puntos de atención:** mantener consistencia en IDs de assets/eventos.

### Subpaquete: `utils.actions`
**Descripción:** Modelos de acciones generadas por reglas.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Acciones. | Representar acciones en el dominio. |

**Análisis detallado**
- **Propósito:** Representar acciones de dominio.
- **Responsabilidades principales:** encapsular comandos para el modelo.
- **Interacción con otras capas/paquetes:** usado por `game` y `model`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`Action`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/actions/Action.java), [`ActionDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/actions/ActionDTO.java).
- **Patrones de diseño relevantes:** Command/DTO.
- **Puntos de atención:** mantener compatibilidad con acciones generadas en `game`.

### Subpaquete: `utils.assets`
**Descripción:** Catálogo de assets y metadatos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `utils.assets.core` | Catálogo. | Registro de assets. |
| `utils.assets.implementations` | Implementación concreta. | Assets del proyecto. |
| `utils.assets.ports` | DTOs y enums. | Tipos e intensidades. |

**Análisis detallado**
- **Propósito:** Centralizar definición de assets visuales.
- **Responsabilidades principales:** registrar assets y exponer metadatos.
- **Interacción con otras capas/paquetes:** consumido por `view` y `game` al cargar assets.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`AssetCatalog`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/core/AssetCatalog.java), [`ProjectAssets`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/implementations/ProjectAssets.java), [`AssetInfoDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/ports/AssetInfoDTO.java), [`AssetType`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/ports/AssetType.java), [`AssetIntensity`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/ports/AssetIntensity.java).
- **Patrones de diseño relevantes:** Catalog/Registry, DTO.
- **Puntos de atención:** asegurar que los IDs de assets coincidan con `resources`.

#### Subpaquete: `utils.assets.core`
**Descripción:** Catálogo base de assets.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Catálogo. | Registro y lookup de assets. |

**Análisis detallado**
- **Propósito:** Registrar assets por ID y tipo.
- **Responsabilidades principales:** almacenar rutas y metadatos.
- **Interacción con otras capas/paquetes:** usado por `view` y `game`.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`AssetCatalog`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/core/AssetCatalog.java).
- **Patrones de diseño relevantes:** Registry.
- **Puntos de atención:** control de IDs duplicados.

#### Subpaquete: `utils.assets.implementations`
**Descripción:** Implementación concreta de assets del proyecto.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Assets concretos. | Configuración de assets del juego. |

**Análisis detallado**
- **Propósito:** Definir el set concreto de assets.
- **Responsabilidades principales:** agregar assets al catálogo con paths y tipos.
- **Interacción con otras capas/paquetes:** consumido por `game` para inicialización.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`ProjectAssets`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/implementations/ProjectAssets.java).
- **Patrones de diseño relevantes:** Factory/Builder (manual).
- **Puntos de atención:** mantener paths válidos.

#### Subpaquete: `utils.assets.ports`
**Descripción:** DTOs y enums de assets.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | DTOs/enums. | Tipos, intensidades y DTOs. |

**Análisis detallado**
- **Propósito:** Definir contratos de assets.
- **Responsabilidades principales:** representar tipos e intensidades.
- **Interacción con otras capas/paquetes:** usado en `view` y `game`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`AssetInfoDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/ports/AssetInfoDTO.java), [`AssetType`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/ports/AssetType.java), [`AssetIntensity`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/assets/ports/AssetIntensity.java).
- **Patrones de diseño relevantes:** DTO.
- **Puntos de atención:** mantener enums en sync con assets reales.

### Subpaquete: `utils.events`
**Descripción:** Eventos de dominio usados en el pipeline de simulación.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `utils.events.domain` | Eventos de dominio. | Tipos, payloads y DTOs. |

**Análisis detallado**
- **Propósito:** Agrupar eventos de dominio y sus payloads.
- **Responsabilidades principales:** definir eventos y payloads asociados.
- **Interacción con otras capas/paquetes:** consumido por `model` y `controller`.
- **Concurrencia / threading:** eventos pasan entre threads de simulación.
- **Patrones de diseño relevantes:** Event/Observer.
- **Puntos de atención:** mantener consistencia entre tipos y payloads.

#### Subpaquete: `utils.events.domain`
**Descripción:** Eventos y payloads de dominio.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `utils.events.domain.core` | Base de eventos. | Evento abstracto. |
| `utils.events.domain.ports` | DTOs y tipos. | Tipos de evento y payloads. |

**Análisis detallado**
- **Propósito:** Centralizar la definición de eventos de dominio.
- **Responsabilidades principales:** base abstracta y contratos de eventos.
- **Interacción con otras capas/paquetes:** usado por `model` para detección y `game` para reglas.
- **Concurrencia / threading:** eventos viajan entre threads; DTOs inmutables.
- **Patrones de diseño relevantes:** Observer/Event.
- **Puntos de atención:** garantizar compatibilidad entre eventos y acciones.

##### Subpaquete: `utils.events.domain.core`
**Descripción:** Base abstracta de eventos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Evento base. | Comportamiento común de eventos. |

**Análisis detallado**
- **Propósito:** Proveer base común para eventos.
- **Responsabilidades principales:** estructura base de eventos.
- **Interacción con otras capas/paquetes:** extendido por eventos concretos.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`AbstractDomainEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/core/AbstractDomainEvent.java).
- **Patrones de diseño relevantes:** Event base.
- **Puntos de atención:** asegurar serialización/immutabilidad.

##### Subpaquete: `utils.events.domain.ports`
**Descripción:** DTOs y tipos de eventos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `utils.events.domain.ports.eventtype` | Tipos de evento. | Colisión, límite, emisión, etc. |
| `utils.events.domain.ports.payloads` | Payloads de evento. | Datos específicos de eventos. |

**Análisis detallado**
- **Propósito:** Definir contratos y payloads de eventos.
- **Responsabilidades principales:** tipos de evento y DTOs de payload.
- **Interacción con otras capas/paquetes:** consumido por `model` y `game`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`DomainEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/DomainEvent.java), [`CollisionEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/CollisionEvent.java), [`LimitEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/LimitEvent.java), [`EmitEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/EmitEvent.java), [`LifeOver`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/LifeOver.java), [`DomainEventType`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/DomainEventType.java), [`BodyRefDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/BodyRefDTO.java), [`BodyToEmitDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/BodyToEmitDTO.java), [`DomainEventPayload`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/DomainEventPayload.java), [`CollisionPayload`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/CollisionPayload.java), [`EmitPayloadDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/EmitPayloadDTO.java), [`NoPayload`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/NoPayload.java).
- **Patrones de diseño relevantes:** Event/DTO.
- **Puntos de atención:** mantener correspondencia entre tipos y payloads.

###### Subpaquete: `utils.events.domain.ports.eventtype`
**Descripción:** Tipos de eventos de dominio.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Tipos de evento. | Eventos de colisión, límites, emisión, vida. |

**Análisis detallado**
- **Propósito:** Enumerar eventos concretos del dominio.
- **Responsabilidades principales:** contratos de eventos y datos asociados.
- **Interacción con otras capas/paquetes:** usado por `model` y `game`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`DomainEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/DomainEvent.java), [`CollisionEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/CollisionEvent.java), [`LimitEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/LimitEvent.java), [`EmitEvent`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/EmitEvent.java), [`LifeOver`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/eventtype/LifeOver.java).
- **Patrones de diseño relevantes:** Event.
- **Puntos de atención:** mantener consistencia con `DomainEventType`.

###### Subpaquete: `utils.events.domain.ports.payloads`
**Descripción:** Payloads de eventos de dominio.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Payloads. | DTOs con datos específicos de eventos. |

**Análisis detallado**
- **Propósito:** Encapsular datos asociados a eventos.
- **Responsabilidades principales:** payloads de colisión, emisión, etc.
- **Interacción con otras capas/paquetes:** usados por `model` y `game`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`DomainEventPayload`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/DomainEventPayload.java), [`CollisionPayload`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/CollisionPayload.java), [`EmitPayloadDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/EmitPayloadDTO.java), [`NoPayload`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/events/domain/ports/payloads/NoPayload.java).
- **Patrones de diseño relevantes:** DTO.
- **Puntos de atención:** mantener payloads en sync con `DomainEventType`.

### Subpaquete: `utils.fx`
**Descripción:** Modelos de efectos visuales.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | FX. | Tipos de FX y sprites de efectos. |

**Análisis detallado**
- **Propósito:** Representar efectos visuales asociados a entidades.
- **Responsabilidades principales:** tipos y configuración de FX.
- **Interacción con otras capas/paquetes:** consumido por `view`/`model` según efecto.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`Fx`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/fx/Fx.java), [`FxType`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/fx/FxType.java), [`FxImage`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/fx/FxImage.java), [`Spin`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/fx/Spin.java).
- **Patrones de diseño relevantes:** Value Object.
- **Puntos de atención:** evitar duplicación de FX en assets.

### Subpaquete: `utils.helpers`
**Descripción:** Utilidades matemáticas y colecciones.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Helpers. | Vectores y estructuras auxiliares. |

**Análisis detallado**
- **Propósito:** Proveer utilidades reusables.
- **Responsabilidades principales:** operaciones con vectores y listas random.
- **Interacción con otras capas/paquetes:** usado por `game` y `model`.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`DoubleVector`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/helpers/DoubleVector.java), [`RandomArrayList`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/helpers/RandomArrayList.java).
- **Patrones de diseño relevantes:** Value Object.
- **Puntos de atención:** evitar mutabilidad en utilidades compartidas.

### Subpaquete: `utils.images`
**Descripción:** Carga y cacheo de imágenes.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Images. | Cache, DTOs y acceso a sprites. |

**Análisis detallado**
- **Propósito:** Manejar carga y cache de imágenes.
- **Responsabilidades principales:** cache por clave, DTOs de imagen.
- **Interacción con otras capas/paquetes:** usado por `view` para cargar sprites.
- **Concurrencia / threading:** puede ser accedido desde render thread; cache debe ser thread-safe.
- **Principales clases/interfaces/DTOs:** [`Images`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/images/Images.java), [`ImageDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/images/ImageDTO.java), [`ImageCache`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/images/ImageCache.java), [`ImageCacheKeyDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/utils/images/ImageCacheKeyDTO.java).
- **Patrones de diseño relevantes:** Cache, DTO.
- **Puntos de atención:** evitar cargas duplicadas en runtime.

---

## Paquete: `view` (`src/view`)
**Descripción:** Capa de presentación Swing, render loop activo, HUDs y DTOs de render.

### Resumen del paquete
| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `view.core` | UI y render loop. | Ventana Swing, input, render thread. |
| `view.huds` | HUDs y widgets. | UI overlays y métricas. |
| `view.renderables` | DTOs/renderables. | Representaciones visuales de entidades. |

> Nota: `view` contiene más de 15 clases; se listan las más relevantes. Otros archivos no listados — ver enlace al directorio.

### Análisis detallado
- **Propósito:** Renderizar el mundo y capturar input del usuario.
- **Responsabilidades principales:** render loop, HUDs, gestión de sprites y DTOs visuales.
- **Interacción con otras capas/paquetes:** `view.core.View` interactúa directamente con `controller.implementations.Controller`; no accede a `model`.
- **Concurrencia / threading:** render loop activo en un thread dedicado (`Renderer`).
- **Principales clases/interfaces/DTOs:** [`View`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/View.java), [`Renderer`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/Renderer.java), [`Renderable`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/renderables/implementations/Renderable.java).
- **Patrones de diseño relevantes:** MVC, DTO, Active Object.
- **Puntos de atención:** el render loop es hot path y depende de snapshots coherentes.

**Detalles críticos**
- `Renderer` usa estrategia de doble/triple buffer; cambios pueden impactar estabilidad visual. ([`Renderer`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/Renderer.java))
- El render thread consulta snapshots por frame; latencia de `Controller` afecta el frame pacing. ([`View`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/View.java))
- Actualización de renderables estáticos se hace por push para evitar costo per-frame. ([`View`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/View.java))

### Subpaquete: `view.core`
**Descripción:** Núcleo de UI y render loop.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | UI/Rendering. | Ventana, render thread, input y assets. |

**Análisis detallado**
- **Propósito:** Gestionar la ventana principal y el render loop.
- **Responsabilidades principales:** input, render, carga de assets visuales.
- **Interacción con otras capas/paquetes:** acceso directo a `controller` para comandos y DTOs.
- **Concurrencia / threading:** `Renderer` corre en thread dedicado; el resto usa EDT de Swing.
- **Principales clases/interfaces/DTOs:** [`View`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/View.java), [`Renderer`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/Renderer.java), [`ControlPanel`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/ControlPanel.java), [`SystemDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/core/SystemDTO.java).
- **Patrones de diseño relevantes:** MVC, Active Object.
- **Puntos de atención:** coordinar correctamente EDT vs render thread.

### Subpaquete: `view.huds`
**Descripción:** HUDs y widgets reutilizables.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `view.huds.core` | Componentes base. | Ítems de UI, barras y texto. |
| `view.huds.implementations` | HUDs concretos. | HUD de jugador, sistema, grid. |

**Análisis detallado**
- **Propósito:** Separar HUDs de la lógica de render principal.
- **Responsabilidades principales:** dibujar overlays y métricas.
- **Interacción con otras capas/paquetes:** consumido por `Renderer`; no toca `model`.
- **Concurrencia / threading:** ejecutado en render thread.
- **Patrones de diseño relevantes:** Composite/Renderer.
- **Puntos de atención:** HUDs costosos pueden afectar FPS.

#### Subpaquete: `view.huds.core`
**Descripción:** Componentes base de HUD.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Componentes base. | Items, textos, barras, separadores. |

**Análisis detallado**
- **Propósito:** Proveer piezas reutilizables de HUD.
- **Responsabilidades principales:** dibujo de texto, barras y layout básico.
- **Interacción con otras capas/paquetes:** utilizado por HUDs concretos.
- **Concurrencia / threading:** render thread.
- **Principales clases/interfaces/DTOs:** [`GridHUD`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/core/GridHUD.java), [`DataHUD`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/core/DataHUD.java), [`Item`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/core/Item.java), [`TextItem`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/core/TextItem.java), [`BarItem`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/core/BarItem.java).
- **Patrones de diseño relevantes:** Composite.
- **Puntos de atención:** mantener layout consistente en resoluciones distintas.

#### Subpaquete: `view.huds.implementations`
**Descripción:** HUDs concretos.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | HUDs concretos. | Visualización de jugador, sistema y grid. |

**Análisis detallado**
- **Propósito:** Proveer HUDs específicos del juego.
- **Responsabilidades principales:** mostrar estadísticas de jugador y sistema.
- **Interacción con otras capas/paquetes:** consumen DTOs de `controller`.
- **Concurrencia / threading:** render thread.
- **Principales clases/interfaces/DTOs:** [`PlayerHUD`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/implementations/PlayerHUD.java), [`SystemHUD`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/implementations/SystemHUD.java), [`SpatialGridHUD`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/huds/implementations/SpatialGridHUD.java).
- **Patrones de diseño relevantes:** Strategy/Composite.
- **Puntos de atención:** evitar cálculos pesados por frame.

### Subpaquete: `view.renderables`
**Descripción:** DTOs de render y clases de sprite.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `view.renderables.ports` | DTOs de render. | Datos de render por entidad. |
| `view.renderables.implementations` | Renderables concretos. | Sprites dinámicos/estáticos. |

**Análisis detallado**
- **Propósito:** Representar el estado visual de entidades.
- **Responsabilidades principales:** DTOs de render y lógica de paint.
- **Interacción con otras capas/paquetes:** DTOs provienen del `controller`.
- **Concurrencia / threading:** render thread y snapshots inmutables.
- **Principales clases/interfaces/DTOs:** [`RenderDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/renderables/ports/RenderDTO.java), [`DynamicRenderDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/renderables/ports/DynamicRenderDTO.java), [`PlayerRenderDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/renderables/ports/PlayerRenderDTO.java), [`SpatialGridStatisticsRenderDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/renderables/ports/SpatialGridStatisticsRenderDTO.java), [`Renderable`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/renderables/implementations/Renderable.java), [`DynamicRenderable`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/view/renderables/implementations/DynamicRenderable.java).
- **Patrones de diseño relevantes:** DTO.
- **Puntos de atención:** mantener inmutabilidad de DTOs.

---

## Paquete: `world` (`src/world`)
**Descripción:** Definiciones de mundo y DTOs para assets, ítems y armas.

### Resumen del paquete
| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| `world.core` | Fábricas/registro. | Registries y factories de definiciones. |
| `world.ports` | DTOs/contratos. | Definiciones de items, armas y world providers. |

### Análisis detallado
- **Propósito:** Centralizar definiciones de mundo reutilizables.
- **Responsabilidades principales:** definir assets, items, armas y estructuras de mundo.
- **Interacción con otras capas/paquetes:** consumido por `game` y `controller` durante la inicialización.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`WorldDefinition`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/WorldDefinition.java), [`WorldDefinitionProvider`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/WorldDefinitionProvider.java), [`DefItem`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefItem.java).
- **Patrones de diseño relevantes:** Factory, DTO.
- **Puntos de atención:** asegurar consistencia de definiciones con assets reales.

### Subpaquete: `world.core`
**Descripción:** Fábricas y registros de definiciones.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | Fábricas/registro. | Registrar y construir definiciones. |

**Análisis detallado**
- **Propósito:** Centralizar registro de definiciones y factories.
- **Responsabilidades principales:** registrar assets y armas definidas.
- **Interacción con otras capas/paquetes:** usado por `game` y `controller`.
- **Concurrencia / threading:** N/A.
- **Principales clases/interfaces/DTOs:** [`AbstractWorldDefinitionProvider`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/core/AbstractWorldDefinitionProvider.java), [`WorldAssetsRegister`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/core/WorldAssetsRegister.java), [`WeaponDefFactory`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/core/WeaponDefFactory.java), [`WeaponDefRegister`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/core/WeaponDefRegister.java).
- **Patrones de diseño relevantes:** Factory, Registry.
- **Puntos de atención:** mantener sincronía con `utils.assets`.

### Subpaquete: `world.ports`
**Descripción:** DTOs y contratos de definiciones.

| Paquete | Descripción breve | Responsabilidades clave |
| --- | --- | --- |
| — | DTOs/contratos. | Definición de items, armas, backgrounds. |

**Análisis detallado**
- **Propósito:** Definir contratos para definiciones de mundo.
- **Responsabilidades principales:** DTOs de items, armas, backgrounds.
- **Interacción con otras capas/paquetes:** consumido por `game` y `controller`.
- **Concurrencia / threading:** DTOs inmutables.
- **Principales clases/interfaces/DTOs:** [`WorldDefinition`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/WorldDefinition.java), [`WorldDefinitionProvider`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/WorldDefinitionProvider.java), [`DefItem`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefItem.java), [`DefItemDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefItemDTO.java), [`DefItemPrototypeDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefItemPrototypeDTO.java), [`DefBackgroundDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefBackgroundDTO.java), [`DefWeaponDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefWeaponDTO.java), [`DefWeaponType`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefWeaponType.java), [`DefEmitterDTO`](https://github.com/{repoOwner}/{repoName}/blob/{branch}/src/world/ports/DefEmitterDTO.java).
- **Patrones de diseño relevantes:** DTO.
- **Puntos de atención:** asegurar que las definiciones cubran todos los assets requeridos.

---

## Explora más
- Árbol del repositorio: https://github.com/{repoOwner}/{repoName}/tree/{branch}/src

## Limitaciones
- Este informe se generó a partir del contenido disponible en `src/`. Si hay módulos adicionales fuera de este árbol (por ejemplo `test/`), deberían analizarse manualmente para completar el panorama.

## Siguientes pasos sugeridos
1. Revisar la estabilidad del render loop y documentar los contratos de snapshot en `view.core.Renderer`.
2. Añadir pruebas unitarias a los mappers de `controller.mappers` y a las reglas de `game.implementations.actions`.
3. Auditar los parámetros de `model.spatial.SpatialGrid` y `model.physics` para balancear rendimiento vs. precisión.
