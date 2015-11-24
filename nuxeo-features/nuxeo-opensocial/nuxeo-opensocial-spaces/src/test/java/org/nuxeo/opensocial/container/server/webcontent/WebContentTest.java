package org.nuxeo.opensocial.container.server.webcontent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.spaces.api.Constants.UNIT_DOCUMENT_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.opensocial.container.OpenSocialContainerFeature;
import org.nuxeo.opensocial.container.server.service.WebContentSaverService;
import org.nuxeo.opensocial.container.shared.webcontent.HTMLData;
import org.nuxeo.opensocial.container.shared.webcontent.PictureData;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(OpenSocialContainerFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class WebContentTest {
    @Inject
    CoreSession session;

    @Inject
    WebContentSaverService service;

    public DocumentModel createNxUnit() throws ClientException {
        DocumentModel unit = session.createDocumentModel("/", "unit",
                UNIT_DOCUMENT_TYPE);
        unit = session.createDocument(unit);
        session.save();

        return unit;
    }

    private WebContentData initTest(WebContentData data) throws ClientException {
        DocumentModel unit = createNxUnit();

        Map<String, String> preferences = new HashMap<String, String>();
        preferences.put(WebContentData.WC_TITLE_COLOR, "blue");

        data.setUnitId(unit.getId());
        data.setPosition(2);
        data.setTitle("title");
        data.setName("test");
        data.setIsInAPortlet(true);
        data.setIsCollapsed(true);
        data.setPreferences(preferences);

        return data;
    }

    @Test
    public void iCanSetPropertyForAnAbstractGadget() throws Exception {
        DocumentModel unit = createNxUnit();

        HTMLData data = (HTMLData) initTest(new HTMLData());

        data = (HTMLData) service.create(data, unit.getId(), session);
        session.save();

        WebContentData dataToTest = service.read(session.getDocument(new IdRef(
                data.getId())), session);

        assertNotNull(dataToTest.getId());
        assertEquals(data.getName(), dataToTest.getName());
        assertEquals("title", dataToTest.getTitle());
        assertNotNull(dataToTest.getUnitId());
        assertEquals(2, dataToTest.getPosition());
        assertEquals(true, dataToTest.isInAPorlet());
        assertEquals(true, dataToTest.isCollapsed());
        assertEquals(1, dataToTest.getPreferences().size());
        assertEquals("blue", dataToTest.getPreferences().get(
                WebContentData.WC_TITLE_COLOR.toString()));
        assertEquals(session.getPrincipal().getName(), data.getViewer());
    }

    @Test
    public void iCanCreateAHTMLGadget() throws Exception {
        DocumentModel unit = createNxUnit();

        HTMLData data = (HTMLData) initTest(new HTMLData());

        data.setHtml("<div></div>");
        data.setHtmlTitle("test");

        data = (HTMLData) service.create(data, unit.getId(), session);

        HTMLData dataToTest = (HTMLData) service.read(
                session.getDocument(new IdRef(data.getId())), session);

        assertEquals("<div></div>", dataToTest.getHtml());
        assertEquals("test", dataToTest.getHtmlTitle());
    }

    @Test
    public void iCanCreateAPictureGadget() throws Exception {
        DocumentModel unit = createNxUnit();

        PictureData data = (PictureData) initTest(new PictureData());

        data.setPictureTitle("picture title");
        data.setPictureLegend("legend");
        data.setPictureLink("http://");

        data = (PictureData) service.create(data, unit.getId(), session);

        PictureData dataToTest = (PictureData) service.read(
                session.getDocument(new IdRef(data.getId())), session);

        assertEquals("picture title", dataToTest.getPictureTitle());
        assertEquals("legend", dataToTest.getPictureLegend());
        assertEquals("http://", dataToTest.getPictureLink());
    }
}
