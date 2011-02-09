package org.nuxeo.opensocial.container.server.layout;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.Sorter;
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

    private YUILayout layout;

    private DocumentModel doc;

    private CoreSession session;

    public YUILayoutAdapterImpl(DocumentModel doc) {
        this.session = doc.getCoreSession();
        this.doc = doc;

        layout = new YUILayoutImpl();
    }

    public YUILayout getLayout() throws ClientException {
        mapDataValuesWithLayout();
        return layout;
    }

    public void setBodySize(YUIBodySize size) throws ClientException {
        doc.setPropertyValue("yuilayout:bodySize", size.getSize());
        save();
    }

    public YUIUnit setSideBar(YUISideBarStyle sideBar) throws ClientException {
        doc.setPropertyValue("yuilayout:sidebar", sideBar.toString());

        if (!sideBar.equals(YUISideBarStyle.YUI_SB_NO_COLUMN)) {
            try {
                DocumentModel sideBarDoc;

                sideBarDoc = session.getChild(doc.getRef(), "sidebar");
                save();
                return new YUIUnitImpl(sideBarDoc.getId());
            } catch (ClientException e) {
                DocumentModel sideBarDoc;

                sideBarDoc = session.createDocumentModel(doc.getPathAsString(),
                        "sidebar", "Unit");
                sideBarDoc = session.createDocument(sideBarDoc);
                sideBarDoc = session.saveDocument(sideBarDoc);
                save();
                return new YUIUnitImpl(sideBarDoc.getId());
            }
        } else {
            PathRef sideBarRef = new PathRef(doc.getPathAsString() + "/sidebar");
            if (session.exists(sideBarRef)) {
                session.removeDocument(sideBarRef);
            }
            save();
            return null;
        }
    }

    public YUIUnit setHeader(YUIUnit header) throws ClientException {
        if (header != null) {
            DocumentModel headerDoc;
            headerDoc = session.createDocumentModel(doc.getPathAsString(),
                    "header", "Unit");
            headerDoc = session.createDocument(headerDoc);
            headerDoc = session.saveDocument(headerDoc);
            save();
            ((YUIComponent) header).setId(headerDoc.getId());
            return header;
        } else {
            PathRef headerRef = new PathRef(doc.getPathAsString() + "/header");
            if (session.exists(headerRef)) {
                session.removeDocument(headerRef);
            }
            save();
            return null;
        }
    }

    public YUIUnit setFooter(YUIUnit footer) throws ClientException {
        if (footer != null) {
            DocumentModel footerDoc;
            footerDoc = session.createDocumentModel(doc.getPathAsString(),
                    "footer", "Unit");
            footerDoc = session.createDocument(footerDoc);
            footerDoc = session.saveDocument(footerDoc);
            save();
            ((YUIComponent) footer).setId(footerDoc.getId());
            return footer;
        } else {
            PathRef footerRef = new PathRef(doc.getPathAsString() + "/footer");
            if (session.exists(footerRef)) {
                session.removeDocument(footerRef);
            }
            save();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public YUIComponentZone createZone(YUIComponentZone zone, int zoneIndex)
            throws ClientException {
        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue("yuilayout:zones");

        Map<String, Serializable> property = new HashMap<String, Serializable>();

        property.put("template", ((YUIComponentZone) zone).getTemplate()
                .toString());

        properties.add(zoneIndex, property);

        int unitIndex = 0;

        for (YUIComponent unitComponent : ((YUIComponent) zone).getComponents()) {
            DocumentModel unitDoc;

            unitDoc = session.createDocumentModel(doc.getPathAsString(),
                    IdUtils.generateStringId(), "Unit");

            unitDoc.setPropertyValue("yuiunit:position", unitIndex);
            unitDoc.setPropertyValue("yuiunit:zoneIndex", zoneIndex);

            unitDoc = session.createDocument(unitDoc);

            unitComponent.setId(unitDoc.getId());

            unitIndex++;
        }

        doc.setPropertyValue("yuilayout:zones", (Serializable) properties);
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

                session.removeDocument(new IdRef(unit.getId()));

                ((YUIAbstractComponent) zone).getComponents()
                        .remove(i);
            }
        } else if (actualNumberOfUnits < wantedNumberOfUnits) {
            for (int i = actualNumberOfUnits; i < wantedNumberOfUnits; i++) {
                YUIUnitImpl unit = new YUIUnitImpl();
                zone.addComponent(unit);

                DocumentModel unitDoc = session.createDocumentModel(
                        doc.getPathAsString(), IdUtils.generateStringId(),
                        "Unit");

                unitDoc.setPropertyValue("yuiunit:position", i);
                unitDoc.setPropertyValue("yuiunit:zoneIndex", zoneIndex);

                unitDoc = session.createDocument(unitDoc);

                unit.setId(unitDoc.getId());
            }
        }

        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue("yuilayout:zones");
        Map<String, Serializable> property = properties.get(zoneIndex);

        property.put("template", template.toString());

        doc.setPropertyValue("yuilayout:zones", (Serializable) properties);
        save();
        return zone;
    }

    @SuppressWarnings( { "serial", "unchecked" })
    public void deleteZone(final int zoneIndex) throws ClientException {
        for (DocumentModel unitDoc : session.getChildren(doc.getRef(), "Unit",
                new Filter() {
                    public boolean accept(DocumentModel arg0) {
                        try {
                            if (zoneIndex == ((Long) arg0.getPropertyValue("yuiunit:zoneIndex")))
                                return true;
                            else
                                return false;
                        } catch (Exception e) {
                            return false;
                        }
                    }
                }, null)) {
            session.removeDocument(unitDoc.getRef());
        }

        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue("yuilayout:zones");
        properties.remove(zoneIndex);
        doc.setPropertyValue("yuilayout:zones", (Serializable) properties);
        save();
    }

    public void save() throws ClientException {
        session.saveDocument(doc);
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

    private void cleanLayout() throws ClientException {
        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue("yuilayout:zones");
        properties.clear();
        doc.setPropertyValue("yuilayout:zones", (Serializable) properties);

        session.removeChildren(doc.getRef());
    }

    @SuppressWarnings( { "unchecked", "serial" })
    private void mapDataValuesWithLayout() throws ClientException {
        // TODO
        try {
            DocumentModel header = session.getChild(doc.getRef(), "header");
            layout.setHeader(new YUIUnitImpl(header.getId()));
        } catch (ClientException e) {
            layout.setHeader(null);
        }
        // TODO
        try {
            DocumentModel footer = session.getChild(doc.getRef(), "footer");
            layout.setFooter(new YUIUnitImpl(footer.getId()));
        } catch (ClientException e) {
            layout.setFooter(null);
        }

        YUISize[] sizeValues = YUISize.values();

        Long size = (Long) doc.getPropertyValue("yuilayout:bodySize");

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

        layout.setSideBarStyle(YUISideBarStyle.valueOf((String) doc.getPropertyValue("yuilayout:sidebar")));
        try {
            DocumentModel sidebar = session.getChild(doc.getRef(), "sidebar");
            layout.setSideBar(new YUIUnitImpl(sidebar.getId()));
        } catch (ClientException e) {
            layout.setSideBarStyle(YUISideBarStyle.YUI_SB_NO_COLUMN);
        }

        layout.setSideBarStyle(YUISideBarStyle.valueOf((String) doc.getPropertyValue("sidebar")));

        List<Map<String, Serializable>> properties = (List<Map<String, Serializable>>) doc.getPropertyValue("yuilayout:zones");

        for (Map<String, Serializable> property : properties) {
            YUITemplate template = YUITemplate.valueOf((String) property.get("template"));
            final YUIComponentZoneImpl zone = new YUIComponentZoneImpl(template);
            layout.getContent()
                    .addComponent(zone);

            for (DocumentModel unitDoc : session.getChildren(doc.getRef(),
                    "Unit", new Filter() {
                        public boolean accept(DocumentModel arg0) {
                            try {
                                if (layout.getContent()
                                        .getComponents()
                                        .indexOf(zone) == ((Long) arg0.getPropertyValue("yuiunit:zoneIndex"))
                                        && !"header".equals(arg0.getName())
                                        && !"footer".equals(arg0.getName())
                                        && !"sidebar".equals(arg0.getName()))
                                    return true;
                                else
                                    return false;
                            } catch (Exception e) {
                                return false;
                            }
                        }
                    }, new Sorter() {
                        public int compare(DocumentModel doc1,
                                DocumentModel doc2) {
                            Long pos1;
                            Long pos2;
                            try {
                                pos1 = (Long) doc1.getPropertyValue("yuiunit:position");
                                pos2 = (Long) doc2.getPropertyValue("yuiunit:position");
                                return pos1.compareTo(pos2);
                            } catch (Exception e) {
                                return 0;
                            }
                        }
                    })) {
                zone.addComponent(new YUIUnitImpl(unitDoc.getId()));
            }
        }
    }
}
