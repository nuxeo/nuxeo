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
 * $Id: AbstractTransformer.java 28508 2008-01-05 15:08:18Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.transformer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.service.TransformService;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract transformer.
 * <p>
 * Define default transformer implementation.
 *
 * @see org.nuxeo.ecm.platform.transform.interfaces
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractTransformer implements Transformer {

    private static final long serialVersionUID = 6646610808363319901L;

    private static final Log log = LogFactory.getLog(AbstractTransformer.class);

    protected String name;

    protected Map<String, Map<String, Serializable>> defaultOptions = new HashMap<String, Map<String, Serializable>>();

    protected List<String> pluginsChain = new ArrayList<String>();

    protected AbstractTransformer() {
        this(null, null);
    }

    protected AbstractTransformer(String name, List<String> pluginsChain) {
        this.name = name;
        this.pluginsChain = pluginsChain;
    }

    protected static TransformService getNXTransform() {
        return (TransformService) Framework.getRuntime().getComponent(TransformService.NAME);
    }

    public Map<String, Map<String, Serializable>> getDefaultOptions() {
        return defaultOptions;
    }

    public String getMimeTypeDestination() {
        String mtype = null;
        if (!pluginsChain.isEmpty()) {
            String pluginName = pluginsChain.get(pluginsChain.size() - 1);
            Plugin plugin = getNXTransform().getPluginByName(pluginName);
            mtype = plugin.getDestinationMimeType();
        }
        return mtype;
    }

    public List<String> getMimeTypeSources() {
        List<String> mtypes = new ArrayList<String>();
        if (!pluginsChain.isEmpty()) {
            String pluginName = pluginsChain.get(0);
            Plugin plugin = getNXTransform().getPluginByName(pluginName);
            mtypes = plugin.getSourceMimeTypes();
        }
        return mtypes;
    }

    public String getName() {
        return name;
    }

    public List<Plugin> getPluginChains() {
        List<Plugin> chain = new ArrayList<Plugin>();

        TransformServiceCommon nxtransform = getNXTransform();
        assert nxtransform != null;

        log.debug("pluginsChain " + pluginsChain);
        for (String name : pluginsChain) {
            Plugin plugin = nxtransform.getPluginByName(name);
            if (plugin != null) {
                chain.add(plugin);
            } else {
                log.warn("Plugin for name " + name + " not found");
            }
        }
        return chain;
    }

    public void setDefaultOptions(
            Map<String, Map<String, Serializable>> defaultOptions) {
        this.defaultOptions = defaultOptions;
    }

    public void setPluginChains(List<String> pluginsChain) {
        this.pluginsChain = pluginsChain;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TransformDocument> transform(
            Map<String, Map<String, Serializable>> options, Blob... blobs) {
        TransformDocument[] trs = new TransformDocument[blobs.length];
        for (int i = 0; i < blobs.length; i++) {
            trs[i] = new TransformDocumentImpl(blobs[i]);
        }
        return transform(options, trs);
    }

    public List<TransformDocument> transform(
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources) {

        SimpleTimer timer = new SimpleTimer();
        timer.start();

        List<TransformDocument> results = new ArrayList<TransformDocument>();
        TransformDocument[] pending = sources;
        for (Plugin plugin : getPluginChains()) {

            // :FIXME: Highly inefficient because of mimetype sniff on large
            // blobs.
            // Filter out before forwarding to the plugin
            // pending = filterSourcesFor(pending, plugin);
            // log.debug(timer.mark("Filtering transformation sources."));

            // Get options specified by the transformer for this plugin and
            // merge them with the one from the transformer. And then the
            // options given as direct parameter in this method will override
            // the whole merged options.
            Map<String, Serializable> mergedOptions = mergeOptionsFor(plugin,
                    options != null ? options.get(plugin.getName()) : null);

            log.debug("launching transformation : " + plugin.getName());
            try {
                results = plugin.transform(mergedOptions, pending);
                pending = results.toArray(pending);
            } catch (Exception e) {
                log.error("An error occured while trying to perform "
                        + "a transformation chain.", e);
            } finally {
                log.debug(timer.mark("Plugin transformation terminated, name="
                        + plugin.getName()));
            }
        }

        timer.stop();
        log.debug("Global transformation chain terminated for transformer name="
                + name + timer);

        return results;
    }

    /**
     * Filters input transform document sources for a given plugin.
     * <p>
     * Note the discrimination is done plugin instance side.
     *
     * @param sources transform documents instances.
     * @param plugin a Nuxeo Transform Plugin instance.
     * @return a filtered list of transform documents.
     */
    protected static TransformDocument[] filterSourcesFor(TransformDocument[] sources,
            Plugin plugin) {
        List<TransformDocument> filtered = new ArrayList<TransformDocument>();
        for (TransformDocument source : sources) {
            if (plugin.isSourceCandidate(source)) {
                filtered.add(source);
            }
        }
        return filtered.toArray(new TransformDocument[filtered.size()]);
    }

    /**
     * Filters input blobs sources for a given plugin.
     * <p>
     * Note the discrimination is done plugin instance side.
     *
     * @param blobs Nuxeo Core blob instances.
     * @param plugin a Nuxeo Transform Plugin instance.
     * @return a filtered list of blobs.
     */
    protected static Blob[] filterSourcesFor(Blob[] blobs, Plugin plugin) {
        List<Blob> filtered = new ArrayList<Blob>();
        for (Blob blob : blobs) {
            if (plugin.isSourceCandidate(blob)) {
                filtered.add(blob);
            }
        }
        return filtered.toArray(new Blob[filtered.size()]);
    }

    /**
     * Merges options from a given plugin with the ones the transformer defines
     * for this as overridden purpose plugin.
     *
     * @param plugin a Nuxeo Transform plugin
     * @return a map from String to Serializable.
     */
    protected Map<String, Serializable> mergeOptionsFor(Plugin plugin,
            Map<String, Serializable> options) {

        // Get plugin options
        Map<String, Serializable> mergedOptions = plugin.getDefaultOptions();

        // Override with transformer ones.
        if (defaultOptions != null
                && defaultOptions.get(plugin.getName()) != null) {
            mergedOptions.putAll(defaultOptions.get(plugin.getName()));
        }

        // Then override with direct options given as parameters.
        if (options != null) {
            mergedOptions.putAll(options);
        }

        return mergedOptions;
    }

}
