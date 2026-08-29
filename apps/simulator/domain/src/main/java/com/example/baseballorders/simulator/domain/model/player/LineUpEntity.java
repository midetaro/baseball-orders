package com.example.baseballorders.simulator.domain.model.player;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public class LineUpEntity {

    @Getter
    private List<BatterEntity> batterEntities;
}
