package org.nuxeo.ecm.platform.suggestbox.service;


/**
 * Suggest to navigate to a specific user profile.
 */
public class UserSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    protected final String userId;

    public UserSuggestion(String userId, String label, String iconURL) {
        super(CommonSuggestionTypes.USER, label, iconURL);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
