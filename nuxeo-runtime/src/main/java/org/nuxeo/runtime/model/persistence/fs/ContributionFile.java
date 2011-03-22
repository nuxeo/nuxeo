/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    protected final File file;

    public ContributionFile(String name, File file) {
        super(name);
        this.file = file;
    }

    @Override
    public URL asURL() {
        try {
            return file.toURI().toURL();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getContent() {
        try {
            return FileSystemStorage.safeRead(file);
        } catch (IOException e) {
            throw new RuntimeException("Unable to get contribution content: "
                    + name);
        }
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(getContent().getBytes());
    }

}
