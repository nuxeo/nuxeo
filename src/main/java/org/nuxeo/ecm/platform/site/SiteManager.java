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

package org.nuxeo.ecm.platform.site;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.platform.site.scripting.Scripting;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface SiteManager {

    Scripting getScripting();

    SiteRoot getDefaultSiteRoot() throws Exception;

    SiteRoot getSiteRoot(String name) throws Exception;

    File getRootDirectory();

    void registerObject(ObjectDescriptor obj);

    void unregisterObject(ObjectDescriptor obj);

    List<ObjectDescriptor> getRegisteredObjects();

    List<ObjectDescriptor> getPendingObjects();

    List<ObjectDescriptor> getResolvedObjects();

    ObjectDescriptor getInstanceOf(String type);

    public ObjectDescriptor getObject(String id);

    public boolean isObjectResolved(String id);

    void reset();
}
