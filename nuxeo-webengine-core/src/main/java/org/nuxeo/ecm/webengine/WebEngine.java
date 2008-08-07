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

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.forms.FormManager;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.runtime.deploy.FileChangeNotifier;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface WebEngine {

    /**
     * Create a request context
     * @param req
     * @param resp
     * @return the context. Cannot return null
     * @throws WebException
     */
    WebContext createContext(HttpServletRequest req, HttpServletResponse resp) throws WebException;

    /**
     * Get the URL path for the given document path
     * @param docPath
     * @return the url path or null if cannot be computed
     */
    Path getUrlPath(Path docPath);

    WebApplication getDefaultApplication();

    ResourceBundle getMessageBundle();

    Scripting getScripting();

    File getRootDirectory();

    void registerObject(WebObjectDescriptor obj);

    void unregisterObject(WebObjectDescriptor obj);

    String getTypeBinding(String type);

    void registerBinding(String type, String objectId);

    void unregisterBinding(String type);

    void addApplicationMapping(WebApplicationMapping mapping);

    void removeApplicationMapping(WebApplicationMapping mapping);

    Collection<WebObjectDescriptor> getObjects();

    WebObjectDescriptor getObject(String id);

    void reset();

    Map<String, Object> getEnvironment();

    WebApplication getApplication(String name);

    WebApplication getApplicationByPath(Path path);

    void registerApplication(WebApplicationDescriptor desc) throws WebException;

    void unregisterApplication(String id);

    WebApplication[]  getApplications();

    void addConfigurationChangedListener(ConfigurationChangedListener listener);

    void removeConfigurationChangedListener(ConfigurationChangedListener listener);

    void fireConfigurationChanged() throws WebException;

    void registerRenderingExtension(String id, Object obj);

    void unregisterRenderingExtension(String id);

    Object getRenderingExtension(String id);

    Map<String, Object> getRenderingExtensions();

    FileChangeNotifier getFileChangeNotifier();

    void destroy();

    /**
     * Get the form manager
     * @return the form manager instance. cannot be null
     */
    FormManager getFormManager();

}
