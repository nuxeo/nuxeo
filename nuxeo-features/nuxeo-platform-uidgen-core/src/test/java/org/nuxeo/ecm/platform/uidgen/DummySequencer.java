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

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory sequencer - no persistence. Usefull for test cases.
 *
 * @author DM
 */
public class DummySequencer implements UIDSequencer {

    private final Map<String, Integer> seqs = new HashMap<String, Integer>();

    public int getNext(String key) {
        if (seqs.containsKey(key)) {
            int current = seqs.get(key);
            current++;
            seqs.put(key, current);
            return current;
        } else {
            seqs.put(key, 1);
            return 1;
        }
    }

}
