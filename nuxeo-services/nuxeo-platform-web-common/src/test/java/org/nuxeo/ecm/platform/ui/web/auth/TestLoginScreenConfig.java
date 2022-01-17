/*
 * (C) Copyright 2010-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.auth;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.common.Environment.DISTRIBUTION_PACKAGE;
import static org.nuxeo.common.Environment.DISTRIBUTION_VERSION;
import static org.nuxeo.common.Environment.PRODUCT_VERSION;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginStartupPage;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginVideo;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.sun.jersey.api.uri.UriComponent;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/authentication-framework.xml")
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/authentication-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig.xml")
public class TestLoginScreenConfig {

    @Before
    public void setUp() throws Exception {
        Properties properties = Framework.getProperties();
        properties.setProperty(PRODUCT_VERSION, "LTS-2015");
        properties.setProperty(DISTRIBUTION_VERSION, "7.10");
        properties.setProperty(DISTRIBUTION_PACKAGE, "zip");
    }

    private PluggableAuthenticationService getAuthService() {
        PluggableAuthenticationService authService;
        authService = (PluggableAuthenticationService) Framework.getRuntime()
                                                                .getComponent(PluggableAuthenticationService.NAME);

        return authService;
    }

    @Inject
    protected HotDeployer hotDeployer;

    @Inject
    public RuntimeHarness runtimeHarness;

    @Test
    public void testSimpleConfig() {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        assertEquals("#CCCCCC", config.getHeaderStyle());
        assertNull(config.getDisableBackgroundSizeCover());
        assertEquals(3, config.getProviders().size());
        assertTrue(config.hasVideos());
        assertEquals(2, config.getVideos().size());
        assertTrue(config.getDisplayNews());
        assertTrue(config.getDisplayMobileBanner());

        LoginVideo loginVideo = config.getVideos().get(0);
        assertTrue(isNotBlank(loginVideo.getType()));
        assertTrue(isNotBlank(loginVideo.getSrc()));

        Map<String, LoginStartupPage> startupPages = config.getStartupPages();
        assertNotNull(startupPages);
        assertEquals(2, startupPages.size());
        LoginStartupPage jsf = startupPages.get("jsf");
        assertNotNull(jsf);
        assertEquals(10, jsf.getPriority());
        assertEquals("nxstartup.faces", jsf.getPath());
        LoginStartupPage other = startupPages.get("other");
        assertNotNull(other);
        assertEquals(5, other.getPriority());
        assertEquals("other.html", other.getPath());

        // Highest priority wins
        assertEquals("nxstartup.faces", LoginScreenHelper.getStartupPagePath());

        // Get all the startup page paths
        assertEquals(Arrays.asList("nxstartup.faces", "other.html"), LoginScreenHelper.getStartupPagePaths());

        assertEquals("es_ES", config.getDefaultLocale());
        assertEquals(2, config.getSupportedLocales().size());
        assertTrue(config.getSupportedLocales().contains("es_ES"));
        assertTrue(config.getSupportedLocales().contains("fr"));
    }

    @Test
    public void testVariableExpension() {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        String style = config.getBodyBackgroundStyle();
        assertTrue(style.contains("/nuxeo/img/login_bg.png"));
    }

    @Test
    public void testMergeConfig() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        assertEquals("#CCCCCC", config.getHeaderStyle());
        assertEquals("Something", config.getFooterStyle());
        assertEquals(3, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("facebook"));
        assertNotNull(config.getProvider("linkedin"));
        assertTrue(config.getDisplayNews());
        assertTrue(config.getDisplayMobileBanner());
        assertNull(config.getDisableBackgroundSizeCover());

        assertEquals("XXXX", config.getProvider("google").getLink(null, null));

        assertEquals("es_ES", config.getDefaultLocale());
        assertEquals(2, config.getSupportedLocales().size());
        assertTrue(config.getSupportedLocales().contains("es_ES"));
        assertTrue(config.getSupportedLocales().contains("fr"));

        hotDeployer.deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge.xml");

        authService = getAuthService();
        config = authService.getLoginScreenConfig();
        assertNotNull(config);

        assertEquals("#DDDDDD", config.getHeaderStyle());
        assertEquals("Something", config.getFooterStyle());
        assertFalse(config.getDisplayNews());
        assertFalse(config.getDisplayMobileBanner());
        assertEquals(2, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("linkedin"));
        assertNull(config.getProvider("facebook"));
        assertEquals("News", config.getProvider("google").getLink(null, null));
        assertEquals(Boolean.TRUE, config.getDisableBackgroundSizeCover());

        assertTrue(config.hasVideos());

        assertTrue(config.getVideoMuted());
        assertFalse(config.getVideoLoop());

        Map<String, LoginStartupPage> startupPages = config.getStartupPages();
        assertNotNull(startupPages);
        assertEquals(3, startupPages.size());
        LoginStartupPage jsf = startupPages.get("jsf");
        assertNotNull(jsf);
        assertEquals(10, jsf.getPriority());
        assertEquals("nxstartup.faces", jsf.getPath());
        LoginStartupPage other = startupPages.get("other");
        assertNotNull(other);
        assertEquals(8, other.getPriority());
        assertEquals("merged.html", other.getPath());
        LoginStartupPage web = startupPages.get("web");
        assertNotNull(web);
        assertEquals(100, web.getPriority());
        assertEquals("ui/", web.getPath());

        // Highest priority wins
        assertEquals("ui/", LoginScreenHelper.getStartupPagePath());

        // Get all the startup page paths
        assertEquals(Arrays.asList("ui/", "nxstartup.faces", "merged.html"), LoginScreenHelper.getStartupPagePaths());

        assertEquals("fr", config.getDefaultLocale());
        assertEquals(3, config.getSupportedLocales().size());
        assertTrue(config.getSupportedLocales().contains("es_ES"));
        assertTrue(config.getSupportedLocales().contains("fr"));
        assertTrue(config.getSupportedLocales().contains("de"));

        hotDeployer.undeploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge.xml");
    }

    /**
     * Non-regression test for NXP-23786.
     */
    @Test
    public void testMergeConfigLanguages() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();

        assertNotNull(config);
        assertEquals("#CCCCCC", config.getHeaderStyle());

        assertEquals("es_ES", config.getDefaultLocale());
        assertEquals(2, config.getSupportedLocales().size());
        assertTrue(config.getSupportedLocales().contains("es_ES"));
        assertTrue(config.getSupportedLocales().contains("fr"));

        hotDeployer.deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge2.xml");

        authService = getAuthService();
        config = authService.getLoginScreenConfig();

        assertNotNull(config);
        assertEquals("#DDDDDD", config.getHeaderStyle());

        assertEquals("es_ES", config.getDefaultLocale());
        assertEquals(2, config.getSupportedLocales().size());
        assertTrue(config.getSupportedLocales().contains("es_ES"));
        assertTrue(config.getSupportedLocales().contains("fr"));

        hotDeployer.undeploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge2.xml");
    }

    /**
     * Non-regression test for NXP-23786.
     */
    @Test
    public void testMergeConfigLanguagesNoAppend() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();

        assertNotNull(config);
        assertEquals("#CCCCCC", config.getHeaderStyle());

        assertEquals("es_ES", config.getDefaultLocale());
        assertEquals(2, config.getSupportedLocales().size());
        assertTrue(config.getSupportedLocales().contains("es_ES"));
        assertTrue(config.getSupportedLocales().contains("fr"));

        hotDeployer.deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge3.xml");

        authService = getAuthService();
        config = authService.getLoginScreenConfig();

        assertNotNull(config);
        assertEquals("#DDDDDD", config.getHeaderStyle());

        assertEquals("es_ES", config.getDefaultLocale());
        assertEquals(2, config.getSupportedLocales().size());
        assertTrue(config.getSupportedLocales().contains("es_ES"));
        // de replaced fr
        assertTrue(config.getSupportedLocales().contains("de"));

        hotDeployer.undeploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge3.xml");
    }

    @Test
    public void testHelper() throws Exception {
        LoginScreenConfig config = LoginScreenHelper.getConfig();
        assertNotNull(config);

        assertEquals("#CCCCCC", config.getHeaderStyle());
        assertEquals("Something", config.getFooterStyle());
        assertEquals(3, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("facebook"));
        assertNotNull(config.getProvider("linkedin"));
        assertEquals("XXXX", config.getProvider("google").getLink(null, null));

        LoginStartupPage defaultStartupPage = LoginScreenHelper.getDefaultStartupPage(config);
        assertNotNull(defaultStartupPage);
        assertEquals(10, defaultStartupPage.getPriority());
        assertEquals("nxstartup.faces", defaultStartupPage.getPath());

        // dynamically register some login providers
        LoginScreenConfig googleConfig = LoginScreenHelper.registerSingleProviderLoginScreenConfig("google", "XXX",
                "new", null, null, null);
        LoginScreenConfig otherConfig = LoginScreenHelper.registerSingleProviderLoginScreenConfig("OuvertId", "AAA",
                "BBB", null, null, null);
        // reload config from registry
        config = LoginScreenHelper.getConfig();

        assertEquals(4, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("facebook"));
        assertNotNull(config.getProvider("linkedin"));
        assertNotNull(config.getProvider("OuvertId"));
        assertEquals("new", config.getProvider("google").getLink(null, null));
        assertEquals("BBB", config.getProvider("OuvertId").getLink(null, null));

        // dynamically unregister the login providers
        LoginScreenHelper.unregisterLoginScreenConfig(googleConfig);
        LoginScreenHelper.unregisterLoginScreenConfig(otherConfig);
        // reload config from registry
        config = LoginScreenHelper.getConfig();

        assertEquals(3, config.getProviders().size());
        assertNotNull(config.getProvider("google"));
        assertNotNull(config.getProvider("facebook"));
        assertNotNull(config.getProvider("linkedin"));
        assertEquals("XXXX", config.getProvider("google").getLink(null, null));
    }

    /**
     * Non-regression test for NXP-25837.
     */
    @Test
    public void testLoginProviderRegistrationNotOverriddenByContribution() throws Exception {
        LoginScreenConfig config = LoginScreenHelper.getConfig();
        assertNotNull(config);
        assertEquals(3, config.getProviders().size());

        // dynamically register a login provider
        LoginScreenConfig loginProviderConfig = LoginScreenHelper.registerSingleProviderLoginScreenConfig("OuvertId",
                "AAA", "BBB", null, null, null);
        // reload config from registry
        config = LoginScreenHelper.getConfig();
        assertEquals(4, config.getProviders().size());
        assertNotNull(config.getProvider("OuvertId"));
        assertEquals("BBB", config.getProvider("OuvertId").getLink(null, null));

        // deploy a login screen contribution
        // Note that we cannot use the HotDeployer because it doesn't work like the hot reload at runtime, the strategy
        // is different: at runtime we are using "standby", so the components are not deactivated/activated whereas in
        // the tests they are.
        // In any case, what we really want to test is the deployment of a login screen configuration contribution after
        // having manually registered a login provider.
        runtimeHarness.deployContrib("org.nuxeo.ecm.platform.web.common.test",
                "OSGI-INF/test-loginscreenconfig-merge2.xml");

        // check that the dynamically registered login provider is still taken into account
        config = LoginScreenHelper.getConfig();
        assertEquals(4, config.getProviders().size());
        assertNotNull(config.getProvider("OuvertId"));
        assertEquals("BBB", config.getProvider("OuvertId").getLink(null, null));

        runtimeHarness.undeployContrib("org.nuxeo.ecm.platform.web.common.test",
                "OSGI-INF/test-loginscreenconfig-merge2.xml");
        LoginScreenHelper.unregisterLoginScreenConfig(loginProviderConfig);
    }

    @Test
    public void iCanGetNewsIframeURL() throws Exception {

        LoginScreenConfig config = new LoginScreenConfig();
        String strUrl = config.getNewsIframeUrl();
        if (!strUrl.startsWith("http")) {
            strUrl = "http:" + strUrl;
        }
        URL url = new URL(strUrl);

        MultivaluedMap<String, String> query = UriComponent.decodeQuery(url.getQuery(), true);

        assertThat(query.keySet()).contains(PRODUCT_VERSION, DISTRIBUTION_VERSION, DISTRIBUTION_PACKAGE);
        assertThat(query.get(PRODUCT_VERSION)).contains("LTS-2015");
        assertThat(query.get(DISTRIBUTION_VERSION)).contains("7.10");
        assertThat(query.get(DISTRIBUTION_PACKAGE)).contains("zip");

        // Testing with contribution
        config.setNewsIframeUrl("http://example.com?why=testing");

        strUrl = config.getNewsIframeUrl();
        url = new URL("http:" + strUrl);
        query = UriComponent.decodeQuery(url.getQuery(), true);

        assertThat(query.keySet()).contains("why");
        assertFalse(query.containsKey(PRODUCT_VERSION));
        assertThat(query.get("why")).contains("testing");
    }

    // NXP-30831
    @Test
    public void testRemoveNewsDisplayMobileBannerMerge() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        LoginScreenConfig config = authService.getLoginScreenConfig();

        assertNotNull(config);
        assertTrue(config.getDisplayNews());
        assertTrue(config.getDisplayMobileBanner());
        assertEquals("someurl", config.getNewsIframeUrl());

        hotDeployer.deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge-removeNews-displayMobileBanner.xml");

        authService = getAuthService();
        config = authService.getLoginScreenConfig();

        assertNotNull(config);
        assertFalse(config.getDisplayNews());
        assertFalse(config.getDisplayMobileBanner());
        assertEquals("someurl", config.getNewsIframeUrl());

        hotDeployer.deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig-merge-removeNews-displayMobileBanner2.xml");

        authService = getAuthService();
        config = authService.getLoginScreenConfig();

        assertNotNull(config);
        assertFalse(config.getDisplayNews());
        assertFalse(config.getDisplayMobileBanner());
        assertEquals("aNewURLWhichShouldntCauseNewsActivationAsRemoveNewsIsTrueInPreviousContrib", config.getNewsIframeUrl());
    }

    // this test should be the last one because it un-deploys a contribution deployed by annotation on the class
    @Test
    public void testUndeployConfig() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        LoginScreenConfig config = authService.getLoginScreenConfig();
        assertNotNull(config);

        hotDeployer.undeploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-loginscreenconfig.xml");

        authService = getAuthService();
        config = authService.getLoginScreenConfig();
        assertNull(config);
    }
}
