/*
 * (C) Copyright 2002-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.importer.xml.parser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.InvalidXPathException;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultText;
import org.mvel2.MVEL;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.runtime.api.Framework;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Main implementation class for delivering the Import logic
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XMLImporterServiceImpl {

    protected static final String FILE_PROPERTY = "file";

    protected static final String CONTENT_PROPERTY = "content";

    protected static final String MIME_TYPE_PROPERTY = "mimetype";

    protected static final String FILE_NAME_PROPERTY = "filename";

    private static final String MSG_NO_ELEMENT_FOUND = "**CREATION**\n"
            + "No element \"%s\" found in %s, use the DOC_TYPE-INDEX value";

    private static final String MSG_CREATION = "**CREATION**\n"
            + "Try to create document in %s with name %s based on \"%s\" fragment " + "with the following conf: %s\n";

    private static final String MSG_UPDATE = "**DOCUMENT UPDATE**\n"
            + "Try to update document in %s with name %s based on \"%s\" fragment " + "with the following conf: %s\n";

    private static final String MSG_UPDATE_PROPERTY_TRACE = "**PROPERTY UPDATE**\n"
            + "Value found for %s in %s is \"%s\". With the following conf: %s";

    private static final String MSG_UPDATE_PROPERTY = "**PROPERTY UPDATE**\n"
            + "Try to set value into %s property based on %s element on document \"%s\" (%s). Conf activated: %s";

    public static final Log log = LogFactory.getLog(XMLImporterServiceImpl.class);

    public static final String XML_IMPORTER_INITIALIZATION = "org.nuxeo.xml.importer.initialization";

    protected CoreSession session;

    protected DocumentModel rootDoc;

    protected Stack<DocumentModel> docsStack;

    protected Map<String, List<String>> deletedAttributes = new HashMap<>();

    protected Map<String, Object> mvelCtx = new HashMap<>();

    protected Map<Element, DocumentModel> elToDoc = new HashMap<>();

    protected ParserConfigRegistry registry;

    protected Boolean deferSave = false;

    public XMLImporterServiceImpl(DocumentModel rootDoc, ParserConfigRegistry registry) {
        this(rootDoc, registry, null, false);
    }

    public XMLImporterServiceImpl(DocumentModel rootDoc, ParserConfigRegistry registry,
            Map<String, Object> mvelContext, boolean deferSave) {
        if (mvelContext != null) {
            mvelCtx.putAll(mvelContext);
        }

        session = rootDoc.getCoreSession();
        this.rootDoc = rootDoc;
        this.deferSave = deferSave;

        docsStack = new Stack<>();
        pushInStack(rootDoc);
        mvelCtx.put("root", rootDoc);
        mvelCtx.put("docs", docsStack);
        mvelCtx.put("session", session);

        this.registry = registry;
    }

    protected ParserConfigRegistry getRegistry() {
        return registry;
    }

    protected DocConfigDescriptor getDocCreationConfig(Element el) {
        for (DocConfigDescriptor conf : getRegistry().getDocCreationConfigs()) {
            // direct tagName match
            if (conf.getTagName().equals(el.getName())) {
                return conf;
            } else {
                // try xpath match
                try {
                    if (el.matches(conf.getTagName())) {
                        return conf;
                    }
                } catch (InvalidXPathException e) {
                    // NOP
                }
            }
        }
        return null;
    }

    protected List<AttributeConfigDescriptor> getAttributConfigs(Element el) {
        List<AttributeConfigDescriptor> result = new ArrayList<>();
        for (AttributeConfigDescriptor conf : getRegistry().getAttributConfigs()) {
            if (conf.getTagName().equals(el.getName())) {
                result.add(conf);
            } else {
                // try xpath match
                try {
                    if (el.matches(conf.getTagName())) {
                        result.add(conf);
                    }
                } catch (InvalidXPathException e) {
                    // NOP
                }
            }
        }
        return result;
    }

    protected File workingDirectory;

    private AutomationService automationService;

    public List<DocumentModel> parse(InputStream is) throws IOException {
        mvelCtx.put("source", is);
        try {
            Document doc;
            doc = new SAXReader().read(is);
            workingDirectory = null;
            return parse(doc);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    public List<DocumentModel> parse(File file) throws IOException {
        mvelCtx.put("source", file);

        Document doc = null;
        File directory = null;
        try {
            doc = new SAXReader().read(file);
            workingDirectory = file.getParentFile();
        } catch (DocumentException e) {
            File tmp = Environment.getDefault().getTemp();
            directory = new File(tmp, file.getName() + System.currentTimeMillis());
            directory.mkdir();
            ZipUtils.unzip(file, directory);
            for (File child : directory.listFiles()) {
                if (child.getName().endsWith(".xml")) {
                    return parse(child);
                }
            }
            throw new NuxeoException("Can not find XML file inside the zip archive", e);
        } finally {
            FileUtils.deleteQuietly(directory);
        }
        return parse(doc);
    }

    public List<DocumentModel> parse(Document doc) {
        Element root = doc.getRootElement();
        elToDoc = new HashMap<>();
        mvelCtx.put("xml", doc);
        mvelCtx.put("map", elToDoc);
        process(root);

        // defer saveDocument to end of operation
        if (deferSave) {
            ArrayList<DocumentModel> a = new ArrayList<>();
            DocumentModel d = null;
            while (docsStack.size() > 0) {
                d = popStack();
                d.putContextData(XML_IMPORTER_INITIALIZATION, Boolean.TRUE);
                d = session.saveDocument(d);
                a.add(d);
            }
            return a;
        } else {
            return new ArrayList<>(docsStack);
        }
    }

    protected Object resolveComplex(Element el, AttributeConfigDescriptor conf) {
        Map<String, Object> propValue = new HashMap<>();
        for (String name : conf.getMapping().keySet()) {
            propValue.put(name, resolveAndEvaluateXmlNode(el, conf.getMapping().get(name)));
        }

        return propValue;
    }

    protected Blob resolveBlob(Element el, AttributeConfigDescriptor conf, String propertyName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> propValues = (Map<String, Object>) resolveComplex(el, conf);

        if (propValues.containsKey(propertyName)) {
            try {
                Blob blob = null;
                String content = (String) propValues.get(propertyName);
                if (content != null && workingDirectory != null) {
                    File file = new File(workingDirectory, content.trim());
                    if (file.exists()) {
                        blob = Blobs.createBlob(file);
                    }
                }
                if (blob == null && content != null) {
                    blob = Blobs.createBlob(content);
                }

                if (blob != null) {
                    if (propValues.containsKey(MIME_TYPE_PROPERTY)) {
                        blob.setMimeType((String) propValues.get(MIME_TYPE_PROPERTY));
                    }
                    if (propValues.containsKey(FILE_NAME_PROPERTY)) {
                        blob.setFilename((String) propValues.get(FILE_NAME_PROPERTY));
                    }
                }

                return blob;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void processDocAttributes(DocumentModel doc, Element el, AttributeConfigDescriptor conf) {
        String targetDocProperty = conf.getTargetDocProperty();

        if (log.isDebugEnabled()) {
            log.debug(String.format(MSG_UPDATE_PROPERTY, targetDocProperty, el.getUniquePath(), doc.getPathAsString(),
                    doc.getType(), conf.toString()));
        }
        Property property = doc.getProperty(targetDocProperty);

        if (property.isScalar()) {
            Object value = resolveAndEvaluateXmlNode(el, conf.getSingleXpath());
            if (log.isTraceEnabled()) {
                log.trace(String.format(MSG_UPDATE_PROPERTY_TRACE, targetDocProperty, el.getUniquePath(), value,
                        conf.toString()));
            }
            property.setValue(value);

        } else if (property.isComplex()) {

            if (property instanceof BlobProperty) {
                Object value = resolveBlob(el, conf, CONTENT_PROPERTY);
                if (log.isTraceEnabled()) {
                    log.trace(String.format(MSG_UPDATE_PROPERTY_TRACE, targetDocProperty, el.getUniquePath(), value,
                            conf.toString()));
                }
                property.setValue(value);
            } else {
                Object value = resolveComplex(el, conf);
                if (log.isTraceEnabled()) {
                    log.trace(String.format(MSG_UPDATE_PROPERTY_TRACE, targetDocProperty, el.getUniquePath(), value,
                            conf.toString()));
                }
                property.setValue(value);
            }

        } else if (property.isList()) {

            ListType lType = (ListType) property.getType();

            Serializable value;

            if (lType.getFieldType().isSimpleType()) {
                value = (Serializable) resolveAndEvaluateXmlNode(el, conf.getSingleXpath());
                if (value != null) {
                    Object values = property.getValue();
                    if (values == null) {
                        property.setValue(new Object[] { value });
                    } else if (values instanceof Object[]) {
                        int len = ((Object[]) values).length;
                        Object[] newValues = new Object[len + 1];
                        System.arraycopy(values, 0, newValues, 0, len);
                        newValues[len] = value;
                        property.setValue(newValues);
                    } else {
                        log.error("Simple multi value property " + targetDocProperty
                                + " is not an Array");
                    }
                }
            } else {
                Map<String, Object> props = (Map<String, Object>) resolveComplex(el, conf);
                if (props.containsKey(FILE_PROPERTY)) {
                    Blob blob = resolveBlob(el, conf, FILE_PROPERTY);
                    props.put(FILE_PROPERTY, blob);
                }
                property.addValue(props);
                value = (Serializable) props;
            }

            if (log.isTraceEnabled()) {
                log.trace(String.format(MSG_UPDATE_PROPERTY_TRACE, targetDocProperty, el.getUniquePath(), value,
                        conf.toString()));
            }
        }
    }

    protected Map<String, Object> getMVELContext(Element el) {
        mvelCtx.put("currentDocument", docsStack.peek());
        mvelCtx.put("currentElement", el);
        mvelCtx.put("Fn", new MVELImporterFunction(session, docsStack, elToDoc, el));
        return mvelCtx;
    }

    protected Object resolve(Element el, String xpr) {
        if (xpr == null) {
            return null;
        }

        if (xpr.startsWith("#{") && xpr.endsWith("}")) { // MVEL
            xpr = xpr.substring(2, xpr.length() - 1);
            return resolveMVEL(el, xpr);
        } else if (xpr.contains("{{")) { // String containing XPaths
            StringBuffer sb = new StringBuffer();
            int idx = xpr.indexOf("{{");
            while (idx >= 0) {
                int idx2 = xpr.indexOf("}}", idx);
                if (idx2 > 0) {
                    sb.append(xpr.substring(0, idx));
                    String xpath = xpr.substring(idx + 2, idx2);
                    sb.append(resolveAndEvaluateXmlNode(el, xpath));
                    xpr = xpr.substring(idx2);
                } else {
                    sb.append(xpr);
                    xpr = "";
                }
                idx = xpr.indexOf("{{");
            }
            return sb.toString();
        } else {
            return resolveXP(el, xpr); // default to pure XPATH
        }
    }

    protected Object resolveMVEL(Element el, String xpr) {
        Map<String, Object> ctx = new HashMap<>(getMVELContext(el));
        Serializable compiled = MVEL.compileExpression(xpr);
        return MVEL.executeExpression(compiled, ctx);
    }

    protected Object resolveXP(Element el, String xpr) {
        List<Object> nodes = el.selectNodes(xpr);
        if (nodes.size() == 1) {
            return nodes.get(0);
        } else if (nodes.size() > 1) {
            // Workaround for NXP-11834
            if (xpr.endsWith("text()")) {
                String value = "";
                for (Object node : nodes) {
                    if (!(node instanceof DefaultText)) {
                        String msg = "Text selector must return a string (expr:\"%s\") element %s";
                        log.error(String.format(msg, xpr, el.getStringValue()));
                        return value;
                    }
                    value += ((DefaultText) node).getText();
                }
                return new DefaultText(value);
            }
            return nodes;
        }
        return null;
    }

    protected String resolvePath(Element el, String xpr) {
        Object ob = resolve(el, xpr);
        if (ob == null) {
            for (int i = 0; i < docsStack.size(); i++) {
                if (docsStack.get(i).isFolder()) {
                    return docsStack.get(i).getPathAsString();
                }
            }
        } else {
            if (ob instanceof DocumentModel) {
                return ((DocumentModel) ob).getPathAsString();
            } else if (ob instanceof Node) {
                if (ob instanceof Element) {
                    Element targetElement = (Element) ob;
                    DocumentModel target = elToDoc.get(targetElement);
                    if (target != null) {
                        return target.getPathAsString();
                    } else {
                        return targetElement.getText();
                    }
                } else if (ob instanceof Attribute) {
                    return ((Attribute) ob).getValue();
                } else if (ob instanceof Text) {
                    return ((Text) ob).getText();
                } else if (ob.getClass().isAssignableFrom(Attribute.class)) {
                    return ((Attribute) ob).getValue();
                }
            } else {
                return ob.toString();
            }
        }
        return rootDoc.getPathAsString();
    }

    protected String resolveName(Element el, String xpr) {
        Object ob = resolveAndEvaluateXmlNode(el, xpr);
        if (ob == null) {
            return null;
        }
        return ob.toString();
    }

    protected Object resolveAndEvaluateXmlNode(Element el, String xpr) {
        Object ob = resolve(el, xpr);
        if (ob == null) {
            return null;
        }
        if (ob instanceof Node) {
            return ((Node) ob).getText();
        } else {
            return ob;
        }
    }

    protected void createNewDocument(Element el, DocConfigDescriptor conf) {
        DocumentModel doc = session.createDocumentModel(conf.getDocType());

        String path = resolvePath(el, conf.getParent());
        Object nameOb = resolveName(el, conf.getName());
        String name = null;
        if (nameOb == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(MSG_NO_ELEMENT_FOUND, conf.getName(), el.getUniquePath()));
            }
            int idx = 1;
            for (int i = 0; i < docsStack.size(); i++) {
                if (docsStack.get(i).getType().equals(conf.getDocType())) {
                    idx++;
                }
            }
            name = conf.getDocType() + "-" + idx;
        } else {
            name = nameOb.toString();
        }
        doc.setPathInfo(path, name);

        if (log.isDebugEnabled()) {
            if (conf.getUpdate()) {
                log.debug(String.format(MSG_UPDATE, path, name, el.getUniquePath(), conf.toString()));
            } else {
                log.debug(String.format(MSG_CREATION, path, name, el.getUniquePath(), conf.toString()));
            }
        }

        try {
            if (conf.getUpdate() && session.exists(doc.getRef())) {
                DocumentModel existingDoc = session.getDocument(doc.getRef());

                // get attributes, if attribute needs to be overwritten, empty in the document
                for (Object e : el.elements()) {
                    List<AttributeConfigDescriptor> configs = getAttributConfigs((Element) e);
                    if (configs != null) {
                        if (!deletedAttributes.containsKey(existingDoc.getId())) {
                            deletedAttributes.put(existingDoc.getId(), new ArrayList<String>());
                        }
                        for (AttributeConfigDescriptor config : configs) {
                            String targetDocProperty = config.getTargetDocProperty();
                            // check deletedAttributes for attribute which should be overwritten
                            // if it is there, don't empty it a second time
                            if (config.overwrite
                                    && !deletedAttributes.get(existingDoc.getId()).contains(targetDocProperty)) {
                                deletedAttributes.get(existingDoc.getId()).add(targetDocProperty);
                                existingDoc.setPropertyValue(targetDocProperty, new ArrayList<>());
                            }
                        }
                    }
                }
                doc = existingDoc;
            } else {
                doc = session.createDocument(doc);
            }
        } catch (NuxeoException e) {
            e.addInfo(String.format(MSG_CREATION, path, name, el.getUniquePath(), conf.toString()));
            throw e;
        }
        pushInStack(doc);
        elToDoc.put(el, doc);
    }

    protected void process(Element el) {
        DocConfigDescriptor createConf = getDocCreationConfig(el);
        if (createConf != null) {
            createNewDocument(el, createConf);
        }
        List<AttributeConfigDescriptor> configs = getAttributConfigs(el);
        if (configs != null) {
            for (AttributeConfigDescriptor config : configs) {
                processDocAttributes(docsStack.peek(), el, config);
            }

            DocumentModel doc = popStack();
            doc.putContextData(XML_IMPORTER_INITIALIZATION, Boolean.TRUE);
            if (!deferSave) {
                doc = session.saveDocument(doc);
            }
            pushInStack(doc);

            if (createConf != null) {
                String chain = createConf.getAutomationChain();
                if (chain != null && !"".equals(chain.trim())) {
                    try (OperationContext ctx = new OperationContext(session)) {
                        ctx.putAll(mvelCtx);
                        ctx.setInput(docsStack.peek());
                        getAutomationService().run(ctx, chain);
                    } catch (NuxeoException e) {
                        throw e;
                    } catch (Exception e) {
                        ExceptionUtils.checkInterrupt(e);
                    }
                }
            }
        }
        for (Object e : el.elements()) {
            process((Element) e);
        }
    }

    private AutomationService getAutomationService() {
        if (automationService == null) {
            automationService = Framework.getService(AutomationService.class);
        }
        return automationService;

    }

    private void pushInStack(DocumentModel doc) {
        mvelCtx.put("changeableDocument", doc);
        docsStack.push(doc);
    }

    private DocumentModel popStack() {
        DocumentModel doc = docsStack.pop();
        mvelCtx.put("changeableDocument", doc);
        return doc;
    }

}
