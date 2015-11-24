package org.nuxeo.opensocial.container.server.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.spaces.api.Constants.UNIT_DOCUMENT_TYPE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.opensocial.container.OpenSocialContainerFeature;
import org.nuxeo.opensocial.container.server.webcontent.api.WebContentAdapter;
import org.nuxeo.opensocial.container.server.webcontent.gadgets.html.HTMLAdapter;
import org.nuxeo.opensocial.container.shared.webcontent.HTMLData;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(OpenSocialContainerFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class WebContentSaverServiceTest {
    @Inject
    CoreSession session;

    @Inject
    WebContentSaverService service;

    @Test
    public void serviceTest() throws Exception {
        assertNotNull(service);
    }

    public DocumentModel createNxUnit() throws ClientException {
        DocumentModel unit = session.createDocumentModel("/", "unit",
                UNIT_DOCUMENT_TYPE);
        unit = session.createDocument(unit);
        session.save();

        return unit;
    }

    private HTMLData createHTMLData(String unitId) {
        HTMLData html = new HTMLData();

        html.setUnitId(unitId);
        html.setPosition(2);
        html.setHtml("<div>");
        html.setTitle("title");
        html.setName("test");
        html.setIsInAPortlet(true);
        html.setIsCollapsed(true);

        return html;
    }

    @Test
    public void creatorServiceTest() throws Exception {
        DocumentModel unit = createNxUnit();
        HTMLData html = createHTMLData(unit.getId());
        html = (HTMLData) service.create(html, unit.getId(), session);
        session.save();

        IdRef htmlIdRef = new IdRef(html.getId());
        DocumentModel doc = session.getDocument(htmlIdRef);
        HTMLAdapter adapter = (HTMLAdapter) doc.getAdapter(WebContentAdapter.class);
        HTMLData dataFromNuxeo = adapter.getData();

        assertEquals(html.getId(), doc.getId());
        assertEquals(1, session.getChildren(unit.getRef()).size());
        assertTrue(session.exists(htmlIdRef));
        assertEquals("test", dataFromNuxeo.getName());

        assertEquals(2, dataFromNuxeo.getPosition());
        assertEquals("title", dataFromNuxeo.getTitle());
        assertEquals("test", dataFromNuxeo.getName());
        assertEquals(unit.getId(), dataFromNuxeo.getUnitId());
        assertEquals(true, dataFromNuxeo.isInAPorlet());
        assertEquals(true, dataFromNuxeo.isCollapsed());
    }

    @Test
    public void readServiceTest() throws Exception {
        DocumentModel unit = createNxUnit();
        HTMLData html = createHTMLData(unit.getId());

        HTMLData dataSaved = (HTMLData) service.create(html, unit.getId(),
                session);
        session.save();

        HTMLData dataFromNuxeo = (HTMLData) service.read(
                session.getDocument(new IdRef(dataSaved.getId())), session);

        assertNotNull(dataFromNuxeo);
        assertTrue(dataFromNuxeo instanceof HTMLData);

        assertEquals(2, dataFromNuxeo.getPosition());
        assertEquals("<div>", dataFromNuxeo.getHtml());
        assertEquals("title", dataFromNuxeo.getTitle());
        assertEquals("test", dataFromNuxeo.getName());
        assertEquals(unit.getId(), dataFromNuxeo.getUnitId());
        assertEquals(true, dataFromNuxeo.isInAPorlet());
        assertEquals(true, dataFromNuxeo.isCollapsed());
    }

    @Test
    public void updateServiceTest() throws Exception {
        DocumentModel unit = createNxUnit();
        HTMLData html = createHTMLData(unit.getId());
        html = (HTMLData) service.create(html, unit.getId(), session);
        session.save();

        html.setHtml("");
        service.update(html, session);
        session.save();

        HTMLData dataFromNuxeo = (HTMLData) service.read(
                session.getDocument(new IdRef(html.getId())), session);

        assertEquals("", dataFromNuxeo.getHtml());
    }

    @Test
    public void deleteServiceTest() throws Exception {
        DocumentModel unit = createNxUnit();
        HTMLData html = createHTMLData(unit.getId());
        html = (HTMLData) service.create(html, unit.getId(), session);
        session.save();

        service.delete(html, session);
        session.save();

        assertEquals(0, session.getChildren(unit.getRef()).size());
    }
}
