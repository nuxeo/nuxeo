"LogEntry" : {
      "id": "LogEntry",
      "uniqueItems": false,
      "properties": {
        "entity-type": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "category": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "principalName": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "comment": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "docLifeCycle": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "docPath": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "docType": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "docUUID": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "eventId": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "repositoryId": {
          "uniqueItems": false,
          "type": "string",
          "required": true
        },
        "eventDate": {
          "uniqueItems": false,
          "type": "date-time",
          "required": true
        },
        "logDate": {
          "uniqueItems": false,
          "type": "date-time",
          "required": true
        }


      }
    },
    "LogEntries" : {
      "id": "LogEntries",
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
            "$ref":"LogEntry"
          },
          "required": true
        }

      }
    }