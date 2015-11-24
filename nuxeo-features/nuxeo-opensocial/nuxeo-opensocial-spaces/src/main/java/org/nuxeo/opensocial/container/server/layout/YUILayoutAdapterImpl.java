package org.nuxeo.opensocial.container.server.layout;

import static org.nuxeo.ecm.spaces.api.Constants.UNIT_DOCUMENT_TYPE;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.spaces.api.Constants;
import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponentZone;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.layout.api.YUIUnit;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractComponent;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUICustomBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIFixedBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUILayoutImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIUnitImpl;

/**
 * @author Stéphane Fourrier
 */

// TODO delete save() in methods
public class YUILayoutAdapterImpl implements YUILayoutAdapter {

    private static final long serialVersionUID = 1L;

    public static final String YUI_LAYOUT_BODY_SIZE_PROPERTY = "yuilayout:bodySize";

    public static final String YUI_LAYOUT_SIDEBAR_PROPERTY = "yuilayout:sidebar";

    public static final String YUI_LAYOUT_ZONES_PROPERTY = "yuilayout:zones";

    public static final String YUI_UNIT_POSITION_PROPERTY = "yuiunit:position";

    public static final String YUI_UNIT_ZONE_INDEX_PROPERTY = "yuiunit:zoneIndex";

    public static final String HEADER = "header";

    public static final String FOOTER = "footer";

    public static final String SIDEBAR = "sidebar";

    private DocumentModel doc;

    public YUILayoutAdapterImpl(DocumentModel doc) {
        this.doc = doc;
    }

    public void setBodySize(YUIBodySize size) throws ClientException {
        doc.setPropertyValue(YUI_LAYOUT_BODY_SIZE_PROPERTY, size.getSize());
        save();
    }

    public YUIUnit setSideBar(YUISideBarStyle sideBar) throws ClientException {
        doc.setPropertyValue(YUI_LAYOUT_SIDEBAR_PROPERTY, sideBar.toString());

        if (!sideBar.equals(YUISideBarStyle.YUI_SB_NO_COLUMN)) {
            String sidebarPath = doc.getPathAsString() + '/' + SIDEBAR;
            DocumentRef sidebarRef = new PathRef(sidebarPath);

            if (session().exists(sidebarRef)) {
                DocumentModel sideBarDoc = session().getDocument(sidebarRef);
                return new YUIUnitImpl(sideBarDoc.getId());
            } else {
                DocumentModel sideBarDoc = session().createDocumentModel(
                        doc.getPathAsString(), SIDEBAR,
                        Constants.UNIT_DOCUMENT_TYPE);
                sideBarDoc = session().createDocument(sideBarDoc);
                save();
                return new YUIUnitImpl(sideBarDoc.getId());
            }
        } else {
            PathRef sideBarRef = new PathRef(doc.getPathAsString() + "/sidebar");
            if (session().exists(sideBarRef)) {
                session().removeDocument(sideBarRef);
            }
            save();
            return null;
        }
    }

    public YUIUnit setHeader(YUIUnit header) throws ClientException {
        if (header != null) {
            DocumentModel headerDoc = session().createDocumentModel(doc.getPathAsString(),
                    HEADER, UNIT_DOCUMENT_TYPE);
            headerDoc = session().createDocument(headerDoc);
            headerDoc = session().saveDocument(headerDoc);
            save();
            ((YUIComponent) header).setId(headerDoc.getId());
            return header;
        } else {
            PathRef headerRef = new PathRef(doc.getPathAsString() + "/header");
            if (session().exists(headerRef)) {
                session().removeDocument(headerRef);
            }
            save();
            return null;
        }
    }

    public YUIUnit setFooter(YUIUnit footer) throws ClientException {
        if (footer != null) {
            DocumentModel footerDoc = session().createDocumentModel(doc.getPathAsString(),
                    FOOTER, UNIT_DOCUMENT_TYPE);
            footerDoc = session().createDocument(footerDoc);
            footerDoc = session().saveDocument(footerDoc);
            save();
            ((YUIComponent) footer).setId(footerDoc.getId());
            return footer;
        } else {
            PathRef footerRef = new PathRef(doc.getPathAsString() + "/footer");
            if (session().exists(footerRef)) {
                session().removeDocument(footerRef);
            }
            save();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public YUIComponentZone createZone(YUIComponentZone zone, int zoneIndex)
            throws ClientException {
        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue(YUI_LAYOUT_ZONES_PROPERTY);

        Map<String, Serializable> property = new HashMap<String, Serializable>();

        property.put("template", zone.getTemplate()
                .toString());

        properties.add(zoneIndex, property);

        int unitIndex = 0;

        for (YUIComponent unitComponent : ((YUIComponent) zone).getComponents()) {
            String unitName = "unit" + unitIndex;
            DocumentModel unitDoc = session().createDocumentModel(doc.getPathAsString(),
                    unitName, UNIT_DOCUMENT_TYPE);

            unitDoc.setPropertyValue(YUI_UNIT_POSITION_PROPERTY, unitIndex);
            unitDoc.setPropertyValue(YUI_UNIT_ZONE_INDEX_PROPERTY, zoneIndex);

            unitDoc = session().createDocument(unitDoc);

            unitComponent.setId(unitDoc.getId());

            unitIndex++;
        }

        doc.setPropertyValue(YUI_LAYOUT_ZONES_PROPERTY,
                (Serializable) properties);
        save();
        return zone;
    }

    @SuppressWarnings("unchecked")
    public YUIComponentZone updateZone(YUIComponentZone zone, int zoneIndex,
            YUITemplate template) throws ClientException {
        int actualNumberOfUnits = zone.getTemplate()
                .getNumberOfComponents();
        int wantedNumberOfUnits = template.getNumberOfComponents();

        zone.setTemplate(template);

        if (actualNumberOfUnits > wantedNumberOfUnits) {
            for (int i = actualNumberOfUnits - 1; i > wantedNumberOfUnits - 1; i--) {
                YUIUnitImpl unit = (YUIUnitImpl) ((YUIAbstractComponent) zone).getComponents()
                        .get(i);

                session().removeDocument(new IdRef(unit.getId()));

                ((YUIAbstractComponent) zone).getComponents()
                        .remove(i);
            }
        } else if (actualNumberOfUnits < wantedNumberOfUnits) {
            for (int i = actualNumberOfUnits; i < wantedNumberOfUnits; i++) {
                YUIUnitImpl unit = new YUIUnitImpl();
                zone.addComponent(unit);

                String unitName = "unit" + i;
                DocumentModel unitDoc = session().createDocumentModel(
                        doc.getPathAsString(), unitName,
                        UNIT_DOCUMENT_TYPE);

                unitDoc.setPropertyValue(YUI_UNIT_POSITION_PROPERTY, i);
                unitDoc.setPropertyValue(YUI_UNIT_ZONE_INDEX_PROPERTY,
                        zoneIndex);

                unitDoc = session().createDocument(unitDoc);

                unit.setId(unitDoc.getId());
            }
        }

        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue(YUI_LAYOUT_ZONES_PROPERTY);
        Map<String, Serializable> property = properties.get(zoneIndex);

        property.put("template", template.toString());

        doc.setPropertyValue(YUI_LAYOUT_ZONES_PROPERTY,
                (Serializable) properties);
        save();
        return zone;
    }

    @SuppressWarnings( { "serial", "unchecked" })
    public void deleteZone(final int zoneIndex) throws ClientException {
        for (DocumentModel unitDoc : session().getChildren(doc.getRef(),
                UNIT_DOCUMENT_TYPE, new Filter() {
                    public boolean accept(DocumentModel arg0) {
                        try {
                            return zoneIndex == ((Long) arg0.getPropertyValue(YUI_UNIT_ZONE_INDEX_PROPERTY));
                        } catch (Exception e) {
                            return false;
                        }
                    }
                }, null)) {
            session().removeDocument(unitDoc.getRef());
        }

        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue(YUI_LAYOUT_ZONES_PROPERTY);
        properties.remove(zoneIndex);
        doc.setPropertyValue(YUI_LAYOUT_ZONES_PROPERTY,
                (Serializable) properties);
        save();
    }

    public void save() throws ClientException {
        session().saveDocument(doc);
    }

    public void initLayout(YUILayout layout) throws ClientException {
        cleanLayout();

        setBodySize(layout.getBodySize());
        setHeader(layout.getHeader());
        setFooter(layout.getHeader());
        setSideBar(layout.getSidebarStyle());

        int zoneIndex = 0;
        for (YUIComponent zone : layout.getContent()
                .getComponents()) {
            createZone((YUIComponentZone) zone, zoneIndex);
            zoneIndex++;
        }
        save();
    }

    @SuppressWarnings("unchecked")
    private void cleanLayout() throws ClientException {
        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue(YUI_LAYOUT_ZONES_PROPERTY);
        properties.clear();
        doc.setPropertyValue(YUI_LAYOUT_ZONES_PROPERTY,
                (Serializable) properties);

        session().removeChildren(doc.getRef());
    }

    @SuppressWarnings( { "unchecked", "serial" })
    public YUILayout getLayout() throws ClientException {
        YUILayout layout = new YUILayoutImpl();
        String headerPath = doc.getPathAsString() + '/' + HEADER;
        DocumentRef headerRef = new PathRef(headerPath);
        if (session().exists(headerRef)) {
            DocumentModel headerDoc = session().getDocument(headerRef);
            layout.setHeader(new YUIUnitImpl(headerDoc.getId()));
        } else {
            layout.setHeader(null);
        }
        String footerPath = doc.getPathAsString() + '/' + FOOTER;
        DocumentRef footerRef = new PathRef(footerPath);
        if (session().exists(footerRef)) {
            DocumentModel footerDoc = session().getDocument(footerRef);
            layout.setFooter(new YUIUnitImpl(footerDoc.getId()));
        } else {
            layout.setFooter(null);
        }

        YUISize[] sizeValues = YUISize.values();

        Long size = (Long) doc.getPropertyValue(YUI_LAYOUT_BODY_SIZE_PROPERTY);

        int index = 0;
        boolean bodySizeNotFound = true;
        while (index < sizeValues.length && bodySizeNotFound) {
            if (sizeValues[index].getSize() == size) {
                bodySizeNotFound = false;
            }
            index++;
        }

        if (bodySizeNotFound) {
            layout.setBodySize(new YUICustomBodySize(size));
        } else {
            layout.setBodySize(new YUIFixedBodySize(sizeValues[index - 1]));
        }

        layout.setSideBarStyle(YUISideBarStyle.valueOf((String) doc.getPropertyValue(YUI_LAYOUT_SIDEBAR_PROPERTY)));
        String sidebarPath = doc.getPathAsString() + '/' + SIDEBAR;
        DocumentRef sidebarRef = new PathRef(sidebarPath);
        if (session().exists(sidebarRef)) {
            DocumentModel sidebar = session().getDocument(sidebarRef);
            layout.setSideBar(new YUIUnitImpl(sidebar.getId()));
        } else {
            layout.setSideBarStyle(YUISideBarStyle.YUI_SB_NO_COLUMN);
        }

        layout.setSideBarStyle(YUISideBarStyle.valueOf((String) doc.getPropertyValue(SIDEBAR)));

        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue(YUI_LAYOUT_ZONES_PROPERTY);

        for (Map<String, Serializable> property : properties) {
            YUITemplate template = YUITemplate.valueOf((String) property.get("template"));
            final YUIComponentZoneImpl zone = new YUIComponentZoneImpl(template);
            layout.getContent()
                    .addComponent(zone);

            for (DocumentModel unitDoc : session().getChildren(doc.getRef(),
                    UNIT_DOCUMENT_TYPE, new Filter() {
                        public boolean accept(DocumentModel doc) {
                            List<String> specialUnits = Arrays.asList(HEADER,
                                    FOOTER, SIDEBAR);
                            return !specialUnits.contains(doc.getName());
                        }
                    }, new Sorter() {
                        public int compare(DocumentModel doc1,
                                DocumentModel doc2) {
                            Long pos1;
                            Long pos2;
                            try {
                                pos1 = (Long) doc1.getPropertyValue(YUI_UNIT_POSITION_PROPERTY);
                                pos2 = (Long) doc2.getPropertyValue(YUI_UNIT_POSITION_PROPERTY);
                                return pos1.compareTo(pos2);
                            } catch (Exception e) {
                                return 0;
                            }
                        }
                    })) {
                if (layout.getContent()
                        .getComponents()
                        .indexOf(zone) == ((Long) unitDoc.getPropertyValue(YUI_UNIT_ZONE_INDEX_PROPERTY))) {
                    zone.addComponent(new YUIUnitImpl(unitDoc.getId()));
                }
            }
        }
        return layout;
    }

    private CoreSession session() {
        return doc.getCoreSession();
    }

}
