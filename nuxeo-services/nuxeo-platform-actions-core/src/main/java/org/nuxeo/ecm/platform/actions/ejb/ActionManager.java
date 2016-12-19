/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.actions.ejb;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ActionFilter;
import org.nuxeo.ecm.platform.actions.ActionFilterRegistry;
import org.nuxeo.ecm.platform.actions.ActionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ActionManager extends Serializable {

    boolean isEnabled(String actionId, ActionContext context);

    boolean isRegistered(String actionId);

    /**
     * Gets actions for a category (filters are evaluated).
     * <p>
     * Only actions available in the given context are returned
     */
    List<Action> getActions(String category, ActionContext context);

    /**
     * Gets actions for a category (filters are evaluated).
     * <p>
     * If hideUnavailableActions, all actions of the category are returned but actions are flagged with an available
     * flag depending on filters evaluation.
     */
    List<Action> getActions(String category, ActionContext context, boolean hideUnavailableActions);

    Action getAction(String actionId);

    /**
     * Returns action with given id, evaluating its filters in given context, and returning null if filters evaluation
     * denies access or if action is not found.
     * <p>
     * If hideUnavailableActions is false, the action is always returned but it is flagged with an available flag
     * depending on filters evaluation.
     *
     * @since 5.6
     */
    Action getAction(String actionId, ActionContext context, boolean hideUnavailableActions);

    ActionFilter[] getFilters(String actionId);

    /**
     * @see ActionFilterRegistry#getFilter(String)
     * @since 9.1
     */
    ActionFilter getFilter(String filterId);

    /**
     * Returns false if given filter evaluation is supposed to deny access when checking for this filter.
     *
     * @since 5.6
     */
    boolean checkFilter(String filterId, ActionContext context);

    /**
     * Returns false if given filters evaluation is supposed to deny access when checking for this filter.
     *
     * @since 7.1
     */
    boolean checkFilters(List<String> filterIds, ActionContext context);

    /**
     * @since 8.2
     */
    boolean checkFilters(Action action, ActionContext context);

    /**
     * Gets all actions in a category (filters are NOT evaluated).
     */
    List<Action> getAllActions(String category);

    /**
     * @see ActionRegistry#addAction(Action)
     * @since 9.1
     */
    void addAction(Action action);

    /**
     * @see ActionRegistry#removeAction(String)
     * @since 9.1
     */
    Action removeAction(String actionId);

    /**
     * Cleanup method.
     */
    void remove();

}
