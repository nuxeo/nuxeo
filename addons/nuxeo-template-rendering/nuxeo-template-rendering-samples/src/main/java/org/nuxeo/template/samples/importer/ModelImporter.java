/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template.samples.importer;

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
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
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

/**
 * Imports models and samples from resources or filesystem via CoreIO.
 * <p>
 * The association between template and document is translated during the IO import (because UUIDs change).
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class ModelImporter {

    protected final static Log log = LogFactory.getLog(ModelImporter.class);

    private static final String TEMPLATE_SAMPLE_INIT_EVENT = "TemplateSampleInit";

    private static final String[] IMPORT_ALREADY_DONE_EVENTS = { TEMPLATE_SAMPLE_INIT_EVENT };

    public static final String EXAMPLE_ROOT = "example";

    public static final String TEST_EXAMPLE_ROOT = "testexample";

    public static final String TEMPLATE_ROOT = "template";

    public static final String TEST_TEMPLATE_ROOT = "testtemplate";

    protected static final String RESOURCES_ROOT = "templatesamples";

    protected static final String RAW_RESOURCES_ROOT = "rawsamples";

    protected static final String DOMAIN_QUERY = "select * from Domain where ecm:isVersion = 0  AND  ecm:isTrashed = 0 order by dc:created ASC";

    protected final CoreSession session;

    public ModelImporter(CoreSession session) {
        this.session = session;
    }

    protected String getTemplateResourcesRootPath() {
        return RESOURCES_ROOT;
    }

    protected String getRawTemplateResourcesRootPath() {
        return RAW_RESOURCES_ROOT;
    }

    protected DocumentModel getTargetDomain() {
        return getTargetDomain(true);
    }

    protected DocumentModel getTargetDomain(boolean canRetry) {
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

    protected DocumentModel getOrCreateTemplateContainer() {
        DocumentModel rootDomain = getTargetDomain();

        if (rootDomain != null) {
            DocumentModelList roots = session.getChildren(rootDomain.getRef(), "TemplateRoot");
            if (roots.size() > 0) {
                return roots.get(0);
            }
        }
        return null;
    }

    protected DocumentModel getWSRoot(DocumentModel rootDomain) {
        if (rootDomain != null) {
            DocumentModelList roots = session.getChildren(rootDomain.getRef(), "WorkspaceRoot");
            if (roots.size() > 0) {
                DocumentModel WSRoot = roots.get(0);
                return WSRoot;
            }
        }
        return null;
    }

    protected DocumentModel getOrCreateSampleContainer() {
        DocumentModel rootDomain = getTargetDomain();
        DocumentModel container = null;

        DocumentModel WSRoot = getWSRoot(rootDomain);
        if (WSRoot != null) {
            PathRef targetPath = new PathRef(WSRoot.getPathAsString() + "/" + getTemplateResourcesRootPath());
            if (!session.exists(targetPath)) {
                container = session.createDocumentModel(WSRoot.getPathAsString(), getTemplateResourcesRootPath(),
                        "nxtrSamplesContainer");
                container.setPropertyValue("dc:title", "Discover Customization Examples");
                container.setPropertyValue("nxtplsamplescontainer:instructions",
                        "<span class=\"nxtrExplanations\">The BigCorp company uses Nuxeo Studio and template rendering to generate custom project portfolios that showcase relevant expertise to potential new clients.<br /><br /><strong>It's your turn now! Open the \"BigCorp Transforms GreatBank Customer Service\" project</strong> and follow the instructions.</span>");
                container = session.createDocument(container);
            } else {
                container = session.getDocument(targetPath);
            }
        }
        return container;
    }

    private DocumentModel getOrCreateTestSamplesContainer() {
        DocumentModel container = null;
        DocumentModel parentContainer = getOrCreateSampleContainer();

        PathRef targetPath = new PathRef(getOrCreateSampleContainer().getPathAsString() + "/" + getRawTemplateResourcesRootPath());
        if (!session.exists(targetPath)) {
            container = session.createDocumentModel(parentContainer.getPathAsString(), "rawsamples",
                    "Workspace");
            container.setPropertyValue("dc:title", "More (Raw) Examples");
            container.setPropertyValue("dc:description",
                    "This space contains raw examples to demonstrate the Nuxeo template rendering add-on's advanced possibilities. Go to the \"Discover Customization Samples\" folder first if you did not follow its instructions yet.");
            container = session.createDocument(container);
        } else {
            container = session.getDocument(targetPath);
        }
        return container;
    }

    protected boolean isImportAlreadyDone() {
        if (Framework.isTestModeSet()) {
            return false;
        }

        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> entries = reader.queryLogs(IMPORT_ALREADY_DONE_EVENTS, null);
        return !entries.isEmpty();
    }

    protected void markImportDone() {
        if (Framework.isTestModeSet()) {
            return;
        }

        AuditLogger writer = Framework.getService(AuditLogger.class);

        LogEntry entry = writer.newLogEntry();
        entry.setEventId(TEMPLATE_SAMPLE_INIT_EVENT);
        entry.setEventDate(Calendar.getInstance().getTime());

        List<LogEntry> entries = new ArrayList<LogEntry>();
        entries.add(entry);
        writer.addLogEntries(entries);

    }

    public int importModels() {

        if (isImportAlreadyDone()) {
            return 0;
        }

        int nbImportedDocs = 0;
        Path path = TemplateBundleActivator.getDataDirPath();
        path = path.append(getTemplateResourcesRootPath());
        File root = new File(path.toString());
        if (root.exists()) {
            File[] modelRoots = root.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (!pathname.isDirectory()) {
                        return false;
                    }
                    return true;
                }
            });

            if (modelRoots != null && modelRoots.length > 0) {
                for (File modelRoot : modelRoots) {
                    log.info("Importing template from " + modelRoot.getAbsolutePath());
                    try {
                        nbImportedDocs += importModelAndExamples(modelRoot);
                    } catch (IOException e) {
                        throw new NuxeoException("Failed to import from template: " + modelRoot.getAbsolutePath(), e);
                    }
                }
                markImportDone();
            }
        }

        return nbImportedDocs;
    }

    public int importModelAndExamples(File root) throws IOException {

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
                } else if (file.getName().equals(TEST_TEMPLATE_ROOT) && Framework.isTestModeSet()) {
                	roots.put(TEMPLATE_ROOT, file);
                    return true;
                } else if (file.getName().equals(EXAMPLE_ROOT)) {
                    roots.put(EXAMPLE_ROOT, file);
                    return true;
                } else if (file.getName().equals(TEST_EXAMPLE_ROOT) && Framework.isTestModeSet()) {
                    roots.put(TEST_EXAMPLE_ROOT, file);
                    return true;
                }

                return false;
            }
        });

        if (roots.size() >= 1) {
            if (roots.get(TEMPLATE_ROOT) != null) {
                DocumentModel templatesContainer = getOrCreateTemplateContainer();
                DocumentModel samplesContainer = getOrCreateSampleContainer();
                DocumentModel testSamplesContainer = null;
                if(Framework.isTestModeSet()){
                	testSamplesContainer = getOrCreateTestSamplesContainer();
                }
                if (templatesContainer != null) {
                    DocumentRef modelRef = importModel(root.getName(), roots.get(TEMPLATE_ROOT), templatesContainer);
                    nbImportedDocs++;
                    if (samplesContainer != null) {
                        if (roots.get(EXAMPLE_ROOT) != null) {
                            nbImportedDocs = nbImportedDocs
                                    + importSamples(roots.get(EXAMPLE_ROOT), modelRef, samplesContainer);
                        }
                        if (roots.get(TEST_EXAMPLE_ROOT) != null
                        	&& Framework.isTestModeSet()) {
                            nbImportedDocs = nbImportedDocs
                                    + importSamples(roots.get(TEST_EXAMPLE_ROOT), modelRef, testSamplesContainer);
                        }
                    }
                }
            }
        }

        return nbImportedDocs;

    }

    protected DocumentRef importModel(String modelName, File source, DocumentModel root) throws IOException {

        // import
        DocumentReader reader = new XMLModelReader(source, modelName);
        DocumentWriter writer = new DocumentModelWriter(session, root.getPathAsString());

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        DocumentTranslationMap map = pipe.run();

        DocumentRef ref = map.getDocRefMap().values().iterator().next();
        session.save();

        return ref;
    }

    protected int importSamples(File root, DocumentRef modelRef, DocumentModel rootDoc) throws IOException {

        int nbImportedDocs = 0;
        for (File exampleDir : root.listFiles()) {
            if (!exampleDir.isDirectory()) {
                continue;
            }

            // import
            DocumentReader reader = new XMLModelReader(exampleDir, exampleDir.getName());
            DocumentWriter writer = new DocumentModelWriter(session, rootDoc.getPathAsString());

            DocumentPipe pipe = new DocumentPipeImpl(10);

            final String targetUUID = modelRef.toString();

            pipe.addTransformer(new DocumentTransformer() {

                @Override
                public boolean transform(ExportedDocument xdoc) throws IOException {
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
