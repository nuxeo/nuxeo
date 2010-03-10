package org.nuxeo.opensocial.gadgets.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;

public class ExternalGadgetDescriptor implements GadgetDeclaration {

    protected String category;

    protected String directory;

    protected boolean disabled;

    protected URL gadgetDefinition;

    protected String iconURL;

    protected String name;

    public ExternalGadgetDescriptor(String category, String directory,
            boolean disabled, URL gadgetDefinition, String iconURL, String name) {
        this.category = category;
        this.directory = directory;
        this.disabled = disabled;
        this.gadgetDefinition = gadgetDefinition;
        this.iconURL = iconURL;
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public String getDirectory() {
        return directory;
    }

    public boolean getDisabled() {
        return disabled;
    }

    public URL getGadgetDefinition() throws MalformedURLException {
        return gadgetDefinition;
    }

    public String getIconUrl() {
        return iconURL;
    }

    public String getName() {
        return name;
    }

    public InputStream getResourceAsStream(String resourcePath)
            throws IOException {
        URL result = new URL(getGadgetDefinition(), resourcePath);
        return result.openStream();
    }

}
