package com.example.baseballorders.simulator.domain.model.player;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class LineUpEntity {

    @Getter private List<BatterEntity> batterEntities;
}
