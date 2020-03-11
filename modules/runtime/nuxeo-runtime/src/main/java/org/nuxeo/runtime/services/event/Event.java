/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.runtime.services.event;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
