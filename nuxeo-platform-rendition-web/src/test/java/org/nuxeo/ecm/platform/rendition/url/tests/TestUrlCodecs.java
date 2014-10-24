package org.nuxeo.ecm.platform.rendition.url.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.rendition.url.DocumentRenditionCodec;
import org.nuxeo.ecm.platform.rendition.url.RenditionBasedCodec;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;

public class TestUrlCodecs {

    protected String validateUrl(String repo, String path, String rendition) {
        DocumentLocation dl = new DocumentLocationImpl(repo, new IdRef("id"),
                new PathRef(path));
        DocumentView dv = new DocumentViewImpl(dl, rendition);

        RenditionBasedCodec codec = new RenditionBasedCodec();
        codec.setPrefix("testingUrl");
        String url = codec.getUrlFromDocumentView(dv);

        DocumentView dv2 = codec.getDocumentViewFromUrl(url);

        assertNotNull(dv2);
        assertEquals(repo, dv2.getDocumentLocation().getServerName());
        assertEquals(path, dv2.getDocumentLocation().getPathRef().toString());
        assertEquals(rendition,
                dv2.getParameter(RenditionBasedCodec.RENDITION_PARAM_NAME));
        assertEquals(RenditionBasedCodec.RENDITION_VIEW_ID, dv2.getViewId());

        return url;
    }

    @Test
    public void testURLEncodeDecode() {
        String url = validateUrl("default", "/some/path", "rendition");
        url = validateUrl("default", "/some/path", "rendition name");
        assertTrue(url.contains("%20"));
        url = validateUrl("default", "/some/path", "rendition éèa");
        assertTrue(url.contains("%A8"));
        url = validateUrl("default", "/some/path", "rendition/name");
        assertTrue(url.contains("%2F"));
    }

    @Test
    public void testRenditionUrl() {
        String renditionName = "My Rendition Name";
        String docPath = "/some/path";
        String docId = "dbefd5a0-35ee-4ed2-a023-6817714f32cf";

        DocumentLocation documentLocation = new DocumentLocationImpl("default",
                new IdRef(docId), new PathRef(docPath));
        DocumentView docView = new DocumentViewImpl(documentLocation,
                renditionName);

        RenditionBasedCodec codec = new DocumentRenditionCodec();
        String url = codec.getUrlFromDocumentView(docView);
        assertEquals("nxrendition/default/some/path@My%20Rendition%20Name", url);

        DocumentView docView2 = codec.getDocumentViewFromUrl(url);
        assertEquals(renditionName,
                docView2.getParameter(RenditionBasedCodec.RENDITION_PARAM_NAME));
        assertEquals(RenditionBasedCodec.RENDITION_VIEW_ID,
                docView2.getViewId());

        // force version to get a URL based on docid
        docView.addParameter("version", "true");
        url = codec.getUrlFromDocumentView(docView);
        assertEquals(
                "nxrendition/default/dbefd5a0-35ee-4ed2-a023-6817714f32cf/My%20Rendition%20Name?version=true",
                url);

        docView2 = codec.getDocumentViewFromUrl(url);
        assertEquals(renditionName,
                docView2.getParameter(RenditionBasedCodec.RENDITION_PARAM_NAME));
        assertEquals(RenditionBasedCodec.RENDITION_VIEW_ID,
                docView2.getViewId());
    }

}
