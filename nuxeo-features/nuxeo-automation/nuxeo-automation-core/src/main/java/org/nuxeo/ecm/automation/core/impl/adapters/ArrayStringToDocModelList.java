/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @since 5.9.6
 */
public class ArrayStringToDocModelList implements TypeAdapter {

    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt)
            throws TypeAdaptException {
        String[] content = (String[]) objectToAdapt;
        DocumentModelList result = new DocumentModelListImpl();
        try {
            for (String value : content) {
                result.add(ctx.getCoreSession().getDocument(createRef(value)));
            }
        } catch (ClientException e) {
            throw new TypeAdaptException(e);
        }
        return result;
    }

    public DocumentRef createRef(String value) {
        return value.startsWith("/") ? new PathRef(value) : new IdRef(value);
    }

}
