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
 */
package org.nuxeo.ecm.webengine.gwt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineGwtServlet extends RemoteServiceServlet {

    private static final long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog(WebEngineGwtServlet.class);

    public static boolean HOSTED_MODE = false;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            Class.forName("com.google.gwt.dev.HostedMode");
            HOSTED_MODE = true;
        } catch (Exception e) {
            HOSTED_MODE = false;
        }
    }

    /**
     * When in hosted mode the default mechanism is used.
     * In production mode the last path element from the request URL is considered
     * as the GWT module identifier and the GWT application root will be resolved to
     * <code>${nxserver}/web/root.war/gwt/gwtModuleId</code>
     * <p>
     * The GWT web application will be copied there at startup time by using the extension to
     * {@link InstallGwtAppComponent} extension point <code>install</code>.
     * in your GWT bundle.
     *
     * @see {@link #_doGetSerializationPolicy(HttpServletRequest, String, String)}
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(
            HttpServletRequest request, String moduleBaseURL, String strongName) {
        if (HOSTED_MODE) {
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } else { // We are in production mode : return webengine policy
            return _doGetSerializationPolicy(request, moduleBaseURL, strongName);
        }
    }

    protected SerializationPolicy _doGetSerializationPolicy(
            HttpServletRequest request, String moduleBaseURL, String strongName) {

        String modulePath = null;
        if (moduleBaseURL != null) {
          try {
            modulePath = new URL(moduleBaseURL).getPath();
          } catch (MalformedURLException ex) {
            // log the information, we will default
            log.warn("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
          }
        }
        String moduleId = new File(modulePath).getName();
        if (moduleId.length() == 0) {
            moduleId = "root";
        }

        File dir = GwtBundleActivator.GWT_ROOT;
        dir = new File(dir, moduleId);
        if (!dir.isDirectory()) { // use default
            log.warn("Could not find gwt resources in web/root.war/gwt for module "+moduleId);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        }
        String path = SerializationPolicyLoader.getSerializationPolicyFileName(strongName);
        log.debug("Found gwt serialization policy file: "+path);
        File policyFile = new File(dir, path);
        if (!policyFile.isFile()) {
            log.warn("Could not find gwt serialization policy file for module "+moduleId+" [ "+path+" ]");
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(policyFile);
          return SerializationPolicyLoader.loadFromStream(in, null);
        } catch (IOException e) {
            log.error("Failed to read gwt serialization policy file for module "+moduleId+" [ "+path+" ]", e);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } catch (ParseException e) {
            log.error("Failed to parse the policy file '"
                    + policyFile + "'", e);
            return super.doGetSerializationPolicy(request, moduleBaseURL, strongName);
        } finally {
            if (in != null) {
                try {in.close();} catch (IOException e) {log.error(e);}
            }
        }
    }

}
