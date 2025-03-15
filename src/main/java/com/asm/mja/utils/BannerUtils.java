package com.asm.mja.utils;

/**
 * The BannerUtils class provides utility methods to generate banners for titles.
 *
 * @author ashut
 * @since 11-04-2024
 */

public class BannerUtils {

    /**
     * Generates a banner for the given title.
     *
     * @param title The title for which the banner is generated.
     * @return A string representing the banner.
     */
    public static String getBanner(String title) {
        title = title.toUpperCase();
        int length = title.length();
        StringBuilder banner = new StringBuilder();
        banner.append("  ");
        for (int i = 0; i < length + 2; i++) {
            banner.append('_');
        }
        banner.append("\n");
        banner.append(" |");
        for (int i = 0; i < length + 2; i++) {
            banner.append(' ');
        }
        banner.append("|\n");
        banner.append(" | ").append(title).append(" |\n");
        banner.append(" |");
        for (int i = 0; i < length + 2; i++) {
            banner.append('_');
        }
        banner.append("|\n");

        return banner.toString();
    }
}
