/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.audit.api.comment;

/**
 * Simple POJO to store pre-processed comment and associated document.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class UIAuditComment {

    protected final String comment;
    protected final LinkedDocument linkedDoc;

    public UIAuditComment(String comment,LinkedDocument linkedDoc) {
        this.comment=comment;
        this.linkedDoc = linkedDoc;
    }

    public String getComment() {
        return comment;
    }

    public LinkedDocument getLinkedDoc() {
        return linkedDoc;
    }

}
