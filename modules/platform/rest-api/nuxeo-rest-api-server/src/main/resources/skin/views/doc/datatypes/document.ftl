"document" : {
    "id": "document",
    "type": "any",
    "required": false,
    "uniqueItems": false,
    "properties": {
        "entity-type": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "repository": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "uid": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "path": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "type": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "state": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "versionLabel": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "title": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "lastModified": {
            "uniqueItems": false,
            "type": "Date",
            "required": false
        },


        "properties": {
            "uniqueItems": true,
            "type": "container",
            "items":{
              "$ref":"Property"
            }
        },
        "facets": {

            "type": "array",
            "items" : {
              "type":"string"
            },
            "required": false
        },


        "changeToken": {
            "uniqueItems": false,
            "type": "string",
            "required": false
        },
        "contextParameters": {
            "uniqueItems": false,
            "type": "object",
            "required": false
        }

    }

},

 "documents" : {
      "id": "documents",
      "uniqueItems": false,
      "properties": {
        "entity-type": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        <#include "views/doc/datatypes/paginable.ftl"/>,
        "entries": {
          "uniqueItems": false,
          "type": "array",
          "items": {
            "$ref":"Document"
          },
          "required": true
        }

      }
    }
