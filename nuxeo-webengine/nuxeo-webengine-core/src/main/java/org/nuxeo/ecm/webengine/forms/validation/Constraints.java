/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.forms.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.forms.validation.constraints.And;
import org.nuxeo.ecm.webengine.forms.validation.constraints.Eq;
import org.nuxeo.ecm.webengine.forms.validation.constraints.Gt;
import org.nuxeo.ecm.webengine.forms.validation.constraints.GtEq;
import org.nuxeo.ecm.webengine.forms.validation.constraints.Like;
import org.nuxeo.ecm.webengine.forms.validation.constraints.Lt;
import org.nuxeo.ecm.webengine.forms.validation.constraints.LtEq;
import org.nuxeo.ecm.webengine.forms.validation.constraints.Not;
import org.nuxeo.ecm.webengine.forms.validation.constraints.Or;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("constraints")
public class Constraints {

    protected static Map<String,Class<? extends Constraint>>factories = new HashMap<String, Class<? extends Constraint>>();
    static {
        factories.put("eq", Eq.class);
        factories.put("lt", Lt.class);
        factories.put("gt", Gt.class);
        factories.put("lteq", LtEq.class);
        factories.put("gteq", GtEq.class);
        factories.put("enum", org.nuxeo.ecm.webengine.forms.validation.constraints.Enumeration.class);
        factories.put("like", Like.class);
        factories.put("not", Not.class);
        factories.put("or", Or.class);
        factories.put("and", And.class);
    }

    public static Constraint newConstraint(String name) throws Exception {
        Class<? extends Constraint> klass = factories.get(name);
        if (klass == null) {
            throw new NoSuchElementException("Constraint "+name+" not defined");
        }
        return klass.newInstance();
    }

}
