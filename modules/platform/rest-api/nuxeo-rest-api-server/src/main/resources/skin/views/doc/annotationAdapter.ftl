<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
    <@block name="apis">
    {
    "path": "/id/{docId}/@annotation",
    "description": "Create or update an annotation",
    "operations" : [{
    "method":"POST",
    "nickname":"createAnnotation",
    "type":"annotation",
        <@params names = ["docid", "annotationBody"]/>,
    "summary":"Create an annotation on a given document",
        <#include "views/doc/errorresponses.ftl"/>
    },
    {
    "method":"PUT",
    "nickname":"updateAnnotation",
    "type":"annotation",
        <@params names = ["docid", "annotationBody"]/>,
    "summary":"Update an annotation on a given document",
        <#include "views/doc/errorresponses.ftl"/>
    },
    {
    "method":"GET",
    "nickname":"getAnnotations",
    "type":"annotationList",
        <@params names = ["docid", "fieldpath"]/>,
    "summary":"Get all annotations on a given document blob",
        <#include "views/doc/errorresponses.ftl"/>
    }]
    },
    {
    "path": "/id/{docId}/@annotation/{annotationId}",
    "description": "Retrieve or delete an annotation",
    "operations" : [{
    "method":"GET",
    "nickname":"getAnnotation",
    "type":"annotation",
        <@params names = ["docid", "annotationId"]/>,
    "summary":"Retrieve an annotation on a document given its id",
        <#include "views/doc/errorresponses.ftl"/>
    },
    {
    "method":"DELETE",
    "nickname":"deleteAnnotation",
        <@params names = ["docid", "annotationId"]/>,
    "summary":"Delete an annotation on a document given its id",
        <#include "views/doc/errorresponses.ftl"/>
    }]
    }

    </@block>
    <@block name="models">
        <#include "views/doc/datatypes/annotation.ftl"/>
    </@block>

</@extends>