package org.nuxeo.ecm.spaces.core.contribs.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PathHelperTest {

    @Test
    public void getParentPath() throws Exception {
        String path = "/default-domain/workspaces/galaxy/intralm";
        assertEquals("/default-domain/workspaces/galaxy",
                SingleDocSpaceProvider.getParentPath(path));
        assertEquals("intralm", SingleDocSpaceProvider.getDocName(path));
        assertEquals("/", SingleDocSpaceProvider.getParentPath("/home"));
    }
}
