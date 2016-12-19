/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;

/**
 * Helper used for bulk edit actions
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class BulkEditHelper {

    private static final Log log = LogFactory.getLog(BulkEditHelper.class);

    private BulkEditHelper() {
        // Helper class
    }

    /**
     * Returns the common layouts of the {@code docs} for the {@code edit} mode.
     */
    public static List<String> getCommonLayouts(TypeManager typeManager, List<DocumentModel> docs) {
        return getCommonLayouts(typeManager, docs, BuiltinModes.EDIT);
    }

    /**
     * Returns the common layouts of the {@code docs} for the given layout {@code mode}.
     */
    public static List<String> getCommonLayouts(TypeManager typeManager, List<DocumentModel> docs, String mode) {
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

}
