package org.nuxeo.opensocial.dashboard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceSecurityException;
import org.nuxeo.ecm.spaces.core.contribs.impl.DefaultSpaceProvider;
import org.nuxeo.ecm.spaces.core.impl.Constants;
import org.nuxeo.ecm.spaces.core.impl.DocumentHelper;
import org.nuxeo.ecm.spaces.core.impl.exceptions.NoElementFoundException;

public class DashboardSpaceProvider extends DefaultSpaceProvider {

    public static final String DASHBOARD_SPACE_NAME = "dashboardSpace";
    public static final String DASHBOARD_DEFAULT_LAYOUT = "x-2-default";

    private static final Log log = LogFactory.getLog(DashboardSpaceProvider.class);

    // we really would like to use COLS[0] from ContainerPortal here.
    // however, we can't because that would cause linkage with the
    // client
    // side code, which would imply that we can run in the browser,
    // and we can't... so we have to duplicate the constants here

    private static final String[] COLS = new String[] { "firstCol",
            "secondCol", "thirdCol", "fourCol" };

    @Override
    public Space create(Space data, Univers parent, CoreSession session)
            throws SpaceException {

        throw new SpaceException("Cannot create other spaces with the "
                + "dashboard space provider!");
    }

    @Override
    public Space getElement(String name, Univers parent, CoreSession session)
            throws NoElementFoundException, SpaceSecurityException {
        if (!name.equals(DASHBOARD_SPACE_NAME)) {
            throw new SpaceSecurityException(
                    "Only one space is supported by the "
                            + "dashboard space provider!");
        }
        try {
            IdRef universeRef = new IdRef(parent.getId());
            DocumentModel universeDoc = session.getDocument(universeRef);
            PathRef spaceRef = new PathRef(universeDoc.getPathAsString() + "/"
                    + DASHBOARD_SPACE_NAME);
            DocumentModel spaceDocument;
            if (session.exists(spaceRef)) {
                spaceDocument = session.getDocument(spaceRef);
            } else {
                DocumentModel model = session.createDocumentModel(
                        universeDoc.getPathAsString(), DASHBOARD_SPACE_NAME,
                        Constants.Space.TYPE);
                model.setProperty("dc", "title", "nuxeo dashboard space");
                model.setProperty("dc", "description", "dashboard space");
                Space desiredSpace = model.getAdapter(Space.class);
                spaceDocument = DocumentHelper.createInternalDocument(
                        universeDoc, desiredSpace.getName(),
                        desiredSpace.getTitle(), desiredSpace.getDescription(),
                        session, Constants.Space.TYPE);
                // set layout
                spaceDocument.setProperty("space", "layout", DASHBOARD_DEFAULT_LAYOUT);
                spaceDocument = session.saveDocument(spaceDocument);
                session.save();
                createInitialGadgets(session, spaceDocument);
                return spaceDocument.getAdapter(Space.class);
            }
            return spaceDocument.getAdapter(Space.class);
        } catch (Exception e) {
            // really not ideal... Framework.getService can throw exception
            throw new SpaceSecurityException(e);
        }
    }

    protected void createInitialGadgets(CoreSession session,
            DocumentModel spaceDocument) throws Exception {

        LocaleSelector localeSelector = (LocaleSelector) Component.getInstance("org.jboss.seam.international.localeSelector");
        String language = "en";
        if (localeSelector!=null) {
            language = localeSelector.getLanguage();
        }
        String labelKey = "";
        String title = "";

        // UserTasks
        log.debug("creating UserTasks ");
        labelKey = "title.dashboard.userTasks";
        title = TranslationHelper.getLabel(labelKey, language);
        createGadgetForInitialDashboard(session, spaceDocument,
                "tasks", title, COLS[0], new Integer(0));

        /*
        log.debug("creating UserWorkflow ");
        labelKey = "title.dashboard.userProcesses";
        title = TranslationHelper.getLabel(labelKey, language);
        createGadgetForInitialDashboard(session, spaceDocument,
                "tasks", title, COLS[0], new Integer(0));
                */

        // User Sites
        log.debug("creating UserSites ");
        labelKey = "title.dashboard.userSites";
        title = TranslationHelper.getLabel(labelKey, language);
        createGadgetForInitialDashboard(session, spaceDocument,
                "usersites",title,  COLS[0], new Integer(0));

        // UserDocuments
        log.debug("creating UserDocuments ");
        labelKey = "title.dashboard.userDocuments";
        title = TranslationHelper.getLabel(labelKey, language);
        createGadgetForInitialDashboard(session, spaceDocument,
                "userdocuments", title, COLS[1], new Integer(0));

        // User Workspaces
        log.debug("creating UserWorkspaces ");
        labelKey = "title.dashboard.userWorkspaces";
        title = TranslationHelper.getLabel(labelKey, language);
        createGadgetForInitialDashboard(session, spaceDocument,
                "userworkspaces",title,  COLS[1], new Integer(0));

        session.save();


    }

    protected Gadget createGadgetForInitialDashboard(CoreSession session,
            DocumentModel parent, String name, String title, String placeId, Integer position)
            throws Exception {

        DocumentModel model = DocumentHelper.createInternalDocument(parent,
                name, title, "", session, Constants.Gadget.TYPE);

        // TODO: Why is this null not something sensible?
        model.setPropertyValue(Constants.Gadget.GADGET_CATEGORY, null);
        model.setPropertyValue(Constants.Gadget.GADGET_COLLAPSED, Boolean.FALSE);
        model.setPropertyValue(Constants.Gadget.GADGET_PLACEID, placeId);
        model.setPropertyValue(Constants.Gadget.GADGET_POSITION, position);
        model.setPropertyValue(Constants.Gadget.GADGET_PREFERENCES,
                new String[0]);
        Gadget g = model.getAdapter(Gadget.class);

        log.info("trying to create a gadget:" + name + "," + g.getPlaceID());

        model = session.saveDocument(model);
        session.save();

        log.info("created a gadget...:" + name + ","
                + model.getProperty(Constants.Gadget.GADGET_PLACEID));
        return model.getAdapter(Gadget.class);

    }
}
