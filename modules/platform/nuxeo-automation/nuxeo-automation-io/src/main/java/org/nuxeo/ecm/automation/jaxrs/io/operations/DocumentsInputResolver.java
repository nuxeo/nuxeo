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
 *     matic
 */
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.impl.DocumentRefListImpl;

/**
 * @author matic
 */
public class DocumentsInputResolver implements InputResolver<DocumentRefList> {

    @Override
    public String getType() {
        return "docs";
    }

    @Override
    public DocumentRefList getInput(String input) {
        String[] ar = StringUtils.split(input, ',', true);
        DocumentRefList list = new DocumentRefListImpl(ar.length);
        for (String s : ar) {
            list.add(DocumentInputResolver.docRefFromString(s));
        }
        return list;
    }

}
