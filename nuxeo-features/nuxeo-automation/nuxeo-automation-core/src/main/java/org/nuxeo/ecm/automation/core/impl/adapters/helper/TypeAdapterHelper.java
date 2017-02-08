/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.automation.core.impl.adapters.helper;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * Helper for type adapters
 *
 * @since 9.1
 */
public class TypeAdapterHelper {

    /**
     * Create document reference from its path
     *
     * @param value the document path
     * @return the document reference
     */
    public static DocumentRef createDocumentRef(String value) {
        return value.startsWith("/") ? new PathRef(value) : new IdRef(value);
    }

    /**
     * Create a document reference from its path
     *
     * @param value the document path
     * @return the document reference
     */
    public static Object createDocumentRefOrExpression(String value) {
        if (value.startsWith(".")) {
            return Scripting.newExpression("Document.resolvePathAsRef(\"" + value + "\")");
        } else {
            return value.startsWith("/") ? new PathRef(value) : new IdRef(value);
        }
    }

    /**
     * Create a document reference from its path
     *
     * @param ctx the operation context
     * @param value the document path
     * @return the document reference
     */
    public static DocumentRef createDocumentRef(OperationContext ctx, String value) throws TypeAdaptException {
        Object obj = createDocumentRefOrExpression(value);
        if (obj instanceof DocumentRef) {
            return (DocumentRef) obj;
        } else if (obj instanceof Expression) {
            if (ctx != null) {
                obj = ((Expression) obj).eval(ctx);
            }
            if (obj instanceof DocumentModel) {
                return ((DocumentModel) obj).getRef();
            } else if (obj instanceof DocumentRef) {
                return (DocumentRef) obj;
            }
            throw new TypeAdaptException(String.format("Cannot adapt value '%s' to a DocumentRef instance", value));
        } else {
            throw new RuntimeException(String.format("Unhandled value: %s", value));
        }
    }

    /**
     * Create a document model from its path
     *
     * @param ctx the operation context
     * @param value the document path
     * @return the document model
     */
    public static DocumentModel createDocumentModel(OperationContext ctx, String value) throws TypeAdaptException {
        DocumentRef docRef = createDocumentRef(ctx, value);
        return ctx.getCoreSession().getDocument(docRef);
    }
}
