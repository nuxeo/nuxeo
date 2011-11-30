package org.nuxeo.apidoc.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public abstract class AbstractDocumentationItem implements DocumentationItem {

    @Override
    public int compareTo(DocumentationItem o) {

        List<String> myVersions = new ArrayList<String>(getApplicableVersion());
        List<String> otherVersions = new ArrayList<String>(o.getApplicableVersion());

        Collections.sort(myVersions);
        Collections.sort(otherVersions);
        Collections.reverse(myVersions);
        Collections.reverse(otherVersions);

        if (myVersions.isEmpty()) {
            if (otherVersions.isEmpty()) {
                return 0;
            }
            return 1;
        } else if (otherVersions.isEmpty()) {
            return -1;
        }

        return myVersions.get(0).compareTo(otherVersions.get(0));
    }

}
