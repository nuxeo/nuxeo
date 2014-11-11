package org.nuxeo.ecm.wiki.relation;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;

public interface WikiRelationConstants {

    public static final Resource HAS_LINK_TO = new ResourceImpl("http://www.nuxeo.org/wiki/hasLinkTo");

}
