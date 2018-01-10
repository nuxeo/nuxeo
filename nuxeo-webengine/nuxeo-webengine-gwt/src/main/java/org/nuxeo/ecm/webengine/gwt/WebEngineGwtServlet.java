/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.gwt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineGwtServlet extends RemoteServiceServlet {

    private static final long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog(WebEngineGwtServlet.class);


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * When in hosted mode the default mechanism is used. In production mode the last path element from the request URL
     * is considered as the GWT module identifier and the GWT application root will be resolved to
     * <code>${nxserver}/web/root.war/gwt/gwtModuleId</code>
     * <p>
     * The GWT web application will be copied there at startup time by using the extension to
     * {@link InstallGwtAppComponent} extension point <code>install</code>. in your GWT bundle.
     *
     * @see {@link #_doGetSerializationPolicy(HttpServletRequest, String, String)}
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL,
            String strongName) {
        try {
            return _doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } catch (FileNotFoundException cause) {
            throw new NuxeoException("Cannot find serialization policy for " + moduleBaseURL, cause);
        }
    }

    protected SerializationPolicy _doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL,
            String strongName) throws FileNotFoundException  {

        String modulePath;
        try {
            modulePath = new URL(moduleBaseURL).getPath();
        } catch (MalformedURLException ex) {
            // log the information, we will default
            log.warn("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        }
        String moduleId = new File(modulePath).getName();
        if (moduleId.length() == 0) {
            moduleId = "root";
        }
        String filename = SerializationPolicyLoader.getSerializationPolicyFileName(strongName);
        File policyFile = Framework.getService(GwtResolver.class).resolve(moduleId+"/"+filename);
        if (policyFile == null || !policyFile.isFile()) {
            log.warn("Could not find gwt serialization policy file for module " + moduleId + " [ " + filename + " ]");
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        }
        log.debug("Found gwt serialization policy file: " + policyFile);
        FileInputStream in = null;
        try {
            in = new FileInputStream(policyFile);
            return SerializationPolicyLoader.loadFromStream(in, null);
        } catch (IOException e) {
            log.error("Failed to read gwt serialization policy file for module " + moduleId + " [ " + filename + " ]", e);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } catch (ParseException e) {
            log.error("Failed to parse the policy file '" + policyFile + "'", e);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

}
