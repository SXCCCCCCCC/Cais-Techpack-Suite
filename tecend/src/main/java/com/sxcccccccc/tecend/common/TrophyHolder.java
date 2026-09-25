package com.sxcccccccc.tecend.common;

/**
 * 「这个方块实体里装着奖杯状态」。方块形态有两个 BE（普通 / Create 动力变体），
 * 它们没有共同父类，需要统一访问状态的地方（计划刻、掉落、以后的显示）走这个接口。
 */
public interface TrophyHolder {

    TrophyState trophyState();
}
