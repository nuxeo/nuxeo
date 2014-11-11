package org.nuxeo.ecm.platform.transform.compat;

import java.util.List;

import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.service.extensions.TransformerExtension;

public class TransformerCompatConverterDescriptor extends ConverterDescriptor {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected Class<Transformer> transformerClass;

    protected TransformerExtension transformerDescriptor;

    public TransformerCompatConverterDescriptor(TransformerExtension transformerExtension) throws ConversionException {
        super();
        List<String> pluginChain = transformerExtension.getPluginsChain().getPluginsChain();
        subConverters = pluginChain;
        steps= null;
        converterName = transformerExtension.getName();

        sourceMimeTypes = ConversionServiceImpl.getConverterDesciptor(pluginChain.get(0)).getSourceMimeTypes();
        destinationMimeType = ConversionServiceImpl.getConverterDesciptor(pluginChain.get(pluginChain.size()-1)).getDestinationMimeType();
    }


}
