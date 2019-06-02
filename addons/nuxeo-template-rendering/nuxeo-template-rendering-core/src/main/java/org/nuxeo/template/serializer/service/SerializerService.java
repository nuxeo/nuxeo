package org.nuxeo.template.serializer.service;


import org.nuxeo.template.serializer.executors.Serializer;

/**
 * Service Exposing serializer and deserializer used to manipulate template rendering data to be injected in
 * the rendition context. Here are the current service usage :
 * <ul>
 *     <li>API request => Inline context preparation : see in {@link org.nuxeo.template.automation.RenderWithTemplateOperation}</li>
 *     <li>Inline context preparation => store into the {@link org.nuxeo.template.api.adapters.TemplateBasedDocument}</li>
 *     <li>Context defined on Template creation => store into the {@link org.nuxeo.template.api.adapters.TemplateSourceDocument}</li>
 *     <li>And finally before rendition to collect data from TemplateSource and TemplateBased to generate the global context</li>
 * </ul>
 * You can create your own Serializer contributing to the extension point and call it on the API request. For instance,
 * if you want to send json instead XML.
 *
 * @Since 11.1
 */
public interface SerializerService {

    /**
     * Return the Serializer/Deserializer named id that transform List<TemplateInput> to a target serialized format.
     * Default serialized format is XML except if you override contributing a "default" serializer.
     * If no serializer named id, throws a NuxeoException.
     *
     * @param id : name of the requested serializer
     * @return the constructed serializer
     */
    Serializer getSerializer(String id);
}
