/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.connect.identity.LogicalInstanceIdentifier;
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
            if ((refreshStatus == null || refreshStatus.isError()) && lastStatus != null && !lastStatus.isError()) {
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
                    log.warn("Cannot reach Nuxeo Online Services", e);
                    instanceStatus = new SubscriptionStatusWrapper("Nuxeo Online Services is not reachable");
                    instanceStatus.canNotReachConnectServer = true;
                } catch (ConnectClientVersionMismatchError e) {
                    log.warn(
                            "Nuxeo Connect Client does not have the required version to communicate with Nuxeo Online Services",
                            e);
                    instanceStatus = new SubscriptionStatusWrapper(e.getMessage());
                    instanceStatus.versionMismatch = true;
                } catch (ConnectSecurityError e) {
                    log.warn("Cannot authenticate against Nuxeo Online Services", e);
                    instanceStatus = new SubscriptionStatusWrapper(e);
                } catch (ConnectServerError e) {
                    log.error("Error while calling Nuxeo Online Services", e);
                    instanceStatus = new SubscriptionStatusWrapper(e.getMessage());
                }
            } else {
                instanceStatus = new UnresgistedSubscriptionStatusWrapper();
            }
        }
        return instanceStatus;
    }

    /**
     * Returns the registration expiration timestamp included in the CLID, or -1 if the CLID cannot be loaded or doesn't
     * include the expiration timestamp (old v0 format).
     *
     * @since 10.2
     */
    public long getRegistrationExpirationTimestamp() {
        LogicalInstanceIdentifier clid = getService().getCLID();
        if (clid == null) {
            return -1;
        }
        String clid1 = clid.getCLID1();
        if (clid1.length() == 36) {
            // no expiration timestamp (old v0 format)
            return -1;
        }
        // check format
        String[] split = clid1.split("\\.");
        if (split.length != 3) {
            // invalid format
            return -1;
        }
        // return expiration timestamp
        return Long.parseLong(split[1]);
    }

}
