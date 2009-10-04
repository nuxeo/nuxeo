package org.nuxeo.ecm.platform.tag;

import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.runtime.api.Framework;

public class TestJCRDeploy extends RepositoryOSGITestCase {

    TagServiceImpl tagService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");

        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.comment.core");
        deployBundle("org.nuxeo.ecm.platform.tag");
        deployBundle("org.nuxeo.ecm.platform.tag.tests");

        openRepository();

        tagService = (TagServiceImpl) Framework
                .getLocalService(TagService.class);
    }

    public void testTagServiceUnabled() throws Exception {
        assertFalse(tagService.isEnabled());
    }
}
