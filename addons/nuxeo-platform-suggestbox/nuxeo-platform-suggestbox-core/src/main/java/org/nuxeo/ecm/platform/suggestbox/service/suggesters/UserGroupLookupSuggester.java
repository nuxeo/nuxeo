/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.suggestbox.service.GroupSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.UserSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.usermanager.UserAdapter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Perform a lookup by name query on the UserManager service and suggest to navigate to the top user and / or group
 * profiles matching that query. If searchFields are provided in the parameters, suggestion for searching document with
 * reference to the users are also generated.
 *
 * @author ogrisel
 */
public class UserGroupLookupSuggester implements Suggester {

    protected String userIconURL = "/icons/user.png";

    protected String groupIconURL = "/icons/group.png";

    protected String searchIconURL = "/img/facetedSearch.png";

    protected SuggesterDescriptor descriptor;

    protected List<String> searchFields = new ArrayList<String>();

    protected final String searchLabelPrefix = "label.searchDocumentsByUser_";

    protected int userSuggestionsLimit = 5;

    protected int groupSuggestionsLimit = 5;

    protected String suggesterId = "UserGroupLookupSuggester";

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context) throws SuggestionException {
        I18nHelper i18n = I18nHelper.instanceFor(context.messages);
        UserManager userManager = Framework.getService(UserManager.class);
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        List<Suggestion> searchSuggestions = new ArrayList<Suggestion>();
        try {
            int count = 0;
            for (DocumentModel userDoc : userManager.searchUsers(userInput)) {
                UserAdapter user = userDoc.getAdapter(UserAdapter.class);
                // suggest to navigate to the user profile
                String firstName = user.getFirstName();
                String userLabel = firstName != null ? firstName : "";
                String lastName = user.getLastName();
                userLabel += " ";
                userLabel += lastName != null ? lastName : "";
                userLabel = userLabel.trim();
                if (userLabel.isEmpty()) {
                    userLabel = user.getName();
                }
                suggestions.add(new UserSuggestion(userDoc.getId(), userLabel, userIconURL));

                count++;
                if (count >= userSuggestionsLimit) {
                    break;
                }
            }
            count = 0;
            String groupIdField = userManager.getGroupIdField();
            String groupLabelField = userManager.getGroupLabelField();
            for (DocumentModel group : userManager.searchGroups(userInput)) {
                String label = group.getProperty(groupLabelField).getValue(String.class);
                if (label == null || label.isEmpty()) {
                    label = group.getProperty(groupIdField).getValue(String.class);
                }
                suggestions.add(new GroupSuggestion(group.getId(), label, groupIconURL));
                count++;
                if (count >= groupSuggestionsLimit) {
                    break;
                }
            }
            suggestions.addAll(searchSuggestions);
            return suggestions;
        } catch (DirectoryException e) {
            throw new SuggestionException(String.format("Suggester '%s' failed to perform query with input '%s'",
                    descriptor.getName(), userInput), e);
        }
    }

    @Override
    public void initWithParameters(SuggesterDescriptor descriptor) {
        Map<String, String> params = descriptor.getParameters();
        String userIconURL = params.get("userIconURL");
        if (userIconURL != null) {
            this.userIconURL = userIconURL;
        }
        String groupIconURL = params.get("groupIconURL");
        if (groupIconURL != null) {
            this.groupIconURL = groupIconURL;
        }
        String searchIconURL = params.get("searchIconURL");
        if (searchIconURL != null) {
            this.searchIconURL = searchIconURL;
        }
        String userSuggestionsLimit = params.get("userSuggestionsLimit");
        if (userSuggestionsLimit != null) {
            this.userSuggestionsLimit = Integer.valueOf(userSuggestionsLimit).intValue();
        }
        String groupSuggestionsLimit = params.get("groupSuggestionsLimit");
        if (groupSuggestionsLimit != null) {
            this.groupSuggestionsLimit = Integer.valueOf(groupSuggestionsLimit).intValue();
        }
        String searchFields = params.get("searchFields");
        if (searchFields != null && !searchFields.isEmpty()) {
            this.searchFields = Arrays.asList(searchFields.split(", *"));
        }
        this.descriptor = descriptor;
    }
}
