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
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.ecm.spaces.api.SpaceProvider;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

public class DocSpaceImpl implements Space {

    protected final DocumentModel doc;

    public static final String TYPE = "Space";

    protected static final String SPACE_THEME = "space:theme";

    protected static final String SPACE_LAYOUT = "space:layout";

    protected static final String SPACE_CATEGORY = "space:categoryId";

    protected static final String SPACE_VERSIONNABLE = "space:versionnable";

    protected static final String PUBLICATION_DATE = "dc:valid";

    private static final Log LOGGER = LogFactory.getLog(DocSpaceImpl.class);

    protected DocSpaceImpl(DocumentModel doc) {
        this.doc = doc;
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

    public String getTheme() throws ClientException {
        return (String) doc.getPropertyValue(SPACE_THEME);
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

    public String getDescription() {
        try {
            return (String) doc.getPropertyValue("dc:description");
        } catch (ClientException e) {
            return "";
        }
    }

    public List<Gadget> getGadgets() throws ClientException {
        List<Gadget> result = new ArrayList<Gadget>();
        CoreSession session = doc.getCoreSession();
        DocumentModelList gadgets = session.getChildren(doc.getRef(),
                DocGadgetImpl.TYPE);
        for (DocumentModel gadget : gadgets) {
            Gadget item = gadget.getAdapter(Gadget.class);
            if (item != null) {
                result.add(item);
            } else {
                LOGGER.warn("Unable to find gadget adapter for doc : "
                        + gadget.getId());
            }
        }
        return result;
    }

    public Gadget getGadget(String id) throws ClientException {
        CoreSession session = doc.getCoreSession();
        IdRef docRef = new IdRef(id);
        if (session.exists(docRef)) {
            DocumentModel document = session.getDocument(docRef);
            return document.getAdapter(Gadget.class);
        }
        return null;
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

    public List<String> getAvailableSecurityPermissions()
            throws ClientException {
        return session().getAvailableSecurityPermissions();
    }

    public boolean isReadOnly() throws ClientException {
        return !hasPermission(SecurityConstants.WRITE);
    }

    public void setLayout(String name) throws ClientException {
        doc.setPropertyValue(SPACE_LAYOUT, name);
    }

    public void save(Gadget gadget) throws ClientException {
        DocumentModel docGadget = null;

        DocumentRef gadgetRef = new IdRef(gadget.getId());
        if (session().exists(gadgetRef)) {
            docGadget = session().getDocument(gadgetRef);
            Gadget sessionGadget = docGadget.getAdapter(Gadget.class);
            sessionGadget.copyFrom(gadget);
        }

        if (docGadget != null) {
            session().saveDocument(docGadget);
            session().save();
        } else {
            throw new ClientException(
                    "Unable to save gadget: did not find the gadget in DB");
        }

    }

    public Gadget createGadget(String gadgetName) throws ClientException {
        CoreSession session = session();
        DocumentModel doc = session.createDocumentModel(
                this.doc.getPathAsString(), gadgetName, DocGadgetImpl.TYPE);
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
        DocumentModel doc = session.createDocumentModel(
                this.doc.getPathAsString(), "url", DocGadgetImpl.TYPE);
        doc = session.createDocument(doc);

        doc.setPropertyValue("gadget:url", gadgetDefUrl.toString());

        session.saveDocument(doc);
        session.save();
        return doc.getAdapter(Gadget.class);
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public static DocSpaceImpl createFromSpace(Space o, String path,
            CoreSession session) throws ClientException {

        DocumentModel doc = session.createDocumentModel(path, o.getName(), TYPE);
        // TODO: fill the doc with space properties

        return new DocSpaceImpl(doc);

    }

    public void remove(Gadget gadget) throws ClientException {
        CoreSession session = doc.getCoreSession();
        DocumentRef ref = new IdRef(gadget.getId());
        DocumentModel gadgetDoc = session.getDocument(ref);

        if (gadgetDoc != null && gadgetDoc.getParentRef().equals(doc.getRef())) {
            session.removeDocument(ref);
        }
    }

    public void save() throws ClientException {
        doc.getCoreSession().saveDocument(doc);
        doc.getCoreSession().save();

    }

    public Space copyFrom(Space space) throws ClientException {
        setLayout(space.getLayout());
        setTheme(space.getTheme());
        setDescription(space.getDescription());
        setTitle(space.getTitle());
        return this;
    }

    public void setDescription(String description) throws ClientException {
        doc.setPropertyValue("dc:description", description);

    }

    public void setTheme(String theme) throws ClientException {
        doc.setPropertyValue(SPACE_THEME, theme);

    }

    public void setTitle(String title) throws ClientException {
        doc.setPropertyValue("dc:title", title);

    }

    public void setCategory(String category) throws ClientException {
        doc.setPropertyValue(SPACE_CATEGORY, category);

    }

    public Calendar getPublicationDate() throws ClientException {
        return (Calendar) doc.getPropertyValue("dc:valid");
    }

    public void setPublicationDate(Calendar datePublication)
            throws ClientException {
        doc.setPropertyValue("dc:valid", datePublication);
    }

    public int compareTo(Space o) {
        try {
            Calendar dte1 = getPublicationDate();
            Calendar dte2 = o.getPublicationDate();
            return dte1.compareTo(dte2);
        } catch (ClientException e) {
            return 0;
        }
    }

    public void remove() throws ClientException {
        CoreSession session = doc.getCoreSession();
        session.removeDocument(doc.getRef());
        session.save();
    }

    public List<String> getPermissions() throws Exception {
        ACP acp = session().getACP(doc.getRef());

        String user = session().getPrincipal().getName();

        UserManager userManager = Framework.getService(UserManager.class);
        List<String> perms = new ArrayList<String>();
        for (ACL acl : acp.getACLs()) {
            for (ACE ace : acl.getACEs()) {
                if (user.equals(ace.getUsername())) {
                    perms.add(ace.getPermission());
                } else {
                    NuxeoGroup group = userManager.getGroup(ace.getUsername());
                    if (group != null && group.getMemberUsers().contains(user))
                        perms.add(ace.getPermission());
                }
            }
        }
        return perms;
    }


}
