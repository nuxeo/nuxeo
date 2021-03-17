/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.registry;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.MarshallerInspector;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Implementation of {@link MarshallerRegistry}.
 * <p>
 * This implementation is based on {@link MarshallerInspector} class which is able to create marshaller instance and
 * inject properties. This class also manage marshaller's priorities.
 * </p>
 *
 * @since 7.2
 */
public class MarshallerRegistryImpl extends DefaultComponent implements MarshallerRegistry {

    private static final Logger log = LogManager.getLogger(MarshallerRegistryImpl.class);

    /**
     * @since 10.3
     */
    public static final String XP_MARSHALLERS = "marshallers";

    /**
     * All {@link Writer}'s {@link MarshallerInspector} ordered by their priority.
     */
    private static final Set<MarshallerInspector> writers = new ConcurrentSkipListSet<>();

    /**
     * {@link Writer}'s {@link MarshallerInspector} organized by their managed {@link MediaType}.
     */
    private static final Map<MediaType, Set<MarshallerInspector>> writersByMediaType = new ConcurrentHashMap<>();

    /**
     * All {@link Reader}'s {@link MarshallerInspector} ordered by their priority.
     */
    private static final Set<MarshallerInspector> readers = new ConcurrentSkipListSet<>();

    /**
     * {@link Reader}'s {@link MarshallerInspector} organized by their managed {@link MediaType}.
     */
    private static final Map<MediaType, Set<MarshallerInspector>> readersByMediaType = new ConcurrentHashMap<>();

    /**
     * {@link MarshallerInspector} organized by their managed {@link Marshaller} class.
     */
    private static final Map<Class<?>, MarshallerInspector> marshallersByType = new ConcurrentHashMap<>();

    @Override
    public void deactivate(ComponentContext context) {
        clear();
        super.deactivate(context);
    }

    @Override
    protected boolean register(String xp, Descriptor descriptor) {
        boolean registered = super.register(xp, descriptor);
        if (registered) {
            MarshallerRegistryDescriptor mrd = getDescriptor(xp, descriptor.getId());
            if (mrd.enable) {
                register(mrd.klass);
            } else {
                deregister(mrd.klass);
            }
        }
        return registered;
    }

    @Override
    protected boolean unregister(String xp, Descriptor descriptor) {
        MarshallerRegistryDescriptor mrd = getDescriptor(xp, descriptor.getId());
        if (mrd == null) {
            return false;
        }
        boolean unregistered = super.unregister(xp, descriptor);
        if (unregistered) {
            if (mrd.enable) {
                deregister(mrd.klass);
            }
        }
        return unregistered;
    }

    @Override
    public void register(Class<?> marshaller) {
        if (marshaller == null) {
            throw new MarshallingException("Cannot register null marshaller");
        }
        MarshallerInspector inspector = new MarshallerInspector(marshaller);
        if (!inspector.isWriter() && !inspector.isReader()) {
            throw new MarshallingException(
                    "The marshaller registry just supports Writer and Reader for now. You have to implement "
                            + Writer.class.getName() + " or " + Reader.class.getName());
        }
        if (marshallersByType.get(marshaller) != null) {
            log.warn("The marshaller {} is already registered.", marshaller.getName());
            return;
        } else {
            marshallersByType.put(marshaller, inspector);
        }
        if (inspector.isWriter()) {
            writers.add(inspector);
            for (MediaType mediaType : inspector.getSupports()) {
                Set<MarshallerInspector> inspectors = writersByMediaType.get(mediaType);
                if (inspectors == null) {
                    inspectors = new ConcurrentSkipListSet<>();
                    writersByMediaType.put(mediaType, inspectors);
                }
                inspectors.add(inspector);
            }
        }
        if (inspector.isReader()) {
            readers.add(inspector);
            for (MediaType mediaType : inspector.getSupports()) {
                Set<MarshallerInspector> inspectors = readersByMediaType.get(mediaType);
                if (inspectors == null) {
                    inspectors = new ConcurrentSkipListSet<>();
                    readersByMediaType.put(mediaType, inspectors);
                }
                inspectors.add(inspector);
            }
        }
    }

    @Override
    public void deregister(Class<?> marshaller) {
        if (marshaller == null) {
            log.warn("Cannot deregister null marshaller");
            return;
        }
        MarshallerInspector inspector = new MarshallerInspector(marshaller);
        if (!inspector.isWriter() && !inspector.isReader()) {
            throw new MarshallingException(
                    "The marshaller registry just supports Writer and Reader for now. You have to implement "
                            + Writer.class.getName() + " or " + Reader.class.getName());
        }
        marshallersByType.remove(marshaller);
        if (inspector.isWriter()) {
            writers.remove(inspector);
            for (MediaType mediaType : inspector.getSupports()) {
                Set<MarshallerInspector> inspectors = writersByMediaType.get(mediaType);
                if (inspectors != null) {
                    inspectors.remove(inspector);
                }
            }
        }
        if (inspector.isReader()) {
            readers.remove(inspector);
            for (MediaType mediaType : inspector.getSupports()) {
                Set<MarshallerInspector> inspectors = readersByMediaType.get(mediaType);
                if (inspectors != null) {
                    inspectors.remove(inspector);
                }
            }
        }
    }

    @Override
    public <T> Writer<T> getWriter(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype) {
        Set<MarshallerInspector> candidates = writersByMediaType.get(mediatype);
        return (Writer<T>) getMarshaller(ctx, marshalledClazz, genericType, mediatype, candidates, writers, false);
    }

    @Override
    public <T> Writer<T> getUniqueWriter(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype) {
        Set<MarshallerInspector> candidates = writersByMediaType.get(mediatype);
        return (Writer<T>) getMarshaller(ctx, marshalledClazz, genericType, mediatype, candidates, writers, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<Writer<T>> getAllWriters(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype) {
        Set<MarshallerInspector> candidates = writersByMediaType.get(mediatype);
        Collection<Marshaller<T>> founds = getAllMarshallers(ctx, marshalledClazz, genericType, mediatype, candidates,
                writers);
        return (Collection<Writer<T>>) (Collection<?>) founds;
    }

    @Override
    public <T> Writer<T> getWriter(RenderingContext ctx, Class<T> marshalledClazz, MediaType mediatype) {
        return getWriter(ctx, marshalledClazz, marshalledClazz, mediatype);
    }

    @Override
    public <T> Reader<T> getReader(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype) {
        Set<MarshallerInspector> candidates = readersByMediaType.get(mediatype);
        return (Reader<T>) getMarshaller(ctx, marshalledClazz, genericType, mediatype, candidates, readers, false);
    }

    @Override
    public <T> Reader<T> getUniqueReader(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype) {
        Set<MarshallerInspector> candidates = readersByMediaType.get(mediatype);
        return (Reader<T>) getMarshaller(ctx, marshalledClazz, genericType, mediatype, candidates, readers, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Collection<Reader<T>> getAllReaders(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype) {
        Set<MarshallerInspector> candidates = readersByMediaType.get(mediatype);
        Collection<Marshaller<T>> founds = getAllMarshallers(ctx, marshalledClazz, genericType, mediatype, candidates,
                readers);
        return (Collection<Reader<T>>) (Collection<?>) founds;
    }

    @Override
    public <T> Reader<T> getReader(RenderingContext ctx, Class<T> marshalledClazz, MediaType mediatype) {
        return getReader(ctx, marshalledClazz, marshalledClazz, mediatype);
    }

    public <T> Marshaller<T> getMarshaller(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype, Set<MarshallerInspector> customs, Set<MarshallerInspector> wildcards,
            boolean forceInstantiation) {
        if (customs != null) {
            Marshaller<T> found = searchCandidate(ctx, marshalledClazz, genericType, mediatype, customs,
                    forceInstantiation);
            if (found != null) {
                return found;
            }
        }
        return searchCandidate(ctx, marshalledClazz, genericType, mediatype, wildcards, forceInstantiation);
    }

    /**
     * Returns the marshallers, in fact always enrichers, in their reverse priority order.
     * <p>
     * The {@code customs} and {@code wildcards} sets come sorted with higher priorities first, e.g.: [3000, 2000], see
     * {@link MarshallerInspector#compareTo(MarshallerInspector)}. The enrichers with higher priority need to be
     * processed last, e.g.: [2000, 3000], to make sure they override any enricher with the same name and lower
     * priority, see ExtensibleEntityJsonWriter#write(EntityType entity, JsonGenerator jg).
     */
    public <T> Collection<Marshaller<T>> getAllMarshallers(RenderingContext ctx, Class<T> marshalledClazz,
            Type genericType, MediaType mediatype, Set<MarshallerInspector> customs,
            Set<MarshallerInspector> wildcards) {
        // keep the marshallers (in fact enrichers) sorted to respect their priority
        Map<MarshallerInspector, Marshaller<T>> result = new LinkedHashMap<>();
        if (customs != null) {
            result.putAll(searchAllCandidates(ctx, marshalledClazz, genericType, mediatype, customs));
        }
        result.putAll(searchAllCandidates(ctx, marshalledClazz, genericType, mediatype, wildcards));
        List<Marshaller<T>> resultList = new ArrayList<>(result.values());
        Collections.reverse(resultList);
        return resultList;
    }

    @SuppressWarnings("unchecked")
    private <T> Marshaller<T> searchCandidate(RenderingContext ctx, Class<T> marshalledClazz, Type genericType,
            MediaType mediatype, Set<MarshallerInspector> candidates, boolean forceInstantiation) {
        for (MarshallerInspector inspector : candidates) {
            // checks the managed class is compatible
            if (inspector.getMarshalledType().isAssignableFrom(marshalledClazz)) {
                // checks the generic type is compatible
                if (genericType == null || marshalledClazz.equals(inspector.getGenericType())
                        || TypeUtils.isAssignable(genericType, inspector.getGenericType())) {
                    Marshaller<T> marshaller = null;
                    if (forceInstantiation) {
                        marshaller = (Marshaller<T>) inspector.getNewInstance(ctx, false);
                    } else {
                        marshaller = inspector.getInstance(ctx);
                    }
                    // checks the marshaller accepts the request
                    if (marshaller.accept(marshalledClazz, genericType, mediatype)) {
                        return marshaller;
                    }
                }
            }
        }
        return null;
    }

    private <T> Map<MarshallerInspector, Marshaller<T>> searchAllCandidates(RenderingContext ctx,
            Class<T> marshalledClazz, Type genericType, MediaType mediatype, Set<MarshallerInspector> candidates) {
        // keep the marshallers (in fact enrichers) sorted to respect their priority
        Map<MarshallerInspector, Marshaller<T>> result = new LinkedHashMap<>();
        for (MarshallerInspector inspector : candidates) {
            // checks the managed class is compatible
            if (inspector.getMarshalledType().isAssignableFrom(marshalledClazz)) {
                // checks the generic type is compatible
                if (genericType == null || marshalledClazz.equals(inspector.getGenericType())
                        || TypeUtils.isAssignable(genericType, inspector.getGenericType())) {
                    // checks the marshaller accepts the request
                    Marshaller<T> marshaller = inspector.getInstance(ctx);
                    if (marshaller.accept(marshalledClazz, genericType, mediatype)) {
                        result.put(inspector, marshaller);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public <T> T getInstance(RenderingContext ctx, Class<T> marshallerClass) {
        MarshallerInspector inspector = marshallersByType.get(marshallerClass);
        if (inspector == null) {
            inspector = new MarshallerInspector(marshallerClass);
        }
        return inspector.getInstance(ctx);
    }

    @Override
    public <T> T getUniqueInstance(RenderingContext ctx, Class<T> marshallerClass) {
        MarshallerInspector inspector = marshallersByType.get(marshallerClass);
        if (inspector == null) {
            inspector = new MarshallerInspector(marshallerClass);
        }
        @SuppressWarnings("unchecked")
        T result = (T) inspector.getNewInstance(ctx, false);
        return result;
    }

    @Override
    public void clear() {
        marshallersByType.clear();
        writersByMediaType.clear();
        readersByMediaType.clear();
        writers.clear();
        readers.clear();
    }

}
