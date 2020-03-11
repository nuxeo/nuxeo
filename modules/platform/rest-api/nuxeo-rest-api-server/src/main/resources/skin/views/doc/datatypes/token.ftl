"AuthenticationTokenList" : {
  "id": "AuthenticationToken",
  "uniqueItems": false,
  "properties": {
    "token": {
      "uniqueItems": true,
      "type": "string",
      "required": true
    },
    "userName": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    },
    "url": {
      "uniqueItems": false,
      "type": "string",
      "required": true
    }
  }
}
