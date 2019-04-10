package org.nuxeo.ecm.platform.suggestbox.service.suggesters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.suggestbox.service.GroupSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SearchDocumentsSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.ecm.platform.suggestbox.service.Suggestion;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionContext;
import org.nuxeo.ecm.platform.suggestbox.service.SuggestionException;
import org.nuxeo.ecm.platform.suggestbox.service.UserSuggestion;
import org.nuxeo.ecm.platform.suggestbox.service.descriptors.SuggesterDescriptor;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Perform a lookup by name query on the UserManager service and suggest to
 * navigate to the top user and / or group profiles matching that query.
 * 
 * If searchFields are provided in the parameters, suggestion for searching
 * document with reference to the users are also generated.
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

    @Override
    public List<Suggestion> suggest(String userInput, SuggestionContext context)
            throws SuggestionException {
        I18nHelper i18n = I18nHelper.instanceFor(context.messages);
        UserManager userManager = Framework.getLocalService(UserManager.class);
        if (userManager == null) {
            throw new SuggestionException("UserManager is not active");
        }
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        List<Suggestion> searchSuggestions = new ArrayList<Suggestion>();
        try {
            int count = 0;
            for (DocumentModel user : userManager.searchUsers(userInput)) {
                // suggest to navigate to the user profile
                String userLabel = user.getProperty("user:firstName").getValue(
                        String.class);
                userLabel += " ";
                userLabel += user.getProperty("user:lastName").getValue(
                        String.class);
                if (userLabel.trim().isEmpty()) {
                    userLabel = user.getProperty("user:username").getValue(
                            String.class);
                }
                suggestions.add(new UserSuggestion(user.getId(), userLabel,
                        userIconURL));

                // suggest to search documents related to the user profile
                for (String searchField : searchFields) {
                    String i18nLabel = i18n.translate(searchLabelPrefix
                            + searchField.replaceAll(":", "_"), userLabel);
                    Suggestion suggestion = new SearchDocumentsSuggestion(
                            i18nLabel, searchIconURL).withSearchCriterion(
                            searchField, user.getId());
                    searchSuggestions.add(suggestion);
                }
                count++;
                if (count >= userSuggestionsLimit ) {
                    break;
                }
            }
            count = 0;
            for (DocumentModel group : userManager.searchGroups(userInput)) {
                String label = group.getProperty("group:grouplabel").getValue(
                        String.class);
                if (label == null || label.isEmpty()) {
                    label = group.getProperty("group:groupname").getValue(
                            String.class);
                }
                suggestions.add(new GroupSuggestion(group.getId(), label,
                        groupIconURL));
                count++;
                if (count >= groupSuggestionsLimit) {
                    break;
                }
            }
            suggestions.addAll(searchSuggestions);
            return suggestions;
        } catch (ClientException e) {
            throw new SuggestionException(String.format(
                    "Suggester '%s' failed to perform query with input '%s'",
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
