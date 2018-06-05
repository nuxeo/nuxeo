/*
 * (C) Copyright 2013-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
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

    public static final String NUXEO_NEWS_URL = "//www.nuxeo.com/login-page-embedded/";

    /**
     * @since 8.4
     */
    @XNodeMap(value = "startupPages/startupPage", key = "@id", type = HashMap.class, componentType = LoginStartupPage.class)
    protected Map<String, LoginStartupPage> startupPages = new HashMap<>();

    @XNodeList(value = "loginProviders/loginProvider", type = ArrayList.class, componentType = LoginProviderLink.class)
    protected List<LoginProviderLink> providers;

    /**
     * @since 7.10
     */
    @XNodeList(value = "videos/video", type = ArrayList.class, componentType = LoginVideo.class, nullByDefault = true)
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

    /**
     * @since 7.10
     */
    protected String backgroundImage;

    @XNode("removeNews")
    protected Boolean removeNews = false;

    protected String headerStyle;

    protected String footerStyle;

    protected String newsIframeUrl = NUXEO_NEWS_URL;

    protected String newsIframeFullUrl = null;

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

    /**
     * @since 8.4
     */
    @XNode("defaultLocale")
    protected String defaultLocale;

    /**
     * @since 8.4
     */
    @XNode("supportedLocales@append")
    Boolean appendSupportedLocales;

    /**
     * @since 8.4
     */
    @XNodeList(value = "supportedLocales/locale", type = ArrayList.class, componentType = String.class)
    List<String> supportedLocales;

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
                providers = new ArrayList<>();
            }
            providers.add(newProvider);
        }
    }

    /**
     * @since 8.4
     */
    public Map<String, LoginStartupPage> getStartupPages() {
        return startupPages;
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

    @XNode("backgroundImage")
    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = Framework.expandVars(backgroundImage);
    }

    public String getBackgroundImage() {
        return this.backgroundImage;
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

    /**
     * @since 7.10
     */
    @XNode("newsIframeUrl")
    public void setNewsIframeUrl(String newsIframeUrl) {
        this.newsIframeUrl = newsIframeUrl;
        newsIframeFullUrl = null;
    }

    public String getNewsIframeUrl() {
        if (newsIframeFullUrl == null) {
            UriBuilder newsIFrameBuilder = UriBuilder.fromPath(newsIframeUrl);
            if (NUXEO_NEWS_URL.equals(newsIframeUrl)) {
                newsIFrameBuilder.queryParam(Environment.PRODUCT_VERSION,
                        Framework.getProperty(Environment.PRODUCT_VERSION))
                                 .queryParam(Environment.DISTRIBUTION_VERSION,
                                         Framework.getProperty(Environment.DISTRIBUTION_VERSION))
                                 .queryParam(Environment.DISTRIBUTION_PACKAGE,
                                         Framework.getProperty(Environment.DISTRIBUTION_PACKAGE));
            }
            newsIframeFullUrl = newsIFrameBuilder.build().toString();
        }
        try {
            return URLDecoder.decode(newsIframeFullUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Cannot decode login iframe URL " + newsIframeFullUrl);
        }
    }

    /**
     * @since 5.8
     * @see #disableBackgroundSizeCover
     */
    public Boolean getDisableBackgroundSizeCover() {
        return disableBackgroundSizeCover;
    }

    /**
     * @since 8.4
     */
    public String getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * @since 8.4
     */
    public Boolean isAppendSupportedLocales() {
        return appendSupportedLocales;
    }

    /**
     * @since 8.4
     */
    public List<String> getSupportedLocales() {
        List<String> res = new ArrayList<>();
        if (supportedLocales != null) {
            res.addAll(supportedLocales);
        }
        String defaultLocale = getDefaultLocale();
        if (defaultLocale != null && !res.contains(defaultLocale)) {
            res.add(defaultLocale);
        }
        return res;
    }

    protected void merge(LoginScreenConfig newConfig) {
        if (newConfig.newsIframeUrl != null) {
            setNewsIframeUrl(newConfig.newsIframeUrl);
        }
        if (newConfig.headerStyle != null) {
            headerStyle = newConfig.headerStyle;
        }
        if (newConfig.footerStyle != null) {
            footerStyle = newConfig.footerStyle;
        }
        if (newConfig.bodyBackgroundStyle != null) {
            bodyBackgroundStyle = newConfig.bodyBackgroundStyle;
        }
        if (newConfig.loginBoxBackgroundStyle != null) {
            loginBoxBackgroundStyle = newConfig.loginBoxBackgroundStyle;
        }
        if (newConfig.loginBoxWidth != null) {
            loginBoxWidth = newConfig.loginBoxWidth;
        }
        if (newConfig.disableBackgroundSizeCover != null) {
            disableBackgroundSizeCover = newConfig.disableBackgroundSizeCover;
        }
        if (newConfig.logoAlt != null) {
            logoAlt = newConfig.logoAlt;
        }
        if (newConfig.logoHeight != null) {
            logoHeight = newConfig.logoHeight;
        }
        if (newConfig.logoUrl != null) {
            logoUrl = newConfig.logoUrl;
        }
        if (newConfig.logoWidth != null) {
            logoWidth = newConfig.logoWidth;
        }
        if (newConfig.fieldAutocomplete != null) {
            fieldAutocomplete = newConfig.fieldAutocomplete;
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
        if (newConfig.backgroundImage != null) {
            backgroundImage = newConfig.backgroundImage;
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

        if (startupPages == null) {
            startupPages = newConfig.startupPages;
        } else if (newConfig.startupPages != null && !newConfig.startupPages.isEmpty()) {
            for (Map.Entry<String, LoginStartupPage> startupPage : newConfig.startupPages.entrySet()) {
                if (startupPages.containsKey(startupPage.getKey())) {
                    startupPages.get(startupPage.getKey()).merge(startupPage.getValue());
                } else {
                    startupPages.put(startupPage.getKey(), startupPage.getValue());
                }
            }
        }

        if (newConfig.defaultLocale != null) {
            defaultLocale = newConfig.defaultLocale;
        }

        Boolean append = newConfig.isAppendSupportedLocales();
        List<String> newLocales = newConfig.getSupportedLocales();
        Set<String> mergedLocales = new HashSet<String>();
        if (!Boolean.FALSE.equals(append) && supportedLocales != null) {
            mergedLocales.addAll(supportedLocales);
        }
        if (newLocales != null) {
            mergedLocales.addAll(newLocales);
        }
        supportedLocales = new ArrayList<>(mergedLocales);
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
        if (startupPages != null) {
            clone.startupPages = new HashMap<String, LoginStartupPage>();
            for (Map.Entry<String, LoginStartupPage> startupPage : startupPages.entrySet()) {
                clone.startupPages.put(startupPage.getKey(), startupPage.getValue().clone());
            }
        }
        clone.removeNews = removeNews;
        if (videos != null) {
            clone.videos = new ArrayList<LoginVideo>();
            for (LoginVideo v : videos) {
                clone.videos.add(v.clone());
            }
        }
        clone.defaultLocale = defaultLocale;
        clone.appendSupportedLocales = appendSupportedLocales;
        if (supportedLocales != null) {
            clone.supportedLocales = new ArrayList<>(supportedLocales);
        }
        return clone;
    }

}
