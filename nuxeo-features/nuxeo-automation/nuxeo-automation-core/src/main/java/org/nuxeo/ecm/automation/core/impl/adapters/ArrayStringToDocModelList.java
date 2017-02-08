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
import org.nuxeo.ecm.automation.core.impl.adapters.helper.TypeAdapterHelper;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @since 6.0
 */
public class ArrayStringToDocModelList implements TypeAdapter {

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        String[] content = (String[]) objectToAdapt;
        DocumentModelList result = new DocumentModelListImpl();
        try {
            for (String value : content) {
                result.add(ctx.getCoreSession().getDocument(TypeAdapterHelper.createRef(ctx, value)));
            }
        } catch (DocumentNotFoundException e) {
            throw new TypeAdaptException(e);
        }
        return result;
    }

}
