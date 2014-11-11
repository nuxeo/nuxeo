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

package org.nuxeo.runtime.services.event;

import java.util.Arrays;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("listener")
public class ListenerDescriptor {

    private static final NullListener NULL_LISTENER = new NullListener();

    private static final Log log = LogFactory.getLog(ListenerDescriptor.class);

    @XNodeList(value = "topic", type = String[].class, componentType = String.class)
    String[] topics;

    EventListener listener;

    @XNode("@class")
    public void setListener(Class<EventListener> listenerClass) {
        try {
            listener = listenerClass.newInstance();
        } catch (Exception e) {
            log.error(e);
            listener = NULL_LISTENER;
        }
    }

    @Override
    public String toString() {
        return listener + " { " + Arrays.toString(topics) + " }";
    }

}

class NullListener implements EventListener {

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
    }

}
