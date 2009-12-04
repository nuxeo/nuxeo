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

package org.nuxeo.ecm.platform.web.common.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;

public class AggregatedJSProvider extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    protected static Map<String, String> cachedResponses = new HashMap<String, String>();

    private static final Log log = LogFactory
            .getLog(AggregatedJSProvider.class);

    protected static JSMinimizer minimizer = null;
    public static final String MINIMIZER_IMPL_KEY = "org.nuxeo.ecm.platform.web.common.resources.JSMinimizer";

    protected static final String SCRIPT_SEP = "\\|";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String scriptsStr = req.getParameter("scripts");
        String refreshStr = req.getParameter("refresh");
        String minimizeStr = req.getParameter("minimize");
        boolean minimize = "true".equalsIgnoreCase(minimizeStr);
        boolean refresh = "true".equalsIgnoreCase(refreshStr);

        if (scriptsStr == null) {
            super.doGet(req, resp);
            return;
        }

        String cacheKey = scriptsStr + "*" + minimize;

        String resultScript = null;
        if (!refresh) {
            cacheLock.readLock().lock();
            try {
                resultScript = cachedResponses.get(cacheKey);
            } finally {
                cacheLock.readLock().unlock();
            }
        }

        if (resultScript == null) {
            String[] scripts = scriptsStr.split(SCRIPT_SEP);
            resultScript = computeResult(scripts, minimize);

            cacheLock.writeLock().lock();
            try {
                cachedResponses.put(cacheKey, resultScript);
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        resp.getWriter().write(resultScript);
    }

    protected String computeResult(String[] scripts, boolean minimize)
            throws IOException {
        String fsPath = getServletContext().getRealPath("/");

        Path dirPath = new Path(fsPath).append("scripts");

        StringBuffer buf = new StringBuffer();
        for (String script : scripts) {

            script = new Path(script).lastSegment(); // be sure to remove any
                                                        // ../
            Path scriptPath = dirPath.append(script);
            File scriptFile = new File(scriptPath.toString());
            if (scriptFile.exists()) {
                buf.append("// *******************************\n");
                buf.append("// include script " + script);
                buf.append("\n");

                InputStream is = new FileInputStream(scriptFile);
                if (is != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(is));
                    String line = null;
                    StringBuilder sb = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    is.close();
                    buf.append(sb.toString());
                    buf.append("\n");
                }
            }
        }
        if (minimize) {
            return minimize(buf.toString());
        } else {
            return buf.toString();
        }
    }

    protected String minimize(String jsContent) {
        return getMinimizer().minimize(jsContent);
    }

    protected JSMinimizer getMinimizer() {
        if (minimizer == null) {
            String minimizerClassName = getServletContext().getInitParameter(
                    MINIMIZER_IMPL_KEY);
            if (minimizerClassName == null) {
                minimizerClassName = getInitParameter(MINIMIZER_IMPL_KEY);
            }
            if (minimizerClassName != null) {

                try {
                    Class<?> minimizerClass = Thread.currentThread()
                            .getContextClassLoader().loadClass(
                                    minimizerClassName);
                    minimizer = (JSMinimizer) minimizerClass.newInstance();
                } catch (Exception e) {
                    log.error("Error while getting minimizer implementation", e);
                }
            }
        }

        if (minimizer == null) {
            minimizer = new DummyMinimizer();
        }

        return minimizer;
    }

    protected class DummyMinimizer implements JSMinimizer {

        public String minimize(String jsScriptContent) {
            StringBuffer sb = new StringBuffer();
            sb.append("// No (correct) JSMinimizer implementation class was defined\n");
            sb.append("please check web.xml to set init parameter\n");
            sb.append(jsScriptContent);
            return sb.toString();
        }

    }
}
