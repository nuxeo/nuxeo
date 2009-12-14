/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.chemistry.ws;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;

import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;

import org.apache.chemistry.Connection;
import org.apache.chemistry.Inclusion;
import org.apache.chemistry.ListPage;
import org.apache.chemistry.ObjectEntry;
import org.apache.chemistry.Paging;
import org.apache.chemistry.Property;
import org.apache.chemistry.PropertyType;
import org.apache.chemistry.RelationshipDirection;
import org.apache.chemistry.Repository;
import org.apache.chemistry.ws.CmisException;
import org.apache.chemistry.ws.CmisExtensionType;
import org.apache.chemistry.ws.CmisObjectListType;
import org.apache.chemistry.ws.CmisObjectType;
import org.apache.chemistry.ws.CmisPropertiesType;
import org.apache.chemistry.ws.CmisProperty;
import org.apache.chemistry.ws.CmisPropertyBoolean;
import org.apache.chemistry.ws.CmisPropertyDateTime;
import org.apache.chemistry.ws.CmisPropertyDecimal;
import org.apache.chemistry.ws.CmisPropertyHtml;
import org.apache.chemistry.ws.CmisPropertyId;
import org.apache.chemistry.ws.CmisPropertyInteger;
import org.apache.chemistry.ws.CmisPropertyString;
import org.apache.chemistry.ws.CmisPropertyUri;
import org.apache.chemistry.ws.DiscoveryServicePort;
import org.apache.chemistry.ws.EnumIncludeRelationships;
import org.apache.chemistry.ws.Query;
import org.apache.chemistry.ws.QueryResponse;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoRepository;

/**
 * @author Florent Guillaume
 */
@WebService(name = "DiscoveryServicePort", //
targetNamespace = "http://docs.oasis-open.org/ns/cmis/ws/200908/", //
serviceName = "DiscoveryService", //
portName = "DiscoveryServicePort", //
endpointInterface = "org.apache.chemistry.ws.DiscoveryServicePort")
public class DiscoveryServicePortImpl implements DiscoveryServicePort {

    public void getContentChanges(String repositoryId,
            Holder<String> changeLogToken, Boolean includeProperties,
            String filter, Boolean includePolicyIds, Boolean includeACL,
            BigInteger maxItems, CmisExtensionType extension,
            Holder<CmisObjectListType> objects) throws CmisException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public QueryResponse query(Query parameters) throws CmisException {
        // parameters
        String repositoryName = parameters.getRepositoryId();
        String statement = parameters.getStatement();

        JAXBElement<Boolean> searchAllVersionsB = parameters.getSearchAllVersions();
        boolean searchAllVersions = searchAllVersionsB == null ? false
                : searchAllVersionsB.getValue().booleanValue();

        JAXBElement<BigInteger> maxItemsBI = parameters.getMaxItems();
        int maxItems = maxItemsBI == null
                || maxItemsBI.getValue().intValue() < 0 ? 0
                : maxItemsBI.getValue().intValue();
        JAXBElement<BigInteger> skipCountBI = parameters.getSkipCount();
        int skipCount = skipCountBI == null
                || skipCountBI.getValue().intValue() < 0 ? 0
                : skipCountBI.getValue().intValue();
        Paging paging = new Paging(maxItems, skipCount);

        JAXBElement<Boolean> includeAllowableActions = parameters.getIncludeAllowableActions();
        boolean allowableActions = includeAllowableActions == null ? false
                : includeAllowableActions.getValue().booleanValue();
        JAXBElement<EnumIncludeRelationships> includeRelationships = parameters.getIncludeRelationships();
        RelationshipDirection relationships = RelationshipDirection.fromInclusion(includeRelationships.getValue().name());
        JAXBElement<String> renditionFilter = parameters.getRenditionFilter();
        String renditions = renditionFilter == null ? null
                : renditionFilter.getValue();
        Inclusion inclusion = new Inclusion(null, renditions, relationships,
                allowableActions, false, false);

        // response
        QueryResponse response = new QueryResponse();
        CmisObjectListType objects = new CmisObjectListType();
        response.setObjects(objects);

        // call chemistry implementation
        Repository repository = new NuxeoRepository(repositoryName);

        Connection connection = repository.getConnection(null);
        try {
            ListPage<ObjectEntry> res = connection.getSPI().query(statement,
                    searchAllVersions, inclusion, paging);
            objects.setHasMoreItems(res.getHasMoreItems());
            objects.setNumItems(BigInteger.valueOf(res.getNumItems()));
            List<CmisObjectType> objectList = objects.getObjects();
            for (ObjectEntry entry : res) {
                CmisObjectType object = new CmisObjectType();
                chemistryToWS(entry, object);
                objectList.add(object);
            }
        } finally {
            connection.close();
        }
        return response;
    }

    /**
     * Bridges Chemistry API to WS bindings.
     */
    protected static void chemistryToWS(ObjectEntry entry, CmisObjectType object) {
        CmisPropertiesType properties = new CmisPropertiesType();
        List<CmisProperty> list = properties.getProperty();
        for (Entry<String, Serializable> e : entry.getValues().entrySet()) {
            list.add(getWSCmisProperty(e.getKey(), e.getValue()));
        }
        object.setProperties(properties);
        // object.setAllowableActions(null);
    }

    /**
     * Transforms a Chemistry property into a WS one.
     */
    protected static CmisProperty getWSCmisProperty(String key,
            Serializable value) {
        CmisProperty p;
        PropertyType propertyType = guessType(key, value);
        // boolean multi = false; // TODO
        switch (propertyType.ordinal()) {
        case PropertyType.STRING_ORD:
            p = new CmisPropertyString();
            ((CmisPropertyString) p).getValue().add((String) value);
            break;
        case PropertyType.DECIMAL_ORD:
            p = new CmisPropertyDecimal();
            ((CmisPropertyDecimal) p).getValue().add((BigDecimal) value);
            break;
        case PropertyType.INTEGER_ORD:
            p = new CmisPropertyInteger();
            Long l;
            if (value == null) {
                l = null;
            } else if (value instanceof Long) {
                l = (Long) value;
            } else if (value instanceof Integer) {
                l = Long.valueOf(((Integer) value).longValue());
            } else {
                throw new AssertionError("not a int/long: " + value);
            }
            ((CmisPropertyInteger) p).getValue().add(
                    l == null ? null : BigInteger.valueOf(l.longValue()));
            break;
        case PropertyType.BOOLEAN_ORD:
            p = new CmisPropertyBoolean();
            ((CmisPropertyBoolean) p).getValue().add((Boolean) value);
            break;
        case PropertyType.DATETIME_ORD:
            p = new CmisPropertyDateTime();
            ((CmisPropertyDateTime) p).getValue().add(
                    getXMLGregorianCalendar((Calendar) value));
            break;
        case PropertyType.URI_ORD:
            p = new CmisPropertyUri();
            URI u = (URI) value;
            ((CmisPropertyUri) p).getValue().add(
                    u == null ? null : u.toString());
            break;
        case PropertyType.ID_ORD:
            p = new CmisPropertyId();
            ((CmisPropertyId) p).getValue().add((String) value);
            break;
        case PropertyType.HTML_ORD:
            p = new CmisPropertyHtml();
            // ((CmisPropertyHtml)property).getAny().add(element);
            break;
        default:
            throw new AssertionError();
        }
        p.setPropertyDefinitionId(key);
        return p;

    }

    // TODO XXX we shouldn't guess, values should be typed in ObjectEntry
    protected static PropertyType guessType(String key, Serializable value) {
        for (String n : Arrays.asList( //
                Property.ID, //
                Property.TYPE_ID, //
                Property.BASE_TYPE_ID, //
                Property.VERSION_SERIES_ID, //
                Property.VERSION_SERIES_CHECKED_OUT_ID, //
                Property.PARENT_ID, //
                Property.SOURCE_ID, //
                Property.TARGET_ID)) {
            if (key.toUpperCase().endsWith(n.toUpperCase())) {
                return PropertyType.ID;
            }
        }
        if (value instanceof String) {
            return PropertyType.STRING;
        }
        if (value instanceof BigDecimal) {
            return PropertyType.DECIMAL;
        }
        if (value instanceof Number) {
            return PropertyType.INTEGER;
        }
        if (value instanceof Boolean) {
            return PropertyType.BOOLEAN;
        }
        if (value instanceof Calendar) {
            return PropertyType.DATETIME;
        }
        return PropertyType.STRING;
    }

    protected static DatatypeFactory datatypeFactory;

    protected static XMLGregorianCalendar getXMLGregorianCalendar(
            Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        if (datatypeFactory == null) {
            try {
                datatypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new java.lang.RuntimeException(e);
            }
        }
        return datatypeFactory.newXMLGregorianCalendar((GregorianCalendar) calendar);
    }

}
