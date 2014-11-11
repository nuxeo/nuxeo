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

package org.nuxeo.ecm.platform.management.statuses;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.InlineEventContext;
import org.nuxeo.runtime.api.Framework;

public class AdministrativeStatus {

    public static final String ACTIVE = "active";

    public static final String PASSIVE = "passive";

    public static final String ADMINISTRATIVE_INSTANCE_ID = "org.nuxeo.ecm.instance.administrative.id";

    public static final String ADMINISTRATIVE_EVENT_CATEGORY = "administrativeCategory";

    public static final String ADMINISTRATIVE_STATUS_DOC_CREATED_EVENT = "administrativeStatusDocumentCreated";

    public static final String ACTIVATED_EVENT = "serverActivated";

    public static final String PASSIVATED_EVENT = "serverPassivated";

    protected AdministrativeStatusPersister persister = new NuxeoAdministrativeStatusPersister();

    protected String value;

    protected String serverInstanceName;


    protected void notifyEvent(String name) {
        Map<String, Serializable> eventProperties = new HashMap<String, Serializable>();
        eventProperties.put("category", ADMINISTRATIVE_EVENT_CATEGORY);
        EventContext ctx = new InlineEventContext(new SimplePrincipal(SecurityConstants.SYSTEM_USERNAME),eventProperties);
        Event event = ctx.newEvent(name);
        try {
            Framework.getService(EventProducer.class).fireEvent(event);
        } catch (Exception e) {
            throw new ClientRuntimeException(e);
        }
    }

    protected void activate() {
         serverInstanceName = Framework.getProperties().getProperty(
                ADMINISTRATIVE_INSTANCE_ID);
        if (StringUtils.isEmpty(serverInstanceName)) {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                serverInstanceName = addr.getHostName();
            } catch (UnknownHostException e) {
                serverInstanceName = "localhost";
            }
        }
        value = persister.getValue(serverInstanceName);
        if (value.equals(ACTIVE)) {
            notifyEvent(ACTIVATED_EVENT);
        } if (value.equals(PASSIVATED_EVENT)) {
             notifyEvent(PASSIVATED_EVENT);
        }
    }

    protected void deactivate() {
        serverInstanceName = null;
        value = null;
    }

    /**
     * Disables services for this server
     */
    public void setPassive() {
        value = PASSIVE;
        String lastValue = persister.setValue(serverInstanceName, value);
        if (!lastValue.equals(value)) {
            notifyEvent(PASSIVATED_EVENT);
        }
    }

    /**
     * Enables services for this server
     */
    public void setActive() {
        value = ACTIVE;
        String lastValue = persister.setValue(serverInstanceName, value);
        if (!lastValue.equals(value)) {
            notifyEvent(ACTIVATED_EVENT);
        }
    }

    /**
     * Returns the stringified value of this status, ie: "passive" or "active".
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns this server unique name.
     */
    public String getServerInstanceName() {
        return serverInstanceName;
    }

    /**
     * Returns true if server is in active state.
     */
    public boolean isActive() {
        return value.equals(ACTIVE);
    }

    /**
     * Returns true if server is in passive state.
     */
    boolean isPassive() {
        return value.equals(PASSIVE);
    }

}
