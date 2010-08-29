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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.uidgen;

import org.nuxeo.ecm.platform.uidgen.generators.UIDGenerator1;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides different UID generators based on the given doc type. It is up to
 * each UID generator to use the provided sequencer.
 *
 * @author DM
 */
public final class UIDGenFactory {

    // Utility class.
    private UIDGenFactory() {
    }

    public static UIDGenerator createGeneratorForDocType(String docTypeName) {
        return createGeneratorForDocType(docTypeName,
                Framework.getLocalService(UIDSequencer.class));
    }

    /**
     * Mockup generator factory. Will instantiate a Generator based on document
     * type.
     */
    public static UIDGenerator createGeneratorForDocType(String docTypeName,
            UIDSequencer sequencer) {
        final AbstractUIDGenerator generator = new UIDGenerator1();
        generator.setSequencer(sequencer);
        return generator;
    }

}
