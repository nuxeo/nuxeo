/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import java.util.Collection;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @since 10.3
 */
public class CollectionToBlobList implements TypeAdapter {

    @Override
    @SuppressWarnings("unchecked")
    public Object getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        Collection<Object> list = (Collection<Object>) objectToAdapt;
        BlobList result = new BlobList(list.size());
        for (Object val : list) {
            if (val instanceof Blob) {
                result.add((Blob) val);
            }
        }
        return result;
    }

}
