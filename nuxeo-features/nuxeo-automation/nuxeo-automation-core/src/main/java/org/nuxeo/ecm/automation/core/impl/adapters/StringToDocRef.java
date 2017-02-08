/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl.adapters;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.TypeAdaptException;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.impl.adapters.helper.TypeAdapterHelper;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StringToDocRef implements TypeAdapter {

    @Override
    public DocumentRef getAdaptedValue(OperationContext ctx, Object objectToAdapt) throws TypeAdaptException {
        try {
            String value = (String) objectToAdapt;
            return TypeAdapterHelper.createDocumentRef(ctx, value);
        } catch (TypeAdaptException e) {
            throw e;
        } catch (NuxeoException e) {
            throw new TypeAdaptException(e);
        }
    }

    /**
     * @deprecated since 9.1, see {@link TypeAdapterHelper#createDocumentRef(String)} instead
     */
    @Deprecated
    public static DocumentRef createRef(String value) {
        return TypeAdapterHelper.createDocumentRef(value);
    }
}
