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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: PublishingInformation.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.api;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Helper class to display a publishing information
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class PublishingInformation {

    private final DocumentModel proxy;

    private final DocumentModel section;

    public PublishingInformation(final DocumentModel proxy,
            final DocumentModel section) {
        this.proxy = proxy;
        this.section = section;
    }

    public DocumentModel getProxy() {
        return proxy;
    }

    public DocumentModel getSection() {
        return section;
    }

}
