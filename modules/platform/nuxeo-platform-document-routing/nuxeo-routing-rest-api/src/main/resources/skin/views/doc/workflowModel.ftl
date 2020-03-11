<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

     {
      "path": "/workflowModel/{modelName}",
      "description": "Browse workflow model",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getWorkflowModel",
          "type":"workflow",
          <@params names = ["modelName"]/>,
          "summary":"Find a workflow model by its name",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/workflowModel/{modelName}/graph",
      "description": "Get the json serialization of a workflow model graph",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getWorkflowModelGraph",
          <@params names = ["modelName"]/>,
          "type":"workflowGraph",
          "summary":"Get the json serialization of a workflow model graph",
          "notes": "Get the json serialization of a workflow model graph",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]
    },
    {
      "path": "/workflowModel",
      "description": "List workflow",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getWorkflowModels",
          "type":"workflows",
          "summary":"Get the workflow models",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    }

</@block>

<@block name="models">
  <#include "views/doc/datatypes/workflow.ftl"/>
  <#include "views/doc/datatypes/workflowGraph.ftl"/>
</@block>
</@extends>