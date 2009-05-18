package org.nuxeo.ecm.platform.importer.threading;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class DefaultMultiThreadingPolicy implements ImporterThreadingPolicy {

    public boolean needToCreateThreadAfterNewFolderishNode(DocumentModel parent,
            SourceNode node, long uploadedSources, int batchSize, int scheduledTasks) {

          if (uploadedSources < (batchSize / 3)) {
              return false;
          }

          if (scheduledTasks >= 5) {
              return false;
          } else {
              return true;
          }
    }

}
