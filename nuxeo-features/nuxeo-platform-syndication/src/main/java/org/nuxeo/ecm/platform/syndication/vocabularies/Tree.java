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
 *     <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.syndication.vocabularies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public final class Tree {

    public static final class Builder {

        private final Map<String, List<SimpleVocabulary>> pendingVocabularies
                = new HashMap<String, List<SimpleVocabulary>>();

        private final Map<String, HierarchicalVocabulary> mapVocabularies
                = new HashMap<String, HierarchicalVocabulary>();


        public void addElement(final String parent,
                HierarchicalVocabulary parentVoca1,
                final SimpleVocabulary voca) {
            if (null == parent) {
                final HierarchicalVocabulary newVoca = new HierarchicalVocabulary(
                        null, voca);
                addNewVocabulary(newVoca);
            } else {
                final HierarchicalVocabulary parentVoca = mapVocabularies.get(parent.toLowerCase());
                if (null != parentVoca) {
                    final HierarchicalVocabulary newVoca = new HierarchicalVocabulary(
                            parentVoca, voca);
                    parentVoca.addChild(newVoca);
                    addNewVocabulary(newVoca);

                } else {
                    mapVocabularies.put(parent, parentVoca1);
                    addPendingVocabulary(parent, voca);
                }
            }
        }

        private void addNewVocabulary(final HierarchicalVocabulary voca) {
            final String id = voca.getVocabulary().getId().toLowerCase();
            mapVocabularies.put(id, voca);
            addWaitingChilds(voca);
        }

        private void addWaitingChilds(final HierarchicalVocabulary voca) {
            final String id = voca.getVocabulary().getId().toLowerCase();
            final List<SimpleVocabulary> childs = pendingVocabularies.remove(id);
            if (null != childs) {
                for (final SimpleVocabulary child : childs) {
                    final HierarchicalVocabulary newVoca = new HierarchicalVocabulary(
                            voca, child);
                    voca.addChild(newVoca);
                }
            }
        }

        private void addPendingVocabulary(final String parent,
                final SimpleVocabulary dir) {
            List<SimpleVocabulary> dirs = pendingVocabularies.get(parent.toLowerCase());
            if (null == dirs) {
                dirs = new ArrayList<SimpleVocabulary>();
            }
            dirs.add(dir);
            pendingVocabularies.put(parent.toLowerCase(), dirs);
        }

        public Tree build() {
            final List<HierarchicalVocabulary> rootNodes = new ArrayList<HierarchicalVocabulary>();

            Map<String, HierarchicalVocabulary> mV = new HashMap<String, HierarchicalVocabulary>();
            mV.putAll(mapVocabularies);

            for(String key : mV.keySet()) {
                addWaitingChilds(mapVocabularies.get(key));
            }

            for (HierarchicalVocabulary voca : mapVocabularies.values()) {
                HierarchicalVocabulary vp =  getNode(voca);
                if(rootNodes.contains(vp) ==false){
                    rootNodes.add(vp);
                }
            }

            Collections.sort(rootNodes, HierarchicalVocabulary.ORDER_BY_ID);
            return new Tree(rootNodes);
        }

        public HierarchicalVocabulary getNode(HierarchicalVocabulary hv) {
            if (hv.getParent() == null) {
                return hv;
            } else {
                return getNode(hv.getParent());
            }
        }

    }

    private final List<HierarchicalVocabulary> rootNodes = new ArrayList<HierarchicalVocabulary>();

    private Tree(final List<HierarchicalVocabulary> rootNodes) {
        this.rootNodes.addAll(rootNodes);
    }

    // Missing methods for finding, adding, etc an element...

    public String asXML() {
        final DOMDocumentFactory domfactory = new DOMDocumentFactory();
        final DOMDocument document = (DOMDocument) domfactory.createDocument();

        final Element current = document.createElement("entries");
        document.setRootElement((org.dom4j.Element) current);

        buildXML(rootNodes, current, document);
        return document.asXML();
    }

    public void buildXML(final DOMDocument document) {
        final Element current = document.createElement("entries");
        document.setRootElement((org.dom4j.Element) current);

        buildXML(rootNodes, current, document);
    }

    private static void buildXML(final List<HierarchicalVocabulary> nodes,
            final Element currentElement, final DOMDocument document) {

        for (final HierarchicalVocabulary voca : nodes) {
            final Element element = document.createElement("entry");
            element.setAttribute("id", voca.getVocabulary().getId());
            element.setAttribute("label", voca.getVocabulary().getLabel());
            element.setAttribute("translatedLabel",
                    voca.getVocabulary().getTranslatedLabel());
            if (voca.hasParent()) {
                element.setAttribute("parent",
                        voca.getParent().getVocabulary().getId());
            }
            if (voca.hasChilds()) {
                buildXML(voca.getChilds(), element, document);
            }
            currentElement.appendChild(element);
        }
    }



}
