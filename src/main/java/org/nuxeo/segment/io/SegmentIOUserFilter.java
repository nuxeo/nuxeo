package org.nuxeo.segment.io;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
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
            UserManager um = Framework.getLocalService(UserManager.class);
            if (um==null && Framework.isTestModeSet()) {
                return "Guest";
            }
            try {
                anonymousUserId = um.getAnonymousUserId();
            } catch (ClientException e) {
                anonymousUserId = "Guest";
            }
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
