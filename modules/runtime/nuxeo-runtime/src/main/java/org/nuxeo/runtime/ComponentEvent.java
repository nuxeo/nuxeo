/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime;

import java.io.Serializable;

import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * A component event.
 * <p>
 * The following event types are defined:
 * <ul>
 * <li> <code>COMPONENT_REGISTERED</code> Sent when registering a component after the component is created
 * <li> <code>ACTIVATING_COMPONENT</code> Sent before a component is activated
 * <li> <code>COMPONENT_ACTIVATED</code> Sent after the component is activated
 * <li> <code>DEACTIVATING_COMPONENT</code> Sent before a component is deactivated
 * <li> <code>COMPONENT_DEACTIVATED</code> Sent after a component is deactivated
 * <li> <code>COMPONENT_RESOLVED</code> Sent when a component was resolved (all dependencies are satisfied)
 * <li> <code>COMPONENT_UNRESOLVED</code> Sent when a component is unresolved (either it will be unregistered, either one
 * of its dependencies is no more satosfied)
 * <li> <code>COMPONENT_UNREGISTERED</code> Sent when unregsitering a component before the component is destroyed
 * <li> <code>COMPONENT_EVENT</code> May be used by components to end custom events
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComponentEvent implements Serializable {

    // the event IDs
    public static final int COMPONENT_REGISTERED = 1;

    public static final int ACTIVATING_COMPONENT = 2;

    public static final int DEACTIVATING_COMPONENT = 3;

    public static final int COMPONENT_ACTIVATED = 4;

    public static final int COMPONENT_DEACTIVATED = 5;

    public static final int COMPONENT_UNREGISTERED = 6;

    public static final int COMPONENT_RESOLVED = 7;

    public static final int COMPONENT_UNRESOLVED = 8;

    public static final int EXTENSION_REGISTERED = 9;

    public static final int EXTENSION_UNREGISTERED = 10;

    public static final int EXTENSION_PENDING = 11;

    public static final int STARTING_COMPONENT = 12;

    public static final int STOPPING_COMPONENT = 13;

    public static final int COMPONENT_STARTED = 14;

    public static final int COMPONENT_STOPPED = 15;

    public static final int COMPONENT_EVENT = 100;

    private static final long serialVersionUID = 8936615866437064000L;

    /** The event id. */
    public final int id;

    /** The component this event relates to if any, null otherwise. */
    public final RegistrationInfo registrationInfo;

    /** Optional event data. */
    public final Serializable data;

    public ComponentEvent(int id, RegistrationInfo ri) {
        this(id, ri, null);
    }

    public ComponentEvent(int id, RegistrationInfo ri, Serializable data) {
        this.id = id;
        registrationInfo = ri;
        this.data = data;
    }

    /**
     * Gets the event name as a string.
     *
     * @return the event name
     */
    public final String getEventName() {
        switch (id) {
        case COMPONENT_REGISTERED:
            return "COMPONENT_REGISTERED";
        case COMPONENT_RESOLVED:
            return "COMPONENT_RESOLVED";
        case COMPONENT_UNRESOLVED:
            return "COMPONENT_UNRESOLVED";
        case ACTIVATING_COMPONENT:
            return "ACTIVATING_COMPONENT";
        case DEACTIVATING_COMPONENT:
            return "DEACTIVATING_COMPONENT";
        case COMPONENT_ACTIVATED:
            return "COMPONENT_ACTIVATED";
        case COMPONENT_DEACTIVATED:
            return "COMPONENT_DEACTIVATED";
        case COMPONENT_UNREGISTERED:
            return "COMPONENT_UNREGISTERED";
        case STARTING_COMPONENT:
            return "STARTING_COMPONENT";
        case STOPPING_COMPONENT:
            return "STOPPING_COMPONENT";
        case COMPONENT_STARTED:
            return "COMPONENT_STARTED";
        case COMPONENT_STOPPED:
            return "COMPONENT_STOPPED";
        case COMPONENT_EVENT:
            return "COMPONENT_EVENT";
        case EXTENSION_REGISTERED:
            return "EXTENSION_REGISTERED";
        case EXTENSION_PENDING:
            return "EXTENSION_PENDING";
        }
        return "UNKNOWN_" + id;
    }

    @Override
    public String toString() {
        return getEventName() + ": " + registrationInfo.getName();
    }

}
