/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.model.persistence.fs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
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
            throw new Error("Cannot get '".concat(name).concat("' content"), e);
        }
    }

    @Override
    public String getContent() {
        try {
            return FileUtils.read(location.openStream());
        } catch (IOException e) {
            throw new Error("Cannot get '".concat(name).concat("' content"), e);
        }
    }

    @Override
    public URL asURL() {
        return location;
    }

}
