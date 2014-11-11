/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * An event context is describing the context in which a core event was raised.
 * <p>
 * You can subclass this class to implement more specialized event contexts like
 * operations.
 * <p>
 * An event context is exposing information about the process the raised the
 * event such as
 * <ul>
 * <li>the current core session.
 * <li>the current principal.
 * <li>the event data exposed as a list of arguments.
 * <li>random properties that are associated with the event. These properties
 * can be set by the event source or by any event listener that processes the
 * event.
 * </ul>
 *
 * To add more information you need to implement more specialized event
 * contexts.
 * <p>
 * An event context also acts as an event factory. See {@link #newEvent(String)}
 * and {@link #newEvent(String, int>)} methods. Events created by an event
 * context are automatically mapped to that context.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface EventContext extends Serializable {

    /**
     * Gets event data. More objects can be associated with an event.
     * <p>
     * For this reason an array of objects is returned. This array is usually
     * representing the arguments of the operation that raised the event.
     *
     * @return the event data
     */
    Object[] getArguments();

    /**
     * Gets the events properties.
     * <p>
     * Event properties are used to attach random information to an event
     * context and can be set by the event source or by any listener that is
     * processing the event. These properties usually serves to share data
     * between the source and the listeners.
     *
     * @return the event properties
     */
    Map<String, Serializable> getProperties(); // TODO Serializable or Object?

    /**
     * Replaces all properties with the given ones. The given map is set as is -
     * no copy occurs.
     *
     * @param properties the properties to use
     */
    void setProperties(Map<String, Serializable> properties);

    /**
     * Sets a event context property
     *
     * @param key the property key
     * @param value the property value
     */
    void setProperty(String key, Serializable value);

    /**
     * Gets the named property from this context or null if not exists.
     *
     * @param key the property key
     * @return the property, or null if it does not exist
     */
    Serializable getProperty(String key);

    /**
     * Tests whether or not the given property exists.
     *
     * @param key the property to test
     * @return true if the named property was set, false otherwise
     */
    boolean hasProperty(String key);

    /**
     * Gets the current core session if any.
     *
     * @return the core session, or null if none
     */
    CoreSession getCoreSession();

    /**
     * Gets the current principal.
     *
     * @return the principal. Cannot be null.
     */
    Principal getPrincipal();

    /**
     * Sets the core session.
     */
    void setCoreSession(CoreSession session);

    /**
     * Sets the principal.
     */
    void setPrincipal(Principal principal);

    /**
     * Creates a new event in that context given the event name. The default
     * flags for the event will be used.
     *
     * @param name the event name
     * @return the event
     * @see #newEvent(String, int)
     */
    Event newEvent(String name);

    /**
     * Creates a new event in that context given the event name. The given flags
     * will be applied on the event.
     *
     * @param name the event name
     * @param flags the event flags to use
     * @return the event
     */
    Event newEvent(String name, int flags);

    /**
     * Returns the repository name associated to the event context, if any.
     *
     * @return the repository name
     */
    String getRepositoryName();

    /**
     * Sets the repository name. Only used if no CoreSession is available.
     *
     * @param repositoryName the repository name, or {@code null}
     */
    void setRepositoryName(String repositoryName);

}
