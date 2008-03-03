/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.events.api.impl;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.events.api.RepositoryChangeEvent;

/**
 * @author Max Stepanov
 *
 */
public class RepositoryChangeEventImpl implements RepositoryChangeEvent {

    private static final long serialVersionUID = -2608403896092624758L;

    private final String originSessionId;
    private final String repository;
    private final int type;
    private final DocumentRef targetRef;
    private final Object details;

    public RepositoryChangeEventImpl(String originSessionId, String repository,
            int type, DocumentRef targetRef, Object details) {
        this.originSessionId = originSessionId;
        this.repository = repository;
        this.type = type;
        this.targetRef = targetRef;
        this.details = details;
    }

    public String getOriginSessionId() {
        return originSessionId;
    }

    public int getType() {
        return type;
    }

    public String getRepositoryName() {
        return repository;
    }

    public DocumentRef getTargetDocumentRef() {
        return targetRef;
    }

    public Object getDetails() {
        return details;
    }

}
