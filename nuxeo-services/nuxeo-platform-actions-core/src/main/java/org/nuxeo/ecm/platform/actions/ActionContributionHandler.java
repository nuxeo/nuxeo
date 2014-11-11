/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ActionContributionHandler extends
        ContributionFragmentRegistry<Action> {

    private final Log log = LogFactory.getLog(ActionContributionHandler.class);

    protected ActionRegistry actionReg;

    protected ActionFilterRegistry filterReg;

    public ActionContributionHandler(ActionFilterRegistry fitlerReg) {
        actionReg = new ActionRegistry();
        this.filterReg = fitlerReg;
    }

    public ActionRegistry getRegistry() {
        return actionReg;
    }

    @Override
    public Action clone(Action object) {
        try {
            return object.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e); // never happens
        }
    }

    @Override
    public void contributionRemoved(String id, Action origContrib) {
        actionReg.removeAction(id);
    }

    @Override
    public void contributionUpdated(String actionId, Action action,
            Action origAction) {
        // given action is already merged, do nothing with origAction

        Action registeredAction = actionReg.getAction(actionId);
        if (registeredAction != null) {
            log.debug("Upgrading web action with id " + actionId);
            actionReg.removeAction(actionId);
        }

        List<String> newFilterIds = new ArrayList<String>();
        ActionFilter[] newFilters = action.getFilters();
        if (newFilters != null) {
            // register embedded filters and save corresponding filter ids
            for (ActionFilter filter : newFilters) {
                String filterId = filter.getId();
                filterReg.removeFilter(filterId);
                filterReg.addFilter(filter);
                newFilterIds.add(filterId);
            }
            // XXX: Remove filters from action as it was just temporary,
            // filters are now in their own registry.
            action.setFilters(null);
        }

        List<String> actionFilterIds = action.getFilterIds();
        if (actionFilterIds == null) {
            action.setFilterIds(newFilterIds);
        } else {
            actionFilterIds.addAll(newFilterIds);
            action.setFilterIds(actionFilterIds);
        }

        // set a default label
        if (action.getLabel() == null) {
            action.setLabel(action.getId());
        }

        actionReg.addAction(action);
    }

    @Override
    public String getContributionId(Action contrib) {
        return contrib.getId();
    }

    @Override
    public void merge(Action source, Action dest) {
        // Icon
        String newIcon = source.getIcon();
        if (newIcon != null && !newIcon.equals(dest.getIcon())) {
            dest.setIcon(newIcon);
        }

        // Enabled ?
        if (source.isEnabled() != dest.isEnabled()) {
            dest.setEnabled(source.isEnabled());
        }

        // Merge categories without duplicates
        Set<String> mergedCategories = new HashSet<String>(
                Arrays.asList(dest.getCategories()));
        mergedCategories.addAll(new HashSet<String>(
                Arrays.asList(source.getCategories())));
        dest.setCategories(mergedCategories.toArray(new String[mergedCategories.size()]));

        // label
        String newLabel = source.getLabel();
        if (newLabel != null && !newLabel.equals(dest.getLabel())) {
            dest.setLabel(newLabel);
        }

        // link
        String newLink = source.getLink();
        if (newLink != null && !newLink.equals(dest.getLink())) {
            dest.setLink(newLink);
        }

        // confirm
        String newConfirm = source.getConfirm();
        if (newConfirm != null && !"".equals(newConfirm)
                && !newConfirm.equals(dest.getConfirm())) {
            dest.setConfirm(newConfirm);
        }

        // title (tooltip)
        String tooltip = source.getHelp();
        if (tooltip != null && !tooltip.equals(dest.getHelp())) {
            dest.setHelp(tooltip);
        }

        // order
        int newOrder = source.getOrder();
        if (newOrder > 0 && newOrder != dest.getOrder()) {
            dest.setOrder(newOrder);
        }

        // filter ids
        List<String> newFilterIds = new ArrayList<String>();
        newFilterIds.addAll(dest.getFilterIds());
        newFilterIds.addAll(source.getFilterIds());
        dest.setFilterIds(newFilterIds);

        // filters
        ActionFilter[] existingFilters = dest.getFilters();
        ActionFilter[] newFilters = source.getFilters();
        List<ActionFilter> filters = new ArrayList<ActionFilter>();
        if (existingFilters != null) {
            filters.addAll(Arrays.asList(existingFilters));
        }
        if (newFilters != null) {
            filters.addAll(Arrays.asList(newFilters));
        }
        dest.setFilters(filters.toArray(new ActionFilter[] {}));

    }
}
