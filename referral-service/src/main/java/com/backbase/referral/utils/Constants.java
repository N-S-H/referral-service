package com.backbase.referral.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String UPPER_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGITS = "0123456789";
    public static final String ALPHANUMERIC = UPPER_ALPHA + DIGITS;
    public static final String QR_CODE_CHARSET = "UTF-8";
    public static final Integer QR_CODE_HEIGHT = 200;
    public static final Integer QR_CODE_WIDTH = 200;
}