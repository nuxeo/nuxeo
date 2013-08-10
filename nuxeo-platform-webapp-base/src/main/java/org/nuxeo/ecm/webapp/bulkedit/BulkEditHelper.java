/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper used for bulk edit actions
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class BulkEditHelper {

    private static final Log log = LogFactory.getLog(BulkEditHelper.class);

    /**
     * @deprecated since 5.7.3. Now in {@link BulkEditService}.
     */
    @Deprecated
    public static final String BULK_EDIT_PREFIX = "bulkEdit/";

    /**
     * @deprecated since 5.7.3. Now in {@link BulkEditService}.
     */
    @Deprecated
    public static final String CONTEXT_DATA = "contextData";

    private BulkEditHelper() {
        // Helper class
    }

    /**
     * Returns the common layouts of the {@code docs} for the {@code edit} mode.
     */
    public static List<String> getCommonLayouts(TypeManager typeManager,
            List<DocumentModel> docs) {
        return getCommonLayouts(typeManager, docs, BuiltinModes.EDIT);
    }

    /**
     * Returns the common layouts of the {@code docs} for the given layout
     * {@code mode}.
     */
    public static List<String> getCommonLayouts(TypeManager typeManager,
            List<DocumentModel> docs, String mode) {
        List<String> layouts = null;
        for (DocumentModel doc : docs) {
            Type type = typeManager.getType(doc.getType());
            List<String> typeLayouts = Arrays.asList(type.getLayouts(mode));
            if (layouts == null) {
                // first document
                layouts = new ArrayList<String>();
                layouts.addAll(typeLayouts);
            } else {
                layouts.retainAll(typeLayouts);
            }
        }
        return layouts;
    }

    /**
     * Returns the common schemas of the {@code docs}.
     */
    public static List<String> getCommonSchemas(List<DocumentModel> docs) {
        List<String> schemas = null;
        for (DocumentModel doc : docs) {
            List<String> docSchemas = Arrays.asList(doc.getSchemas());
            if (schemas == null) {
                // first document
                schemas = new ArrayList<String>();
                schemas.addAll(docSchemas);
            } else {
                schemas.retainAll(docSchemas);
            }
        }
        return schemas;
    }

    /**
     * Copy all the marked properties (stored in the ContextData of
     * {@code sourceDoc}) from {@code sourceDoc} to all the {@code targetDocs}.
     *
     * @param session the {@code CoreSession} to use
     * @param sourceDoc the doc where to get the metadata to copy
     * @param targetDocs the docs where to set the metadata
     *
     * @deprecated since 5.7.3. Now in {@link BulkEditService}.
     */
    @Deprecated
    public static void copyMetadata(CoreSession session,
            DocumentModel sourceDoc, List<DocumentModel> targetDocs)
            throws ClientException {
        Framework.getLocalService(BulkEditService.class).updateDocuments(session,
                sourceDoc, targetDocs);
    }

    /**
     * Extracts the properties to be copied from {@code sourceDoc}. The
     * properties are stored in the ContextData of {@code sourceDoc}: the key is
     * the xpath property, the value is {@code true} if the property has to be
     * copied, {@code false otherwise}.
     *
     * @deprecated since 5.7.3. Now in {@link BulkEditServiceImpl}.
     */
    @Deprecated
    protected static List<String> getPropertiesToCopy(DocumentModel sourceDoc) {
        List<String> propertiesToCopy = new ArrayList<String>();
        for (Map.Entry<String, Serializable> entry : sourceDoc.getContextData().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(BULK_EDIT_PREFIX)) {
                String[] properties = key.replace(BULK_EDIT_PREFIX, "").split(
                        " ");
                Serializable value = entry.getValue();
                if (value instanceof Boolean && (Boolean) value) {
                    for (String property : properties) {
                        if (!property.startsWith(CONTEXT_DATA)) {
                            propertiesToCopy.add(property);
                        }
                    }
                }
            }
        }
        return propertiesToCopy;
    }

}
