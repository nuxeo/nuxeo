/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 */
package org.nuxeo.ecm.core.uidgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service that writes MetaData.
 */
public class UIDGeneratorComponent extends DefaultComponent implements UIDGeneratorService {

    private static final Logger log = LogManager.getLogger(UIDGeneratorComponent.class);

    public static final String ID = "org.nuxeo.ecm.core.uidgen.UIDGeneratorService";

    public static final String UID_GENERATORS_EXTENSION_POINT = "generators";

    public static final String SEQUENCERS_EXTENSION_POINT = "sequencers";

    protected final Map<String, UIDGenerator> generators = new HashMap<>();

    protected final Map<String, UIDSequencer> sequencers = new HashMap<>();

    protected String defaultSequencer;

    @Override
    public void start(ComponentContext context) {
        initGenerators();
        initSequencers();
    }

    protected void initGenerators() {
        List<UIDGeneratorDescriptor> contribs = getRegistryContributions(UID_GENERATORS_EXTENSION_POINT);
        for (UIDGeneratorDescriptor generatorDescriptor : contribs) {
            final String generatorName = generatorDescriptor.getName();
            UIDGenerator generator;
            try {
                generator = generatorDescriptor.getGenerator();
            } catch (ReflectiveOperationException e) {
                String msg = String.format("Failed to create UIDGenerator with name '%s': %s", name, e.getMessage());
                log.error(msg, e);
                addRuntimeMessage(Level.ERROR, msg);
                continue;
            }
            final String[] propNames = generatorDescriptor.getPropertyNames();
            if (propNames.length == 0) {
                String msg = String.format("No property name defined on generator '%s'", generatorName);
                log.error(msg);
                addRuntimeMessage(Level.ERROR, msg);
                continue;
            }
            generator.setPropertyNames(propNames);
            final String[] docTypes = generatorDescriptor.getDocTypes();
            for (String docType : docTypes) {
                final UIDGenerator previous = generators.put(docType, generator);
                if (previous != null) {
                    log.info("Overwriting generator: {} for docType: {}", previous.getClass(), docType);
                }
                log.info("Registered generator: {}  for docType: {}", generator.getClass(), docType);
            }
        }
    }

    protected void initSequencers() {
        List<UIDSequencerProviderDescriptor> seqs = getRegistryContributions(SEQUENCERS_EXTENSION_POINT);
        String def = null;
        String last = null;
        for (UIDSequencerProviderDescriptor contrib : seqs) {
            String name = contrib.getName();
            UIDSequencer seq;
            try {
                seq = contrib.getSequencer();
            } catch (ReflectiveOperationException e) {
                String msg = String.format("Failed to create UIDSequencer with name '%s': %s", name, e.getMessage());
                log.error(msg, e);
                addRuntimeMessage(Level.ERROR, msg);
                continue;
            }
            seq.setName(name);
            seq.init();
            sequencers.put(name, seq);
            if (contrib.isDefault()) {
                def = name;
            }
            last = name;
        }
        if (def == null) {
            def = last;
        }
        defaultSequencer = def;
    }

    @Override
    public void stop(ComponentContext context) {
        generators.clear();
        sequencers.values().forEach(UIDSequencer::dispose);
        sequencers.clear();
        defaultSequencer = null;
    }

    /**
     * Returns the uid generator to use for this document.
     * <p>
     * Choice is made following the document type and the generator configuration.
     */
    @Override
    public UIDGenerator getUIDGeneratorFor(DocumentModel doc) {
        final String docTypeName = doc.getType();
        final UIDGenerator generator = generators.get(docTypeName);

        if (generator == null) {
            log.debug("No UID Generator defined for doc type: {}", docTypeName);
            return null;
        }

        // TODO maybe maintain an initialization state for generators
        // so the next call could be avoided (for each request)
        generator.setSequencer(Framework.getService(UIDSequencer.class));

        return generator;
    }

    /**
     * Creates a new UID for the given doc and sets the field configured in the generator component with this value.
     */
    @Override
    public void setUID(DocumentModel doc) throws PropertyNotFoundException {
        final UIDGenerator generator = getUIDGeneratorFor(doc);
        if (generator != null) {
            generator.setUID(doc);
        }
    }

    /**
     * @return a new UID for the given document
     */
    @Override
    public String createUID(DocumentModel doc) {
        final UIDGenerator generator = getUIDGeneratorFor(doc);
        if (generator == null) {
            return null;
        } else {
            return generator.createUID(doc);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (UIDSequencer.class.isAssignableFrom(adapter)) {
            return adapter.cast(getSequencer());
        }
        if (UIDGeneratorService.class.isAssignableFrom(adapter)) {
            return adapter.cast(this);
        }
        return null;
    }

    @Override
    public UIDSequencer getSequencer() {
        return getSequencer(null);
    }

    @Override
    public UIDSequencer getSequencer(String name) {
        if (name == null) {
            name = defaultSequencer;
        }
        return sequencers.get(name);
    }

}
