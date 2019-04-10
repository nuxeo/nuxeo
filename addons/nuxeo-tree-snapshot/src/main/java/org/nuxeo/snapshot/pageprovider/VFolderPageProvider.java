package org.nuxeo.snapshot.pageprovider;

import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.snapshot.Snapshot;

public class VFolderPageProvider extends AbstractPageProvider<DocumentModel> implements PageProvider<DocumentModel> {

    private static final long serialVersionUID = 1L;

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    @Override
    public List<DocumentModel> getCurrentPage() {

        DocumentModel target = (DocumentModel) getParameters()[0];

        Snapshot snap = target.getAdapter(Snapshot.class);

        try {
            return snap.getChildren();
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}
