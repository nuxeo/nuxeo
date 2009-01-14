/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.webapp.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Methods to get user/groups suggestions from searches
 *
 * @author Anahide Tchertchian
 *
 */
@Name("userSuggestionActions")
@SerializedConcurrentAccess
public class UserSuggestionActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserSuggestionActionsBean.class);

    public static final String USER_TYPE = "USER_TYPE";

    public static final String GROUP_TYPE = "GROUP_TYPE";

    public static final String TYPE_KEY_NAME = "type";

    public static final String ID_KEY_NAME = "id";

    public static final String LABEL_KEY_NAME = "label";

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @RequestParameter
    protected String userSuggestionSearchType;

    @RequestParameter
    protected Integer userSuggestionMaxSearchResults;

    @RequestParameter
    protected String userSuggestionMessageId;

    /**
     * Id of the editable list component where selection ids are put.
     * <p>
     * Component must be an instance of {@link UIEditableList}
     */
    @RequestParameter
    protected String suggestionSelectionListId;

    protected void addSearchOverflowMessage() {
        if (userSuggestionMessageId != null) {
            facesMessages.addToControl(userSuggestionMessageId,
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.security.searchOverFlow"));
        } else {
            log.error("Search overflow");
        }
    }

    public List<NuxeoGroup> getGroupsSuggestions(Object input)
            throws ClientException {
        try {
            String pattern = (String) input;
            // XXX: this doesn't fetch group members (references)
            return userManager.searchGroups(pattern);
        } catch (SizeLimitExceededException e) {
            addSearchOverflowMessage();
            return Collections.emptyList();
        } catch (Exception e) {
            throw new ClientException("error searching for groups", e);
        }
    }

    public List<NuxeoPrincipal> getUserSuggestions(Object input)
            throws ClientException {
        try {
            String searchPattern = (String) input;
            return userManager.searchPrincipals(searchPattern);
        } catch (SizeLimitExceededException e) {
            addSearchOverflowMessage();
            return Collections.emptyList();
        } catch (Exception e) {
            throw new ClientException("error searching for principals", e);
        }
    }

    public Object getSuggestions(Object input) throws ClientException {
        List<NuxeoPrincipal> users;
        if (USER_TYPE.equals(userSuggestionSearchType)
                || StringUtils.isEmpty(userSuggestionSearchType)) {
            users = getUserSuggestions(input);
        } else {
            users = Collections.emptyList();
        }

        List<NuxeoGroup> groups;
        if (GROUP_TYPE.equals(userSuggestionSearchType)
                || StringUtils.isEmpty(userSuggestionSearchType)) {
            groups = getGroupsSuggestions(input);
        } else {
            groups = Collections.emptyList();
        }

        int userSize = users.size();
        int groupSize = groups.size();
        int totalSize = userSize + groupSize;

        if (userSuggestionMaxSearchResults != null
                && userSuggestionMaxSearchResults > 0) {
            if (userSize > userSuggestionMaxSearchResults
                    || groupSize > userSuggestionMaxSearchResults
                    || totalSize > userSuggestionMaxSearchResults) {
                addSearchOverflowMessage();
                return null;
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(
                totalSize);

        for (NuxeoPrincipal user : users) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put(TYPE_KEY_NAME, USER_TYPE);
            entry.put(ID_KEY_NAME, user.getName());
            entry.put(LABEL_KEY_NAME, getUserLabel(user));
            result.add(entry);
        }

        for (NuxeoGroup group : groups) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put(TYPE_KEY_NAME, GROUP_TYPE);
            entry.put(ID_KEY_NAME, group.getName());
            entry.put(LABEL_KEY_NAME, getGroupLabel(group));
            result.add(entry);
        }

        return result;
    }

    public static String getUserLabel(NuxeoPrincipal user) {
        String name = user.getName();
        StringBuilder label = new StringBuilder(name);
        label.append(" (");
        label.append(Functions.principalFullName(user));
        label.append(')');
        return label.toString();
    }

    public static String getGroupLabel(NuxeoGroup group) {
        return group.getName();
    }

}
