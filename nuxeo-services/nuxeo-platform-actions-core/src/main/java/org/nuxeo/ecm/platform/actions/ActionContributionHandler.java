/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ActionContributionHandler extends ContributionFragmentRegistry<Action> {

    protected ActionRegistry actionReg;

    protected FilterContributionHandler filters;

    public ActionContributionHandler(FilterContributionHandler filters) {
        actionReg = new ActionRegistry();
        this.filters = filters;
    }

    public ActionRegistry getRegistry() {
        return actionReg;
    }

    @Override
    public Action clone(Action object) {
        return object.clone();
    }

    @Override
    public void contributionRemoved(String id, Action action) {
        actionReg.removeAction(id);
        // also remove local filters
        ActionFilter[] localFilters = action.getFilters();
        if (localFilters != null) {
            for (ActionFilter filter : localFilters) {
                // XXX: local filters implicitly append their rules to existing
                // ones => see append to true
                DefaultActionFilter f = (DefaultActionFilter) filter;
                f.setAppend(true);
                filters.removeContribution(f, true);
            }
        }
    }

    @Override
    public void contributionUpdated(String actionId, Action action, Action origAction) {
        // given action is already merged, just retrieve its inner filters to
        // register them to the filter registry
        List<String> newFilterIds = new ArrayList<>();
        List<String> existingFilterIds = action.getFilterIds();
        if (existingFilterIds != null) {
            newFilterIds.addAll(existingFilterIds);
        }
        ActionFilter[] newFilters = action.getFilters();
        if (newFilters != null) {
            // register embedded filters and save corresponding filter ids
            for (ActionFilter filter : newFilters) {
                String filterId = filter.getId();
                // XXX: local filters implicitly append their rules to existing
                // ones => see append to true
                DefaultActionFilter f = (DefaultActionFilter) filter;
                f.setAppend(true);
                filters.addContribution(f);
                if (!newFilterIds.contains(filterId)) {
                    newFilterIds.add(filterId);
                }
            }
            // XXX: Remove filters from action as it was just temporary,
            // filters are now in their own registry.
            action.setFilters(null);
        }
        action.setFilterIds(newFilterIds);

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
        if (source.isEnableSet() && source.isEnabled() != dest.isEnabled()) {
            dest.setEnabled(source.isEnabled());
        }

        // Merge categories without duplicates
        Set<String> mergedCategories = new HashSet<>(Arrays.asList(dest.getCategories()));
        mergedCategories.addAll(new HashSet<>(Arrays.asList(source.getCategories())));
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
        if (newConfirm != null && !"".equals(newConfirm) && !newConfirm.equals(dest.getConfirm())) {
            dest.setConfirm(newConfirm);
        }

        // title (tooltip)
        String tooltip = source.getHelp();
        if (tooltip != null && !tooltip.equals(dest.getHelp())) {
            dest.setHelp(tooltip);
        }

        // ui action type
        String type = source.getType();
        if (type != null && !type.equals(dest.getType())) {
            dest.setType(type);
        }

        // order
        int newOrder = source.getOrder();
        if (newOrder > 0 && newOrder != dest.getOrder()) {
            dest.setOrder(newOrder);
        }

        // filter ids
        List<String> newFilterIds = new ArrayList<>();
        newFilterIds.addAll(dest.getFilterIds());
        newFilterIds.addAll(source.getFilterIds());
        dest.setFilterIds(newFilterIds);

        // filters
        ActionFilter[] existingFilters = dest.getFilters();
        ActionFilter[] newFilters = source.getFilters();
        List<ActionFilter> filters = new ArrayList<>();
        if (existingFilters != null) {
            filters.addAll(Arrays.asList(existingFilters));
        }
        if (newFilters != null) {
            filters.addAll(Arrays.asList(newFilters));
        }
        dest.setFilters(filters.toArray(new ActionFilter[] {}));

        // accessKey
        String newAccessKey = source.getAccessKey();
        if (newAccessKey != null && !newAccessKey.isEmpty()) {
            dest.setAccessKey(newAccessKey);
        }

        // properties
        ActionPropertiesDescriptor newProps = source.getPropertiesDescriptor();
        if (newProps != null) {
            boolean append = newProps.isAppend();
            if (!append) {
                dest.setPropertiesDescriptor(newProps);
            } else {
                ActionPropertiesDescriptor oldProps = dest.getPropertiesDescriptor();
                if (oldProps != null) {
                    oldProps.merge(newProps);
                    dest.setPropertiesDescriptor(oldProps);
                } else {
                    dest.setPropertiesDescriptor(newProps);
                }
            }
        }

    }
}
