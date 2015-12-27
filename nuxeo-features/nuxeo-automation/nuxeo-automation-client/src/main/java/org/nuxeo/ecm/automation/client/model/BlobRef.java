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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.automation.client.Session;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BlobRef extends Blob {

    private static final long serialVersionUID = 1L;

    protected String ref;

    protected volatile Blob blob;

    protected transient Session session;

    public BlobRef(String ref) {
        this.ref = ref;
    }

    public void attach(Session session) {
        this.session = session;
    }

    public Session session() {
        return session;
    }

    public String getRef() {
        return ref;
    }

    @Override
    public InputStream getStream() throws IOException {
        if (blob == null) {
            synchronized (this) {
                if (blob == null) {
                    blob = session.getFile(ref);
                }
            }
        }
        return blob.getStream();
    }

}
