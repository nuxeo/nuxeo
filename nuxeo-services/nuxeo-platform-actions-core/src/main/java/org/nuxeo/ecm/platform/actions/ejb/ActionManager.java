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
 * $Id: ActionManager.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions.ejb;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ActionFilter;

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
     * Cleanup method.
     */
    void remove();

}
