/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.gadgets.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;

public class ExternalGadgetDescriptor implements GadgetDeclaration {

    protected String category;

    protected boolean disabled;

    protected URL gadgetDefinition;

    protected String iconURL;

    protected String name;

    public ExternalGadgetDescriptor(String category, boolean disabled,
            URL gadgetDefinition, String iconURL, String name) {
        this.category = category;
        this.disabled = disabled;
        this.gadgetDefinition = gadgetDefinition;
        this.iconURL = iconURL;
        this.name = name;
    }

    public String getCategory() {
        return category;
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
