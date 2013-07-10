/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     guillaume
 */
package org.nuxeo.ecm.platform.ui.web.application;

import javax.faces.application.StateManager;
import javax.faces.context.FacesContext;

import org.ajax4jsf.application.AjaxStateManager;
import org.ajax4jsf.application.StateHolder;

/**
 * Custom State Manager to force register NuxeoConversationStateHolder in session.
 *
 * @since 5.7.2
 */
public class NuxeoStateManager extends AjaxStateManager {

    /**
     * @param parent
     */
    public NuxeoStateManager(StateManager parent) {
        super(parent);
    }

    /**
     *
     *
     * @since 5.7.2
     */
    protected StateHolder getStateHolder(FacesContext context) {
        return NuxeoConversationStateHolder.newInstance(context);
    }

}
