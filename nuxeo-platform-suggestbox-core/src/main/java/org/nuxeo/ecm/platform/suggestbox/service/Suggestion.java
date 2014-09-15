/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.io.Serializable;

/**
 * Base class for building data transfer objects for results of requests to the
 * SuggestionService.
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

    protected String description = "";

    protected boolean disabled = false;

    public Suggestion(String id, String type, String label, String iconURL) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.iconURL = iconURL;
    }

    /**
     * The id of the object associated to the suggestion.
     *
     * @since 5.9.6
     */
    public String getId() {
        return id;
    }

    /**
     * A string marker to give the type (i.e. category) of the suggested user
     * action / intent. The type is used to broadcast the selected suggestion to
     * the correct handler.
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
     * Relative URL path to download an icon (can represent the type of
     * suggestion or the specific instance such as the mimetype icon of a
     * document suggestion or the avatar icon of a user profile suggestion).
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
     * Disabled suggestions can be useful to display suggestions that might have
     * been relevant if the context was slightly different (e.g. if the user was
     * logged in instead of anonymous): the UI should not make them selectable
     * but the description should give information to the user on how to make
     * that suggestion enabled (e.g. by logging in). The SuggestionService will
     * throw an exception if the user selects a disabled suggestion.
     */
    public boolean getIsDisabled() {
        return disabled;
    }

    public Suggestion disable() {
        this.disabled = true;
        return this;
    }

    /**
     * @return the url to access to the object. It used by the navigation in the select2.
     *
     * @since 5.9.6
     */
    public abstract String getObjectUrl();

    @Override
    public String toString() {
        return String.format("Suggestion(\"%s\", \"%s\", \"%s\")", type, label,
                iconURL);
    }
}
