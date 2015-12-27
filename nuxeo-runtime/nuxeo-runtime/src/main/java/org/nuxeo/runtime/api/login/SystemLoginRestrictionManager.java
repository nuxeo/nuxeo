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
package org.nuxeo.runtime.api.login;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Manage restrictions for usage of SystemLogin.
 * <p>
 * The main point is to prevent system login from untrusted remote nuxeo runtime instances.
 * <p>
 * Restrictions can be adjusted via system properties :
 * <ul>
 * <li>org.nuxeo.systemlogin.restrict : true/false (default true) ; turns on/off restrictions
 * <li>org.nuxeo.systemlogin.trusted.instances : comma separated list of trusted off (default : empty)
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
// FIXME: typos in API names.
public class SystemLoginRestrictionManager {

    public static final String RESTRICT_REMOTE_SYSTEM_LOGIN_PROP = "org.nuxeo.systemlogin.restrict";

    public static final String REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP = "org.nuxeo.systemlogin.trusted.instances";

    public static final String TRUSTED_INSTANCES_SEP = ",";

    protected static final Log log = LogFactory.getLog(SystemLoginRestrictionManager.class);

    protected Boolean restrictRemoteSystemLogin;

    protected List<String> allowedInstancesForSystemLogin;

    public boolean isRemoteSystemLoginRestricted() {
        if (restrictRemoteSystemLogin == null) {
            String prop = Framework.getProperty(RESTRICT_REMOTE_SYSTEM_LOGIN_PROP, "true");
            this.restrictRemoteSystemLogin = !prop.equalsIgnoreCase("false");
        }
        return restrictRemoteSystemLogin.booleanValue();
    }

    public List<String> getAllowedInstanceForSystemLogin() {
        if (allowedInstancesForSystemLogin == null) {
            String instanceKeys = Framework.getProperty(REMOTE_SYSTEM_LOGIN_TRUSTED_INSTANCES_PROP, null);
            if (instanceKeys != null) {
                instanceKeys = instanceKeys.trim();
                if (instanceKeys.endsWith(TRUSTED_INSTANCES_SEP)) {
                    instanceKeys = instanceKeys.substring(0, instanceKeys.length() - 1);
                }
                allowedInstancesForSystemLogin = Arrays.asList(instanceKeys.split(TRUSTED_INSTANCES_SEP));
            } else {
                allowedInstancesForSystemLogin = new ArrayList<String>();
            }
        }
        return allowedInstancesForSystemLogin;
    }

    public boolean isRemoveSystemLoginAllowedForInstance(String instanceId) {
        return getAllowedInstanceForSystemLogin().contains(instanceId);
    }

}
