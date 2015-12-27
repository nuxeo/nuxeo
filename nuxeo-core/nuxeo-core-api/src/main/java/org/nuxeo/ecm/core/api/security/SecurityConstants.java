/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    static final String READ_VERSION = "ReadVersion";

    static final String WRITE_VERSION = "WriteVersion";

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

    /**
     * Flag that can be used as principal to mark an unsupported ACL.
     */
    static final String UNSUPPORTED_ACL = "_UNSUPPORTED_ACL_";

}
