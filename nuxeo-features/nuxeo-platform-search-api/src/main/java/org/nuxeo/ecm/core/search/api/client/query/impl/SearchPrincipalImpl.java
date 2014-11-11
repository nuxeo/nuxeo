/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SearchPrincipalImpl.java 28515 2008-01-06 20:37:29Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query.impl;

import java.io.Serializable;
import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;

/**
 * Search principal implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class SearchPrincipalImpl implements SearchPrincipal {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SearchPrincipalImpl.class);

    protected String name;

    protected String[] groups;

    protected boolean systemUser = false;

    protected Serializable originalPrincipal;

    public SearchPrincipalImpl() {
    }

    public SearchPrincipalImpl(String name) {
        this.name = name;
    }

    public SearchPrincipalImpl(String name, String[] groups, boolean systemUser) {
        this.name = name;
        this.groups = groups;
        this.systemUser = systemUser;
    }

    public SearchPrincipalImpl(String name, String[] groups,
            boolean systemUser, Principal originalPrincipal) {
        this(name, groups, systemUser);
        if (originalPrincipal != null) {
            if (originalPrincipal instanceof Serializable) {
                this.originalPrincipal = (Serializable)originalPrincipal;
            } else {
                log.warn("Principal with name= "
                        + originalPrincipal.getName()
                        + " is not serializble. Cannot store it on SearchPrincipal...");
            }
        }
    }

    public String[] getGroups() {
        if (groups == null) {
            groups = new String[0];
        }
        return groups;
    }

    public String getName() {
        return name;
    }

    public boolean isSystemUser() {
        return systemUser;
    }

    public Serializable getOriginalPrincipal() {
        return originalPrincipal;
    }

}
