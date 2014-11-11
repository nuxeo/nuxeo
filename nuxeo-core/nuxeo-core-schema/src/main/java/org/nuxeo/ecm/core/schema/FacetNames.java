/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
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
     * If your Nuxeo target version is 5.3.2 or higher, you'll be able to
     * manage the order of this document children
     */
    public static final String ORDERABLE = "Orderable";

    /**
     * The download link will be displayed in consistent places of the
     * application
     */
    public static final String DOWNLOADABLE = "Downloadable";

    /**
     * The document type will be available in the seam context as variable
     * "currentSuperSpace" when navigating in its children documents
     */
    public static final String SUPER_SPACE = "SuperSpace";

    /**
     * The publishing tab will be displayed on the document (unless you forbid
     * the display of the publishing tab in the tabs filtering section)
     */
    public static final String PUBLISHABLE = "Publishable";

    /**
     * The document will be flagged as able to receive publications (and will
     * be displayed in the publication tree).
     */
    public static final String PUBLISH_SPACE = "PublishSpace";

    /**
     * The document will be flagged as a container for documents able to
     * receive publications (and will be displayed as a root in the available
     * publication trees)
     */
    public static final String MASTER_PUBLISH_SPACE = "MasterPublishSpace";

    /**
     * It will display the comment tab and the comments associated to the
     * document instance in the summary tab
     */
    public static final String COMMENTABLE = "Commentable";

    /**
     * The document type won't appear in the tree and in the folder content
     * listing
     */
    public static final String HIDDEN_IN_NAVIGATION = "HiddenInNavigation";

    /**
     * The document type corresponds to a system document, not a user-visible
     * document. It is often (but not always) hidden in navigation as well.
     */
    public static final String SYSTEM_DOCUMENT = "SystemDocument";

    /**
     * The document won't be full-text indexed.
     *
     * @since 5.7
     */
    public static final String NOT_FULLTEXT_INDEXABLE = "NotFulltextIndexable";

    @Deprecated
    public static final String BROWSE_VIA_SEARCH = "BrowseViaSearch";

    /**
     * Facet to be used for full-text indexing of related text content (e.g.
     * comments, annotations, tags...)
     */
    public static final String HAS_RELATED_TEXT = "HasRelatedText";

    private FacetNames() {
    }

}
