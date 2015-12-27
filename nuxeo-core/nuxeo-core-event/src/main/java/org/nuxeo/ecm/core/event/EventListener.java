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


/**
 * An event listener receives notifications from core components.
 * <p>
 * Notifications are expressed as Event objects. This type of listeners are always invoked synchronously immediately
 * after the event is raised.
 *
 * @see PostCommitEventListener for asynchronous listeners or post commit listeners
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface EventListener {

    /**
     * Handle the given event. The listener can cancel the event by calling {@link Event#cancel()}
     *
     * @param event the event
     */
    void handleEvent(Event event);

}
