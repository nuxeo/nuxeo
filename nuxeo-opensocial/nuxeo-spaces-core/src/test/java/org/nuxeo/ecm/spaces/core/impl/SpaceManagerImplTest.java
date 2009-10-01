package org.nuxeo.ecm.spaces.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.GadgetNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.ecm.spaces.api.exceptions.UniversNotFoundException;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.UniversDocumentWrapper;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRepositoryHandler;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Unit test classes concerning class SpaceManagerImpl . This class is treated
 * like a framework service implementation of the interface SpaceManager
 *
 * @author 10044893
 *
 */
@RunWith(NuxeoRunner.class)
public class SpaceManagerImplTest {

  private static final class GadgetImplementation implements Gadget {

    public Map<String, String> getPreferences() {
      return preferences;
    }
    public String getDescription() {
      return description;
    }
    public String getUrl() {
      return url;
    }
    public String getTitle() {
      return title;
    }
    public String getId() {
      return id;
    }
    public String getCategory() {
      return category;
    }
    public int getPosition() {
      return position;
    }
    public String getName() {
      return name;
    }
    public String getOwner() {
      return owner;
    }
    public boolean isCollapsed() {
      return collapsed;
    }
    public String getPlaceID() {
      return placeID;
    }
    private final Map<String, String> preferences;
    private final String description;
    private final String url;
    private final String title;
    private final String id;
    private final String category;
    private final int position;
    private final String name;
    private final String owner;
    private final boolean collapsed;
    private final String placeID;
    public GadgetImplementation(Map<String, String> preferences,
        String description,  String url, String title, String id,
        String category, int position, String name, String owner,
        boolean collapsed, String placeID) {
          this.preferences = preferences;
          this.description = description;
          this.url = url;
          this.title = title;
          this.id = id;
          this.category = category;
          this.position = position;
          this.name = name;
          this.owner = owner;
          this.collapsed = collapsed;
          this.placeID = placeID;

    }
    public boolean isEqualTo(Gadget gadget) {
      return false;
    }



  }


  private static final class UniversImplementation implements Univers {
    private final String name;
    private final String title;
    private final String description;
    private final String id;

    private UniversImplementation(String name, String title,
        String description, String id) {
      this.name = name;
      this.title = title;
      this.description = description;
      this.id = id;
    }

    public String getDescription() {
      return description;
    }

    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getTitle() {
      return title;
    }

    public boolean isEqualTo(Univers univers) {
      return false;
    }
  }





  private static final String DEFAULT_DOMAIN = "default-domain";

  private static SpaceManager service = null;


  @Inject
  private CoreSession session;

  private static DocumentModel myUniversRoot = null;

  private static Univers intralmUnivers = null;
  private static Space space1Space = null;

  @Inject
  public SpaceManagerImplTest(TestRuntimeHarness harness) throws Exception {

    harness.deployBundle("org.nuxeo.ecm.spaces.core");

    // disable user Univers contrib
    harness.deployContrib(
        "com.leroymerlin.corp.fr.nuxeo.portal.spaces.core.test",
        "OSGI-INF/tests-space-config.xml");

    service = Framework.getService(SpaceManager.class);


  }

  /**
   * Build a set of documents for testing
   *
   * @throws ClientException
   * @throws SpaceException
   */
  @Before
  public void reinitDatas() throws ClientException, SpaceException {
    createDomainAndSite(session);
  }

  /**
   * Creation of a tree of documents in the repository
   * /default-domain/workspaces/galaxy /default-domain/workspaces/galaxy/intralm
   * [Univers] /default-domain/workspaces/galaxy/intralm2 [Univers]
   * /default-domain/workspaces/galaxy/intralm/space1 [Space]
   * /default-domain/workspaces/galaxy/intralm/space1/g1 [Gadget]
   * /default-domain/workspaces/galaxy/intralm/space1/g2 [Gadget]
   * /default-domain/workspaces/galaxy/intralm/space2 [Space]
   *
   * @throws SpaceException
   */
  private static void createDomainAndSite(CoreSession session) throws ClientException,
      SpaceException {

    DocumentModel domain = createDocument(session.getRootDocument(),
        DEFAULT_DOMAIN, "Domain", session);
    DocumentModel workspaces = createDocument(domain, "workspaces",
        "WorkspaceRoot", session);
    myUniversRoot = createDocument(workspaces, "galaxy", "Workspace", session);

    intralmUnivers = createUnivers("intralm", "", "", null, session);
    createUnivers("intralm2", "d2", "univers title 2", null, session);
    space1Space = createSpace(intralmUnivers, "space1", "space 1 title",
        "space 1 description ", "space1Layout", "s1","t1", session);
    createSpace(intralmUnivers, "space2", "space 2 title",
        "space 2 description ", "space2Layout", "s2","t2", session);
    createGadget(space1Space, "g1", "g1 title", "g2 desc", null, "pl1", "url",
        10, "c1",  false, session);
    Map<String, String> hashWith1Elt = new HashMap<String, String>();
    hashWith1Elt.put("k1", "v1");
    createGadget(space1Space, "g2", "g2 title", "g2 desc", hashWith1Elt, "pl2",
        "ur2", 12, "c2",  true, session);

  }

  /**
   * Check that the service exists and moreover it is an instance of
   * SpaceManagerImpl
   *
   * @throws Exception
   */
  @Test
  public void theServiceExists() throws Exception {
    assertNotNull(service);
    assertTrue(service instanceof SpaceManagerImpl);

  }

  /**
   * The method getUnivers(String name,...) throws an exception when the passed
   * 'name' parameter does not corresponds to any univers
   *
   * @throws Exception
   */
  @Test(expected = UniversNotFoundException.class)
  public void methodGetUniversThrowsAnExceptionWithAnUnknownNameParameter()
      throws SpaceException {
    service.getUnivers("nonExistingUniversName", session);
    fail("Should have thrown a client exception");
  }

  /**
   * The method getSpace(String name,...) throws an exception when the passed
   * 'name' parameter does not corresponds to any space
   *
   * @throws ClientException
   */
  @Test(expected = SpaceNotFoundException.class)
  public void methodGetSpaceThrowsAnExceptionWithAnUnknownNameParameter()
      throws SpaceException {
    service.getSpace("nonExistingSpaceName", intralmUnivers, session);
    fail("Should have thrown a client exception");
  }

  /**
   * The method getGadget(String name,...) throws an exception when the passed
   * 'name' parameter does not corresponds to any univers
   */
  @Test(expected = GadgetNotFoundException.class)
  public void methodGetGadgetThrowsAnExceptionWithAnUnknownNameParameter()
      throws Exception {
    service.getGadget("nonExistingGadgetName", space1Space, session);
    fail("Should have thrown a client exception");
  }

  /**
   * Simple usage of the method getUnivers : You supply the name of the univers
   * that you want and you get it !! Incredible
   *
   * @throws Exception
   */
  @Test
  public void methodGetUniversWorksFine() throws Exception {
    Univers intralm = service.getUnivers("intralm", session);
    assertNotNull(intralm);
    assertEquals("intralm", intralm.getName());
  }

  /**
   * Simple usage of the method getGadget : You supply the name of the gadget
   * that you want , plus the parent space , and you get your gadget !!
   * Incredible
   *
   * @throws Exception
   */
  @Test
  public void methodGetGadgetWorksFine() throws Exception {
    Gadget intralm = service.getGadget("g2", space1Space, session);
    assertNotNull(intralm);
    assertEquals("g2", intralm.getName());
  }

  /**
   * Simple usage of the method getUniversList
   *
   * @throws Exception
   */
  @Test
  public void methodGetUniversListWorksFine() throws Exception {

    // intralmUnivers = createUnivers("intralm", "", "", null);
    // createUnivers("intralm2", "d2", "univers title 2", null);

    Sorter nameSorter= new Sorter(){

      public int compare(DocumentModel o1, DocumentModel o2) {
        return o1.getName().compareTo(o2.getName());
      }};
    // nuxeo-core way
    DocumentModelList coreList = session.getChildren(myUniversRoot.getRef(),"Univers",null,nameSorter);
   // assertEquals(2, coreList.size());

    // nuxeo-spaces api way
    List<Univers> apiList = service.getUniversList(session);
    Comparator nameComparator=new Comparator(){

      public int compare(Object o1, Object o2) {
        Univers u1 = (Univers)o1;
        Univers u2 = (Univers)o2;
        return ((UniversDocumentWrapper)u1).getName().compareTo(((UniversDocumentWrapper)u2).getName());
      }};
    Collections.sort(apiList, nameComparator);
    assertEquals(2, apiList.size());

    // the way for getting an universes from a document model is
    DocumentModel firsDocumentModel = coreList.get(0);
    DocumentModel secondDocumentModel = coreList.get(1);
    Univers uni = firsDocumentModel.getAdapter(Univers.class);
    Univers uni2 = secondDocumentModel.getAdapter(Univers.class);
    Univers apiUni = apiList.get(0);
    Univers apiUni2 = apiList.get(1);

    assertTrue(uni.isEqualTo(apiUni));
    assertEquals(uni.getId(), apiUni.getId());
    assertEquals(uni.getName(), apiUni.getName());
    assertEquals(uni.getDescription(), apiUni.getDescription());
    assertEquals(uni.getDescription(), "");
    assertEquals("intralm", apiUni.getName());

    assertEquals(uni2.getId(), apiUni2.getId());
    assertEquals(uni2.getTitle(), "univers title 2");
    assertEquals(uni2.getTitle(), apiUni2.getTitle());
    assertEquals(uni2.getDescription(), apiUni2.getDescription());
    assertEquals(uni2.getName(), apiUni2.getName());

  }

  /**
   * Simple usage of the method getSpacesForUnivers
   *
   * @throws Exception
   */
  @Test
  public void methodGetSpacesForUniversWorksFine() throws Exception {

    // space1Space = createSpace(intralmUnivers, "space1","space 1 title",
    // "space 1 description ", "space1Layout","s1");
    // createSpace(intralmUnivers, "space2",
    // "space 2 title","space 2 description ", "space2Layout","s2");

    Univers intralm = service.getUnivers("intralm", session);
    List<Space> spaces = service.getSpacesForUnivers(intralm, session);

    assertNotNull(spaces);
    assertEquals(2, spaces.size());

    assertEquals(spaces.get(0)
        .getTitle(), "space 1 title");
    assertEquals(spaces.get(0)
        .getDescription(), "space 1 description ");
    assertEquals(spaces.get(0)
        .getLayout(), "space1Layout");
    assertEquals(spaces.get(0)
        .getCategory(), "s1");
    assertEquals(spaces.get(1)
        .getTitle(), "space 2 title");
    assertEquals(spaces.get(1)
        .getDescription(), "space 2 description ");
    assertEquals(spaces.get(1)
        .getLayout(), "space2Layout");
    assertEquals(spaces.get(1)
        .getCategory(), "s2");

  }

  /**
   * Simple usage of the method getGadgetsForSpace
   *
   * @throws Exception
   */
  @Test
  public void methodGetGadgetsForSpaceWorksFine() throws Exception {

    // createGadget(space1Space,
    // "g1","g1 title","g2 desc",null,"pl1","url",10,"c1","t1",false);
    // createGadget(space1Space,
    // "g2","g2 title","g2 desc",hashWith1Elt,"pl2","ur2",12,"c2","t2",true);

    Univers intralm = service.getUnivers("intralm", session);
    Space space1 = service.getSpacesForUnivers(intralm, session)
        .get(0);
    List<Gadget> gadgets = service.getGadgetsForSpace(space1, session);
    assertNotNull(gadgets);
    assertEquals(2, gadgets.size());

    Gadget g1 = gadgets.get(0);
    Gadget g2 = gadgets.get(1);
    assertEquals(g1.getCategory(), "c1");
    assertEquals(g1.getName(), "g1");
    assertEquals(g1.getPlaceID(), "pl1");
    assertEquals(g1.getPosition(), 10);
    //assertEquals(g1.getType(), "t1");
    assertEquals(g1.getTitle(), "g1 title");
    assertTrue(g1.getPreferences() == null || g1.getPreferences()
        .size() == 0);

    assertEquals(g2.getCategory(), "c2");
    assertEquals(g2.getName(), "g2");
    assertEquals(g2.getPlaceID(), "pl2");
    assertEquals(g2.getPosition(), 12);
    //assertEquals(g2.getType(), "t2");
    assertEquals(g2.getTitle(), "g2 title");
    assertNotNull(g2.getPreferences());
    assertEquals(g2.getPreferences()
        .size(), 1);
    assertEquals(g2.getPreferences()
        .get("k1"), "v1");

  }

  /**
   * Simple usage of the method createUnivers
   *
   * @throws Exception
   */
  @Test
  public void methodCreateUniversWorksFine() throws Exception {

    Univers univers = createUniversData("newunivers", "une description",
        "un titre", null);
    assertNotNull(service.createUnivers(univers, session));

    List<Univers> allUnivers = service.getUniversList(session);
    assertEquals(3, allUnivers.size());

  }

  /**
   * Simple usage of the method deleteUnivers
   *
   * @throws Exception
   */
  @Test
  public void methodDeleteUniversWorksFine() throws Exception {

    List<Univers> allUnivers = service.getUniversList(session);
    assertEquals(2, allUnivers.size());

    service.deleteUnivers(allUnivers.get(1), session);

    allUnivers = service.getUniversList(session);
    assertEquals(1, allUnivers.size());

  }

  /**
   * Simple usage of the method updateUnivers
   *
   * @throws Exception
   */
  @Test
  public void methodUpdateUniversWorksFine() throws Exception {

    List<Univers> allUnivers = service.getUniversList(session);
    assertEquals(2, allUnivers.size());

    Univers intralm = allUnivers.get(0);
    String oldName = intralm.getName();
    String aName = "newunivers";
    String aDesc = "une description";
    String aTitle = "un titre";

    // assertTrue(spaceManager.createUnivers(univers, session));

    // valeur de remplacement
    Univers universModified = createUniversData(aName,
        aDesc, aTitle, intralm.getId());
     Univers res = service.updateUnivers(universModified, session);
    assertNotNull(res);

    allUnivers = service.getUniversList(session);
    // on en a toujours 2
    assertEquals(2, allUnivers.size());
    // newUnivers n existe plus
    try {
      service.getUnivers(oldName, session);
      fail("Une exception aurait du etre levee");
    } catch (UniversNotFoundException ce) {
    }
    // le nouvel Univers existe
    Univers newUniversModif = service.getUnivers(aName, session);
    assertNotNull(newUniversModif);
    assertEquals(aName, newUniversModif.getName());
    assertEquals(aDesc, newUniversModif.getDescription());
    assertEquals(aTitle, newUniversModif.getTitle());

  }

  /**
   * Simple usage of the method updateGadget
   *
   * @throws Exception
   */
  @Test
  public void methodUpdateGadgetWorksFine() throws Exception {

    List<Gadget> allGadgets = service.getGadgetsForSpace(space1Space, session);
    assertEquals(2, allGadgets.size());

    String aGadgetName = "newgadget";
    String aGadgetDesc = "une description";
    String aGadgetTitle = "un titre";
    String aGadgetPlaceId = "placeId1";
    String aGadgetUrl = "aGadgetUrl";
    String aGadgetCategory = "aGadgetCat";
    //String aGadgetType = "aGadgetType";
    boolean aGadgetCollapsed = false;
    Map<String, String> aMapPreferences = new HashMap<String, String>();
    aMapPreferences.put("key1", "val1");
    aMapPreferences.put("key2", "val2");
    int aGadgetPosition = 1;

    String bGadgetName = "newgadgetb";
    String bGadgetDesc = "une description2";
    String bGadgetTitle = "un titre2";
    String bGadgetPlaceId = "placeId12";
    String bGadgetUrl = "aGadgetUrl2";
    String bGadgetCat = "aGadgetCat2";
   // String bGadgetType = "aGadgetType2";
    boolean bGadgetCollapsed = true;
    Map<String, String> bMapPreferences = new HashMap<String, String>();
    bMapPreferences.put("key1", "val11");
    bMapPreferences.put("key2", "val22");
    bMapPreferences.put("key3", "val33");
    int bGadgetPosition = 2;

    Gadget gadgetData = createGadgetData(aGadgetName, aGadgetTitle,
        aGadgetDesc, aMapPreferences, aGadgetPlaceId, aGadgetUrl,
        aGadgetPosition, aGadgetCategory,
        aGadgetCollapsed, null, session.getPrincipal()
            .getName());
    Gadget createdGadget = createGadget(space1Space, gadgetData, aGadgetName, session);
    allGadgets = service.getGadgetsForSpace(space1Space, session);
    assertEquals(3, allGadgets.size());

    // valeur de remplacement
    Gadget modifiedGadget = createGadgetData(bGadgetName, bGadgetTitle,
        bGadgetDesc, bMapPreferences, bGadgetPlaceId, bGadgetUrl,
        bGadgetPosition, bGadgetCat,
        bGadgetCollapsed, createdGadget.getId(), session.getPrincipal()
            .getName());
    Gadget res = service.updateGadget(modifiedGadget, session);
    assertNotNull(res);

    Gadget gadgetFromId = service.getGadgetFromId(modifiedGadget.getId(),
        session);

    Gadget gadgetFromName = service.getGadget(modifiedGadget.getName(),
        space1Space, session);

    // controle sur gadgetFromId
    assertEquals(gadgetFromId.getCategory(), bGadgetCat);
    assertEquals(gadgetFromId.getDescription(), bGadgetDesc);
    assertEquals(gadgetFromId.getPlaceID(), bGadgetPlaceId);
    assertEquals(gadgetFromId.getPosition(), bGadgetPosition);
    assertEquals(gadgetFromId.getPreferences()
        .size(), 3);
    assertEquals(gadgetFromId.getPreferences()
        .get("key1"), "val11");
    assertEquals(gadgetFromId.getPreferences()
        .get("key2"), "val22");
    assertEquals(gadgetFromId.getPreferences()
        .get("key3"), "val33");
    assertEquals(gadgetFromId.getTitle(), bGadgetTitle);
    //assertEquals(gadgetFromId.getType(), bGadgetType);

    // controle sur gadgetFromName
    assertEquals(gadgetFromName.getCategory(), bGadgetCat);
    assertEquals(gadgetFromName.getDescription(), bGadgetDesc);
    assertEquals(gadgetFromName.getPlaceID(), bGadgetPlaceId);
    assertEquals(gadgetFromName.getPosition(), bGadgetPosition);
    assertEquals(gadgetFromName.getPreferences()
        .size(), 3);
    assertEquals(gadgetFromName.getPreferences()
        .get("key1"), "val11");
    assertEquals(gadgetFromName.getPreferences()
        .get("key2"), "val22");
    assertEquals(gadgetFromName.getPreferences()
        .get("key3"), "val33");
    assertEquals(gadgetFromName.getTitle(), bGadgetTitle);
    //assertEquals(gadgetFromName.getType(), bGadgetType);

  }

  /**
   * Simple usage of the method updateSpace
   *
   * @throws Exception
   */
  @Test
  public void methodUpdateSpaceWorksFine() throws Exception {

    List<Space> allSpaces = service.getSpacesForUnivers(intralmUnivers, session);
    assertEquals(2, allSpaces.size());

    // we re gonna replace the first one
    Space spaceOne = allSpaces.get(0);

    String aSpaceName = "newspace";
    String aSpaceTitle = "un titre";
    String aSpaceDesc = "une description";
    String aSpaceLayout = "aLayout";
    String aSpaceCat = "a_cat";
    String aSpaceTheme = "a_Theme";

    Space newSpaceData = createSpaceData(aSpaceName, aSpaceTitle,
        aSpaceDesc, aSpaceLayout, spaceOne.getId(),
        session.getPrincipal()
            .getName(), aSpaceCat,aSpaceTheme);

     Space res = service.updateSpace(newSpaceData, session);
     assertNotNull(res);

    allSpaces = service.getSpacesForUnivers(intralmUnivers, session);

    // always 2
    assertEquals(2, allSpaces.size());

    Space newSpaceModif = allSpaces.get(1);
    assertNotNull(newSpaceModif);
    assertEquals(aSpaceName, newSpaceModif.getName());
    assertEquals(aSpaceDesc, newSpaceModif.getDescription());
    assertEquals(aSpaceTitle, newSpaceModif.getTitle());
    assertEquals(aSpaceLayout, newSpaceModif.getLayout());
    assertEquals(aSpaceCat, newSpaceModif.getCategory());
    assertEquals(aSpaceTheme, newSpaceModif.getTheme());

  }

  /**
   * Simple usage of the method createSpace
   *
   * @throws Exception
   */
  @Test
  public void methodCreateSpaceWorksFine() throws Exception {

    List<Space> allSpaces = service.getSpacesForUnivers(intralmUnivers, session);
    assertEquals(2, allSpaces.size());

    Space myNewSpace = createSpaceData("mySpace", "mySpaceTitle",
        "mySpaceDesc", "myNewSpaceLayout", null, session.getPrincipal()
            .getName(), "c1","theme1");
    assertNotNull(service.createSpace(myNewSpace, intralmUnivers, session));
    session.save();

    List<Space> spacesByService = service.getSpacesForUnivers(intralmUnivers,
        session);
    assertEquals(3, spacesByService.size());
    assertEquals("space1Layout", spacesByService.get(0)
        .getLayout());
    assertEquals("space2Layout", spacesByService.get(1)
        .getLayout());
    assertEquals(myNewSpace.getTitle(), spacesByService.get(2)
        .getTitle());
    assertEquals(myNewSpace.getLayout(), spacesByService.get(2)
        .getLayout());
    assertEquals(myNewSpace.getCategory(), spacesByService.get(2)
        .getCategory());
    assertEquals("c1", spacesByService.get(2)
        .getCategory());
    assertEquals("theme1", spacesByService.get(2)
        .getTheme());

  }

  /**
   * Simple usage of the method deleteSpace
   *
   * @throws Exception
   */
  @Test
  public void methodDeleteSpaceWorksFine() throws Exception {

    List<Space> allSpaces = service.getSpacesForUnivers(intralmUnivers, session);
    assertEquals(2, allSpaces.size());

     service.deleteSpace(allSpaces.get(1), session);

    allSpaces = service.getSpacesForUnivers(intralmUnivers, session);
    assertEquals(1, allSpaces.size());

  }

  /**
   * Simple usage of the method createGadget
   *
   * @throws Exception
   */
  @Test
  public void methodCreateGadgetWorksFine() throws Exception {

    List<Gadget> allGadgets = service.getGadgetsForSpace(space1Space, session);

    assertEquals(2, allGadgets.size());

    String aGadgetName = "newgadget";
    String aGadgetDesc = "une description";
    String aGadgetTitle = "un titre";
    String aGadgetPlaceId = "placeId1";
    String aGadgetUrl = "aGadgetUrl";
    String aGadgetCat = "aGadgetCat";
    //String aGadgetType = "aGadgetType";
    boolean aGadgetCollapsed = false;
    Map<String, String> aPrefs = new HashMap<String, String>();
    aPrefs.put("key1", "val1");
    aPrefs.put("key2", "val2");
    int aGadgetPos = 1;

    Gadget gadgetData = createGadgetData(aGadgetName, aGadgetTitle,
        aGadgetDesc, aPrefs, aGadgetPlaceId, aGadgetUrl,
        aGadgetPos, aGadgetCat,
        aGadgetCollapsed, null, session.getPrincipal()
            .getName());
    assertTrue(service.createGadget(gadgetData, space1Space, session) != null);

    allGadgets = service.getGadgetsForSpace(space1Space, session);
    assertEquals(3, allGadgets.size());

  }

  /**
   * Simple usage of the method deleteGadget
   *
   * @throws Exception
   */
  @Test
  public void methodDeleteGadgetWorksFine() throws Exception {

    List<Gadget> allGadgets = service.getGadgetsForSpace(space1Space, session);
    assertEquals(2, allGadgets.size());

    // on en supprime 1 => il doit en rester 1

    service.deleteGadget(allGadgets.get(0), session);

    allGadgets = service.getGadgetsForSpace(space1Space, session);
    assertEquals(1, allGadgets.size());

  }

  @Test
  public void methodCreateAndDeleteGadget() throws Exception {

    List<Gadget> allGadgets = service.getGadgetsForSpace(space1Space, session);

    assertEquals(2, allGadgets.size());

    String aGadgetName = "newgadget";
    String aGadgetDesc = "une description";
    String aGadgetTitle = "un titre";
    String aGadgetPlaceId = "placeId1";
    String aGadgetUrl = "aGadgetUrl";
    String aGadgetCat = "aGadgetCat";
    //String aType = "aGadgetType";
    boolean aCollapsed = false;
    Map<String, String> aPref = new HashMap<String, String>();
    aPref.put("key1", "val1");
    aPref.put("key2", "val2");
    int aPos = 1;

    Gadget gadgetData = createGadgetData(aGadgetName, aGadgetTitle,
        aGadgetDesc, aPref, aGadgetPlaceId, aGadgetUrl,
        aPos, aGadgetCat,
        aCollapsed, null, session.getPrincipal()
            .getName());
    assertTrue(service.createGadget(gadgetData, space1Space, session) != null);

    allGadgets = service.getGadgetsForSpace(space1Space, session);
    assertEquals(3, allGadgets.size());

    session.cancel();
    TestRepositoryHandler repository = new TestRepositoryHandler("test");
    CoreSession session2 = repository.openSessionAs("Administrator");
    assertTrue(session.getSessionId() != session2.getSessionId());

    List<Gadget> gadgets = service.getGadgetsForSpace(space1Space, session2);
    assertEquals(3, gadgets.size());

    // on en supprime 1 => il doit en rester 2

    service.deleteGadget(gadgets.get(0), session2);

    gadgets = service.getGadgetsForSpace(space1Space, session2);
    assertEquals(2, gadgets.size());

  }

  /**
   * UTILITY METHODS
   */

  private static DocumentModel createDocument(DocumentModel parent, String id,
      String type, CoreSession session) throws ClientException {
    DocumentModel doc = session.createDocumentModel(parent.getPathAsString(),
        id, type);
    doc = session.createDocument(doc);
    doc.setPropertyValue("dc:title", id);
    doc.setPropertyValue("dc:created", new Date());
    session.saveDocument(doc);
    session.save();
    return doc;
  }

  private static Univers createUnivers(final String name,
      final String description, final String title, final String id, CoreSession session)
      throws ClientException, SpaceException {
    final Univers dataUnivers = createUniversData(name, description, title, id);
    return service.createUnivers(dataUnivers, session);
  }

  private static Univers createUniversData(final String name,
      final String description, final String title, final String id)
      throws ClientException, SpaceException {
    final Univers dataUnivers = new UniversImplementation(name, title, description, id);
    return dataUnivers;
  }

  private static Space createSpace(Univers univers, final String name,
      final String title, final String desc, final String layout,
      final String category,String theme, CoreSession session) throws ClientException, SpaceException {
    final Space spaceData = createSpaceData(name, title, desc, layout, null,
        session.getPrincipal()
            .getName(), category,theme);
    service.createSpace(spaceData, univers, session);
    return service.getSpace(name, univers, session);
  }

  private static Space createSpaceData(final String name, final String title,
      final String desc, final String layout, final String id,
      final String owner, final String category,String theme) throws ClientException,
      SpaceException {
    final Space spaceData = new SpaceImplementation(category, layout, owner, name, title, desc, id,theme);

    return spaceData;
  }

  private static Gadget createGadget(Space space, final String name,
      final String title, final String description, final Map<String,String> preferences,
      final String placeID, final String url, final int position,
      final String category, final boolean collapsed, CoreSession session)
      throws ClientException, SpaceException {
    final Gadget gadgetData = createGadgetData(name, title, description,
        preferences, placeID, url, position, category,  collapsed, null,
        session.getPrincipal()
            .getName());
    service.createGadget(gadgetData, space, session);
    return service.getGadget(name, space, session);
  }

  private static Gadget createGadget(Space space, Gadget gadgetData, String name, CoreSession session)
      throws ClientException, SpaceException {
    service.createGadget(gadgetData, space, session);
    return service.getGadget(name, space, session);
  }

  private static Gadget createGadgetData(final String name, final String title,
      final String description, final Map<String,String> preferences, final String placeID,
      final String url, final int position, final String category,
       final boolean collapsed, final String id,
      final String owner) throws ClientException, SpaceException {
    final Gadget gadgetData = new GadgetImplementation(preferences, description, url, title, id,
        category, position, name, owner, collapsed, placeID);
    return gadgetData;
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


private static final class SpaceImplementation implements Space {
  private final String category;
  private final String layout;
  private final String owner;
  private final String name;
  private final String title;
  private final String desc;
  private final String id;
  private final String theme;

  private SpaceImplementation(String category, String layout, String owner,
      String name, String title, String desc, String id,String theme) {
    this.category = category;
    this.layout = layout;
    this.owner = owner;
    this.name = name;
    this.title = title;
    this.desc = desc;
    this.id = id;
    this.theme  = theme;
  }

  public String getDescription() {
    return desc;
  }

  public String getId() {
    return id;
  }

  public String getLayout() {
    return layout;
  }

  public String getName() {
    return name;
  }

  public String getTitle() {
    return title;
  }

  public String getOwner() {
    return owner;
  }

  public String getCategory() {
    return category;
  }

  public boolean isEqualTo(Space space) {
    return false;
  }

  public String getTheme() {
    return theme;
  }


}
}
