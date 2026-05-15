package org.example.domain.model.behavior;

import org.example.domain.code.StealResult;

public interface StealStrategy {

    StealResult runToDouble();

    StealResult runToTriple();
}
