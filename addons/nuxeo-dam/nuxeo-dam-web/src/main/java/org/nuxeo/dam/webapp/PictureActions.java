/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.dam.Constants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Scope(CONVERSATION)
@Name("pictureActions")
@Install(precedence = FRAMEWORK)
public class PictureActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final long MAX_WIDTH_OR_HEIGHT = 384;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    private transient Context conversationContext;

    public void downloadPicture(DocumentView docView) throws ClientException {
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            if (documentManager == null) {
                RepositoryLocation loc = new RepositoryLocation(
                        docLoc.getServerName());
                documentManager = getOrCreateDocumentManager(loc);
            }
            DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
            if (doc != null) {
                String[] propertyPath = docView.getParameter(
                        DocumentFileCodec.FILE_PROPERTY_PATH_KEY).split(":");
                String title = null;
                String field = null;
                Property datamodel = null;
                if (propertyPath.length == 2) {
                    title = propertyPath[0];
                    field = propertyPath[1];
                    datamodel = doc.getProperty("picture:views");
                } else if (propertyPath.length == 3) {
                    String schema = propertyPath[0];
                    title = propertyPath[1];
                    field = propertyPath[2];
                    datamodel = doc.getProperty(schema + ":" + "views");
                }
                Property view = null;
                for (Property property : datamodel) {
                    if (property.get("title").getValue().equals(title)) {
                        view = property;
                    }
                }

                if (view == null) {
                    for (Property property : datamodel) {
                        if (property.get("title").getValue().equals("Thumbnail")) {
                            view = property;
                        }
                    }
                }
                if (view == null) {
                    return;
                }
                Blob blob = (Blob) view.getValue(field);
                String filename = (String) view.getValue("filename");
                // download
                FacesContext context = FacesContext.getCurrentInstance();

                ComponentUtils.download(context, blob, filename);
            }
        }
    }

    protected CoreSession getOrCreateDocumentManager(
            RepositoryLocation repositoryLocation) throws ClientException {
        if (documentManager != null) {
            return documentManager;
        }
        DocumentManagerBusinessDelegate documentManagerBD = (DocumentManagerBusinessDelegate) Contexts.lookupInStatefulContexts("documentManager");
        if (documentManagerBD == null) {
            // this is the first time we select the location, create a
            // DocumentManagerBusinessDelegate instance
            documentManagerBD = new DocumentManagerBusinessDelegate();
            conversationContext.set("documentManager", documentManagerBD);
        }
        documentManager = documentManagerBD.getDocumentManager(repositoryLocation);
        return documentManager;
    }

    public boolean isLandscape(DocumentModel doc) throws ClientException {
        if (!doc.hasSchema(Constants.PICTURE_SCHEMA)) {
            return false;
        }
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        Map<String, Serializable> view = (Map<String, Serializable>) doc.getPropertyValue(picture.getViewXPath("Medium"));
        long width = (Long) view.get("width");
        long height = (Long) view.get("height");

        return width > height;
    }

    public long getMaxWidth(DocumentModel doc) throws ClientException {
        if (!doc.hasSchema(Constants.PICTURE_SCHEMA)) {
            return MAX_WIDTH_OR_HEIGHT;
        }
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        Map<String, Serializable> view = (Map<String, Serializable>) doc.getPropertyValue(picture.getViewXPath("Medium"));
        long width = (Long) view.get("width");
        return width > 384 ? MAX_WIDTH_OR_HEIGHT : 384;
    }

    public long getMaxHeight(DocumentModel doc) throws ClientException {
        if (!doc.hasSchema(Constants.PICTURE_SCHEMA)) {
            return MAX_WIDTH_OR_HEIGHT;
        }
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        Map<String, Serializable> view = (Map<String, Serializable>) doc.getPropertyValue(picture.getViewXPath("Medium"));
        long height = (Long) view.get("height");
        return height > 384 ? MAX_WIDTH_OR_HEIGHT : height;
    }

}
