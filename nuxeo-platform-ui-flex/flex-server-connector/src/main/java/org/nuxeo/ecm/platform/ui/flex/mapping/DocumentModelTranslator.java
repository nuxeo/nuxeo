package org.nuxeo.ecm.platform.ui.flex.mapping;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.flex.javadto.FlexDocumentModel;

public class DocumentModelTranslator {


    public static FlexDocumentModel toFlexType(DocumentModel doc) throws Exception
    {

        FlexDocumentModel fdm = new FlexDocumentModel(doc.getRef(),doc.getName(),doc.getPathAsString(), doc.getCurrentLifeCycleState());


        DocumentPart[] parts = doc.getParts();

        for (int i=0;i<parts.length;i++)
        {
            Map<String,Serializable> map = new HashMap<String, Serializable>();
            Collection<Property> props = parts[i].getChildren();

            String schemaPrefix=parts[i].getSchema().getNamespace().prefix;
            if (schemaPrefix=="")
                schemaPrefix=parts[i].getSchema().getName();

            for (Property prop : props)
            {
                String fieldName = prop.getName();
                fieldName=fieldName.replace(schemaPrefix+":", "");

                if (prop.getType().isSimpleType())
                {
                    map.put(fieldName, prop.getValue());
                }
                else if (prop.getType().isComplexType())
                {
                    if (prop instanceof BlobProperty) {
                        BlobProperty blobProp = (BlobProperty) prop;
                        map.put(fieldName,"someurl");
                    }
                }
            }
            fdm.feed(parts[i].getName(), map);
        }
        return fdm;
    }

    public static DocumentModel toDocumentModel(FlexDocumentModel fdoc, CoreSession session) throws Exception
    {

        DocumentRef docRef = new IdRef(fdoc.getDocRef());

        DocumentModel doc = session.getDocument(docRef);

        Map<String, Serializable> dirtyFields = fdoc.getDirtyFields();

        for (String path : dirtyFields.keySet())
        {
            doc.setPropertyValue(path, dirtyFields.get(path));
        }

        return doc;
    }


}
