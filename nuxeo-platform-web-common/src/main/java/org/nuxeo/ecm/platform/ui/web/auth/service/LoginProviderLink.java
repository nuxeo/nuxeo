package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

@XObject("loginProvider")
public class LoginProviderLink implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Log log = LogFactory.getLog(LoginProviderLink.class);

    public LoginProviderLink() {

    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LoginProviderLink && name != null) {
            return name.equals(((LoginProviderLink) obj).getName());
        }
        return super.equals(obj);
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
                urlComputer = (LoginProviderLinkComputer) urlComputerClass.newInstance();
            } catch (Exception e) {
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

    public void merge(LoginProviderLink newLink) {
        if (newLink.link != null) {
            link = newLink.link;
        }
        if (newLink.description != null) {
            description = newLink.description;
        }
        if (newLink.iconPath != null) {
            iconPath = newLink.iconPath;
        }
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

}
