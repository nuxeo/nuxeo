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
 * $Id: ActionService.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ActionService extends DefaultComponent implements ActionManager {

    public static final ComponentName ID = new ComponentName(
            "org.nuxeo.ecm.platform.actions.ActionService");

    private static final long serialVersionUID = -5256555810901945824L;

    private static final Log log = LogFactory.getLog(ActionService.class);

    private ActionRegistry actionReg;

    private ActionFilterRegistry filterReg;

    @Override
    public void activate(ComponentContext context) {
        actionReg = new ActionRegistry();
        filterReg = new ActionFilterRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        actionReg = null;
        filterReg = null;
    }

    public ActionRegistry getActionRegistry() {
        return actionReg;
    }

    public ActionFilterRegistry getFilterRegistry() {
        return filterReg;
    }

    private void applyFilters(ActionContext context, List<Action> actions) {
        Iterator<Action> it = actions.iterator();
        while (it.hasNext()) {
            Action action = it.next();
            for (String filterId : action.getFilterIds()) {
                ActionFilter filter = filterReg.getFilter(filterId);
                if (filter == null) {
                    continue;
                }
                if (!filter.accept(action, context)) {
                    it.remove();
                    // handle next action
                    break;
                }
            }
        }
    }

    public List<Action> getActions(String category, ActionContext context) {
        return getActions(category, context, true);
    }

    public List<Action> getAllActions(String category) {
        return actionReg.getActions(category);
    }

    public List<Action> getActions(String category, ActionContext context,
            boolean hideUnavailableActions) {
        List<Action> actions = actionReg.getActions(category);
        if (hideUnavailableActions) {
            applyFilters(context, actions);
            Collections.sort(actions);
            return actions;
        } else {
            List<Action> allActions = new ArrayList<Action>();
            allActions.addAll(actions);
            applyFilters(context, actions);

            for (Action a : allActions) {
                a.setAvailable(actions.contains(a));
            }

            Collections.sort(allActions);
            return allActions;
        }
    }

    public Action getAction(String actionId) {
        return actionReg.getAction(actionId);
    }

    public boolean isRegistered(String actionId) {
        return actionReg.getAction(actionId) != null;
    }

    public boolean isEnabled(String actionId, ActionContext context) {
        Action action = actionReg.getAction(actionId);
        if (action != null) {
            return isEnabled(action, context);
        }
        return false;
    }

    public boolean isEnabled(Action action, ActionContext context) {
        for (String filterId : action.getFilterIds()) {
            ActionFilter filter = filterReg.getFilter(filterId);
            if (filter != null && !filter.accept(action, context)) {
                return false;
            }
        }
        return true;
    }

    public ActionFilter[] getFilters(String actionId) {
        Action action = actionReg.getAction(actionId);
        if (action == null) {
            return null;
        }
        List<String> filterIds = action.getFilterIds();
        if (filterIds != null && !filterIds.isEmpty()) {
            ActionFilter[] filters = new ActionFilter[filterIds.size()];
            for (int i=0; i<filters.length; i++) {
                String filterId = filterIds.get(i);
                filters[i] = filterReg.getFilter(filterId);
            }
            return filters;
        }
        return null;
    }

    @Override
    public void registerExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("actions")) {
            registerActionExtension(extension);
        } else if (xp.equals("filters")) {
            registerFilterExtension(extension);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        String xp = extension.getExtensionPoint();
        if (xp.equals("actions")) {
            unregisterActionExtension(extension);
        } else if (xp.equals("filters")) {
            unregisterFilterExtension(extension);
        }
    }

    public void registerFilterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            ActionFilter filter;
            if (contrib.getClass() == FilterFactory.class) {
                FilterFactory ff = (FilterFactory) contrib;
                filterReg.removeFilter(ff.id);
                try {
                    filter = (ActionFilter) Thread.currentThread().getContextClassLoader().loadClass(
                            ff.className).newInstance();
                    filter.setId(ff.id);
                    filterReg.addFilter(filter);
                } catch (Exception e) {
                    log.error("Failed to create action filter", e);
                }
            } else {
                filter = (ActionFilter) contrib;
                if (filterReg.getFilter(filter.getId()) != null) {
                    DefaultActionFilter newFilter = (DefaultActionFilter) filter;
                    DefaultActionFilter oldFilter = (DefaultActionFilter) filterReg.getFilter(filter.getId());

                    if (newFilter.getAppend()) {
                        List<FilterRule> mergedRules = new ArrayList<FilterRule>();

                        mergedRules.addAll(Arrays.asList(oldFilter.getRules()));
                        mergedRules.addAll(Arrays.asList(newFilter.getRules()));
                        oldFilter.setRules(mergedRules.toArray(new FilterRule[mergedRules.size()]));
                    } else {
                        filterReg.removeFilter(filter.getId());
                        filterReg.addFilter(filter);
                    }
                } else {
                    filterReg.addFilter(filter);
                }
            }
        }
    }

    public void unregisterFilterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            if (contrib.getClass() == FilterFactory.class) {
                filterReg.removeFilter(((FilterFactory) contrib).id);
            } else {
                filterReg.removeFilter(((ActionFilter) contrib).getId());
            }
        }
    }

    public void registerActionExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            Action action = (Action) contrib;
            String actionId = action.getId();
            Action existingAction = actionReg.getAction(actionId);
            if (existingAction != null) {
                log.debug("Upgrading web action with id " + actionId);
                action = mergeActions(existingAction, action);
                actionReg.removeAction(actionId);
            }

            // Register action's filter ids.
            List<String> filterIds = new ArrayList<String>();
            ActionFilter[] filters = action.getFilters();
            if (filters != null) {
                // register filters and save corresponding filter ids
                for (ActionFilter filter : filters) {
                    filterReg.removeFilter(filter.getId());
                    filterReg.addFilter(filter);
                    filterIds.add(filter.getId());
                }
                // XXX: Remove filters from action as it was just temporary,
                // filters are now in their own registry.
                action.setFilters(null);
            }

            List<String> actionFilterIds = action.getFilterIds();
            if (actionFilterIds == null) {
                action.setFilterIds(filterIds);
            } else {
                actionFilterIds.addAll(filterIds);
                action.setFilterIds(actionFilterIds);
            }

            if (action.getLabel() == null) {
                action.setLabel(actionId);
            }

            actionReg.addAction(action);
        }
    }

    public void unregisterActionExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            Action action = (Action) contrib;
            actionReg.removeAction(action.getId());
        }
    }

    /**
     * Merges two actions. Used when registering an existing action to overload
     * its configuration or add items to its properties.
     * <p>
     * Single properties are overloaded only when redefined (icon, enabled,
     * order, etc.), while multiple properties are merged with existing ones
     * (categories).
     *
     * @param existingOne the existing action
     * @param newOne the new action
     * @return a merged Action
     */
    protected static Action mergeActions(Action existingOne, Action newOne) {
        // Icon
        String newIcon = newOne.getIcon();
        if (newIcon != null && !newIcon.equals(existingOne.getIcon())) {
            existingOne.setIcon(newIcon);
        }

        // Enabled ?
        if (newOne.isEnabled() != existingOne.isEnabled()) {
            existingOne.setEnabled(newOne.isEnabled());
        }

        // Merge categories without duplicates
        Set<String> mergedCategories = new HashSet<String>(Arrays.asList(existingOne.getCategories()));
        mergedCategories.addAll(new HashSet<String>(Arrays.asList(newOne.getCategories())));
        existingOne.setCategories(mergedCategories.toArray(new String[mergedCategories.size()]));

        // label
        String newLabel = newOne.getLabel();
        if (newLabel != null && !newLabel.equals(existingOne.getLabel())) {
            existingOne.setLabel(newLabel);
        }

        // link
        String newLink = newOne.getLink();
        if (newLink != null && !newLink.equals(existingOne.getLink())) {
            existingOne.setLink(newLink);
        }

        // confirm
        String newConfirm = newOne.getConfirm();
        if (newConfirm != null && !newConfirm.equals(existingOne.getConfirm())) {
            existingOne.setConfirm(newConfirm);
        }

        // title (tooltip)
        String tooltip = newOne.getHelp();
        if (tooltip != null && !tooltip.equals(existingOne.getHelp())) {
            existingOne.setHelp(tooltip);
        }

        // XXX AT: maybe update param types but it seems a bit critical to do it
        // without control: a new action should be registered for this kind of
        // uses cases.

        // order
        int newOrder = newOne.getOrder();
        if (newOrder > 0 && newOrder != existingOne.getOrder()) {
            existingOne.setOrder(newOrder);
        }

        // filter ids
        List<String> newFilterIds = newOne.getFilterIds();
        newFilterIds.addAll(existingOne.getFilterIds());
        existingOne.setFilterIds(newFilterIds);

        // filters
        ActionFilter[] newFilters = newOne.getFilters();
        if (newFilters != null) {
            // XXX AT: no neded to merge: filters are not kept on the existing
            // action as they have been transformed into filter ids.
            existingOne.setFilters(newFilters);
        }

        return existingOne;
    }

    public void remove() {
    }

}
