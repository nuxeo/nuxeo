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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;

import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;

/**
 * @since 10.1
 */
public class AnnotationImpl extends CommentImpl implements Annotation, ExternalEntity {

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String xpath;

    /**
     * @since 11.1
     */
    public AnnotationImpl() {
        this(SimpleDocumentModel.ofType(ANNOTATION_DOC_TYPE));
    }

    /**
     * Constructor for the document adapter factory.
     *
     * @since 11.1
     */
    protected AnnotationImpl(DocumentModel docModel) {
        super(docModel);
    }

    @Override
    public String getXpath() {
        return (String) docModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY);
    }

    @Override
    public void setXpath(String xpath) {
        docModel.setPropertyValue(ANNOTATION_XPATH_PROPERTY, xpath);
    }

}
