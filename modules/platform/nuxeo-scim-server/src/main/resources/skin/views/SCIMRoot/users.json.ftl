
{
  "id":"urn:scim:schemas:core:1.0:User",
  "name":"User",
  "description":"Core User",
  "schema":"urn:scim:schemas:core:1.0",
  "endpoint":"/Users",
  "attributes":[
    {
      "name":"id",
      "type":"string",
      "multiValued":false,
      "description":"Unique identifier for the SCIM resource as defined by the Service Provider. Each representation of the resource MUST include a non-empty id value. This identifier MUST be unique across the Service Provider's entire set of resources. It MUST be a stable, non-reassignable identifier that does not change when the same resource is returned in subsequent requests. The value of the id attribute is always issued by the Service Provider and MUST never be specified by the Service Consumer. REQUIRED.",
      "schema":"urn:scim:schemas:core:1.0",
      "readOnly":true,
      "required":true,
      "caseExact":false
    },
    {
      "name":"name",
      "type":"complex",
      "multiValued":false,
      "description":"The components of the user's real name. Providers MAY return just the full name as a single string in the formatted sub-attribute, or they MAY return just the individual component attributes using the other sub-attributes, or they MAY return both. If both variants are returned, they SHOULD be describing the same name, with the formatted name indicating how the component attributes should be combined.",
      "schema":"urn:scim:schemas:core:1.0",
      "readOnly":false,
      "required":false,
      "caseExact":false,
      "subAttributes":[
        {
          "name":"formatted",
          "type":"string",
          "multiValued":false,
          "description":"The full name, including all middle names, titles, and suffixes as appropriate, formatted for display (e.g. Ms. Barbara J Jensen, III.)." ,
          "readOnly":false,
          "required":false,
          "caseExact":false
        },
        {
          "name":"familyName",
          "type":"string",
          "multiValued":false,
          "description":"The family name of the User, or Last Name in most Western languages (e.g. Jensen given the full name Ms. Barbara J Jensen, III.).",
          "readOnly":false,
          "required":false,
          "caseExact":false
        },
        {
          "name":"givenName",
          "type":"string",
          "multiValued":false,
          "description":"The given name of the User, or First Name in most Western languages (e.g. Barbara given the full name Ms. Barbara J Jensen, III.).",
          "readOnly":false,
          "required":false,
          "caseExact":false
        },
        {
          "name":"middleName",
          "type":"string",
          "multiValued":false,
          "description":"The middle name(s) of the User (e.g. Robert given the full name Ms. Barbara J Jensen, III.).",
          "readOnly":false,
          "required":false,
          "caseExact":false
        },
        {
          "name":"honorificPrefix",
          "type":"string",
          "multiValued":false,
          "description":"The honorific prefix(es) of the User, or Title in most Western languages (e.g. Ms. given the full name Ms. Barbara J Jensen, III.).",
          "readOnly":false,
          "required":false,
          "caseExact":false
        },
        {
          "name":"honorificSuffix",
          "type":"string",
          "multiValued":false,
          "description":"The honorific suffix(es) of the User, or Suffix in most Western languages (e.g. III. given the full name Ms. Barbara J Jensen, III.).",
          "readOnly":false,
          "required":false,
          "caseExact":false
        }
      ]
     },
     {
       "name":"emails",
       "type":"complex",
       "multiValued":true,
       "multiValuedAttributeChildName":"email",
       "description":"E-mail addresses for the user. The value SHOULD be canonicalized by the Service Provider, e.g. bjensen@example.com instead of bjensen@EXAMPLE.COM. Canonical Type values of work, home, and other.",
       "schema":"urn:scim:schemas:core:1.0",
       "readOnly":false,
       "required":false,
       "caseExact":false,
       "subAttributes":[
         {
           "name":"value",
           "type":"string",
           "multiValued":false,
           "description":"E-mail addresses for the user. The value SHOULD be canonicalized by the Service Provider, e.g. bjensen@example.com instead of bjensen@EXAMPLE.COM. Canonical Type values of work, home, and other.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         },
         {
           "name":"display",
           "type":"string",
           "multiValued":false,
           "description":"A human readable name, primarily used for display purposes. READ-ONLY.",
           "readOnly":true,
           "required":false,
           "caseExact":false
         },
         {
           "name":"type",
           "type":"string",
           "multiValued":false,
           "description":"A label indicating the attribute's function; e.g., 'work' or 'home'.",
           "readOnly":false,
           "required":false,
           "caseExact":false,
           "canonicalValues":["work","home","other"]
         },
         {
           "name":"primary",
           "type":"boolean",
           "multiValued":false,
           "description":"A Boolean value indicating the 'primary' or preferred attribute value for this attribute, e.g. the preferred mailing address or primary e-mail address. The primary attribute value 'true' MUST appear no more than once.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         }
       ]
     },
     {
       "name":"addresses",
       "type":"complex",
       "multiValued":true,
       "multiValuedAttributeChildName":"address",
       "description":"A physical mailing address for this User, as described in (address Element). Canonical Type Values of work, home, and other. The value attribute is a complex type with the following sub-attributes.",
       "schema":"urn:scim:schemas:core:1.0",
       "readOnly":false,
       "required":false,
       "caseExact":false,
       "subAttributes":[
         {
           "name":"formatted",
           "type":"string",
           "multiValued":false,
           "description":"The full mailing address, formatted for display or use with a mailing label. This attribute MAY contain newlines.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         },
         {
           "name":"streetAddress",
           "type":"string",
           "multiValued":false,
           "description":"The full street address component, which may include house number, street name, PO BOX, and multi-line extended street address information. This attribute MAY contain newlines.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         },
         {
           "name":"locality",
           "type":"string",
           "multiValued":false,
           "description":"The city or locality component.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         },
         {
           "name":"region",
           "type":"string",
           "multiValued":false,
           "description":"The state or region component.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         },
         {
           "name":"postalCode",
           "type":"string",
           "multiValued":false,
           "description":"The zipcode or postal code component.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         },
         {
           "name":"country",
           "type":"string",
           "multiValued":false,
           "description":"The country name component.",
           "readOnly":false,
           "required":false,
           "caseExact":false
         },
         {
           "name":"type",
           "type":"string",
           "multiValued":false,
           "description":"A label indicating the attribute's function; e.g., 'work' or 'home'.",
           "readOnly":false,
           "required":false,
           "caseExact":false,
           "canonicalValues":["work","home","other"]
         }
       ]
     },
     {
       "name":"employeeNumber",
       "type":"string",
       "multiValued":false,
       "description":"Numeric or alphanumeric identifier assigned to a person, typically based on order of hire or association with an organization.",
       "schema":"urn:scim:schemas:extension:enterprise:1.0",
       "readOnly":false,
       "required":false,
       "caseExact":false
     }
   ]
}