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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: EventMessage.java 29569 2008-01-23 14:42:42Z tdelprat $
 */

package org.nuxeo.ecm.platform.events.api;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.Map;

import org.nuxeo.ecm.core.api.event.CoreEvent;

/**
 * Base event message interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface EventMessage extends Serializable {

    public String DUPLICATED = "DUPLICATED";

    public String BLOCK_JMS_PRODUCING = "BLOCK_JMS_PRODUCING";

    public String BLOCK_SYNC_INDEXING = "BLOCK_SYNC_INDEXING";

    public String BLOCK_ASYNC_INDEXING = "BLOCK_ASYNC_INDEXING";

    public String PERFORM_FULL_SYNC_INDEXING = "PERFORM_FULL_SYNC_INDEXING";

    Date getEventDate();

    String getEventId();

    Map<String, Serializable> getEventInfo();

    void setEventInfo(Map<String, Serializable> eventInfo);

    String getPrincipalName();

    Principal getPrincipal();

    String getCategory();

    String getComment();

    /**
     * Initialize this with a NXCore CoreEvent instance.
     *
     * @param coreEvent CoreEvent instance.
     * @deprecated feed the coreEvent through the constructor instead.
     */
    @Deprecated
    void feed(CoreEvent coreEvent);

}
