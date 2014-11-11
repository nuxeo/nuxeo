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

package org.nuxeo.theme.perspectives;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.Registrable;
import org.nuxeo.theme.elements.Element;
import org.nuxeo.theme.relations.DefaultPredicate;
import org.nuxeo.theme.relations.DyadicRelation;
import org.nuxeo.theme.relations.Predicate;
import org.nuxeo.theme.relations.Relation;
import org.nuxeo.theme.relations.RelationStorage;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

public class PerspectiveManager implements Registrable {

    private static final String PREDICATE_NAME = "_ is visible in perspective _";

    private static final Predicate predicate = new DefaultPredicate(
            PREDICATE_NAME);

    private final RelationStorage relationStorage = Manager.getRelationStorage();

    public static PerspectiveType getPerspectiveByName(String name) {
        return (PerspectiveType) Manager.getTypeRegistry().lookup(
                TypeFamily.PERSPECTIVE, name);
    }

    public static boolean hasPerspective(String perspectiveName) {
        List<String> perspectiveNames = Manager.getTypeRegistry().getTypeNames(
                TypeFamily.PERSPECTIVE);
        if (perspectiveNames == null) {
            return false;
        }
        return perspectiveNames.contains(perspectiveName);
    }

    public boolean isVisibleInPerspective(Element element,
            PerspectiveType perspective) {
        // the fragment is not in relation with any specific perspective: it is
        // always visible.
        if (relationStorage.search(predicate, element, null).isEmpty()) {
            return true;
        }
        return !relationStorage.search(predicate, element, perspective).isEmpty();
    }

    public static void setVisibleInPerspective(Element element,
            PerspectiveType perspective) {
        Manager.getRelationStorage().add(
                new DyadicRelation(predicate, element, perspective));
    }

    public void setVisibleInAllPerspectives(Element element) {
        // remove existing relations
        removeRelationsFrom(element);
        // create new relations
        for (PerspectiveType perspective : listPerspectives()) {
            Manager.getRelationStorage().add(
                    new DyadicRelation(predicate, element, perspective));
        }
    }

    public void setVisibleInPerspectives(Element element,
            List<String> perspectiveNames) {
        // remove existing relations
        removeRelationsFrom(element);
        // create new relations
        for (String perspectiveName : perspectiveNames) {
            PerspectiveType perspective = getPerspectiveByName(perspectiveName);
            Manager.getRelationStorage().add(
                    new DyadicRelation(predicate, element, perspective));
        }
    }

    public void setAlwaysVisible(Element element) {
        removeRelationsFrom(element);
    }

    public boolean isAlwaysVisible(Element element) {
        return getPerspectivesFor(element).isEmpty();
    }

    public List<PerspectiveType> getPerspectivesFor(Element element) {
        List<PerspectiveType> perspectives = new ArrayList<PerspectiveType>();
        for (Relation relation : relationStorage.search(predicate, element,
                null)) {
            PerspectiveType perspective = (PerspectiveType) relation.getRelate(2);
            perspectives.add(perspective);
        }
        return perspectives;
    }

    public static List<PerspectiveType> listPerspectives() {
        List<PerspectiveType> perspectives = new ArrayList<PerspectiveType>();
        for (Type perspective : Manager.getTypeRegistry().getTypes(
                TypeFamily.PERSPECTIVE)) {
            perspectives.add((PerspectiveType) perspective);
        }
        return perspectives;
    }

    private void removeRelationsFrom(Element element) {
        for (Relation relation : relationStorage.search(predicate, element,
                null)) {
            relationStorage.remove(relation);
        }
    }

    public void clear() {
    }

}
