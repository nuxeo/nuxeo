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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("theme")
public class ThemeDescriptor implements Type {

    private Date lastLoaded;

    private boolean configured = false;

    private Date lastSaved;

    private String name;

    private URL url;

    @XNode("src")
    public String src = "";

    public TypeFamily getTypeFamily() {
        return TypeFamily.THEME;
    }

    public String getTypeName() {
        return src;
    }

    public URL getUrl() {
        if (url != null) {
            return url;
        }
        try {
            url = new URL(src);
        } catch (MalformedURLException e) {
            url = Thread.currentThread().getContextClassLoader().getResource(
                    src);
        }
        return url;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    private boolean isXmlConfigured() {
        return configured;
    }

    private boolean isCustom() {
        return !isXmlConfigured();
    }

    private boolean isLoaded() {
        return lastLoaded != null;
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
        return isLoaded();
    }

    public boolean isSaveable() {
        return isWritable() && (isLoaded() || isCustom());
    }

    public boolean isExportable() {
        return isCustom() || isLoaded();
    }

    public boolean isLoadingFailed() {
        return isXmlConfigured() && !isLoaded();
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
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

}
