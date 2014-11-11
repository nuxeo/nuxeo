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

import javax.ejb.Local;
import javax.ejb.Remote;

import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ActionFilter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Local
@Remote
public interface ActionManager extends Serializable {

    boolean isEnabled(String actionId, ActionContext context);

    boolean isRegistered(String actionId);

    /**
     * Get actions for a category
     * Only actions available in the give context are returned
     * (Filters are evaluated)
     * @param category
     * @param context
     * @return
     */
    List<Action> getActions(String category, ActionContext context);

    /**
     * Get actions for a category
     * If hideUnavailableActions, all actions of the category are returned
     * but actions are flaged with a enable flag depending on filters evaluation
     * (Filters are evaluated)
     *
     * @param category
     * @param context
     * @param hideUnavailableActions
     * @return
     */
    List<Action> getActions(String category, ActionContext context,
            boolean hideUnavailableActions);

    Action getAction(String actionId);

    ActionFilter[] getFilters(String actionId);

    /**
     * Get all actions in a category
     * (Filters are NOT evaluated)
     * @param category
     * @return
     */
    List<Action> getAllActions(String category);

    /**
     * Initializes the bean with the associated nx runtime component.
     *
     */
    // public void initialize();
    /**
     * Cleanup method.
     *
     */
    void remove();

    // public void readState();

    // public void saveState();

}
