package com.asm.mja.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ashut
 * @since 12-04-2024
 */

public class DateUtils {

    public static String getFormattedTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return dateFormat.format(new Date());
    }

    public static String getFormattedTimestampForFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dateFormat.format(new Date());
    }

}
