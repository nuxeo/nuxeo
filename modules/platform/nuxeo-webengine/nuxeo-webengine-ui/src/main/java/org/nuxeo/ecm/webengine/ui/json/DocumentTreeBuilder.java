/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.json;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentTreeBuilder extends JQueryTreeBuilder<DocumentModel> {

    private static final Log log = LogFactory.getLog(DocumentTreeBuilder.class);

    protected final CoreSession session;

    public DocumentTreeBuilder(CoreSession session) {
        this.session = session;
    }

    @Override
    protected String getName(DocumentModel obj) {
        return obj.getName();
    }

    @Override
    protected Collection<DocumentModel> getChildren(DocumentModel obj) {
        return session.getChildren(obj.getRef());
    }

    @Override
    protected Map<String, Object> toJson(DocumentModel obj) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("text", obj.getName());
        json.put("id", obj.getPathAsString());
        return json;
    }

    @Override
    protected DocumentModel getObject(String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
