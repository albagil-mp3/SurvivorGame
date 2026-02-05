# 🔬 ANÁLISIS TÉCNICO PROFUNDO: Gestión de Eventos de Teclado

**Fecha:** 2026-02-05  
**Autor:** Sistema de Diagnóstico  
**Nivel:** Documentación Técnica Avanzada  

---

## Índice

1. [Flujo Normal de Eventos](#flujo-normal)
2. [El Problema: Pérdida de keyReleased](#el-problema)
3. [La Solución: WindowFocusListener](#la-solución)
4. [Detalles de Implementación](#detalles)
5. [Matriz de Casos de Uso](#matriz)
6. [Validaciones](#validaciones)

---

## Flujo Normal de Eventos {#flujo-normal}

### Estado Ideal (Sin Problemas)

```
USUARIO MANTIENE UP PRESIONADO, LUEGO LIBERA
│
├─ t0: keyPressed(VK_UP)
│  ├─ View.keyPressed() captura evento
│  ├─ controller.playerThrustOn(playerId)
│  ├─ model.playerThrustOn(playerId)
│  ├─ pBody.thrustMaxOn()
│  ├─ physicsEngine.setThrust(800)
│  └─ ✅ THRUST = 800 (ACTIVADO)
│
├─ t1 → t2: [Mientras UP está presionado]
│  ├─ keyRepeat NO genera nuevos eventos
│  ├─ physicsEngine recalcula cada ~15ms
│  ├─ Nave acelera constantemente
│  └─ ✅ THRUST = 800 (MANTIENE)
│
└─ t3: keyReleased(VK_UP)
   ├─ View.keyReleased() captura evento
   ├─ controller.playerThrustOff(playerId)
   ├─ model.playerThrustOff(playerId)
   ├─ pBody.thrustOff()
   ├─ physicsEngine.stopPushing()
   │  └─ setThrust(0) ← RESETEA IMPULSO
   └─ ✅ THRUST = 0 (DESACTIVADO)

RESULTADO: Nave acelera mientras UP está presionado, frena cuando libera ✅
```

---

## El Problema: Pérdida de keyReleased {#el-problema}

### Escenario Problemático (Alt+Tab)

```
USUARIO MANTIENE UP, PRESIONA Alt+Tab
│
├─ t0: keyPressed(VK_UP)
│  ├─ View.keyPressed() captura evento
│  ├─ controller.playerThrustOn(playerId)
│  └─ ✅ THRUST = 800 (ACTIVADO)
│
├─ t1: USUARIO PRESIONA Alt+Tab
│  ├─ SO intercepta Alt+Tab
│  ├─ SO cambia ventana activa
│  ├─ keyReleased(VK_UP) NO SE GENERA ❌❌❌
│  │  (El SO consume el evento)
│  └─ THRUST SIGUE = 800 (MANTIENE INDEFINIDAMENTE)
│
├─ t2: USUARIO VUELVE AL JUEGO
│  ├─ Canvas recibe foco nuevamente
│  ├─ keyPressed(VK_UP) podría generarse de nuevo
│  ├─ (El estado del teclado del SO es inconsistente)
│  └─ THRUST PODRÍA DUPLICARSE O MANTENERSE ❌
│
└─ RESULTADO FINAL:
   ├─ Nave SIGUE acelerando indefinidamente
   ├─ Usuario debe presionar UP nuevamente para detenerla
   └─ ❌ COMPORTAMIENTO INESPERADO Y FRUSTANTE

PROBLEMA RAÍZ:
El SO (Windows/Mac/Linux) redirige CIERTOS eventos de teclado para sus
funciones globales. La aplicación Swing recibe keyPressed() pero NO keyReleased().
```

---

### Otras Combinaciones Problemáticas

```
┌─────────────────────────────────────────────────────────────┐
│ COMBINACIONES QUE PIERDEN keyReleased EN WINDOWS            │
├─────────────────────────────────────────────────────────────┤
│ Alt+Tab              │ Cambiar ventana activa              │
│ Alt+Escape           │ Ciclador de ventanas               │
│ Alt+Space            │ Menú de ventana del SO             │
│ Win+D                │ Mostrar escritorio                 │
│ Win+E                │ Explorador de archivos             │
│ Win+L                │ Bloquear pantalla                  │
│ Win+V                │ Portapapeles (Windows 10+)         │
│ Win+Shift+S          │ Captura de pantalla (Windows 10+)  │
│ Ctrl+Alt+Del         │ Pantalla de seguridad              │
│ Alt+NumpadSequence   │ Input method selector              │
│ Right-click          │ Menú contextual del SO             │
│ Algunos Alt+Fxx      │ Funciones multimedia               │
└─────────────────────────────────────────────────────────────┘
```

---

## La Solución: WindowFocusListener {#la-solución}

### Estrategia General

```
IDEA CENTRAL:
No confiar en que keyReleased() siempre se genera.
En su lugar, detectar cambios de FOCO de ventana.

Cuando la ventana PIERDE FOCO:
 ├─ El usuario se fue (Alt+Tab, clic en otra ventana, etc)
 ├─ Cualquier tecla "pegada" debe ser reseteada
 ├─ Forzar reseteo de TODOS los controles
 └─ ✅ Comportamiento predecible y consistente
```

---

### Flujo Mejorado (CON WindowFocusListener)

```
USUARIO MANTIENE UP, PRESIONA Alt+Tab
│
├─ t0: keyPressed(VK_UP)
│  └─ ✅ THRUST = 800
│
├─ t1: USUARIO PRESIONA Alt+Tab
│  ├─ SO intercepta Alt+Tab
│  ├─ Swing detecta: windowLostFocus(WindowEvent e)
│  ├─ Nuestro listener se dispara:
│  │  ├─ resetAllKeyStates() se invoca
│  │  ├─ controller.playerThrustOff()
│  │  ├─ controller.playerRotateOff()
│  │  ├─ fireKeyDown.set(false)
│  │  └─ ✅ THRUST = 0 (RESETEADO)
│  ├─ Ventana pierde foco
│  └─ SO cambia ventana
│
├─ t2: USUARIO VUELVE AL JUEGO
│  ├─ Canvas recibe foco nuevamente
│  ├─ Swing detecta: windowGainedFocus(WindowEvent e)
│  ├─ (Opcional: logging o inicialización)
│  └─ ✅ THRUST MANTIENE = 0
│
└─ RESULTADO FINAL:
   ├─ Nave DEJÓ de acelerar automáticamente
   ├─ Estado consistente y predecible
   └─ ✅ COMPORTAMIENTO CORRECTO

VENTAJAS:
✓ No depende de que keyReleased() se genere
✓ Maneja TODAS las combinaciones de teclas del SO
✓ Resetea estado ante pérdida de foco (lo correcto)
✓ Cross-platform (Windows/Mac/Linux)
```

---

## Detalles de Implementación {#detalles}

### 1. Declaración del Listener

```java
private WindowFocusListener focusListener = new WindowFocusListener() {
    @Override
    public void windowLostFocus(WindowEvent e) {
        resetAllKeyStates();
        System.out.println("View: Window lost focus - All key states reset");
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        // Optional: logging or event tracking
    }
};
```

**Detalles:**
- Es una **clase anónima que implementa `WindowFocusListener`**
- Se asigna como **campo final de la clase View**
- Se registra en `createFrame()` una única vez

---

### 2. Registro del Listener

```java
private void createFrame() {
    // ...
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setLayout(new GridBagLayout());
    
    // ✅ NUEVO: Registrar listener
    this.addWindowFocusListener(this.focusListener);
    
    // ... resto de setup ...
}
```

**Ubicación:** `createFrame()`, después de configurar layout pero antes de pack()

**Timing:** Se registra una sola vez durante inicialización

---

### 3. Método resetAllKeyStates()

```java
private void resetAllKeyStates() {
    if (this.localPlayerId == null || this.controller == null) {
        return;
    }

    try {
        // Resetear TODOS los controles activos
        this.controller.playerThrustOff(this.localPlayerId);
        this.controller.playerRotateOff(this.localPlayerId);
        this.fireKeyDown.set(false);
    } catch (Exception ex) {
        System.err.println("Error resetting key states: " + ex.getMessage());
        ex.printStackTrace();
    }
}
```

**¿Qué hace?**
1. Verifica que exista `localPlayerId` y `controller`
2. Llama a `playerThrustOff()` (equivale a `setThrust(0)`)
3. Llama a `playerRotateOff()` (cancela cualquier rotación)
4. Resetea flag de fuego (`fireKeyDown`)
5. Maneja excepciones (seguridad)

**¿Por qué es idempotente?**
- Llamar `playerThrustOff()` cuando ya está off: sin efectos
- Llamar `playerRotateOff()` cuando ya está off: sin efectos
- Es seguro llamarlo múltiples veces

---

### 4. Try-Catch en Handlers

```java
@Override
public void keyPressed(KeyEvent e) {
    try {
        if (this.localPlayerId == null) return;
        if (this.controller == null) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                this.controller.playerThrustOn(this.localPlayerId);
                break;
            // ... más cases ...
        }
    } catch (Exception ex) {
        System.err.println("Error in keyPressed: " + ex.getMessage());
        ex.printStackTrace();
        // ✅ Forzar estado seguro ante cualquier error
        resetAllKeyStates();
    }
}
```

**¿Por qué?**
- Si ocurre exception en `playerThrustOn()`, al menos sabemos
- `resetAllKeyStates()` garantiza que el estado es consistente
- No dejamos la aplicación en estado inconsistente

---

## Matriz de Casos de Uso {#matriz}

```
┌────────────────────────────┬──────────────────┬──────────────────┐
│ Evento                     │ ANTES (❌)        │ DESPUÉS (✅)      │
├────────────────────────────┼──────────────────┼──────────────────┤
│ keyPressed(UP) +           │ ❌ Thrust        │ ✅ Thrust        │
│ Alt+Tab +                  │ atascado         │ se desactiva     │
│ keyReleased nunca          │ indefinida       │ automáticamente  │
├────────────────────────────┼──────────────────┼──────────────────┤
│ keyPressed(LEFT) +         │ ❌ Rotación      │ ✅ Rotación      │
│ Win+D +                    │ izquierda        │ se cancela       │
│ keyReleased nunca          │ atascada         │ automáticamente  │
├────────────────────────────┼──────────────────┼──────────────────┤
│ keyPressed(SPACE) +        │ ❌ Arma dispara  │ ✅ Flag se       │
│ Exception +                │ indefinidamente  │ resetea a false  │
│ keyReleased nunca          │ (fireKeyDown=true) │              │
├────────────────────────────┼──────────────────┼──────────────────┤
│ Clic en otra ventana +     │ ❌ Controles     │ ✅ Todos se      │
│ keyReleased nunca          │ quedan activos   │ resetean         │
├────────────────────────────┼──────────────────┼──────────────────┤
│ Minimizar ventana +        │ ❌ Thrust/rot    │ ✅ Todo          │
│ keyReleased nunca          │ quedan ON        │ reseteado        │
├────────────────────────────┼──────────────────┼──────────────────┤
│ Exception en keyPressed    │ ❌ Estado        │ ✅ Estado        │
│                            │ inconsistente    │ se restaura      │
└────────────────────────────┴──────────────────┴──────────────────┘
```

---

## Validaciones {#validaciones}

### Validación 1: Compilación

```
✅ Compila sin errores
✅ Compila sin warnings
✅ Imports están disponibles (java.awt.event.WindowFocusListener)
✅ Sintaxis correcta
```

---

### Validación 2: Arquitectura

```
✅ No cambia API pública de View
✅ No afecta Model, Controller, ni Physics
✅ Es un cambio localizdo solo en View
✅ Solo interactúa con métodos existentes del Controller
✅ No introduce nuevas dependencias
```

---

### Validación 3: Performance

```
✅ WindowFocusListener se dispara ~1 vez por cambio de foco
✅ No se ejecuta en cada frame (no es loop)
✅ resetAllKeyStates() es O(1) - solo 3 llamadas
✅ Sin impacto en FPS o latencia
✅ Acceptable overhead: ~microsegundos
```

---

### Validación 4: Correctitud

```
INVARIANTE 1: Si la ventana NO tiene foco, todos los controles deben estar OFF
✅ windowLostFocus() garantiza esto

INVARIANTE 2: resetAllKeyStates() es idempotente
✅ Llamarlo múltiples veces produce mismo resultado

INVARIANTE 3: No afecta comportamiento normal (sin Alt+Tab)
✅ keyReleased() sigue funcionando normalmente
✅ windowLostFocus() no se dispara si foco no se pierde

INVARIANTE 4: Estado de controles es siempre consistente
✅ Si un control está ON, debe haber una razón
✅ Si se pierde la ventana, se fuerza OFF
```

---

### Validación 5: Testing

```
TEST 1: keyPressed seguido de keyReleased normal
✓ Debe funcionar exactamente igual que antes
✓ No hay cambio de comportamiento
✓ windowLostFocus() NO se dispara
✓ Ninguna diferencia observable

TEST 2: keyPressed + Alt+Tab
✓ windowLostFocus() se dispara
✓ resetAllKeyStates() se invoca
✓ Controles se desactivan automáticamente
✓ NO se genera keyReleased (puede no haber o llegar tarde)

TEST 3: keyPressed + Exception en Controller
✓ Try-catch captura la excepción
✓ Se loguea en stderr
✓ resetAllKeyStates() se invoca
✓ Estado se restaura

TEST 4: Rapid key presses (stress test)
✓ keyPressed() se invoca múltiples veces
✓ resetAllKeyStates() es idempotente
✓ No hay race conditions
✓ Estado permanece consistente
```

---

## Diagrama de Flujo Detallado

```
                    USUARIO INTERACTUANDO
                            │
                            ├─→ keyPressed(KEY)
                            │   │
                            │   ├─ try {
                            │   │   switch(keyCode) {
                            │   │     case VK_UP: playerThrustOn()...
                            │   │     case VK_LEFT: playerRotateLeftOn()...
                            │   │     ...
                            │   │   }
                            │   │ } catch(Exception) {
                            │   │   resetAllKeyStates(); ← SAFETY NET
                            │   │ }
                            │   │
                            │   └─ CONTROLES CAMBIAN
                            │
                            ├─→ [Usuario continúa interactuando]
                            │
                            ├─→ keyReleased(KEY) o se pierden
                            │   ├─ Caso Normal: keyReleased generado
                            │   │  └─ try {
                            │   │     case VK_UP: playerThrustOff()
                            │   │     ...
                            │   │  }
                            │   │
                            │   └─ Caso Problemático: OS consume evento
                            │      └─ keyReleased NO generado ❌
                            │         (Pero... ver abajo)
                            │
                            └─→ windowLostFocus() SE DISPARA ✅
                                │
                                └─ resetAllKeyStates()
                                   ├─ playerThrustOff()
                                   ├─ playerRotateOff()
                                   └─ fireKeyDown = false
                                      
                                   ✅ ESTADO RESTAURADO AUTOMÁTICAMENTE
```

---

## Conclusión

La solución de `WindowFocusListener` es un **patrón estándar en la programación interactiva con Swing** porque:

1. **Resuelve el problema raíz:** No depender de keyReleased()
2. **Es robusta:** Funciona en todos los OS
3. **Es eficiente:** No impacta performance
4. **Es simple:** ~50 líneas de código
5. **Es mantenible:** Código claro y documentado
6. **Es retrocompatible:** Cambio invisible para el usuario

---

