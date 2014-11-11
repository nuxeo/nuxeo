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

import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;

/**
 * Event stats service. Montoring / management frameworks may implement this interface and
 * register it as a nuxeo service to log event stats.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface EventStats {

    void logAsyncExec(EventListenerDescriptor desc, long delta);

    void logSyncExec(EventListenerDescriptor desc, long delta);

}
