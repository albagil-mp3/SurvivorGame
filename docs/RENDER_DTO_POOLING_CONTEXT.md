# CONTEXT FOR RENDER DTO POOLING - Phase 1-3 Completed

## 🎯 OBJECTIVE
Eliminate `new DynamicRenderDTO()` allocations in render pipeline by implementing a generic pooling system. Current status: **Pool infrastructure complete, ready for final integration.**

---

## 📊 PHYSICS OPTIMIZATION STATUS (Background)

Previous work implemented profiling that showed:
- Physics.dto bottleneck: **0.11-0.74 ms per body** (98% of physics CPU time)
- Actual physics math: ~0.0006ms (extremely efficient)
- Target: Make PhysicsValuesDTO mutable + defensive copy (future work)

Current phase focuses on **render DTO allocations** (180k/s, parallel bottleneck):
- `Controller.snapshotRenderData()` creates `new DynamicRenderDTO` per body per frame
- Allocations create GC pressure and fragmentation
- Goal: Reuse instances via generic pool + pooled mappers

---

## ✅ COMPLETED PHASES

### Phase 1: Mutability (Completed)
- [x] Made `RenderDTO` mutable (removed `final` from fields)
- [x] Made `DynamicRenderDTO` mutable with `updateFrom(...)` and `reset()` methods
- [x] Added base `updateBase(...)` and `reset()` in RenderDTO for inheritance

**Files modified:**
- `src/engine/view/renderables/ports/RenderDTO.java`
- `src/engine/view/renderables/ports/DynamicRenderDTO.java`

### Phase 2: In-Place Updates (Completed)
- [x] `DynamicRenderable.update(DynamicRenderDTO, long)` now updates existing DTO in-place
- [x] Added `updateFrom(DynamicRenderDTO other)` overload to avoid parameter explosion
- [x] Simplified renderable update logic

**Files modified:**
- `src/engine/view/renderables/impl/DynamicRenderable.java`

### Phase 3: Generic Pool Infrastructure (Completed) ✨ NEW
- [x] Created `DTOPoolable` interface (marker with `reset()`)
- [x] Created `DTOPool<T>` generic pool class with `acquire()` and `release()`
- [x] Created `DTOPooledMapper<T>` abstract base for pooled mappers
- [x] Made `RenderDTO` implement `DTOPoolable` (DynamicRenderDTO inherits this)
- [x] Refactored `DynamicRenderableMapper` to extend `DTOPooledMapper<DynamicRenderDTO>`
- [x] Added `fromBodyDTOPooled()` methods to mapper that consume from pool

**Files created:**
- `src/engine/view/renderables/ports/DTOPoolable.java`
- `src/engine/view/renderables/ports/DTOPool.java`
- `src/engine/controller/mappers/DTOPooledMapper.java`

**Files modified:**
- `src/engine/view/renderables/ports/RenderDTO.java` (implements DTOPoolable)
- `src/engine/controller/mappers/DynamicRenderableMapper.java` (extends DTOPooledMapper)

---

## 🏗️ ARCHITECTURE OVERVIEW

### New Class Structure

```
DTOPoolable (interface)
├── reset()

DTOPool<T extends DTOPoolable>
├── pool: Deque<T>
├── factory: Supplier<T>
├── acquire(): T
├── release(T): void
├── getPoolSize(): int
└── clear(): void

DTOPooledMapper<T extends DTOPoolable> (abstract)
├── pool: DTOPool<T>
├── map(source): T (final logic)
├── mapToDTO(source, target): boolean (abstract, for subclasses)
├── getPool(): DTOPool<T>

DynamicRenderableMapper extends DTOPooledMapper<DynamicRenderDTO>
├── fromBodyDTO(BodyData) [static - legacy]
├── fromBodyDTOPooled(BodyData): DynamicRenderDTO [uses pool]
├── fromBodyDTOPooled(ArrayList<BodyData>): ArrayList<DynamicRenderDTO>
└── mapToDTO(Object, DynamicRenderDTO):boolean [implements abstract]
```

---

## 🔄 USAGE FLOW (How it will work)

### Current (No Pool)
```
Controller.snapshotRenderData()
  ├─ Model.snapshotRenderData() → List<BodyData>
  └─ for each BodyData:
     └─ DynamicRenderableMapper.fromBodyDTO(bodyData)
        └─ NEW DynamicRenderDTO(...) ← ALLOCATION HERE
```

### After Integration (With Pool)
```
Renderer
├─ pool = new DTOPool<>(() -> new DynamicRenderDTO(...))
├─ mapper = new DynamicRenderableMapper(pool)
└─ Controller.snapshotRenderData(mapper)
   ├─ Model.snapshotRenderData() → List<BodyData>
   └─ for each BodyData:
      └─ mapper.fromBodyDTOPooled(bodyData)
         ├─ dto = pool.acquire() [reuse from pool or new if empty]
         └─ mapToDTO(bodyData, dto) [updates in-place via updateFrom()]
         
Result: DTOs are reused, no allocations after warm-up

On renderable death:
  └─ Renderer.updateDynamicRenderables()
     └─ pool.release(dto) [dto.reset() + add to deque]
```

---

## 📝 NEXT STEPS (TO COMPLETE INTEGRATION)

### Step 1: Renderer Pool Setup
```java
// In Renderer.__init__ or activate()
this.dynamicRenderPool = new DTOPool<>(
    () -> new DynamicRenderDTO(null, 0, 0, 0, 0, 0L, 0, 0, 0, 0, 0L)
);
this.dynamicRenderMapper = new DynamicRenderableMapper(this.dynamicRenderPool);
```

### Step 2: Controller Integration
```java
// Add method overload in Controller
public ArrayList<DynamicRenderDTO> snapshotRenderData(DynamicRenderableMapper mapper) {
    ArrayList<BodyData> snapshot = this.model.snapshotRenderData();
    return mapper.fromBodyDTOPooled(snapshot);
}
```

### Step 3: Renderer Calls Pool-Aware Mapper
```java
// In Renderer.run() where snapshot is requested
ArrayList<DynamicRenderDTO> renderData = this.view.snapshotRenderData(this.dynamicRenderMapper);
this.updateDynamicRenderables(renderData);
```

### Step 4: Recycling on Renderable Death
```java
// In DynamicRenderable death or update failure
DynamicRenderDTO dto = (DynamicRenderDTO) renderable.getRenderData();
if (dto != null) {
    pool.release(dto);  // Resets DTO internally
}
```

---

## 📁 FILES THAT NEED MODIFICATION (Next Session)

1. **src/engine/view/core/Renderer.java**
   - Add `DTOPool<DynamicRenderDTO> dynamicRenderPool`
   - Add `DynamicRenderableMapper dynamicRenderMapper`
   - Initialize in constructor or activate()
   - Call `this.view.snapshotRenderData(mapper)` instead of no args
   - Add pool.release() in updateDynamicRenderables() for dead renderables

2. **src/engine/view/core/View.java**
   - Add overload: `snapshotRenderData(DynamicRenderableMapper mapper)`
   - Call `this.controller.snapshotRenderData(mapper)`

3. **src/engine/controller/impl/Controller.java**
   - Add overload: `snapshotRenderData(DynamicRenderableMapper mapper)`
   - Return `mapper.fromBodyDTOPooled(snapshot)`
   - Keep old `snapshotRenderData()` for backward compatibility if needed

---

## 🔧 KEY DESIGN DECISIONS

1. **Generic Pool vs Specific:** Used `DTOPool<T>` to enable pooling for ANY DTOPoolable type, not just DynamicRenderDTO.

2. **Mapper Pattern:** Created `DTOPooledMapper<T>` abstract base so future mappers (RenderableMapper, PlayerRenderableMapper, etc.) can reuse the same pattern.

3. **Both Paths:** Kept static `fromBodyDTO()` methods for backward compatibility; added `fromBodyDTOPooled()` for pool-aware usage.

4. **Reset Contract:** DTOs must implement `reset()` before returning to pool to ensure clean state.

5. **Deque vs Queue:** Used `ArrayDeque` in pool for efficient LIFO cache behavior (warm cache, less allocation fragmentation).

---

## ✨ BENEFITS AFTER INTEGRATION

| Metric | Before | After |
|--------|--------|-------|
| **DynamicRenderDTO allocations/frame** | 3000 | ~0 (after warm-up) |
| **Allocations/second** | ~180k | ~0 (stabilized) |
| **GC pressure** | High | Low |
| **Code complexity** | Low | Medium (but reusable) |

---

## ⚠️ IMPORTANT NOTES

- **Pool warm-up:** First frame creates up to `maxBodies` DTOs; subsequent frames reuse them.
- **Thread safety:** Current design (pool in Renderer, called only by render thread) is thread-safe without locks.
- **Reset importance:** If SKIP `dto.reset()` on release, stale data corrupts next use.
- **Null checks:** `mapToDTO()` must validate source and set default values if mapping fails.

---

## 🚀 START THE WORK

Implement the three integration steps above:
1. Setup pool and mapper in Renderer
2. Add overload in View and Controller
3. Pass mapper to controller snapshot call
4. Implement recycling logic in Renderer

After implementation, re-run profiling to verify allocation reduction (target: 180k → ~0/s after warm-up).
