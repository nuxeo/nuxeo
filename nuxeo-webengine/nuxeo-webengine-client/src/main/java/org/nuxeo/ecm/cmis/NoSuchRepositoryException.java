/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.cmis;


/**
 * @author matic
 *
 */
public class NoSuchRepositoryException extends ContentManagerException {

    private static final long serialVersionUID = -3017945902792388422L;

    public final String baseURL;

    public final String repositoryId;

    public NoSuchRepositoryException(String baseURL, String id) {
        super("no such repository " + id + " in "
                + baseURL);
        this.baseURL = baseURL;
        this.repositoryId = id;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

}
