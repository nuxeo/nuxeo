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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.spaces.api.Gadget;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.core.impl.Constants;

public class DocSpaceImpl implements Space {

    private final DocumentModel doc;

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

    public String getLayout() {
        return getInternalStringProperty(Constants.Space.SPACE_LAYOUT);
    }

    public String getCategory() {
        return getInternalStringProperty(Constants.Space.SPACE_CATEGORY);
    }

    public boolean isEqualTo(Space space) {
        return space.getId() != null && space.getId().equals(getId());
    }

    public String getTheme() {
        return getInternalStringProperty(Constants.Space.SPACE_THEME);
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
        return getBooleanProperty(Constants.Space.SPACE_VERSIONNABLE);
    }

    public List<Space> getVersions() {
        if (isVersionnable()) {
            try {
                List<DocumentModel> docs = doc.getCoreSession().getChildren(
                        doc.getParentRef(), Constants.Space.TYPE, null,
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

    public List<Gadget> getGagets() throws ClientException {
        List<Gadget> result = new ArrayList<Gadget>();
        CoreSession session = doc.getCoreSession();
        DocumentModelList gadgets = session.getChildren(doc.getRef(), "Gadget");
        for (DocumentModel gadget : gadgets) {
            Gadget item = gadget.getAdapter(Gadget.class);
            if (item != null) {
                result.add(item);
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

    public String getOwner() {
        try {
            return doc.getProperty("dc:creator").toString();
        } catch (ClientException e) {
            return "";
        }
    }

    public String getTitle() {
        try {
            return doc.getTitle();
        } catch (ClientException e) {
            return "";
        }
    }

    public String getViewer() {
        CoreSession session = doc.getCoreSession();
        return session.getPrincipal().getName();
    }

    public boolean hasPermission(String permissionName) {
        CoreSession session = doc.getCoreSession();
        try {
            return session.hasPermission(doc.getRef(), permissionName);
        } catch (ClientException e) {
            return false;
        }
    }

    public boolean isReadOnly() {
        return hasPermission("Write");
    }

    public String setLayout(String name) {
        try {
            return doc.getProperty(Constants.Space.SPACE_LAYOUT).toString();
        } catch (ClientException e) {
            return "";
        }
    }

    public void updateGadget(Gadget gagdet) throws ClientException {
        // TODO Auto-generated method stub

    }

    public Gadget createGadget(String gadgetName) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Gadget createGadget(URL gadgetDefUrl) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public Calendar getDatePublication() {

        try {
            Serializable ser = doc.getProperty(Constants.Document.PUBLICATION_DATE).getValue();
            if (ser != null)
                return (Calendar) ser;
        } catch (ClientException e) {
        }
        return null;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public static DocSpaceImpl createFromSpace(Space o, String path,
            CoreSession session) throws ClientException {

        DocumentModel doc = session.createDocumentModel(path, o.getName(), Constants.Space.TYPE);
        //TODO: fill the doc with space properties

        return new DocSpaceImpl(doc);

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
