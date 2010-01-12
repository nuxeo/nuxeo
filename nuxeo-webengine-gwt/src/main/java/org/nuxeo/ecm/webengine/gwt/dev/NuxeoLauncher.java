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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.gwt.dev;

import java.util.ArrayList;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.dev.NuxeoApp;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.webengine.gwt.GwtBundleActivator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoLauncher extends NuxeoAuthenticationFilter {

    private static final long serialVersionUID = 1L;


    @Override
    public void init(FilterConfig config) throws ServletException {
        System.setProperty(GwtBundleActivator.GWT_DEV_MODE_PROP, "true");
        String home = config.getInitParameter("home");
        String h = config.getInitParameter("host");
        String p = config.getInitParameter("profile");
        String v = config.getInitParameter("version");
        String c = config.getInitParameter("config");
        if (h == null) h = "localhost:8081";
        if (v == null) v = "5.3.1-SNAPSHOT";        
        ArrayList<String> args = new ArrayList<String>();
        if (home == null) {
            String userDir = System.getProperty("user.home");
            String sep = userDir.endsWith("/") ? "" : "/";
            args.add(userDir+sep+".nxserver-gwt");
        } else {
            home = StringUtils.expandVars(home, System.getProperties());
            args.add(home);
        }
        args.add("-h");
        args.add(h);
        args.add("-v");
        args.add(v);    
        if (c != null) {
            args.add("-c"); 
            args.add(c);            
        } else {
            if (p == null) p = NuxeoApp.CORE_SERVER;
            args.add("-p"); 
            args.add(p);
        }
        try {
            org.nuxeo.dev.Main.main(args.toArray(new String[args.size()]));
            frameworkStarted();
        } catch (Exception e) {
            System.err.println("Failed to start nuxeo");
            throw new ServletException(e);
        }
        super.init(config);
    }

    protected void frameworkStarted() {
        // do nothing
    }

}
