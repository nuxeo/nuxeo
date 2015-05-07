<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

     {
      "path": "/workflow/{workflowInstanceId}",
      "description": "Browse workflow",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getWorkflowInstanceById",
          "type":"workflow",
          <@params names = ["workflowInstanceId"]/>,
          "summary":"Find a workflow instance by its id",
          "notes": "Only workflow instance which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"DELETE",
          "nickname":"deleteWorkflowInstanceById",
          <@params names = ["workflowInstanceId"]/>,
          "summary":"Delete a workflow instance by its id",
          "notes": "Only workflow instance which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/workflow",
      "description": "List workflow",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getRunningWorkflowInstancesLaunchedByCurrentUser",
          "type":"workflows",
          "summary":"Get the workflow instances launched by the current user",
          "notes": "Only workflow instances launched by current user will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"POST",
          "nickname":"createWorkflowInstanceById",
          "type":"workflowRequest",
          <@params names = ["workflowRequestBody"]/>,
          "summary":"Start a workflow instance",
          "notes": "",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/workflow/{workflowInstanceId}/graph",
      "description": "Get the json serialization of a workflow instance graph",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getWorkflowModelGraph",
          <@params names = ["workflowInstanceId"]/>,
          "type":"workflowGraph",
          "summary":"Get the json serialization of a workflow instance graph",
          "notes": "Get the json serialization of a workflow instance graph",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]
    },
    {
      "path": "/id/{docId}/@workflow",
      "description": "Workflow adapter",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getDocumentWorkflow",
          "type":"workflows",
          <@params names = ["docid"]/>,
          "summary":"Get the workflow instances launched on the given document",
          "notes": "Only workflow instances launched by current user will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"POST",
          "nickname":"createWorkflowInstanceOnDocument",
          "type":"workflowRequest",
          <@params names = ["docid", "workflowRequestBody"]/>,
          "summary":"Start a workflow instance on the given document",
          "notes": "",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]
    },
    {
      "path": "/path/{docPath}/@workflow",
      "description": "Workflow adapter",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getDocumentWorkflow",
          "type":"workflows",
          <@params names = ["docpath"]/>,
          "summary":"Get the workflow instances launched on the given document",
          "notes": "Only workflow instances launched by current user will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"POST",
          "nickname":"createWorkflowInstanceOnDocument",
          "type":"workflowRequest",
          <@params names = ["docpath", "workflowRequestBody"]/>,
          "summary":"Start a workflow instance on the given document",
          "notes": "",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    }



</@block>

<@block name="models">
  <#include "views/doc/datatypes/workflow.ftl"/>
  <#include "views/doc/datatypes/workflowRequest.ftl"/>
</@block>
</@extends>