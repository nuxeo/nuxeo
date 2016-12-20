
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
  "paths": {"login": "login"},
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
    // a list of operation chains (definition is identical to regular operations)
  ]
}

------------

You can see the automation service is returning the registry of operations and chains available on the server.
Each operation and chain signature is fully described to be able to done operation validation on client side if needed.
Also some additional information that can be used in an UI is provided (operation label, full description , operation category etc.)

The "url" property of an operation (or operation chain) is the relative path to use to execute the operation.
For example if the service URL is http://localhost:8080/automation and the Blob.Attach operation has the url "Blob.Attach" then the full URL to that operation will be:
http://localhost:8080/automation/Blob.Attach

The "paths" property is used to specify various relative paths (relative to the automation service) of services exposed by the automation server.
In the above example you can see that the "login" service is using the relative path "login".
This service can be used to sign-in and check if the username/password is valid. To use this service you should do a POST
to the login URL (e.g. http://localhost:8080/automation/login ) using Basic Authentication. If authentication fails you will receive a 401 HTTP response otherwise the 200 code is returned.

The login service can be used to do preemptive authentication.
Otherwise the login will be done when requested by the server by sending a WWW-Authenticate response.
TODO: WWW-Authenticate is not yet implemented.

*Note* that you should not be logged in to be able to get the service description




2. Executing Operations.
=========================

The operations registry (loaded doing a GET on the automation service URL) contains the entire information you need to execute operations.
To execute an operation you should build an operation request descriptor and post it on the operation URL.
When sending an operation request you must use the "application/json+nxrequest" content type.
Also you need to authenticate (using Basic Authentication) your request since most of the operations are accessing the Nuxeo repository.
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
There are 4 types of supported inputs: void, document, document list, blob, blob list.

To specify a "void" input (i.e. no input) you should omit the input parameter.

To specify a reference to a document you should use the doucment absolute path or document UID prefixed using the string "doc:".
Example: "doc:/default-domain/workspaces/myworkspace" or "doc:/96bfb9cb-a13d-48a2-9bbd-9341fcf24801"

To specify a reference to a list of documents you should use a comma separated list of absolute document paths or UID prefixed by the string "docs"
Example: "docs:/default-domain/workspaces/myworkspace, /96bfb9cb-a13d-48a2-9bbd-9341fcf24801"

When using blobs (files) as input you cannot refer them using a string locator since the blob is usually a file on the client file system or raw binary data.
For example, let say you want to execute the Blob.Attach operation that takes as input a blob and set it on the given document (the document is specified through 'params').
Because the file content you want to set is located on the client computer you cannot use a string reference.
In that case you MUST use a multipart/related request that encapsulate as the root part your JSON request as an "application/json+nxrequest" content and the blob binary content in a related part.
In case you want a list of blobs as input then you simply add one additional content part for each blob in the list.
The only limitation (in both blob and blob list case) is to put the request content part as the first part in the multipart document.
The order of the blob parts will be preserved and blobs will be processed in the same order.
(The server assumes the request part is the first part of the multipart document - e.g. Content-Ids are not used by the server to identify the request part)

Examples
--------

1. Invoking a simple operation:

POST /automation/Document.Fetch HTTP/1.1
Accept: application/json+nxentity, */*
Content-Type: application/json+nxrequest; charset=UTF-8
Authorization: Basic QWRtaW5pc3RyYXRvcjpBZG1pbmlzdHJhdG9y
Host: localhost:8080

{"params":{"value":"/default-domain/workspaces/myws/file"},"context":{}}

This operation will return the document content specified by the "value" parameter.

2. Invoking an operation using as input a blob.
Here is an example on invoking Blob.Attach operation on a document given by its path ("/default-domain/workspaces/myws/file" in our example).


POST /automation/Blob.Attach HTTP/1.1
Accept: application/json+nxentity, */*
Content-Type: multipart/related;
    boundary="----=_Part_0_130438955.1274713628403"; type="application/json+nxrequest"; start="request"
Authorization: Basic QWRtaW5pc3RyYXRvcjpBZG1pbmlzdHJhdG9y
Host: localhost:8080

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

1. document

A JSON document entity contains the minimal required information about the document as top level entries.
These entries are always set on any document and are using the following keys:

- uid   - the document UID
- path  - the document path (in the repository)
- type  - the document type
- state - the current life cycle state
- title - the document title
- lastModifed - the last modified timestamp

All the other document properties are contained within a "properties" map using the property xpath as the key for
the top level entries.
Complex properties are represented as embedded JSON objects and list properties as embedded JSON arrays.

*Note* All "application/json+nxentity" JSON entities always contains a required top level property: "entity-type"
This property is used to identity which type of object is described. There are 3 possible entity types:
- document
- documents
- exception

Example of a JSON document entry

{
  "entity-type": "document",
  "uid": "96bfb9cb-a13d-48a2-9bbd-9341fcf24801",
  "path": "/default-domain/workspaces/myws/file",
  "type": "File",
  "state": "project",
  "title": "file",
  "lastModified": "2010-05-24T15:07:08Z",
  "properties":   {
    "uid:uid": null,
    "uid:minor_version": "0",
    "uid:major_version": "1",
    "dc:creator": "Administrator",
    "dc:contributors": ["Administrator"],
    "dc:source": null,
    "dc:created": "2010-05-22T08:42:56Z",
    "dc:description": "",
    "dc:rights": null,
    "dc:subjects": [],
    "dc:valid": null,
    "dc:format": null,
    "dc:issued": null,
    "dc:modified": "2010-05-24T15:07:08Z",
    "dc:coverage": null,
    "dc:language": null,
    "dc:expired": null,
    "dc:title": "file",
    "files:files": [],
    "common:icon": null,
    "common:icon-expanded": null,
    "file:content":     {
      "name": "test.jpg",
      "mime-type": "image/jpeg",
      "encoding": null,
      "digest": null,
      "length": "290096",
      "data": "files/96bfb9cb-a13d-48a2-9bbd-9341fcf24801?path=%2Fcontent"
    }
  }

The top level properties "title" and "lastModified" have the same value as the corresponding embedded properties "dc:title" and "dc:modified".

*Note* that the blob data instead of containing the raw data contains a relative URL (relative to automation service URL) that can be used
to retrieve the real data of the blob (using a GET request on that URL).

2. documents

The documents JSON entity is a list of JSON document entities and have the entity type "documents".
The documents in the list are containing only the required top level properties.
Example:
{
  entity-type: "documents"
  entries: [
   {
     "entity-type": "document",
     "uid": "96bfb9cb-a13d-48a2-9bbd-9341fcf24801",
     "path": "/default-domain/workspaces/myws/file",
     "type": "File",
     "state": "project",
     "title": "file",
     "lastModified": "2010-05-24T15:07:08Z",
   },
   ...
   ]
 }

 3. exception

 JSON exception entities have a "exception" entity type. and contains information about the exception, including the server stack trace.
 Example:
{
  "entity-type": "exception",
  "type": "org.nuxeo.ecm.automation.OperationException",
  "status": 500,
  "message": "Failed to execute operation: Blob.Attach",
  "stack": "org.nuxeo.ecm.automation.OperationException: Failed to invoke operation Blob.Attach\n\tat org.nuxeo.ecm.automation.core.impl.InvokableMethod.invoke(InvokableMethod.java:143)\n\t ..."
}


Special HTTP headers
=====================

There are two custom HTTP headers that you can use to have more control on how operations are executed.

1. X-NXVoidOperation

Possible values: "true" or "false"
If not specified the default is "false"

This header can be used to force the server to assume that the executed operation has no content to return (a void operation).
This can be very useful when dealing with blobs to avoid having the blob output sent back to the client.
For example if you want to set a blob content on a document using "Blob.Attach" operation after the operation execute
the blob you sent to the server is sent back to the client (because the operation is returning the original blob).
This behavior is useful when creating operation chains but when calling such an operation from remote it will to much network traffic than necesar.
To avoid this use the header:

X-NXVoidOperation: true

Example:

POST /automation/Blob.Attach HTTP/1.1
Accept: application/json+nxentity, */*
Content-Type: multipart/related;
    boundary="----=_Part_0_130438955.1274713628403"; type="application/json+nxrequest"; start="request"
Authorization: Basic QWRtaW5pc3RyYXRvcjpBZG1pbmlzdHJhdG9y
X-NXVoidOperation: true
Host: localhost:8080

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

2. X-NXDocumentProperties

This header can be used whenever a Document will be returned by the server.
The header is forcing the server to fill up the returned document with data from schemas that matches the X-NXDocumentProperties filter.

So, X-NXDocumentProperties is a filter of schemas. If you don't use the header only the minimal required properties of the document are returned.
To have more properties in the returned document specify a list of document schema names:

  X-NXDocumentProperties: dublincore, file

or to have all the document content use the '*' character as the filter value:

  X-NXDocumentProperties: *


Document property types
=======================

Here you can find more details on the JSON document entity format.
- A document entity is using a string value for any scalar property value.
- Dates are encoded as W3C dates (in UTC timezone)
- Apart strings you may have null values for properties that are not set (but are defined by the schema), JSON objects (maps) for complex properties and JSON arrays for array or list properties.
- blob data is encoded as a relative URL (relative to automation service URL) from where you can download the raw data of the blob. (using a GET request on that URL)

Property values can be of one of these types:
- string
- long          - encoded as a string representation of the number (in java Long.toString())
- double        - encoded as a string representation of the number (in java Double.toString())
- date          - encoded as a W3C format (UTC timezone preferred)
- boolean       - "true" or "false"
For null values the JSON null keyword is used.


Operation Request Parameters
=============================

Here you can find more details about the request format.

- Request input

As we seen a request may have as input:
- a document
- a list of documents
- a blob
- a list of blobs

To refer to a document you should use either the absolute path of the document (starting with '/' !) either the document UID.
Example:
input : "/" -> to refer to the repository root
input : "/default-domain/workspaces/ws" - to refer to the 'ws' workspace
input: "96bfb9cb-a13d-48a2-9bbd-9341fcf24801" - to refer to a document havin this UID.

To refer to a list of documents you must use a comma separated list of document identifiers.
Example:
input: "/default-domain/workspaces/ws, 96bfb9cb-a13d-48a2-9bbd-9341fcf24801"

When using a blob as the input of an operation you cannot use the "input" request property since the blob may contain binary data.
In that case you must use a Multipart/Related request as described above and put the blob raw data in the second body part of the request.

To use a list of blobs as the input you should use the same method as for a singl blob and put each blob in the list in a separate body part.
Note that the order of the body parts is signifiant - blobs will be processed in the same order that they appear in the Multipart document.

Also, when using Multipart/Related requests you must always put the JSOn encoded request in the first body part.


- Request parameter types

The operation parameters specified inside the 'params' property of the request are all strings.
Operation parameters are typed so on the server side the operation will know how to decode parameters in real java classes.
The supported parameter types are:
- string, long (integer number), double (floating point number), date, properties, document, documents, EL expressions, EL templates
Here are some rules on how to encode operation parameters:

- string        - let it as is
- long          - just use a string representation of the number (in java Long.toString())
- double        - just use a string representation of the number (in java Double.toString())
- date          - Use W3C format (UTC timezone preferred)
- boolean       - "true" or "false"
- document      - use the document UID or the absolute document path
- documents     - use a comma separated list of document references.
- EL expression - put the "expr:" string before your EL expression. (e.g. "expr: Document.path")
- EL template   - put the "expr:" string before your template (e.g. "expr: SELECT * FROM Document WHERE dc:title=@{my_var}")

Note that expressions also you specify relative paths (relative to the context document) using "expr: ./my/doc".

In fact all these encoding rules are the same as the one you can use when defining operation chains in Nuxeo XML extensions.


Operation chains
==================

You can see operation chains as macro operations. These are chains of atomic operations that are registered on the server.
To extend the default operation set provided by Nuxeo you can either write your own atomic operation etiher define an operation chain through a Nuxeo XML extension.

When defining an operation chain on the server it will become visible in the operation registry returned by the GET request o the automation service.
You must note that operation chains doesn't takes parameters (only an input) because when defining such a chain you also define all the parameters needed by each operation in the chain.
If you need parametrixable parameters (they value are computed at each execution) then use EL expressions as values.


Operation vs. Transactions
===========================
The server run an operation or operation chain in a single transaction. A rollback is done is the operation caused an exception.
The transaction is commited when the operation (or operation chain) succesfully terminate.
Operations are operating in 2 modes: either in the context of a stateful repository session either one session per operation.
By default operations are reusing the same session if your client supports cookies. (even in Basic Authntication).
To enable stateless sessions you need to modify some Nuxeo configuration. This will not be explained here.
In staeless mode the session is closed at the end of the request.
Note: automation service is using Nuxeo WebEngine for HTTP request management.

Operation Security
===================

Some operations are allowed to be executed only by some users/groups. This is defined on the server side through Nuxeo XML extensions.

TODO


JavaScript clients
====================
Automation service is ready to use with java script clients (from a browser)
Browser support will be improved by working on Multipart/form-data encoding support to execute operations.
A GWT client will be done as an example.


Java Clients
====================
A java client is in progress. More details later.


Appendix 1 - Example
=====================

Here is a complete example on using automation service. This example will do:
1. get the automation registry
2. set a blob on an existing document (let say "/default-domain/workspaces/myws/file") by forcing the server to avoid returning back the blob.
3. get the same document with all the data inside (all the schemas)
4. download the content of the blob we set at step 2. (and using the information available in the document retirved at step 3.)



1. Get the automation registry
------------------------------

GET http://localhost:8080/automation
Accept: application/json+nxautomation

You will get response a JSON document like:

REQUEST:

GET /automation HTTP/1.1
Accept: application/json+nxautomation
Host: localhost:8080


RESPONSE:

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
      "params": [ ... ]
    },
    ... // other operation follows here
  ],
  "chains" : [
    // a list of operation chains (definition is identical to regular operations)
  ]
}

2. Upload a blob into a File document: "/default-domain/workspaces/myws/file"
(see the X-NXVoidOperation header to avoid the blob being returned by the server)


REQUEST:

POST /automation/Blob.Attach HTTP/1.1
Accept: application/json+nxentity, */*
Content-Type: multipart/related;
    boundary="----=_Part_0_130438955.1274713628403"; type="application/json+nxrequest"; start="request"
Authorization: Basic QWRtaW5pc3RyYXRvcjpBZG1pbmlzdHJhdG9y
X-NXVoidOperation: true
Host: localhost:8080

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


RESPONSE: 204


3. Get the document data where we uploaded the blob
(see X-NXDocumentProperties header used to specify that all the document data (schemas) should be returned)

REQUEST:

POST /automation/Document.Fetch HTTP/1.1
Accept: application/json+nxentity, */*
Content-Type: application/json+nxrequest; charset=UTF-8
Authorization: Basic QWRtaW5pc3RyYXRvcjpBZG1pbmlzdHJhdG9y
X-NXDocumentProperties: *
Host: localhost:8080

{"params":{"value":"/default-domain/workspaces/myws/file"},"context":{}}



RESPONSE:

HTTP/1.1 200 OK
Content-Type: application/json+nxentity
Content-Length: 1121

{
  "entity-type": "document",
  "uid": "96bfb9cb-a13d-48a2-9bbd-9341fcf24801",
  "path": "/default-domain/workspaces/myws/file",
  "type": "File",
  "state": "project",
  "title": "file",
  "lastModified": "2010-05-24T15:07:08Z",
  "properties":   {
    "uid:uid": null,
    "uid:minor_version": "0",
    "uid:major_version": "1",
    "dc:creator": "Administrator",
    "dc:contributors": ["Administrator"],
    "dc:source": null,
    "dc:created": "2010-05-22T08:42:56Z",
    "dc:description": "",
    "dc:rights": null,
    "dc:subjects": [],
    "dc:valid": null,
    "dc:format": null,
    "dc:issued": null,
    "dc:modified": "2010-05-24T15:07:08Z",
    "dc:coverage": null,
    "dc:language": null,
    "dc:expired": null,
    "dc:title": "file",
    "files:files": [],
    "common:icon": null,
    "common:icon-expanded": null,
    "file:content":     {
      "name": "test.jpg",
      "mime-type": "image/jpeg",
      "encoding": null,
      "digest": null,
      "length": "290096",
      "data": "files/96bfb9cb-a13d-48a2-9bbd-9341fcf24801?path=%2Fcontent"
    }
  }

4. Download the content of the blob we set at step 2.

You notice in the last result that the documents contains our blob and the "data" property points to a relative URL that can be used
to download the blob content.
Let's download it:


REQUEST:

GET /automation/files/96bfb9cb-a13d-48a2-9bbd-9341fcf24801?path=%2Fcontent HTTP/1.1
Authorization: Basic QWRtaW5pc3RyYXRvcjpBZG1pbmlzdHJhdG9y
Host: localhost:8080


RESPONSE:

HTTP/1.1 200 OK
Content-Type: image/jpeg
Content-Length: 290096
Content-Disposition: attachment; filename=test.jpg

[the blob raw data here]

