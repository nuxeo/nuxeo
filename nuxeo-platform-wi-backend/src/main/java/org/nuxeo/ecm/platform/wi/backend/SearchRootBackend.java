package org.nuxeo.ecm.platform.wi.backend;

import org.apache.commons.lang.StringUtils;

/**
 * Date: 04.03.2011
 * Time: 0:10:49
 *
 * @author Vitalii Siryi
 */
public class SearchRootBackend extends SearchVirtualBackend {

    private static final String QUERY = "select * from Workspace where ecm:mixinType != 'HiddenInNavigation' " +
            "AND  ecm:currentLifeCycleState != 'deleted' AND ecm:isProxy = 0 order by ecm:path";

    public SearchRootBackend() {
        super("", "", QUERY);
    }

    @Override
    public Backend getBackend(String uri) {
        if (StringUtils.isEmpty(uri) || "/".equals(uri)) {
            return this;
        } else {
            return super.getBackend(uri);
        }

    }

    @Override
    public boolean isRoot() {
        return true;
    }
}
