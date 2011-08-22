/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.gadgets.service;

import static org.nuxeo.launcher.config.Environment.NUXEO_LOOPBACK_URL;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_PATH;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.helper.OpenSocialGadgetHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.osgi.framework.Bundle;

@XObject("internalGadget")
public class InternalGadgetDescriptor extends BaseGadgetDescriptor implements
        GadgetDeclaration {

    private static final long serialVersionUID = 1L;

    public static final String HTTP = "http://";

    public static final String HTTP_SEPARATOR = ":";

    public static final String URL_SEPARATOR = "/";

    @XNode("@name")
    protected String name;

    @XNode("@label")
    protected String label;

    @XNode("@disabled")
    protected Boolean disabled;

    // File Name of the gadget's XML
    @XNode("entryPoint")
    protected String entryPoint = "";

    // URL's mount point /gadgets/{mountPoint}/{entryPoint}
    @XNode("mountPoint")
    protected String mountPoint = "";

    // Directory where the gadgets files are stored in the JAR
    @XNode("directory")
    protected String directory = "";

    @XNode("category")
    protected String category;

    @XNode("description")
    protected String description;

    @XNode("icon")
    protected String icon;

    protected Bundle bundle;

    protected ComponentName componentName;

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public boolean getDisabled() {
        if (disabled == null) {
            return false;
        }
        return disabled;
    }

    public String getIcon() {
        return icon;
    }

    public final String getMountPoint() {
        if ("".equals(mountPoint)) {
            return name;
        }
        return mountPoint;
    }

    public final String getCategory() {
        return category;
    }

    public final void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public final Bundle getBundle() {
        return bundle;
    }

    public final void setComponentName(ComponentName name) {
        componentName = name;

    }

    public final ComponentName getComponentName() {
        return componentName;
    }

    public final void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public final String getEntryPoint() {
        if ("".equals(entryPoint)) {
            return name + ".xml";
        } else {
            return entryPoint;
        }
    }

    public String getDirectory() {
        if (StringUtils.isBlank(directory)) {
            return name;
        } else {
            return directory;
        }
    }

    public String getIconUrl() {
        StringBuilder sb = getUrlPrefix(true);
        sb.append(getMountPoint());
        sb.append(URL_SEPARATOR);
        sb.append(icon);
        return sb.toString();
    }

    public StringBuilder getUrlPrefix(boolean relativeUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append(OpenSocialGadgetHelper.getGadgetsBaseUrl(relativeUrl, false));
        sb.append(Framework.getProperty(OPENSOCIAL_GADGETS_PATH));
        return sb;
    }

    public InputStream getResourceAsStream(String resourcePath)
            throws IOException {

        ComponentInstance component = Framework.getRuntime().getComponentInstance(
                componentName);
        Bundle bundle = component.getRuntimeContext().getBundle();
        URL gadgetURL = bundle.getEntry("gadget/" + getDirectory() + "/"
                + resourcePath);
        if (gadgetURL == null) {
            gadgetURL = bundle.getEntry("gadget/resources/" + resourcePath);
        }
        if (gadgetURL != null) {
            return gadgetURL.openStream();
        } else {
            return null;
        }
    }

    public URL getGadgetDefinition() throws MalformedURLException {
        return new URL(getGadgetDefinition(false));
    }

    protected String getGadgetDefinition(boolean relativeUrl)
            throws MalformedURLException {
        StringBuilder sb = getUrlPrefix(relativeUrl);
        sb.append(getMountPoint());
        sb.append(URL_SEPARATOR);
        sb.append(getEntryPoint());
        return sb.toString();
    }

    @Override
    public String getPublicGadgetDefinition() throws MalformedURLException {
        return getGadgetDefinition(true);
    }

    @Override
    public String getDescription() {
        if (description != null) {
            return description;
        }
        return getDescriptionFromSpec();
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Override
    public String getThumbnail() {
        String thumb = super.getThumbnail();
        if (thumb == null || "".equals(thumb.trim())) {
            return getIconUrl();
        } else {
            thumb = rewriteURL(thumb);
        }
        return thumb;
    }

    /**
     * Rewrite the URL to remove the private IP from it and keep a relative URL.
     * <p>
     * Shindig always prepend the beginning of the Spec URL if it gets a
     * relative URL for Thumbnail or Screenshot in the gadget spec, but we need
     * a relative URL.
     *
     */
    protected String rewriteURL(String url) {
        if (url != null) {
            String loopbackURL = Framework.getProperty(NUXEO_LOOPBACK_URL);
            if (url.startsWith(loopbackURL)) {
                url = url.replaceFirst(loopbackURL,
                        VirtualHostHelper.getContextPathProperty());
            }
        }
        return url;
    }

    @Override
    public String getScreenshot() {
        String screenshot = super.getScreenshot();
        if (screenshot != null) {
            screenshot = rewriteURL(screenshot);
        }
        return screenshot;
    }

}
