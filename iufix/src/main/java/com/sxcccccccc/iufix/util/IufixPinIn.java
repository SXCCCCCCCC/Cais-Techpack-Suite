package com.sxcccccccc.iufix.util;

import com.iucore.lib.pinin.PinIn;

/**
 * PinIn 拼音库持有类，初始化逻辑照搬 iucore 的 IUCore#<clinit>：
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
