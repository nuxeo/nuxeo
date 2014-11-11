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
 * $Id: ActionFilter.java 20637 2007-06-17 12:37:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ActionFilter extends Serializable {

    String getId();

    void setId(String id);

    /**
     * Checks whether this action is valid in the given context.
     * <p>
     * The action is considered valid if no denying rule is found and at least
     * one granting rule is found. If no rule is found at all, it is valid.
     * <p>
     * In other words: OR between granting rules, AND between denying rules,
     * denial is favored (also if exceptions occur), AND inside of rules, OR
     * inside or rule items (type, facet,...).
     *
     * @param action the optional action to check against, should be able to be
     *            null if filters evaluation only depends on given context.
     * @param context mandatory context holding variables to check against.
     * @returns true if filters configuration for given action and context.
     *          Returns false if an error occurs during one of the conditions
     *          evaluation.
     */
    boolean accept(Action action, ActionContext context);

    /**
     * Returns a clone, useful for hot reload.
     *
     * @since 5.6
     */
    ActionFilter clone();

}
