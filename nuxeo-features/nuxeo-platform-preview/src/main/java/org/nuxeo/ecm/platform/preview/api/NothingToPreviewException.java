/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.preview.api;

/**
 * Preview exception. Use this when there is nothing to preview and this is not an error. ie when there is no Blob to
 * preview (not when it cannot be found)
 */
public class NothingToPreviewException extends PreviewException {

    private static final long serialVersionUID = 1L;

    public NothingToPreviewException(Throwable cause) {
        super(cause);
    }

    public NothingToPreviewException(String message, Throwable cause) {
        super(message, cause);
    }

    public NothingToPreviewException(String message) {
        super(message);
    }

}
