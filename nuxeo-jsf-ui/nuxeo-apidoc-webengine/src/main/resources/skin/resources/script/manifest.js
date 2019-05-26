/*
Language: Apache
Author: Ruslan Keba <rukeba@gmail.com>
Website: http://rukeba.com/
Description: language definition for Apache configuration files (httpd.conf & .htaccess)
Version 1.1
Date: 2008-12-27
*/

hljs.LANGUAGES.manifest =
{
  case_insensitive: true,
  defaultMode: {
    lexems: [hljs.IDENT_RE],
    contains: ['tag', 'builtin', 'attribute'],
    keywords: {
      'keyword': {
        'Bundle-Name:': 1,
        'Bundle-SymbolicName:': 1,
        'Bundle-Version:': 1,
        'Bundle-Vendor:': 1,
        'Nuxeo-Require:': 1,
        'Nuxeo-WebModule:': 1,
        'Manifest-Version:': 1
      },
      'literal': {'on': 1, 'off': 1}
    }
  },
  modes: [
    hljs.HASH_COMMENT_MODE,
    {
      className: 'builtin',
      begin: 'Bundle-', end: ':'
    },
    {
      className: 'attribute',
      begin: 'Nuxeo-', end: ':'
    },
    {
      className: 'tag',
      begin: 'Manifest-', end: ':'
    },
    hljs.QUOTE_STRING_MODE,
    hljs.BACKSLASH_ESCAPE
  ]
};
