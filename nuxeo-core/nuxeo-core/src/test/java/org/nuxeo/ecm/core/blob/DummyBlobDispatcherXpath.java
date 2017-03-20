/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Michael Vachette
 */
package org.nuxeo.ecm.core.blob;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.model.Document;

/**
 * Dummy blob dispatcher that stores files/0/file in a second blob provider.
 */
public class DummyBlobDispatcherXpath extends DummyBlobDispatcher {

    protected static final String XPATH = "files/0/file";

    @Override
    public BlobDispatch getBlobProvider(Document doc, Blob blob, String xpath) {
        String provider = XPATH.equals(xpath) ? secondProvider : defaultProvider;
        return new BlobDispatch(provider, true);
    }

}
