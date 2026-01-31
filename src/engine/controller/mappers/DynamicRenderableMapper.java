package engine.controller.mappers;

import java.util.ArrayList;

import engine.model.bodies.ports.BodyDTO;
import engine.view.renderables.ports.DynamicRenderDTO;

public class DynamicRenderableMapper {

    public static DynamicRenderDTO fromBodyDTO(BodyDTO bodyDto) {
        if (bodyDto.physicsValues == null || bodyDto.entityId == null) {
            return null;
        }

        DynamicRenderDTO renderablesData = new DynamicRenderDTO(
                bodyDto.entityId,
                bodyDto.physicsValues.posX, bodyDto.physicsValues.posY,
                bodyDto.physicsValues.angle,
                bodyDto.physicsValues.size,
                bodyDto.physicsValues.timeStamp,
                bodyDto.physicsValues.speedX, bodyDto.physicsValues.speedY,
                bodyDto.physicsValues.accX, bodyDto.physicsValues.accY,
                bodyDto.physicsValues.timeStamp);

        return renderablesData;
    }

    public static ArrayList<DynamicRenderDTO> fromBodyDTO(ArrayList<BodyDTO> bodyData) {
        ArrayList<DynamicRenderDTO> renderableValues = new ArrayList<>();

        for (BodyDTO bodyDto : bodyData) {
            DynamicRenderDTO renderable = DynamicRenderableMapper.fromBodyDTO(bodyDto);
            renderableValues.add(renderable);
        }

        return renderableValues;
    }

}