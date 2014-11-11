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
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StringToDocRef implements TypeAdapter {

    public DocumentRef getAdapter(OperationContext ctx, Object objectToAdapt) throws Exception {
        String value = (String)objectToAdapt;
        if (value.startsWith(".")) {
            Object obj = Scripting.newExpression("Document.resolvePathAsRef(\""+value+"\")").eval(ctx);
            if (obj instanceof DocumentModel) {
                return ((DocumentModel)obj).getRef();
            } else if (obj instanceof DocumentRef) {
                return (DocumentRef)obj;
            } else {
                return null;
            }
        }
        return createRef(value);
    }

    public static DocumentRef createRef(String value) throws Exception {
        return value.startsWith("/") ?
                new PathRef(value) : new IdRef(value);
    }

}
