# ThreadPoolManager - Refactorización a Arquitectura Instanciable

**Fecha:** 7 de Febrero de 2026  
**Cambio:** De Singleton Estático → Instancia Gestionada por Model  
**Impacto:** Mejor encapsulación, arquitectura más limpia  

---

## 🎯 Motivación del Cambio

### Antes (Problemático)
```java
// Main.java - Detalles de implementación expuestos
ThreadPoolManager.configure(maxBodies);
ThreadPoolManager.prestartAllCoreThreads();

// DynamicBody.java
ThreadPoolManager.submit(this); // Dependencia global estática
```

**Problemas:**
- ❌ Detalles de implementación del dominio expuestos en `Main`
- ❌ Singleton global dificulta testing
- ❌ Acoplamiento estático entre capas
- ❌ Viola principio de inyección de dependencias

### Después (Mejorado)
```java
// Main.java - Limpio, sin detalles de implementación
DoubleVector worldDimension = new DoubleVector(40000, 40000);
Controller controller = new Controller(...);

// Model.java - Encapsula la gestión del pool
private final ThreadPoolManager threadPoolManager;

public Model(DoubleVector worldDimension, int maxDynamicBodies) {
    this.threadPoolManager = new ThreadPoolManager(maxDynamicBodies);
    // ...
}

// DynamicBody.java - Usa dependencia inyectada
this.getThreadPoolManager().submit(this);
```

**Beneficios:**
- ✅ Detalles de implementación encapsulados en el dominio
- ✅ Inyección de dependencias explícita
- ✅ Testeable (se puede inyectar mock)
- ✅ Arquitectura más limpia y mantenible

---

## 📊 Cambios Realizados

### 1. ThreadPoolManager - De Singleton a Instanciable

**Antes:**
```java
public final class ThreadPoolManager {
    private static ThreadPoolManager instance;
    private static int configuredPoolSize = -1;
    
    private ThreadPoolManager(int poolSize) { ... }
    
    public static void configure(int poolSize) { ... }
    public static void submit(Runnable task) { ... }
    // ... todos los métodos estáticos
}
```

**Después:**
```java
public final class ThreadPoolManager {
    private final ThreadPoolExecutor executor;
    private final int poolSize;
    
    public ThreadPoolManager() { ... }
    public ThreadPoolManager(int poolSize) { ... }
    
    public void submit(Runnable task) { ... }
    public void prestartAllCoreThreads() { ... }
    public void shutdown() { ... }
    // ... todos los métodos de instancia
}
```

---

### 2. Model - Gestiona el ThreadPoolManager

**Cambios en `Model.java`:**

```java
import engine.utils.threading.ThreadPoolManager;

public class Model implements BodyEventProcessor {
    
    // Añadido como campo
    private final ThreadPoolManager threadPoolManager;
    
    // Creado en constructor
    public Model() {
        this.maxBodies = DEFAULT_MAX_BODIES;
        this.threadPoolManager = new ThreadPoolManager(this.maxBodies);
        // ...
    }
    
    // Prestarteo en activate()
    public void activate() {
        // ...
        this.threadPoolManager.prestartAllCoreThreads();
        this.state = ModelState.ALIVE;
    }
    
    // Pasado a BodyFactory
    AbstractBody body = BodyFactory.create(
        this, this.spatialGrid, dto1, dto2, dto3, 
        bodyType, maxLifeTime, shooterId, 
        this.bodyProfiler, this.threadPoolManager);  // ← Nuevo parámetro
}
```

---

### 3. AbstractBody - Recibe y Almacena la Referencia

**Cambios en `AbstractBody.java`:**

```java
public abstract class AbstractBody {
    
    // Añadido como campo
    private final ThreadPoolManager threadPoolManager;
    
    // Añadido parámetro en constructor
    public AbstractBody(BodyEventProcessor bodyEventProcessor, 
                       SpatialGrid spatialGrid,
                       PhysicsEngine phyEngine, 
                       BodyType type,
                       double maxLifeInSeconds, 
                       String emitterId, 
                       ThreadPoolManager threadPoolManager) {  // ← Nuevo
        
        this.bodyEventProcessor = bodyEventProcessor;
        this.phyEngine = phyEngine;
        this.threadPoolManager = threadPoolManager;  // ← Guardado
        // ...
    }
    
    // Getter protegido para subclases
    protected ThreadPoolManager getThreadPoolManager() {
        return this.threadPoolManager;
    }
}
```

---

### 4. BodyFactory - Recibe y Propaga el ThreadPoolManager

**Cambios en `BodyFactory.java`:**

```java
import engine.utils.threading.ThreadPoolManager;

public class BodyFactory {

    public static AbstractBody create(
            BodyEventProcessor bodyEventProcessor,
            SpatialGrid spatialGrid,
            PhysicsValuesDTO dto1,
            PhysicsValuesDTO dto2,
            PhysicsValuesDTO dto3,
            BodyType bodyType,
            double maxLifeTime,
            String emitterId,
            BodyProfiler profiler,
            ThreadPoolManager threadPoolManager) {  // ← Nuevo parámetro

        AbstractBody body = null;
        PhysicsEngine phyEngine = null;

        switch (bodyType) {
            case DYNAMIC:
                phyEngine = new BasicPhysicsEngine(dto1, dto2, dto3, profiler);
                body = new DynamicBody(
                    bodyEventProcessor, spatialGrid, phyEngine,
                    BodyType.DYNAMIC,
                    maxLifeTime, null, profiler, 
                    threadPoolManager);  // ← Propagado
                break;
            
            case PLAYER:
                phyEngine = new BasicPhysicsEngine(dto1, dto2, dto3, profiler);
                body = new PlayerBody(
                    bodyEventProcessor, spatialGrid, phyEngine,
                    maxLifeTime, null, profiler, 
                    threadPoolManager);  // ← Propagado
                break;
            
            // ... etc para todos los tipos
        }

        return body;
    }
}
```

---

### 5. DynamicBody y StaticBody - Usan la Instancia

**Cambios en `DynamicBody.java`:**

```java
public class DynamicBody extends AbstractBody implements Runnable {

    // Constructor actualizado
    public DynamicBody(BodyEventProcessor bodyEventProcessor, 
                      SpatialGrid spatialGrid,
                      PhysicsEngine phyEngine, 
                      BodyType bodyType, 
                      double maxLifeInSeconds, 
                      String emitterId, 
                      BodyProfiler profiler, 
                      ThreadPoolManager threadPoolManager) {  // ← Nuevo

        super(bodyEventProcessor, spatialGrid, phyEngine,
              bodyType, maxLifeInSeconds, emitterId, 
              threadPoolManager);  // ← Propagado
        this.profiler = profiler;
    }

    @Override
    public synchronized void activate() {
        super.activate();
        this.setState(BodyState.ALIVE);
        this.getThreadPoolManager().submit(this);  // ← Usa instancia
    }
}
```

**Cambios en `StaticBody.java`:**

```java
public class StaticBody extends AbstractBody implements Runnable {

    public StaticBody(BodyEventProcessor bodyEventProcessor, 
                     SpatialGrid spatialGrid,
                     PhysicsEngine phyEngine, 
                     BodyType bodyType,
                     double maxLifeInSeconds, 
                     String emitterId, 
                     ThreadPoolManager threadPoolManager) {  // ← Nuevo

        super(bodyEventProcessor, spatialGrid, phyEngine,
              bodyType, maxLifeInSeconds, emitterId, 
              threadPoolManager);  // ← Propagado
    }

    @Override
    public synchronized void activate() {
        super.activate();
        this.setState(BodyState.ALIVE);
        this.getThreadPoolManager().submit(this);  // ← Usa instancia
    }
}
```

---

### 6. PlayerBody - Propaga el Cambio

**Cambios en `PlayerBody.java`:**

```java
import engine.utils.threading.ThreadPoolManager;

public class PlayerBody extends DynamicBody {

    public PlayerBody(BodyEventProcessor bodyEventProcessor,
                     SpatialGrid spatialGrid,
                     PhysicsEngine physicsEngine,
                     double maxLifeInSeconds,
                     String emitterId,
                     BodyProfiler profiler,
                     ThreadPoolManager threadPoolManager) {  // ← Nuevo

        super(bodyEventProcessor, spatialGrid, physicsEngine,
              BodyType.PLAYER, maxLifeInSeconds, emitterId,
              profiler, threadPoolManager);  // ← Propagado
        
        this.setMaxThrustForce(800);
        this.setMaxAngularAcceleration(1000);
        this.setAngularSpeed(30);
    }
}
```

---

### 7. Main - Limpiado

**Cambios en `Main.java`:**

```diff
- import engine.utils.threading.ThreadPoolManager;

  public class Main {
      public static void main(String[] args) {
          // ... configuración gráfica ...
          
          int maxBodies = 3500;
          ProjectAssets projectAssets = new ProjectAssets();
          
-         ThreadPoolManager.configure(maxBodies);
-         ThreadPoolManager.prestartAllCoreThreads();
          
          ActionsGenerator gameRules = new gamerules.LimitRebound();
          // ... resto sin cambios ...
      }
  }
```

---

### 8. ThreadPoolMonitor - Actualizado para Instancias

**Cambios en `ThreadPoolMonitor.java`:**

```java
public class ThreadPoolMonitor {
    
    private final ThreadPoolManager threadPoolManager;  // ← Nuevo campo
    
    // Constructor actualizado
    public ThreadPoolMonitor(ThreadPoolManager threadPoolManager) {
        if (threadPoolManager == null) {
            throw new IllegalArgumentException("ThreadPoolManager cannot be null");
        }
        this.threadPoolManager = threadPoolManager;
    }
    
    // Métodos actualizados para usar la instancia
    private void checkAndReport() {
        int queueSize = threadPoolManager.getQueueSize();  // ← Usa instancia
        int activeThreads = threadPoolManager.getActiveThreadCount();
        // ...
    }
}
```

---

## 🔄 Flujo de Dependencias

```
Main.java
  └─> Controller.java
       └─> Model.java
            ├─> ThreadPoolManager (CREA INSTANCIA)
            │    └─> ThreadPoolExecutor
            │
            └─> BodyFactory.create(...)
                 ├─> DynamicBody(..., threadPoolManager)
                 │    └─> AbstractBody(..., threadPoolManager)
                 │         └─> this.threadPoolManager = threadPoolManager
                 │
                 ├─> PlayerBody(..., threadPoolManager)
                 │    └─> DynamicBody(..., threadPoolManager)
                 │
                 └─> StaticBody(..., threadPoolManager)
                      └─> AbstractBody(..., threadPoolManager)
```

**Cuando un Body se activa:**
```
body.activate()
  └─> this.getThreadPoolManager().submit(this)
       └─> threadPoolManager.submit(this)
            └─> executor.submit(this)
                 └─> thread.start() → body.run()
```

---

## ✅ Beneficios Arquitectónicos

### 1. Encapsulación
- El `ThreadPoolManager` es un detalle de implementación del `Model`
- `Main` no conoce ni debe conocer cómo el `Model` gestiona sus threads
- Respeta el principio de "Tell, Don't Ask"

### 2. Testabilidad
```java
// Antes (difícil de testear)
@Test
public void testDynamicBody() {
    ThreadPoolManager.configure(10);  // Estado global compartido
    DynamicBody body = new DynamicBody(...);
    // Difícil aislar el test
}

// Después (fácil de testear)
@Test
public void testDynamicBody() {
    ThreadPoolManager mockPool = mock(ThreadPoolManager.class);
    DynamicBody body = new DynamicBody(..., mockPool);
    
    body.activate();
    
    verify(mockPool).submit(body);  // ✅ Verifica comportamiento
}
```

### 3. Flexibilidad
```java
// Ahora es posible tener múltiples Models con diferentes pools
Model model1 = new Model(worldDim, 1000);  // Pool de 1000
Model model2 = new Model(worldDim, 500);   // Pool de 500

// Cada Model tiene su propio ThreadPoolManager aislado
```

### 4. Ciclo de Vida Claro
```java
// Model controla el ciclo de vida completo
public class Model {
    public void activate() {
        this.threadPoolManager.prestartAllCoreThreads();
        this.state = ModelState.ALIVE;
    }
    
    public void shutdown() {
        this.state = ModelState.STOPPED;
        this.threadPoolManager.shutdown(30, TimeUnit.SECONDS);
    }
}
```

---

## 🧪 Cómo Probarlo

### 1. Compilar
```bash
mvn clean compile
```

### 2. Ejecutar
```bash
mvn exec:java -Dexec.mainClass="Main"
```

### 3. Verificar Output
Deberías ver:
```
[ThreadPoolManager] Created with 3500 threads
... (resto de la inicialización)
Model: Activated
[ThreadPoolManager] Prestarted 3500/3500 core threads
```

### 4. Ejecutar Demo
```bash
mvn exec:java -Dexec.mainClass="engine.utils.threading.ThreadPoolDemo"
```

---

## 📚 Documentación Actualizada

Se debe actualizar:
- ✅ `THREADPOOL_IMPROVEMENTS.md` - Reflejar arquitectura instanciable
- ✅ `THREADPOOL_INTEGRATION_GUIDE.md` - Actualizar ejemplos de uso
- ✅ `THREADPOOL_EXECUTIVE_SUMMARY.md` - Resumen de cambios arquitectónicos

---

## 🔍 Compatibilidad

**Breaking Changes:** ⚠️ Sí (pero solo internamente)

El API público del `Model` **NO cambia**. Los usuarios del `Model` (Controller, Main) **NO necesitan cambios**. 

Los cambios son internos a la arquitectura del dominio:
- `Model` gestiona su `ThreadPoolManager`
- `BodyFactory` recibe parámetro adicional (llamado desde `Model`)
- Constructores de `Body` tienen parámetro adicional (llamados desde `BodyFactory`)

**Para el usuario final:** ✅ No hay cambios visibles

---

## 💡 Lecciones Aprendidas

### 1. Evitar Singletons Globales
Los singletons estáticos crean acoplamiento global y dificultan testing. Preferir inyección de dependencias.

### 2. Encapsular Detalles de Implementación
Los detalles de cómo el `Model` gestiona threads no deben ser visibles en `Main`.

### 3. Inyección de Dependencias Explícita
Aunque verboso, hace las dependencias explícitas y facilita testing y mantenimiento.

### 4. Ownership Claro
El `Model` "posee" el `ThreadPoolManager` → responsable de su ciclo de vida.

---

## 🎓 Conclusión

Esta refactorización transforma un diseño basado en Singleton estático a uno basado en inyección de dependencias explícita, mejorando significativamente:

- ✅ **Arquitectura** - Encapsulación clara de responsabilidades
- ✅ **Testabilidad** - Fácil crear tests aislados
- ✅ **Mantenibilidad** - Flujo de dependencias explícito
- ✅ **Flexibilidad** - Posibilidad de múltiples instancias

El cambio es **transparente para el usuario final** pero mejora dramáticamente la calidad interna del código.

---

**Autor:** Refactorización Arquitectónica  
**Versión:** 2.0 (Instanciable)  
**Estado:** ✅ Implementado y Funcional
