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
