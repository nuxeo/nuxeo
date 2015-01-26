/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry;

import java.lang.reflect.Type;
import java.util.Collection;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Instantiations;

/**
 * Service to get specialize marshaller.
 * <p>
 * This service provides an extension point to register/deregister marshallers.
 *
 * <pre>
 * {@code
 * <extension target="org.nuxeo.ecm.core.io.MarshallerRegistry" point="marshallers">
 *   <register class="org.nuxeo.ecm.core.io.marshallers.json.validation.ConstraintWriter" enable="true" />
 *   <register class="org.nuxeo.ecm.core.io.marshallers.json.validation.DocumentValidationReportWriter" enable="true" />
 * </extension>
 * }
 * </pre>
 *
 * </p>
 * <p>
 * You can also register/deregister your marshaller programmatically by calling {@link #register(Class)} and
 * {@link #deregister(Class)}.
 * </p>
 * <p>
 * All marshallers are provided with injected properties.
 * </p>
 * <p>
 * You can get a {@link Writer} using:
 * <ul>
 * <li>{@link #getWriter(RenderingContext, Class, MediaType)} A {@link Writer} which manage the given class and mimetype
 * </li>
 * <li>{@link #getWriter(RenderingContext, Class, Type, MediaType)} A {@link Writer} which manage the given class and
 * mimetype, plus the checks the given generic type.</li>
 * <li>{@link #getAllWriters(RenderingContext, Class, Type, MediaType)} All {@link Writer} which manage the given class
 * and mimetype, plus the checks the given generic type.</li>
 * <li>{@link #getInstance(RenderingContext, Class)} An instance of the given {@link Writer} class.</li>
 * </ul>
 * </p>
 * <p>
 * You can get a {@link Reader} using:
 * <ul>
 * <li>{@link #getReader(RenderingContext, Class, MediaType)} A {@link Reader} which manage the given class and mimetype
 * </li>
 * <li>{@link #getReader(RenderingContext, Class, Type, MediaType)} A {@link Reader} which manage the given class and
 * mimetype, plus the checks the given generic type.</li>
 * <li>{@link #getAllReaders(RenderingContext, Class, Type, MediaType)} All {@link Reader} which manage the given class
 * and mimetype, plus the checks the given generic type.</li>
 * <li>{@link #getInstance(RenderingContext, Class)} An instance of the given {@link Reader} class.</li>
 * </ul>
 * </p>
 * <p>
 * If several marshaller matches a demand of the single instance, the registry use the following rules to choose one:
 * <ul>
 * <li>The marshaller with the greatest priority is choosen.</li>
 * <li>Then, Less instance is better: {@link Instantiations#SINGLETON} are preferred to
 * {@link Instantiations#PER_THREAD} to {@link Instantiations#EACH_TIME}</li>
 * <li>Then, Expert is better: A marshaller which manage a subclass is prefered.</li>
 * <li>Then, references works: A subclass of an existing marshaller is not choosen. You have to specify an higher
 * priority.</li>
 * </ul>
 *
 * @since 7.2
 */
public interface MarshallerRegistry {

    /**
     * Be careful !!! That's deregister all marshallers.
     *
     * @since 7.2
     */
    public void clear();

    /**
     * Makes a marshaller class available.
     *
     * @param marshaller The marshaller class.
     * @throws MarshallingException If marshaller class is null or if it's not a valid marshaller.
     * @since 7.2
     */
    public void register(Class<?> marshaller) throws MarshallingException;

    /**
     * Remove a marshaller from the registry.
     *
     * @param marshaller The marshaller class.
     * @throws MarshallingException If marshaller class is null or if it's not a valid marshaller.
     * @since 7.2
     */
    public void deregister(Class<?> marshaller) throws MarshallingException;

    /**
     * Provides a {@link Writer} instance to manage marshalling of the given Java Type and mimetype.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshalledClazz The java type to manage.
     * @param genericType The generic Java type to manage.
     * @param mediatype The expected mimetype.
     * @return A valid {@link Writer} instance.
     * @since 7.2
     */
    public <T> Writer<T> getWriter(RenderingContext ctx, Class<T> marshalledClazz, Type genericType, MediaType mediatype);

    /**
     * Provides a {@link Writer} instance to manage marshalling of the given Java Type and mimetype. It creates a new
     * instance even for {@link Instantiations#SINGLETON} marshallers.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshalledClazz The java type to manage.
     * @param genericType The generic Java type to manage.
     * @param mediatype The expected mimetype.
     * @return A valid {@link Writer} instance.
     * @since 7.2
     */
    public <T> Writer<T> getUniqueWriter(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype);

    /**
     * Provides all {@link Writer} instance that manage marshalling of the given Java Type and mimetype.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshalledClazz The java type to manage.
     * @param genericType The generic Java type to manage.
     * @param mediatype The expected mimetype.
     * @return A list of valid {@link Writer} instance.
     * @since 7.2
     */
    public <T> Collection<Writer<T>> getAllWriters(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype);

    /**
     * see {@link #getWriter(RenderingContext, Class, Type, MediaType)}
     */
    public <T> Writer<T> getWriter(RenderingContext ctx, Class<T> marshalledClazz, MediaType mediatype);

    /**
     * Provides a {@link Reader} instance to manage marshalling of a mimetype in a Java Type.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshalledClazz The java type to manage.
     * @param genericType The generic Java type to manage.
     * @param mediatype The expected mimetype.
     * @return A valid {@link Reader} instance.
     * @since 7.2
     */
    public <T> Reader<T> getReader(RenderingContext ctx, Class<T> marshalledClazz, Type genericType, MediaType mediatype);

    /**
     * Provides a {@link Reader} instance to manage marshalling of a mimetype in a Java Type. It creates a new instance
     * even for {@link Instantiations#SINGLETON} marshallers.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshalledClazz The java type to manage.
     * @param genericType The generic Java type to manage.
     * @param mediatype The expected mimetype.
     * @return A valid {@link Reader} instance.
     * @since 7.2
     */
    public <T> Reader<T> getUniqueReader(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype);

    /**
     * Provides all {@link Reader} instance that manage marshalling of a mimetype in a Java Type.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshalledClazz The java type to manage.
     * @param genericType The generic Java type to manage.
     * @param mediatype The expected mimetype.
     * @return A list of valid {@link Reader} instance.
     * @since 7.2
     */
    public <T> Collection<Reader<T>> getAllReaders(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype);

    /**
     * see {@link #getReader(RenderingContext, Class, Type, MediaType)}
     */
    public <T> Reader<T> getReader(RenderingContext ctx, Class<T> marshalledClazz, MediaType mediatype);

    /**
     * Provides an instance of a given marshaller class.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshallerClass A valid marshaller instance.
     * @return A valid marshaller instance.
     * @since 7.2
     */
    public <T> T getInstance(RenderingContext ctx, Class<T> marshallerClass);

    /**
     * Provides an instance of the given marshaller class. It creates a new instance even for
     * {@link Instantiations#SINGLETON} marshallers.
     *
     * @param ctx The marshalling context (see {@link RenderingContext}).
     * @param marshallerClass A valid marshaller instance.
     * @return A valid marshaller instance.
     * @since 7.2
     */
    public <T> T getUniqueInstance(RenderingContext ctx, Class<T> marshallerClass);

}
