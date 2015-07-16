/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.descriptors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for contributed negotiators.
 *
 * @since 7.4
 */
@XObject("negotiation")
public class NegotiationDescriptor {

    @XNode("@target")
    protected String target;

    @XNode("@append")
    protected boolean append = false;

    @XNodeList(value = "negotiator", type = ArrayList.class, componentType = NegotiatorDescriptor.class)
    List<NegotiatorDescriptor> negotiators;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public List<NegotiatorDescriptor> getNegotiators() {
        List<NegotiatorDescriptor> res = new ArrayList<NegotiatorDescriptor>();
        if (negotiators != null) {
            res.addAll(negotiators);
        }
        Collections.sort(res);
        return res;
    }

    public void setNegotiators(List<NegotiatorDescriptor> negotiators) {
        this.negotiators = negotiators;
    }

    public void merge(NegotiationDescriptor src) {
        List<NegotiatorDescriptor> negotiators = src.negotiators;
        if (negotiators != null) {
            List<NegotiatorDescriptor> merged = new ArrayList<NegotiatorDescriptor>();
            merged.addAll(negotiators);
            boolean keepOld = src.isAppend() || (negotiators.isEmpty() && !src.isAppend());
            if (keepOld) {
                // add back old contributions
                List<NegotiatorDescriptor> oldNegotiators = this.negotiators;
                if (oldNegotiators != null) {
                    merged.addAll(0, oldNegotiators);
                }
            }
            setNegotiators(merged);
        }
    }

    @Override
    public NegotiationDescriptor clone() {
        NegotiationDescriptor clone = new NegotiationDescriptor();
        clone.setTarget(getTarget());
        clone.setAppend(isAppend());
        List<NegotiatorDescriptor> negotiators = this.negotiators;
        if (negotiators != null) {
            List<NegotiatorDescriptor> cnegociators = new ArrayList<NegotiatorDescriptor>();
            for (NegotiatorDescriptor neg : negotiators) {
                cnegociators.add(neg.clone());
            }
            clone.setNegotiators(cnegociators);
        }
        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NegotiationDescriptor)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        NegotiationDescriptor p = (NegotiationDescriptor) obj;
        return new EqualsBuilder().append(target, p.target).append(append, p.append).append(negotiators, p.negotiators).isEquals();
    }
}
