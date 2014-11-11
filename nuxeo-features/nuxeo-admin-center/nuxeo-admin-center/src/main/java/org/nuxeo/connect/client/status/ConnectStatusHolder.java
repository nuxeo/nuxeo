/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.connect.client.status;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.connector.CanNotReachConnectServer;
import org.nuxeo.connect.connector.ConnectClientVersionMismatchError;
import org.nuxeo.connect.connector.ConnectSecurityError;
import org.nuxeo.connect.connector.ConnectServerError;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class ConnectStatusHolder {

    protected static ConnectStatusHolder instance;

    protected SubscriptionStatusWrapper instanceStatus;

    protected static final Log log = LogFactory.getLog(ConnectStatusHolder.class);

    protected static final int REFRESH_PERIOD_MINUTES = 10;

    public static ConnectStatusHolder instance() {
        if (instance == null) {
            instance = new ConnectStatusHolder();
        }
        return instance;
    }

    protected ConnectRegistrationService getService() {
        return Framework.getLocalService(ConnectRegistrationService.class);
    }

    public boolean isRegistred() {
        // no cache needed
        return getService().isInstanceRegistred();
    }

    public SubscriptionStatusWrapper getStatus() {

        // get status (possibility from cache)
        SubscriptionStatusWrapper lastStatus = getStatus(false);

        // check freshness
        Calendar oldestStatusDate = Calendar.getInstance();
        oldestStatusDate.add(Calendar.MINUTE, -REFRESH_PERIOD_MINUTES);
        if (lastStatus == null || lastStatus.refreshDate.before(oldestStatusDate)) {
            // try to refresh
            SubscriptionStatusWrapper refreshStatus = getStatus(true);
            // keep last success status in case of error
            if ((refreshStatus == null || refreshStatus.isError())
                    && lastStatus != null && !lastStatus.isError()) {
                instanceStatus = lastStatus;
                instanceStatus.refreshDate = Calendar.getInstance();
            }
        }

        return instanceStatus;
    }

    public void flush() {
        instanceStatus = null;
    }

    public SubscriptionStatusWrapper getStatus(boolean forceRefresh) {
        if (instanceStatus == null || forceRefresh) {
            if (isRegistred()) {
                try {
                    instanceStatus = new SubscriptionStatusWrapper(getService().getConnector().getConnectStatus());
                } catch (CanNotReachConnectServer e) {
                    log.warn("can not reach connect server", e);
                    instanceStatus = new SubscriptionStatusWrapper("Nuxeo Connect Server is not reachable");
                    instanceStatus.canNotReachConnectServer = true;
                } catch (ConnectClientVersionMismatchError e) {
                    log.warn(
                            "Connect Client does not have the required version to communicate with Nuxeo Connect Server",
                            e);
                    instanceStatus = new SubscriptionStatusWrapper(e.getMessage());
                    instanceStatus.versionMismatch = true;
                } catch (ConnectSecurityError e) {
                    log.warn("Can not authenticated against Connect Server", e);
                    instanceStatus = new SubscriptionStatusWrapper(e);
                } catch (ConnectServerError e) {
                    log.error("Error while calling connect server", e);
                    instanceStatus = new SubscriptionStatusWrapper(e.getMessage());
                }
            } else {
                instanceStatus = new UnresgistedSubscriptionStatusWrapper();
            }
        }
        return instanceStatus;
    }

}
