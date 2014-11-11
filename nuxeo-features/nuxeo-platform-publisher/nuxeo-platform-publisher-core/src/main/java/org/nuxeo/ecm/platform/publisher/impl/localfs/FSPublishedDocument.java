package org.nuxeo.ecm.platform.publisher.impl.localfs;

import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.CoreIODocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.helper.VersioningHelper;

import java.io.File;
import java.io.StringReader;

public class FSPublishedDocument implements PublishedDocument {

    private static final long serialVersionUID = 1L;

    public static final Namespace nxfspub = new Namespace("nxfspub",
            "http://www.nuxeo.org/publisher/filesystem");

    public static final QName pubInfoQN = DocumentFactory.getInstance().createQName(
            "publicationInfo", nxfspub);

    public static final QName sourceDocRefQN = DocumentFactory.getInstance().createQName(
            "sourceDocumentRef", nxfspub);

    public static final QName sourceRepositoryNameQN = DocumentFactory.getInstance().createQName(
            "sourceRepositoryName", nxfspub);

    public static final QName sourceServerQN = DocumentFactory.getInstance().createQName(
            "sourceServer", nxfspub);

    public static final QName sourceVersionQN = DocumentFactory.getInstance().createQName(
            "sourceVersion", nxfspub);

    public static final QName isPendingQN = DocumentFactory.getInstance().createQName(
            "isPending", nxfspub);

    protected DocumentRef sourceDocumentRef;

    protected String sourceRepositoryName;

    protected String sourceServer;

    protected String sourceVersion;

    protected String persistPath;

    protected String parentPath;

    protected boolean isPending;

    protected String xmlRepresentation;

    public FSPublishedDocument(File file)
            throws NotFSPublishedDocumentException {
        parseXML(file);
        persistPath = file.getAbsolutePath();
        parentPath = file.getParent();
    }

    protected void parseXML(File file) throws NotFSPublishedDocumentException {
        SAXReader xmlReader = new SAXReader();
        try {
            Document doc = xmlReader.read(file);
            Element info = doc.getRootElement().element(pubInfoQN);
            if (info == null) {
                // valid xml file, but not a published document
                throw new NotFSPublishedDocumentException();
            }
            sourceDocumentRef = new IdRef(
                    info.element(sourceDocRefQN).getTextTrim());
            sourceRepositoryName = info.element(sourceRepositoryNameQN).getTextTrim();
            sourceServer = info.element(sourceServerQN).getTextTrim();
            sourceVersion = info.element(sourceVersionQN).getTextTrim();
            isPending = Boolean.parseBoolean(info.element(isPendingQN).getTextTrim());
        } catch (DocumentException e) {
            throw new NotFSPublishedDocumentException(e);
        }
    }

    public void persist(String containerPath) throws Exception {
        String filePath = new Path(containerPath).append(
                sourceDocumentRef.toString()).toString();
        File output = new File(filePath);
        FileUtils.writeFile(output, xmlRepresentation);
        persistPath = filePath;
    }

    public FSPublishedDocument(String server, DocumentModel doc)
            throws Exception {
        this(server, doc, false);
    }

    public FSPublishedDocument(String server, DocumentModel doc,
            boolean isPending) throws Exception {

        sourceRepositoryName = doc.getRepositoryName();
        sourceDocumentRef = doc.getRef();
        sourceVersion = VersioningHelper.getVersionLabelFor(doc);
        sourceServer = server;
        this.isPending = isPending;

        CoreIODocumentModelMarshaler marshaler = new CoreIODocumentModelMarshaler();
        String xmlDoc = marshaler.marshalDocument(doc);

        SAXReader xmlReader = new SAXReader();
        Document xml = xmlReader.read(new StringReader(xmlDoc));

        xml.getRootElement().add(nxfspub);
        Element info = xml.getRootElement().addElement(pubInfoQN);

        info.addElement(sourceDocRefQN).setText(sourceDocumentRef.toString());
        info.addElement(sourceRepositoryNameQN).setText(sourceRepositoryName);
        if (sourceServer != null) {
            info.addElement(sourceServerQN).setText(sourceServer);
        } else {
            info.addElement(sourceServerQN);
        }
        if (sourceVersion != null) {
            info.addElement(sourceVersionQN).setText(sourceVersion);
        } else {
            info.addElement(sourceVersionQN);
        }
        info.addElement(isPendingQN).setText(String.valueOf(isPending));
        xmlRepresentation = xml.asXML();
    }

    public DocumentRef getSourceDocumentRef() {
        return sourceDocumentRef;
    }

    public String getSourceRepositoryName() {
        return sourceRepositoryName;
    }

    public String getSourceServer() {
        return sourceServer;
    }

    public String getSourceVersionLabel() {
        return sourceVersion;
    }

    public String getPersistPath() {
        return persistPath;
    }

    public String getPath() {
        return getPersistPath();
    }

    public String getParentPath() {
        return parentPath;
    }

    public boolean isPending() {
        return isPending;
    }

    public Type getType() {
        return Type.FILE_SYSTEM;
    }

}
