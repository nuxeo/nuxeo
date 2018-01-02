/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
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

    private List<TypeCompatibility> typeCategoryRelations;

    public ActionRegistry() {
        actions = new HashMap<>();
        categories = new HashMap<>();
        typeCategoryRelations = new ArrayList<>();
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
                acts = new ArrayList<>();
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
        List<Action> result = new LinkedList<>();
        Collection<String> ids;
        synchronized (this) {
            ids = categories.get(category);
        }
        if (ids != null) {
            for (String id : ids) {
                Action action = actions.get(id);
                if (action != null && action.isEnabled()) {
                    // UI type action compat check
                    Action finalAction = getClonedAction(action);
                    applyCompatibility(category, finalAction);
                    // return only enabled actions
                    result.add(finalAction);
                }
            }
        }
        result = sortActions(result);
        return result;
    }

    protected void applyCompatibility(Action finalAction) {
        if (finalAction != null && finalAction.getType() == null) {
            // iterate over all categories to apply compat
            String[] cats = finalAction.getCategories();
            if (cats != null) {
                for (String cat : cats) {
                    if (applyCompatibility(cat, finalAction)) {
                        break;
                    }
                }
            }
        }
    }

    protected boolean applyCompatibility(String category, Action finalAction) {
        if (finalAction != null && finalAction.getType() == null) {
            for (TypeCompatibility compat : typeCategoryRelations) {
                for (String compatCategory : compat.getCategories()) {
                    if (StringUtils.equals(compatCategory, category)) {
                        finalAction.setType(compat.getType());
                        if (applyCustomCompatibility(compat.getType(), finalAction)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Displays specific help messages for migration of actions.
     *
     * @since 6.0
     */
    protected boolean applyCustomCompatibility(String compatType, Action action) {
        // 6.0 BBB: home/admin tab actions migrated to widgets
        if ("admin_rest_document_link".equals(compatType) || "home_rest_document_link".equals(compatType)) {
            boolean applied = false;
            String link = action.getLink();
            if (link != null && !link.startsWith("/")) {
                action.setLink("/" + link);
                applied = true;
            }
            if (applied) {
                log.warn(String.format(
                        "Applied compatibility to action '%s', its configuration "
                                + "should be reviewed: make sure the link references an " + "absolute path",
                        action.getId()));
                return true;
            }
        }
        return false;
    }

    public synchronized Action getAction(String id) {
        Action action = actions.get(id);
        Action finalAction = getClonedAction(action);
        applyCompatibility(finalAction);
        return finalAction;
    }

    protected Action getClonedAction(Action action) {
        if (action == null) {
            return null;
        }
        return action.clone();
    }

    protected static List<Action> sortActions(Collection<Action> actions) {
        List<Action> sortedActions = new ArrayList<>();
        if (actions != null) {
            sortedActions.addAll(actions);
            Collections.sort(sortedActions);
        }
        return sortedActions;
    }

    public List<TypeCompatibility> getTypeCategoryRelations() {
        return typeCategoryRelations;
    }

    public void setTypeCategoryRelations(List<TypeCompatibility> typeCategoryRelations) {
        this.typeCategoryRelations = typeCategoryRelations;
    }

}
