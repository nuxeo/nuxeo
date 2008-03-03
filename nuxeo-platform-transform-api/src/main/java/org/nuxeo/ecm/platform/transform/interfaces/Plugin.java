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
 * $Id: Plugin.java 19121 2007-05-22 11:53:22Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.interfaces;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;

/**
 * TransformServiceCommon plugin default interface.
 * <p>
 * Responsible to convert given sources to a given format using default and
 * specific options.
 * <p>
 * Plugin instance will be stored as a POJO object. They don't need to be EJBs
 * since the actual transformation will be delegated to another transformation
 * server (cf. Adobe Server or JOOoConverter). Furthermore, if a given plugin
 * requires some heavy computing, then the actual heavy computing can be defined
 * within a separated EJB used by the POJO plugin. (cf. Adobe plugins for
 * instance). It greatly simplifies the TransformServiceCommon architecture.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface Plugin extends Serializable {

    /**
     * Returns the default options.
     *
     * @return a map holding the default plugin options.
     */
    Map<String, Serializable> getDefaultOptions();

    /**
     * Gets the destination mimetype.
     *
     * @return a string holding the destination mimetype
     */
    String getDestinationMimeType();

    /**
     * Gets the plugin name.
     *
     * @return a string holding the plugin name
     */
    String getName();

    /**
     * Gets mimetypes for source.
     * <p>
     * The source mimetypes are all the formats this plugin can deal with.
     *
     * @return list of string holding each mimetype.
     */
    List<String> getSourceMimeTypes();

    /**
     * Sets default options.
     *
     * @param defaultOptions
     *            a map of string to objects holding the default plugin options.
     */
    void setDefaultOptions(Map<String, Serializable> defaultOptions);

    /**
     * Sets plugin name.
     *
     * @param name
     *            a string holding the name
     */
    void setName(String name);

    /**
     * Sets source mimetypes.
     * <p>
     * The source mimetypes are all the formats this plugin can deal with.
     *
     * @param sourceMimeTypes
     *            a list of strings representing each mimetype
     */
    void setSourceMimeTypes(List<String> sourceMimeTypes);

    /**
     * Sets the destination mimetype.
     * <p>
     * The destination mimetype is the format the result of the plugin
     * transformation.
     *
     * @param destinationMimeType
     *            a string holding the destination mimetype
     */
    void setDestinationMimeType(String destinationMimeType);

    /**
     * Sets specific options.
     * <p>
     * Override default plugin options.
     *
     * @param options
     *            a map from string to serializable.
     */
    void setSpecificOptions(Map<String, Serializable> options);

    /**
     * Transforms sources given specific plugin options.
     *
     * @param options
     *            plugin options
     * @param sources
     *            list of sources as TransformDocument instances
     * @return list of TransformDocument instances
     */
    List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception;

    /**
     * Transforms sources given specific plugin options.
     *
     * @param options
     *            plugin options
     * @param blobs
     *            list of sources as streaming blob instance.
     * @return list of TransformDocument instances
     */
    List<TransformDocument> transform(Map<String, Serializable> options,
            Blob... blobs) throws Exception;

    /**
     * Is a given transform document a candidate for this plugin.
     *
     * @param doc :
     *            a transform document instance.
     * @return true if candidate / false if not.
     */
    boolean isSourceCandidate(TransformDocument doc);

    /**
     * Is a given streaming blob instance a candidate for this plugin.
     *
     * @param blob :
     *            a streaming blob instance
     * @return true if candidate / false if not.
     */
    boolean isSourceCandidate(Blob blob);

}
