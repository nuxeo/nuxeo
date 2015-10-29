/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

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

    /**
     * @since 7.10
     */
    @XNodeList(value = "videos/video", type = ArrayList.class, componentType = LoginVideo.class)
    protected List<LoginVideo> videos;

    /**
     * @since 7.10
     */
    @XNode("videos@muted")
    protected Boolean muted;

    /**
     * @since 7.10
     */
    @XNode("videos@loop")
    protected Boolean loop;

    @XNode("removeNews")
    protected Boolean removeNews = false;

    protected String headerStyle;

    protected String footerStyle;

    @XNode("newsIframeUrl")
    protected String newsIframeUrl = "//www.nuxeo.com/standalone-login-page/";

    protected String bodyBackgroundStyle;

    protected String loginBoxBackgroundStyle;

    @XNode("loginBoxWidth")
    protected String loginBoxWidth;

    protected String logoUrl;

    @XNode("logoAlt")
    protected String logoAlt;

    @XNode("logoWidth")
    protected String logoWidth;

    @XNode("logoHeight")
    protected String logoHeight;

    /**
     * @since 7.10
     */
    @XNode("fieldAutocomplete")
    protected Boolean fieldAutocomplete;

    /**
     * Boolean to disable background-cover CSS behavior on login page background, as it may not be compliant with all
     * browsers (see NXP-12972/NXP-12978).
     *
     * @since 5.8
     */
    @XNode("disableBackgroundSizeCover")
    protected Boolean disableBackgroundSizeCover;

    /**
     * @since 7.10
     */
    @XNode("loginButtonBackgroundColor")
    protected String loginButtonBackgroundColor;

    public LoginScreenConfig() {
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

    public void registerLoginProvider(String name, String iconUrl, String link, String label, String description,
            LoginProviderLinkComputer computer) {

        LoginProviderLink newProvider = new LoginProviderLink();
        newProvider.name = name;
        newProvider.iconPath = iconUrl;
        newProvider.link = link;
        newProvider.label = label;
        newProvider.description = description;
        if (computer != null) {
            newProvider.urlComputer = computer;
        }

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

    public List<LoginVideo> getVideos() {
        return videos;
    }

    public Boolean getVideoMuted() {
        return muted == null ? false : muted;
    }

    public Boolean getVideoLoop() {
        return loop == null ? true : loop;
    }

    public boolean hasVideos() {
        return videos != null && !videos.isEmpty();
    }

    public boolean getDisplayNews() {
        return !(removeNews || StringUtils.isBlank(newsIframeUrl));
    }

    public Boolean getFieldAutocomplete() {
        return fieldAutocomplete == null ? true : fieldAutocomplete;
    }

    @XNode("headerStyle")
    public void setHeaderStyle(String headerStyle) {
        this.headerStyle = Framework.expandVars(headerStyle);
    }

    @XNode("footerStyle")
    public void setFooterStyle(String footerStyle) {
        this.footerStyle = Framework.expandVars(footerStyle);
    }

    @XNode("bodyBackgroundStyle")
    public void setBodyBackgroundStyle(String bodyBackgroundStyle) {
        this.bodyBackgroundStyle = Framework.expandVars(bodyBackgroundStyle);
    }

    public String getLoginButtonBackgroundColor() {
        return loginButtonBackgroundColor;
    }

    @XNode("loginBoxBackgroundStyle")
    public void setLoginBoxBackgroundStyle(String loginBoxBackgroundStyle) {
        this.loginBoxBackgroundStyle = Framework.expandVars(loginBoxBackgroundStyle);
    }

    @XNode("logoUrl")
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = Framework.expandVars(logoUrl);
    }

    public String getNewsIframeUrl() {
        return newsIframeUrl;
    }

    /**
     * @since 5.8
     * @see #disableBackgroundSizeCover
     */
    public Boolean getDisableBackgroundSizeCover() {
        return disableBackgroundSizeCover;
    }

    protected void merge(LoginScreenConfig newConfig) {
        if (newConfig.newsIframeUrl != null) {
            this.newsIframeUrl = newConfig.newsIframeUrl;
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
        if (newConfig.disableBackgroundSizeCover != null) {
            this.disableBackgroundSizeCover = newConfig.disableBackgroundSizeCover;
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
        if (newConfig.fieldAutocomplete != null) {
            this.fieldAutocomplete = newConfig.fieldAutocomplete;
        }
        if (newConfig.videos != null) {
            videos = newConfig.videos;
        }
        if (newConfig.loop != null) {
            loop = newConfig.loop;
        }
        if (newConfig.removeNews) {
            removeNews = newConfig.removeNews;
        }
        if (newConfig.muted != null) {
            muted = newConfig.muted;
        }
        if (newConfig.loginButtonBackgroundColor != null) {
            loginButtonBackgroundColor = newConfig.loginButtonBackgroundColor;
        }

        if (providers == null) {
            providers = newConfig.providers;
        } else if (newConfig.providers != null && newConfig.providers.size() > 0) {
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

    /**
     * @since 7.10
     */
    @Override
    public LoginScreenConfig clone() {
        LoginScreenConfig clone = new LoginScreenConfig();
        clone.bodyBackgroundStyle = bodyBackgroundStyle;
        clone.disableBackgroundSizeCover = disableBackgroundSizeCover;
        clone.fieldAutocomplete = fieldAutocomplete;
        clone.footerStyle = footerStyle;
        clone.headerStyle = headerStyle;
        clone.loginBoxBackgroundStyle = loginBoxBackgroundStyle;
        clone.loginBoxWidth = loginBoxWidth;
        clone.loginButtonBackgroundColor = loginButtonBackgroundColor;
        clone.logoAlt = logoAlt;
        clone.logoHeight = logoHeight;
        clone.logoUrl = logoUrl;
        clone.logoWidth = logoWidth;
        clone.loop = loop;
        clone.muted = muted;
        clone.newsIframeUrl = newsIframeUrl;
        if (providers != null) {
            clone.providers = new ArrayList<LoginProviderLink>();
            for (LoginProviderLink l : providers) {
                clone.providers.add(l.clone());
            }
        }
        clone.removeNews = removeNews;
        if (videos != null) {
            clone.videos = new ArrayList<LoginVideo>();
            for (LoginVideo v : videos) {
                clone.videos.add(v.clone());
            }
        }
        return clone;
    }

}
