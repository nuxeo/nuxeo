package org.nuxeo.apidoc.documentation;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

public interface DocumentationService {

     DocumentationItem createDocumentationItem(CoreSession session, NuxeoArtifact item,String title, String content, String type, List<String> applicableVersions, boolean approved, String renderingType) throws ClientException;

     DocumentationItem updateDocumentationItem(CoreSession session, DocumentationItem docItem) throws ClientException;

     List<DocumentationItem> findDocumentItems(CoreSession session,NuxeoArtifact nxItem) throws ClientException;

     List<DocumentationItem> findDocumentationItemVariants(CoreSession session, DocumentationItem item) throws ClientException;

     Map<String, String> getCategories()  throws Exception;

     List<String> getCategoryKeys()  throws Exception;

     void exportDocumentation(CoreSession session, OutputStream out);

     void importDocumentation(CoreSession session,InputStream is);

     String getDocumentationStats(CoreSession session);

}
