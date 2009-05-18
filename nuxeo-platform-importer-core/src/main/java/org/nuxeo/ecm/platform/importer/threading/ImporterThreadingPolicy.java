package org.nuxeo.ecm.platform.importer.threading;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public interface ImporterThreadingPolicy {


    boolean needToCreateThreadAfterNewFolderishNode(DocumentModel parent, SourceNode node, long uploadedSources , int batchSize, int scheduledTasks);

}
