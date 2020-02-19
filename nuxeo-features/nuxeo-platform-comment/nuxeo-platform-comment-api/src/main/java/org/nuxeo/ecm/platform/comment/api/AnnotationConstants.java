/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.comment.api;

/**
 * @since 10.1
 */
public class AnnotationConstants {

    private AnnotationConstants() {
        // utility class
    }

    public static final String ANNOTATION_DOC_TYPE = "Annotation";

    /** @since 11.1 */
    public static final String ANNOTATION_SCHEMA = "annotation";

    public static final String ANNOTATION_XPATH_PROPERTY = "annotation:xpath";

    public static final String ANNOTATION_XPATH = "xpath";

    /** @since 10.3 */
    public static final String ANNOTATION_PERMISSIONS = "permissions";

}
