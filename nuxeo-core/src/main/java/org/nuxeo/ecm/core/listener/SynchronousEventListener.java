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
 * $Id: SynchronousEventListener.java 19491 2007-05-27 13:51:18Z sfermigier $
 */

package org.nuxeo.ecm.core.listener;

import org.nuxeo.ecm.core.api.event.CoreEvent;


/**
 * Synchronous core document event listener.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface SynchronousEventListener extends EventListener {

    /**
     * Document Listener notification.
     *
     * @param coreEvent the change event
     * @param timeout
     * @return boolean
     */
    boolean notifyEvent(CoreEvent coreEvent, int timeout);

}
