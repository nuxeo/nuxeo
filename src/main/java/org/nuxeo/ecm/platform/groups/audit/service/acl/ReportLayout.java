package org.nuxeo.ecm.platform.groups.audit.service.acl;

import java.util.HashMap;
import java.util.Map;

public class ReportLayout {
    protected Map<String, Integer> userColumn;

    protected Map<Pair<String, String>, Integer> userAclColumn;

    public ReportLayout() {
        reset();
    }

    public void reset() {
        userColumn = new HashMap<String, Integer>();
        userAclColumn = new HashMap<Pair<String, String>, Integer>();
    }

    /** Store the user column */
    public void setUserColumn(int column, String userOrGroup) {
        userColumn.put(userOrGroup, column);
    }

    /** Return the user column */
    public int getUserColumn(String user) {
        return userColumn.get(user);
    }

    public void setUserAclColumn(int column, Pair<String, String> userAcl) {
        userAclColumn.put(userAcl, column);
    }

    /** Return the user column */
    public int getUserAclColumn(Pair<String, String> userAcl) {
        return userAclColumn.get(userAcl);
    }
}
