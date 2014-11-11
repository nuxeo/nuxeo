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
 * $Id$
 */

package org.nuxeo.ecm.core.event.impl;

import java.util.Comparator;

public class EventListenerDescriptorComparator  implements Comparator<EventListenerDescriptor> {

    @Override
    public int compare(EventListenerDescriptor o1, EventListenerDescriptor o2) {
        return o1.getPriority() - o2.getPriority();
    }

}
