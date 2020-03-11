"docType": {
  "id": "docType",
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "name": {
      "type": "string",
      "required": true
    },
    "parent": {
      "type": "string",
      "required": true
    },
    "facets": {
      "type": "array",
      "required": true,
      "items": {
        "type":"string"
      }
    },
    "schemas": {
      "type": "array",
      "required": true,
      "items": {
        <#include "views/doc/datatypes/schema.ftl"/>
      }
    }
  }
}
