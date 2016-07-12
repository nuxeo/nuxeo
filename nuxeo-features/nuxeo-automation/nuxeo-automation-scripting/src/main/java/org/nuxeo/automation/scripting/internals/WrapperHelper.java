/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.automation.scripting.internals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.objects.NativeArray;

/**
 * @since 8.4
 */
public class WrapperHelper {

    private WrapperHelper() {
        // empty
    }

    public static Object wrap(Object object, CoreSession session) {
        if (object instanceof DocumentModel) {
            return new DocumentScriptingWrapper(session, (DocumentModel) object);
        } else if (object instanceof DocumentModelList) {
            List<DocumentScriptingWrapper> docs = new ArrayList<>();
            for (DocumentModel doc : (DocumentModelList) object) {
                docs.add(new DocumentScriptingWrapper(session, doc));
            }
            return docs;
        }
        return object;
    }

    public static Object unwrap(Object object) {
        // First unwrap object if it's a nashorn object
        Object result = object;
        if (result instanceof ScriptObjectMirror) {
            result = ScriptObjectMirrors.unwrap((ScriptObjectMirror) result);
        }
        // TODO: not sure if this code is used, but we shouldn't use NativeArray as it's an internal class of nashorn
        if (result instanceof NativeArray) {
            result = Arrays.asList(((NativeArray) result).asObjectArray());
        }
        // Second unwrap object
        if (result instanceof DocumentScriptingWrapper) {
            result = ((DocumentScriptingWrapper) result).getDoc();
        } else if (result instanceof List<?>) {
            List<?> l = (List<?>) result;
            // Several possible cases here:
            // - l is of type DocumentModelList or BlobList -> already in right type
            // - l is a list of DocumentScriptingWrapper -> elements need to be unwrapped into a DocumentModelList
            // - l is a list of DocumentWrapper -> l needs to be converted to DocumentModelList
            // - l is a list of Blob -> l needs to be converted to BlobList
            // - l is a list -> do nothing
            if (l.size() > 0 && !(result instanceof DocumentModelList || result instanceof BlobList)) {
                Object first = l.get(0);
                if (first instanceof DocumentModel) {
                    result = l.stream()
                              .map(DocumentModel.class::cast)
                              .collect(Collectors.toCollection(DocumentModelListImpl::new));
                } else if (first instanceof Blob) {
                    result = l.stream().map(Blob.class::cast).collect(Collectors.toCollection(BlobList::new));
                } else if (first instanceof DocumentScriptingWrapper) {
                    result = l.stream()
                              .map(DocumentScriptingWrapper.class::cast)
                              .map(DocumentScriptingWrapper::getDoc)
                              .collect(Collectors.toCollection(DocumentModelListImpl::new));
                }
            }
        }
        return result;
    }

}
