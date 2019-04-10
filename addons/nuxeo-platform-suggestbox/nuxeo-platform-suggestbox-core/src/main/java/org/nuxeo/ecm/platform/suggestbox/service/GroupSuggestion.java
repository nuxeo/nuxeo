package org.nuxeo.ecm.platform.suggestbox.service;


/**
 * Suggest to navigate to a specific group profile.
 */
public class GroupSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    protected final String groupId;

    public GroupSuggestion(String groupId, String label, String iconURL) {
        super(CommonSuggestionTypes.GROUP, label, iconURL);
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }
}
