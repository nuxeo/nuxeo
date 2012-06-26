package org.nuxeo.ecm.quota.size;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.quota.count.DocumentsCountUpdater;

public class DocumentsCountAndSizeUpdater extends DocumentsCountUpdater {

    public static final String DOCUMENTS_SIZE_STATISTICS_FACET = "DocumentsSizeStatistics";

    public static final String DOCUMENTS_SIZE_INNER_SIZE_PROPERTY = "dss:innerSize";

    public static final String DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY = "dss:totalSize";

    @Override
    protected void processDocumentCreated(CoreSession session, DocumentModel doc)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);

        BlobSizeInfo bsi = getBlobsSizeChanges(doc);
        if (bsi.changed()) {
            updateSizeOnNodeAndAncestors(session, doc, ancestors, bsi);
        }
    }

    @Override
    protected void processDocumentCopied(CoreSession session, DocumentModel doc)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
        BlobSizeInfo bsi = getBlobsSizeChanges(doc);
        if (bsi.changed()) {
            updateSizeOnNodeAndAncestors(session, doc, ancestors, bsi);
        }
    }

    @Override
    protected void processDocumentUpdated(CoreSession session, DocumentModel doc)
            throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        BlobSizeInfo bsi = getBlobsSizeChanges(doc);
        if (bsi.changed()) {
            updateSizeOnNodeAndAncestors(session, doc, ancestors, bsi);
        }
    }

    @Override
    protected void processDocumentMoved(CoreSession session, DocumentModel doc,
            DocumentModel sourceParent) throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        List<DocumentModel> sourceAncestors = getAncestors(session,
                sourceParent);
        sourceAncestors.add(0, sourceParent);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, docCount);
        updateCountStatistics(session, doc, sourceAncestors, -docCount);
        BlobSizeInfo bsi = getBlobsSizeChanges(doc);
        if (bsi.changed()) {
            updateSizeOnNodeAndAncestors(session, doc, ancestors, bsi);
            updateSizeOnNodeAndAncestors(session, doc, sourceAncestors,
                    bsi.invert(), true);
        }
    }

    @Override
    protected void processDocumentAboutToBeRemoved(CoreSession session,
            DocumentModel doc) throws ClientException {
        List<DocumentModel> ancestors = getAncestors(session, doc);
        long docCount = getCount(doc);
        updateCountStatistics(session, doc, ancestors, -docCount);
        BlobSizeInfo bsi = getBlobsSizeChanges(doc);
        updateSizeOnNodeAndAncestors(session, doc, ancestors,
                bsi.removeValue(), true);
    }

    protected void updateSizeOnNodeAndAncestors(CoreSession session,
            DocumentModel doc, List<DocumentModel> ancestors, BlobSizeInfo bsi)
            throws ClientException {
        updateSizeOnNodeAndAncestors(session, doc, ancestors, bsi, false);
    }

    protected void updateSizeOnNodeAndAncestors(CoreSession session,
            DocumentModel doc, List<DocumentModel> ancestors, BlobSizeInfo bsi,
            boolean onlyAncestors) throws ClientException {

        if (!bsi.changed()) {
            return;
        }
        if (!onlyAncestors) {
            if (!doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
                doc.addFacet(DOCUMENTS_SIZE_STATISTICS_FACET);
            }
            doc.setPropertyValue(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY,
                    bsi.blobSize);
            Long total = (Long) doc.getPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY);
            total += bsi.blobSizeDelta;
            doc.setPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY, total);
            session.saveDocument(doc);
        }

        for (DocumentModel ancestor : ancestors) {
            if (!ancestor.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
                ancestor.addFacet(DOCUMENTS_SIZE_STATISTICS_FACET);
                System.out.println("Add facet on " + ancestor.getPathAsString());
            }
            Long total = (Long) ancestor.getPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY);
            total += bsi.blobSizeDelta;
            ancestor.setPropertyValue(DOCUMENTS_SIZE_TOTAL_SIZE_PROPERTY, total);
            session.saveDocument(ancestor);
        }
        session.save();
    }

    protected BlobSizeInfo getBlobsSizeChanges(DocumentModel doc)
            throws ClientException {
        BlobSizeInfo result = new BlobSizeInfo();

        if (doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
            result.blobSize = (Long) doc.getPropertyValue(DOCUMENTS_SIZE_INNER_SIZE_PROPERTY);
        } else {
            result.blobSize = 0;
        }

        List<Blob> blobs = getBlobs(doc, false);
        if (blobs.size() == 0) {
            result.blobSizeDelta = 0;
        } else {
            long size = 0;
            for (Blob blob : blobs) {
                if (blob != null) {
                    size += blob.getLength();
                }
            }
            result.blobSizeDelta = size - result.blobSize;
            result.blobSize = size;
        }
        return result;
    }

    protected List<Blob> getBlobs(DocumentModel doc, boolean onlyChangedBlob)
            throws ClientException {

        try {
            BlobsExtractor extractor = new BlobsExtractor();
            List<Property> blobProperties = extractor.getBlobsProperties(doc);

            boolean needRecompute = !onlyChangedBlob;
            if (!needRecompute) {
                if (blobProperties.size() > 0) {
                    for (Property blobProperty : blobProperties) {
                        if (blobProperty.isDirty()) {
                            needRecompute = true;
                            break;
                        }
                    }
                }
            }
            List<Blob> result = new ArrayList<Blob>();
            if (needRecompute) {
                for (Property blobProperty : blobProperties) {
                    Blob blob = (Blob) blobProperty.getValue();
                    result.add(blob);
                }
            }
            return result;
        } catch (Exception e) {
            throw new ClientException("Unable to extract Blob size", e);
        }
    }

}
