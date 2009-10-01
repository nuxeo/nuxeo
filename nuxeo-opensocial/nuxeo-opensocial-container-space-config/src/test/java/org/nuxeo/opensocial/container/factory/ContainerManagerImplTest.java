package org.nuxeo.opensocial.container.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

import edu.emory.mathcs.backport.java.util.Collections;

@RunWith(NuxeoRunner.class)
public class ContainerManagerImplTest {


  @Inject
  private CoreSession session;

  private static Space space1Space = null;

  private static SpaceManager spaceManager = null;
  private static ContainerManager containerManager = null;
  private static GadgetManager gadgetManager = null;

  private static HashMap<String, String> map = null;

  @Inject
  public ContainerManagerImplTest(TestRuntimeHarness harness) throws Exception {
    harness.deployBundle("org.nuxeo.opensocial.container");
    harness.deployBundle("org.nuxeo.opensocial.container.space.config");
    harness.deployBundle("org.nuxeo.ecm.spaces.api");
    harness.deployBundle("org.nuxeo.ecm.spaces.core");

    spaceManager = Framework.getService(SpaceManager.class);


    gadgetManager = new GadgetManagerImpl() {
      @Override
      protected CoreSession getCoreSession(Map<String, String> gwtParams)
          throws Exception {
        return session;
      }
    };
    containerManager = new ContainerManagerImpl() {
      @Override
      protected CoreSession getCoreSession(Map<String, String> containerParams)
          throws ClientException {
        return session;
      }
    };

    map = new HashMap<String, String>();

  }

  /**
   * Build a set of documents for testing
   *
   * @throws ClientException
   * @throws SpaceException
   */
  @Before
  public void reinitDatas() throws ClientException, SpaceException {
    DomainAndSiteUtils d = new DomainAndSiteUtils(session, spaceManager);
    d.create();
    space1Space = d.getSpace1Space();

  }

  @Test
  public void servicesExists() throws Exception {
    assertNotNull(spaceManager);
    assertNotNull(containerManager);
    assertNotNull(gadgetManager);
  }

  @Test
  public void testContainerManager() throws Exception {
    FactoryManager service = Framework.getService(FactoryManager.class);
    ContainerManager cFactory = service.getContainerFactory();
    assertTrue(cFactory instanceof ContainerManagerImpl);
  }

  @Test
  public void iCanCreateAContainerWithGadgetBeansFromADocumentId()
      throws Exception {

    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());

    Container container = containerManager.createContainer(map);

    assertNotNull(container);
    List<GadgetBean> gadgetBeans = container.getGadgets();
    assertNotNull(gadgetBeans);
    assertEquals(2, gadgetBeans.size());

  }

  @Test
  public void gadgetBeansFromContainerDataAreOk() throws Exception {

    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());

    Container container = containerManager.createContainer(map);

    List<GadgetBean> gadgetBeans = container.getGadgets();
    GadgetBean g1GadgetBean = gadgetBeans.get(0);
    GadgetBean g2GadgetBean = gadgetBeans.get(1);

    // createGadget(space1Space,
    // "g1","g1 title","g2 desc",null,"pl1","url",10,"c1","t1",false);
    assertEquals(g1GadgetBean.getTitle(), "g1 title");
    assertEquals(g1GadgetBean.getGadgetPosition()
        .getPlaceID(), "pl1");
    assertEquals(g1GadgetBean.getGadgetPosition()
        .getPosition()
        .intValue(), 10);
    assertEquals(g1GadgetBean.isCollapse(), false);

    // createGadget(space1Space,
    // "g2","g2 title","g2 desc",hashWith1Elt,"pl2","ur2",12,"c2","t2",true);
    assertEquals(g2GadgetBean.getTitle(), "g2 title");
    assertEquals(g2GadgetBean.getGadgetPosition()
        .getPlaceID(), "pl2");
    assertEquals(g2GadgetBean.getGadgetPosition()
        .getPosition()
        .intValue(), 12);
    assertEquals(g2GadgetBean.isCollapse(), true);

  }

  @Test
  public void iCanSaveLayout() throws ClientException {
    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());
    Container container = containerManager.createContainer(map);
    assertNotNull(container);
    assertEquals(container.getStructure(),
        ContainerManagerImpl.DEFAULT_STRUCTURE);
    assertEquals(container.getLayout(), ContainerManagerImpl.DEFAULT_LAYOUT);

    Container saveLayout = containerManager.saveLayout(map, "x-2-test");

    assertNotNull(saveLayout);
    assertEquals(saveLayout.getStructure(), 2);
    assertEquals(saveLayout.getLayout(), "x-2-test");

    Container saveLayout2 = containerManager.createContainer(map);
    assertNotNull(saveLayout2);
    assertEquals(saveLayout2.getStructure(), 2);
    assertEquals(saveLayout2.getLayout(), "x-2-test");

  }

  @Test
  public void iCanRemoveAGadget() throws Exception {
    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());

    Container container = containerManager.createContainer(map);

    List<GadgetBean> gadgetBeans = container.getGadgets();
    GadgetBean g2GadgetBean = gadgetBeans.get(1);

    gadgetManager.removeGadget(g2GadgetBean, map);

    List<GadgetBean> gadgetBeansAfterRemove = containerManager.createContainer(
        map)
        .getGadgets();
    assertNotNull(gadgetBeansAfterRemove);
    assertEquals(1, gadgetBeansAfterRemove.size());

  }

  @Test
  public void iCanSaveCollapsed() throws Exception {
    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());

    Container container = containerManager.createContainer(map);

    List<GadgetBean> gadgetBeans = container.getGadgets();
    GadgetBean g1GadgetBean = gadgetBeans.get(0);

    assertFalse(g1GadgetBean.isCollapse());
    g1GadgetBean.setCollapse(true);
    assertTrue(g1GadgetBean.isCollapse());
    gadgetManager.saveCollapsed(g1GadgetBean, map);

    List<GadgetBean> gadgetBeansAfterReload = containerManager.createContainer(
        map)
        .getGadgets();

    GadgetBean g1GadgetBeanAfterReload = gadgetBeansAfterReload.get(0);
    assertTrue(g1GadgetBeanAfterReload.isCollapse());
  }

  @Test
  public void iCanSavePosition() throws Exception {
    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());

    Container container = containerManager.createContainer(map);

    List<GadgetBean> gadgetBeans = container.getGadgets();
    Collections.sort(gadgetBeans);
    GadgetBean g1GadgetBean = gadgetBeans.get(0);

    assertEquals(10, g1GadgetBean.getGadgetPosition()
        .getPosition()
        .intValue());
    assertEquals("pl1", g1GadgetBean.getGadgetPosition()
        .getPlaceID());
    g1GadgetBean.getGadgetPosition()
        .setPosition(11);
    g1GadgetBean.getGadgetPosition()
        .setPlaceId("pl1Changed");
    assertEquals(11, g1GadgetBean.getGadgetPosition()
        .getPosition()
        .intValue());
    assertEquals("pl1Changed", g1GadgetBean.getGadgetPosition()
        .getPlaceID());

    gadgetManager.savePosition(g1GadgetBean, map);

    List<GadgetBean> gadgetBeansAfterReload = containerManager.createContainer(
        map)
        .getGadgets();

    Collections.sort(gadgetBeansAfterReload);

    GadgetBean g1GadgetBeanAfterReload = gadgetBeansAfterReload.get(0);
    assertEquals(11, g1GadgetBeanAfterReload.getGadgetPosition()
        .getPosition()
        .intValue());
    assertEquals("pl1Changed", g1GadgetBeanAfterReload.getGadgetPosition()
        .getPlaceID());

  }

  // @Test
  public void iCanSavePreferences() throws Exception {
    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());

    Container container = containerManager.createContainer(map);

    List<GadgetBean> gadgetBeans = container.getGadgets();
    Collections.sort(gadgetBeans);
    GadgetBean g1GadgetBean = gadgetBeans.get(0);

    assertEquals(0, g1GadgetBean.getUserPrefs()
        .size());

    Map<String, String> updatePrefs = new HashMap<String, String>();
    updatePrefs.put("key1", "value1");

    gadgetManager.savePreferences(g1GadgetBean, updatePrefs, map);

    List<GadgetBean> gadgetBeansAfterReload = containerManager.createContainer(
        map)
        .getGadgets();

    Collections.sort(gadgetBeansAfterReload);

    GadgetBean g1GadgetBeanAfterReload = gadgetBeansAfterReload.get(0);

    assertNotNull(g1GadgetBeanAfterReload.getUserPrefs());

    assertEquals(1, g1GadgetBeanAfterReload.getUserPrefs()
        .size());
    assertEquals(g1GadgetBeanAfterReload.getUserPrefs()
        .get(0)
        .getName(), "key1");
    assertEquals(g1GadgetBeanAfterReload.getUserPrefs()
        .get(0)
        .getValue(), "value1");

  }

  // @Test
  public void iCanGetGadgetRenderedUrlFromGadgetName() throws Exception {
    map.put(ContainerManagerImpl.DOC_REF, space1Space.getId());

    Container container = containerManager.createContainer(map);
    String renderUrl = container.getGadgets()
        .get(0)
        .getRenderUrl();
    assertEquals(renderUrl, "");
  }

  /**
   * Remove the set of documents
   *
   * @throws ClientException
   */
  @After
  public void clearDatas() throws ClientException {
    session.removeChildren(session.getRootDocument()
        .getRef());
    session.save();
  }

}
