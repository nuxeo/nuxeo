package org.nuxeo.ecm.platform.publisher.helper;

import org.nuxeo.ecm.core.api.CoreSession;

public interface RootSectionFinderFactory {

    RootSectionFinder getRootSectionFinder(CoreSession session);

}
