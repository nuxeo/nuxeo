/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.webengine.ResourceBinding;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Module extends MessagesProvider {

    String getName();

    File getRoot();

    Resource getRootObject(WebContext ctx);

    WebEngine getEngine();

    void flushCache();

    Module getSuperModule();

    String getTemplateFileExt();

    String getMediaTypeId(MediaType mt);

    @Override
    Map<String, String> getMessages(String language);

    Messages getMessages();

    List<ResourceBinding> getResourceBindings();

    /**
     * Gets a file using the configured directory stack. Each directory in the stack is asked for the file until a file
     * is found. If no file is found, returns null.
     * <p>
     * Note that the implementation may cache the results. To clear any cached data, you should call the
     * {@link #flushCache()} method.
     *
     * @param path the file path
     * @return null if no file found otherwise the file
     */
    ScriptFile getFile(String path);

    /**
     * Gets a skin resource input stream. This must not cache resources. This method is using the module stacking
     * directory to find the resource.
     */
    ScriptFile getSkinResource(String path) throws IOException;

    /**
     * Loads a class given its name.
     * <p>
     * The scripting class loader will be used to load the class.
     *
     * @param className the class name
     * @return the class instance
     */
    Class<?> loadClass(String className) throws ClassNotFoundException;

    /**
     * Gets a {@link ResourceType} instance given its name.
     * <p>
     * The web type lookup is performed in the following order:
     * <ol>
     * <li>First the annotated Groovy classes are checked. (web/ directory)
     * <li>Then the configuration type registry corresponding
     * </ol>
     *
     * @param typeName the type name
     * @return the web type instance
     * @throws WebResourceNotFoundException if no such web type was defined
     */
    ResourceType getType(String typeName);

    /**
     * Gets the types registered within this module.
     *
     * @return the types. Cannot be null.
     */
    ResourceType[] getTypes();

    /**
     * Gets the adapters registered within this module.
     *
     * @return the adapters. Cannot be null.
     */
    AdapterType[] getAdapters();

    /**
     * Gets the named adapter definition for the given resource.
     *
     * @param ctx the target resource
     * @param name the adapter name
     * @return the adapter if any adapter with that name applies for that resource otherwise throws an exception
     * @throws WebSecurityException if the adapter exists but cannot be accessed in the context of that resource
     * @throws WebResourceNotFoundException if no such adapter exists for that resource
     */
    AdapterType getAdapter(Resource ctx, String name);

    /**
     * Gets the list of adapters that applies to the given resource.
     *
     * @param ctx the context resource
     * @return the list of adapters Cannot be null.
     */
    List<AdapterType> getAdapters(Resource ctx);

    /**
     * Gets the list of adapter names that applies to the given resource.
     *
     * @param ctx the context resource
     * @return the list of adapters Cannot be null.
     */
    List<String> getAdapterNames(Resource ctx);

    /**
     * Gets the list of adapters that are enabled for the given context.
     * <p>
     * Enabled adapters are those adapters which can be accessed in the current security context.
     *
     * @param ctx the context resource
     * @return the list of adapter.s Cannot be null.
     */
    List<AdapterType> getEnabledAdapters(Resource ctx);

    /**
     * Gets the list of adapter names that are enabled for the given context.
     * <p>
     * Enabled services are those adapters which can be accessed in the current security context.
     *
     * @param ctx the context resource
     * @return the list of adapters. Cannot be null.
     */
    List<String> getEnabledAdapterNames(Resource ctx);

    List<LinkDescriptor> getLinks(String category);

    List<LinkDescriptor> getActiveLinks(Resource context, String category);

    /**
     * Get the path prefix to be used from templates to prepend to links to static resources.
     * <p>
     * This prefix is exposed to templates as ${skinPath}.
     *
     * @return the skin path prefix. never null.
     */
    String getSkinPathPrefix();

    boolean isDerivedFrom(String moduleName);

}
