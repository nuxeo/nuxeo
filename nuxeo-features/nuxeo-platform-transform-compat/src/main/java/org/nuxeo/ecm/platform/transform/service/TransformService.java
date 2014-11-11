/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TransformService.java 30390 2008-02-21 01:42:54Z tdelprat $
 */

package org.nuxeo.ecm.platform.transform.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.ecm.core.convert.service.MimeTypeTranslationHelper;
import org.nuxeo.ecm.platform.transform.api.TransformException;
import org.nuxeo.ecm.platform.transform.compat.CompatConverterDescriptor;
import org.nuxeo.ecm.platform.transform.compat.PluginWrappingConverter;
import org.nuxeo.ecm.platform.transform.compat.TransformerCompatConverterDescriptor;
import org.nuxeo.ecm.platform.transform.compat.TransformerWrappingConverter;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.service.extensions.PluginExtension;
import org.nuxeo.ecm.platform.transform.service.extensions.TransformerExtension;
import org.nuxeo.ecm.platform.transform.service.extensions.TransformerExtensionPluginsConfiguration;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Backward compatibility implementation of the TransformService
 */
public class TransformService extends DefaultComponent implements
        TransformServiceCommon {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.transform.service.TransformService");

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TransformService.class);

    private static final String SUPPORTED_TRANSFORMER_CLASS = "org.nuxeo.ecm.platform.transform.transformer.TransformerImpl";

    protected ConversionService cs;

    protected ConversionService getConversionService() throws Exception {
        if (cs == null) {
            cs = Framework.getService(ConversionService.class);
        }
        if (cs == null) {
            throw new ClientException("Unable to locale ConversionService");
        }
        return cs;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        // TODO: put initialization here! not in ctor
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        // TODO shutdown registries here
    }

    @Override
    public void registerExtension(Extension extension) {
        if (extension.getExtensionPoint().equals("plugins")) {
            Object[] contribs = extension.getContributions();

            for (Object contrib : contribs) {
                PluginExtension pluginExtension = (PluginExtension) contrib;
                registerPluginAsConverter(pluginExtension, extension);
            }

        } else if (extension.getExtensionPoint().equals("transformers")) {
            Object[] contribs = extension.getContributions();

            for (Object contrib : contribs) {
                TransformerExtension transformerExtension = (TransformerExtension) contrib;
                try {
                    registerTransformerAsConverter(transformerExtension, extension);
                } catch (ConversionException e) {
                    log.error("Unable to register transformer", e);
                }
            }
        } else {
            log.error("Unknown contributions... can't register !");
        }
    }

    @SuppressWarnings("unchecked")
    protected void registerPluginAsConverter(PluginExtension descriptor, Extension extension) {
        String pluginName = descriptor.getName();

        log.warn("Trying to register a Transformer plugin with name " + pluginName
                + ". TransformService is now deprecated, please use ConversionService. "
                + "Wrapping the Transformer as a Converter.");

        ConverterDescriptor newDesc = ConversionServiceImpl.getConverterDesciptor(pluginName);
        if (newDesc != null) {
            log.warn("Can not register a Transformer plugin with name " + pluginName
                    + " because a converter with the same name already exists.");
        }

        Class<Plugin> pluginClass;
        try {
            pluginClass = extension.getContext().loadClass(descriptor.getClassName());
        } catch (ClassNotFoundException e) {
            log.error("Error while trying to create Plugin Class", e);
            return;
        }

        ConverterDescriptor converterDescriptor = new CompatConverterDescriptor(descriptor, pluginClass);
        ConversionServiceImpl.registerConverter(converterDescriptor);
    }

    protected void registerTransformerAsConverter(TransformerExtension descriptor, Extension extension) throws ConversionException {
        if (!SUPPORTED_TRANSFORMER_CLASS.equals(descriptor.getClassName())) {
            log.warn("only transformer based on " + SUPPORTED_TRANSFORMER_CLASS + " are supported by compatibility, skipping");
            return;
        }
        TransformerExtensionPluginsConfiguration pluginChain = descriptor.getPluginsChain();

        if (pluginChain.getPluginsChain().size() == 1) {
            log.warn("Skipping transformer registration since this is just a wrapper!!!");
            return;
        }

        ConverterDescriptor converterDescriptor = new TransformerCompatConverterDescriptor(descriptor);
        ConversionServiceImpl.registerConverter(converterDescriptor);
    }

    @Override
    public void unregisterExtension(Extension extension) {
        if (extension.getExtensionPoint().equals("plugins")) {

        } else if (extension.getExtensionPoint().equals("transformers")) {

        } else {
            log.error("Unknown contributions... can't unregister!");
        }
    }

    public Plugin getPluginByName(String name) {
        ConverterDescriptor desc = ConversionServiceImpl.getConverterDesciptor(name);
        if (desc == null) {
            return null;
        }
        return new PluginWrappingConverter(desc);
    }

    public Plugin getPluginByMimeTypes(String sourceMT, String destinationMT) {
        String converterName;
        try {
            converterName = getConversionService().getConverterName(sourceMT, destinationMT);
            return getPluginByName(converterName);
        } catch (Exception e) {
            log.error("Error while accessing the ConversionService", e);
            return null;
        }
    }

    public List<Plugin> getPluginByDestinationMimeTypes(String destinationMT) {
        List<Plugin> plugins = new ArrayList<Plugin>();
        List<String> srcMTs = MimeTypeTranslationHelper.getSourceMimeTypes(destinationMT);

        for (String srcMT : srcMTs) {
            Plugin plugin = getPluginByMimeTypes(srcMT, destinationMT);
            if (plugin != null) {
                plugins.add(plugin);
            }
        }
        return plugins;
    }

    public Transformer getTransformerByName(String name) {

        ConverterDescriptor desc = ConversionServiceImpl.getConverterDesciptor(name);
        if (desc == null) {
            return null;
        }
        return new TransformerWrappingConverter(desc);
    }

    public void registerPlugin(String name, Plugin plugin) {
        throw new IllegalStateException("This method is no longer supported");
    }

    public void registerTransformer(String name, Transformer transformer) {
        throw new IllegalStateException("This method is no longer supported");
    }

    public List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources) throws TransformException {

        Transformer trans = getTransformerByName(transformerName);
        return trans.transform(options, sources);
    }

    public List<TransformDocument> transform(String transformerName,
            Map<String, Map<String, Serializable>> options,
            Blob... blobs) throws TransformException {

        TransformDocument[] tds = new TransformDocument[blobs.length];
        for (int i = 0; i < blobs.length; i++) {
            tds[i] = new TransformDocumentImpl(blobs[i]);
        }

        return transform(transformerName, options, tds);
    }

    public void unregisterPlugin(String name) {
    }

    public void unregisterTransformer(String name) {
    }

    public boolean isMimetypeSupportedByPlugin(String pluginName,
            String mimetype) {
        Plugin plugin = getPluginByName(pluginName);
        if (plugin == null) {
            return false;
        }
        List<String> sourceMimetype = plugin.getSourceMimeTypes();
        return sourceMimetype.contains(mimetype);
    }

}
