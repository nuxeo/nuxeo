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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentRef;
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

    /**
     * The application path. An application is mapped to a path so that any
     * request that comes into an application path is delegated to the application
     *
     * @return the path. Cannot return null.
     */
    Path getPath();

    /**
     * Get the application path as a string
     * @return the application path as a string. Cannot be null
     */
    String getPathAsString();

    /**
     * Create a new web context given a servlet request and response
     * @param path the path info
     * @param req the servlet request
     * @param resp the servlet response
     * @return the context
     * @throws WebException if any error occurs
     */
    WebContext createContext(PathInfo pathInfo, HttpServletRequest req, HttpServletResponse resp) throws WebException;

    /**
     * Get the document root for this application if any
     * @return the document root reference or null if this application is not bound on a repository tree.
     */
    DocumentRef getDocumentRoot();

    /**
     * Set the document root to be used by this application
     * @param root
     */
    void setDocumentRoot(Path rootPath);

    /**
     * Set the application path
     */
    void setPath(Path path);

    /**
     * Given a document path return its relative path to the root document if any.
     * <p>
     * If no root document is not defined or the path cannot be made relative to the
     * root then null is returned
     * <br>
     * The returned path is always starting with a '/'
     *
     * @param docPath the path
     * @return the relative path or null if the relative path cannot be computed
     */
    Path getRelativeDocumentPath(Path docPath);

    /**
     * Tests if this application is the default repository view
     * @return
     */
    boolean isDefaultRepositoryView();

    RenderingEngine getRendering();

    String getTypeBinding(String type);

    WebObjectDescriptor getObjectDescriptor(Type type);

    void flushCache();

    String getErrorPage();

    String getIndexPage();

    String getDefaultPage();

    void setDefaultPage(String page);

    ScriptFile getFile(String path)  throws IOException;

    ScriptFile getActionScript(String action, DocumentType docType) throws IOException;

    WebEngine getWebEngine();

    void registerRenderingExtension(String id, Object obj);

    void unregisterRenderingExtension(String id);

    PathMapper getPathMapper();

    String getRepositoryName();

    void setRepositoryName(String repositoryName);
}
