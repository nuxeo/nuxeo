/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.services.event;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("listener")
public class ListenerDescriptor {

    private static final EventListener NULL_LISTENER = event -> {};

    private static final Log log = LogFactory.getLog(ListenerDescriptor.class);

    @XNodeList(value = "topic", type = String[].class, componentType = String.class)
    String[] topics;

    EventListener listener;

    @XNode("@class")
    public void setListener(Class<EventListener> listenerClass) {
        try {
            listener = listenerClass.newInstance();
        } catch (ReflectiveOperationException e) {
            log.error(e);
            listener = NULL_LISTENER;
        }
    }

    @Override
    public String toString() {
        return listener + " { " + Arrays.toString(topics) + " }";
    }

}
