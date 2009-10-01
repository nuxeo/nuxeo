package org.nuxeo.opensocial.container.component;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class FactoryManagerTest {



  @Inject
  public FactoryManagerTest(TestRuntimeHarness harness) throws Exception {
    harness.deployBundle("org.nuxeo.opensocial.container");
  }


  @Test
  public void testFactoryManager() throws Exception {
    FactoryManager service = Framework.getService(FactoryManager.class);
    assertNotNull(service);
  }

}
