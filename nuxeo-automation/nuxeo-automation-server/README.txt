
The Nuxeo Automation Server module provides a REST API to easy execute operation on a Nuxeo Server.

To use the REST service you need only to know the URL where the service is exposed and the different format used by the service to exchange information.
All the other URLs that appears in the content exchanged between the client and server are relative paths to the automation service URL.


1. The Automation REST Service.
=================================

This service is bound to the Automation server root. To get the information provided by this service
you should do a GET on the service url using an Accept type of "application/json+nxautomation". You will get in reponse
a the service description as a JSON document.

Example:
Let say the service is bound to http://localhost:8080/automation
Then to get the service description you should do:

GET http://localhost:8080/automation
Accept: application/json+nxautomation

You will get response a JSON document like:

HTTP/1.1 200 OK
Content-Type: application/json+nxautomation
...

{
  "operations": [
    {
      "id" : "Blob.Attach",
      "label": "Attach File",
      "category": "Files",
      "description": "Attach the input file to the document given as a parameter. If the xpath points to a blob list then the blob is appended to the list, otherwise the xpath should point to a blob property. If the save parameter is set the document modification will be automatically saved. Return the blob.",
      "url": "Blob.Attach",
      "signature": [ "blob", "blob" ],
      "params": [
         {
           "name": "document",
            "type": "document",
            "required": true,
            "values": []
         },
         {
           "name": "save",
           "type": "boolean",
           "required": false,
           "values": ["true"]
         },
         {
           "name": "xpath",
           "type": "string",
           "required": false,
           "values": ["file:content"]
         }
      ]


    },
    ... // other operation follows here
  ],
  "chains" : [
    // a list of operation chains (definition is identical to regular operations
  ]
}

------------

You can see the automation service is returning the registry of operations and chains available on the server.
Each operation and chain signature is fully described to be able to done operation validation on client side if needed.
Also some additional information that can be used in an UI is provided (operation label, full description , operation category etc.)

The "url" property of an operation (or operation chain) is the relative path to use to execute the operation.
For example if the service URL is http://localhost:8080/automation and the Blob.Attach operation has the url "Blob.Attach" then the full URL to that operation will be:
http://localhost:8080/automation/Blob.Attach

*Note* that you should not be logged in to be able to get the service description




2. Executing Operations.
=========================

After having loaded the operations registry you have the entire information you need to execute operations.
To execute an operation you should build an operation request descriptor and post it on the operation URL.
When sending an operation request you must use the "application/json+nxrequest" content type.
An operation request is a JSON document having the following format:

{
  input: "the_operation_input_object_reference",
  params: {key1: "value1", key: "value2", ...},
  context: {key1: "val1", ... }
}

All these three request parameters are optional and depend on the executed operation.
If the operation has no input (a void operation) then the input parameter can be omitted.
If the operation has no parameters then 'params' can be omitted.
If the operation does not want to push some specific properties in the operation execution context then context can be omitted. In fact context parameters are useless for now but may be used in future.

The 'input' parameter is a string that acts as a reference to the real object to be used as the input.
For example if you want to use as input a document -> you can put as input value either the document absolute path (starting with /) wither putting the document UID.
When using blobs (files) as input you may not be able to refer to them using a string locator.
For example, let say you want to execute the Blob.Attach operation that takes as input a blob and set it on the given document (the document is specified through 'params').
Because the file content you want to set may be located on the client computer you cannot use a string reference.
In that case you MUST use a multipart/related request that encapsulate as the root part your JSON request as an "application/json+nxrequest" content and the blob binary content in a related part.
In case you want a list of blobs as input then you simply add one additional content part for each blob in the list.
The only limitation (in both blob and blob list case) is to put the request content part as the first part in the multipart document.
The order of the blob parts will be preserved and blobs will be processed in the same order.
(The server assumes the request part is the first part of the multipart document - e.g. Content-Ids are not used by the server to identify the request part)

Examples
--------

1. Invoking a simple operation:


2. Invoking an operation using as input a blob.
Here is an example on invoking Blob.Attach operation on a document given by its path ("/default-domain/workspaces/myws/file" in our example).


POST /automation/Blob.Attach HTTP/1.1
Accept: application/json+nxentity, */*
Content-Type: multipart/related;
    boundary="----=_Part_0_130438955.1274713628403"; type="application/json+nxrequest"; start="request"
Authorization: Basic QWRtaW5pc3RyYXRvcjpBZG1pbmlzdHJhdG9y

------=_Part_0_130438955.1274713628403
Content-Type: application/json+nxrequest; charset=UTF-8
Content-Transfer-Encoding: 8bit
Content-ID: request
Content-Length: 75

{"params":{"document":"/default-domain/workspaces/myws/file"},"context":{}}

------=_Part_0_130438955.1274713628403
Content-Type: image/jpeg
Content-Transfer-Encoding: binary
Content-Disposition: attachment; filename=test.jpg
Content-ID: input

[binary data comes here]

------=_Part_0_130438955.1274713628403--


-------------------------------

In both examples you can see that the following Accept header is used:
Accept: application/json+nxentity, */*

This header is important since is is specifying that the client accept as a response either a JSON encoded entity, either a blob that may have any type (in case of blob download).
The application/json+nxentity is the first content type to help the server choose the format of the response when returning an encoded object.



Operation Execution Response
============================

An Operation can have one of the following output types:
1. document  - a repository document
2. documents - a list of documents
2. blob      - a blob (binary content usually attached to a document)
3. blobs     - a list of blobs
3. void      - the operation has no output (output is void)

Apart these possible outputs of an operation - you can have an operation which is aborting due to an exception.

All these cases are represented using the following HTTP responses:

1. document  -> a JSON object describing the document is returned. The used Content-Type is "application/json+nxentity"
2. documents -> a JSON object describing the document list is returned. The used Content-Type is "application/json+nxentity"
3. blob      -> The blob raw content is returned. The used Content-Type will be the same as the blob mime-type.
4. blobs     -> A Multipart/Mixed content is returned. Each part will be a blob from the list (order is preserved). Each part will use the right Content-Type as given by the blob mime-type.
5. void      -> HTTP 204 is returned. No content and no Content-Type is returned.
6. exception -> A status code > 400 is returned and the content will be the server exception encoded as a JSON object.
                The used Content-Type is "application/json+nxentity".
                In the case of an exception the server try to do this best to return a meaningful status code.
                If no suitable status code is found a generic 500 code (server error) will be used.

You noticed that each time when return objects are encoded as JSON objects the "application/json+nxentity" Content-Type will be used.
We also saw that only document, documents and exception objects are encoded as JSON.
Here we will discuss the JSON format used to encode these objects


