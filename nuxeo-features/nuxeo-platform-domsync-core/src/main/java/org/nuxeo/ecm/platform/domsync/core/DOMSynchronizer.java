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
 *     Max Stepanov
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.domsync.core;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.domsync.core.events.DOMAttrModifiedEvent;
import org.nuxeo.ecm.platform.domsync.core.events.DOMCharacterDataModifiedEvent;
import org.nuxeo.ecm.platform.domsync.core.events.DOMMutationEvent;
import org.nuxeo.ecm.platform.domsync.core.events.DOMNodeInsertedEvent;
import org.nuxeo.ecm.platform.domsync.core.events.DOMNodeRemovedEvent;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.MutationEvent;

/**
 * @author Max Stepanov
 *
 */
public class DOMSynchronizer implements EventListener, IDOMMutationListener {

    private static final String DOM_SUBTREE_MODIFIED = "DOMSubtreeModified";
    private static final String DOM_NODE_INSERTED = "DOMNodeInserted";
    private static final String DOM_NODE_REMOVED = "DOMNodeRemoved";
    private static final String DOM_NODE_REMOVED_FROM_DOCUMENT = "DOMNodeRemovedFromDocument";
    private static final String DOM_NODE_INSERTED_INTO_DOCUMENT = "DOMNodeInsertedIntoDocument";
    private static final String DOM_ATTR_MODIFIED = "DOMAttrModified";
    private static final String DOM_CHARACTER_DATA_MODIFIED = "DOMCharacterDataModified";

    private final Document document;
    private final IDOMSupport domSupport;
    private int dispatchLevel;
    private DOMMutationEvent currentEvent; /* for debug/test purposes */
    private final List<IDOMMutationListener> listeners = new ArrayList<IDOMMutationListener>();

    public DOMSynchronizer(Document document, IDOMSupport domSupport) {
        this.document = document;
        this.domSupport = domSupport;
    }

    /**
     * Document event handler
     */
    public void handleEvent(Event evt) {
        if (!(evt instanceof MutationEvent)) {
            return;
        }
        MutationEvent event = (MutationEvent) evt;
        String type = event.getType();

        if (DOM_CHARACTER_DATA_MODIFIED.equals(type)) {
            Node target = (Node) event.getTarget();
            String newValue = event.getNewValue();
            dispatchEvent(new DOMCharacterDataModifiedEvent(
                    DOMUtil.computeNodeXPath(document, target), newValue));

        } else if (DOM_NODE_INSERTED.equals(type)) {
            Node target = event.getRelatedNode();
            Node insertedNode = (Node)event.getTarget();
            int position = DOMUtil.getNodePosition(insertedNode);
            List<DOMNodeInsertedEvent> list = new ArrayList<DOMNodeInsertedEvent>();
            buildFragmentInsertedEvents(DOMUtil.computeNodeXPath(document, target),
                    insertedNode, position, list);
            for (DOMNodeInsertedEvent aList : list) {
                dispatchEvent(aList);
            }

        } else if (DOM_NODE_REMOVED.equals(type)) {
            Node target = (Node) event.getTarget();
            dispatchEvent(new DOMNodeRemovedEvent(
                    DOMUtil.computeNodeXPath(document, target)));

        } else if (DOM_ATTR_MODIFIED.equals(type)) {
            Node target = (Node) event.getTarget();
            dispatchEvent(new DOMAttrModifiedEvent(
                    DOMUtil.computeNodeXPath(document, target), event.getAttrName(), event.getAttrChange(), event.getNewValue()));

        } else {
            System.err.println("!Unsupported event type "+type);
        }
    }

    private static void buildFragmentInsertedEvents(String baseXPath, Node node,
            int position, List<DOMNodeInsertedEvent> list) {
        if (node instanceof Text) {
            list.add(new DOMNodeInsertedEvent(baseXPath,
                    "#text" + ((Text) node).getData(), position));
        } else if (node instanceof Element) {
            list.add(new DOMNodeInsertedEvent(baseXPath,
                    DOMUtil.getElementOuterNoChildren((Element) node), position));
            if (node.hasChildNodes()) {
                baseXPath += DOMUtil.computeNodeXPath(node.getParentNode(), node);
                node = node.getFirstChild();
                position = 0;
                while (node != null) {
                    buildFragmentInsertedEvents(baseXPath, node, position,
                            list);
                    node = node.getNextSibling();
                    ++position;
                }
            }
        } else {
            System.err.println("!Unsupported node type");
        }
    }

    private void dispatchEvent(DOMMutationEvent event) {
        if (dispatchLevel != 0) {
            if (!event.equals(currentEvent)) {
                System.err.println("Events don't match");
                System.err.println("original " + currentEvent);
                System.err.println("generated " + event);
            }
            return;
        }
        IDOMMutationListener[] list = listeners.toArray(new IDOMMutationListener[listeners.size()]);
        for (IDOMMutationListener listener : list) {
            listener.handleEvent(event);
        }
    }

    public void addMutationListener(IDOMMutationListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeMutationListener(IDOMMutationListener listener) {
        listeners.remove(listener);
    }

    /**
     * External mutation event handler
     */
    public void handleEvent(DOMMutationEvent event) {
        currentEvent = event;
        ++dispatchLevel;
        try {
            Node target = DOMUtil.findNodeByXPath(document, event.getTarget());
            if (target == null) {
                System.err.println("!Null target for "+event.getTarget());
                return;
            }

            if (event instanceof DOMNodeInsertedEvent) {
                if (!(target instanceof Element) && !(target instanceof Document)) {
                    System.err.println("!Unsupported target node type");
                    return;
                }
                int position = ((DOMNodeInsertedEvent)event).getPosition();
                String fragment = ((DOMNodeInsertedEvent)event).getFragment();
                Node docFragment;
                if(fragment.startsWith("#text")) {
                    docFragment = document.createTextNode(fragment.substring(5));
                } else {
                    docFragment = domSupport.createContextualFragment(target, fragment);
                }
                Node nodeBefore = DOMUtil.getNodeAtPosition(target, position);
                if (nodeBefore != null) {
                    target.insertBefore(docFragment, nodeBefore);
                } else {
                    target.appendChild(docFragment);
                }

            } else if (event instanceof DOMNodeRemovedEvent) {
                if (!(target instanceof Element) && !(target instanceof CharacterData)
                        && !(target instanceof ProcessingInstruction)) {
                    System.err.println("!Unsupported target node type");
                    return;
                }
                target.getParentNode().removeChild(target);

            } else if (event instanceof DOMAttrModifiedEvent) {
                if (!(target instanceof Element)) {
                    System.err.println("!Unsupported target node type");
                    return;
                }
                String attrName = ((DOMAttrModifiedEvent)event).getAttrName();
                short attrChange = ((DOMAttrModifiedEvent)event).getAttrChange();
                String newValue = ((DOMAttrModifiedEvent)event).getNewValue();
                NamedNodeMap attrs = target.getAttributes();
                if(attrChange == MutationEvent.REMOVAL) {
                    attrs.removeNamedItem(attrName);
                } else {
                    Attr attr = (Attr) attrs.getNamedItem(attrName);
                    if (attr != null) {
                        attr.setValue(newValue);
                    } else {
                        attr = document.createAttribute(attrName);
                        attr.setValue(newValue);
                        attrs.setNamedItem(attr);
                    }
                }

            } else if (event instanceof DOMCharacterDataModifiedEvent) {
                String data = ((DOMCharacterDataModifiedEvent) event).getNewValue();
                if (target instanceof CharacterData) {
                    ((CharacterData) target).setData(data);
                } else if (target instanceof ProcessingInstruction) {
                    ((ProcessingInstruction) target).setData(data);
                } else {
                    System.err.println("!Unsupported target node type");
                }

            } else {
                System.err.println("!Unsupported event " + event);
            }
        } finally {
            --dispatchLevel;
        }
    }

}
