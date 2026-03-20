/*
 * (C) Copyright 2013-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.UriBuilder;

import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Descriptor;

/**
 * {@link XMap} object to manage configuration of the login screen (login.jsp)
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
@XObject("loginScreenConfig")
public class LoginScreenConfig implements Descriptor {

    public static final String NUXEO_NEWS_URL = "//www.nuxeo.com/login-page-embedded-1/";

    /** @since 8.4 */
    @XNodeMap(value = "startupPages/startupPage", key = "@id", type = HashMap.class, componentType = LoginStartupPage.class)
    protected Map<String, LoginStartupPage> startupPages = new HashMap<>();

    @XNodeList(value = "loginProviders/loginProvider", type = ArrayList.class, componentType = LoginProviderLink.class)
    protected List<LoginProviderLink> providers;

    /** @since 7.10 */
    @XNodeList(value = "videos/video", type = ArrayList.class, componentType = LoginVideo.class, nullByDefault = true)
    protected List<LoginVideo> videos;

    /** @since 7.10 */
    @XNode("videos@muted")
    protected Boolean muted;

    /** @since 7.10 */
    @XNode("videos@loop")
    protected Boolean loop;

    /** @since 7.10 */
    protected String backgroundImage;

    @XNode("removeNews")
    protected Boolean removeNews;

    protected String headerStyle;

    protected String footerStyle;

    protected String newsIframeUrl;

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

    /** @since 7.10 */
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

    /** @since 7.10 */
    @XNode("loginButtonBackgroundColor")
    protected String loginButtonBackgroundColor;

    /** @since 8.4 */
    @XNode("defaultLocale")
    protected String defaultLocale;

    /** @since 8.4 */
    @XNode("supportedLocales@append")
    protected Boolean appendSupportedLocales;

    /** @since 8.4 */
    @XNodeList(value = "supportedLocales/locale", type = ArrayList.class, componentType = String.class)
    protected List<String> supportedLocales;

    public LoginScreenConfig() {
    }

    /**
     * Instantiates a login screen configuration with the given login provider.
     *
     * @since 10.10
     */
    public LoginScreenConfig(LoginProviderLink provider) {
        providers = new ArrayList<>();
        providers.add(provider);
    }

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
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
        return isTrue(muted);
    }

    public Boolean getVideoLoop() {
        return toBooleanDefaultIfNull(loop, true);
    }

    public boolean hasVideos() {
        return videos != null && !videos.isEmpty();
    }

    public boolean getDisplayNews() {
        return isNotTrue(removeNews) && isNotBlank(internalGetNewsIframeUrl());
    }

    public Boolean getFieldAutocomplete() {
        return toBooleanDefaultIfNull(fieldAutocomplete, true);
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

    protected String internalGetNewsIframeUrl() {
        return getIfNull(newsIframeUrl, NUXEO_NEWS_URL);
    }

    public String getNewsIframeUrl() {
        if (newsIframeFullUrl == null) {
            UriBuilder newsIFrameBuilder = UriBuilder.fromPath(internalGetNewsIframeUrl());
            if (NUXEO_NEWS_URL.equals(internalGetNewsIframeUrl())) {
                newsIFrameBuilder.queryParam(Environment.PRODUCT_VERSION,
                        Framework.getProperty(Environment.PRODUCT_VERSION))
                                 .queryParam(Environment.DISTRIBUTION_VERSION,
                                         Framework.getProperty(Environment.DISTRIBUTION_VERSION))
                                 .queryParam(Environment.DISTRIBUTION_PACKAGE,
                                         Framework.getProperty(Environment.DISTRIBUTION_PACKAGE));
            }
            newsIframeFullUrl = newsIFrameBuilder.build().toString();
        }
        return URLDecoder.decode(newsIframeFullUrl, UTF_8);
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

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (LoginScreenConfig) o;
        var merged = new LoginScreenConfig();
        merged.newsIframeUrl = getIfNull(other.newsIframeUrl, newsIframeUrl);
        merged.headerStyle = getIfNull(other.headerStyle, headerStyle);
        merged.footerStyle = getIfNull(other.footerStyle, footerStyle);
        merged.bodyBackgroundStyle = getIfNull(other.bodyBackgroundStyle, bodyBackgroundStyle);
        merged.loginBoxBackgroundStyle = getIfNull(other.loginBoxBackgroundStyle, loginBoxBackgroundStyle);
        merged.loginBoxWidth = getIfNull(other.loginBoxWidth, loginBoxWidth);
        merged.disableBackgroundSizeCover = getIfNull(other.disableBackgroundSizeCover, disableBackgroundSizeCover);
        merged.logoAlt = getIfNull(other.logoAlt, logoAlt);
        merged.logoHeight = getIfNull(other.logoHeight, logoHeight);
        merged.logoUrl = getIfNull(other.logoUrl, logoUrl);
        merged.logoWidth = getIfNull(other.logoWidth, logoWidth);
        merged.fieldAutocomplete = getIfNull(other.fieldAutocomplete, fieldAutocomplete);
        merged.videos = getIfNull(other.videos, videos);
        merged.loop = getIfNull(other.loop, loop);
        merged.removeNews = getIfNull(other.removeNews, removeNews);
        merged.muted = getIfNull(other.muted, muted);
        merged.loginButtonBackgroundColor = getIfNull(other.loginButtonBackgroundColor, loginButtonBackgroundColor);
        merged.backgroundImage = getIfNull(other.backgroundImage, backgroundImage);

        // handle providers merge
        var providersMap = new HashMap<String, LoginProviderLink>();
        emptyIfNull(providers).forEach(provider -> providersMap.put(provider.getName(), provider));
        emptyIfNull(other.providers).forEach(
                provider -> providersMap.compute(provider.getName(), (name, previousProvider) -> {
                    if (previousProvider == null) {
                        return provider;
                    } else if (provider.remove) {
                        return null;
                    } else {
                        return previousProvider.merge(provider);
                    }
                }));
        merged.providers = new ArrayList<>(providersMap.values());

        // handle startupPages merge
        merged.startupPages = new HashMap<>(emptyIfNull(startupPages));
        emptyIfNull(other.startupPages).forEach(
                (key, value) -> merged.startupPages.merge(key, value, LoginStartupPage::merge));

        merged.defaultLocale = getIfNull(other.defaultLocale, defaultLocale);

        var supportedLocalesSet = new HashSet<String>();
        if (!Boolean.FALSE.equals(other.isAppendSupportedLocales())) { // true by default
            supportedLocalesSet.addAll(emptyIfNull(supportedLocales));
        }
        supportedLocalesSet.addAll(emptyIfNull(other.supportedLocales));
        merged.supportedLocales = new ArrayList<>(supportedLocalesSet);
        return merged;
    }
}
