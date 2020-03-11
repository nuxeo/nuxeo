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
 *     Nuxeo - initial API and implementation
 *
 * $Id: CoreEvent.java 30799 2008-03-01 12:36:18Z bstefanescu $
 */

package org.nuxeo.ecm.core.api.event;

import java.security.Principal;
import java.util.Date;
import java.util.Map;

/**
 * Nuxeo core event.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface CoreEvent {

    /**
     * Returns the date when the event occurred.
     *
     * @return a java date object
     */
    Date getDate();

    /**
     * Returns the event identifier.
     *
     * @return the event identifier
     */
    String getEventId();

    /**
     * Returns the information attached to the event.
     *
     * @return a map holding the event information
     */
    Map<String, ?> getInfo();

    /**
     * Returns the source object that originated the event.
     *
     * @return the object that originated the event
     */
    Object getSource();

    /**
     * Returns the principal responsible for this event.
     *
     * @return the principal responsible for this event.
     */
    Principal getPrincipal();

    /**
     * Returns the event category.
     *
     * @return the event category
     */
    String getCategory();

    /**
     * Returns the associated event comment.
     *
     * @return the associated event comment
     */
    String getComment();

}
