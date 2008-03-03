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

public class EventContext {

    private final Object source;

    private final Object target;

    public EventContext(Object source, Object target) {
        this.source = source;
        this.target = target;
    }

    public Object getSource() {
        return source;
    }

    public Object getTarget() {
        return target;
    }
}
