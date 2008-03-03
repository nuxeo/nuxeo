/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: CoreEvent.java 30799 2008-03-01 12:36:18Z bstefanescu $
 */

package org.nuxeo.ecm.core.api.event;

import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Nuxeo core event.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface CoreEvent {

    /**
     * Returns the date when the event occured.
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
