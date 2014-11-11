/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.runtime.model.persistence.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.runtime.model.persistence.AbstractContribution;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContributionFile extends AbstractContribution {

    protected String name;

    protected String description;

    protected boolean disabled;

    protected boolean loaded;

    protected File file;

    public ContributionFile(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    protected void load() {
        if (!loaded) {
            FileSystemStorage.loadMetadata(this);
            loaded = true;
        }
    }

    public String getDescription() {
        load();
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDisabled() {
        load();
        return disabled;
    }

    public void setDisabled(boolean isDisabled) {
        this.disabled = isDisabled;
    }

    public URL asURL() {
        try {
            return file.toURI().toURL();
        } catch (Exception e) {
            return null;
        }
    }

    public String getContent() {
        try {
            return FileSystemStorage.safeRead(file);
        } catch (IOException e) {
            throw new RuntimeException("Unable to get contribution content: "
                    + name);
        }
    }

    public InputStream getStream() {
        return new ByteArrayInputStream(getContent().getBytes());
    }
}
