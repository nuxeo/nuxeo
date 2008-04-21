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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.template;

import java.io.File;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface SiteManager {

    Scripting getScripting();

    SiteRoot getDefaultSiteRoot() throws Exception;

    SiteRoot getSiteRoot(String name) throws Exception;

    SitePageTemplate[] getSiteObjects();

    SitePageTemplate getTemplate(String name);

    void registerSiteObject(SitePageTemplate object);

    void unregisterSiteObject(String name);

    SitePageTemplate resolve(DocumentModel doc);

    void registerBinding(SiteObjectBinding binding);

    void unregisterBinding(SiteObjectBinding binding);

    SiteObjectBinding[] getBindings();

    SitePageTemplate getDefaultSiteObject();

    void setDefaultSiteObject(SitePageTemplate object);

    File getRootDirectory();

    void reset();
}
