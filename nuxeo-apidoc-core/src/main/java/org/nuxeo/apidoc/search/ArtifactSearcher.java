package org.nuxeo.apidoc.search;

import java.util.List;

import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.CoreSession;

public interface ArtifactSearcher {

    List<NuxeoArtifact> searchArtifact(CoreSession session, String fulltext) throws Exception;

    List<DocumentationItem> searchDocumentation(CoreSession session,String fulltext, String targetType) throws Exception;

    List<NuxeoArtifact> filterArtifact(CoreSession session, String distribId, String type, String fulltext) throws Exception;

}
