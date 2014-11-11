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

import java.util.List;

public interface Relation {

    RelationTypeFamily getRelationTypeFamily();

    String hash();

    List<Relate> getRelates();

    Relate getRelate(Integer position);

    boolean hasPredicate(Predicate predicate);

}
