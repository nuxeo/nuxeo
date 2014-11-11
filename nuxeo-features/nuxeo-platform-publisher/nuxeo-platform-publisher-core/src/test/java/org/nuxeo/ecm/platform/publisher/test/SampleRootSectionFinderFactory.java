package org.nuxeo.ecm.platform.publisher.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinderFactory;

public class SampleRootSectionFinderFactory implements RootSectionFinderFactory {

    @Override
    public RootSectionFinder getRootSectionFinder(CoreSession session) {
        return new SampleRootSectionFinder(session);
    }

}
