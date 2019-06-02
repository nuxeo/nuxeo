package org.nuxeo.template.serializer.executors;

import org.dom4j.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.TemplateInput;

import java.util.List;

/**
 * (was org.nuxeo.template.XMLSerializer).
 * {@link TemplateInput} parameters are stored in the {@link DocumentModel} as a single String Property via XML
 * Serialization. This class contains the Serialization/Deserialization logic.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author bjalon (bjalon@qastia.com)
 *
 * @Since 11.1
 */
public interface Serializer {

    /**
     * Transform String to a List of TemplateInput. TemplateInput represent a field that will be finally added into the
     * context of the file rendition. Please for more information look the documentation of
     * {@link org.nuxeo.template.serializer.service.SerializerService}
     *
     * @param content : String containing a list of fields' description serialized
     * @return the content deserialized
     * @throws DocumentException
     */
    List<TemplateInput> doDeserialization(String content) throws DocumentException;

    /**
     * Transform the List of TemplateInput to String. Used to store a rendition context in the Document TemplateBased.
     * Please for more information look the documentation of
     * {@link org.nuxeo.template.serializer.service.SerializerService}
     *
     * @param content
     * @return
     */
    String doSerialization(List<TemplateInput> content);
}
