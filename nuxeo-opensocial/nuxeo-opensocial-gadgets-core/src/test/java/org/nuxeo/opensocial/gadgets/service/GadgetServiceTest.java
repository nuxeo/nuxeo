package org.nuxeo.opensocial.gadgets.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class GadgetServiceTest {

  private GadgetService service;

  @Inject
  public GadgetServiceTest(TestRuntimeHarness harness) throws Exception {

    harness.deployBundle("org.nuxeo.opensocial.gadgets.core");
    harness.deployBundle("org.nuxeo.opensocial.gadgets.core.test");
    service = Framework.getService(GadgetService.class);
    assertNotNull(service);

    OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();
    runtime.setProperty("gadgets.host", "localhost");
    runtime.setProperty("gadgets.port", "8080");
    runtime.setProperty("gadgets.path", "/manager/gadgets");

  }

  @Test
  public void iCanGetTheHelloGadgetExtension() throws Exception {
    GadgetDeclaration gadget = service.getGadget("hello");
    assertNotNull(gadget);

    assertEquals("hello", gadget.getName());
    assertEquals("/hello/hello", gadget.getMountPoint());
    assertEquals("hello-gadget", gadget.getDirectory());
    assertEquals("testCategory", gadget.getCategory());
  }
  
  @Test
  public void iCanGetTheHelloGadgetExtensionBis() throws Exception {
    GadgetDeclaration gadget = service.getGadget("hello.12354646464");
    assertNotNull(gadget);

    assertEquals("hello", gadget.getName());
    assertEquals("/hello/hello", gadget.getMountPoint());
    assertEquals("hello-gadget", gadget.getDirectory());
    assertEquals("testCategory", gadget.getCategory());
  }

  @Test
  public void iCanGetTheGadgetDef() throws Exception {


    GadgetDeclaration gadget = service.getGadget("hello");
    assertNotNull(gadget);
    InputStream gadgetStream = service.getGadgetResource("hello", "hello.xml");
    assertNotNull(gadgetStream);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int bytesRead = 0;
    while ((bytesRead = gadgetStream.read(buffer, 0, 8192)) != -1) {
      baos.write(buffer, 0, bytesRead);
    }
    // close the stream
    gadgetStream.close();

    String content = baos.toString();
    assertTrue(content.contains("Hello, world!"));

  }

  @Test
  public void iCanGetAGadgetResource() throws Exception {
    assertNotNull(service.getGadgetResource("hello", "hello.txt"));
    assertNotNull(service.getGadgetResource("hello", "dir/test.txt"));
    assertNull(service.getGadgetResource("hello", "noresources.txt"));
  }

  @Test
  public void iCanGetTheGadgetList() throws Exception {
    List<GadgetDeclaration> gadgetList = service.getGadgetList();
    assertTrue(1 < gadgetList.size());
    GadgetDeclaration gadget = service.getGadget("hello");
    assertNotNull(gadget);
  }

  @Test
  public void entryPointIsNameOrTheOneSpecified() throws Exception {
    GadgetDeclaration gadget = service.getGadget("hello2");
    assertNotNull(gadget);
    assertEquals("hello2.xml", gadget.getEntryPoint());

    gadget = service.getGadget("hello");
    assertNotNull(gadget);
    assertEquals("hello.xml", gadget.getEntryPoint());

  }

  @Test
  public void getURLOfTheGadget() throws Exception {
    OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();

    runtime.setProperty("gadgets.host", "localhost");
    runtime.setProperty("gadgets.port", "8080");
    runtime.setProperty("gadgets.path", "/gadgets");

    assertEquals(
        new URL("http://localhost:8080/gadgets/hello/hello/hello.xml"),
        service.getGadgetDefinition("hello"));
  }

  @Test
  public void getURLOfIconGadget() throws Exception {
    OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();

    runtime.setProperty("gadgets.host", "localhost");
    runtime.setProperty("gadgets.port", "8080");
    runtime.setProperty("gadgets.path", "/gadgets");

    URL icon = new URL("http://localhost:8080/gadgets/hello/hello/hello.png");
    assertEquals(icon, service.getIconUrl("hello"));
    assertEquals(icon, service.getGadget("hello")
        .getIconUrl());

    ;
  }

  @Test
  public void iCanGetTheGadgetListNameByCategory() throws Exception {
    Map<String, ArrayList<String>> gadgetNameByCategory = service.getGadgetNameByCategory();
    assertTrue(0 < gadgetNameByCategory.size());
    assertEquals(gadgetNameByCategory.get("testCategory")
        .size(), 2);
    assertTrue(gadgetNameByCategory.get("testCategory")
        .contains("hello"));
  }
}
