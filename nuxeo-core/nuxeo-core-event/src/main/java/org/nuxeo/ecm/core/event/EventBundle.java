/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event;

import java.io.Serializable;
import java.rmi.dgc.VMID;

/**
 * An ordered set of events raised during an user operation.
 * <p>
 * The bundle is used collects any events that is raised during an user operation. The bundle will be send after the
 * operation commit to any registered {@link PostCommitEventListener}.
 * <p>
 * The bundle implementation is free to ignore some events. This is the case for events marked as inline or for
 * duplicate events.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface EventBundle extends Iterable<Event>, Serializable {

    /**
     * Gets the bundle name.
     * <p>
     * This is usually the first event repository name in the bundle but the implementation may decide to change this
     * behavior.
     *
     * @return the bundle name. Can be null only if the bundle is empty.
     */
    String getName();

    /**
     * Adds an event in that bundle at the end of the list.
     * <p>
     * The bundle implementation must ignore redundant events and events marked as inline.
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
     * @return true if the event bundle was fired from a remote machine, false otherwise
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
