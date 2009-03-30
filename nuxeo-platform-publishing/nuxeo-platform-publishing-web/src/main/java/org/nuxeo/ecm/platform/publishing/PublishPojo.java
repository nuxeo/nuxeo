/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.publishing;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Simple POJO used in order to store additional info needed to be
 * displayed in 'TAB_PUBLISH' page.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin BAICAN</a>
 */
public class PublishPojo {

    DocumentModel section;

    String proxyVersion;

    public PublishPojo(DocumentModel section, String proxyVersion) {
        super();
        this.section = section;
        this.proxyVersion = proxyVersion;
    }

    public DocumentModel getSection() {
        return section;
    }

    public void setSection(DocumentModel section) {
        this.section = section;
    }

    public String getProxyVersion() {
        return proxyVersion;
    }

    public void setProxyVersion(String proxyVersion) {
        this.proxyVersion = proxyVersion;
    }

}
