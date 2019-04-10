package org.nuxeo.snapshot.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.snapshot.Snapshotable;

/**
 * Listener snapshoting documents with the {@link Snapshotable#FACET} facet if
 * the property {@code snapshotVersioningOption} is set.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class SnapshotableListener implements EventListener {

    private static final Log log = LogFactory.getLog(SnapshotableListener.class);

    public static final String SNAPSHOT_VERSIONING_OPTION_KEY = "snapshotVersioningOption";

    @Override
    public void handleEvent(Event event) throws ClientException {
        String eventName = event.getName();
        if (!DOCUMENT_UPDATED.equals(eventName)) {
            return;
        }

         if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        if (doc.isProxy() || doc.isVersion()
                || !doc.hasFacet(Snapshotable.FACET)) {
            return;
        }

        String versioningOption = (String) ctx.getProperty(SNAPSHOT_VERSIONING_OPTION_KEY);
        if (versioningOption == null) {
            return;
        }

        VersioningOption option;
        try {
            option = VersioningOption.valueOf(versioningOption);
        } catch (Exception e) {
            log.error(String.format("Unknown versioning option value '%s': %s",
                    versioningOption, e.getMessage()));
            log.debug(e, e);
            return;
        }
        if (option == VersioningOption.NONE) {
            return;
        }

        try {
            Snapshotable snapshotable = doc.getAdapter(Snapshotable.class);
            snapshotable.createSnapshot(option);
        } catch (ClientException e) {
            event.markRollBack(e.getMessage(), e);
            throw e;
        }

    }
}
