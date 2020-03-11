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
package org.nuxeo.ecm.platform.suggestbox.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Base class for building data transfer objects for results of requests to the SuggestionService.
 *
 * @author ogrisel
 */
public abstract class Suggestion implements Serializable {

    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    private static final long serialVersionUID = 1L;

    protected final String id;

    protected final String type;

    protected final String label;

    protected final String iconURL;

    protected String thumbnailURL = "";

    protected String description = "";

    protected Map<String, List<String>> highlights;

    protected boolean disabled = false;

    public Suggestion(String id, String type, String label, String iconURL) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.iconURL = iconURL;
    }

    /**
     * @since 8.4
     */
    public Suggestion(String id, String type, String label, String iconURL, String thumbnailURL) {
        this(id, type, label, iconURL);
        this.thumbnailURL = thumbnailURL;
    }

    /**
     * @since 9.2
     */
    public Suggestion(String id, String type, String label, String iconURL, String thumbnailURL,
            Map<String, List<String>> highlights) {
        this(id, type, label, iconURL);
        this.thumbnailURL = thumbnailURL;
        this.highlights = highlights;
    }

    /**
     * The id of the object associated to the suggestion.
     *
     * @since 6.0
     */
    public String getId() {
        return id;
    }

    /**
     * A string marker to give the type (i.e. category) of the suggested user action / intent. The type is used to
     * broadcast the selected suggestion to the correct handler.
     */
    public String getType() {
        return type;
    }

    /**
     * The i18n label to display to the user for this suggestion.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Relative URL path to download an icon (can represent the type of suggestion or the specific instance such as the
     * mimetype icon of a document suggestion or the avatar icon of a user profile suggestion).
     */
    public String getIconURL() {
        return iconURL;
    }

    public String getDescription() {
        return description;
    }

    public Suggestion withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Disabled suggestions can be useful to display suggestions that might have been relevant if the context was
     * slightly different (e.g. if the user was logged in instead of anonymous): the UI should not make them selectable
     * but the description should give information to the user on how to make that suggestion enabled (e.g. by logging
     * in). The SuggestionService will throw an exception if the user selects a disabled suggestion.
     */
    public boolean getIsDisabled() {
        return disabled;
    }

    /**
     * @since 8.4
     */
    public String getThumbnailURL() {
        return thumbnailURL;
    }

    /**
     * @since 9.2
     */
    public Suggestion withHighlights(Map<String, List<String>> highlights) {
        this.highlights = highlights;
        return this;
    }

    /**
     * @since 8.4
     */
    public Suggestion withThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
        return this;
    }


    public Suggestion disable() {
        this.disabled = true;
        return this;
    }

    /**
     * @return the url to access to the object. It used by the navigation in the select2.
     * @since 6.0
     */
    public abstract String getObjectUrl();

    @Override
    public String toString() {
        return String.format("Suggestion(\"%s\", \"%s\", \"%s\")", type, label, iconURL);
    }

    /**
     * Get the map of highlights associated to the suggested result. The key of a map entry item represents the
     * highlighted field, the value is the list of segment.
     *
     * @since 9.2
     */
    public Map<String, List<String>> getHighlights() {
        return highlights;
    }
}
