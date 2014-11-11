/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

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

    public DocumentEventContext(CoreSession session, Principal principal, DocumentModel source) {
        super(session, principal, source, null);
    }

    public DocumentEventContext(CoreSession session, Principal principal, DocumentModel source,
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
