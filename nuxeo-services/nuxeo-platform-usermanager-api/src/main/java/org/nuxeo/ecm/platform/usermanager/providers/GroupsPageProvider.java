package org.nuxeo.ecm.platform.usermanager.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Default Groups Provider
 *
 * @since 5.4.2
 */
public class GroupsPageProvider extends AbstractGroupsPageProvider {

    @Override
    protected List<DocumentModel> searchAllGroups() throws Exception {
        return Framework.getService(UserManager.class).searchGroups(
                Collections.<String, Serializable> emptyMap(), null);
    }

    @Override
    protected List<DocumentModel> searchGroups() throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        List<DocumentModel> groups = new ArrayList<DocumentModel>();
        String searchString = getFirstParameter();
        if ("*".equals(searchString)) {
            groups = searchAllGroups();
        } else if (!StringUtils.isEmpty(searchString)) {
            groups = userManager.searchGroups(searchString);
        }
        return groups;
    }

}
