"oauth2ProviderData": {
  "id": "oauth2ProviderData",
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
    "description": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "clientId": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "clientSecret": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "authorizationServerURL": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "tokenServerURL": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "userAuthorizationURL": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "scopes": {
      "uniqueItems": false,
      "type": "array",
      "items": {
        "type":"string"
      },
      "required": false
    },
    "isEnabled": {
      "uniqueItems": false,
      "type": "boolean",
      "required": false
    },
    "isAvailable": {
      "uniqueItems": false,
      "type": "boolean",
      "required": false
    },
    "authorizationURL": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    },
    "isAuthorized": {
      "uniqueItems": false,
      "type": "boolean",
      "required": false
    },
    "userId": {
      "uniqueItems": false,
      "type": "string",
      "required": false
    }
  }
},

"oauth2ProviderDataList" : {
  "id":"oauth2ProviderDataList",
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
        "$ref":"oauth2ProviderData"
      },
      "required": true
    }
  }
}
