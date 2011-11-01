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

    public String getIconURL() {
        return icon;
    }

}
