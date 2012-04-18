package org.nuxeo.template.samples;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentTransformer;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.template.samples.io.XMLModelReader;

public class ModelImporter {

    public static final String EXAMPLES_ROOT = "examples";

    public static final String TEMPLATE_ROOT = "template";

    protected final CoreSession session;

    public ModelImporter(CoreSession session) {
        this.session = session;
    }

    public void importModels() throws Exception {
        File root = FileUtils.getResourceFileFromContext("samples");

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
            importModelAndExamples(modelRoot);
        }
    }

    public void importModelAndExamples(File root) throws Exception {

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
                DocumentRef modelRef = importModel(root.getName(),
                        roots.get(TEMPLATE_ROOT));
                System.out.println(modelRef.toString());
                if (roots.get(EXAMPLES_ROOT) != null) {
                    importSamples(roots.get(EXAMPLES_ROOT), modelRef);
                }
            }
        }

    }

    protected DocumentRef importModel(String modelName, File source)
            throws Exception {

        // import
        DocumentReader reader = new XMLModelReader(source, modelName);
        DocumentWriter writer = new DocumentModelWriter(session, "/");

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        DocumentTranslationMap map = pipe.run();

        session.save();
        return map.getDocRefMap().values().iterator().next();
    }

    protected void importSamples(File root, DocumentRef modelRef)
            throws Exception {

        for (File exampleDir : root.listFiles()) {
            if (!exampleDir.isDirectory()) {
                continue;
            }

            // import
            DocumentReader reader = new XMLModelReader(exampleDir,
                    exampleDir.getName());
            DocumentWriter writer = new DocumentModelWriter(session, "/");

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
                            // XXX Should do better
                            if ("templateId".equalsIgnoreCase(node.getName())
                                    && "templateEntry".equalsIgnoreCase(node.getParent().getName())) {
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
            DocumentTranslationMap map = pipe.run();

        }
        session.save();
    }
}
