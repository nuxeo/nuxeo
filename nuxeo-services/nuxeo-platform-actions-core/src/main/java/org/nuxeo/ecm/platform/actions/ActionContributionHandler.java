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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ActionContributionHandler extends ContributionFragmentRegistry<Action> {

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
    public void contributionUpdated(String actionId, Action action, Action origAction) {
        // given action contribution is already merged

        // store locally filters of new action since if action is
        // merged we will lose these filters - we should use the origAction
        // to retrieve the filters since the merged operation remove the filters.
        ActionFilter[] filters = origAction.getFilters();
        Action existingAction = actionReg.getAction(actionId);
        if (existingAction != null) {
            log.debug("Upgrading web action with id " + actionId);
            actionReg.removeAction(actionId);
        }

        // Register action's filter ids.
        List<String> filterIds = new ArrayList<String>();
        // now register embedded filters to filter registry and update the
        // filterIds list
        if (filters != null) {
            // register filters and save corresponding filter ids
            for (ActionFilter filter : filters) {
                filterReg.removeFilter(filter.getId());
                filterReg.addFilter(filter);
                filterIds.add(filter.getId());
            }
            // XXX: Remove filters from action as it was just temporary,
            // filters are now in their own registry.
            // XXX: let the filters as is - required to be able to reload the
            // component action.setFilters(null);
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

    @Override
    public String getContributionId(Action contrib) {
        return contrib.getId();
    }

    @Override
    public void merge(Action src, Action dst) {
        dst.mergeWith(src);
    }

}
