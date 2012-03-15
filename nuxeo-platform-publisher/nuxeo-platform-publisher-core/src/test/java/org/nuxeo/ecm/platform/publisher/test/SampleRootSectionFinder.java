package org.nuxeo.ecm.platform.publisher.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;

public class SampleRootSectionFinder extends DefaultRootSectionsFinder {

    public SampleRootSectionFinder(CoreSession userSession) {
        super(userSession);
    }

    protected String buildQuery(String path) {
        String query = "SELECT * FROM Document WHERE (";
        int i = 0;
        for (String type : getSectionTypes()) {
            query = query + " ecm:primaryType = '" + type + "'";
            i++;
            if (i < getSectionTypes().size()) {
                query = query + " or ";
            } else {
                query = query + " )";
            }
        }
        query = query + " order by ecm:path ";
        return query;
    }

}
