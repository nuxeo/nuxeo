package org.nuxeo.runtime.management;

import java.util.Set;

import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestManageRuntimeService extends NXRuntimeTestCase {

    private RuntimeServiceMBeanAdapter adapterUnderTest = new RuntimeServiceMBeanAdapter();

    public void testPrint() {
        Set<String> resolvedComponents = adapterUnderTest.getResolvedComponents();
        assertNotNull(resolvedComponents);
        assertTrue(resolvedComponents.size() > 0);
        assertTrue(resolvedComponents.contains("service:org.nuxeo.runtime.api.ServiceManagement"));
    }

}
