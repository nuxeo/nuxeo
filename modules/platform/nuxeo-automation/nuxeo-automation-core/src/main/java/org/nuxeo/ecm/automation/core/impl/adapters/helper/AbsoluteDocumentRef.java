/*
 * (C) Copyright 2023 Nuxeo SA (http://nuxeo.com/) and others.
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
 *  Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.automation.core.impl.adapters.helper;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * An absolute reference to a {@link DocumentModel}, composed of:
 *
 * <pre>
 * - a repository name
 * - a {@link DocumentRef}
 * </pre>
 *
 * @since 2023.5
 */
public class AbsoluteDocumentRef implements DocumentRef {

    private static final long serialVersionUID = 1L;

    protected final String repositoryName;

    protected final DocumentRef documentRef;

    public AbsoluteDocumentRef(String repositoryName, DocumentRef documentRef) {
        this.repositoryName = repositoryName;
        this.documentRef = documentRef;
    }

    @Override
    public int type() {
        return documentRef.type();
    }

    @Override
    public Object reference() {
        return documentRef.reference();
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
