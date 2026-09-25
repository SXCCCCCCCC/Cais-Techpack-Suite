package com.sxcccccccc.iufix.util;

import me.towdium.pinin.PinIn;

/**
 * PinIn 拼音库持有类（1.4.3：改用官方 Towdium/PinIn 原版包名 me.towdium.pinin，
 * 消除 iucore 重定位版 com.iucore.lib.pinin 争议）。
 * 初始化逻辑照搬 iucore 的 IUCore#<clinit>（官方 API 与重定位 1.6.0 逐字节同源，
 * 仅包名不同，初始化链不变）：
 * PININ = new PinIn();
 * PININ.config().fZh2Z(true).fSh2S(true).fCh2C(true).accelerate(true).commit();
 */
public final class IufixPinIn {

    public static final PinIn PININ = new PinIn();

    static {
        PININ.config().fZh2Z(true).fSh2S(true).fCh2C(true).accelerate(true).commit();
    }

    private IufixPinIn() {
    }
}
