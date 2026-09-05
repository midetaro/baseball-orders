package com.example.baseballorders.simulator.domain.code;

/** 打席時のアウトカウント。 */
public enum OutCount {
    NO_OUT(0),
    ONE_OUT(1),
    TWO_OUT(2),
    THREE_OUT(3);

    private final int value;

    OutCount(int value) {
        this.value = value;
    }

    /**
     * アウト数を加算し、三アウトを上限とする新しいカウントを返す。
     *
     * @param increment 加算するアウト数
     * @return 加算後のアウトカウント
     */
    public OutCount add(long increment) {
        int nextValue = (int) Math.min(value + increment, THREE_OUT.value);
        return values()[nextValue];
    }
}
