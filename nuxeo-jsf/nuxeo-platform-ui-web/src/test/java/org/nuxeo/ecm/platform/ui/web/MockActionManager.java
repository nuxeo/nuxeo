/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
        this.actions = new HashMap<>();
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
    public void addAction(Action action) {

    }

    @Override
    public Action removeAction(String actionId) {
        return null;
    }

    @Override
    public ActionFilter[] getFilters(String actionId) {
        throw new NotImplementedException();
    }

    @Override
    public ActionFilter getFilter(String filterId) {
        return null;
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
