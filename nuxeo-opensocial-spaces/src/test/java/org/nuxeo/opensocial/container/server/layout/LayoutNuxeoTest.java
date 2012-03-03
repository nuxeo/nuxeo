package org.nuxeo.opensocial.container.server.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.spaces.api.Constants.SPACE_DOCUMENT_TYPE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.opensocial.container.OpenSocialContainerFeature;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractComponent;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIFixedBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(OpenSocialContainerFeature.class)
public class LayoutNuxeoTest {
    @Inject
    CoreSession session;

    private YUIComponentZone createYUIZone(YUITemplate template) {
        YUIAbstractComponent zone = new YUIComponentZoneImpl(template);

        for (int i = 0; i < template.getNumberOfComponents(); i++) {
            ((YUIComponentZone) zone).addComponent(new YUIUnitImpl());
        }

        return (YUIComponentZone) zone;
    }

    private DocumentModel createNxLayout() throws Exception {
        DocumentModel createdDoc = session.createDocumentModel("/", "layout",
                SPACE_DOCUMENT_TYPE);
        return session.createDocument(createdDoc);
    }

    private DocumentModel createNxZone(YUIComponent zone) throws Exception {
        DocumentModel createdDoc = createNxLayout();

        YUILayoutAdapter layoutAdapter = createdDoc.getAdapter(YUILayoutAdapter.class);

        layoutAdapter.createZone((YUIComponentZone) zone, 0);
        createdDoc = session.saveDocument(createdDoc);

        return createdDoc;
    }

    @Test
    public void iCanCreateAZone() throws Exception {
        YUIAbstractComponent zone = (YUIAbstractComponent) createYUIZone(YUITemplate.YUI_ZT_33_33_33);
        DocumentModel createdDoc = createNxZone(zone);

        DocumentModel doc = session.getDocument(createdDoc.getRef());
        YUILayoutAdapter layout = doc.getAdapter(YUILayoutAdapter.class);
        doc = session.saveDocument(doc);

        assertEquals(
                YUITemplate.YUI_ZT_33_33_33,
                ((YUIComponentZone) layout.getLayout().getContent().getComponents().get(
                        0)).getTemplate());

        DocumentModelList children = session.getChildren(createdDoc.getRef());
        assertEquals(3, children.size());
        checkSame(zone, children);
    }

    @Test
    public void iCanUpdateAZoneWithMoreUnitsThanPreviously() throws Exception {
        YUIAbstractComponent zone = (YUIAbstractComponent) createYUIZone(YUITemplate.YUI_ZT_100);
        DocumentModel createdDoc = createNxZone(zone);

        YUILayoutAdapter adapter = createdDoc.getAdapter(YUILayoutAdapter.class);
        adapter.updateZone((YUIComponentZone) zone, 0,
                YUITemplate.YUI_ZT_33_33_33);
        createdDoc = session.saveDocument(createdDoc);

        DocumentModel doc = session.getDocument(createdDoc.getRef());
        YUILayoutAdapter layout = doc.getAdapter(YUILayoutAdapter.class);

        YUIComponent firstZone = layout.getLayout().getContent().getComponents().get(
                0);
        assertEquals(YUITemplate.YUI_ZT_33_33_33,
                ((YUIComponentZone) firstZone).getTemplate());

        DocumentModelList children = session.getChildren(createdDoc.getRef());
        assertEquals(3, children.size());
        checkSame(firstZone, children);
    }

    protected void checkSame(YUIComponent zone, DocumentModelList children) {
        int size = children.size();
        assertEquals(zone.getComponents().size(), size);
        List<String> expected = new ArrayList<String>(size);
        List<String> actual = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            expected.add(((YUIUnitImpl) zone.getComponents().get(i)).getId());
            actual.add(children.get(i).getId());
        }
        assertEquals(new HashSet<String>(expected), new HashSet<String>(actual));
    }

    @Test
    public void iCanUpdateAZoneWithLessUnitsThanPreviously() throws Exception {
        YUIComponent zone = (YUIComponent) createYUIZone(YUITemplate.YUI_ZT_33_33_33);
        DocumentModel createdDoc = createNxZone(zone);

        YUILayoutAdapter adapter = createdDoc.getAdapter(YUILayoutAdapter.class);
        adapter.updateZone((YUIComponentZone) zone, 0, YUITemplate.YUI_ZT_100);
        createdDoc = session.saveDocument(createdDoc);

        DocumentModel doc = session.getDocument(createdDoc.getRef());
        YUILayoutAdapter layout = doc.getAdapter(YUILayoutAdapter.class);

        YUIComponent firstZone = layout.getLayout().getContent().getComponents().get(
                0);
        assertEquals(YUITemplate.YUI_ZT_100,
                ((YUIComponentZone) firstZone).getTemplate());

        DocumentModelList children = session.getChildren(createdDoc.getRef());
        assertEquals(1, children.size());

        assertEquals(1, zone.getComponents().size());

        assertEquals(children.get(0).getId(),
                ((YUIUnitImpl) firstZone.getComponents().get(0)).getId());
    }

    @Test
    public void iCanDeleteAZone() throws Exception {
        YUIComponent zone = (YUIComponent) createYUIZone(YUITemplate.YUI_ZT_33_33_33);
        DocumentModel createdDoc = createNxZone(zone);

        YUILayoutAdapter adapter = createdDoc.getAdapter(YUILayoutAdapter.class);
        adapter.deleteZone(0);
        createdDoc = session.saveDocument(createdDoc);

        DocumentModelList children = session.getChildren(createdDoc.getRef());
        assertEquals(0, children.size());

        DocumentModel doc = session.getDocument(createdDoc.getRef());
        YUILayoutAdapter layout = doc.getAdapter(YUILayoutAdapter.class);

        assertEquals(0, layout.getLayout().getContent().getComponents().size());
    }

    @Test
    public void iCanSetLayoutSBodySize() throws Exception {
        DocumentModel createdDoc = createNxLayout();

        YUILayoutAdapter adapter = createdDoc.getAdapter(YUILayoutAdapter.class);
        adapter.setBodySize(new YUIFixedBodySize(YUISize.YUI_BS_974_PX));
        createdDoc = session.saveDocument(createdDoc);

        DocumentModel doc = session.getDocument(createdDoc.getRef());
        YUILayoutAdapter layout = doc.getAdapter(YUILayoutAdapter.class);

        assertEquals(YUISize.YUI_BS_974_PX.getSize(),
                layout.getLayout().getBodySize().getSize());
    }

    @Test
    public void iCanSetLayoutSSideBar() throws Exception {
        DocumentModel createdDoc = createNxLayout();

        YUILayoutAdapter adapter = createdDoc.getAdapter(YUILayoutAdapter.class);
        adapter.setSideBar(YUISideBarStyle.YUI_SB_LEFT_180PX);

        session.saveDocument(createdDoc);

        DocumentModel doc = session.getDocument(createdDoc.getRef());
        YUILayoutAdapter layout = doc.getAdapter(YUILayoutAdapter.class);

        assertEquals(YUISideBarStyle.YUI_SB_LEFT_180PX.toString(),
                layout.getLayout().getSidebarStyle().toString());
    }

    @Test
    public void iCanSetLayoutSHeaderAndFooter() throws Exception {
        DocumentModel createdDoc = createNxLayout();

        YUILayoutAdapter adapter = createdDoc.getAdapter(YUILayoutAdapter.class);
        adapter.setHeader(new YUIUnitImpl());
        adapter.setFooter(null);

        DocumentModel doc = session.getDocument(createdDoc.getRef());
        YUILayoutAdapter layout = doc.getAdapter(YUILayoutAdapter.class);

        assertNotNull(layout.getLayout().getHeader());
        assertNull(layout.getLayout().getFooter());
    }
}
