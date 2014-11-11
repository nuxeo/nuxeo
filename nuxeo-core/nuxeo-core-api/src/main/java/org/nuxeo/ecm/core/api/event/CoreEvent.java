/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
