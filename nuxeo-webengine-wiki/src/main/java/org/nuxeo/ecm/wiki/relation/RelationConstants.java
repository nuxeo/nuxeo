package org.nuxeo.ecm.wiki.relation;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

public class RelationConstants {

    public static final String GRAPH_NAME = "wiki";

    public static final Resource HAS_LINK_TO = new ResourceImpl("http://www.nuxeo.org/wiki/hasLinkTo");

    public static final String METADATA_NAMESPACE = "http://www.nuxeo.org/metadata/";

    public static final String DOCUMENT_NAMESPACE = "http://www.nuxeo.org/document/uid/";

    public static final Resource TITLE = new ResourceImpl(METADATA_NAMESPACE + "title");

    public static final Resource UUID = new ResourceImpl(METADATA_NAMESPACE + "uuid");

}
