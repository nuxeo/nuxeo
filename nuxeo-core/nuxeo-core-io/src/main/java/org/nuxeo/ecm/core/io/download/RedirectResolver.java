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
 *     Remi Cattiau
 */
package org.nuxeo.ecm.core.io.download;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;

/**
 *
 * @since 8.3
 * Return a URI to redirect client to for a specified blob if possible
 */
public interface RedirectResolver {


    /**
     * Return a redirect URI if possible to the specified Blob
     * @param blob to get the URI for
     * @param usage for
     * @param request Native Http request
     * @return The URI to the resource or null if cannot create a direct URI
     */
    URI getURI(Blob blob, UsageHint usage, HttpServletRequest request) throws IOException;

}
