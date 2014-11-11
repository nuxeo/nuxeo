package org.nuxeo.ecm.platform.transform.compat;

import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.service.extensions.PluginExtension;

public class CompatConverterDescriptor extends ConverterDescriptor {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected Class<Plugin> pluginClass;

    public CompatConverterDescriptor(PluginExtension pluginDesciptor,  Class<Plugin> pluginClass) {
        super();
        destinationMimeType =  pluginDesciptor.getDestinationMimeType();
        sourceMimeTypes = pluginDesciptor.getSourceMimeTypes();
        this.pluginClass=pluginClass;
        converterName = pluginDesciptor.getName();
    }

    @Override
    public void initConverter() throws Exception {
        if (instance == null) {
            Plugin plugin= pluginClass.newInstance();
            instance = new ConverterWrappingPlugin(plugin);
            instance.init(this);
        }
    }

    @Override
    public Converter getConverterInstance() {
        try {
            initConverter();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return instance;
    }

}
