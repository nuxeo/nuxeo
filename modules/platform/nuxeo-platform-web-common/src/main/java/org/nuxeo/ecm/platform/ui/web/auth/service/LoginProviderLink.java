/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.ui.web.auth.service;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
@XObject("loginProvider")
public class LoginProviderLink implements Descriptor {

    private static final Logger log = LogManager.getLogger(LoginProviderLink.class);

    public LoginProviderLink() {

    }

    /**
     * @since 10.10
     */
    public LoginProviderLink(String name, String iconPath, String link, String label, String description,
            LoginProviderLinkComputer urlComputer) {
        this.name = name;
        this.iconPath = iconPath;
        this.link = link;
        this.label = label;
        this.description = description;
        this.urlComputer = urlComputer;
    }

    @XNode("@name")
    protected String name;

    @XNode("label")
    protected String label;

    @XNode("@remove")
    protected boolean remove = false;

    protected String iconPath;

    protected String link;

    @XNode("@class")
    protected Class<LoginProviderLinkComputer> urlComputerClass;

    protected LoginProviderLinkComputer urlComputer;

    @XNode("description")
    protected String description;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconPath() {
        return iconPath;
    }

    @XNode("iconPath")
    public void setIconPath(String iconPath) {
        this.iconPath = Framework.expandVars(iconPath);
    }

    public String getLink(HttpServletRequest req, String requestedUrl) {
        if (urlComputerClass != null && urlComputer == null) {
            try {
                urlComputer = urlComputerClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Unable to instantiate LoginProviderLinkComputer", e);
            }
        }
        if (urlComputer != null) {
            return urlComputer.computeUrl(req, requestedUrl);
        } else {
            return link;
        }
    }

    @XNode("link")
    public void setLink(String link) {
        this.link = Framework.expandVars(link);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabel() {
        if (label == null) {
            return getName();
        }
        return label;
    }

    public String getLink() {
        return link;
    }

    @Override
    public LoginProviderLink merge(Descriptor o) {
        var other = (LoginProviderLink) o;
        var merged = new LoginProviderLink();
        merged.name = name; // we merge based on name, so no need for merging it
        merged.label = getIfNull(other.label, label);
        merged.remove = other.remove;
        merged.iconPath = getIfNull(other.iconPath, iconPath);
        merged.link = getIfNull(other.link, link);
        merged.urlComputerClass = getIfNull(other.urlComputerClass, urlComputerClass);
        merged.description = getIfNull(other.description, description);
        return merged;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LoginProviderLink && name != null) {
            return name.equals(((LoginProviderLink) obj).getName());
        }
        return super.equals(obj);
    }
}
