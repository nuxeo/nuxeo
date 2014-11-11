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
 * $Id$
 */

package org.nuxeo.ecm.core.api.security;

import org.nuxeo.runtime.api.login.LoginComponent;

/**
 * @author Bogdan Stefanescu
 */
// TODO: make it a constant utility class instead of an interface.
public interface SecurityConstants {

    static final String SYSTEM_USERNAME = LoginComponent.SYSTEM_USERNAME;

    /**
     * @deprecated administrator user names are configurable on user manager
     */
    @Deprecated
    static final String ADMINISTRATOR = "Administrator";

    /**
     * @deprecated anonymous user name is configurable on user manager
     */
    @Deprecated
    static final String ANONYMOUS = "anonymous";

    /**
     * @deprecated administrators groups are configurable on user manager
     */
    @Deprecated
    static final String ADMINISTRATORS = "administrators";

    /**
     * @deprecated default group is configurable on user manager
     */
    @Deprecated
    static final String MEMBERS = "members";

    static final String EVERYONE = "Everyone";

    static final String EVERYTHING = "Everything";

    static final String RESTRICTED_READ = "RestrictedRead";

    static final String READ = "Read";

    static final String WRITE = "Write";

    static final String READ_WRITE = "ReadWrite";

    static final String REMOVE = "Remove";

    static final String VERSION = "Version";

    static final String BROWSE = "Browse";

    static final String WRITE_SECURITY = "WriteSecurity";

    static final String READ_SECURITY = "ReadSecurity";

    static final String READ_PROPERTIES = "ReadProperties";

    static final String WRITE_PROPERTIES = "WriteProperties";

    static final String READ_CHILDREN = "ReadChildren";

    static final String ADD_CHILDREN = "AddChildren";

    static final String REMOVE_CHILDREN = "RemoveChildren";

    static final String READ_LIFE_CYCLE = "ReadLifeCycle";

    static final String WRITE_LIFE_CYCLE = "WriteLifeCycle";

    static final String MANAGE_WORKFLOWS = "ManageWorkflows";

    static final String VIEW_WORKLFOW = "ReviewParticipant";

    static final String UNLOCK = "Unlock";

}
