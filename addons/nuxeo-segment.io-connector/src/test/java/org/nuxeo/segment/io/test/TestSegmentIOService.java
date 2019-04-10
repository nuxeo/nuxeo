/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.segment.io.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.segment.io.SegmentIO;
import org.nuxeo.segment.io.SegmentIOComponent;
import org.nuxeo.segment.io.SegmentIODataWrapper;
import org.nuxeo.segment.io.SegmentIOMapper;
import org.nuxeo.segment.io.SegmentIOUserFilter;

import com.github.segmentio.models.Options;
import com.google.inject.Inject;

@Deploy("org.nuxeo.segmentio.connector")
@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.segmentio.connector:segmentio-contribs.xml")
@Features(CoreFeature.class)
public class TestSegmentIOService {

    @Inject
    protected EventProducer eventProducer;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    protected void waitForAsyncCompletion() {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    protected void sendAuthenticationEvent(NuxeoPrincipal principal) {
        Map<String, Serializable> props = new HashMap<>();
        props.put("AuthenticationPlugin", "FakeAuth");
        props.put("LoginPlugin", "FakeLogin");
        EventContext ctx = new UnboundEventContext(principal, props);
        eventProducer.fireEvent(ctx.newEvent("loginSuccess"));
    }

    @Test
    public void checkDeploy() {
        SegmentIO service = Framework.getService(SegmentIO.class);
        Assert.assertNotNull(service);

        Map<String, List<SegmentIOMapper>> allMappers = service.getAllMappers();
        Assert.assertTrue(allMappers.size() > 0);
        Assert.assertTrue(allMappers.containsKey("loginSuccess"));
        Assert.assertTrue(allMappers.get("loginSuccess").size() > 0);
        Assert.assertTrue(allMappers.get("loginSuccess").get(0).getName().equals("testIdentify"));

    }

    @Test
    public void shouldRunIdentifyAndGroupOnLogin() throws Exception {

        SegmentIOComponent component = (SegmentIOComponent) Framework.getService(SegmentIO.class);
        Assert.assertNotNull(component);

        component.getTestData().clear();

        UserPrincipal principal = (UserPrincipal) session.getPrincipal();
        principal.setEmail("john@test.com");
        principal.setCompany("testCorp");
        sendAuthenticationEvent(principal);
        waitForAsyncCompletion();

        List<Map<String, Object>> testData = component.getTestData();

        Assert.assertEquals(2, testData.size());

        Map<String, Object> data = testData.remove(0);
        Assert.assertEquals(6, data.size());
        Assert.assertEquals("identify", data.get("action"));
        // check metadata
        Assert.assertEquals("FakeAuth", data.get("plugin"));
        Assert.assertEquals(principal.getCompany(), data.get("company"));
        Assert.assertEquals(principal.getEmail(), data.get(SegmentIODataWrapper.EMAIL_KEY));
        Assert.assertEquals(principal.getName(), data.get(SegmentIODataWrapper.PRINCIPAL_KEY));
        // check options
        Options options = (Options) data.get("options");
        Assert.assertEquals(1, options.getIntegrations().size());
        Assert.assertNotNull(options.getTimestamp());

        Map<String, Object> grpdata = testData.remove(0);
        Assert.assertEquals(5, grpdata.size());
        Assert.assertEquals("group", grpdata.get("action"));
        // check metadata
        Assert.assertEquals("testGroup", grpdata.get("id"));
        Assert.assertEquals("TestGroup", grpdata.get("name"));
        Assert.assertEquals(principal.getName(), data.get(SegmentIODataWrapper.PRINCIPAL_KEY));
        // check options
        options = (Options) grpdata.get("options");
        Assert.assertEquals(1, options.getIntegrations().size());
        Assert.assertNotNull(options.getTimestamp());

    }

    @Test
    public void shouldRunTrackOnDocEvent() {
        SegmentIOComponent component = (SegmentIOComponent) Framework.getService(SegmentIO.class);
        Assert.assertNotNull(component);

        component.getTestData().clear();

        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:title", "Test Doc");
        session.createDocument(doc);
        session.save();
        waitForAsyncCompletion();

        List<Map<String, Object>> testData = component.getTestData();

        Assert.assertEquals(1, testData.size());

        Map<String, Object> data = testData.remove(0);
        Assert.assertEquals("track", data.get("action"));
        // check metadata
        Assert.assertEquals("documentCreated", data.get("eventName"));
        Assert.assertEquals("Test Doc", data.get("title"));
        Assert.assertEquals("File", data.get("type"));
        Assert.assertEquals(session.getPrincipal().getName(), data.get(SegmentIODataWrapper.PRINCIPAL_KEY));
        // check options
        Options options = (Options) data.get("options");
        Assert.assertEquals(1, options.getIntegrations().size());
        Assert.assertNotNull(options.getTimestamp());
    }

    @Test
    public void shouldBeAbleToTrackScreenAndPage() {
        SegmentIOComponent component = (SegmentIOComponent) Framework.getService(SegmentIO.class);
        component.screen(session.getPrincipal(), "Login", new HashMap<>());

        List<Map<String, Object>> testData = component.getTestData();
        Assert.assertEquals(1, testData.size());
        Map<String, Object> data = testData.remove(0);
        Assert.assertEquals("screen", data.get("action"));
        Assert.assertEquals("Login", data.get("eventName"));

        component.page(session.getPrincipal(), "login.jsp", new HashMap<>());
        testData = component.getTestData();
        Assert.assertEquals(1, testData.size());
        data = testData.remove(0);
        Assert.assertEquals("page", data.get("action"));
        Assert.assertEquals("login.jsp", data.get("eventName"));

    }

    @Test
    public void shouldHaveDefaultIntegrationsConfig() {
        SegmentIO sio = Framework.getService(SegmentIO.class);
        Assert.assertNotNull(sio);

        Map<String, Boolean> integrations = sio.getIntegrations();
        Assert.assertNotNull(integrations);
        Assert.assertTrue(integrations.get("Marketo"));
    }

    @Test
    public void shouldHaveUserFilter() {

        SegmentIO sio = Framework.getService(SegmentIO.class);
        Assert.assertNotNull(sio);

        SegmentIOUserFilter filters = sio.getUserFilters();

        Assert.assertNotNull(filters);

        Assert.assertFalse(filters.isEnableAnonymous());

        Assert.assertTrue(filters.getBlackListedUsers().contains("RemoteConnectInstance"));

        Assert.assertTrue(filters.canTrack(session.getPrincipal().getName()));

    }

}
