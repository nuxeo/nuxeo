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

/**
 * An ordered set of events raised during an user operation.
 *  
 * The bundle is used collects any events that is raised during an user operation.
 * The bundle will be send after the operation commit to any registered {@link PostCommitEventListener}.   
 * 
 * The bundle implementation is free to ignore some events. 
 * This is the case for events marked as inline or for duplicate events. 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface EventBundle extends Iterable<Event> {

    /**
     * Get the bundle name. This is usually the first event name in the bundle but 
     * the implementation may decide to change this behavior. 
     * @return the bundle name. Can be null only if the bundle is empty.
     */
    String getName();
    
    /**
     * Get a list of event names in this bundle 
     * @return an array of event names. cannot be null.
     */
    String[] getEventNames();
    
    /**
     * Get a list of events in this bundle.
     * The return array is a copy of internal list of events in that bundle.  
     * @return the events in that bundle
     */
    Event[] getEvents();
    
    /**
     * Add an event in that bundle at the end of the list.
     * The bundle implementation must ignore redundant events and events marked as inline.   
     * @param event the event to append.
     */
    void push(Event event);    
    
    /**
     * Get the first event in that bundle
     * @return the first event. Can be null if the bundle is empty
     */
    Event peek();
    
    /**
     * Test whether or not this bundle is empty.
     * @return
     */
    boolean isEmpty();
    
    /**
     * Get the size of that bundle 
     * @return the number of events in that bundle
     */
    int size();
}
