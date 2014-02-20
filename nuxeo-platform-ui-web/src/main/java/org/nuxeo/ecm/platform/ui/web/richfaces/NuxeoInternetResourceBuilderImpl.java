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

package org.nuxeo.ecm.platform.ui.web.richfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.resource.JarResource;
import org.nuxeo.common.utils.Path;
import org.richfaces.renderkit.html.ResourceRenderer;

/**
 * Extended {@link ResourceBuilderImpl} that tries to dynamically generate the
 * JS packs.
 *
 * @author tiry
 */
public class NuxeoInternetResourceBuilderImpl extends ResourceBuilderImpl {

    private static final Log log = LogFactory.getLog(NuxeoInternetResourceBuilderImpl.class);

    protected static final String JS_FRAMEWORK_ALL_KEY = "/org/ajax4jsf/framework.pack.js";

    protected static final String JS_UI_ALL_KEY = "/org/richfaces/ui.pack.js";

    protected final Map<String, String> jsRegistredResources = new HashMap<String, String>();

    protected final List<String> blackListedScripts = new ArrayList<String>();

    protected InternetResource UIPack;

    protected InternetResource A4JPack;

    protected void trackRegistredJSResource(String key,
            InternetResource resource) {
        if (key == null) {
            key = resource.getKey();
        }
        if (key != null && key.endsWith(".js")) {
            if (resource instanceof JarResource) {
                JarResource jarRes = (JarResource) resource;
                String path = jarRes.getPath();
                if (!jsRegistredResources.values().contains(key)) {
                    jsRegistredResources.put(key, path);
                }
            } else {
                log.debug("Non Jar resource for key " + key);
            }
        }
    }

    public List<String> getJSResourcesToInclude() {
        List<String> scripts = new ArrayList<String>();
        List<String> blackList = getBlackListedScripts();

        for (String key : jsRegistredResources.keySet()) {

            String scriptNameByKey = new Path(key).lastSegment();
            String scriptNameByPath = new Path(jsRegistredResources.get(key)).lastSegment();

            if (!blackList.contains(scriptNameByKey)
                    && !blackList.contains(scriptNameByPath)) {
                scripts.add(key);
            } else {
                log.debug("bypass script with key " + key);
            }
        }
        return scripts;
    }

    @Override
    public void addResource(InternetResource resource) {
        String key = resource.getKey();
        log.info("### adding resource : implicit key = " + resource.getKey());
        trackRegistredJSResource(null, resource);
        super.addResource(resource);
    }

    @Override
    public void addResource(String key, InternetResource resource) {
        log.info("### adding resource : explicit key = " + key);
        trackRegistredJSResource(key, resource);
        super.addResource(key, resource);
    }

    /*
     * @Override public InternetResource getResourceForKey(String key) throws
     * ResourceNotFoundException { if (JS_FRAMEWORK_ALL_KEY.equals(key)) {
     * return super.getResourceForKey(key); } else if
     * (JS_UI_ALL_KEY.equals(key)) { return super.getResourceForKey(key); }
     * else { return super.getResourceForKey(key); } }
     */

    @Override
    public InternetResource getResource(String key)
            throws ResourceNotFoundException {

        InternetResource res = null;

        if (JS_FRAMEWORK_ALL_KEY.equals(key)) {
            res = super.getResource(key);
            res = getA4JPack(res, key);
        } else if (JS_UI_ALL_KEY.equals(key)) {
            res = super.getResource(key);
            res = getUIPack(res, key);
        } else {
            res = super.getResource(key);
        }
        return res;
    }

    protected InternetResource getA4JPack(InternetResource defaultResource,
            String key) {
        if (A4JPack == null) {
            buildA4JPack(defaultResource, key);
        }
        return A4JPack;
    }

    protected InternetResource getUIPack(InternetResource defaultResource,
            String key) {
        if (UIPack == null) {
            buildUIPack(defaultResource, key);
        }
        return UIPack;
    }

    protected synchronized void buildA4JPack(InternetResource defaultResource,
            String key) {
        if (A4JPack == null) {
            StringBuffer sb;
            try {
                sb = buildAggregatedScript();
            } catch (IOException e) {
                log.error("Error while processing aggregated scripts", e);
                sb = new StringBuffer("Error in processing : " + e.getMessage());
            }
            A4JPack = new AggregatedResources(sb, defaultResource.getKey());
            ResourceRenderer renderer = defaultResource.getRenderer(null);
            if (renderer == null) {
                renderer = new CompressedScriptRenderer();
            }
            A4JPack.setRenderer(renderer);
        }
    }

    protected synchronized void buildUIPack(InternetResource defaultResource,
            String key) {
        if (UIPack == null) {
            UIPack = new AggregatedResources(defaultResource.getKey());
            ResourceRenderer renderer = defaultResource.getRenderer(null);
            if (renderer == null) {
                renderer = new CompressedScriptRenderer();
            }
            UIPack.setRenderer(renderer);
        }
    }

    protected List<String> getBlackListedScripts() {
        synchronized (blackListedScripts) {
            if (blackListedScripts.isEmpty()) {
                blackListedScripts.add("framework.pack.js");
                blackListedScripts.add("ui.pack.js");
                blackListedScripts.add("jquery.js");
                // blackListedScripts.add("org/richfaces/renderkit/html/scripts/scrollable-data-table.js");
                // blackListedScripts.add("org/ajax4jsf/javascript/scripts/prototype.js");
            }
        }
        return blackListedScripts;
    }

    protected StringBuffer buildAggregatedScript() throws IOException {
        StringBuffer buf = new StringBuffer();
        for (String key : getJSResourcesToInclude()) {
            InternetResource res = super.getResource(key);
            InputStream is = res.getResourceAsStream(null);
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
            } else {
                log.error("Unable to read InputStream for resource " + key);
            }
        }
        return buf;
    }

}
