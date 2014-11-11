package org.nuxeo.ecm.platform.usermanager.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;import java.util.HashMap;import java.util.HashSet;import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Page provider listing groups.
 * <p>
 * This page provider requires one parameter: the first one to
 * be filled with the search string.
 * <p>
 * This page provider requires the property {@link #GROUPS_LISTING_MODE_PROPERTY} to
 * be filled with a the listing mode to use.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public class GroupsPageProvider extends
        AbstractPageProvider<DocumentModel> implements
        PageProvider<DocumentModel> {

    private static final Log log = LogFactory.getLog(UsersPageProvider.class);

    private static final long serialVersionUID = 1L;

    protected static final String GROUPS_LISTING_MODE_PROPERTY = "groupsListingMode";

    protected static final String ALL_MODE = "all";

    protected static final String SEARCH_ONLY_MODE = "search_only";

    protected static final String SEARCH_OVERFLOW_ERROR_MESSAGE = "label.security.searchOverFlow";

    protected List<DocumentModel> pageGroups;

    @Override
    public List<DocumentModel> getCurrentPage() {
        if (pageGroups == null) {
            error = null;
            errorMessage = null;
            pageGroups = new ArrayList<DocumentModel>();

            List<DocumentModel> groups = new ArrayList<DocumentModel>();
            try {
                UserManager userManager = Framework.getService(UserManager.class);
                String groupListingMode = getGroupListingMode();
                if (ALL_MODE.equals(groupListingMode)) {
                    groups = searchAllGroups(userManager);
                } else if (SEARCH_ONLY_MODE.equals(groupListingMode)) {
                    groups = searchGroups(userManager);
                }
            } catch (SizeLimitExceededException slee) {
                error = slee;
                errorMessage = SEARCH_OVERFLOW_ERROR_MESSAGE;
                log.warn(slee.getMessage(), slee);
            } catch (Exception e) {
                error = e;
                errorMessage = e.getMessage();
                log.warn(e.getMessage(), e);
            }

            if (!hasError()) {
                resultsCount = groups.size();
                // post-filter the results "by hand" to handle pagination
                long pageSize = getMinMaxPageSize();
                if (pageSize == 0) {
                    pageGroups.addAll(groups);
                } else {
                    // handle offset
                    if (offset <= resultsCount) {
                        for (int i = Long.valueOf(offset).intValue(); i < resultsCount
                                && i < offset + pageSize; i++) {
                            pageGroups.add(groups.get(i));
                        }
                    }
                }
            }
        }
        return pageGroups;
    }

    protected String getGroupListingMode() {
        Map<String, Serializable> props = getProperties();
        if (props.containsKey(GROUPS_LISTING_MODE_PROPERTY)) {
            return (String) props.get(GROUPS_LISTING_MODE_PROPERTY);
        }
        return SEARCH_ONLY_MODE;
    }

    protected String getFirstParameter() {
        Object[] parameters = getParameters();
        if (parameters.length > 0) {
            String param = (String) parameters[0];
            if (param != null) {
                return param.trim();
            }
        }
        return "";
    }

    protected List<DocumentModel> searchAllGroups(UserManager userManager) throws ClientException {
       return userManager.searchGroups(Collections.<String, Serializable> emptyMap(), null);
    }

    protected List<DocumentModel> searchGroups(UserManager userManager) throws ClientException {
        List<DocumentModel> groups = new ArrayList<DocumentModel>();
        String searchString = getFirstParameter();
        if ("*".equals(searchString)) {
            groups = searchAllGroups(userManager);
        } else if (!StringUtils.isEmpty(searchString)) {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            // XXX: search only on id, better conf should be set in
            // user manager interface
            filter.put(userManager.getGroupIdField(), searchString);
            // parameters must be serializable so copy keySet to
            // HashSet
            groups = userManager.searchGroups(filter,
                    new HashSet<String>(filter.keySet()));
        }
        return groups;
    }

    /**
     * This page provider does not support sort for now => override what may be
     * contributed in the definition
     */
    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    protected void pageChanged() {
        super.pageChanged();
        pageGroups = null;
    }

    @Override
    public void refresh() {
        super.refresh();
        pageGroups = null;
    }

}
