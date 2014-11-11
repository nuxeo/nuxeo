/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.negotiation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("negotiation")
public final class NegotiationType implements Type {

    private static final Log log = LogFactory.getLog(NegotiationType.class);

    @XNode("@object")
    public String object;

    @XNode("@strategy")
    public String strategy;

    @XNodeList(value = "scheme", type = ArrayList.class, componentType = String.class)
    private List<String> schemeClassNames;

    private List<Scheme> schemes;

    public String getTypeName() {
        return String.format("%s/%s", strategy, object);
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.NEGOTIATION;
    }

    public synchronized List<Scheme> getSchemes() {
        if (schemes == null && schemeClassNames != null) {
            schemes = new ArrayList<Scheme>();
            for (String schemeClassName : schemeClassNames) {
                Scheme scheme = null;
                try {
                    scheme = (Scheme) Class.forName(schemeClassName).newInstance();
                } catch (Exception e) {
                    log.error("Could not create instance: " + schemeClassName);
                    continue;
                }
                schemes.add(scheme);
            }
        }
        return schemes;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getObject() {
        return object;
    }

}
