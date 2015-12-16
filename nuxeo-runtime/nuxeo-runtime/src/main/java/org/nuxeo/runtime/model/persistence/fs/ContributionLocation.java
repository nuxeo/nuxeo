/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.model.persistence.fs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.nuxeo.runtime.model.persistence.AbstractContribution;

public class ContributionLocation extends AbstractContribution {

    protected final URL location;

    public ContributionLocation(String name, URL location) {
        super(name);
        this.location = location;
    }

    @Override
    public InputStream getStream() {
        try {
            return location.openStream();
        } catch (IOException e) {
            throw new RuntimeException("Cannot get '".concat(name).concat("' content"), e);
        }
    }

    @Override
    public String getContent() {
        try {
            return IOUtils.toString(location.openStream(), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot get '".concat(name).concat("' content"), e);
        }
    }

    @Override
    public URL asURL() {
        return location;
    }

}
