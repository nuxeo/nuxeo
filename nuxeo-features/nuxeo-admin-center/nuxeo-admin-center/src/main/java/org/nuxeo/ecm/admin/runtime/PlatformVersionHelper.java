package org.nuxeo.ecm.admin.runtime;

import org.nuxeo.runtime.api.Framework;

public class PlatformVersionHelper {

    public static final String UNKNOWN = "Unknown";

    public static String getApplicationName() {
        return Framework.getProperty("org.nuxeo.ecm.product.name", UNKNOWN);
    }

    public static String getApplicationVersion() {
        return Framework.getProperty("org.nuxeo.ecm.product.version", UNKNOWN);
    }

    public static String getPlatformFilter() {
        if (getDistributionName().equals(UNKNOWN)) {
            return null;
        }
        return getDistributionName() + "-" + getDistributionVersion();
    }

    public static String getDistributionName() {
        return Framework.getProperty("org.nuxeo.distribution.name", UNKNOWN);
    }

    public static String getDistributionVersion() {
        return Framework.getProperty("org.nuxeo.distribution.version", UNKNOWN);
    }

    public static String getDistributionDate() {
        return Framework.getProperty("org.nuxeo.distribution.date", UNKNOWN);
    }

    public static String getDistributionHost() {
        return Framework.getProperty("org.nuxeo.distribution.server", UNKNOWN);
    }

    public static boolean isCompatible(String[] targetPlatforms, String currentPlatform) {
        if (targetPlatforms == null || targetPlatforms.length == 0 || currentPlatform == null) {
            return true;
        }
        for (String target : targetPlatforms) {
            if (currentPlatform.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCompatible(String[] targetPlatforms) {
        return isCompatible(targetPlatforms, getPlatformFilter());
    }

}
