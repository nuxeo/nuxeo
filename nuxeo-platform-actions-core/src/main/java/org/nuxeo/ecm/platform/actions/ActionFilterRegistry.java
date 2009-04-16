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
 *
 */
public class ActionFilterRegistry implements Serializable {

    private static final Log log = LogFactory.getLog(ActionFilterRegistry.class);
    private static final long serialVersionUID = 4838253869020090052L;

    private final Map<String, ActionFilter> filters;

    public ActionFilterRegistry() {
        filters = new HashMap<String, ActionFilter>();
    }

    public synchronized void addFilter(ActionFilter filter) {
        String id = filter.getId();

        if (log.isDebugEnabled()) {
            log.debug("Registering action filter: " + id);
        }

        if (filters.containsKey(id)) {
            // do not add twice an action
            return;
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
