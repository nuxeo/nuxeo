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
