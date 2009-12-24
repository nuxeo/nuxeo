/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.core.impl.docwrapper;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

public class DocSpaceImpl implements Space {

    private final DocumentModel doc;

    public static final String TYPE = "Space";
    private static final String SPACE_THEME = "space:theme";
    private static final String SPACE_LAYOUT = "space:layout";
    private static final String SPACE_CATEGORY = "space:categoryId";
    private static final String SPACE_VERSIONNABLE = "space:versionnable";
    private static final String PUBLICATION_DATE = "dc:valid";

    private static final Log LOGGER = LogFactory.getLog(DocSpaceImpl.class);

    DocSpaceImpl(DocumentModel doc) {
        this.doc = doc;
    }

    private String getInternalStringProperty(String xpath) {
        try {
            return doc.getProperty(xpath).toString();
        } catch (ClientException e) {
            return null;
        }
    }

    public String getLayout() throws ClientException {
        return (String) doc.getPropertyValue(SPACE_LAYOUT);

    }

    public String getCategory() throws ClientException {
        return (String) doc.getPropertyValue(SPACE_CATEGORY);
    }

    public boolean isEqualTo(Space space) {
        return space.getId() != null && space.getId().equals(getId());
    }

    public String getTheme() {
        return getInternalStringProperty(SPACE_THEME);
    }

    protected boolean getBooleanProperty(String xpath) {
        try {
            Serializable value = doc.getPropertyValue(xpath);
            if (value == null) {
                return false;
            } else {
                return (Boolean) value;
            }
        } catch (ClientException e) {
            return false;
        }
    }

    public boolean isVersionnable() {
        return getBooleanProperty(SPACE_VERSIONNABLE);
    }

    public List<Space> getVersions() {
        if (isVersionnable()) {
            try {
                List<DocumentModel> docs = doc.getCoreSession().getChildren(
                        doc.getParentRef(), TYPE, null,
                        new SpaceSorter());
                List<Space> spaces = new ArrayList<Space>();
                for (DocumentModel doc : docs) {
                    spaces.add(doc.getAdapter(Space.class));
                }
                return spaces;
            } catch (ClientException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getDescription() {
        try {
            return doc.getProperty("dc:title").toString();
        } catch (ClientException e) {
            return "";
        }
    }

    public List<Gadget> getGadgets() throws ClientException {
        List<Gadget> result = new ArrayList<Gadget>();
        CoreSession session = doc.getCoreSession();
        DocumentModelList gadgets = session.getChildren(doc.getRef(), DocGadgetImpl.TYPE);
        for (DocumentModel gadget : gadgets) {
            Gadget item = gadget.getAdapter(Gadget.class);
            if (item != null) {
                result.add(item);
            } else {
                LOGGER.warn("Unable to find gadget adapter for doc : " + gadget.getId());
            }
        }
        return result;

    }

    public String getId() {
        return doc.getId();
    }

    public String getName() {
        return doc.getName();
    }

    public String getOwner() throws ClientException {
        return (String) doc.getPropertyValue("dc:creator");
    }

    public String getTitle() throws ClientException {

        return doc.getTitle();

    }

    private CoreSession session() {
        return doc.getCoreSession();
    }

    public String getViewer() {
        return session().getPrincipal().getName();
    }

    public boolean hasPermission(String permissionName) throws ClientException {
        return session().hasPermission(doc.getRef(), permissionName);
    }

    public boolean isReadOnly() throws ClientException {
        return hasPermission("Write");
    }

    public String setLayout(String name) throws ClientException {
        return (String) doc.getPropertyValue(SPACE_LAYOUT);
    }

    public void save(Gadget gadget) throws ClientException {
        if(DocGadgetImpl.class.isAssignableFrom(gadget.getClass())) {
            DocumentModel docGadget = ((DocGadgetImpl) gadget).getDocument();
            session().saveDocument(docGadget);
            session().save();
        }

    }

    public Gadget createGadget(String gadgetName) throws ClientException {
        CoreSession session = session();
        DocumentModel doc = session.createDocumentModel(this.doc
                .getPathAsString(), gadgetName, DocGadgetImpl.TYPE);
        doc = session.createDocument(doc);
        Gadget gadget = doc.getAdapter(Gadget.class);

        // Sets the gadget Url
        try {
            GadgetService service = Framework.getService(GadgetService.class);
            URL def = service.getGadgetDefinition(gadgetName);
            gadget.setDefinitionUrl(new URL(def.toString()));
            gadget.setName(gadgetName);
        } catch (Exception e) {
            LOGGER.error("Unable to find gadget URL for " + gadgetName
                    + " (ID:" + doc.getId() + ")");
        }

        session.saveDocument(doc);
        session.save();
        return gadget;

    }

    public Gadget createGadget(URL gadgetDefUrl) throws ClientException {
        CoreSession session = session();
        DocumentModel doc = session.createDocumentModel(this.doc
                .getPathAsString(), "url", DocGadgetImpl.TYPE);
        doc = session.createDocument(doc);

        doc.setPropertyValue("gadget:url", gadgetDefUrl.toString());

        session.saveDocument(doc);
        session.save();
        return doc.getAdapter(Gadget.class);
    }

    public Calendar getDatePublication() throws ClientException {

        return (Calendar) doc
                .getPropertyValue(PUBLICATION_DATE);

    }

    public DocumentModel getDocument() {
        return doc;
    }

    public static DocSpaceImpl createFromSpace(Space o, String path,
            CoreSession session) throws ClientException {

        DocumentModel doc = session.createDocumentModel(path, o.getName(),
                TYPE);
        // TODO: fill the doc with space properties

        return new DocSpaceImpl(doc);

    }

    public void remove(Gadget gadget) throws ClientException {
        CoreSession session  = doc.getCoreSession();
        DocumentRef ref = new IdRef(gadget.getId());
        DocumentModel gadgetDoc = session.getDocument(ref);

        if(gadgetDoc != null && gadgetDoc.getParentRef().equals(doc.getRef())) {
            session.removeDocument(ref);
        }
    }

    // public boolean isCurrentVersion() {
    // List<Space> spaces = getVersions();
    // if (spaces != null
    // && getVersions().get(0).getDatePublication().equals(
    // this.getDatePublication()))
    // return true;
    // return false;
    // }
}
