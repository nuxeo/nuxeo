package org.nuxeo.ecm.core.management.statuses;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.api.Framework;

public class NuxeoInstanceIdentifierHelper {

    protected static String serverInstanceName;

    public static String getServerInstanceName() {
        if (serverInstanceName == null) {
            serverInstanceName = Framework.getProperties().getProperty(
                    AdministrativeStatusManager.ADMINISTRATIVE_INSTANCE_ID);
            if (StringUtils.isEmpty(serverInstanceName)) {
                InetAddress addr;
                try {
                    addr = InetAddress.getLocalHost();
                    serverInstanceName = addr.getHostName();
                } catch (UnknownHostException e) {
                    serverInstanceName = "localhost";
                }
            }
        }

        return serverInstanceName;
    }
}
