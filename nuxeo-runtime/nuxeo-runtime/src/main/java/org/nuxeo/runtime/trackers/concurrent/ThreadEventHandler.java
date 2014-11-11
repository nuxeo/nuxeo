/*******************************************************************************
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.nuxeo.runtime.trackers.concurrent;

import org.nuxeo.runtime.services.event.EventService;

/**
 * {@link ThreadEvent} handler that should be implemented by consumers. Could
 * be enlisted in the @{link {@link EventService} through the use of a
 * {@link ThreadEventListener}.
 *
 * @since 5.9.6
 * @author Stephane Lacoin at Nuxeo (aka matic)
 *
 */
public interface ThreadEventHandler {

    void onEnter(boolean isLongRunning);

    void onLeave();

}
