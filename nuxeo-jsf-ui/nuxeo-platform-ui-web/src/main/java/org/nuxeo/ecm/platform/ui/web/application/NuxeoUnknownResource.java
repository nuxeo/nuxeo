/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.faces.application.ViewResource;

import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.runtime.api.Framework;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Resource representing a facelet that is not found in this application.
 * <p>
 * This is used to avoid crashing triggering an exception when a facelet resource is missing.
 * <p>
 * Instead, a message referencing the missing resource is displayed in red and in bold where the facelet would have been
 * included.
 *
 * @since 6.0
 */
public class NuxeoUnknownResource extends ViewResource {

    public static final String MARKER = NuxeoUnknownResource.class.getName();

    public static final String PLACEHOLDER = "/facelet_not_found.xhtml";

    private static final Log log = LogFactory.getLog(NuxeoUnknownResource.class);

    protected final String path;

    public NuxeoUnknownResource(String path) {
        super();
        this.path = path;
    }

    @Override
    public URL getURL() {
        try {
            String urlPath = MARKER + path;
            return new URL("", "", -1, urlPath, new NuxeoNotFoundResourceHandler());
        } catch (MalformedURLException e) {
            return null;
        }
    }

    class NuxeoNotFoundResourceHandler extends URLStreamHandler {

        public NuxeoNotFoundResourceHandler() {
            super();
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            log.error("facelet not found: " + path);
            return new Connection(url);
        }

        class Connection extends URLConnection {

            public Connection(URL url) {
                super(url);
            }

            @Override
            public void connect() throws IOException {
            }

            @Override
            public InputStream getInputStream() throws IOException {
                String message = "ERROR: facelet not found";
                // NXP-25746
                if (Framework.isDevModeSet() && !path.contains("$") && !path.contains("#")) {
                    message += " at '" + path + "'";
                }
                String msg = "<span><span style=\"color:red;font-weight:bold;\">"
                        + StringEscapeUtils.escapeHtml4(message) + "</span><br/></span>";
                return new ByteArrayInputStream(msg.getBytes());
            }
        }
    }
}
