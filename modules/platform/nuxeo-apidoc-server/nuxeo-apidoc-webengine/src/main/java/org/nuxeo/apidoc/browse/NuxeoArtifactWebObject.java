/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.browse;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

public abstract class NuxeoArtifactWebObject extends DefaultObject {

    protected String nxArtifactId;

    @Override
    protected void initialize(Object... args) {
        nxArtifactId = (String) args[0];
    }

    protected String getNxArtifactId() {
        return nxArtifactId;
    }

    protected SnapshotManager getSnapshotManager() {
        return Framework.getService(SnapshotManager.class);
    }

    @Override
    public Template getView(String viewId) {
        return super.getView(viewId).arg(Distribution.DIST_ID, getDistributionId()).arg("onArtifact", true);
    }

    public abstract NuxeoArtifact getNxArtifact();

    protected String getDistributionId() {
        return (String) ctx.getProperty(Distribution.DIST_ID);
    }

    @Deprecated
    protected String computeUrl(String suffix) throws UnsupportedEncodingException {
        String targetUrl = ctx.getUrlPath();
        targetUrl = URLDecoder.decode(targetUrl, "ISO-8859-1");
        targetUrl = targetUrl.replace(ctx.getBasePath(), "");
        targetUrl = targetUrl.replace(suffix, "/doc");
        targetUrl = URLEncoder.encode(targetUrl, "ISO-8859-1");
        return targetUrl;
    }

    /**
     * Default getter on artifact.
     * <p>
     * Can be overridden for customization.
     *
     * @since 11.1
     */
    @GET
    @Produces("text/html")
    public Object doGet() {
        return doViewDefault();
    }

    @Produces("text/html")
    public Object doViewDefault() {
        NuxeoArtifact nxItem = getNxArtifact();
        return getView("default").arg("nxItem", nxItem);
    }

    public String getSearchCriterion() {
        return String.format("'%s'", getNxArtifactId());
    }
}
