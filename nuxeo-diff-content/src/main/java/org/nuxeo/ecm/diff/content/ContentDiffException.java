/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Content diff exception.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public class ContentDiffException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public ContentDiffException(Throwable cause) {
        super(cause);
    }

    public ContentDiffException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentDiffException(String message) {
        super(message);
    }
}
