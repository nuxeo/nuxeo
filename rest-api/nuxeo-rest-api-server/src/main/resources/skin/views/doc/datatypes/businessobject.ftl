"BusinessObject" : {
      "id":"BusinessObject",
      "uniqueItems": false,
      "properties": {
        "entity-type": {
            "uniqueItems": false,
            "type": "string",
            "required": true
        },
        "value": {
          "uniqueItems": false,
          "type": "object"
        }
      }
    },


   "BusinessObjectList" : {
      "id": "BusinessObjectList",
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
            "$ref":"BusinessObject"
          },
          "required": true
        }

      }
    }
