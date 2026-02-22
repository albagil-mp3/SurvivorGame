# Killer Game - Maze Chase Edition

Un juego de caza en laberinto inspirado en Pacman, creado con el framework MVCGameEngine.

## Descripción

Killer Game es un juego de acción en laberinto donde el jugador debe cazar y destruir a los enemigos que se mueven por el laberinto. A diferencia del Pacman clásico, aquí el jugador está armado y debe disparar a los enemigos para eliminarlos.

## Características

- **Laberinto**: Arena cuadrada de 1000x1000 píxeles con paredes que forman un laberinto
- **Jugador**: Nave espacial controlable (triángulo azul visualmente) que aparece en una esquina
- **Enemigos**: Asteroides que se mueven por el laberinto (círculos rojos visualmente)
- **Paredes**: Obstáculos estáticos (planetas) que bloquean el movimiento
- **Sistema de combate**: Los enemigos solo mueren al ser disparados, las colisiones causan rebote
- **Movimiento dinámico**: Los enemigos se mueven aleatoriamente rebotando en las paredes

## Controles

- **W** - Mover hacia adelante
- **S** - Mover hacia atrás
- **A** - Rotar a la izquierda
- **D** - Rotar a la derecha
- **ESPACIO** - Disparar

## Cómo ejecutar

### Opción 1: Compilar y ejecutar con Maven
```bash
mvn compile
java -cp target/classes killergame.KillerGameMain
```

### Opción 2: Compilar y ejecutar en un solo comando
```bash
mvn compile && java -cp target/classes killergame.KillerGameMain
```

## Arquitectura del juego

El juego sigue la arquitectura MVC del framework y está organizado en:

### Paquete: `killergame/`

- **KillerGameMain.java** - Punto de entrada del juego
- **KillerWorldDefinitionProvider.java** - Define el mundo del juego (assets, jugador, enemigos, paredes)
- **KillerLevelGenerator.java** - Genera el laberinto con paredes
- **KillerEnemySpawner.java** - IA que genera enemigos en posiciones aleatorias del laberinto
- **KillerGameRules.java** - Reglas del juego (colisiones, proyectiles, rebotes)
- **KillerGameAssets.java** - Catálogo de assets (actualmente sin uso, se usan los assets del proyecto principal)

## Reglas del juego

### Combate
1. **Proyectil + Enemigo**: Enemigo muere, proyectil se destruye
2. **Proyectil + Pared**: Proyectil se destruye

### Colisiones (rebote)
1. **Jugador + Enemigo**: Ambos rebotan (no hay daño)
2. **Jugador + Pared**: Jugador rebota
3. **Enemigo + Pared**: Enemigo rebota
4. **Enemigo + Enemigo**: Ambos rebotan

### Límites
- Todos los objetos rebotan en los límites del mundo

## Configuración

Puedes modificar los siguientes parámetros en `KillerGameMain.java`:

```java
// Dimensiones del laberinto (cuadrado)
DoubleVector viewDimension = new DoubleVector(1000, 1000);
DoubleVector worldDimension = new DoubleVector(1000, 1000);

// Número máximo de entidades
int maxBodies = 150; // Incluye jugador, enemigos y paredes

// Delay de spawn de enemigos (en segundos)
int maxEnemySpawnDelay = 3;
```

## Mecánicas del laberinto

### Estructura del laberinto
- **Paredes exteriores**: Crean el perímetro del laberinto
- **Paredes interiores**: Distribuidas en patrón simétrico con espacios centrales abiertos
- **Paredes adicionales**: Añaden complejidad al laberinto

### Movimiento de enemigos
- Los enemigos aparecen en posiciones aleatorias dentro del laberinto
- Se mueven con velocidad y dirección aleatoria inicial
- Rebotan en paredes y otros enemigos
- No persiguen ni huyen activamente del jugador (movimiento aleatorio)

## Extensiones futuras

Ideas para mejorar el juego:

- [ ] Sistema de puntuación (puntos por enemigo eliminado)
- [ ] Niveles de dificultad progresiva
- [ ] IA de huida real (enemigos que detectan y huyen del jugador)
- [ ] Diferentes tipos de enemigos (rápidos, lentos, grandes, pequeños)
- [ ] Power-ups dispersos por el laberinto
- [ ] Laberintos generados proceduralmente
- [ ] Sistema de vidas para el jugador
- [ ] Time attack mode (eliminar todos los enemigos en tiempo limitado)
- [ ] Efectos visuales y sonidos
- [ ] Pantalla de game over y estadísticas
- [ ] Assets personalizados (triángulo azul real y círculos rojos)
- [ ] Mini-mapa para navegar el laberinto

## Notas técnicas

Este juego fue creado siguiendo las mejores prácticas del framework MVCGameEngine:

✅ **No se modificó el paquete `engine/`**
✅ Se creó un paquete independiente `killergame/`
✅ Se implementaron todos los componentes requeridos:
  - WorldDefinitionProvider (con paredes del laberinto)
  - LevelGenerator (construye el laberinto)
  - IAGenerator (spawn de enemigos aleatorio)
  - ActionsGenerator (reglas de combate y rebote)
✅ Se mantiene la separación MVC
✅ Se usan DTOs para comunicación entre capas
✅ Mundo cuadrado (1000x1000) optimizado para laberinto
✅ Sistema de física para rebotes en paredes

## Gameplay

El objetivo es cazar a todos los enemigos que se mueven por el laberinto. Los enemigos se mueven de forma errática rebotando en las paredes, lo que los hace objetivos móviles difíciles de acertar. El jugador debe navegar por el laberinto, evitando colisiones innecesarias y usando sus proyectiles con precisión para eliminar a todos los enemigos.

## Autor

Creado siguiendo la guía del framework MVCGameEngine.
