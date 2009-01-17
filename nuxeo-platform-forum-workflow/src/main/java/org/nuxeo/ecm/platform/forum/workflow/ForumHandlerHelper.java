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

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;

/**
 * Handler with helper methods for forum jbpm action handlers.
 *
 * @author Anahide Tchertchian
 *
 */
public abstract class ForumHandlerHelper extends AbstractJbpmHandlerHelper {

    protected NuxeoPrincipal getNuxeoPrincipal() throws Exception {
        NuxeoPrincipal principal = (NuxeoPrincipal) executionContext.getContextInstance().getTransientVariable(
                ForumConstants.PRINCIPAL);
        if (principal == null) {
            throw new IllegalArgumentException(
                    "Principal not found in transient process variables with key "
                            + ForumConstants.PRINCIPAL);
        }
        return principal;
    }

    protected void followTransition(String transition) throws Exception {
        String postId = getStringVariable(ForumConstants.POST_REF);
        DocumentRef postRef = new IdRef(postId);
        followTransition(getNuxeoPrincipal(), postRef, transition);
    }

}
