# ThreadPoolManager - Guía de Integración

Esta guía muestra cómo integrar las nuevas capacidades del `ThreadPoolManager` mejorado en tu aplicación MVCGameEngine.

---

## 🚀 Inicio Rápido

### Opción 1: Uso Básico (Sin Cambios)

El código existente sigue funcionando exactamente igual:

```java
// Main.java
ThreadPoolManager.configure(maxBodies);
ThreadPoolManager.prestartAllCoreThreads();

// DynamicBody.java
ThreadPoolManager.submit(this);
```

**El shutdown hook automático se encarga de la limpieza al cerrar la aplicación.**

---

### Opción 2: Uso Avanzado con Monitoreo

Para aplicaciones en producción o debugging:

```java
// Main.java
public class Main {
    public static void main(String[] args) {
        // 1. Configurar pool
        int maxBodies = 3500;
        ThreadPoolManager.configure(maxBodies);
        ThreadPoolManager.prestartAllCoreThreads();
        
        // 2. Opcional: Activar monitoreo
        ThreadPoolMonitor monitor = new ThreadPoolMonitor();
        monitor.setQueueSizeWarningThreshold(200);
        monitor.start(30000); // Check cada 30 segundos
        
        // 3. Iniciar engine
        Controller controller = new Controller(...);
        controller.activate();
        
        // ... resto de la aplicación ...
        
        // 4. Opcional: Shutdown explícito
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            monitor.stop();
            controller.stop(); // Si existe
            
            boolean success = ThreadPoolManager.shutdown(60, TimeUnit.SECONDS);
            if (success) {
                System.out.println("Clean shutdown completed");
            } else {
                System.err.println("Forced shutdown after timeout");
            }
        }));
    }
}
```

---

## 📊 Integración con Controller (Opcional)

Si quieres más control sobre el ciclo de vida, puedes añadir métodos al `Controller`:

```java
// Controller.java
public class Controller implements WorldManager, DomainEventProcessor {
    
    private volatile EngineState state = EngineState.STARTING;
    
    // ... existing code ...
    
    /**
     * Stop the engine gracefully
     */
    public void stop() {
        if (state == EngineState.STOPPED) {
            return;
        }
        
        System.out.println("[Controller] Stopping engine...");
        state = EngineState.STOPPED;
        
        // 1. Signal model to stop accepting new entities
        model.setState(ModelState.STOPPED);
        
        // 2. Wait a bit for current physics loops to detect the state change
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 3. Shutdown thread pool gracefully
        boolean success = ThreadPoolManager.shutdown(30, TimeUnit.SECONDS);
        if (!success) {
            System.err.println("[Controller] Thread pool shutdown timeout - forcing");
        }
        
        // 4. Stop renderer
        if (view != null) {
            view.stopRenderer();
        }
        
        System.out.println("[Controller] Engine stopped");
    }
    
    /**
     * Get thread pool statistics for HUD display
     */
    public Object[] getThreadPoolStats() {
        return new Object[] {
            ThreadPoolManager.getActiveThreadCount(),
            ThreadPoolManager.getQueueSize(),
            ThreadPoolManager.getSubmittedTaskCount(),
            ThreadPoolManager.getCompletedTaskCount()
        };
    }
}
```

Luego en `Main.java`:

```java
Controller controller = new Controller(...);
controller.activate();

// ... run application ...

// Al cerrar (por ejemplo, con ESC key o window close)
controller.stop();
```

---

## 🎮 Integración con View para Shutdown

Puedes añadir una tecla para cerrar la aplicación limpiamente:

```java
// View.java - en keyPressed()
case KeyEvent.VK_ESCAPE:
    System.out.println("[View] ESC pressed - initiating shutdown");
    
    // Opción 1: Llamar a controller.stop()
    if (this.controller instanceof Controller) {
        ((Controller) this.controller).stop();
    }
    
    // Opción 2: Shutdown directo
    ThreadPoolManager.printStatistics();
    ThreadPoolManager.shutdown(30, TimeUnit.SECONDS);
    
    // Cerrar ventana
    System.exit(0);
    break;
```

---

## 📈 Mostrar Statistics en HUD

Integra las métricas del pool en tu HUD existente:

```java
// ControlPanel.java
public void updateThreadPoolStats() {
    int activeThreads = ThreadPoolManager.getActiveThreadCount();
    int queueSize = ThreadPoolManager.getQueueSize();
    long submitted = ThreadPoolManager.getSubmittedTaskCount();
    long completed = ThreadPoolManager.getCompletedTaskCount();
    
    // Añadir a tu HUD display
    this.threadPoolLabel.setText(String.format(
        "Pool: %d active | %d queued | %d/%d tasks",
        activeThreads, queueSize, completed, submitted
    ));
    
    // Advertencia visual si queue crece demasiado
    if (queueSize > 200) {
        this.threadPoolLabel.setBackground(Color.YELLOW);
    } else if (queueSize > 500) {
        this.threadPoolLabel.setBackground(Color.RED);
    } else {
        this.threadPoolLabel.setBackground(Color.GREEN);
    }
}
```

Llama a `updateThreadPoolStats()` en tu método de actualización periódica del HUD.

---

## 🧪 Testing del Ciclo de Vida

Prueba que el shutdown funciona correctamente:

### Test 1: Shutdown Normal (Ventana)
```bash
1. Iniciar aplicación
2. Esperar a que aparezcan entities
3. Cerrar ventana con X
4. Verificar en console:
   ✅ "[ThreadPoolManager] JVM shutdown detected - cleaning up"
   ✅ "[ThreadPoolManager] Shutdown now completed"
   ✅ Estadísticas impresas
   ✅ No errores
```

### Test 2: Shutdown con Ctrl+C
```bash
1. Iniciar aplicación desde terminal
2. Presionar Ctrl+C
3. Verificar mismo output que Test 1
```

### Test 3: Shutdown Programático
```java
// En un test o en un timer
new Timer().schedule(new TimerTask() {
    @Override
    public void run() {
        System.out.println("Test: Initiating shutdown after 10 seconds");
        boolean success = ThreadPoolManager.shutdown(30, TimeUnit.SECONDS);
        System.out.println("Shutdown result: " + success);
        System.exit(0);
    }
}, 10000);
```

### Test 4: Comportamiento bajo Carga
```java
// Crear muchos bodies rápidamente
for (int i = 0; i < 5000; i++) {
    controller.addDynamicBody(...);
}

// Inmediatamente después, shutdown
ThreadPoolManager.printStatistics();
boolean success = ThreadPoolManager.shutdown(60, TimeUnit.SECONDS);

// Verificar:
// ✅ Todas las tareas completaron o fueron canceladas
// ✅ No quedaron threads zombie
// ✅ No hubo deadlocks
```

---

## 🐛 Debugging con las Nuevas Herramientas

### Problema: Aplicación No Responde

```java
// 1. Verificar estado del pool
ThreadPoolManager.printStatistics();

// Si "Active Threads" está al máximo y "Queue Size" crece:
//   → Bodies están en deadlock o loop infinito
//   → Revisar DynamicBody.run() y StaticBody.run()

// Si "Queue Size" = 0 y "Active Threads" = 0:
//   → Pool está idle, problema en otra parte
```

### Problema: Memory Leak

```java
// Monitorear tareas pendientes
long submitted = ThreadPoolManager.getSubmittedTaskCount();
long completed = ThreadPoolManager.getCompletedTaskCount();
long pending = submitted - completed;

System.out.println("Pending tasks: " + pending);

// Si pending crece continuamente:
//   → Bodies no están terminando (BodyState no llega a DEAD)
//   → Revisar lógica de lifecycle en AbstractBody
```

### Problema: Crashes en Physics

Con el nuevo UncaughtExceptionHandler, ahora verás el stacktrace completo:

```
[ThreadPoolManager] Uncaught exception in thread BodyThread-1707847234567: 
    NullPointerException: phyEngine is null
    at DynamicBody.run(DynamicBody.java:175)
    at ThreadPoolExecutor.runWorker(...)
    at Thread.run(...)
```

Esto hace debugging mucho más fácil.

---

## 💡 Best Practices

### 1. Configure Pool Size Apropiadamente

```java
// ❌ MALO: No configurar (usa default 250)
ThreadPoolManager.submit(this);

// ✅ BIEN: Configurar basado en maxBodies
ThreadPoolManager.configure(maxBodies);

// ✅ MEJOR: Considerar capacidad del sistema
int cores = Runtime.getRuntime().availableProcessors();
int optimalSize = Math.min(maxBodies, cores * 10);
ThreadPoolManager.configure(optimalSize);
```

### 2. Prestart Threads para Mejor Rendimiento

```java
// ✅ Hacer esto ANTES de crear bodies
ThreadPoolManager.configure(maxBodies);
ThreadPoolManager.prestartAllCoreThreads();

// Luego crear bodies
controller.addDynamicBody(...);
```

### 3. Monitor en Producción

```java
// Para aplicaciones de larga duración
ThreadPoolMonitor monitor = new ThreadPoolMonitor();
monitor.setQueueSizeWarningThreshold(maxBodies / 2);
monitor.start(60000); // Check cada minuto
```

### 4. Graceful Shutdown

```java
// ❌ MALO: Salir abruptamente
System.exit(0);

// ✅ BIEN: Shutdown graceful
boolean success = ThreadPoolManager.shutdown(30, TimeUnit.SECONDS);
if (success) {
    System.exit(0);
} else {
    System.exit(1);
}

// ✅ MEJOR: Multi-stage shutdown
controller.stop();  // Señalar a todos que paren
Thread.sleep(500);  // Dar tiempo a que reaccionen
ThreadPoolManager.shutdown(30, TimeUnit.SECONDS);
System.exit(0);
```

---

## 📝 Ejemplo Completo: Main.java Mejorado

```java
import engine.utils.threading.ThreadPoolManager;
import engine.utils.threading.ThreadPoolMonitor;
import java.util.concurrent.TimeUnit;

public class Main {
    
    private static ThreadPoolMonitor monitor;
    
    public static void main(String[] args) {
        System.out.println("=== MVCGameEngine Starting ===");
        
        // 1. Configurar ThreadPoolManager
        int maxBodies = 3500;
        ThreadPoolManager.configure(maxBodies);
        ThreadPoolManager.prestartAllCoreThreads();
        
        // 2. Opcional: Activar monitoreo (útil en desarrollo)
        boolean enableMonitoring = Boolean.getBoolean("threadpool.monitor");
        if (enableMonitoring) {
            monitor = new ThreadPoolMonitor();
            monitor.setQueueSizeWarningThreshold(maxBodies / 4);
            monitor.setQueueSizeCriticalThreshold(maxBodies / 2);
            monitor.start(30000);
            System.out.println("[Main] ThreadPool monitoring enabled");
        }
        
        // 3. Configurar graphics
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.d3d", "false");
        
        // 4. Inicializar engine
        DoubleVector worldDimension = new DoubleVector(40000, 40000);
        DoubleVector viewDimension = new DoubleVector(2400, 1500);
        int maxAsteroidCreationDelay = 5;
        
        ProjectAssets projectAssets = new ProjectAssets();
        ActionsGenerator gameRules = new gamerules.LimitRebound();
        WorldDefinitionProvider worldProv = 
            new gameworld.RandomWorldDefinitionProvider(worldDimension, projectAssets);
        
        Controller controller = new Controller(
            worldDimension, viewDimension, maxBodies,
            new View(), new Model(worldDimension, maxBodies),
            gameRules);
        
        controller.activate();
        
        // 5. Crear world
        WorldDefinition worldDef = worldProv.provide();
        new gamelevel.LevelBasic(controller, worldDef);
        new gameai.AIBasicSpawner(controller, worldDef, maxAsteroidCreationDelay).activate();
        
        // 6. Registrar shutdown hook
        registerShutdownHook();
        
        System.out.println("=== MVCGameEngine Running ===");
        System.out.println("Press Ctrl+C or close window to shutdown");
    }
    
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Initiating Graceful Shutdown ===");
            
            // 1. Stop monitoring
            if (monitor != null && monitor.isRunning()) {
                monitor.stop();
            }
            
            // 2. Print final statistics
            System.out.println("\nFinal Thread Pool Statistics:");
            ThreadPoolManager.printStatistics();
            
            // 3. Graceful shutdown
            System.out.println("\nShutting down thread pool...");
            boolean success = ThreadPoolManager.shutdown(30, TimeUnit.SECONDS);
            
            if (success) {
                System.out.println("✅ Clean shutdown completed");
            } else {
                System.err.println("⚠️ Forced shutdown after timeout");
            }
            
            System.out.println("=== Shutdown Complete ===");
        }, "Main-ShutdownHook"));
    }
}
```

**Para ejecutar con monitoreo:**
```bash
java -Dthreadpool.monitor=true -cp target/classes Main
```

---

## ⚙️ Configuración Avanzada

### Variables de Sistema
```bash
# Activar monitoreo
-Dthreadpool.monitor=true

# Ajustar timeout de shutdown (segundos)
-Dthreadpool.shutdown.timeout=60

# Verbose logging
-Dthreadpool.verbose=true
```

### Integración con JMX (Futuro)
El ThreadPoolManager podría exponer métricas vía JMX para monitoreo externo:
```java
// Posible extensión futura
ThreadPoolManagerMBean poolMBean = new ThreadPoolManagerMBean();
MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
ObjectName name = new ObjectName("engine.threading:type=ThreadPoolManager");
mbs.registerMBean(poolMBean, name);
```

---

## ✅ Checklist de Integración

- [ ] Configurar pool size apropiado en Main.java
- [ ] Prestart threads antes de crear bodies
- [ ] Opcional: Activar ThreadPoolMonitor para debugging
- [ ] Opcional: Añadir método stop() al Controller
- [ ] Opcional: Mostrar stats en HUD
- [ ] Probar shutdown con ventana X
- [ ] Probar shutdown con Ctrl+C
- [ ] Verificar no hay threads zombie
- [ ] Verificar logs de UncaughtExceptionHandler
- [ ] Documentar configuración en README

---

## 🎓 Conclusión

Las mejoras al ThreadPoolManager son **100% compatibles** con el código existente. Puedes:

1. **No hacer nada** - El shutdown hook automático se encarga de todo
2. **Añadir monitoreo** - Usa ThreadPoolMonitor para debugging
3. **Control total** - Implementa shutdown explícito en Controller

Elige el nivel de integración que mejor se adapte a tus necesidades.
