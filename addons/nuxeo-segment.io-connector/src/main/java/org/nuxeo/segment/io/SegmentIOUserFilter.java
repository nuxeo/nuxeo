/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.segment.io;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@XObject("userFilter")
public class SegmentIOUserFilter {

    @XNode("enableAnonymous")
    protected boolean enableAnonymous = false;

    @XNodeList(value = "blackListedUser", type = ArrayList.class, componentType = String.class)
    protected List<String> blackListedUsers;

    protected String anonymousUserId = null;

    public boolean isEnableAnonymous() {
        return enableAnonymous;
    }

    public List<String> getBlackListedUsers() {
        return blackListedUsers;
    }

    public String getAnonymousUserId() {
        if (anonymousUserId == null) {
            UserManager um = Framework.getService(UserManager.class);
            if (um==null && Framework.isTestModeSet()) {
                return "Guest";
            }
            anonymousUserId = um.getAnonymousUserId();
        }
        return anonymousUserId;
    }

    public boolean canTrack(String principalName) {

        if (!enableAnonymous && principalName.equals(getAnonymousUserId())) {
            return false;
        }
        if (blackListedUsers != null
                && blackListedUsers.contains(principalName)) {
            return false;
        }
        return true;
    }

}
