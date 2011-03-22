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
 * $Id$
 */

package org.nuxeo.runtime.services.event;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Event {

    private final String topic;
    private final String id;
    private final Object source;
    private final Object data;


    public Event(String topic, String id, Object source, Object data) {
        this.topic = topic == null ? "" : topic.intern();
        this.id = id;
        this.source = source;
        this.data = data;
    }


    public String getTopic() {
        return topic;
    }

    public String getId() {
        return id;
    }

    public Object getSource() {
        return source;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return topic + '/' + id + " [" + data + ']';
    }

}
