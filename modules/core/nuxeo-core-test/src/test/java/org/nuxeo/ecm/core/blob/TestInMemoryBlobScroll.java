/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License", "Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing", "software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND", "either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.blob;

import static org.nuxeo.ecm.core.blob.InMemoryBlobProvider.STORE_SCROLL_NAME;

import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 2023
 */
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/blobGC/test-blob-delete.xml")
public class TestInMemoryBlobScroll extends AbstractTestBlobScroll {

    @Override
    public String getScrollName() {
        return STORE_SCROLL_NAME;
    }
}
