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
package org.nuxeo.runtime.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.runtime.model.StreamRef;

/**
 * InlineRef allows to create stream ref on the fly, using only a String.
 *
 * @since 5.8
 */
public class InlineRef implements StreamRef {

    protected final String id;

    protected final String content;

    public InlineRef(String id, String content) {
        this.id = id;
        this.content = content;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new ByteArrayInputStream(content.getBytes());
    }

    @Override
    public URL asURL() {
        return null;
    }

}
