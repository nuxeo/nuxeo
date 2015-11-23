package org.nuxeo.ecm.user.center.dashboard;

import static org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate.YUI_ZT_50_50;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.management.storage.DocumentStoreManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.helper.WebContentHelper;
import org.nuxeo.opensocial.container.shared.layout.api.LayoutHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Common methods used for creating dashboard space in unrestricted sessions.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public abstract class AbstractDashboardSpaceCreator extends
        UnrestrictedSessionRunner {

    public static final String DASHBOARD_MANAGEMENT_NAME = "dashboard-management";

    public static final String DASHBOARD_MANAGEMENT_PATH = DocumentStoreManager.MANAGEMENT_ROOT_PATH
            + "/" + DASHBOARD_MANAGEMENT_NAME;

    public static final String DASHBOARD_MANAGEMENT_TYPE = "HiddenFolder";

    protected Map<String, String> parameters = new HashMap<String, String>();

    protected AbstractDashboardSpaceCreator(CoreSession session,
            Map<String, String> parameters) {
        super(session);
        this.parameters = parameters;
    }

    /**
     * Returns the dashboard management document, creates it if needed.
     */
    protected DocumentModel getDashboardManagement() throws ClientException {
        String dashboardManagementPath = new Path(
                DocumentStoreManager.MANAGEMENT_ROOT_PATH).append(
                DASHBOARD_MANAGEMENT_NAME).toString();
        DocumentRef dashboardManagementPathRef = new PathRef(
                dashboardManagementPath);
        DocumentModel dashboardManagement;
        if (!session.exists(dashboardManagementPathRef)) {
            dashboardManagement = session.createDocumentModel(
                    DocumentStoreManager.MANAGEMENT_ROOT_PATH,
                    DASHBOARD_MANAGEMENT_NAME, DASHBOARD_MANAGEMENT_TYPE);
            return session.createDocument(dashboardManagement);
        } else {
            return session.getDocument(dashboardManagementPathRef);
        }
    }

    protected void addInitialGadgets(DocumentModel anonymousDashboardSpace)
            throws ClientException {
        Space space = anonymousDashboardSpace.getAdapter(Space.class);
        initializeLayout(space);
        String userLanguage = parameters.get("userLanguage");
        Locale locale = userLanguage != null ? new Locale(userLanguage) : null;
        initializeGadgets(space, session, locale);
    }

    protected void initializeLayout(Space space) throws ClientException {
        space.initLayout(LayoutHelper.buildLayout(YUI_ZT_50_50, YUI_ZT_50_50,
                YUI_ZT_50_50));
    }

    protected void initializeGadgets(Space space, CoreSession session,
            Locale locale) throws ClientException {
        // first column
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "userworkspaces", 0, 0, 0);
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "userdocuments", 0, 0, 1);
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "lastdocuments", 0, 0, 2);
        // second column
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "quicksearch", 0, 1, 0);
        WebContentHelper.createOpenSocialGadget(space, session, locale,
                "tasks", 0, 1, 2);
    }

    protected UserManager getUserManager() throws ClientException {
        try {
            return Framework.getService(UserManager.class);
        } catch (Exception e) {
            throw new ClientException("Unable to retrieve UserManager service",
                    e);
        }
    }

}
