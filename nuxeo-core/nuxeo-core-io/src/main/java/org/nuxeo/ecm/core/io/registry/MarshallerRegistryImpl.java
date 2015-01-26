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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.MarshallerInspector;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

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

    private final Log log = LogFactory.getLog(MarshallerRegistryImpl.class);

    /**
     * All {@link Writer}'s {@link MarshallerInspector} ordered by their priority.
     */
    private static final Set<MarshallerInspector> writers = new ConcurrentSkipListSet<MarshallerInspector>();

    /**
     * {@link Writer}'s {@link MarshallerInspector} organized by their managed {@link MediaType}.
     */
    private static final Map<MediaType, Set<MarshallerInspector>> writersByMediaType = new ConcurrentHashMap<MediaType, Set<MarshallerInspector>>();

    /**
     * All {@link Reader}'s {@link MarshallerInspector} ordered by their priority.
     */
    private static final Set<MarshallerInspector> readers = new ConcurrentSkipListSet<MarshallerInspector>();

    /**
     * {@link Reader}'s {@link MarshallerInspector} organized by their managed {@link MediaType}.
     */
    private static final Map<MediaType, Set<MarshallerInspector>> readersByMediaType = new ConcurrentHashMap<MediaType, Set<MarshallerInspector>>();

    /**
     * {@link MarshallerInspector} organized by their managed {@link Marshaller} class.
     */
    private static final Map<Class<?>, MarshallerInspector> marshallersByType = new ConcurrentHashMap<Class<?>, MarshallerInspector>();

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        clear();
    }

    @Override
    public void deactivate(ComponentContext context) {
        clear();
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("marshallers")) {
            MarshallerRegistryDescriptor mrd = (MarshallerRegistryDescriptor) contribution;
            if (mrd.isEnable()) {
                register(mrd.getClazz());
            } else {
                deregister(mrd.getClazz());
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("marshallers")) {
            MarshallerRegistryDescriptor mrd = (MarshallerRegistryDescriptor) contribution;
            if (mrd.isEnable()) {
                deregister(mrd.getClazz());
            } else {
                register(mrd.getClazz());
            }
        }
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
            log.warn("The marshaller " + marshaller.getName() + " is already registered.");
            return;
        } else {
            marshallersByType.put(marshaller, inspector);
        }
        if (inspector.isWriter()) {
            writers.add(inspector);
            for (MediaType mediaType : inspector.getSupports()) {
                Set<MarshallerInspector> inspectors = writersByMediaType.get(mediaType);
                if (inspectors == null) {
                    inspectors = new ConcurrentSkipListSet<MarshallerInspector>();
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
                    inspectors = new ConcurrentSkipListSet<MarshallerInspector>();
                    readersByMediaType.put(mediaType, inspectors);
                }
                inspectors.add(inspector);
            }
        }
    }

    @Override
    public void deregister(Class<?> marshaller) throws MarshallingException {
        if (marshaller == null) {
            log.warn("Cannot register null marshaller");
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
    public <T> Writer<T> getWriter(RenderingContext ctx, Class<T> marshalledClazz, Type genericType, MediaType mediatype) {
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
    public <T> Reader<T> getReader(RenderingContext ctx, Class<T> marshalledClazz, Type genericType, MediaType mediatype) {
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

    public <T> Collection<Marshaller<T>> getAllMarshallers(RenderingContext ctx, Class<T> marshalledClazz,
            Type genericType, MediaType mediatype, Set<MarshallerInspector> customs, Set<MarshallerInspector> wildcards) {
        Map<MarshallerInspector, Marshaller<T>> result = new HashMap<MarshallerInspector, Marshaller<T>>();
        if (customs != null) {
            result.putAll(searchAllCandidates(ctx, marshalledClazz, genericType, mediatype, customs));
        }
        result.putAll(searchAllCandidates(ctx, marshalledClazz, genericType, mediatype, wildcards));
        return result.values();
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
        Map<MarshallerInspector, Marshaller<T>> result = new HashMap<MarshallerInspector, Marshaller<T>>();
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
        writers.clear();
        readersByMediaType.clear();
        readers.clear();
    }

}
