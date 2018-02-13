/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.model.persistence.fs;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
            return IOUtils.toString(location.openStream(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot get '".concat(name).concat("' content"), e);
        }
    }

    @Override
    public URL asURL() {
        return location;
    }

}
