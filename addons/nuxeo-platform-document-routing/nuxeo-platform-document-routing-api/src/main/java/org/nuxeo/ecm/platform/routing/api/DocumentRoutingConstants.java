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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.api;

/**
 * @author arussel
 *
 */
public interface DocumentRoutingConstants {

    // document constant
    String DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE = "DocumentRouteInstancesRoot";

    String DOCUMENT_ROUTE_INSTANCES_ROOT_ID = "document-route-instances-root";

    String DOCUMENT_ROUTE_DOCUMENT_TYPE = "DocumentRoute";

    String STEP_DOCUMENT_TYPE = "DocumentRouteStep";

    String STEP_FOLDER_DOCUMENT_TYPE = "StepFolder";

    String TITLE_PROPERTY_NAME = "dc:title";

    String DESCRIPTION_PROPERTY_NAME = "dc:description";

    String EXECUTION_TYPE_PROPERTY_NAME = "stepf:execution";

    String ATTACHED_DOCUMENTS_PROPERTY_NAME = "docri:participatingDocuments";

        //operation constant
    String OPERATION_CATEGORY_ROUTING_NAME = "Routing";

    String OPERATION_STEP_DOCUMENT_KEY = "document.routing.step";
    
    String ROUTE_STEP_FACET = "RouteStep";
    
    String DOCUMENT_ROUTING_ACL = "routing";
    
    String STEP_DOCUMENT_DESCRIPTION_TYPE = "Step";
    
    String PARALLEL_STEP_FOLDER_DOCUMENT_DESCRIPTION_TYPE = "Parallel";
    
    String SERIAL_STEP_FOLDER_DOCUMENT_DESCRIPTION_TYPE = "Serial";
    
    enum ExecutionTypeValues {
        serial, parallel
    }

}