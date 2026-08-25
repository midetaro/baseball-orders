package com.example.baseballorders.simulator.domain.model.behavior;

import com.example.baseballorders.simulator.domain.code.StealResult;

public interface StealStrategy {

    StealResult runToDouble();

    StealResult runToTriple();
}
