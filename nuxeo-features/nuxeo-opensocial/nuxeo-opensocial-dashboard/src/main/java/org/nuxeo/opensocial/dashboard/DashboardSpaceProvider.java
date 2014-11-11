package org.nuxeo.opensocial.dashboard;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.core.contribs.impl.SingleDocSpaceProvider;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocSpaceImpl;
import org.nuxeo.runtime.api.Framework;

public class DashboardSpaceProvider extends SingleDocSpaceProvider {

  public static final String DASHBOARD_SPACE_NAME = "dashboardSpace";

  // Compat
  public static final String DASHBOARD_UNIVERSE_NAME = "dashboardUniverse";

  public static final String DASHBOARD_DEFAULT_LAYOUT = "x-2-default";

  private static final Log log = LogFactory.getLog(DashboardSpaceProvider.class);

  // we really would like to use COLS[0] from ContainerPortal here.
  // however, we can't because that would cause linkage with the
  // client
  // side code, which would imply that we can run in the browser,
  // and we can't... so we have to duplicate the constants here

  private static final String[] COLS = new String[] { "firstCol", "secondCol",
      "thirdCol", "fourCol" };

  @Override
  public Space doGetSpace(String name, CoreSession session)
      throws SpaceException {
    if (!name.equals(DASHBOARD_SPACE_NAME)) {
      throw new SpaceException("Only one space is supported by the "
          + "dashboard space provider!");
    }

    try {
      DocumentModel space = getOrCreateSpace(session);
      return space.getAdapter(Space.class);
    } catch (ClientException e) {
      log.error("Unable to create or get personal dashboard", e);
      return null;
    }

  }

  public static String getSpaceId(CoreSession session) throws ClientException {
    DocumentModel doc = getOrCreateSpace(session);
    return doc.getId();
  }

  protected static DocumentModel getUserPersonalWorkspace(CoreSession session) {

    try {
      UserWorkspaceService svc = Framework.getService(UserWorkspaceService.class);
      if (svc == null) {
        throw new SpaceException("Can't find the user workspace service!");
      }

      return svc.getCurrentUserPersonalWorkspace(session, null);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * For compat purpose, we put the space in a univers. But it is not really
   * needed
   * 
   * @param session
   * @return
   */
  protected static DocumentModel getOrCreateParentUnivers(CoreSession session)
      throws ClientException {
    DocumentModel pw = getUserPersonalWorkspace(session);
    if (pw == null) {
      throw new ClientException("Unable to get personal workspace");
    }

    PathRef universePath = new PathRef(
        getUserPersonalWorkspace(session).getPathAsString() + "/"
            + DASHBOARD_UNIVERSE_NAME);
    DocumentModel universeDoc;
    if (session.exists(universePath)) {
      universeDoc = session.getDocument(universePath);

    } else {
      universeDoc = session.createDocumentModel(getUserPersonalWorkspace(
          session).getPathAsString(), DASHBOARD_UNIVERSE_NAME, "HiddenFolder");
      universeDoc.setProperty("dc", "title", "nuxeo dashboard universe");
      universeDoc.setProperty("dc", "description", "parent of dashboard space");
      universeDoc = session.createDocument(universeDoc);
      session.saveDocument(universeDoc);
      session.save();
    }
    return universeDoc;
  }

  protected static DocumentModel getOrCreateSpace(CoreSession session)
      throws ClientException {
    DocumentModel parentUnivers = getOrCreateParentUnivers(session);
    PathRef spaceRef = new PathRef(parentUnivers.getPathAsString() + "/"
        + DASHBOARD_SPACE_NAME);

    if (session.exists(spaceRef)) {
      return session.getDocument(spaceRef);
    } else {
      DocumentModel model = session.createDocumentModel(
          getUserPersonalWorkspace(session).getPathAsString() + "/"
              + DASHBOARD_UNIVERSE_NAME, DASHBOARD_SPACE_NAME,
          DocSpaceImpl.TYPE);
      model.setProperty("dc", "title", "nuxeo dashboard space");
      model.setProperty("dc", "description", "dashboard space");
      Space desiredSpace = model.getAdapter(Space.class);
      desiredSpace.setLayout(DASHBOARD_DEFAULT_LAYOUT);
      model = session.createDocument(model);

      createInitialGadgets(session, model);
      session.save();
      return model;
    }

  }

  protected static void createInitialGadgets(CoreSession session,
      DocumentModel spaceDocument) throws ClientException {

    LocaleSelector localeSelector = (LocaleSelector) Component.getInstance("org.jboss.seam.international.localeSelector");
    String language = "en";
    if (localeSelector != null) {
      language = localeSelector.getLanguage();
    }
    String labelKey = "";
    String title = "";

    Space space = spaceDocument.getAdapter(Space.class);

    // UserTasks
    log.debug("creating UserTasks ");
    labelKey = "title.dashboard.userTasks";
    title = TranslationHelper.getLabel(labelKey, language);
    createGadgetForInitialDashboard(session, space, "tasks", title, COLS[0],
        new Integer(0));

    // waiting on others
    log.debug("creating waiting for ");
    labelKey = "title.dashboard.waitingfor";
    title = TranslationHelper.getLabel(labelKey, language);
    createGadgetForInitialDashboard(session, space, "waitingfor", title,
        COLS[0], new Integer(1));

    // User Sites
    log.debug("creating UserSites ");
    labelKey = "title.dashboard.userSites";
    title = TranslationHelper.getLabel(labelKey, language);
    createGadgetForInitialDashboard(session, space, "usersites", title,
        COLS[0], new Integer(2));

    // UserDocuments
    log.debug("creating UserDocuments ");
    labelKey = "title.dashboard.userDocuments";
    title = TranslationHelper.getLabel(labelKey, language);
    createGadgetForInitialDashboard(session, space, "userdocuments", title,
        COLS[1], new Integer(0));

    // User Workspaces
    log.debug("creating UserWorkspaces ");
    labelKey = "title.dashboard.userWorkspaces";
    title = TranslationHelper.getLabel(labelKey, language);
    createGadgetForInitialDashboard(session, space, "userworkspaces", title,
        COLS[1], new Integer(0));

    session.save();

  }

  protected static Gadget createGadgetForInitialDashboard(CoreSession session,
      Space parent, String name, String title, String placeId, Integer position)
      throws ClientException {

    Gadget gadget = parent.createGadget(name);
    gadget.setCategory(null);
    gadget.setCollapsed(Boolean.FALSE);
    gadget.setPlaceId(placeId);
    gadget.setPosition(position);
    gadget.setPreferences(new HashMap<String, String>());
    parent.save(gadget);

    log.debug("created a gadget: " + name + "," + gadget.getPlaceId());
    return gadget;

  }
}
