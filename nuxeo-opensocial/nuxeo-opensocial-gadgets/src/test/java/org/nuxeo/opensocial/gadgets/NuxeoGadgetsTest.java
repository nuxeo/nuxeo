package org.nuxeo.opensocial.gadgets;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class NuxeoGadgetsTest {

  private GadgetService service;

  @Inject
  public NuxeoGadgetsTest(TestRuntimeHarness harness) throws Exception {

    harness.deployBundle("org.nuxeo.opensocial.gadgets.core");
    harness.deployBundle("org.nuxeo.opensocial.gadgets");

    service = Framework.getService(GadgetService.class);
    assertNotNull(service);

  }

  @Test
  public void iCanGetTheMeteoGadget() throws Exception {
    GadgetDeclaration gadget = service.getGadget("meteo");
    assertNotNull(gadget);
  }

}
