package org.nuxeo.ecm.platform.api.login;


public class RestrictedLoginHelper {

    protected static boolean restrictedModeActivated = false;

    public static void setRestrictedModeActivated(boolean restrictedModeActivated) {
        RestrictedLoginHelper.restrictedModeActivated = restrictedModeActivated;
    }

    public static boolean isRestrictedModeActivated() {
        return restrictedModeActivated;
    }

}
