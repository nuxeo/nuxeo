/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.audit.service.extension;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;

/**
 * Descriptor to configure / contribute a Backend for Audit service
 * 
 * @author tiry
 *
 */
@XObject("backend")
public class AuditBackendDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @XNode("@class")
    protected Class<AuditBackend> klass;

    public Class<AuditBackend> getKlass() {
        return klass;
    }
    
    public AuditBackend newInstance() throws Exception {
        return klass.newInstance();
    }
    
}
