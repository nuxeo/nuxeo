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

package org.nuxeo.ecm.platform.uidgen.ejb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.uidgen.UIDSequencerFactory;
import org.nuxeo.ecm.platform.uidgen.service.UIDSequencerImpl;

/**
 * EJB Sequence factory. Will provide ejb based implementation
 * UIDSequencerManager
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class EjbSequencerFactory implements UIDSequencerFactory {

    private static final Log log = LogFactory.getLog(EjbSequencerFactory.class);

    public UIDSequencer createUIDSequencer() {
        log.debug("create UIDSequencer ... ");
        try {
            return getSequencerManager();
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    private static UIDSequencer getSequencerManager() throws Exception {
        return new UIDSequencerImpl();
    }

}
