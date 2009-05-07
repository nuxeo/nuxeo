/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.events;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("event-listener")
public class EventListenerType implements Type {

    @XNode("event")
    public String eventName;

    @XNode("handler")
    public String handlerClassName;

    public EventListenerType() {
    }

    public EventListenerType(String eventName, String handlerClassName) {
        this.eventName = eventName;
        this.handlerClassName = handlerClassName;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.EVENT;
    }

    public String getTypeName() {
        return eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public String getHandlerClassName() {
        return handlerClassName;
    }

    public String getName() {
        return eventName;
    }

}
