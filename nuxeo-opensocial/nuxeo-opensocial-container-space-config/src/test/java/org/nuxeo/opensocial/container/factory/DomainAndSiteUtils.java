package org.nuxeo.opensocial.container.factory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;

public class DomainAndSiteUtils {

  private CoreSession session;
  private Univers intralmUnivers;
  private Space space1Space;
  private SpaceManager spaceManager;

  public DomainAndSiteUtils(CoreSession session, SpaceManager spaceManager) {
    this.spaceManager = spaceManager;
    this.session = session;
  }

  public Space getSpace1Space() {
    return space1Space;
  }

  public Univers getIntralmUnivers() {
    return intralmUnivers;
  }

  /****************************************************************************
   * UTILITY METHODS
   */

  private static final String DEFAULT_DOMAIN = "default-domain";
  public static final String GADGET_NAME_1 = "bookmarks";
  public static final String GADGET_TITLE_1 = "g1 title";
  public static final String GADGET_NAME_2 = "meteo";
  public static final String GADGET_TITLE_2 = "g2 title";
  public static final String GADGET_PLACE_1 = "pl1";
  public static final Integer GADGET_POS = 10;

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
  public void create() throws ClientException, SpaceException {

    DocumentModel domain = createDocument(session.getRootDocument(),
        DEFAULT_DOMAIN, "Domain");
    DocumentModel workspaces = createDocument(domain, "workspaces",
        "WorkspaceRoot");
    createDocument(workspaces, "galaxy", "Workspace");

    intralmUnivers = createUnivers("intralm", "", "", null);
    createUnivers("intralm2", "d2", "univers title 2", null);
    space1Space = createSpace(intralmUnivers, "space1", "space 1 title",
        "space 1 description ", ContainerManagerImpl.DEFAULT_LAYOUT, "bob", "c1","them1");
    createSpace(intralmUnivers, "space2", "space 2 title",
        "space 2 description ", "space2Layout", "bob", "c2","them2");
    createGadget(space1Space, GADGET_NAME_1, GADGET_TITLE_1, "g2 desc", null,
        GADGET_PLACE_1, "url", GADGET_POS, "c1", "t1", false, "bob");
    Map<String, String> hashWith1Elt = new HashMap<String, String>();
    createGadget(space1Space, GADGET_NAME_2, GADGET_TITLE_2, "g2 desc",
        hashWith1Elt, "pl2", "ur2", 12, "c2", "t2", true, "bob");

  }

  private DocumentModel createDocument(DocumentModel parent, String id,
      String type) throws ClientException {
    DocumentModel doc = session.createDocumentModel(parent.getPathAsString(),
        id, type);
    doc = session.createDocument(doc);
    doc.setPropertyValue("dc:title", id);
    doc.setPropertyValue("dc:created", new Date());
    session.saveDocument(doc);
    session.save();
    return doc;
  }

  private Univers createUnivers(final String name, final String description,
      final String title, final String id) throws ClientException,
      SpaceException {
    final Univers dataUnivers = createUniversData(name, description, title, id);
    spaceManager.createUnivers(dataUnivers, session);
    return spaceManager.getUnivers(name, session);
  }

  private Univers createUniversData(final String name,
      final String description, final String title, final String id)
      throws ClientException, SpaceException {
    final Univers dataUnivers = new Univers() {

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
    };
    return dataUnivers;
  }

  private Space createSpace(Univers univers, final String name,
      final String title, final String desc, final String layout,
      final String owner, final String category, final String theme) throws ClientException,
      SpaceException {
    final Space spaceData = createSpaceData(name, title, desc, layout, null,
        owner, category,theme);
    spaceManager.createSpace(spaceData, univers, session);
    return spaceManager.getSpace(name, univers, session);
  }

  private static Space createSpaceData(final String name, final String title,
      final String desc, final String layout, final String id,
      final String owner, final String category,final String theme) throws ClientException,
      SpaceException {
    final Space spaceData = new Space() {

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

    };

    return spaceData;
  }

  private Gadget createGadget(Space space, final String name,
      final String title, final String description,
      final Map<String, String> preferences, final String placeID,
      final String url, final int position, final String category,
      final String type, final boolean collapsed, final String owner)
      throws ClientException, SpaceException {
    final Gadget gadgetData = createGadgetData(name, title, description,
        preferences, placeID, url, position, category, type, collapsed, null,
        owner);
    spaceManager.createGadget(gadgetData, space, session);
    return spaceManager.getGadget(name, space, session);
  }

  @SuppressWarnings("unused")
  private Gadget createGadget(Space space, Gadget gadgetData, String name)
      throws ClientException, SpaceException {
    spaceManager.createGadget(gadgetData, space, session);
    return spaceManager.getGadget(name, space, session);
  }

  private Gadget createGadgetData(final String name, final String title,
      final String description, final Map<String, String> preferences,
      final String placeID, final String url, final int position,
      final String category, final String type, final boolean collapsed,
      final String id, final String owner) throws ClientException,
      SpaceException {

    final Gadget gadgetData = new Gadget() {

      public String getDescription() {
        return description;
      }

      public String getId() {
        return id;
      }

      public String getName() {
        return name;
      }

      public String getOwner() {
        return owner;
      }

      public String getPlaceID() {
        return placeID;
      }

      public int getPosition() {
        return position;
      }

      public Map<String, String> getPreferences() {
        return preferences;
      }

      public String getTitle() {
        return title;
      }

      public boolean isCollapsed() {
        return collapsed;
      }

      public String getCategory() {
        return category;
      }

      public boolean isEqualTo(Gadget gadget) {
        // TODO Auto-generated method stub
        return false;
      }
    };

    return gadgetData;
  }

}
