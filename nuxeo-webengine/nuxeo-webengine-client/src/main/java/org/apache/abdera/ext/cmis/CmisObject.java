/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.apache.abdera.ext.cmis;

import org.apache.abdera.model.ExtensibleElementWrapper;
import org.apache.abdera.model.Element;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.xpath.XPath;
import org.apache.abdera.Abdera;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * Root of CMIS extension.
 *
 * Look at abdera-extensions-rss or abdera-extensions-opensearch for inspiration on how to build
 * this extension. See the unit tests for API usage.
 */
public class CmisObject extends ExtensibleElementWrapper {

    public CmisObject(Element internal) {
        super(internal);
    }

    public CmisObject(Factory factory, QName qname) {
        super(factory, qname);
    }

    /**
     * @return the property value for the given propertyName, converted to the correct type, or null if
     * not found.
     */
    // TODO: do we return a default value if the property is not found ? (Check the specs)
    @SuppressWarnings({"unchecked"})
    public Object getPropertyValue(String propertyName) {
        // TODO: xpath is probably less efficient than DOM navigation
        XPath xpath = Abdera.getInstance().getXPath();
        Map<String, String> ns = xpath.getDefaultNamespaces();
        ns.put("c", CmisConstants.CMIS_NS);
        String req = String.format("c:properties/*[@c:name='%s']/c:value", propertyName);
        List<Element> elements = (List<Element>) xpath.evaluate(req, this, ns);
        if (elements.size() == 0) {
            return null;
        }
        Element parent = elements.get(0).getParentElement();
        String propertyType = parent.getAttributeValue(new QName(CmisConstants.CMIS_NS, "propertyType"));
        String value = elements.get(0).getText();
        if ("Boolean".equals(propertyType)) {
            return Boolean.valueOf(value);
        } else if ("Integer".equals(propertyType)) {
            return Integer.valueOf(value);
        } // TODO: more...
        // Default: don't convert.
        return value;
    }

    // Or implement directly as accessors

    public String getObjectId() {
        return (String) getPropertyValue("objectId");
    }

    public String getObjectType() {
        String objectType = (String) getPropertyValue("objectType");
        // Hack around client who think that Alfresco's implementation is the reference.
        if (objectType == null) {
            objectType = (String) getPropertyValue("ObjectTypeId");
        }
        return objectType;
    }

    public String getBaseType() {
        return (String) getPropertyValue("baseType");
    }

    public Boolean isImmutable() {
        return (Boolean) getPropertyValue("isImmutable");
    }

    public String getContentStreamURI() {
        return (String) getPropertyValue("contentStreamURI");
    }

    public String getContentStreamMimetype() {
        return (String) getPropertyValue("contentStreamMimetype");
    }

    public String getContentStreamName() {
        return (String) getPropertyValue("contentStreamName");
    }

    public Integer getContentStreamLength() {
        return (Integer) getPropertyValue("contentStreamLength");
    }

    // TODO: parse date
    public String getCreationDate() {
        return (String) getPropertyValue("creationDate");
    }

    public String getLastModifiedBy() {
        return (String) getPropertyValue("lastModifiedBy");
    }

    public String getVersionLabel() {
        return (String) getPropertyValue("versionLabel");
    }

    public Boolean isLatestVersion() {
        return (Boolean) getPropertyValue("isLatestVersion");
    }

    public Boolean isMajorVersion() {
        return (Boolean) getPropertyValue("isMajorVersion");
    }

    public Boolean isLatestMajorVersion() {
        return (Boolean) getPropertyValue("isLatestMajorVersion");
    }

    public String getVersionSeriesCheckedOutID() {
        return (String) getPropertyValue("versionSeriesCheckedOutID");
    }

    public Boolean isCheckedOut() {
        return (Boolean) getPropertyValue("isCheckedOut");
    }

    public Boolean isVersionSeriesCheckedOut() {
        return (Boolean) getPropertyValue("isVersionSeriesCheckedOut");
    }

    public String getVersionSeriesCheckedOutBy() {
        return (String) getPropertyValue("versionSeriesCheckedOutBy");
    }

    public String getCheckinComment() {
        return (String) getPropertyValue("checkinComment");
    }

    // TODO

    /**
     * Returns true if the action is explicitely allowed.
     *
     * @param action ex: "Delete", "UpdateProperties", etc.
     * @return true is action is allowed
     */
    public boolean isActionAllowed(String action) {
        XPath xpath = Abdera.getInstance().getXPath();
        Map<String, String> ns = xpath.getDefaultNamespaces();
        ns.put("c", CmisConstants.CMIS_NS);
        String req = String.format("c:allowableActions/c:can%s/text()", action);
        String stringValue = xpath.valueOf(req, this, ns);
        return Boolean.valueOf(stringValue);
    }

    public boolean canDelete() {
        return isActionAllowed("Delete");
    }

    public boolean canUpdateProperties() {
        return isActionAllowed("UpdateProperties");
    }

    public boolean canGetProperties() {
        return isActionAllowed("GetProperties");
    }

    public boolean canGetParents() {
        return isActionAllowed("GetParents");
    }

    public boolean canMove() {
        return isActionAllowed("Move");
    }

    public boolean canDeleteVersion() {
        return isActionAllowed("DeleteVersion");
    }

    public boolean canDeleteContent() {
        return isActionAllowed("DeleteContent");
    }

    public boolean canCheckout() {
        return isActionAllowed("Checkout");
    }

    public boolean canCancelCheckout() {
        return isActionAllowed("CancelCheckout");
    }

    public boolean canCheckin() {
        return isActionAllowed("Checkin");
    }

    public boolean canSetContent() {
        return isActionAllowed("SetContent");
    }

    public boolean canGetAllVersion() {
        return isActionAllowed("GetAllVersions");
    }

    public boolean canAddToFolder() {
        return isActionAllowed("AddToFolder");
    }

    public boolean canRemoveFromFolder() {
        return isActionAllowed("RemoveFromFolder");
    }

    public boolean canViewContent() {
        return isActionAllowed("ViewContent");
    }

    public boolean canAddPolicy() {
        return isActionAllowed("AddPolicy");
    }

    public boolean canRemovePolicy() {
        return isActionAllowed("RemovePolicy");
    }

}
