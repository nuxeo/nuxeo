/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Simple utility class to provide DocumentList related functions.
 *
 * @author tiry
 */
public final class DocumentsListsUtils {

    private static final Log log = LogFactory.getLog(DocumentsListsUtils.class);

    // Utility class.
    private DocumentsListsUtils() {
    }

    /**
     * Returns list of the document types contained in the list of document.
     */
    public static List<String> getTypesInList(List<DocumentModel> documentsList) {
        List<String> res = new ArrayList<>();
        for (DocumentModel doc : documentsList) {
            String dt = doc.getType();
            if (!res.contains(dt)) {
                res.add(dt);
            }
        }
        return res;
    }

    /**
     * Returns list of DocumentRef corresponding to the list of documents.
     */
    public static List<DocumentRef> getDocRefs(List<DocumentModel> documentsList) {
        List<DocumentRef> references = new ArrayList<>();

        for (DocumentModel docModel : documentsList) {
            references.add(docModel.getRef());
        }

        return references;
    }

    /**
     * Removes one document from a list.
     * <p>
     * Removal is based on DocumentRef.
     *
     * @return <code>true</code> if the given list contains specified document and it has been removed
     */
    public static boolean removeDocumentFromList(List<DocumentModel> documentList, DocumentModel documentToRemove) {
        if (null == documentToRemove) {
            return false;
        }
        try {
            boolean found = false;
            for (int i = 0; i < documentList.size(); i++) {
                if (documentList.get(i).getRef().equals(documentToRemove.getRef())) {
                    documentList.remove(i);
                    found = true;
                }
            }
            return found;
        } catch (UnsupportedOperationException e) {
            // XXX: maybe throw a checked exception
            log.error("immutable list, cannot remove document: " + documentToRemove, e);
            return false;
        }
    }

    /**
     * Removes some documents from a list.
     * <p>
     * Removal is based on DocumentRef.
     */
    public static void removeDocumentsFromList(List<DocumentModel> documentList, List<DocumentModel> documentsToRemove) {
        if (null == documentsToRemove || documentsToRemove.isEmpty()) {
            return;
        }

        if (null == documentList || documentList.isEmpty()) {
            return;
        }

        for (DocumentModel documentToRemove : documentsToRemove) {
            for (int i = 0; i < documentList.size(); i++) {
                if (documentList.get(i).getRef().equals(documentToRemove.getRef())) {
                    documentList.remove(i);
                }
            }
        }
    }

    /**
     * Returns the list of parents documentRef.
     */
    public static List<DocumentRef> getParentRefFromDocumentList(List<DocumentModel> documentList) {
        List<DocumentRef> parentRefs = new ArrayList<>();

        for (DocumentModel doc : documentList) {
            if (!parentRefs.contains(doc.getParentRef())) {
                parentRefs.add(doc.getParentRef());
            }
        }

        return parentRefs;
    }

    public static boolean isDocumentInList(DocumentModel doc, List<DocumentModel> list) {
        String strDocRef = doc.getRef().toString();
        for (DocumentModel d : list) {
            if (strDocRef.equals(d.getRef().toString())) {
                return true;
            }
        }
        return false;
    }

}
