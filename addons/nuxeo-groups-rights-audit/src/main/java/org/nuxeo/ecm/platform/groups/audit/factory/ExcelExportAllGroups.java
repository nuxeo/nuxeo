package org.nuxeo.ecm.platform.groups.audit.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.groups.audit.service.ExcelExportFactory;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class ExcelExportAllGroups implements ExcelExportFactory {

    public static final Log log = LogFactory.getLog(ExcelExportAllGroups.class);

    @Override
    public Map<String, Object> getDataToInject() {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        List<String> groupsId = new ArrayList<String>();
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>();
        try {
            groupsId = userManager.getGroupIds();
            for (String groupId : groupsId) {
                NuxeoGroup group;
                group = userManager.getGroup(groupId);
                groups.add(group);
            }
        } catch (ClientException e) {
            log.debug("Unable to fetch Nuxeo groups"
                    + e.getCause().getMessage());
        }
        Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("groups", groups);
        beans.put("userManager", userManager);
        return beans;
    }

}
