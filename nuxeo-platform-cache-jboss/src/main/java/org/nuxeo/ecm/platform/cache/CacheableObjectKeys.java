/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;

public class CacheableObjectKeys {

    private CacheableObjectKeys() {
    }

    /**
     *
     * @param dataModel
     * @return the key for DataModel base on {@link DataModel#getSchema()}
     */
    public static String getCacheKey(DataModel dataModel) {
        return CacheableObjectTypes.CORE_OBJ_DATA_MODEL
                + dataModel.getSchema();
    }

    /**
     *
     * @param docModel
     * @return the key for DocumentModel based on
     *         {@link DocumentModel#getPathAsString()}
     */
    public static String getCacheKey(DocumentModel docModel) {
        //final String path = CacheableObjectTypes.CORE_OBJ_DOCUMENT_MODEL
        //        + docModel.getPathAsString();
        return getCacheKey(docModel.getRef());
    }

    /**
     * Constructs a cache key based on the doc reference which in most cases is
     * a document path  {@link DocumentRef#reference()}.
     *
     * @param docRef
     * @return
     */
    public static String getCacheKey(DocumentRef docRef) {
        // we take into account ID and PATH types only
        if (docRef.type() != DocumentRef.ID && docRef.type() != DocumentRef.PATH) {
            throw new IllegalArgumentException(
                    "Unsupported documentRef type for caching. Offending type: "
                            + docRef.type());
        }
        return CacheableObjectTypes.CORE_OBJ_DOCUMENT_MODEL
                + Integer.toString(docRef.type()) + '/' + docRef.reference();

        // return docRef.toString();
    }

    public static String getCacheKey(Object docRef) {
        if (docRef instanceof DocumentRef) {
            return getCacheKey((DocumentRef) docRef);
        } else {
            throw new IllegalArgumentException("Unsupported object type: "
                    + docRef.getClass());
        }
    }

    /**
     * This method pertains to ServerCache only... Used in Event Listener for
     * Document objects to invalidate entries in the cache. It work only with
     * documents referenced by their paths
     */
    public static String getCacheKeyForDocPath(String docPath) {

        return CacheableObjectTypes.CORE_OBJ_DOCUMENT_MODEL
                + Integer.toString(DocumentRef.PATH) + '/' + docPath;
    }

    /**
     * This method pertains to ServerCache only... Used in Event Listener for
     * Document objects to invalidate entries in the cache. It work only with
     * documents referenced by their UUIDs
     */
    public static String getCacheKeyForDocUUID(String docPath) {

        return CacheableObjectTypes.CORE_OBJ_DOCUMENT_MODEL
                + Integer.toString(DocumentRef.ID) + '/' + docPath;
    }

    /**
     * Constructs a cache key similar to the case when only docRef is given, but
     * adding as a suffix "/child/[childName]".
     *
     * @param docRef
     * @param childName
     * @return
     */
    public static String getCacheKey(DocumentRef docRef, String childName) {
        return getCacheKey(docRef) + "/child/" + childName;
    }

    public static String getCacheKey(Object docRef, String childName) {
        if (docRef instanceof DocumentRef) {
            return getCacheKey((DocumentRef) docRef, childName);
        } else {
            throw new IllegalArgumentException("Unsupported object type: "
                    + docRef.getClass());
        }
    }

    /**
     * Constructs a cache key similar to the case when only docRef is given, but
     * adding as a suffix "/ver/[version label]".
     *
     * @param docRef
     * @param version
     * @return
     */
    public static String getCacheKey(DocumentRef docRef, VersionModel version) {
        return getCacheKey(docRef) + "/ver/" + version.getLabel();
    }

    public static String getCacheKey(Object docRef, Object version) {
        if (docRef instanceof DocumentRef && version instanceof VersionModel) {
            return getCacheKey((DocumentRef) docRef, (VersionModel) version);
        } else {
            throw new IllegalArgumentException("Unsupported object type: "
                    + docRef.getClass() + " , version: " + version.getClass());
        }
    }

}
