/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.themes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("theme")
public class ThemeDescriptor implements Type {

    protected RuntimeContext ctx; 
    
    private boolean configured = false;

    private boolean customized = false;

    private Date lastSaved;

    private Date lastLoaded;

    private boolean loadingFailed = false;

    private String name;

    private URL url;

    private List<String> templateEngines;

    @XNode("src")
    public String src = "";

    
    public void setContext(RuntimeContext ctx) {
        this.ctx = ctx;
    }
    
    public RuntimeContext getContext() {
        return ctx;
    }
    
    public TypeFamily getTypeFamily() {
        return TypeFamily.THEME;
    }

    public String getTypeName() {
        return src;
    }

    private URL getUrl() {
        if (url != null) {
            return url;
        }
        try {
            url = new URL(src);
        } catch (MalformedURLException e) {
            return null;
        }
        return url;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isCustom() {
        return !isXmlConfigured();
    }

    public boolean isLoaded() {
        return lastLoaded != null;
    }

    public boolean isXmlConfigured() {
        return configured;
    }

    public boolean isWritable() {
        if (getUrl() == null) {
            // themes with missing definition are not writable
            return false;
        }
        final String protocol = getUrl().getProtocol();
        return protocol.equals("ftp") || protocol.equals("file");
    }

    public boolean isLoadable() {
        return !isLoaded();
    }

    public boolean isReloadable() {
        return isLoaded() && !isCustomized();
    }

    public boolean isSaveable() {
        return isWritable() && (isLoaded() || isCustom()) && !isCustomized();
    }

    public boolean isExportable() {
        return (isCustom() || isLoaded()) && !isCustomized();
    }

    public boolean isRepairable() {
        return (isCustom() || isLoaded()) && !isCustomized();
    }

    public boolean isLoadingFailed() {
        return loadingFailed;
    }

    public void setLoadingFailed(boolean loadingFailed) {
        this.loadingFailed = loadingFailed;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public boolean isCustomized() {
        return customized;
    }

    public void setCustomized(boolean customized) {
        this.customized = customized;
    }

    public Date getLastLoaded() {
        return lastLoaded;
    }

    public void setLastLoaded(Date lastLoaded) {
        this.lastLoaded = lastLoaded;
    }

    public Date getLastSaved() {
        return lastSaved;
    }

    public void setLastSaved(Date lastSaved) {
        this.lastSaved = lastSaved;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTemplateEngines() {
        return templateEngines;
    }

    public void setTemplateEngines(List<String> templateEngines) {
        this.templateEngines = templateEngines;
    }

    public boolean isCompatibleWith(final String templateEngine) {
        if (templateEngines == null || templateEngines.isEmpty()
                || templateEngines.contains(templateEngine)) {
            return true;
        }
        return false;
    }

}
