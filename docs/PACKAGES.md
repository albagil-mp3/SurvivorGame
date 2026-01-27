# Análisis de estructura de paquetes y responsabilidades

## Mapa general de paquetes

- **game**: punto de entrada y orquestación inicial de la escena (arranque de MVC, reglas, mundo y AI). [`game.Main`](../src/game/Main.java) configura `Controller`, `Model`, `View`, reglas de acciones y generadores de mundo/AI.【F:src/game/Main.java†L1-L47】
- **controller**: coordinador central del MVC, integra View/Model, mapea DTOs de modelo a renderizables y ejecuta reglas del juego mediante `ActionsGenerator`. La clase `Controller` concentra estas responsabilidades.【F:src/controller/implementations/Controller.java†L1-L169】
- **model**: núcleo de simulación (entidades, física, eventos, colisiones, acciones). `Model` describe la gestión de entidades, la generación de eventos y el pipeline de acciones.【F:src/model/implementations/Model.java†L1-L164】
- **view**: capa de presentación y renderizado. `View` gestiona el ciclo de vida, carga de assets y entrada de usuario; `Renderer` ejecuta el render loop y dibuja snapshots.【F:src/view/core/View.java†L1-L105】【F:src/view/core/Renderer.java†L1-L117】
- **world**: definición del mundo (listas de entidades/armas/emitters, assets) y proveedores de configuración. `AbstractWorldDefinitionProvider` y `WorldDefinition` concentran esta estructura.【F:src/world/core/AbstractWorldDefinitionProvider.java†L1-L76】【F:src/world/ports/WorldDefinition.java†L1-L60】
- **assets**: catálogo y registro de recursos visuales. `AssetCatalog` y `ProjectAssets` definen y registran assets y su ubicación.【F:src/assets/core/AssetCatalog.java†L1-L114】【F:src/assets/implementations/ProjectAssets.java†L1-L122】
- **images**: carga y caching de imágenes (DTOs y utilidades de acceso). `Images` registra y carga archivos en memoria.【F:src/images/Images.java†L1-L138】
- **events/domain**: eventos de dominio y contratos de tipos. `AbstractDomainEvent` y `DomainEvent` estructuran los eventos y sus tipos sellados.【F:src/events/domain/core/AbstractDomainEvent.java†L1-L38】【F:src/events/domain/ports/eventtype/DomainEvent.java†L1-L6】
- **actions**: catálogo de acciones del motor (`Action`, `ActionDTO`) que conectan eventos con ejecución en el modelo.【F:src/actions/Action.java†L1-L16】【F:src/actions/ActionDTO.java†L1-L17】
- **fx**: animaciones/efectos (threads de FX, enum de tipos, imágenes). `Fx`, `FxImage`, `FxTyoe` definen esa infraestructura.【F:src/fx/Fx.java†L1-L120】【F:src/fx/FxImage.java†L1-L91】【F:src/fx/FxTyoe.java†L1-L11】
- **_helpers**: utilidades genéricas (`DoubleVector`, `RandomArrayList`).【F:src/_helpers/DoubleVector.java†L1-L120】【F:src/_helpers/RandomArrayList.java†L1-L72】

## Responsabilidades principales por paquete

- **game**: arranque y wiring del motor (MVC, reglas, mundo y AI).【F:src/game/Main.java†L25-L47】
- **controller**: puente entre View y Model; mapea DTOs para render y aplica reglas (acciones) desde eventos de dominio.【F:src/controller/implementations/Controller.java†L38-L110】
- **model**: simulación, eventos, acciones y control de entidades con partición espacial y límites del mundo.【F:src/model/implementations/Model.java†L52-L164】
- **view**: render loop, caching de imágenes y recepción de input de usuario. `View` coordina entrada y carga de assets; `Renderer` pinta snapshots y HUDs.【F:src/view/core/View.java†L28-L105】【F:src/view/core/Renderer.java†L30-L117】
- **world**: configuración declarativa de mundo, assets y listas de entidades/armas/emitters para inicialización del juego.【F:src/world/core/AbstractWorldDefinitionProvider.java†L30-L53】【F:src/world/ports/WorldDefinition.java†L9-L60】
- **assets/images**: inventario de recursos y carga de imágenes desde disco para el renderizado.【F:src/assets/core/AssetCatalog.java†L13-L114】【F:src/images/Images.java†L16-L138】
- **events/actions**: contratos de eventos de dominio y catálogo de acciones ejecutables por el motor.【F:src/events/domain/core/AbstractDomainEvent.java†L7-L38】【F:src/actions/Action.java†L1-L16】
- **fx**: efectos visuales (animaciones como `Spin`) y tipos asociados.【F:src/fx/Spin.java†L1-L33】【F:src/fx/FxTyoe.java†L1-L11】

## Posibles incoherencias / puntos a revisar

1. **Paquetes de render dispersos**: `images` y `fx` son claramente soporte del render, pero están en la raíz en lugar de vivir bajo `view` (que es la capa de presentación). Esto rompe un poco la agrupación conceptual por capa MVC; podría evaluarse moverlos a `view.images` y `view.fx` si se busca mayor cohesión con la capa de vista.【F:src/view/core/View.java†L16-L105】【F:src/images/Images.java†L1-L138】【F:src/fx/Fx.java†L1-L120】
2. **Nombre inconsistente**: el enum `FxTyoe` parece un typo de `FxType`, lo que puede complicar búsquedas y consistencia de API pública en el paquete `fx`.【F:src/fx/FxTyoe.java†L1-L11】
3. **Paquete `_helpers`**: el uso del prefijo `_` es distinto al resto de paquetes (que son nombres simples). Aunque funcional, puede considerarse inconsistente de estilo y podría moverse a un paquete `util`/`common` si se quiere estandarizar nomenclatura.【F:src/_helpers/DoubleVector.java†L1-L120】【F:src/_helpers/RandomArrayList.java†L1-L72】

## Clases fuera de su paquete

No encontré ejemplos de declaraciones de `package` fuera de su carpeta en los archivos inspeccionados (por ejemplo, `View`, `Model`, `Controller`, `WorldDefinition`).【F:src/view/core/View.java†L1-L2】【F:src/model/implementations/Model.java†L1-L2】【F:src/controller/implementations/Controller.java†L1-L2】【F:src/world/ports/WorldDefinition.java†L1-L2】
