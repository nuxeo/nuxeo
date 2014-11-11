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

package org.nuxeo.theme.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.formats.Format;
import org.nuxeo.theme.formats.FormatType;
import org.nuxeo.theme.relations.DyadicRelation;
import org.nuxeo.theme.relations.Relate;
import org.nuxeo.theme.relations.Relation;
import org.nuxeo.theme.relations.RelationStorage;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

public final class ElementFormatter {

    private ElementFormatter() {
        // This class is not supposed to be instantiated.
    }

    public static void setFormat(final Object object, final Format format) {
        // Remove the old format of the same type
        final Format oldFormat = getFormatByType(object, format.getFormatType());
        if (oldFormat != null) {
            removeFormat(object, oldFormat);
        }
        Manager.getRelationStorage().add(
                new DyadicRelation(format.getPredicate(), (Element) object,
                        format));
    }

    public static Format getFormatByType(final Object object,
            final FormatType type) {
        final Collection<Relation> relations = Manager.getRelationStorage().search(
                type.getPredicate(), (Element) object, null);
        final Iterator<Relation> i = relations.iterator();
        if (i.hasNext()) {
            return (Format) ((DyadicRelation) i.next()).getRelate(2);
        }
        // TODO throw exception;
        return null;
    }

    public static Collection<Format> getFormatsFor(final Element element) {
        final Collection<Format> formats = new ArrayList<Format>();
        // TODO use a type manager for registering and getting format types
        final String[] formatTypeNames = { "widget", "style", "layout" };
        for (String typeName : formatTypeNames) {
            Format format = getFormatFor(element, typeName);
            if (format != null) {
                formats.add(format);
            }
        }
        return formats;
    }

    public static Format getFormatFor(final Element element,
            final String typeName) {
        final RelationStorage relationStorage = Manager.getRelationStorage();
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        final FormatType type = (FormatType) typeRegistry.lookup(
                TypeFamily.FORMAT, typeName);
        // FIXME: this loop doesn't loop!
        for (Relation relation : relationStorage.search(type.getPredicate(),
                element, null)) {
            return (Format) relation.getRelate(2);
        }
        return null;
    }

    public static Collection<Element> getElementsFor(final Format format) {
        final Collection<Element> elements = new ArrayList<Element>();
        final RelationStorage relationStorage = Manager.getRelationStorage();
        final String[] formatTypeNames = { "widget", "style", "layout" };
        final TypeRegistry typeRegistry = Manager.getTypeRegistry();
        for (String typeName : formatTypeNames) {
            final FormatType type = (FormatType) typeRegistry.lookup(
                    TypeFamily.FORMAT, typeName);
            for (Relation relation : relationStorage.search(
                    type.getPredicate(), null, format)) {
                elements.add((Element) relation.getRelate(1));
            }
        }
        return elements;
    }

    public static void removeFormat(final Object object, final Format format) {
        final RelationStorage relationStorage = Manager.getRelationStorage();
        for (Relation relation : relationStorage.search(format.getPredicate(),
                (Relate) object, format)) {
            relationStorage.remove(relation);
        }
    }

}
