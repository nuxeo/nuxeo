/*
 * (C) Copyright 2019 Qastia (http://www.qastia.com/) and others.
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
 *     Benjamin JALON
 *
 */

package org.nuxeo.template.processors.fm;

import org.nuxeo.ecm.core.api.Blob;

public class FMBindingResolverForTest extends FMBindingResolver {

    @Override
    protected Object handleLoop(String paramName, Object value) {
        return super.handleLoop(paramName, value);
    }

    @Override
    protected Object handlePictureField(String paramName, Blob blobValue) {
        return super.handlePictureField(paramName, blobValue);
    }

    @Override
    protected void handleBlobField(String paramName, Blob blobValue) {
        super.handleBlobField(paramName, blobValue);
    }

    @Override
    protected String handleHtmlField(String paramName, String htmlValue) {
        return super.handleHtmlField(paramName, htmlValue);
    }
}
