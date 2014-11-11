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

    private Date lastSaved;

    private String name;

    private URL url;

    @XNode("src")
    String src = "";


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

    public boolean isWritable() {
        if (getUrl() == null) {
            // theme with missing definitions are not writable
            return false;
        }
        String protocol = getUrl().getProtocol();
        return protocol.equals("ftp") || protocol.equals("file");
    }

    public String getSrc() {
        return src;
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
