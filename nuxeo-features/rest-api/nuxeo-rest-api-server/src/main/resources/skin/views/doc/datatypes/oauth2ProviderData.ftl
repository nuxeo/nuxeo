"oauth2ProviderData" : {
  "id": "oauth2ProviderData",
  "uniqueItems": false,
  "properties": {
    "serviceName": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "isAvailable": {
      "uniqueItems": false,
      "type": "boolean",
      "required": true
    },
    "clientId": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "authorizationURL": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "isAuthorized": {
      "uniqueItems": false,
      "type": "boolean",
      "required": true
    },
    "userId": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    }
  }
}
