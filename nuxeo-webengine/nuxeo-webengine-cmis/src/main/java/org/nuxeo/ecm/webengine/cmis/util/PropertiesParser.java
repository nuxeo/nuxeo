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
 */
package org.nuxeo.ecm.webengine.cmis.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.abdera.model.Element;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.property.Property;
import org.apache.chemistry.property.PropertyDefinition;
import org.apache.chemistry.repository.Repository;
import org.apache.chemistry.type.Type;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PropertiesParser {
    
    public Map<String,Serializable> parse(Repository repo, Type entryType, Element props) { 
        ArrayList<Object> values = new ArrayList<Object>();
        for (Element child : props.getElements()) {
            String localName = child.getQName().getLocalPart();
            String type = null;
            if (localName.startsWith("property")) {
                type = localName.substring(8);
            }
            String key = child.getAttributeValue(CMIS.NAME);
            if (key == null) {
                throw new IllegalArgumentException("No name for property"); // Invalid cmis object
            }            
            ValueAdapter va = getValueAdapter(type);
            if (entryType == null && Property.TYPE_ID.equals(Property.TYPE_ID)) {
                entryType = repo.getType(key);
                if (entryType == null) {
                    throw new IllegalArgumentException("No Such Type: "+key);
                }
            }            
            ValueIterator it = new ValueIterator(child);
            if (it.hasNext()) {
                Serializable val = va.readValue(it.nextValue());
                if (it.hasNext()) {
                    ArrayList<Serializable> vals = new ArrayList<Serializable>();
                    vals.add(val);
                    do {
                        vals.add(va.readValue(it.nextValue()));
                    } while (it.hasNext());
                    val = vals.toArray(va.createArray(vals.size()));
                }
                values.add(key);
                values.add(va);
                values.add(val);
            } else { // ignore empty values?
                continue;
            }
        }
            
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        // we need to convert scalar to lists when this is the case ...
        // we cannot do this earlier since the object type may not be known
        for (int i=0,len=values.size(); i<len; i+=3) {
            String key = values.get(i).toString();
            Serializable val = (Serializable)values.get(i+2);
            PropertyDefinition pd = entryType.getPropertyDefinition(key);
            if (pd != null) {
                if (pd.isMultiValued()) {
                    if (!val.getClass().isArray()) {
                        Object ar = ((ValueAdapter)values.get(i+1)).createArray(1);
                        Array.set(ar, 0, val);
                        val = (Serializable)ar;
                    }
                }
            } else {
                throw new IllegalArgumentException("No such property: "+key);
            }
            map.put(key, val);
        }
        return map;
    }
    
    public ValueAdapter getValueAdapter(String type) {
        ValueAdapter va = ValueAdapter.getAdapter(type);
        return (va == null) ? ValueAdapter.STRING : va;
    }

    
    public class ValueIterator {
        protected Element val;
        
        public ValueIterator(Element prop) {
            val = prop.getFirstChild(CMIS.VALUE);
        }
        public boolean hasNext() {
            return val != null;
        }
        public String nextValue() {
            if (val == null) {
                throw new NoSuchElementException("no more elements"); 
            }
            String text = val.getText();
            val = val.getNextSibling(CMIS.VALUE);
            return text;
        }
    }
    
}
