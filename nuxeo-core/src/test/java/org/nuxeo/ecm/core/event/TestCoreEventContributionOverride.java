package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.EventListener;
import org.nuxeo.ecm.core.repository.FakeEventListener;
import org.nuxeo.ecm.core.repository.OverrideEventListener;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestCoreEventContributionOverride extends NXRuntimeTestCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        deployBundle("org.nuxeo.ecm.core");
    }
    
    public void testContributionOverride() throws Exception {
        CoreEventListenerService cels = NXCore.getCoreEventListenerService();

        deployContrib("org.nuxeo.ecm.core.tests", "CoreEventListenerTestExtensions.xml");
        
        EventListener eventListener = cels.getEventListenerByName("fakelistener");
        
        assertTrue(eventListener instanceof FakeEventListener);
        
        deployContrib("org.nuxeo.ecm.core.tests","CoreEventListenerTestExtensionsOverride.xml");
        eventListener = cels.getEventListenerByName("fakelistener");
        
        assertTrue(eventListener instanceof OverrideEventListener);
    }
    
    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }

}
