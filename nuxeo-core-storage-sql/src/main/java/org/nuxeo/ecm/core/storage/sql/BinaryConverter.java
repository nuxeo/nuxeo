/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to convert binaries.
 *
 * @author Florent Guillaume
 */
public class BinaryConverter {

    private static final Log log = LogFactory.getLog(BinaryConverter.class);

    private static final String TS = "org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon";

    private static final String TD = "org.nuxeo.ecm.platform.transform.interfaces.TransformDocument";

    private static final String ANY2TEXT = "any2text";

    /**
     * null if not initialized, TRUE if initialized, FALSE if initialization
     * failed.
     */
    private Boolean initialized;

    private Object transformService;

    private Method transformMethod;

    private Method transformDocGetBlobMethod;

    /*
     * Not done in a constructor as it would be called too early before all
     * services are registered.
     */
    private boolean initialized() {
        if (initialized != null) {
            return initialized.booleanValue();
        }
        initialized = Boolean.FALSE;

        /*
         * Find classes.
         */
        Class<?> transformServiceCommonClass;
        Class<?> transformDocumentClass;
        try {
            transformServiceCommonClass = Class.forName(TS);
        } catch (ClassNotFoundException e) {
            log.error("Could not find class: " + TS);
            return false;
        }
        try {
            transformDocumentClass = Class.forName(TD);
        } catch (ClassNotFoundException e) {
            log.error("Could not find class: " + TD);
            return false;
        }

        /*
         * Lookup service.
         */
        try {
            transformService = Framework.getService(transformServiceCommonClass);
        } catch (Exception e) {
            log.error("Could not lookup TransformService", e);
            return false;
        }
        if (transformService == null) {
            log.debug("No TransformService available");
            return false;
        }

        /*
         * Find methods by reflection.
         */
        try {
            transformMethod = transformServiceCommonClass.getMethod(
                    "transform", new Class[] { String.class, Map.class,
                            Blob[].class });
        } catch (Exception e) {
            log.error("Could not find method TransformService.transform", e);
            return false;
        }
        try {
            transformDocGetBlobMethod = transformDocumentClass.getMethod(
                    "getBlob", new Class[] {});
        } catch (Exception e) {
            log.error("Could not find method TransformDocument.getBlob", e);
            return false;
        }

        initialized = Boolean.TRUE;
        return true;
    }

    /**
     * Converts a binary to text.
     *
     * @return a string or {@code null} if conversion failed
     */
    public String getString(Binary binary, String mimeType) {
        if (!initialized()) {
            return null;
        }

        /*
         * Call transform.
         */
        Blob blob;
        try {
            blob = new InputStreamBlob(binary.getStream(), mimeType);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        List<?> results;
        try {
            results = (List<?>) transformMethod.invoke(transformService,
                    ANY2TEXT, null, new Blob[] { blob });
        } catch (Exception e) {
            log.error("Could not invoke transformation", e);
            return null;
        }
        if (results == null || results.size() == 0) {
            return null;
        }

        /*
         * Get the Blob from the result.
         */
        Object tdoc = results.get(0);
        try {
            blob = (Blob) transformDocGetBlobMethod.invoke(tdoc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
        if (blob == null) {
            return null;
        }

        /*
         * Extract the text.
         */
        try {
            return new String(blob.getByteArray(), "UTF-8");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
