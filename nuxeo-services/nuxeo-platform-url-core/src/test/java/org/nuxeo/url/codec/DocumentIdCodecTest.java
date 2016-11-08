/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.url.codec;

import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

/**
 * Test codec for the {@link DocumentViewCodecManager}.
 *
 * @since 8.10
 */
public class DocumentIdCodecTest extends AbstractDocumentViewCodec {

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        return "test/codec/url";
    }

    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        return new DocumentViewImpl(new DocumentLocationImpl("default", new IdRef("12345")));
    }

}
