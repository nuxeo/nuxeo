/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: ActionFilterRegistry.java 20637 2007-06-17 12:37:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ActionFilterRegistry implements Serializable {

    private static final Log log = LogFactory.getLog(ActionFilterRegistry.class);

    private static final long serialVersionUID = 1L;

    private final Map<String, ActionFilter> filters;

    public ActionFilterRegistry() {
        filters = new HashMap<>();
    }

    public synchronized void addFilter(ActionFilter filter) {
        String id = filter.getId();
        if (log.isDebugEnabled()) {
            if (filters.containsKey(id)) {
                log.debug("Overriding action filter: " + id);
            } else {
                log.debug("Registering action filter: " + id);
            }
        }
        filters.put(id, filter);
    }

    public synchronized ActionFilter removeFilter(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Un-Registering action filter: " + id);
        }

        return filters.remove(id);
    }

    public synchronized Collection<ActionFilter> getFilters() {
        return Collections.unmodifiableCollection(filters.values());
    }

    public synchronized ActionFilter getFilter(String id) {
        return filters.get(id);
    }

}
