/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.convert.extension;

import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;

/**
 * Interface that must be implemented by Converter that depend on an external
 * service.
 * <p>
 * Compared to {@link Converter} interface, it adds support for
 * checking converter availability.
 *
 * @author tiry
 */
public interface ExternalConverter extends Converter {

    /**
     * Checks if the converter is available.
     */
    ConverterCheckResult isConverterAvailable();

}
