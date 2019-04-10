/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.xdocreport.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.processors.xdocreport.FieldDefinitionGenerator;

import fr.opensagres.xdocreport.remoting.resources.domain.BinaryData;
import fr.opensagres.xdocreport.remoting.resources.domain.Filter;
import fr.opensagres.xdocreport.remoting.resources.domain.LargeBinaryData;
import fr.opensagres.xdocreport.remoting.resources.domain.Resource;
import fr.opensagres.xdocreport.remoting.resources.domain.ResourceType;
import fr.opensagres.xdocreport.remoting.resources.services.ResourcesException;
import fr.opensagres.xdocreport.remoting.resources.services.jaxrs.JAXRSResourcesService;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class XDocReportResourceService extends AbstractResourceService implements JAXRSResourcesService {

    protected static final Log log = LogFactory.getLog(XDocReportResourceService.class);

    public XDocReportResourceService(CoreSession session) {
        super(session);
    }

    public List<BinaryData> download(List<String> resourcePaths) {
        return null;
    }

    @Override
    public String getName() {
        return "Nuxeo-Repository";
    }

    @Override
    public Resource getRoot() {
        Resource root = new NonRecursiveResource();
        root.setType(ResourceType.CATEGORY);
        root.setName("Nuxeo");
        root.setId("nuxeo");
        List<Resource> children = new ArrayList<Resource>();
        List<TemplateSourceDocument> templates = getTemplates();
        for (TemplateSourceDocument template : templates) {
            children.add(ResourceWrapper.wrap(template));
        }
        root.getChildren().addAll(children);
        return root;
    }

    @GET
    @Path("model/list")
    public String listModels() throws IOException {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        DocumentType[] docTypes = sm.getDocumentTypes();
        List<String> names = new ArrayList<String>();

        for (DocumentType dt : docTypes) {
            names.add(dt.getName());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JsonGenerator gen = JsonHelper.createJsonGenerator(out);
        gen.writeObject(names);

        return out.toString();
    }

    @GET
    @Path("model/{type}")
    public String getFieldDefinition(@PathParam("type") String type) {
        return FieldDefinitionGenerator.generate(type);
    }

    @Override
    public void upload(BinaryData dataIn) {
        String id = dataIn.getResourceId();
        if (id != null) {
            IdRef ref = new IdRef(id);
            try {
                DocumentModel target = session.getDocument(ref);
                TemplateSourceDocument template = target.getAdapter(TemplateSourceDocument.class);
                if (template != null) {
                    Blob oldBlob = template.getTemplateBlob();

                    Blob newBlob = Blobs.createBlob(dataIn.getContent(), oldBlob.getMimeType());
                    newBlob.setFilename(oldBlob.getFilename());
                    template.setTemplateBlob(newBlob, true);
                }
            } catch (PropertyException | IOException e) {
                log.error("Error during template upload", e);
            }
        }
    }

    @Override
    public List<BinaryData> downloadMultiple(List<String> arg0) throws ResourcesException {
        return null;
    }

    @Override
    public Resource getRootWithFilter(Filter filter) throws ResourcesException {
        return getRoot();
    }

    @Override
    public LargeBinaryData downloadLarge(String resourcePath) throws ResourcesException {
        CoreSession session = getCoreSession();
        if (resourcePath.endsWith(".fields.xml")) {
            String uuid = resourcePath.replace(".fields.xml", "");
            DocumentModel targetDoc = session.getDocument(new IdRef(uuid));
            TemplateSourceDocument template = targetDoc.getAdapter(TemplateSourceDocument.class);

            List<String> types = template.getApplicableTypes();
            String targetType = "File";
            if (types.size() > 0) {
                targetType = types.get(0);
            }
            String xml = FieldDefinitionGenerator.generate(targetType);
            try {
                return BinaryDataWrapper.wrapXml(xml, resourcePath);
            } catch (IOException e) {
                throw new ResourcesException(e);
            }
        } else {
            String uuid = resourcePath;
            DocumentModel targetDoc = session.getDocument(new IdRef(uuid));
            TemplateSourceDocument template = targetDoc.getAdapter(TemplateSourceDocument.class);
            try {
                return BinaryDataWrapper.wrap(template);
            } catch (IOException e) {
                throw new ResourcesException(e);
            }
        }
    }

    @Override
    public BinaryData download(String resourcePath) {
        return null;
    }

    @Override
    public void uploadLarge(LargeBinaryData dataIn) throws ResourcesException {
        String id = dataIn.getResourceId();
        if (id != null) {
            IdRef ref = new IdRef(id);
            try {
                DocumentModel target = session.getDocument(ref);
                TemplateSourceDocument template = target.getAdapter(TemplateSourceDocument.class);
                if (template != null) {
                    Blob oldBlob = template.getTemplateBlob();
                    Blob newBlob = Blobs.createBlob(dataIn.getContent(), oldBlob.getMimeType());
                    newBlob.setFilename(oldBlob.getFilename());
                    template.setTemplateBlob(newBlob, true);
                }
            } catch (PropertyException | IOException e) {
                log.error("Error during template upload", e);
            }
        }
    }

}
