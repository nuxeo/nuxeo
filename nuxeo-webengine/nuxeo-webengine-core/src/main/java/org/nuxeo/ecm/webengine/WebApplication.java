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

package org.nuxeo.ecm.webengine;

import java.io.IOException;

import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.webengine.mapping.PathMapper;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebApplication extends ResourceLocator {

    /**
     * The application identifier.
     * @return the identifier. Cannot be null.
     */
    String getId();

    RenderingEngine getRendering();

    String getTypeBinding(String type);

    WebObjectDescriptor getObjectDescriptor(Type type);

    void flushCache();

    String getErrorPage();

    String getIndexPage();

    String getDefaultPage();

    void setDefaultPage(String defaultPage);

    ScriptFile getFile(String path)  throws IOException;

    ScriptFile getActionScript(String action, DocumentType docType) throws IOException;

    WebEngine getWebEngine();

    void registerRenderingExtension(String id, Object obj);

    void unregisterRenderingExtension(String id);

    PathMapper getPathMapper();

    String getRepositoryName();

    void setRepositoryName(String repositoryName);

}
