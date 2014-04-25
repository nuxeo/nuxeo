package org.nuxeo.segment.io;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

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

    public boolean isEnableAnonymous() {
        return enableAnonymous;
    }

    public List<String> getBlackListedUsers() {
        return blackListedUsers;
    }

    public boolean canTrack(Principal principal) {

        if (!enableAnonymous && principal instanceof NuxeoPrincipal
                && ((NuxeoPrincipal) principal).isAnonymous()) {
            return false;
        }
        if (blackListedUsers != null
                && blackListedUsers.contains(principal.getName())) {
            return false;
        }
        return true;
    }
}
