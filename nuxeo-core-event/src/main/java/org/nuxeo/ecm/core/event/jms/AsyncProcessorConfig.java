package org.nuxeo.ecm.core.event.jms;

import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to configured is Async Post Commit EventListener must be process
 * - by the core directly
 * or
 * - by JMS bus
 * <p/>
 * (mainly used for testing)
 *
 * @author tiry
 */
public class AsyncProcessorConfig {

    protected static Boolean forceJMSUsage = null;

    protected static String forceJMSUsageKey = "org.nuxeo.ecm.event.forceJMS";

    public static boolean forceJMSUsage() {

        if (forceJMSUsage == null) {
            String forceFlag = Framework.getProperty(forceJMSUsageKey, "false");
            forceJMSUsage = Boolean.parseBoolean(forceFlag);
        }
        return forceJMSUsage;
    }

    public static void setForceJMSUsage(boolean flag) {
        forceJMSUsage = flag;
    }

}
