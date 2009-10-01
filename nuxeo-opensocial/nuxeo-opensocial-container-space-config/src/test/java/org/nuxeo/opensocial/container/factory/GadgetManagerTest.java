package org.nuxeo.opensocial.container.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.social.core.config.SocialApiGuiceModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.opensocial.container.client.bean.Container;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;
import org.nuxeo.opensocial.container.client.bean.GadgetPosition;
import org.nuxeo.opensocial.container.client.bean.PreferencesBean;
import org.nuxeo.opensocial.container.component.api.FactoryManager;
import org.nuxeo.opensocial.container.factory.api.ContainerManager;
import org.nuxeo.opensocial.container.factory.api.GadgetManager;
import org.nuxeo.opensocial.container.factory.mapping.GadgetMapper;
import org.nuxeo.opensocial.container.factory.utils.UrlBuilder;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.services.NuxeoServiceModule;
import org.nuxeo.opensocial.shindig.NuxeoPropertiesModule;
import org.nuxeo.opensocial.shindig.gadgets.NXGadgetSpecFactoryModule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.URLChecker;


@RunWith(NuxeoRunner.class)
public class GadgetManagerTest {

  private static final String GADGET_DEF_1 = "http://localhost:11111/gadgets/manager/bookmarks/bookmarks/bookmarks.xml";

  @Inject
  private CoreSession session = null;

  private SpaceManager spaceManager = null;
  private Space space1Space = null;
  private DomainAndSiteUtils domainUtils;

  private GadgetManager gadgetManager = null;
  private ContainerManager containerManager = null;
  private HashMap<String, String> gwtParams;
  private OpenSocialService openSocialService;

  @Inject
  public GadgetManagerTest(TestRuntimeHarness harness, WebEngine we) throws Exception {

    harness.deployBundle("org.nuxeo.opensocial.container");
    harness.deployBundle("org.nuxeo.opensocial.container.space.config");
    harness.deployBundle("org.nuxeo.ecm.spaces.api");
    harness.deployBundle("org.nuxeo.ecm.spaces.core");
    harness.deployBundle("org.nuxeo.opensocial.service");
    harness.deployBundle("org.nuxeo.opensocial.gadgets.core");
    harness.deployBundle("org.nuxeo.opensocial.webengine.gadgets");

    OSGiRuntimeService runtime = (OSGiRuntimeService) Framework.getRuntime();
    runtime.setProperty("gadgets.host", "localhost");
    runtime.setProperty("gadgets.port", "11111");
    runtime.setProperty("gadgets.path", "/gadgets/manager");

    spaceManager = Framework.getService(SpaceManager.class);

    harness.loadProperties(GadgetManagerTest.class.getClassLoader()
        .getResourceAsStream("shindig.properties"));

    openSocialService = Framework.getService(OpenSocialService.class);
    openSocialService.setInjector(Guice.createInjector(
        new NuxeoPropertiesModule(), new DefaultGuiceModule(),
        new NuxeoServiceModule(), new SocialApiGuiceModule(),
        new NXGadgetSpecFactoryModule(), new OAuthModule()));

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

    gwtParams = new HashMap<String, String>();



  }

  /**
   * Build a set of documents for testing
   *
   * @throws ClientException
   * @throws SpaceException
   */
  @Before
  public void reinitDatas() throws ClientException, SpaceException {
    assertNotNull(gadgetManager);
    domainUtils = new DomainAndSiteUtils(session, spaceManager);
    domainUtils.create();
    space1Space = domainUtils.getSpace1Space();
    gwtParams.put(ContainerManagerImpl.DOC_REF, space1Space.getId());
    assertNotNull(containerManager);
    Container container = containerManager.createContainer(gwtParams);
    assertNotNull(container);
    List<GadgetBean> gadgetBeans = container.getGadgets();
    assertNotNull(gadgetBeans);
    assertEquals(2, gadgetBeans.size());
  }

  @Test
  public void gadgetManagerServiceExists() throws Exception {
    assertNotNull(gadgetManager);
  }

  @Test
  public void testGadgetManager() throws Exception {
    FactoryManager service = Framework.getService(FactoryManager.class);
    GadgetManager gadgetFactory = service.getGadgetFactory();
    assertTrue(gadgetFactory instanceof GadgetManagerImpl);
  }

  @Test
  public void opensocialAnGadgetServiceExists() throws Exception {
    assertNotNull(openSocialService);
    assertNotNull(Framework.getService(GadgetService.class));
  }

  //@Test
  public void iCanGetAGadgetResource() throws Exception {
    GadgetService service = Framework.getService(GadgetService.class);
    GadgetDeclaration gadget = service.getGadget(DomainAndSiteUtils.GADGET_NAME_1);
    assertNotNull(gadget.getMountPoint(), "/bookmarks/bookmarks");

    URL url = new URL(GADGET_DEF_1);
    assertTrue(new URLChecker().checkUrlContentAndStatusOK(url));
  }

  //@Test
  public void iCanSavePrefs() throws Exception {

    Container container = containerManager.createContainer(gwtParams);
    List<GadgetBean> gadgetBeans = container.getGadgets();
    assertEquals(2, gadgetBeans.size());
    GadgetBean gadgetBean = gadgetBeans.get(0);
    assertEquals(gadgetBean.getTitle(), DomainAndSiteUtils.GADGET_TITLE_1);
    List<PreferencesBean> prefs = gadgetBean.getUserPrefs();
    assertEquals(prefs.size(), 3);
    PreferencesBean titlePref = prefs.get(0);
    PreferencesBean colorPref = prefs.get(1);
    PreferencesBean bookPref = prefs.get(2);
    assertEquals(titlePref.getName(), "title");
    assertEquals(colorPref.getName(), "pickr-bgcolor");
    assertEquals(titlePref.getValue(), null);
    assertEquals(colorPref.getValue(), null);
    assertEquals(bookPref.getName(), "bookmarks");
    assertEquals(bookPref.getValue(), null);

    Map<String, String> updatePrefs = new HashMap<String, String>();

    String titleSave = "My bookmarks";
    updatePrefs.put("title", titleSave);
    String colorSave = "red";
    updatePrefs.put("pickr-bgcolor", colorSave);
    String linkSave = "bookLinks";
    updatePrefs.put("bookmarks", linkSave);

    gadgetManager.savePreferences(gadgetBean, updatePrefs, gwtParams);

    // Test for asynchrone mode
    List<PreferencesBean> prefsSave = gadgetBean.getUserPrefs();
    assertEquals(prefsSave.size(), 3);

    assertEquals(prefsSave.get(0)
        .getValue(), titleSave);
    assertEquals(gadgetBean.getTitle(), titleSave);

    assertEquals(prefsSave.get(1)
        .getValue(), colorSave);
    assertEquals(prefsSave.get(2)
        .getValue(), linkSave);

    // Test persistance
    Container containerSave = containerManager.createContainer(gwtParams);
    List<GadgetBean> gadgetBeansSave = containerSave.getGadgets();
    GadgetBean gadgetBean1 = gadgetBeansSave.get(0);
    assertEquals(gadgetBean1.getTitle(), titleSave);
    List<PreferencesBean> prefsSave1 = gadgetBean1.getUserPrefs();
    assertEquals(prefsSave1.size(), 3);

    assertEquals(prefsSave1.get(0)
        .getValue(), titleSave);
    assertEquals(gadgetBean1.getTitle(), titleSave);

    assertEquals(prefsSave1.get(1)
        .getValue(), colorSave);
    assertEquals(prefsSave1.get(2)
        .getValue(), linkSave);
  }

  @Test
  public void iCanSaveCollapsed() throws Exception {
    Container container = containerManager.createContainer(gwtParams);
    List<GadgetBean> gadgetBeans = container.getGadgets();
    assertEquals(2, gadgetBeans.size());
    GadgetBean gadgetBean = gadgetBeans.get(0);
    assertEquals(gadgetBean.getTitle(), DomainAndSiteUtils.GADGET_TITLE_1);
    assertEquals(gadgetBean.isCollapse(), false);
    gadgetBean.setCollapse(true);
    gadgetManager.saveCollapsed(gadgetBean, gwtParams);

    // Test for asynchrone mode
    assertEquals(gadgetBean.isCollapse(), true);

    // Test persistance
    Container container1 = containerManager.createContainer(gwtParams);
    assertEquals(container1.getGadgets()
        .get(0)
        .isCollapse(), true);
  }

  @Test
  public void iCanSavePosition() throws Exception {
    Container container = containerManager.createContainer(gwtParams);
    List<GadgetBean> gadgetBeans = container.getGadgets();
    assertEquals(2, gadgetBeans.size());
    GadgetBean gadgetBean = gadgetBeans.get(0);
    assertEquals(gadgetBean.getTitle(), DomainAndSiteUtils.GADGET_TITLE_1);
    GadgetPosition pos = gadgetBean.getGadgetPosition();
    assertEquals(pos.getPlaceID(), DomainAndSiteUtils.GADGET_PLACE_1);
    assertEquals(pos.getPosition(), DomainAndSiteUtils.GADGET_POS);

    String placeID = "newPlace";
    Integer position = 2;
    gadgetBean.setPosition(new GadgetPosition(placeID, position));

    gadgetManager.savePosition(gadgetBean, gwtParams);

    // Test for asynchrone mode
    assertEquals(gadgetBean.getGadgetPosition()
        .getPlaceID(), placeID);
    assertEquals(gadgetBean.getGadgetPosition()
        .getPosition(), position);

    // Test persistance
    Container container1 = containerManager.createContainer(gwtParams);

    GadgetPosition pos2 = container1.getGadgets()
        .get(0)
        .getGadgetPosition();
    assertEquals(pos2.getPlaceID(), placeID);
    assertEquals(pos2.getPosition(), position);
  }

  @Test
  public void iCanRemoveGadget() throws Exception {
    Container container = containerManager.createContainer(gwtParams);
    List<GadgetBean> gadgetBeans = container.getGadgets();
    assertEquals(2, gadgetBeans.size());
    GadgetBean gadgetBean = gadgetBeans.get(0);

    gadgetManager.removeGadget(gadgetBean, gwtParams);
    assertEquals(1, containerManager.createContainer(gwtParams)
        .getGadgets()
        .size());

  }

  @Test
  public void iCanMergePreferences() {
    String pREF = "pref";
    String nEWVVAL = "newval";
    String fAKEVAL = "fakenewval";
    ArrayList<PreferencesBean> defaultPrefs = new ArrayList<PreferencesBean>();
    defaultPrefs.add(new PreferencesBean("dataType", "defaultValue",
        "displayName", null, pREF, "oldval"));
    defaultPrefs.add(new PreferencesBean("dataType", "defaultValue",
        "displayName", null, "pref2", "oldval2"));
    Map<String, String> loadPrefs = new HashMap<String, String>();
    loadPrefs.put(pREF, nEWVVAL);
    loadPrefs.put("fakepref", fAKEVAL);
    ArrayList<PreferencesBean> mergePreferences = PreferenceManager.mergePreferences(
        defaultPrefs, loadPrefs);

    assertEquals(mergePreferences.size(), 2);

    for (PreferencesBean pref : mergePreferences) {
      if (pref.getName()
          .equals(pREF))
        assertEquals(pref.getValue(), nEWVVAL);
      else
        assertFalse(pref.getValue()
            .equals(fAKEVAL));
    }
  }

  @Test
  public void openSocialServiceNotNull() {
    assertNotNull(openSocialService);
  }

  //@Test
  public void iCanGetDefaultPreferences() throws Exception {
    ContainerManagerImpl containerManager = new ContainerManagerImpl() {
      @Override
      protected CoreSession getCoreSession(Map<String, String> containerParams)
          throws ClientException {
        return session;
      }
    };
    DomainAndSiteUtils domainUtils = new DomainAndSiteUtils(session,
        Framework.getService(SpaceManager.class));
    domainUtils.create();
    Space space1Space = domainUtils.getSpace1Space();
    Map<String, String> gwtParams = new HashMap<String, String>();
    gwtParams.put(ContainerManagerImpl.DOC_REF, space1Space.getId());
    assertNotNull(containerManager);
    Container container = containerManager.createContainer(gwtParams);
    assertNotNull(container);
    GadgetBean gBookmark = container.getGadgets()
        .get(0);

    GadgetMapper mapper = new GadgetMapper(gBookmark);
    ArrayList<PreferencesBean> defaultPreferences = PreferenceManager.getDefaultPreferences(mapper);

    assertEquals(defaultPreferences.size(), 3);

    assertEquals(defaultPreferences.get(0)
        .getName(), "title");

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
