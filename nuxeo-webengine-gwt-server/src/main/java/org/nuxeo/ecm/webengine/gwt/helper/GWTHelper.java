/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.gwt.helper;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.util.JSonDocumentExporter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GWTHelper {

    // Utility class.
    private GWTHelper() {
    }

    public static String toJSon(DocumentModel doc) {
        return doc2JSon(doc).toString();
    }


    public static JSONArray getChildren(CoreSession session) { // the roots
        try {            
            return getChildren(session, session.getRootDocument(), null);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    public static JSONArray getChildren(CoreSession session, DocumentRef docRef) {
        return getChildren(session, docRef, docRef.reference().toString());
    }

    public static JSONArray getChildren(CoreSession session, DocumentRef docRef, String parentRef) {
        try {
            return getChildren(session, session.getDocument(docRef));
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    public static JSONArray getChildren(CoreSession session, DocumentModel doc) {
        return getChildren(session, doc, doc.getId());
    }
    
    public static JSONArray getChildren(CoreSession session, DocumentModel doc, String parentRef) {
        JSONArray list  = new JSONArray();
        if( doc == null ){
            return list;
        }
        try {
            DocumentModelList docs = session.getChildren(doc.getRef());
            for ( DocumentModel d : docs) {
                JSONObject o = new JSONObject();
                o.put("id", d.getId());
                o.put("parentId", parentRef);
                o.put("name", d.getName());
                o.put("path", d.getPathAsString());
                o.put("type", d.getType());
                o.put("title", d.getTitle());
                o.put("isFolder", d.hasFacet("Folderish"));
                list.add(o);
            }
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }

        return list;
    }

    public static JSONArray getChildrenFiles(CoreSession session, DocumentRef docRef, String parentRef) {
        try {
            return getChildrenFiles(session, session.getDocument(docRef), parentRef);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    public static JSONArray getChildrenFiles(CoreSession session, DocumentModel doc, String parentRef) {
        JSONArray list  = new JSONArray();
        if( doc == null ){
            return list;
        }
        try {
            DocumentModelList docs = session.getChildren(doc.getRef());
            for ( DocumentModel d : docs) {
                if (d.isFolder()) continue;
                JSONObject o = new JSONObject();
                o.put("id", d.getId());
                o.put("name", d.getName());
                o.put("path", d.getPathAsString());
                o.put("type", d.getType());
                o.put("title", d.getTitle());
                o.put("parentId", parentRef);
                o.put("isFolder", d.hasFacet("Folderish"));
                list.add(o);
            }
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }

        return list;
    }

    public static JSONArray getChildrenFolders(CoreSession session, DocumentRef docRef, String parentRef) {
        try {
            return getChildrenFolders(session, session.getDocument(docRef), parentRef);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    public static JSONArray getChildrenFolders(CoreSession session, DocumentModel doc, String parentRef) {
        JSONArray list  = new JSONArray();
        if( doc == null ){
            return list;
        }
        try {
            DocumentModelList docs = session.getChildren(doc.getRef());
            for ( DocumentModel d : docs) {
                if (!d.isFolder()) continue;
                JSONObject o = new JSONObject();
                o.put("id", d.getId());
                o.put("parentId", parentRef);
                o.put("name", d.getName());
                o.put("path", d.getPathAsString());
                o.put("type", d.getType());
                o.put("title", d.getTitle());
                o.put("isFolder", d.hasFacet("Folderish"));
                list.add(o);
            }
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }

        return list;
    }

    public static String toJSon(DocumentModel doc, String ... schemas) {
        return doc2JSon(doc).toString();
    }

    public static JSONObject doc2JSon(DocumentModel doc) {
        return doc2JSon(doc, (String[]) null);
    }

    public static JSONObject doc2JSon(DocumentModel doc, String... schemas) {
        try {
            DocumentRef parentRef = doc.getParentRef(); 
            JSONObject obj = new JSONObject();
            obj.put("id", doc.getId());
            obj.put("parentId", parentRef == null ? null : parentRef.reference());
            obj.put("name", doc.getName());
            obj.put("path", doc.getPathAsString());
            obj.put("isLocked", doc.isLocked());
            obj.put("lifeCycleState", doc.getCurrentLifeCycleState());
            obj.put("lifeCyclePolicy", doc.getLifeCyclePolicy());
            obj.put("type", doc.getType());
            obj.put("isVersion", doc.isVersion());
            obj.put("isProxy", doc.isProxy());
            obj.put("sourceId", doc.getSourceId());
            obj.put("facets", doc.getDeclaredFacets());
            obj.put("schemas", doc.getDeclaredSchemas());
            JSonDocumentExporter jde = new JSonDocumentExporter();
            if (schemas != null) {
                for (String schema : schemas) {
                    obj.put(schema, jde.run(doc.getPart(schema)));
                }
            } else {
                for (DocumentPart part : doc.getParts()) {
                    obj.put(part.getName(), jde.run(part));
                }
            }
            return obj;
        } catch (Exception e) {
            throw WebException.wrap("Failed to export documnt as json: "
                    + doc.getPath(), e);
        }
    }





//    public static DocumentModel fromJSon(JSONObject obj) {
//        String id = obj.getString("id");
//        String type = obj.getString("type");
//        String name = obj.getString("name");
//        String path = obj.getString("path");
//        String parentPath = new Path(path).removeLastSegments(1).toString();
//        DocumentModelImpl doc = new DocumentModelImpl(String sid, String type, String id, Path path,
//                String lock, DocumentRef docRef, DocumentRef parentRef,
//                String[] schemas, Set<String> facets, String sourceId,
//                String repositoryName);
//        return doc;
//    }

}
