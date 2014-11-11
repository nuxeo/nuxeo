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

package org.nuxeo.theme.relations;

import java.util.ArrayList;
import java.util.List;

public final class TriadicRelation extends AbstractRelation {

    public TriadicRelation(Predicate predicate, Relate first, Relate second,
            Relate third) {
        super(predicate);
        List<Relate> relates = new ArrayList<Relate>();
        relates.add(first);
        relates.add(second);
        relates.add(third);
        setRelates(relates);
    }

    @Override
    public RelationTypeFamily getRelationTypeFamily() {
        return RelationTypeFamily.TRIADIC;
    }

}
