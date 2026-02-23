package engine.controller.mappers;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import engine.model.bodies.ports.BodyData;
import engine.model.physics.ports.PhysicsValuesDTO;
import engine.utils.pooling.PoolMDTO;
import engine.view.renderables.ports.DynamicRenderDTO;

public class DynamicRenderableMapper extends DTOPooledMapper<DynamicRenderDTO> {

    // Track entities already warned to avoid spamming the console repeatedly
    private static final Set<String> WARNED_ENTITIES = ConcurrentHashMap.newKeySet();


    public static DynamicRenderDTO fromBodyDTO(BodyData bodyData) {
        PhysicsValuesDTO phyValues = bodyData.getPhysicsValues();

        if (phyValues == null || bodyData.entityId == null) {
            return null;
        }

        DynamicRenderDTO renderablesData = new DynamicRenderDTO(
                bodyData.entityId,
                phyValues.posX, phyValues.posY,
                phyValues.angle,
                phyValues.size,
                phyValues.timeStamp,
                phyValues.speedX, phyValues.speedY,
                phyValues.accX, phyValues.accY,
                phyValues.timeStamp);

        return renderablesData;
    }

    public static ArrayList<DynamicRenderDTO> fromBodyDTO(ArrayList<BodyData> bodyData) {
        ArrayList<DynamicRenderDTO> renderableValues = new ArrayList<>();

        for (BodyData bodyDto : bodyData) {
            DynamicRenderDTO renderable = DynamicRenderableMapper.fromBodyDTO(bodyDto);
            renderableValues.add(renderable);
        }

        return renderableValues;
    }

    // region Pooled mapper

    public DynamicRenderableMapper(PoolMDTO<DynamicRenderDTO> pool) {
        super(pool);
    }

    public DynamicRenderDTO fromBodyDTOPooled(BodyData bodyData) {
        return this.map(bodyData);
    }

    public ArrayList<DynamicRenderDTO> fromBodyDTOPooled(ArrayList<BodyData> bodyDataList) {
        ArrayList<DynamicRenderDTO> renderables = new ArrayList<>();

        for (BodyData bodyData : bodyDataList) {
            DynamicRenderDTO dto = this.fromBodyDTOPooled(bodyData);
            if (dto != null) {
                renderables.add(dto);
            }
        }

        return renderables;
    }

    @Override
    protected boolean mapToDTO(Object source, DynamicRenderDTO target) {
        if (!(source instanceof BodyData)) {
            return false;
        }

        BodyData bodyData = (BodyData) source;
        PhysicsValuesDTO phyValues = bodyData.getPhysicsValues();

        if (phyValues == null || bodyData.entityId == null) {
            return false;
        }

        // Defensive check: detect invalid physics values early
        if (phyValues.size <= 0) {
            // Warn only once per entity to avoid spamming the log when bodies are
            // created/updated frequently before a valid size is available.
            if (WARNED_ENTITIES.add(bodyData.entityId)) {
                System.err.println("WARNING: DynamicRenderableMapper detected invalid size! " +
                    "entityId=" + bodyData.entityId + ", size=" + phyValues.size + 
                    ", pos=(" + phyValues.posX + "," + phyValues.posY + ")");
            }
            return false; // Skip this body to prevent rendering issues
        }

        target.updateFrom(
                bodyData.entityId,
                phyValues.posX, phyValues.posY,
                phyValues.angle,
                phyValues.size,
                phyValues.timeStamp,
                phyValues.speedX, phyValues.speedY,
                phyValues.accX, phyValues.accY,
                phyValues.timeStamp);

        return true;
    }

    // endregion
}