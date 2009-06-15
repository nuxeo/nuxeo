package org.nuxeo.ecm.platform.ui.web.cache;

/**
 *
 * Helper class to check if Seam s:cache tag can be used
 * (s:cache does not only require jboss-cache, but also some internal classes
 *
 * @author Thierry Delprat
 *
 */
public class SeamCacheHelper {

    protected static Boolean canUseSeamCache = null;

    public static boolean canUseSeamCache() {
        if (canUseSeamCache == null) {
            canUseSeamCache = false;
            try {
                Class.forName("org.jboss.system.ServiceMBeanSupport");
                canUseSeamCache = true;
            } catch (ClassNotFoundException e) {
            }
        }
        return canUseSeamCache.booleanValue();
    }
}
