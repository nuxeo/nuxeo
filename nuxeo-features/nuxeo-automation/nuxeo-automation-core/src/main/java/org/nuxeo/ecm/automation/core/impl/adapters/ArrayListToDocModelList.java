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

package org.nuxeo.ecm.automation.core.impl.adapters;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.impl.adapters.helper.TypeAdapterHelper;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

import java.util.Collection;

/**
 * @since 9.1
 */
public class ArrayListToDocModelList implements TypeAdapter {

    @Override
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        Collection<Object> list = (Collection<Object>) objectToAdapt;
        DocumentModelList result = new DocumentModelListImpl(list.size());
        try {
            for (Object val : list) {
                if(val instanceof String) {
                    result.add(TypeAdapterHelper.createDocumentModel(ctx, (String) val));
                }
                else if (val instanceof DocumentRef) {
                    result.add(TypeAdapterHelper.createDocumentModel(ctx, (DocumentRef) val));
                }
            }
        } catch (DocumentNotFoundException e) {
            throw new TypeAdaptException(e);
        }
        return result;
    }

}
