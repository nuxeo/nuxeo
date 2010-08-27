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
 *
 */

package org.nuxeo.ecm.platform.uidgen.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.uidgen.UIDGenerator;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

/**
 * Service that writes MetaData.
 *
 * @author : <a href="dm@nuxeo.com">Dragos Mihalache</a>
 */
public class UIDGeneratorService extends DefaultComponent {

    public static final String ID = "org.nuxeo.ecm.platform.uidgen.service.UIDGeneratorService";

    public static final String UID_GENERATORS_EXTENSION_POINT = "generators";

    /**
     * Extension point is deprecated should be removed - preserved for now only
     * for startup warnings.
     */
    public static final String EXTENSION_POINT_SEQUENCER_FACTORY = "sequencerFactory";

    private static final Log log = LogFactory.getLog(UIDGeneratorService.class);

    private final Map<String, UIDGenerator> generators = new HashMap<String, UIDGenerator>();

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
        UIDSequencerImpl.dispose();
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        log.debug("<registerExtension>");
        super.registerExtension(extension);

        final String extPoint = extension.getExtensionPoint();
        if (UID_GENERATORS_EXTENSION_POINT.equals(extPoint)) {
            log.info("register contributions for extension point: "
                    + UID_GENERATORS_EXTENSION_POINT);

            // if (null == this.sequencer) {
            // throw new RuntimeServiceException("Sequencer not initialized.");
            // }

            final Object[] contribs = extension.getContributions();
            registerGenerators(extension, contribs);
        } else if (EXTENSION_POINT_SEQUENCER_FACTORY.equals(extPoint)) {
            String msg = "UIDSequencer factory no more supported from version 5.4. Faulty component: "
                    + extension.getComponent();
            Framework.getRuntime().getWarnings().add(msg);
            log.error(msg);
        } else {
            log.warn("extension not handled: " + extPoint);
        }
    }

    public UIDSequencer getSequencer() {
        try {
            return Framework.getService(UIDSequencer.class);
        } catch (Exception e) {
            throw new RuntimeException("Service is not available.");
        }
    }

    private void registerGenerators(Extension extension, final Object[] contribs)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {

        // read the list of generators
        for (Object contrib : contribs) {
            final UIDGeneratorDescriptor generatorDescriptor = (UIDGeneratorDescriptor) contrib;
            final String generatorName = generatorDescriptor.getName();

            final UIDGenerator generator = (UIDGenerator) extension.getContext().loadClass(
                    generatorDescriptor.getClassName()).newInstance();

            final String[] propNames = generatorDescriptor.getPropertyNames();
            if (propNames.length == 0) {
                log.error("no property name defined on generator "
                        + generatorName);
            }
            // set the property name on generator
            generator.setPropertyNames(propNames);

            // Register Generator for DocTypes and property name
            final String[] docTypes = generatorDescriptor.getDocTypes();
            registerGeneratorForDocTypes(generator, docTypes);

            log.info("registered UID generator: " + generatorName);
        }
    }

    /**
     * Registers given UIDGenerator for the given document types. If there is
     * already a generator registered for one of document type it will be
     * discarded (and replaced with the new generator).
     */
    private void registerGeneratorForDocTypes(final UIDGenerator generator,
            final String[] docTypes) {

        for (String docType : docTypes) {
            final UIDGenerator previous = generators.put(docType, generator);
            if (previous != null) {
                log.info("Overwriting generator: " + previous.getClass()
                        + " for docType: " + docType);
            }
            log.info("Registered generator: " + generator.getClass()
                    + " for docType: " + docType);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        log.debug("<unregisterExtension>");
        super.unregisterExtension(extension);
    }

    /**
     * Returns the uid generator to use for this document.
     * <p>
     * Choice is made following the document type and the generator
     * configuration.
     */
    public UIDGenerator getUIDGeneratorFor(DocumentModel doc) {
        final String docTypeName = doc.getType();
        final UIDGenerator generator = generators.get(docTypeName);

        if (generator == null) {
            log.debug("No UID Generator defined for doc type: " + docTypeName);
            return null;
        }
        // TODO maybe maintain an initialization state for generators
        // so the next call could be avoided (for each request)
        generator.setSequencer(getSequencer());

        return generator;
    }

    /**
     * Creates a new UID for the given doc and sets the field configured in the
     * generator component with this value.
     */
    public void setUID(DocumentModel doc) throws DocumentException {
        final UIDGenerator generator = getUIDGeneratorFor(doc);
        if (generator != null) {
            generator.setUID(doc);
        }
    }

    /**
     * @return a new UID for the given document
     */
    public String createUID(DocumentModel doc) throws DocumentException {
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
            return adapter.cast(new UIDSequencerImpl());
        }
        return null;
    }

}
