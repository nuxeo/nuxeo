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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security.impl;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecuritySummaryEntry;

public class SecuritySummaryEntryImpl implements SecuritySummaryEntry, Serializable{

    private static final long serialVersionUID = 167576576474L;

    private final ACP acp;
    private final IdRef idRef;
    private final PathRef docPath;


    public SecuritySummaryEntryImpl(IdRef idRef, PathRef docPath, ACP acp) {
        this.idRef = idRef;
        this.docPath = docPath;
        this.acp = acp;
    }

    @Override
    public ACP getAcp() {
        return acp;
    }

    @Override
    public PathRef getDocPath() {
        return docPath;
    }

    @Override
    public IdRef getIdRef() {
        return idRef;
    }

}
