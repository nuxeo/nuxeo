/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * $Id: StampState.java 24407 2007-08-30 18:42:15Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.component.list;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ui.component.UIFileUpload;
import org.nuxeo.ecm.platform.ui.web.directory.ChainSelect;

/**
 * This class saves the state of stamp components.
 * <p>
 * This is an adaptation of the Trinidad component to make it deal correctly with any kind of component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
final class StampState implements Externalizable {

    private static final long serialVersionUID = -4207557910028866684L;

    private static final Log log = LogFactory.getLog(StampState.class);

    private static final Object[] _EMPTY_ARRAY = new Object[0];

    private Map<DualKey, Object> rows;

    private static final class DualKey implements Serializable {

        private static final long serialVersionUID = 8302554393951287224L;

        private final Object key1;

        private final Object key2;

        private final int hash;

        DualKey(Object key1, Object key2) {
            this.key1 = key1;
            this.key2 = key2;

            hash = (key1 == null ? 0 : key1.hashCode()) + (key2 == null ? 0 : key2.hashCode());
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other instanceof DualKey) {
                DualKey otherKey = (DualKey) other;
                if (hashCode() != otherKey.hashCode()) {
                    return false;
                }

                return _eq(key1, otherKey.key1) && _eq(key2, otherKey.key2);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "<" + key1 + ',' + key2 + '>';
        }

    }

    public StampState() {
        rows = Collections.emptyMap();
    }

    /**
     * Clears all state except for the state associated with the give currencyObj.
     *
     * @param skipCurrencyObj
     */
    public void clear(Object skipCurrencyObj) {
        if (!rows.isEmpty()) {
            Iterator<DualKey> iter = rows.keySet().iterator();
            while (iter.hasNext()) {
                DualKey dk = iter.next();
                if (_eq(dk.key1, skipCurrencyObj)) {
                    continue;
                }
                iter.remove();
            }
        }
    }

    /**
     * Clears the held components state for given index.
     *
     * @since 8.1
     */
    public void clearIndex(int index) {
        if (!rows.isEmpty()) {
            Iterator<DualKey> iter = rows.keySet().iterator();
            while (iter.hasNext()) {
                DualKey dk = iter.next();
                if (_eq(dk.key1, index)) {
                    iter.remove();
                }
            }
        }
    }

    public void put(Object currencyObj, String key, Object value) {
        Map<DualKey, Object> comparant = Collections.emptyMap();
        if (rows == comparant) {
            // =-=AEW Better default sizes
            rows = new HashMap<>(109);
        }

        DualKey dk = new DualKey(currencyObj, key);
        rows.put(dk, value);
    }

    public int size() {
        return rows.size();
    }

    public Object get(Object currencyObj, String key) {
        DualKey dk = new DualKey(currencyObj, key);
        return rows.get(dk);
    }

    /**
     * Saves the state of a stamp. This method is called when the currency of this component is changed so that the
     * state of this stamp can be preserved, before the stamp is updated with the state corresponding to the new
     * currency. This method recurses for the children and facets of the stamp.
     *
     * @return this object must be Serializable if client-side state saving is used.
     */
    public static Object saveStampState(FacesContext context, UIComponent stamp) {
        if (stamp.isTransient()) {
            return null;
        }

        // because base components use shallow copies in their saveState method,
        // need to copy separately properties that are likely to change as well
        // as submitted value that is not saved in UIInput state
        Object[] selfState = new Object[5];

        // XXX AT: NXP-1508: saving the whole state is overkill, but it must be
        // done for some components that save some info in other fields than
        // standard EditableValueHolder fields.
        Object[] innerSelfState = new Object[5];
        if (stamp instanceof ChainSelect) {
            innerSelfState[0] = stamp.saveState(context);
        } else if (stamp instanceof UIFileUpload) {
            innerSelfState[0] = stamp.saveState(context);
            UIFileUpload fileUpload = (UIFileUpload) stamp;
            innerSelfState[1] = fileUpload.getLocalContentType();
            innerSelfState[2] = fileUpload.getLocalFileName();
            innerSelfState[3] = fileUpload.getLocalFileSize();
            innerSelfState[4] = fileUpload.getLocalInputStream();
        } else if (stamp instanceof UIEditableList) {
            // TODO: don't save the whole state, it may be costly
            innerSelfState[0] = stamp.saveState(context);
        }
        selfState[0] = innerSelfState;

        // editable value holder standard values saving
        if (stamp instanceof EditableValueHolder) {
            EditableValueHolder evh = (EditableValueHolder) stamp;
            selfState[1] = evh.getSubmittedValue();
            selfState[2] = evh.getLocalValue();
            selfState[3] = evh.isLocalValueSet();
            selfState[4] = evh.isValid();
        }
        Object[] state = new Object[3];
        state[0] = selfState;

        int facetCount = stamp.getFacets().size();
        Object[] facetState;
        if (facetCount == 0) {
            facetState = _EMPTY_ARRAY;
        } else {
            facetState = new Object[facetCount * 2];
            Map<String, UIComponent> facetMap = stamp.getFacets();
            int i = 0;
            for (Map.Entry<String, UIComponent> entry : facetMap.entrySet()) {
                int base = i * 2;
                UIComponent facet = entry.getValue();
                if (!facet.isTransient()) {
                    facetState[base] = entry.getKey();
                    facetState[base + 1] = saveStampState(context, entry.getValue());
                    i++;
                }
            }
        }

        state[1] = facetState;

        int childCount = stamp.getChildCount();
        Object[] childStateArray;
        if (childCount == 0) {
            childStateArray = _EMPTY_ARRAY;
        } else {
            childStateArray = new Object[childCount];
            boolean wasAllTransient = true;
            int i = 0;
            for (UIComponent child : stamp.getChildren()) {
                if (!child.isTransient()) {
                    wasAllTransient = false;
                    childStateArray[i] = saveStampState(context, child);
                }
                i++;
            }

            // If all we found were transient components, just use
            // an empty array
            if (wasAllTransient) {
                childStateArray = _EMPTY_ARRAY;
            }
        }

        state[2] = childStateArray;

        return state;
    }

    /**
     * Restores the state of a stamp. This method is called after the currency of this component is changed so that the
     * state of this stamp can be changed to match the new currency. This method recurses for the children and facets of
     * the stamp.
     */
    public static void restoreStampState(FacesContext context, UIComponent stamp, Object stampState) {
        if (stampState == null || stamp == null) {
            return;
        }
        Object[] state = (Object[]) stampState;

        Object[] selfState = (Object[]) state[0];

        // NXP-1508: restoring specific values for components that do not follow
        // EditableValueHolder standard interface.
        Object[] innerSelfState = (Object[]) selfState[0];
        if (stamp instanceof ChainSelect) {
            stamp.restoreState(context, innerSelfState[0]);
        } else if (stamp instanceof UIFileUpload) {
            stamp.restoreState(context, innerSelfState[0]);
            UIFileUpload fileUpload = (UIFileUpload) stamp;
            fileUpload.setLocalContentType((String) innerSelfState[1]);
            fileUpload.setLocalFileName((String) innerSelfState[2]);
            fileUpload.setLocalFileSize((Integer) innerSelfState[3]);
            fileUpload.setLocalInputStream((InputStream) innerSelfState[4]);
        } else if (stamp instanceof UIEditableList) {
            stamp.restoreState(context, innerSelfState[0]);
        }

        // editable value holder standard values saving
        if (stamp instanceof EditableValueHolder) {
            EditableValueHolder evh = (EditableValueHolder) stamp;
            evh.setSubmittedValue(selfState[1]);
            // XXX AT: must set value before localValueSet because UIInput
            // setValue method will set localValueSet to true.
            evh.setValue(selfState[2]);
            if (selfState[3] != null) {
                evh.setLocalValueSet((Boolean) selfState[3]);
            } else {
                // make sure it's set to false otherwise, see NXP-18734
                evh.setLocalValueSet(false);
            }
            if (selfState[4] != null) {
                evh.setValid((Boolean) selfState[4]);
            }
        }
        // Force the ID to be reset to reset the client identifier (needed
        // for UIComponentBase implementation which caches clientId too
        // aggressively)
        stamp.setId(stamp.getId());

        Object[] facetStateArray = (Object[]) state[1];
        for (int i = 0; i < facetStateArray.length; i += 2) {
            String facetName = (String) facetStateArray[i];
            Object facetState = facetStateArray[i + 1];
            restoreStampState(context, stamp.getFacet(facetName), facetState);
        }

        Object[] childStateArray = (Object[]) state[2];
        int childArrayCount = childStateArray.length;
        int i = 0;
        for (UIComponent child : stamp.getChildren()) {
            if (!child.isTransient() && i < childArrayCount) {
                restoreStampState(context, child, childStateArray[i]);
            }
            i++;
        }
    }

    // TODO can do better...
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(rows.size());

        if (rows.isEmpty()) {
            return;
        }

        Map<DualKey, Object> map = new HashMap<>(rows.size());
        map.putAll(rows);

        if (log.isDebugEnabled()) {
            for (Map.Entry<DualKey, Object> entry : map.entrySet()) {
                log.debug("Saving " + entry.getKey() + ", " + entry.getValue());
            }
        }

        out.writeObject(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();

        if (size > 0) {
            rows = (Map<DualKey, Object>) in.readObject();
        }

        if (log.isDebugEnabled()) {
            for (Map.Entry<DualKey, Object> entry : rows.entrySet()) {
                log.debug("Restoring " + entry.getKey() + ", " + entry.getValue());
            }
        }
    }

    private static boolean _eq(Object k1, Object k2) {
        if (k1 == null) {
            return k2 == null;
        }
        return k1.equals(k2);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append("StampState");
        buf.append(" {");
        buf.append(" rows=");
        buf.append(rows);
        buf.append('}');

        return buf.toString();
    }

}
