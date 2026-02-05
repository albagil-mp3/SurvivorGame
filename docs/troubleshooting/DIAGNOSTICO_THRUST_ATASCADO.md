# 🔴 DIAGNÓSTICO: Thrust Atascado - Análisis de Eventos de Teclado

**Fecha:** 2026-02-05  
**Severidad:** CRÍTICA  
**Estado:** En investigación  

---

## Resumen del Problema

El thrust se queda activado ("on") ocasionalmente, como si se perdiera el evento `keyReleased()` correspondiente. Esto causa que la nave continúe acelerando indefinidamente hasta que se presiona nuevamente la tecla de thrust.

---

## 🔍 Análisis Técnico

### Arquitectura de Control de Teclado

```
┌─────────────────────────────────────────────────────────┐
│ Swing KeyListener (EDT - Event Dispatch Thread)         │
│                                                         │
│  keyPressed(KeyEvent e)  ──► controller.playerThrustOn()│
│                                                         │
│  keyReleased(KeyEvent e) ──► controller.playerThrustOff()│
└──────────────┬──────────────────────────────────────────┘
               │
               ▼
        ┌─────────────────────────┐
        │ Controller (bridge)      │
        │                         │
        │ playerThrustOn()  ─────►│ model.playerThrustOn()
        │ playerThrustOff() ─────►│ model.playerThrustOff()
        └──────────────┬──────────┘
                       │
                       ▼
        ┌──────────────────────────┐
        │ Model                    │
        │                          │
        │ playerThrustOn()  ─────► │ pBody.thrustMaxOn()
        │ playerThrustOff() ─────► │ pBody.thrustOff()
        └──────────────┬───────────┘
                       │
                       ▼
        ┌────────────────────────────┐
        │ PlayerBody → DynamicBody   │
        │                            │
        │ thrustMaxOn()      ─────┐  │
        │ ├─ thurstNow(800)  ─────┼──► setThrust(thrust)
        │ │                        │
        │ thrustOff()        ─────┼───► stopPushing()
        │ └─ stopPushing()   ─────┘     └─ setThrust(0.0)
        └────────────────────────────┘
```

---

## 🐛 Problemas Identificados

### **PROBLEMA 1: Missing KeyRelease Handler para rotación derecha**

**Ubicación:** `View.keyReleased()` línea 366

**Código actual:**
```java
@Override
public void keyReleased(KeyEvent e) {
    switch (e.getKeyCode()) {
        case KeyEvent.VK_UP:
        case KeyEvent.VK_W:
            this.controller.playerThrustOff(this.localPlayerId);
            break;

        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_X:
            this.controller.playerThrustOff(this.localPlayerId);
            break;

        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_A:
            this.controller.playerRotateOff(this.localPlayerId);
            break;

        case KeyEvent.VK_RIGHT:  // ✅ CORRECTO
        case KeyEvent.VK_D:
            this.controller.playerRotateOff(this.localPlayerId);
            break;

        case KeyEvent.VK_SPACE:
            this.fireKeyDown.set(false);
            break;
    }
}
```

**Verificación:** En `keyPressed()` SÍ existe VK_RIGHT/VK_D con `playerRotateRightOn()` ✅  
Pero `keyReleased()` SÍ tiene el case ✅  
*(Este problema NO existe en la versión actual)*

---

### **PROBLEMA 2: CRÍTICO - Pérdida de eventos keyReleased() del SO** ⚠️

**Causas raíz:**

1. **Combinaciones con teclas del Sistema Operativo (Alt/Win)**
   - Cuando usuario presiona: **Alt+Tab**, **Win+X**, **Alt+F4**
   - El SO consume el evento y lo redirige a otro proceso
   - `keyReleased()` **NUNCA se genera** en la aplicación
   - El thrust queda activado indefinidamente

2. **Pérdida de foco de la ventana**
   - Si Swing pierde foco del Canvas durante keyPress
   - `keyReleased()` no es entregado
   - Estado de teclas queda inconsistente

3. **Eventos de Sistema (Alt+Numpad, etc)**
   - Algunos eventos de teclado son interceptados por el SO
   - La aplicación recibe `keyPressed()` pero NO `keyReleased()`

---

### **PROBLEMA 3: Sin manejo de exceptions en handlers de teclado**

**Ubicación:** `View.keyPressed()` y `View.keyReleased()`

**Riesgo:** Si ocurre una excepción durante `keyPressed()`, el estado puede quedar inconsistente:
- El thrust se activó pero no se desactiva
- Las rotaciones quedan inconsistentes

**Escenario:**
```java
@Override
public void keyPressed(KeyEvent e) {
    // Si aquí falla algo...
    this.controller.playerThrustOn(this.localPlayerId); // ✓ Ejecutado
    
    if (someError) {
        throw new RuntimeException("Error!");
        // El keyReleased() nunca compensa esto
    }
}
```

---

### **PROBLEMA 4: Sin validación de estado en Model.playerThrustOff()**

**Ubicación:** `Model.playerThrustOff()` línea 483

```java
public void playerThrustOff(String playerId) {
    PlayerBody pBody = (PlayerBody) this.dynamicBodies.get(playerId);
    if (pBody != null) {
        pBody.thrustOff();  // ✓ Llama stopPushing() correctamente
    }
}
```

**El problema es que si `playerThrustOff()` NO se invoca, el thrust permanece activado**

---

## 📊 Flujo de Ejecución Problemático

```
ESCENARIO: Usuario presiona UP, luego presiona Alt+Tab

1. ✓ keyPressed(VK_UP)
   → controller.playerThrustOn()
   → model.playerThrustOn()
   → pBody.thrustMaxOn()
   → setThrust(800)          ← THRUST ACTIVADO

2. ⚠️ USER PRESSES Alt+Tab (SO intercepts)
   → WindowFocus LOST
   → keyReleased(VK_UP) NUNCA se genera  ❌❌❌

3. ✓ Usuario vuelve a la ventana (hace clic)
   → El Canvas recupera foco
   → PERO... thrust = 800 aún activo ⚠️⚠️⚠️

4. ✗ Usuario presiona UP nuevamente
   → keyPressed(VK_UP) se invoca
   → controller.playerThrustOn()  (sin cambios, ya estaba ON)
   → Sigue acelerando

5. ✓ Usuario libera UP
   → keyReleased(VK_UP)
   → controller.playerThrustOff()
   → FINALMENTE setThrust(0) ← FINALMENTE OFF
```

---

## 🔧 Soluciones Propuestas

### **Solución 1: Interceptar pérdida de foco (RECOMENDADA)**

Detectar cuando la ventana pierde foco y **resetear TODOS los controles**:

```java
// En View.java

private void createFrame() {
    // ...
    this.addWindowFocusListener(new WindowFocusListener() {
        @Override
        public void windowLostFocus(WindowEvent e) {
            // Resetear todos los controles activos
            resetAllKeyStates();
        }
        
        @Override
        public void windowGainedFocus(WindowEvent e) {
            // Opcional: notificar al usuario
        }
    });
}

private void resetAllKeyStates() {
    if (this.localPlayerId == null) return;
    
    // Forzar desactivación de todos los controles
    this.controller.playerThrustOff(this.localPlayerId);
    this.controller.playerRotateOff(this.localPlayerId);
    this.fireKeyDown.set(false);
}
```

### **Solución 2: Mantener mapa de estado de teclas (PREVENTIVA)**

```java
// En View.java

private final Map<Integer, Boolean> keyStates = new ConcurrentHashMap<>();
private static final int[] TRACKED_KEYS = {
    KeyEvent.VK_UP, KeyEvent.VK_W,
    KeyEvent.VK_DOWN, KeyEvent.VK_X,
    KeyEvent.VK_LEFT, KeyEvent.VK_A,
    KeyEvent.VK_RIGHT, KeyEvent.VK_D,
};

@Override
public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    keyStates.put(keyCode, true);
    
    // Procesar comando...
}

@Override
public void keyReleased(KeyEvent e) {
    int keyCode = e.getKeyCode();
    keyStates.put(keyCode, false);
    
    // Procesar comando...
}

// Método de sincronización periódica (p.ej., en render loop)
public void syncKeyStates() {
    // Si Windows/Alt está presionado, resetear todo
    if (Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_ALT)) {
        resetAllKeyStates();
        keyStates.clear();
    }
}
```

### **Solución 3: Manejo de excepciones en handlers**

```java
@Override
public void keyPressed(KeyEvent e) {
    try {
        if (this.localPlayerId == null || this.controller == null) {
            return;
        }
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                this.controller.playerThrustOn(this.localPlayerId);
                break;
            // ...
        }
    } catch (Exception ex) {
        System.err.println("Error in keyPressed: " + ex.getMessage());
        ex.printStackTrace();
        // Forzar estado seguro
        resetAllKeyStates();
    }
}
```

---

## ✅ Recomendaciones de Implementación

| Prioridad | Solución | Impacto | Esfuerzo |
|-----------|----------|--------|---------|
| **CRÍTICA** | Detectar WindowFocusListener | Alto | Bajo |
| **ALTA** | Mantener mapa de teclas | Alto | Medio |
| **MEDIA** | Try-catch en handlers | Medio | Bajo |
| **BAJA** | Logging detallado | Bajo | Bajo |

---

## 📝 Pasos Siguientes

1. ✅ Implementar `WindowFocusListener` en `View.java`
2. ✅ Crear método `resetAllKeyStates()`
3. ✅ Agregar try-catch en key handlers
4. ✅ Probar con Alt+Tab, Alt+numpad, Win+X
5. ✅ Verificar que no haya race conditions con threads

---

## 🔗 Archivos Relacionados

- `src/engine/view/core/View.java` (lines 323-410)
- `src/engine/model/bodies/impl/PlayerBody.java`
- `src/engine/model/bodies/impl/DynamicBody.java` (thrust control)
- `src/engine/controller/impl/Controller.java` (delegation)

---

## 📌 Estado

- **Investigación:** ✅ Completada
- **Problemas encontrados:** 4
- **Soluciones propuestas:** 3
- **Implementación:** ⏳ Pendiente

