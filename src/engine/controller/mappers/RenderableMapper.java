package engine.controller.mappers;

import java.util.ArrayList;

import engine.model.bodies.ports.BodyDTO;
import engine.view.renderables.ports.RenderDTO;

public class RenderableMapper {

    public static RenderDTO fromBodyDTO(BodyDTO bodyDto) {
        if (bodyDto.physicsValues == null || bodyDto.entityId == null) {
            return null;
        }

        RenderDTO renderablesData = new RenderDTO(
                bodyDto.entityId,
                bodyDto.physicsValues.posX, bodyDto.physicsValues.posY,
                bodyDto.physicsValues.angle,
                bodyDto.physicsValues.size,
                bodyDto.physicsValues.timeStamp);

        return renderablesData;
    }

    public static ArrayList<RenderDTO> fromBodyDTO(ArrayList<BodyDTO> bodyData) {
        ArrayList<RenderDTO> renderableValues = new ArrayList<>();

        for (BodyDTO bodyDto : bodyData) {
            RenderDTO renderable = RenderableMapper.fromBodyDTO(bodyDto);
            renderableValues.add(renderable);
        }

        return renderableValues;
    }

}