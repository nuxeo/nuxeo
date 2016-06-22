/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ActionFilter;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;

/**
 * @since 5.4.2
 */
public class MockActionManager implements ActionManager {

    private static final long serialVersionUID = 1L;

    protected final Map<String, Action> actions;

    public MockActionManager(List<Action> actions) {
        super();
        this.actions = new HashMap<String, Action>();
        if (actions != null) {
            for (Action action : actions) {
                this.actions.put(action.getId(), action);
            }
        }
    }

    @Override
    public Action getAction(String actionId) {
        return actions.get(actionId);
    }

    @Override
    public List<Action> getActions(String category, ActionContext context, boolean hideUnavailableActions) {
        throw new NotImplementedException();
    }

    @Override
    public List<Action> getActions(String category, ActionContext context) {
        throw new NotImplementedException();
    }

    @Override
    public List<Action> getAllActions(String category) {
        throw new NotImplementedException();
    }

    @Override
    public ActionFilter[] getFilters(String actionId) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isEnabled(String actionId, ActionContext context) {
        return true;
    }

    @Override
    public boolean isRegistered(String actionId) {
        throw new NotImplementedException();
    }

    @Override
    public void remove() {
        throw new NotImplementedException();
    }

    @Override
    public Action getAction(String actionId, ActionContext context, boolean hideUnavailableActions) {
        throw new NotImplementedException();
    }

    @Override
    public boolean checkFilter(String filterId, ActionContext context) {
        throw new NotImplementedException();
    }

    @Override
    public boolean checkFilters(List<String> filterIds, ActionContext context) {
        throw new NotImplementedException();
    }

    @Override
    public boolean checkFilters(Action action, ActionContext context) {
        throw new NotImplementedException();
    }

}
