/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StringToDocRef implements TypeAdapter {

    public DocumentRef getAdaptedValue(OperationContext ctx,
            Object objectToAdapt) throws TypeAdaptException {
        try {
            String value = (String) objectToAdapt;
            if (value.startsWith(".")) {
                Object obj = Scripting.newExpression(
                        "Document.resolvePathAsRef(\"" + value + "\")").eval(
                        ctx);
                if (obj instanceof DocumentModel) {
                    return ((DocumentModel) obj).getRef();
                } else if (obj instanceof DocumentRef) {
                    return (DocumentRef) obj;
                }
                throw new TypeAdaptException(String.format(
                        "Cannot adapt value '%s' to a DocumentRef instance",
                        value));
            }
            return createRef(value);
        } catch (TypeAdaptException e) {
            throw e;
        } catch (Exception e) {
            throw new TypeAdaptException(e);
        }
    }

    public static DocumentRef createRef(String value) {
        return value.startsWith("/") ? new PathRef(value) : new IdRef(value);
    }

}
