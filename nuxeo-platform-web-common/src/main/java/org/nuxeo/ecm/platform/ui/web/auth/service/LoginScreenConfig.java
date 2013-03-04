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
package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * {@link XMap} object to manage configuration of the login screen (login.jsp)
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
@XObject("loginScreenConfig")
public class LoginScreenConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNodeList(value = "loginProviders/loginProvider", type = ArrayList.class, componentType = LoginProviderLink.class)
    protected List<LoginProviderLink> providers;

    @XNode("headerStyle")
    protected String headerStyle;

    @XNode("footerStyle")
    protected String footerStyle;

    @XNode("@displayNews")
    protected Boolean displayNews;

    @XNode("bodyBackgroundStyle")
    protected String bodyBackgroundStyle;

    @XNode("loginBoxBackgroundStyle")
    protected String loginBoxBackgroundStyle;

    @XNode("loginBoxWidth")
    protected String loginBoxWidth;

    @XNode("logoUrl")
    protected String logoUrl;

    @XNode("logoAlt")
    protected String logoAlt;

    @XNode("logoWidth")
    protected String logoWidth;

    @XNode("logoHeight")
    protected String logoHeight;

    public LoginScreenConfig() {
        //
    }

    public List<LoginProviderLink> getProviders() {
        return providers;
    }

    public void setProviders(List<LoginProviderLink> providers) {
        this.providers = providers;
    }

    public LoginProviderLink getProvider(String name) {
        if (getProviders() == null) {
            return null;
        }
        for (LoginProviderLink provider : getProviders()) {
            if (name.equals(provider.getName())) {
                return provider;
            }
        }
        return null;
    }

    protected void merge(LoginScreenConfig newConfig) {
        if (newConfig.displayNews != null) {
            this.displayNews = newConfig.displayNews;
        }
        if (newConfig.headerStyle != null) {
            this.headerStyle = newConfig.headerStyle;
        }
        if (newConfig.footerStyle != null) {
            this.footerStyle = newConfig.footerStyle;
        }
        if (newConfig.bodyBackgroundStyle != null) {
            this.bodyBackgroundStyle = newConfig.bodyBackgroundStyle;
        }
        if (newConfig.loginBoxBackgroundStyle != null) {
            this.loginBoxBackgroundStyle = newConfig.loginBoxBackgroundStyle;
        }
        if (newConfig.loginBoxWidth != null) {
            this.loginBoxWidth = newConfig.loginBoxWidth;
        }
        if (newConfig.logoAlt != null) {
            this.logoAlt = newConfig.logoAlt;
        }
        if (newConfig.logoHeight != null) {
            this.logoHeight = newConfig.logoHeight;
        }
        if (newConfig.logoUrl != null) {
            this.logoUrl = newConfig.logoUrl;
        }
        if (newConfig.logoWidth != null) {
            this.logoWidth = newConfig.logoWidth;
        }

        if (providers == null) {
            providers = newConfig.providers;
        } else if (newConfig.providers != null
                && newConfig.providers.size() > 0) {
            for (LoginProviderLink link : newConfig.providers) {

                int idx = providers.indexOf(link);
                if (idx >= 0) {
                    if (link.remove) {
                        providers.remove(idx);
                    } else {
                        providers.get(idx).merge(link);
                    }
                } else {
                    providers.add(link);
                }
            }
        }
    }

    public void registerLoginProvider(String name, String iconUrl, String link) {

        LoginProviderLink newProvider = new LoginProviderLink();
        newProvider.name = name;
        newProvider.iconPath = iconUrl;
        newProvider.link = link;

        LoginProviderLink existingProvider = getProvider(name);
        if (existingProvider != null) {
            existingProvider.merge(newProvider);
        } else {
            if (providers == null) {
                providers = new ArrayList<LoginProviderLink>();
            }
            providers.add(newProvider);
        }
    }

    public String getHeaderStyle() {
        return headerStyle;
    }

    public String getFooterStyle() {
        return footerStyle;
    }

    public String getBodyBackgroundStyle() {
        return bodyBackgroundStyle;
    }

    public String getLoginBoxBackgroundStyle() {
        return loginBoxBackgroundStyle;
    }

    public String getLoginBoxWidth() {
        return loginBoxWidth;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getLogoAlt() {
        return logoAlt;
    }

    public String getLogoWidth() {
        return logoWidth;
    }

    public String getLogoHeight() {
        return logoHeight;
    }

    public boolean getDisplayNews() {
        if (displayNews == null) {
            return true;
        }
        return displayNews;
    }

}
