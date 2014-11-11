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
