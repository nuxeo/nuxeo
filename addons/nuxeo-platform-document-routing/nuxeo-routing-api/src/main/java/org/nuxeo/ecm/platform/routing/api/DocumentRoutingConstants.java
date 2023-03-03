/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.api;

/**
 * @author arussel
 */
public interface DocumentRoutingConstants {

    /**
     * @since 2023
     */
    String GC_ROUTES_ACTION_NAME = "garbageCollectWokflows";

    /**
     * @since 2023
     */
    String ALL_WORKFLOWS_QUERY = "SELECT ecm:uuid FROM DocumentRoute";

    /**
     * @since 2023
     */
    String DONE_AND_CANCELED_WORKFLOWS_QUERY = "SELECT ecm:uuid FROM DocumentRoute WHERE ecm:currentLifeCycleState IN ('done', 'canceled')";

    // web
    String SEARCH_ROUTE_BY_ATTACHED_DOC_QUERY = "SEARCH_ROUTE_BY_ATTACHED_DOC";

    /**
     * @since 5.6
     */
    String DOC_ROUTING_SEARCH_ALL_ROUTE_MODELS_PROVIDER_NAME = "DOC_ROUTING_SEARCH_ALL_ROUTE_MODELS";

    /**
     * @since 7.4
     */
    String DOC_ROUTING_SEARCH_ROUTE_MODELS_WITH_TITLE_PROVIDER_NAME = "DOC_ROUTING_SEARCH_ROUTE_MODELS_WITH_TITLE";

    String ROUTE_MANAGERS_GROUP_NAME = "routeManagers";

    // document constant
    String DOCUMENT_ROUTE_INSTANCES_ROOT_DOCUMENT_TYPE = "DocumentRouteInstancesRoot";

    String DOCUMENT_ROUTE_MODELS_ROOT_DOCUMENT_TYPE = "DocumentRouteModelsRoot";

    String DOCUMENT_ROUTE_INSTANCES_ROOT_ID = "document-route-instances-root";

    String DOCUMENT_ROUTE_MODELS_ROOT_ID = "document-route-models-root";

    String DOCUMENT_ROUTE_DOCUMENT_TYPE = "DocumentRoute";

    String DOCUMENT_ROUTE_DOCUMENT_FACET = "DocumentRoute";

    String COMMENTS_INFO_HOLDER_FACET = "CommentsInfoHolder";

    String STEP_DOCUMENT_TYPE = "DocumentRouteStep";

    String STEP_FOLDER_DOCUMENT_TYPE = "StepFolder";

    String STEP_FOLDER_FACET = "StepFolder";

    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Deprecated
    String CONDITIONAL_STEP_FACET = "ConditionalStepFolder";

    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Deprecated
    String CONDITIONAL_STEP_DOCUMENT_TYPE = "ConditionalStepFolder";

    String TITLE_PROPERTY_NAME = "dc:title";

    String DESCRIPTION_PROPERTY_NAME = "dc:description";

    String EXECUTION_TYPE_PROPERTY_NAME = "stepf:execution";

    String INITIATOR = "docri:initiator";

    String ATTACHED_DOCUMENTS_PROPERTY_NAME = "docri:participatingDocuments";

    String COMMENTS_NO_PROPERTY_NAME = "info_comments:numberOfComments";

    // @deprecated since 5.9.2 - Use only routes of type 'graph'
    @Deprecated
    String STEP_TO_BE_EXECUTED_NEXT_PROPERTY_NAME = "condstepf:posOfChildStepToRunNext";

    // operation constant
    String OPERATION_CATEGORY_ROUTING_NAME = "Routing";

    String OPERATION_STEP_DOCUMENT_KEY = "document.routing.step";

    String ROUTE_STEP_FACET = "RouteStep";

    String DOCUMENT_ROUTING_ACL = "routing";

    String DOCUMENT_ROUTING_DELEGATION_ACL = "delegation";

    String WORKFLOW_FORCE_RESUME = "_FORCE_RESUME_";

    /*
     * @since 5.9.3 If this flag is set to Boolean.TRUE in the map of variables to be set on node or workflow, these
     * variables will be decoded to JSON format
     */
    String _MAP_VAR_FORMAT_JSON = "_MAP_VAR_FORMAT_JSON";

    /**
     * @since 5.6
     */
    String TASK_ASSIGNED_NOTIFICATION_TEMPLATE = "taskNotificationTemplate";

    enum ExecutionTypeValues {
        /** Serial running of children nodes. */
        serial,
        /** Parallel running of children nodes. */
        parallel,
        /** Running of the nodes decided by the graph state. */
        graph
    }

    // event
    enum Events {
        /**
         * before the route is validated, each part of the route is in "Draft" state. The session used is unrestricted.
         * The element key is the route.
         */
        beforeRouteValidated,
        /**
         * after the route is validated, each part of the route is in "Validated" state. The session used is
         * unrestricted. The element key is the route.
         */
        afterRouteValidated,
        /**
         * before the route is ready, each part of the route is in "Validated" state.The session used is unrestricted.
         * The element key is the route.
         */
        beforeRouteReady,
        /**
         * after the route is ready, each part of the route is in "Ready" state.The session used is unrestricted. The
         * element key is the route.
         */
        afterRouteReady,
        /**
         * before the route starts. The RouteDocument is in "Running" state, other parts of the route is either in
         * Ready, Running or Done state.The session used is unrestricted. The element key is the route.
         */
        beforeRouteStart,
        /**
         * after the route is finished. The route and each part of the route is in Done state.The session used is
         * unrestricted. The element key is the route.
         */
        afterRouteFinish,
        /**
         * before the operation chain for this step is called. The step is in "Running" state.The session used is
         * unrestricted. The element key is the step.
         */
        beforeStepRunning,
        /**
         * After the operation chain of this step ran and if the step is not done, ie: if we are in a waiting state.The
         * session used is unrestricted. The element key is the step.
         */
        stepWaiting,
        /**
         * after the operation chain for this step is called.The step is in "Done" state.The session used is
         * unrestricted. The element key is the step.
         */
        afterStepRunning,
        /**
         * before a step is put back to ready state.
         */
        beforeStepBackToReady,
        /**
         * after a step was put back to ready state.
         */
        afterStepBackToReady,
        /**
         * before the undo operation is run on the step.
         */
        beforeUndoingStep,
        /**
         * after the undo operation is run on the step.
         */
        afterUndoingStep,
        /**
         * @since 2.7.2
         */
        workflowCanceled,
        /**
         * @since 7.4
         */
        afterWorkflowFinish,
        /**
         * @since 7.4
         */
        afterWorkflowStarted,
        /**
         * @since 7.4
         */
        beforeWorkflowCanceled,
        /**
         * @since 7.4
         */
        afterWorkflowTaskEnded,
        /**
         * @since 7.4
         */
        afterWorkflowTaskCreated,
        /**
         * @since 7.4
         */
        afterWorkflowTaskReassigned,
        /**
         * @since 7.4
         */
        afterWorkflowTaskDelegated
    }

    String DOCUMENT_ELEMENT_EVENT_CONTEXT_KEY = "documentElementEventContextKey";

    String WORKFLOW_TASK_COMPLETION_ACTION_KEY = "workflowTaskCompletionAction";

    String INITIATOR_EVENT_CONTEXT_KEY = "initiator";

    String ROUTING_CATEGORY = "Routing";

    String ROUTING_INITIATOR_ID_KEY = "initiator";

    public static final String TASK_ROUTE_INSTANCE_DOCUMENT_ID_KEY = "routeInstanceDocId";

    public static final String TASK_NODE_ID_KEY = "nodeId";

    public static final String TASK_BUTTONS_KEY = "taskButtons";

    public static final String ROUTING_TASK_FACET_NAME = "RoutingTask";

    /**
     * @since 7.1
     */
    public static final String ROUTING_TASK_DOC_TYPE = "RoutingTask";

    public static final String ROUTE_TASK_LOCAL_ACL = "routingTask";

    /**
     * @since 7.2
     */
    public static final String DOCUMENT_ROUTE_MODEL_LIFECYCLESTATE = "validated";

    /**
     * @since 7.2
     */
    public static final String DOCUMENT_ROUTE_INSTANCE_MODEL_ID = "docri:modelId";

    /**
     * @since 7.2
     */
    public static final String GLOBAL_VAR_SCHEMA_PREFIX = "var_global_";

    /**
     * @since 7.4
     */
    public static final String CAN_DATA_VISU = "DataVisualization";
}
