"oauth2TokenData": {
  "id": "oauth2TokenData",
  "uniqueItems": false,
  "properties": {
    "entity-type": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "serviceName": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "nuxeoLogin": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "serviceLogin": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "clientId": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "isShared": {
      "uniqueItems": false,
      "type": "boolean",
      "required": false
    },
    "sharedWith": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "required": false
    },
    "creationDate": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    }
  }
},

"oauth2TokenDataList": {
  "id":"oauth2TokenDataList",
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
        "$ref":"oauth2TokenData"
      },
      "required": true
    }
  }
}
