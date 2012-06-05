package org.nuxeo.ecm.webapp.security;

import static org.nuxeo.ecm.platform.ui.web.api.WebActions.SUBTAB_CATEGORY_SUFFIX;

import java.security.Principal;

import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Common properties and methods for Users and Groups management.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 * @since 5.4.2
 */
public abstract class AbstractUserGroupManagement {

    public static final String VIEW_HOME = "view_home";

    public static final String MAIN_TABS_CATEGORY = "MAIN_TABS";

    public static final String MAIN_TAB_HOME = MAIN_TABS_CATEGORY + ":home";

    public static final String NUXEO_ADMIN_CATEGORY = "NUXEO_ADMIN";

    public static final String USER_CENTER_CATEGORY = "USER_CENTER";

    public static final String USERS_GROUPS_MANAGER = "UsersGroupsManager";

    public static final String USERS_GROUPS_MANAGER_SUB_TAB = USERS_GROUPS_MANAGER
            + SUBTAB_CATEGORY_SUFFIX;

    public static final String USERS_GROUPS_HOME = "UsersGroupsHome";

    public static final String USERS_GROUPS_HOME_SUB_TAB = USERS_GROUPS_HOME
            + SUBTAB_CATEGORY_SUFFIX;

    public static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-0123456789.@";

    public static final String DEFAULT_LISTING_MODE = "search_only";

    public static final String DETAILS_VIEW_MODE = "view";

    public static final String USERS_GROUPS_MANAGEMENT_ACCESS_FILTER = "usersGroupsManagementAccess";

    @In(create = true)
    protected Principal currentUser;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected WebActions webActions;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    protected String searchString = "";

    protected String listingMode;

    protected String detailsMode;

    protected boolean showCreateForm;

    protected boolean showUserOrGroup;

    protected boolean shouldResetStateOnTabChange = true;

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getListingMode() throws ClientException {
        if (listingMode == null) {
            listingMode = computeListingMode();
            if (listingMode == null || listingMode.trim().isEmpty()) {
                listingMode = DEFAULT_LISTING_MODE;
            }
        }
        return listingMode;
    }

    protected abstract String computeListingMode() throws ClientException;

    public void setListingMode(String listingMode) {
        this.listingMode = listingMode;
    }

    public String getDetailsMode() {
        if (detailsMode == null) {
            detailsMode = DETAILS_VIEW_MODE;
        }
        return detailsMode;
    }

    public void setDetailsMode(String mode) {
        detailsMode = mode;
    }

    public boolean isShowCreateForm() {
        return showCreateForm;
    }

    public void toggleShowCreateForm() {
        showCreateForm = !showCreateForm;
    }

    public boolean isShowUserOrGroup() {
        return showUserOrGroup;
    }

    public void toggleShowUserOrGroup() {
        showUserOrGroup = !showUserOrGroup;
    }

}
