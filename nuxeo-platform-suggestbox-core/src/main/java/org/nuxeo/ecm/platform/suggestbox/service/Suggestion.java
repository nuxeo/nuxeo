package org.nuxeo.ecm.platform.suggestbox.service;

import java.io.Serializable;

/**
 * Base class for building data transfer objects for results of requests to the
 * SuggestionService.
 * 
 * @author ogrisel
 */
public class Suggestion implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String type;

    protected final String value;

    protected final String label;

    protected final String icon;

    protected String description = "";

    protected boolean disabled = false;

    public Suggestion(String type, String value, String label, String iconURL) {
        this.type = type;
        this.label = label;
        this.icon = iconURL;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Relative URL path to download an icon (can represent the type of
     * suggestion or the specific instance such as the mimetype icon of a
     * document suggestion or the avatar icon of a user profile suggestion).
     */
    public String getIconURL() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public Suggestion withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Disabled suggestions can be useful to display suggestions that might have
     * been relevant if the context was slightly different (e.g. if the user was
     * logged in instead of anonymous): the UI should not make them selectable
     * but the description should give information to the user on how to make
     * that suggestion enabled (e.g. by logging in). The SuggestionService will
     * throw an exception if the user selects a disabled suggestion.
     * 
     * @return
     */
    public boolean getIsDisabled() {
        return disabled;
    }

    public Suggestion disable() {
        this.disabled = true;
        return this;
    }
}
