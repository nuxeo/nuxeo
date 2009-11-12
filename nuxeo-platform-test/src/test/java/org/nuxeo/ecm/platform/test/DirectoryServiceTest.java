package org.nuxeo.ecm.platform.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.google.inject.Inject;


@RunWith(NuxeoPlatformRunner.class)
public class DirectoryServiceTest {

    @Inject
    DirectoryService ds;

    @Test
    public void theDirectoryServiceIsProvided() throws Exception {
        assertNotNull(ds);
    }
}
