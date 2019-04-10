package org.nuxeo.snapshot.operation;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.snapshot.Snapshotable;

import static org.nuxeo.ecm.automation.core.Constants.CAT_DOCUMENT;
import static org.nuxeo.ecm.core.api.VersioningOption.MINOR;

@Operation(id = CreateSnapshot.ID, category = CAT_DOCUMENT, label = "Create snapshot", description = "Create a tree snapshot, input document must be eligible to Snapshotable adapter and output will the snapshot")
public class CreateSnapshot {
    public static final String ID = "Document.CreateSnapshot";

    @Param(name = "versioning option", required = false)
    String versioningOption = MINOR.name();

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws ClientException {
        Snapshotable adapter = doc.getAdapter(Snapshotable.class);
        if (adapter == null) {
            throw new ClientException(
                    "Unable to get Snapshotable adapter with document: "
                            + doc.getPathAsString());
        }
        return adapter.createSnapshot(
                VersioningOption.valueOf(versioningOption)).getDocument();
    }
}
