"oauth2ClientData": {
  "id": "oauth2ClientData",
  "uniqueItems": false,
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "id": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "name": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "isEnabled": {
      "uniqueItems": false,
      "type": "boolean",
      "required": false
    }
  }
},

"oauth2ClientDataList": {
  "id":"oauth2ClientDataList",
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
        "$ref":"oauth2ClientData"
      },
      "required": true
    }
  }
}
