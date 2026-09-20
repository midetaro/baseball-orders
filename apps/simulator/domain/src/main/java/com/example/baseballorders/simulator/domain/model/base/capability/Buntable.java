package com.example.baseballorders.simulator.domain.model.base.capability;

/** 犠打を試みられる走者配置の能力。 */
public sealed interface Buntable permits SqueezeBuntable, AdvancingBuntable {}
