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

public abstract class AbstractRelation implements Relation {

    public List<Relate> relates;

    private Predicate predicate;

    protected AbstractRelation(Predicate predicate) {
        this.predicate = predicate;
        relates = new ArrayList<Relate>();
    }

    public abstract RelationTypeFamily getRelationTypeFamily();

    public Relate getRelate(Integer position) {
        if (position > relates.size()) {
            // TODO throw exception;
        }
        return relates.get(position - 1);
    }

    public String hash() {
        List<String> r = new ArrayList<String>();
        for (Relate relate : relates) {
            r.add(relate.hash());
        }
        return String.format(predicate.hash().replace("_", "%s"), r.toArray());
    }

    @Override
    public String toString() {
        return "Relation {'" + hash() + "'}";
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public final void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public boolean hasPredicate(Predicate predicate) {
        return this.predicate.hash().equals(predicate.hash());
    }

    public List<Relate> getRelates() {
        return relates;
    }

    public void setRelates(List<Relate> relates) {
        this.relates = relates;
    }

}
