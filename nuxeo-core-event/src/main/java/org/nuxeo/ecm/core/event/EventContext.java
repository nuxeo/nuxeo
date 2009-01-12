/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * An event context is describing the context in which a core event was raised.
 *  
 * You can subclass this class to implement more specialized event contexts 
 * like operations.
 * <p>
 * An event context is exposing information about the process the raised the event such as
 * <ul>
 * <li> the current core session.
 * <li> the current principal.
 * <li> the event data exposed as a list of arguments.
 * <li> random properties that are associated with the event.
 * These properties can be set by the event source or by any event listener 
 * that processes the event.   
 * </ul>
 * 
 * To add more information you need to implement more specialized event contexts. 
 * 
 * An event context also acts as an event factory. See {@link #event(String)} and {@link #event(String, int)} methods.
 * Events created by an event context are automatically mapped to that context. 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface EventContext extends Serializable {
    
    /**
     * Get event data. More objects can be associated with an event. 
     * For this reason an array of objects is returned.
     * This array is usually representing the arguments of the operation that raised the event.
     * 
     * @return the event data
     */
    Object[] getArguments();
    
    /**
     * Get the events properties.
     * Event properties are used to attach random information to an event context and can be set
     * by the event source or by any listener that is processing the event. 
     * These properties usually serves to share data between the source and the listeners.
     * 
     * @return the event properties
     */
    Map<String, Serializable> getProperties(); // TODO Serializable or Object?
    
    /**
     * Set a event context property
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, Serializable value);
    
    /**
     * Get the named property from this context or null if not exists
     * @param key the property key
     * @return the property or null if not exists
     */
    public Serializable getProperty(String key);
    
    /**
     * Tests whether or not the given property exists
     * @param key the property to test
     * @return true if the named property was set false otherwise 
     */
    public boolean hasProperty(String key);
    
    /**
     * Get the current core session if any. 
     * @return the core session or null if none
     */
    public CoreSession getCoreSession();
    
    /**
     * Get the current principal 
     * @return the principal. Cannot be null.
     */
    public NuxeoPrincipal getPrincipal();
    
    /**
     * Set the core session.
     * @param session
     */
    public void setCoreSession(CoreSession session);
    
    /**
     * Set the principal.
     * @param principal
     */
    public void setPrincipal(NuxeoPrincipal principal);    
    
    /**
     * Create a new event in that context given the event name. 
     * The default flags if the event will be used. 
     * @param name the event name
     * @return the event
     * @see EventContext#event(String, int)
     */
    public Event event(String name);
    
    /**
     * Create a new event in that context given the event name.
     * The given flags will be applied on the event.
     * 
     * @param name the event name
     * @param flags the event flags to use
     * @return the event the event
     */
    public Event event(String name, int flags);
    
}
