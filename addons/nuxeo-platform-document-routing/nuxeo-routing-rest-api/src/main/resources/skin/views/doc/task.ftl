<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

     {
      "path": "/task",
      "description": "List tasks",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getTasks",
          "type":"tasks",
          <@params names = ["taskUserIdQueryParam", "taskWorkflowInstanceIdQueryParam", "taskWorkflowModelNameQueryParam", "paging"]/>,
          "summary":"Query tasks by user and workflow ids",
          "notes": "Only task instance which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/task/{taskId}",
      "description": "Get task by its id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getTaskById",
          "type":"task",
          <@params names = ["taskId"]/>,
          "summary":"Get a task by its id",
          "notes": "Only task instance which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/task/{taskId}/{action}",
      "description": "Complete task",
      "operations" : [
        {
          "method":"PUT",
          "nickname":"completeTask",
          <@params names = ["taskId", "taskAction", "taskCompletionRequestBody"]/>,
          "summary":"Complete task",
          "notes": "Complete a workflow related task",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/task/{taskId}/reassign",
      "description": "Reassign task",
      "operations" : [
        {
          "method":"PUT",
          "nickname":"reassignTask",
          <@params names = ["taskId", "actors", "comment"]/>,
          "summary":"Reassign a task",
          "notes": "See https://doc.nuxeo.com/x/1YcZAQ",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/task/{taskId}/delegate",
      "description": "Delegate task",
      "operations" : [
        {
          "method":"PUT",
          "nickname":"delegateTask",
          <@params names = ["taskId", "delegatedActors", "comment"]/>,
          "summary":"Delegate a task",
          "notes": "See https://doc.nuxeo.com/x/34z1",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/id/{docId}/@task",
      "description": "Task adapter",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getDocumentTasks",
          "type":"tasks",
          <@params names = ["docid", "taskUserIdQueryParam", "taskWorkflowInstanceIdQueryParam", "taskWorkflowModelNameQueryParam"]/>,
          "summary":"List tasks of the given document",
          "notes": "Only task instance which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },
    {
      "path": "/path/{docPath}/@task",
      "description": "Task adapter",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getDocumentRelatedWorkflowTasks",
          "type":"tasks",
          <@params names = ["docpath", "taskUserIdQueryParam", "taskWorkflowInstanceIdQueryParam", "taskWorkflowModelNameQueryParam"]/>,
          "summary":"List tasks of the given document",
          "notes": "Only task instance which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    }


</@block>

<@block name="models">
  <#include "views/doc/datatypes/task.ftl"/>
  <#include "views/doc/datatypes/taskCompletionRequest.ftl"/>
</@block>
</@extends>