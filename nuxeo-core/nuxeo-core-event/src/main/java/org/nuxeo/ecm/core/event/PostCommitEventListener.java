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

import org.nuxeo.ecm.core.api.ClientException;
import org.osgi.framework.BundleEvent;

/**
 * A specialized event listener that is notified after the user operation is
 * committed.
 * <p>
 * This type of listener can be notified either in a synchronous or asynchronous
 * mode.
 *
 * @see EventListener
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface PostCommitEventListener {

    /**
     * Handles the set of events that were raised during the life of an user
     * operation.
     * <p>
     * The events are fired as a {@link BundleEvent} after the transaction is
     * committed.
     *
     * @param events the events to handle
     */
    void handleEvent(EventBundle events) throws ClientException;

}
