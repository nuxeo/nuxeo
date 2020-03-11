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
package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Specialized implementation to be used with an abstract session
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author tiry
 */
public class DocumentEventContext extends EventContextImpl {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY_PROPERTY_KEY = "category";

    public static final String COMMENT_PROPERTY_KEY = "comment";

    public DocumentEventContext(CoreSession session, NuxeoPrincipal principal, DocumentModel source) {
        super(session, principal, source, null);
    }

    public DocumentEventContext(CoreSession session, NuxeoPrincipal principal, DocumentModel source,
            DocumentRef destDoc) {
        super(session, principal, source, destDoc);
    }

    public DocumentModel getSourceDocument() {
        return (DocumentModel) args[0];
    }

    public DocumentRef getDestination() {
        return (DocumentRef) args[1];
    }

    public String getCategory() {
        Serializable data = getProperty(CATEGORY_PROPERTY_KEY);
        if (data instanceof String) {
            return (String) data;
        }
        return null;
    }

    public void setCategory(String category) {
        setProperty(CATEGORY_PROPERTY_KEY, category);
    }

    public String getComment() {
        Serializable data = getProperty(COMMENT_PROPERTY_KEY);
        if (data instanceof String) {
            return (String) data;
        }
        return null;
    }

    public void setComment(String comment) {
        setProperty(COMMENT_PROPERTY_KEY, comment);
    }

    @Override
    public void setProperties(Map<String, Serializable> properties) {
        // preserve Category/Comment from transparent override
        String comment = getComment();
        String category = getCategory();
        super.setProperties(properties);
        if (getComment() == null) {
            setComment(comment);
        }
        if (getCategory() == null) {
            setCategory(category);
        }
    }

}
