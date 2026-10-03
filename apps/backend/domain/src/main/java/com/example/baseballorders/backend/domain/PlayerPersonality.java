package com.example.baseballorders.backend.domain;

/**
 * 選手に設定する行動傾向。
 *
 * <p>{@link #DEFAULT} は画面上の「単打マン」を表す。既存の呼び出し元と未指定時の既定値との互換性のため、値の名前は変えない。
 */
public enum PlayerPersonality {
    /** 単打マン。 */
    DEFAULT,
    /** 中距離砲。 */
    MIDDLE_DISTANCE,
    /** 長距離砲。 */
    EAGER_SLUGGISH,
    /** 高出塁率。 */
    HIGH_ON_BASE,
    /** 盗塁重視。 */
    EAGER_STEAL,
    /** バント職人。 */
    EAGER_BUNT
}
