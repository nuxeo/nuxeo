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
 * $Id: ActionRegistry.java 20637 2007-06-17 12:37:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ActionRegistry implements Serializable {

    private static final Log log = LogFactory.getLog(ActionRegistry.class);

    private static final long serialVersionUID = 8425627293154848041L;

    private final Map<String, Action> actions;

    private final Map<String, List<String>> categories;

    public ActionRegistry() {
        actions = new HashMap<String, Action>();
        categories = new HashMap<String, List<String>>();
    }

    public synchronized void addAction(Action action) {
        String id = action.getId();
        if (log.isDebugEnabled()) {
            if (actions.containsKey(id)) {
                log.debug("Overriding action: " + action);
            } else {
                log.debug("Registering action: " + action);
            }
        }
        // add a default label if not set
        if (action.getLabel() == null) {
            action.setLabel(action.getId());
        }
        actions.put(id, action);
        for (String category : action.getCategories()) {
            List<String> acts = categories.get(category);
            if (acts == null) {
                acts = new ArrayList<String>();
            }
            if (!acts.contains(id)) {
                acts.add(id);
            }
            categories.put(category, acts);
        }
    }

    public synchronized Action removeAction(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Unregistering action: " + id);
        }

        Action action = actions.remove(id);
        if (action != null) {
            for (String category : action.getCategories()) {
                List<String> acts = categories.get(category);
                if (acts != null) {
                    acts.remove(id);
                }
            }
        }
        return action;
    }

    public synchronized Collection<Action> getActions() {
        return Collections.unmodifiableCollection(sortActions(actions.values()));
    }

    public List<Action> getActions(String category) {
        List<Action> result = new LinkedList<Action>();
        Collection<String> ids;
        synchronized (this) {
            ids = categories.get(category);
        }
        if (ids != null) {
            for (String id : ids) {
                Action action = actions.get(id);
                if (action != null && action.isEnabled()) {
                    // return only enabled actions
                    result.add(getClonedAction(action));
                }
            }
        }
        result = sortActions(result);
        return result;
    }

    public synchronized Action getAction(String id) {
        Action action = actions.get(id);
        return getClonedAction(action);
    }

    protected Action getClonedAction(Action action) {
        if (action == null) {
            return null;
        }
        try {
            return action.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e); // should never happen
        }
    }

    protected static List<Action> sortActions(Collection<Action> actions) {
        List<Action> sortedActions = new ArrayList<Action>();
        if (actions != null) {
            sortedActions.addAll(actions);
            Collections.sort(sortedActions);
        }
        return sortedActions;
    }

}
