/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.forum.workflow;

import org.jbpm.graph.exe.ExecutionContext;

/**
 * Makes post document follow lifecycle transition to be in rejected state
 *
 * @author Anahide Tchertchian
 *
 */
public class RejectForumPostHandler extends ForumHandlerHelper {

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
            if (nuxeoHasStarted()) {
                followTransition(ForumConstants.TRANSITION_TO_REJECTED_STATE);
            }
        }
        executionContext.getToken().signal();
    }
}
