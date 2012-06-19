/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template.samples;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.CDATA;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.Entity;
import org.dom4j.Namespace;
import org.dom4j.ProcessingInstruction;
import org.dom4j.Text;
import org.dom4j.Visitor;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentTransformer;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.samples.io.XMLModelReader;

/**
 * Imports models and samples from resources or filesystem via CoreIO.
 * <p>
 * The association between template and document is translated during the IO
 * import (because UUIDs change).
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class ModelImporter {

    protected final static Log log = LogFactory.getLog(ModelImporter.class);

    private static final String TEMPLATE_SAMPLE_INIT_EVENT = "TemplateSampleInit";

    private static final String RESOURCES_ROOT = "templatesamples";

    public static final String EXAMPLES_ROOT = "examples";

    public static final String TEMPLATE_ROOT = "template";

    protected static final String DOMAIN_QUERY = "select * from Domain where ecm:isCheckedInVersion=0  AND  ecm:currentLifeCycleState != 'deleted' order by dc:created ASC";

    protected static final String SAMPLES_ROOT_PATH = "templateSamples";

    protected final CoreSession session;

    public ModelImporter(CoreSession session) {
        this.session = session;
    }

    protected DocumentModel getTargetDomain() throws ClientException {
        return getTargetDomain(true);
    }

    protected DocumentModel getTargetDomain(boolean canRetry)
            throws ClientException {
        DocumentModelList domains = session.query(DOMAIN_QUERY);
        if (domains.size() > 0) {
            return domains.get(0);
        }
        // no domain, that's strange
        // may be a session flush issue
        if (canRetry) {
            session.save();
            return getTargetDomain(false);
        }
        return null;
    }

    protected DocumentModel getOrCreateTemplateContainer()
            throws ClientException {
        DocumentModel rootDomain = getTargetDomain();

        if (rootDomain != null) {
            DocumentModelList roots = session.getChildren(rootDomain.getRef(),
                    "TemplateRoot");
            if (roots.size() > 0) {
                return roots.get(0);
            }
        }
        return null;
    }

    protected DocumentModel getOrCreateSampleContainer() throws ClientException {
        DocumentModel rootDomain = getTargetDomain();
        DocumentModel container = null;

        if (rootDomain != null) {
            DocumentModelList roots = session.getChildren(rootDomain.getRef(),
                    "WorkspaceRoot");
            if (roots.size() > 0) {
                DocumentModel WSRoot = roots.get(0);
                PathRef targetPath = new PathRef(WSRoot.getPathAsString() + "/"
                        + SAMPLES_ROOT_PATH);
                if (!session.exists(targetPath)) {
                    container = session.createDocumentModel(
                            WSRoot.getPathAsString(), SAMPLES_ROOT_PATH,
                            "Workspace");
                    container.setPropertyValue("dc:title",
                            "Template usage samples");
                    container.setPropertyValue("dc:description",
                            "This workspace contains some sample usage of the template rendition system");
                    container = session.createDocument(container);
                } else {
                    container = session.getDocument(targetPath);
                }
            }
        }
        return container;
    }

    protected boolean isImportAlreadyDone() {
        if (Framework.isTestModeSet()) {
            return false;
        }

        AuditReader reader = Framework.getLocalService(AuditReader.class);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("eventId", TEMPLATE_SAMPLE_INIT_EVENT);
        @SuppressWarnings("unchecked")
        List<Long> res = (List<Long>) reader.nativeQuery(
                "select count(log.id) from LogEntry log where log.eventId=:eventId",
                params, 1, 20);
        long resultsCount = res.get(0).longValue();
        if (resultsCount == 0) {
            return false;
        }
        return true;
    }

    protected void markImportDone() {
        if (Framework.isTestModeSet()) {
            return;
        }

        AuditLogger writer = Framework.getLocalService(AuditLogger.class);

        LogEntry entry = writer.newLogEntry();
        entry.setEventId(TEMPLATE_SAMPLE_INIT_EVENT);
        entry.setEventDate(Calendar.getInstance().getTime());

        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        writer.addLogEntries(entries);

    }

    protected static Path getDataDirPath() {
        String dataDir = null;

        if (Framework.isTestModeSet()) {
            dataDir = "/tmp";
        } else {
            dataDir = Framework.getProperty("nuxeo.data.dir");
        }
        Path path = new Path(dataDir);
        path = path.append("resources");
        return path;
    }

    public static void expandResources() throws Exception {
        String jarPath = ModelImporter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        File jar = new File(jarPath);
        Path path = getDataDirPath();
        File dataDir = new File(path.toString());
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        ZipUtils.unzip(jar, dataDir, new PathFilter() {
            @Override
            public boolean isExclusive() {
                return true;
            }

            @Override
            public boolean accept(Path path) {
                if (path.toString().contains("templatesamples")) {
                    return true;
                }
                return false;
            }
        });
    }

    public int importModels() throws Exception {

        if (isImportAlreadyDone()) {
            return 0;
        }

        int nbImportedDocs = 0;
        // in test mode we can directly access the resources as files
        File root = FileUtils.getResourceFileFromContext(RESOURCES_ROOT);
        if (root == null) {
            // in container mode, we rely on expandResources
            // Filesystem extraction
            Path path = getDataDirPath();
            path = path.append(RESOURCES_ROOT);
            root = new File(path.toString());
        }

        File[] modelRoots = root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (!pathname.isDirectory()) {
                    return false;
                }
                return true;
            }
        });

        for (File modelRoot : modelRoots) {
            log.info("Importing template from " + modelRoot.getAbsolutePath());
            nbImportedDocs += importModelAndExamples(modelRoot);
        }

        markImportDone();

        return nbImportedDocs;
    }

    public int importModelAndExamples(File root) throws Exception {

        int nbImportedDocs = 0;
        final Map<String, File> roots = new HashMap<String, File>();
        root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (!file.isDirectory()) {
                    return false;
                }
                if (file.getName().equals(TEMPLATE_ROOT)) {
                    roots.put(TEMPLATE_ROOT, file);
                    return true;
                } else if (file.getName().equals(EXAMPLES_ROOT)) {
                    roots.put(EXAMPLES_ROOT, file);
                    return true;
                }

                return false;
            }
        });

        if (roots.size() >= 1) {
            if (roots.get(TEMPLATE_ROOT) != null) {
                DocumentModel templatesContainer = getOrCreateTemplateContainer();
                DocumentModel samplesContainer = getOrCreateSampleContainer();
                if (templatesContainer != null) {
                    DocumentRef modelRef = importModel(root.getName(),
                            roots.get(TEMPLATE_ROOT), templatesContainer);
                    nbImportedDocs++;
                    if (samplesContainer != null) {
                        if (roots.get(EXAMPLES_ROOT) != null) {
                            nbImportedDocs = nbImportedDocs
                                    + importSamples(roots.get(EXAMPLES_ROOT),
                                            modelRef, samplesContainer);
                        }
                    }
                }
            }
        }

        return nbImportedDocs;

    }

    protected DocumentRef importModel(String modelName, File source,
            DocumentModel root) throws Exception {

        // import
        DocumentReader reader = new XMLModelReader(source, modelName);
        DocumentWriter writer = new DocumentModelWriter(session,
                root.getPathAsString());

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        DocumentTranslationMap map = pipe.run();

        DocumentRef ref = map.getDocRefMap().values().iterator().next();
        session.save();

        return ref;
    }

    protected int importSamples(File root, DocumentRef modelRef,
            DocumentModel rootDoc) throws Exception {

        int nbImportedDocs = 0;
        for (File exampleDir : root.listFiles()) {
            if (!exampleDir.isDirectory()) {
                continue;
            }

            // import
            DocumentReader reader = new XMLModelReader(exampleDir,
                    exampleDir.getName());
            DocumentWriter writer = new DocumentModelWriter(session,
                    rootDoc.getPathAsString());

            DocumentPipe pipe = new DocumentPipeImpl(10);

            final String targetUUID = modelRef.toString();

            pipe.addTransformer(new DocumentTransformer() {

                @Override
                public boolean transform(ExportedDocument xdoc)
                        throws IOException {
                    xdoc.getDocument().accept(new Visitor() {

                        @Override
                        public void visit(Text node) {
                        }

                        @Override
                        public void visit(ProcessingInstruction node) {
                        }

                        @Override
                        public void visit(Namespace namespace) {
                        }

                        @Override
                        public void visit(Entity node) {
                        }

                        @Override
                        public void visit(Comment node) {
                        }

                        @Override
                        public void visit(CDATA node) {
                        }

                        @Override
                        public void visit(Attribute node) {
                        }

                        @Override
                        public void visit(Element node) {
                            if ("templateId".equalsIgnoreCase(node.getName())
                                    && "templateEntry".equalsIgnoreCase(node.getParent().getName())) {
                                log.debug("Translating uuid to " + targetUUID);
                                node.setText(targetUUID);
                            }
                        }

                        @Override
                        public void visit(DocumentType documentType) {
                        }

                        @Override
                        public void visit(Document document) {
                        }
                    });
                    return true;
                }
            });
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
            nbImportedDocs++;

        }
        session.save();
        return nbImportedDocs;
    }
}
