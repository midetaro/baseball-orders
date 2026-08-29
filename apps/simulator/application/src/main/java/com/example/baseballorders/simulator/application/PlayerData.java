package com.example.baseballorders.simulator.application;

/** A player's batting data received from SQS. */
public record PlayerData(String name, float hitAverage, float slugging) {}
