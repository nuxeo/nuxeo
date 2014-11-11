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

package org.nuxeo.ecm.platform.jbpm.core.helper;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Action handler used to follow a given life cycle transition
 *
 * @author arussel
 * @author Anahide Tchertchian
 */
public class LifecycleTransitionActionHandler extends AbstractJbpmHandlerHelper {

    private static final long serialVersionUID = 1L;

    protected NuxeoPrincipal getNuxeoPrincipal(String user) throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        if (user.startsWith(NuxeoPrincipal.PREFIX)) {
            user = user.substring(NuxeoPrincipal.PREFIX.length());
        }
        return userManager.getPrincipal(user);
    }

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
            String endLifecycle = getEndLifecycleTransition();
            if (endLifecycle != null && !"".equals(endLifecycle)) {
                String transition;
                VersioningOption option;
                if (endLifecycle.endsWith(SUFFIX_MINOR)) {
                    transition = endLifecycle.substring(0,
                            endLifecycle.length() - SUFFIX_MINOR.length());
                    option = VersioningOption.MINOR;
                } else if (endLifecycle.endsWith(SUFFIX_MAJOR)) {
                    transition = endLifecycle.substring(0,
                            endLifecycle.length() - SUFFIX_MAJOR.length());
                    option = VersioningOption.MAJOR;
                } else {
                    transition = endLifecycle;
                    option = null;
                }
                String user = getInitiator();
                followTransition(getNuxeoPrincipal(user), getDocumentRef(),
                        transition, option);
            }
        }
        executionContext.getToken().signal();
    }

}
