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
 * $Id: Transformer.java 2310 2006-08-29 16:41:27Z janguenot $
 */

package org.nuxeo.ecm.platform.transform.interfaces;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

/**
 * TransformServiceCommon default transformer interface.
 * <p>
 * A transformer take given sources and transform them using default and
 * options. The transformation is performed by underlying plugins defined in a
 * plugins chain.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface Transformer extends Serializable {

    /**
     * Returns the default transformer options.
     * <p>
     * The map keys are plugin names. The map values are maps which have string
     * as keys and serializable objects as values.
     *
     * @return hashmap holding the default configuration
     */
    Map<String, Map<String, Serializable>> getDefaultOptions();

    /**
     * Returns the mimetype destination this transformer will return as a
     * result.
     * <p>
     * Information taken from the last plugin defined in the plugins chain.
     *
     * @return string holding the destination mimetype.
     */
    String getMimeTypeDestination();

    /**
     * Returns the mimetype source types this transformer is expecting.
     * <p>
     * Information taken from the first plugin defined in the plugins chain.
     *
     * @return list of string representing the mimetypes.
     */
    List<String> getMimeTypeSources();

    /**
     * Returns the transformer name.
     *
     * @return string holding the name
     */
    String getName();

    /**
     * Returns the list of plugins chain this transformer will use.
     * <p>
     * The plugins are registered in the order the transformer should call them
     * to get the expected result.
     *
     * @return list of plugins
     */
    List<Plugin> getPluginChains();

    /**
     * Sets plugin default options.
     * <p>
     * The map keys are plugin names. The map values are maps which
     * have string as keys and serializable objects as values.
     *
     * @param defaultOptions map holding the default configuration
     */
    void setDefaultOptions(Map<String, Map<String, Serializable>> defaultOptions);

    /**
     * Sets the plugin chains given plugin names.
     *
     * @param pluginsChain
     *            list of strings representing plugin names
     */
    void setPluginChains(List<String> pluginsChain);

    /**
     * Sets the transformer name.
     *
     * @param name
     *            string containing the tranformer's name
     */
    void setName(String name);

    /**
     * Transforms given sources and plugin options.
     *
     * @param options
     *            plugin options (the keys are the plugin names)
     * @param sources
     *            list of sources as TransformDocument instances
     * @return list of TransformDocument instances.
     */
    List<TransformDocument> transform(
            Map<String, Map<String, Serializable>> options,
            TransformDocument... sources);

    /**
     * Transforms given sources and plugin options.
     *
     * @param options
     *            plugin options (the keys are the plugin names)
     * @param blobs
     *            list of sources as StreamingBlob instances.
     * @return list of TransformDocument instances.
     */
    List<TransformDocument> transform(
            Map<String, Map<String, Serializable>> options,
            Blob... blobs);

}
