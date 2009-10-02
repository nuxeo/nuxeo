package org.nuxeo.opensocial.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.BasicSecurityTokenDecoder;
import org.apache.shindig.common.crypto.BlobCrypterException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.google.inject.Inject;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.NuxeoRunner;
import com.leroymerlin.corp.fr.nuxeo.portal.testing.TestRuntimeHarness;

@RunWith(NuxeoRunner.class)
public class OpenSocialServletTest {

    private OpenSocialService service;

    private WebClient webClient;
    private CollectingAlertHandler alertHandler;
    private BasicSecurityToken token;

    public static URL getResource(String resource) {
        return OpenSocialServletTest.class.getClassLoader().getResource(
                resource);
    }

    @Inject
    public OpenSocialServletTest(TestRuntimeHarness harness, UserManager um)
            throws Exception {

        assertNotNull(um);

        //TODO: Build a harness for nuxeo-test-util
        harness.loadProperties(OpenSocialServletTest.class.getClassLoader()
                .getResourceAsStream("shindig.properties"));

        harness.deployBundle("org.nuxeo.opensocial.service");

        service = Framework.getService(OpenSocialService.class);
        assertNotNull(service);


        File configDir = new File(harness.getWorkingDir(), "config");
        configDir.mkdirs();

        InputStream jettyConfig = getResource("jetty.xml").openStream();
        File destFile = new File(configDir + "/jetty.xml");
        FileOutputStream nuxeoConfig = new FileOutputStream(destFile);
        FileUtils.copy(jettyConfig, nuxeoConfig);

        harness.deployBundle("org.nuxeo.runtime.jetty");
    }

    @Before
    public void setup() throws Exception {
        webClient = new WebClient();
        // NicelyResynchronizingAjaxController changes XHR calls from
        // asynchronous
        // to synchronous, saving the test from needing to wait or sleep for XHR
        // completion.
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        alertHandler = new CollectingAlertHandler();
        webClient.setAlertHandler(alertHandler);
        token = createToken("Guest", "Guest");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testRenderer() {
        assertNotNull(service.getGadgetSpecFactory());
    }

    @Test
    public void testJSServlet() throws FailingHttpStatusCodeException,
            MalformedURLException, IOException {
        String url = "http://localhost:11111/nuxeo/opensocial/gadgets/js/rpc.js?debug=1&c=1";
        Page p = webClient.getPage(url);
        assertEquals("Failed to load test resource " + url, 200, p
                .getWebResponse().getStatusCode());
    }

    @Test
    public void basicTest() throws Exception {
        BasicSecurityTokenDecoder decoder = new BasicSecurityTokenDecoder();
        String url = "http://localhost:11111/nuxeo/opensocial/social/rest/people/Guest/@self?container=test-container&st="
                + URLEncoder.encode(decoder.encodeToken(token), "UTF-8");
        Page page = webClient.getPage(url);
        System.err.println(page.getWebResponse().getContentAsString());
    }

    private BasicSecurityToken createToken(String owner, String viewer)
            throws BlobCrypterException {
        return new BasicSecurityToken(owner, viewer, "test", "domain",
                "appUrl", "1");
    }

}
