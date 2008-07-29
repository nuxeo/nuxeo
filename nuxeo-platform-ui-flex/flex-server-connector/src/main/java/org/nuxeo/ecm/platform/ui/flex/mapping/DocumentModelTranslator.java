package org.nuxeo.ecm.platform.ui.flex.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.flex.javadto.FlexDocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.runtime.api.Framework;

public class DocumentModelTranslator {

    private static SchemaManager sm;

    private static Map<String, String> schemaCache = new ConcurrentHashMap<String, String>();

    private static FlexDocumentModel addIconURL(FlexDocumentModel fdm,
            DocumentModel doc) {
        String iconURL = DocumentModelFunctions.iconPath(doc);
        fdm.setProperty("common", "icon", iconURL);
        return fdm;
    }

    private static String getSchemaFromPrefix(String prefix) throws Exception {

        String schemaName = schemaCache.get(prefix);

        if (schemaName != null)
            return schemaName;

        if (sm == null)
            sm = Framework.getService(SchemaManager.class);

        Schema schema = sm.getSchemaFromPrefix(prefix);

        schemaName = schema.getSchemaName();

        if (schemaName == null) {
            schemaName = prefix;
        }
        schemaCache.put(prefix, schemaName);
        return schemaName;
    }

    public static FlexDocumentModel toFlexTypeFromPrefetch(DocumentModel doc)
            throws Exception {
        FlexDocumentModel fdm = new FlexDocumentModel(doc.getRef(),
                doc.getName(), doc.getPathAsString(),
                doc.getCurrentLifeCycleState(), doc.getType());

        fdm.setIsFolder(doc.isFolder());

        Map<String, Serializable> prefetch = doc.getPrefetch();
        String[] schemas = doc.getDeclaredSchemas();

        for (int i = 0; i < schemas.length; i++) {
            Map<String, Serializable> map = new HashMap<String, Serializable>();

            fdm.feed(schemas[i], map);
        }

        for (String prefetchKey : prefetch.keySet()) {
            String schemaName;
            String fieldName;

            if (prefetchKey.contains(":")) {
                schemaName = prefetchKey.split(":")[0];
                fieldName = prefetchKey.split(":")[1];
                schemaName = getSchemaFromPrefix(schemaName);
            } else {
                schemaName = prefetchKey.split("\\.")[0];
                fieldName = prefetchKey.split("\\.")[1];
            }

            fdm.setProperty(schemaName, fieldName, prefetch.get(prefetchKey));
        }
        return fdm;
    }

    public static FlexDocumentModel toFlexType(DocumentModel doc)
            throws Exception {

        FlexDocumentModel fdm = new FlexDocumentModel(doc.getRef(),
                doc.getName(), doc.getPathAsString(),
                doc.getCurrentLifeCycleState(), doc.getType());

        fdm.setIsFolder(doc.isFolder());

        DocumentPart[] parts = doc.getParts();

        for (int i = 0; i < parts.length; i++) {
            Map<String, Serializable> map = new HashMap<String, Serializable>();
            Collection<Property> props = parts[i].getChildren();

            String schemaPrefix = parts[i].getSchema().getNamespace().prefix;
            if (schemaPrefix == "")
                schemaPrefix = parts[i].getSchema().getName();

            for (Property prop : props) {
                String fieldName = prop.getName();
                fieldName = fieldName.replace(schemaPrefix + ":", "");

                if (prop.getType().isSimpleType()) {
                    map.put(fieldName, prop.getValue());
                } else if (prop.getType().isComplexType()) {
                    if (prop instanceof BlobProperty) {

                        BlobProperty blobProp = (BlobProperty) prop;
                        Blob blob = (Blob) blobProp.getValue();
                        if (blob != null) {
                            String fileName = blob.getFilename();
                            if (fileName==null)
                            {
                                // XXX Hack !
                                if (parts[i].getSchema().getName().equals("file"))
                                {
                                    fileName=(String) doc.getProperty("file", "filename");
                                }
                            }
                            String dwURL = DocumentModelFunctions.fileUrl(
                                    "downloadFile", doc, prop.getName(),
                                    fileName);
                            map.put(fieldName, dwURL);
                        }
                    } else if (prop instanceof MapProperty) {
                        MapProperty mapProp = (MapProperty) prop;
                        map.put(fieldName, mapProp.getValue());
                    }
                } else if (prop.getType().isListType()) {
                    if (prop instanceof ListProperty) {
                        ListProperty listProp = (ListProperty) prop;
                        List<Serializable> lstProp = new ArrayList<Serializable>();
                        lstProp.addAll(listProp.getChildren());
                        map.put(fieldName, (Serializable) lstProp);
                    } else if (prop instanceof ArrayProperty) {
                        ArrayProperty arrayProp = (ArrayProperty) prop;
                        map.put(fieldName, arrayProp.getValue());
                    }
                }
            }
            fdm.feed(parts[i].getName(), map);
        }
        return fdm;
    }

    public static DocumentModel toDocumentModel(FlexDocumentModel fdoc,
            CoreSession session) throws Exception {

        String refAsString = fdoc.getDocRef();
        DocumentModel doc = null;
        if (refAsString == null || "".equals(refAsString)) {
            String docType = fdoc.getType();
            String name = fdoc.getName();
            String docPath = fdoc.getPath();
            String parentPath = new Path(docPath).removeLastSegments(1).toString();
            doc = session.createDocumentModel(parentPath, name, docType);
            doc = session.createDocument(doc);
        } else {
            DocumentRef docRef = new IdRef(refAsString);
            doc = session.getDocument(docRef);
        }

        Map<String, Serializable> dirtyFields = fdoc.getDirtyFields();

        for (String path : dirtyFields.keySet()) {
            doc.setPropertyValue(path, dirtyFields.get(path));
        }

        return doc;
    }

}
