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
import java.rmi.dgc.VMID;

/**
 * An ordered set of events raised during an user operation.
 * <p>
 * The bundle is used collects any events that is raised during an user
 * operation. The bundle will be send after the operation commit to any
 * registered {@link PostCommitEventListener}.
 * <p>
 * The bundle implementation is free to ignore some events. This is the case for
 * events marked as inline or for duplicate events.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface EventBundle extends Iterable<Event>, Serializable {

    /**
     * Gets the bundle name.
     * <p>
     * This is usually the first event repository name in the bundle but the implementation
     * may decide to change this behavior.
     *
     * @return the bundle name. Can be null only if the bundle is empty.
     */
    String getName();

    /**
     * Adds an event in that bundle at the end of the list.
     * <p>
     * The bundle implementation must ignore redundant events and events marked
     * as inline.
     *
     * @param event the event to append.
     */
    void push(Event event);

    /**
     * Gets the first event in that bundle.
     *
     * @return the first event. Can be null if the bundle is empty
     */
    Event peek();

    /**
     * Tests whether or not this bundle is empty.
     */
    boolean isEmpty();

    /**
     * Gets the size of that bundle.
     *
     * @return the number of events in that bundle
     */
    int size();

    /**
     * Tests whether or not this event bundle was created on a remote machine.
     *
     * @return  true if the event bundle was fired from a remote machine, false otherwise
     */
    boolean hasRemoteSource();

    /**
     * Returns the VMID of the JVM where the bundle was created.
     */
    VMID getSourceVMID();

    /**
     * Check is bundle contains the specified event.
     */
    boolean containsEventName(String eventName);

}
