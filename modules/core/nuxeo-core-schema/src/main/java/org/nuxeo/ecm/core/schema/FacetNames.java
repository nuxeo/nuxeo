/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.schema;

/**
 * Defines base facet names used in the core.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public final class FacetNames {

    public static final String IMMUTABLE = "Immutable";

    /**
     * Document may have children
     */
    public static final String FOLDERISH = "Folderish";

    /**
     * It will make it possible to create versions for this document type
     */
    public static final String VERSIONABLE = "Versionable";

    /**
     * If your Nuxeo target version is 5.3.2 or higher, you'll be able to manage the order of this document children
     */
    public static final String ORDERABLE = "Orderable";

    /**
     * The download link will be displayed in consistent places of the application
     */
    public static final String DOWNLOADABLE = "Downloadable";

    /**
     * The document type will be available in the seam context as variable "currentSuperSpace" when navigating in its
     * children documents
     */
    public static final String SUPER_SPACE = "SuperSpace";

    /**
     * The publishing tab will be displayed on the document (unless you forbid the display of the publishing tab in the
     * tabs filtering section)
     */
    public static final String PUBLISHABLE = "Publishable";

    /**
     * The document will be flagged as able to receive publications (and will be displayed in the publication tree).
     */
    public static final String PUBLISH_SPACE = "PublishSpace";

    /**
     * The document will be flagged as a container for documents able to receive publications (and will be displayed as
     * a root in the available publication trees)
     */
    public static final String MASTER_PUBLISH_SPACE = "MasterPublishSpace";

    /**
     * It will display the comment tab and the comments associated to the document instance in the summary tab
     */
    public static final String COMMENTABLE = "Commentable";

    /**
     * The document type won't appear in the tree and in the folder content listing
     */
    public static final String HIDDEN_IN_NAVIGATION = "HiddenInNavigation";

    /**
     * The document type corresponds to a system document, not a user-visible document. It is often (but not always)
     * hidden in navigation as well.
     */
    public static final String SYSTEM_DOCUMENT = "SystemDocument";

    /**
     * The document won't be full-text indexed.
     *
     * @since 5.7
     */
    public static final String NOT_FULLTEXT_INDEXABLE = "NotFulltextIndexable";

    /**
     * It will display the big folder document without its children
     *
     * @since 8.4
     */
    public static final String BIG_FOLDER = "BigFolder";

    /**
     * Facet to be used for full-text indexing of related text content (e.g. comments, annotations, tags...)
     */
    public static final String HAS_RELATED_TEXT = "HasRelatedText";

    private FacetNames() {
    }

}
