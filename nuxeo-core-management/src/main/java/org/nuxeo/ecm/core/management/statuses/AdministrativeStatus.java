package org.nuxeo.ecm.core.management.statuses;

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

	protected AdministrativeStatusPersister persister = new NuxeoAdministrativeStatusPersister();

	protected String value;

	protected String serverInstanceName;

	protected void activate() {
		 serverInstanceName = Framework.getProperties().getProperty(
				ADMINISTRATIVE_INSTANCE_ID);
		if (StringUtils.isEmpty(serverInstanceName)) {
			InetAddress addr;
			try {
				addr = InetAddress.getLocalHost();
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
	 * Disable services for this server
	 *
	 */
	public void setPassive() {
		value = PASSIVE;
		String lastValue = persister.setValue(serverInstanceName, value);
		if (!lastValue.equals(value)) {
			notifyEvent(PASSIVATED_EVENT);
		}
	}


	/**
	 * Enable services for this server
	 *
	 */
	public void setActive() {
		value = ACTIVE;
		String lastValue = persister.setValue(serverInstanceName, value);
		if (!lastValue.equals(value)) {
			notifyEvent(ACTIVATED_EVENT);
		}
	}

	/**
	 * Returns the stringified value of this status, ie: "passive" or "active"
	 *
	 * @return
	 */

	public String getValue() {
		return value;
	}

	/**
	 * Return this server unique name
	 *
	 */
	public String getServerInstanceName() {
		return serverInstanceName;
	}

	/**
	 * Returns true if server is in active state
	 *
	 * @return
	 */
	public boolean isActive() {
		return value.equals(ACTIVE);
	}

	/**
	 * Returns true if server is in passive state
	 * @return
	 */
	boolean isPassive() {
		return value.equals(PASSIVE);
	}

}