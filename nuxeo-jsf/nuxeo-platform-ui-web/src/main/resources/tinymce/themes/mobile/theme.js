(function () {
var mobile = (function () {
  'use strict';

  var noop = function () {
  };
  var noarg = function (f) {
    return function () {
      return f();
    };
  };
  var compose = function (fa, fb) {
    return function () {
      return fa(fb.apply(null, arguments));
    };
  };
  var constant = function (value) {
    return function () {
      return value;
    };
  };
  var identity = function (x) {
    return x;
  };
  var tripleEquals = function (a, b) {
    return a === b;
  };
  var curry = function (f) {
    var args = new Array(arguments.length - 1);
    for (var i = 1; i < arguments.length; i++)
      args[i - 1] = arguments[i];
    return function () {
      var newArgs = new Array(arguments.length);
      for (var j = 0; j < newArgs.length; j++)
        newArgs[j] = arguments[j];
      var all = args.concat(newArgs);
      return f.apply(null, all);
    };
  };
  var not = function (f) {
    return function () {
      return !f.apply(null, arguments);
    };
  };
  var die = function (msg) {
    return function () {
      throw new Error(msg);
    };
  };
  var apply = function (f) {
    return f();
  };
  var call = function (f) {
    f();
  };
  var never = constant(false);
  var always = constant(true);
  var $_aso7c6wjje4cc0om = {
    noop: noop,
    noarg: noarg,
    compose: compose,
    constant: constant,
    identity: identity,
    tripleEquals: tripleEquals,
    curry: curry,
    not: not,
    die: die,
    apply: apply,
    call: call,
    never: never,
    always: always
  };

  var $_g7q1k3wije4cc0oi = {
    contextmenu: $_aso7c6wjje4cc0om.constant('contextmenu'),
    touchstart: $_aso7c6wjje4cc0om.constant('touchstart'),
    touchmove: $_aso7c6wjje4cc0om.constant('touchmove'),
    touchend: $_aso7c6wjje4cc0om.constant('touchend'),
    gesturestart: $_aso7c6wjje4cc0om.constant('gesturestart'),
    mousedown: $_aso7c6wjje4cc0om.constant('mousedown'),
    mousemove: $_aso7c6wjje4cc0om.constant('mousemove'),
    mouseout: $_aso7c6wjje4cc0om.constant('mouseout'),
    mouseup: $_aso7c6wjje4cc0om.constant('mouseup'),
    mouseover: $_aso7c6wjje4cc0om.constant('mouseover'),
    focusin: $_aso7c6wjje4cc0om.constant('focusin'),
    keydown: $_aso7c6wjje4cc0om.constant('keydown'),
    input: $_aso7c6wjje4cc0om.constant('input'),
    change: $_aso7c6wjje4cc0om.constant('change'),
    focus: $_aso7c6wjje4cc0om.constant('focus'),
    click: $_aso7c6wjje4cc0om.constant('click'),
    transitionend: $_aso7c6wjje4cc0om.constant('transitionend'),
    selectstart: $_aso7c6wjje4cc0om.constant('selectstart')
  };

  var cached = function (f) {
    var called = false;
    var r;
    return function () {
      if (!called) {
        called = true;
        r = f.apply(null, arguments);
      }
      return r;
    };
  };
  var $_b57rigwlje4cc0oq = { cached: cached };

  var firstMatch = function (regexes, s) {
    for (var i = 0; i < regexes.length; i++) {
      var x = regexes[i];
      if (x.test(s))
        return x;
    }
    return undefined;
  };
  var find = function (regexes, agent) {
    var r = firstMatch(regexes, agent);
    if (!r)
      return {
        major: 0,
        minor: 0
      };
    var group = function (i) {
      return Number(agent.replace(r, '$' + i));
    };
    return nu(group(1), group(2));
  };
  var detect = function (versionRegexes, agent) {
    var cleanedAgent = String(agent).toLowerCase();
    if (versionRegexes.length === 0)
      return unknown();
    return find(versionRegexes, cleanedAgent);
  };
  var unknown = function () {
    return nu(0, 0);
  };
  var nu = function (major, minor) {
    return {
      major: major,
      minor: minor
    };
  };
  var $_c4t9ivwoje4cc0ov = {
    nu: nu,
    detect: detect,
    unknown: unknown
  };

  var edge = 'Edge';
  var chrome = 'Chrome';
  var ie = 'IE';
  var opera = 'Opera';
  var firefox = 'Firefox';
  var safari = 'Safari';
  var isBrowser = function (name, current) {
    return function () {
      return current === name;
    };
  };
  var unknown$1 = function () {
    return nu$1({
      current: undefined,
      version: $_c4t9ivwoje4cc0ov.unknown()
    });
  };
  var nu$1 = function (info) {
    var current = info.current;
    var version = info.version;
    return {
      current: current,
      version: version,
      isEdge: isBrowser(edge, current),
      isChrome: isBrowser(chrome, current),
      isIE: isBrowser(ie, current),
      isOpera: isBrowser(opera, current),
      isFirefox: isBrowser(firefox, current),
      isSafari: isBrowser(safari, current)
    };
  };
  var $_d5r5z7wnje4cc0os = {
    unknown: unknown$1,
    nu: nu$1,
    edge: $_aso7c6wjje4cc0om.constant(edge),
    chrome: $_aso7c6wjje4cc0om.constant(chrome),
    ie: $_aso7c6wjje4cc0om.constant(ie),
    opera: $_aso7c6wjje4cc0om.constant(opera),
    firefox: $_aso7c6wjje4cc0om.constant(firefox),
    safari: $_aso7c6wjje4cc0om.constant(safari)
  };

  var windows = 'Windows';
  var ios = 'iOS';
  var android = 'Android';
  var linux = 'Linux';
  var osx = 'OSX';
  var solaris = 'Solaris';
  var freebsd = 'FreeBSD';
  var isOS = function (name, current) {
    return function () {
      return current === name;
    };
  };
  var unknown$2 = function () {
    return nu$2({
      current: undefined,
      version: $_c4t9ivwoje4cc0ov.unknown()
    });
  };
  var nu$2 = function (info) {
    var current = info.current;
    var version = info.version;
    return {
      current: current,
      version: version,
      isWindows: isOS(windows, current),
      isiOS: isOS(ios, current),
      isAndroid: isOS(android, current),
      isOSX: isOS(osx, current),
      isLinux: isOS(linux, current),
      isSolaris: isOS(solaris, current),
      isFreeBSD: isOS(freebsd, current)
    };
  };
  var $_56lubhwpje4cc0ow = {
    unknown: unknown$2,
    nu: nu$2,
    windows: $_aso7c6wjje4cc0om.constant(windows),
    ios: $_aso7c6wjje4cc0om.constant(ios),
    android: $_aso7c6wjje4cc0om.constant(android),
    linux: $_aso7c6wjje4cc0om.constant(linux),
    osx: $_aso7c6wjje4cc0om.constant(osx),
    solaris: $_aso7c6wjje4cc0om.constant(solaris),
    freebsd: $_aso7c6wjje4cc0om.constant(freebsd)
  };

  function DeviceType (os, browser, userAgent) {
    var isiPad = os.isiOS() && /ipad/i.test(userAgent) === true;
    var isiPhone = os.isiOS() && !isiPad;
    var isAndroid3 = os.isAndroid() && os.version.major === 3;
    var isAndroid4 = os.isAndroid() && os.version.major === 4;
    var isTablet = isiPad || isAndroid3 || isAndroid4 && /mobile/i.test(userAgent) === true;
    var isTouch = os.isiOS() || os.isAndroid();
    var isPhone = isTouch && !isTablet;
    var iOSwebview = browser.isSafari() && os.isiOS() && /safari/i.test(userAgent) === false;
    return {
      isiPad: $_aso7c6wjje4cc0om.constant(isiPad),
      isiPhone: $_aso7c6wjje4cc0om.constant(isiPhone),
      isTablet: $_aso7c6wjje4cc0om.constant(isTablet),
      isPhone: $_aso7c6wjje4cc0om.constant(isPhone),
      isTouch: $_aso7c6wjje4cc0om.constant(isTouch),
      isAndroid: os.isAndroid,
      isiOS: os.isiOS,
      isWebView: $_aso7c6wjje4cc0om.constant(iOSwebview)
    };
  }

  var never$1 = $_aso7c6wjje4cc0om.never;
  var always$1 = $_aso7c6wjje4cc0om.always;
  var none = function () {
    return NONE;
  };
  var NONE = function () {
    var eq = function (o) {
      return o.isNone();
    };
    var call = function (thunk) {
      return thunk();
    };
    var id = function (n) {
      return n;
    };
    var noop = function () {
    };
    var me = {
      fold: function (n, s) {
        return n();
      },
      is: never$1,
      isSome: never$1,
      isNone: always$1,
      getOr: id,
      getOrThunk: call,
      getOrDie: function (msg) {
        throw new Error(msg || 'error: getOrDie called on none.');
      },
      or: id,
      orThunk: call,
      map: none,
      ap: none,
      each: noop,
      bind: none,
      flatten: none,
      exists: never$1,
      forall: always$1,
      filter: none,
      equals: eq,
      equals_: eq,
      toArray: function () {
        return [];
      },
      toString: $_aso7c6wjje4cc0om.constant('none()')
    };
    if (Object.freeze)
      Object.freeze(me);
    return me;
  }();
  var some = function (a) {
    var constant_a = function () {
      return a;
    };
    var self = function () {
      return me;
    };
    var map = function (f) {
      return some(f(a));
    };
    var bind = function (f) {
      return f(a);
    };
    var me = {
      fold: function (n, s) {
        return s(a);
      },
      is: function (v) {
        return a === v;
      },
      isSome: always$1,
      isNone: never$1,
      getOr: constant_a,
      getOrThunk: constant_a,
      getOrDie: constant_a,
      or: self,
      orThunk: self,
      map: map,
      ap: function (optfab) {
        return optfab.fold(none, function (fab) {
          return some(fab(a));
        });
      },
      each: function (f) {
        f(a);
      },
      bind: bind,
      flatten: constant_a,
      exists: bind,
      forall: bind,
      filter: function (f) {
        return f(a) ? me : NONE;
      },
      equals: function (o) {
        return o.is(a);
      },
      equals_: function (o, elementEq) {
        return o.fold(never$1, function (b) {
          return elementEq(a, b);
        });
      },
      toArray: function () {
        return [a];
      },
      toString: function () {
        return 'some(' + a + ')';
      }
    };
    return me;
  };
  var from = function (value) {
    return value === null || value === undefined ? NONE : some(value);
  };
  var Option = {
    some: some,
    none: none,
    from: from
  };

  var rawIndexOf = function () {
    var pIndexOf = Array.prototype.indexOf;
    var fastIndex = function (xs, x) {
      return pIndexOf.call(xs, x);
    };
    var slowIndex = function (xs, x) {
      return slowIndexOf(xs, x);
    };
    return pIndexOf === undefined ? slowIndex : fastIndex;
  }();
  var indexOf = function (xs, x) {
    var r = rawIndexOf(xs, x);
    return r === -1 ? Option.none() : Option.some(r);
  };
  var contains = function (xs, x) {
    return rawIndexOf(xs, x) > -1;
  };
  var exists = function (xs, pred) {
    return findIndex(xs, pred).isSome();
  };
  var range = function (num, f) {
    var r = [];
    for (var i = 0; i < num; i++) {
      r.push(f(i));
    }
    return r;
  };
  var chunk = function (array, size) {
    var r = [];
    for (var i = 0; i < array.length; i += size) {
      var s = array.slice(i, i + size);
      r.push(s);
    }
    return r;
  };
  var map = function (xs, f) {
    var len = xs.length;
    var r = new Array(len);
    for (var i = 0; i < len; i++) {
      var x = xs[i];
      r[i] = f(x, i, xs);
    }
    return r;
  };
  var each = function (xs, f) {
    for (var i = 0, len = xs.length; i < len; i++) {
      var x = xs[i];
      f(x, i, xs);
    }
  };
  var eachr = function (xs, f) {
    for (var i = xs.length - 1; i >= 0; i--) {
      var x = xs[i];
      f(x, i, xs);
    }
  };
  var partition = function (xs, pred) {
    var pass = [];
    var fail = [];
    for (var i = 0, len = xs.length; i < len; i++) {
      var x = xs[i];
      var arr = pred(x, i, xs) ? pass : fail;
      arr.push(x);
    }
    return {
      pass: pass,
      fail: fail
    };
  };
  var filter = function (xs, pred) {
    var r = [];
    for (var i = 0, len = xs.length; i < len; i++) {
      var x = xs[i];
      if (pred(x, i, xs)) {
        r.push(x);
      }
    }
    return r;
  };
  var groupBy = function (xs, f) {
    if (xs.length === 0) {
      return [];
    } else {
      var wasType = f(xs[0]);
      var r = [];
      var group = [];
      for (var i = 0, len = xs.length; i < len; i++) {
        var x = xs[i];
        var type = f(x);
        if (type !== wasType) {
          r.push(group);
          group = [];
        }
        wasType = type;
        group.push(x);
      }
      if (group.length !== 0) {
        r.push(group);
      }
      return r;
    }
  };
  var foldr = function (xs, f, acc) {
    eachr(xs, function (x) {
      acc = f(acc, x);
    });
    return acc;
  };
  var foldl = function (xs, f, acc) {
    each(xs, function (x) {
      acc = f(acc, x);
    });
    return acc;
  };
  var find$1 = function (xs, pred) {
    for (var i = 0, len = xs.length; i < len; i++) {
      var x = xs[i];
      if (pred(x, i, xs)) {
        return Option.some(x);
      }
    }
    return Option.none();
  };
  var findIndex = function (xs, pred) {
    for (var i = 0, len = xs.length; i < len; i++) {
      var x = xs[i];
      if (pred(x, i, xs)) {
        return Option.some(i);
      }
    }
    return Option.none();
  };
  var slowIndexOf = function (xs, x) {
    for (var i = 0, len = xs.length; i < len; ++i) {
      if (xs[i] === x) {
        return i;
      }
    }
    return -1;
  };
  var push = Array.prototype.push;
  var flatten = function (xs) {
    var r = [];
    for (var i = 0, len = xs.length; i < len; ++i) {
      if (!Array.prototype.isPrototypeOf(xs[i]))
        throw new Error('Arr.flatten item ' + i + ' was not an array, input: ' + xs);
      push.apply(r, xs[i]);
    }
    return r;
  };
  var bind = function (xs, f) {
    var output = map(xs, f);
    return flatten(output);
  };
  var forall = function (xs, pred) {
    for (var i = 0, len = xs.length; i < len; ++i) {
      var x = xs[i];
      if (pred(x, i, xs) !== true) {
        return false;
      }
    }
    return true;
  };
  var equal = function (a1, a2) {
    return a1.length === a2.length && forall(a1, function (x, i) {
      return x === a2[i];
    });
  };
  var slice = Array.prototype.slice;
  var reverse = function (xs) {
    var r = slice.call(xs, 0);
    r.reverse();
    return r;
  };
  var difference = function (a1, a2) {
    return filter(a1, function (x) {
      return !contains(a2, x);
    });
  };
  var mapToObject = function (xs, f) {
    var r = {};
    for (var i = 0, len = xs.length; i < len; i++) {
      var x = xs[i];
      r[String(x)] = f(x, i);
    }
    return r;
  };
  var pure = function (x) {
    return [x];
  };
  var sort = function (xs, comparator) {
    var copy = slice.call(xs, 0);
    copy.sort(comparator);
    return copy;
  };
  var head = function (xs) {
    return xs.length === 0 ? Option.none() : Option.some(xs[0]);
  };
  var last = function (xs) {
    return xs.length === 0 ? Option.none() : Option.some(xs[xs.length - 1]);
  };
  var $_elh0pqwsje4cc0p4 = {
    map: map,
    each: each,
    eachr: eachr,
    partition: partition,
    filter: filter,
    groupBy: groupBy,
    indexOf: indexOf,
    foldr: foldr,
    foldl: foldl,
    find: find$1,
    findIndex: findIndex,
    flatten: flatten,
    bind: bind,
    forall: forall,
    exists: exists,
    contains: contains,
    equal: equal,
    reverse: reverse,
    chunk: chunk,
    difference: difference,
    mapToObject: mapToObject,
    pure: pure,
    sort: sort,
    range: range,
    head: head,
    last: last
  };

  var detect$1 = function (candidates, userAgent) {
    var agent = String(userAgent).toLowerCase();
    return $_elh0pqwsje4cc0p4.find(candidates, function (candidate) {
      return candidate.search(agent);
    });
  };
  var detectBrowser = function (browsers, userAgent) {
    return detect$1(browsers, userAgent).map(function (browser) {
      var version = $_c4t9ivwoje4cc0ov.detect(browser.versionRegexes, userAgent);
      return {
        current: browser.name,
        version: version
      };
    });
  };
  var detectOs = function (oses, userAgent) {
    return detect$1(oses, userAgent).map(function (os) {
      var version = $_c4t9ivwoje4cc0ov.detect(os.versionRegexes, userAgent);
      return {
        current: os.name,
        version: version
      };
    });
  };
  var $_4e6av8wrje4cc0p1 = {
    detectBrowser: detectBrowser,
    detectOs: detectOs
  };

  var addToStart = function (str, prefix) {
    return prefix + str;
  };
  var addToEnd = function (str, suffix) {
    return str + suffix;
  };
  var removeFromStart = function (str, numChars) {
    return str.substring(numChars);
  };
  var removeFromEnd = function (str, numChars) {
    return str.substring(0, str.length - numChars);
  };
  var $_3g1eniwwje4cc0pr = {
    addToStart: addToStart,
    addToEnd: addToEnd,
    removeFromStart: removeFromStart,
    removeFromEnd: removeFromEnd
  };

  var first = function (str, count) {
    return str.substr(0, count);
  };
  var last$1 = function (str, count) {
    return str.substr(str.length - count, str.length);
  };
  var head$1 = function (str) {
    return str === '' ? Option.none() : Option.some(str.substr(0, 1));
  };
  var tail = function (str) {
    return str === '' ? Option.none() : Option.some(str.substring(1));
  };
  var $_ealqpawxje4cc0ps = {
    first: first,
    last: last$1,
    head: head$1,
    tail: tail
  };

  var checkRange = function (str, substr, start) {
    if (substr === '')
      return true;
    if (str.length < substr.length)
      return false;
    var x = str.substr(start, start + substr.length);
    return x === substr;
  };
  var supplant = function (str, obj) {
    var isStringOrNumber = function (a) {
      var t = typeof a;
      return t === 'string' || t === 'number';
    };
    return str.replace(/\${([^{}]*)}/g, function (a, b) {
      var value = obj[b];
      return isStringOrNumber(value) ? value : a;
    });
  };
  var removeLeading = function (str, prefix) {
    return startsWith(str, prefix) ? $_3g1eniwwje4cc0pr.removeFromStart(str, prefix.length) : str;
  };
  var removeTrailing = function (str, prefix) {
    return endsWith(str, prefix) ? $_3g1eniwwje4cc0pr.removeFromEnd(str, prefix.length) : str;
  };
  var ensureLeading = function (str, prefix) {
    return startsWith(str, prefix) ? str : $_3g1eniwwje4cc0pr.addToStart(str, prefix);
  };
  var ensureTrailing = function (str, prefix) {
    return endsWith(str, prefix) ? str : $_3g1eniwwje4cc0pr.addToEnd(str, prefix);
  };
  var contains$1 = function (str, substr) {
    return str.indexOf(substr) !== -1;
  };
  var capitalize = function (str) {
    return $_ealqpawxje4cc0ps.head(str).bind(function (head) {
      return $_ealqpawxje4cc0ps.tail(str).map(function (tail) {
        return head.toUpperCase() + tail;
      });
    }).getOr(str);
  };
  var startsWith = function (str, prefix) {
    return checkRange(str, prefix, 0);
  };
  var endsWith = function (str, suffix) {
    return checkRange(str, suffix, str.length - suffix.length);
  };
  var trim = function (str) {
    return str.replace(/^\s+|\s+$/g, '');
  };
  var lTrim = function (str) {
    return str.replace(/^\s+/g, '');
  };
  var rTrim = function (str) {
    return str.replace(/\s+$/g, '');
  };
  var $_eehsrpwvje4cc0pp = {
    supplant: supplant,
    startsWith: startsWith,
    removeLeading: removeLeading,
    removeTrailing: removeTrailing,
    ensureLeading: ensureLeading,
    ensureTrailing: ensureTrailing,
    endsWith: endsWith,
    contains: contains$1,
    trim: trim,
    lTrim: lTrim,
    rTrim: rTrim,
    capitalize: capitalize
  };

  var normalVersionRegex = /.*?version\/\ ?([0-9]+)\.([0-9]+).*/;
  var checkContains = function (target) {
    return function (uastring) {
      return $_eehsrpwvje4cc0pp.contains(uastring, target);
    };
  };
  var browsers = [
    {
      name: 'Edge',
      versionRegexes: [/.*?edge\/ ?([0-9]+)\.([0-9]+)$/],
      search: function (uastring) {
        var monstrosity = $_eehsrpwvje4cc0pp.contains(uastring, 'edge/') && $_eehsrpwvje4cc0pp.contains(uastring, 'chrome') && $_eehsrpwvje4cc0pp.contains(uastring, 'safari') && $_eehsrpwvje4cc0pp.contains(uastring, 'applewebkit');
        return monstrosity;
      }
    },
    {
      name: 'Chrome',
      versionRegexes: [
        /.*?chrome\/([0-9]+)\.([0-9]+).*/,
        normalVersionRegex
      ],
      search: function (uastring) {
        return $_eehsrpwvje4cc0pp.contains(uastring, 'chrome') && !$_eehsrpwvje4cc0pp.contains(uastring, 'chromeframe');
      }
    },
    {
      name: 'IE',
      versionRegexes: [
        /.*?msie\ ?([0-9]+)\.([0-9]+).*/,
        /.*?rv:([0-9]+)\.([0-9]+).*/
      ],
      search: function (uastring) {
        return $_eehsrpwvje4cc0pp.contains(uastring, 'msie') || $_eehsrpwvje4cc0pp.contains(uastring, 'trident');
      }
    },
    {
      name: 'Opera',
      versionRegexes: [
        normalVersionRegex,
        /.*?opera\/([0-9]+)\.([0-9]+).*/
      ],
      search: checkContains('opera')
    },
    {
      name: 'Firefox',
      versionRegexes: [/.*?firefox\/\ ?([0-9]+)\.([0-9]+).*/],
      search: checkContains('firefox')
    },
    {
      name: 'Safari',
      versionRegexes: [
        normalVersionRegex,
        /.*?cpu os ([0-9]+)_([0-9]+).*/
      ],
      search: function (uastring) {
        return ($_eehsrpwvje4cc0pp.contains(uastring, 'safari') || $_eehsrpwvje4cc0pp.contains(uastring, 'mobile/')) && $_eehsrpwvje4cc0pp.contains(uastring, 'applewebkit');
      }
    }
  ];
  var oses = [
    {
      name: 'Windows',
      search: checkContains('win'),
      versionRegexes: [/.*?windows\ nt\ ?([0-9]+)\.([0-9]+).*/]
    },
    {
      name: 'iOS',
      search: function (uastring) {
        return $_eehsrpwvje4cc0pp.contains(uastring, 'iphone') || $_eehsrpwvje4cc0pp.contains(uastring, 'ipad');
      },
      versionRegexes: [
        /.*?version\/\ ?([0-9]+)\.([0-9]+).*/,
        /.*cpu os ([0-9]+)_([0-9]+).*/,
        /.*cpu iphone os ([0-9]+)_([0-9]+).*/
      ]
    },
    {
      name: 'Android',
      search: checkContains('android'),
      versionRegexes: [/.*?android\ ?([0-9]+)\.([0-9]+).*/]
    },
    {
      name: 'OSX',
      search: checkContains('os x'),
      versionRegexes: [/.*?os\ x\ ?([0-9]+)_([0-9]+).*/]
    },
    {
      name: 'Linux',
      search: checkContains('linux'),
      versionRegexes: []
    },
    {
      name: 'Solaris',
      search: checkContains('sunos'),
      versionRegexes: []
    },
    {
      name: 'FreeBSD',
      search: checkContains('freebsd'),
      versionRegexes: []
    }
  ];
  var $_8vp9nwuje4cc0pk = {
    browsers: $_aso7c6wjje4cc0om.constant(browsers),
    oses: $_aso7c6wjje4cc0om.constant(oses)
  };

  var detect$2 = function (userAgent) {
    var browsers = $_8vp9nwuje4cc0pk.browsers();
    var oses = $_8vp9nwuje4cc0pk.oses();
    var browser = $_4e6av8wrje4cc0p1.detectBrowser(browsers, userAgent).fold($_d5r5z7wnje4cc0os.unknown, $_d5r5z7wnje4cc0os.nu);
    var os = $_4e6av8wrje4cc0p1.detectOs(oses, userAgent).fold($_56lubhwpje4cc0ow.unknown, $_56lubhwpje4cc0ow.nu);
    var deviceType = DeviceType(os, browser, userAgent);
    return {
      browser: browser,
      os: os,
      deviceType: deviceType
    };
  };
  var $_d36p3swmje4cc0or = { detect: detect$2 };

  var detect$3 = $_b57rigwlje4cc0oq.cached(function () {
    var userAgent = navigator.userAgent;
    return $_d36p3swmje4cc0or.detect(userAgent);
  });
  var $_2j4x3gwkje4cc0oo = { detect: detect$3 };

  var alloy = { tap: $_aso7c6wjje4cc0om.constant('alloy.tap') };
  var $_g6tooswhje4cc0of = {
    focus: $_aso7c6wjje4cc0om.constant('alloy.focus'),
    postBlur: $_aso7c6wjje4cc0om.constant('alloy.blur.post'),
    receive: $_aso7c6wjje4cc0om.constant('alloy.receive'),
    execute: $_aso7c6wjje4cc0om.constant('alloy.execute'),
    focusItem: $_aso7c6wjje4cc0om.constant('alloy.focus.item'),
    tap: alloy.tap,
    tapOrClick: $_2j4x3gwkje4cc0oo.detect().deviceType.isTouch() ? alloy.tap : $_g7q1k3wije4cc0oi.click,
    longpress: $_aso7c6wjje4cc0om.constant('alloy.longpress'),
    sandboxClose: $_aso7c6wjje4cc0om.constant('alloy.sandbox.close'),
    systemInit: $_aso7c6wjje4cc0om.constant('alloy.system.init'),
    windowScroll: $_aso7c6wjje4cc0om.constant('alloy.system.scroll'),
    attachedToDom: $_aso7c6wjje4cc0om.constant('alloy.system.attached'),
    detachedFromDom: $_aso7c6wjje4cc0om.constant('alloy.system.detached'),
    changeTab: $_aso7c6wjje4cc0om.constant('alloy.change.tab'),
    dismissTab: $_aso7c6wjje4cc0om.constant('alloy.dismiss.tab')
  };

  var typeOf = function (x) {
    if (x === null)
      return 'null';
    var t = typeof x;
    if (t === 'object' && Array.prototype.isPrototypeOf(x))
      return 'array';
    if (t === 'object' && String.prototype.isPrototypeOf(x))
      return 'string';
    return t;
  };
  var isType = function (type) {
    return function (value) {
      return typeOf(value) === type;
    };
  };
  var $_eh3yfywzje4cc0pu = {
    isString: isType('string'),
    isObject: isType('object'),
    isArray: isType('array'),
    isNull: isType('null'),
    isBoolean: isType('boolean'),
    isUndefined: isType('undefined'),
    isFunction: isType('function'),
    isNumber: isType('number')
  };

  var shallow = function (old, nu) {
    return nu;
  };
  var deep = function (old, nu) {
    var bothObjects = $_eh3yfywzje4cc0pu.isObject(old) && $_eh3yfywzje4cc0pu.isObject(nu);
    return bothObjects ? deepMerge(old, nu) : nu;
  };
  var baseMerge = function (merger) {
    return function () {
      var objects = new Array(arguments.length);
      for (var i = 0; i < objects.length; i++)
        objects[i] = arguments[i];
      if (objects.length === 0)
        throw new Error('Can\'t merge zero objects');
      var ret = {};
      for (var j = 0; j < objects.length; j++) {
        var curObject = objects[j];
        for (var key in curObject)
          if (curObject.hasOwnProperty(key)) {
            ret[key] = merger(ret[key], curObject[key]);
          }
      }
      return ret;
    };
  };
  var deepMerge = baseMerge(deep);
  var merge = baseMerge(shallow);
  var $_dax12xwyje4cc0pt = {
    deepMerge: deepMerge,
    merge: merge
  };

  var keys = function () {
    var fastKeys = Object.keys;
    var slowKeys = function (o) {
      var r = [];
      for (var i in o) {
        if (o.hasOwnProperty(i)) {
          r.push(i);
        }
      }
      return r;
    };
    return fastKeys === undefined ? slowKeys : fastKeys;
  }();
  var each$1 = function (obj, f) {
    var props = keys(obj);
    for (var k = 0, len = props.length; k < len; k++) {
      var i = props[k];
      var x = obj[i];
      f(x, i, obj);
    }
  };
  var objectMap = function (obj, f) {
    return tupleMap(obj, function (x, i, obj) {
      return {
        k: i,
        v: f(x, i, obj)
      };
    });
  };
  var tupleMap = function (obj, f) {
    var r = {};
    each$1(obj, function (x, i) {
      var tuple = f(x, i, obj);
      r[tuple.k] = tuple.v;
    });
    return r;
  };
  var bifilter = function (obj, pred) {
    var t = {};
    var f = {};
    each$1(obj, function (x, i) {
      var branch = pred(x, i) ? t : f;
      branch[i] = x;
    });
    return {
      t: t,
      f: f
    };
  };
  var mapToArray = function (obj, f) {
    var r = [];
    each$1(obj, function (value, name) {
      r.push(f(value, name));
    });
    return r;
  };
  var find$2 = function (obj, pred) {
    var props = keys(obj);
    for (var k = 0, len = props.length; k < len; k++) {
      var i = props[k];
      var x = obj[i];
      if (pred(x, i, obj)) {
        return Option.some(x);
      }
    }
    return Option.none();
  };
  var values = function (obj) {
    return mapToArray(obj, function (v) {
      return v;
    });
  };
  var size = function (obj) {
    return values(obj).length;
  };
  var $_i587hx0je4cc0pw = {
    bifilter: bifilter,
    each: each$1,
    map: objectMap,
    mapToArray: mapToArray,
    tupleMap: tupleMap,
    find: find$2,
    keys: keys,
    values: values,
    size: size
  };

  var emit = function (component, event) {
    dispatchWith(component, component.element(), event, {});
  };
  var emitWith = function (component, event, properties) {
    dispatchWith(component, component.element(), event, properties);
  };
  var emitExecute = function (component) {
    emit(component, $_g6tooswhje4cc0of.execute());
  };
  var dispatch = function (component, target, event) {
    dispatchWith(component, target, event, {});
  };
  var dispatchWith = function (component, target, event, properties) {
    var data = $_dax12xwyje4cc0pt.deepMerge({ target: target }, properties);
    component.getSystem().triggerEvent(event, target, $_i587hx0je4cc0pw.map(data, $_aso7c6wjje4cc0om.constant));
  };
  var dispatchEvent = function (component, target, event, simulatedEvent) {
    component.getSystem().triggerEvent(event, target, simulatedEvent.event());
  };
  var dispatchFocus = function (component, target) {
    component.getSystem().triggerFocus(target, component.element());
  };
  var $_3fionhwgje4cc0o9 = {
    emit: emit,
    emitWith: emitWith,
    emitExecute: emitExecute,
    dispatch: dispatch,
    dispatchWith: dispatchWith,
    dispatchEvent: dispatchEvent,
    dispatchFocus: dispatchFocus
  };

  function Immutable () {
    var fields = arguments;
    return function () {
      var values = new Array(arguments.length);
      for (var i = 0; i < values.length; i++)
        values[i] = arguments[i];
      if (fields.length !== values.length)
        throw new Error('Wrong number of arguments to struct. Expected "[' + fields.length + ']", got ' + values.length + ' arguments');
      var struct = {};
      $_elh0pqwsje4cc0p4.each(fields, function (name, i) {
        struct[name] = $_aso7c6wjje4cc0om.constant(values[i]);
      });
      return struct;
    };
  }

  var sort$1 = function (arr) {
    return arr.slice(0).sort();
  };
  var reqMessage = function (required, keys) {
    throw new Error('All required keys (' + sort$1(required).join(', ') + ') were not specified. Specified keys were: ' + sort$1(keys).join(', ') + '.');
  };
  var unsuppMessage = function (unsupported) {
    throw new Error('Unsupported keys for object: ' + sort$1(unsupported).join(', '));
  };
  var validateStrArr = function (label, array) {
    if (!$_eh3yfywzje4cc0pu.isArray(array))
      throw new Error('The ' + label + ' fields must be an array. Was: ' + array + '.');
    $_elh0pqwsje4cc0p4.each(array, function (a) {
      if (!$_eh3yfywzje4cc0pu.isString(a))
        throw new Error('The value ' + a + ' in the ' + label + ' fields was not a string.');
    });
  };
  var invalidTypeMessage = function (incorrect, type) {
    throw new Error('All values need to be of type: ' + type + '. Keys (' + sort$1(incorrect).join(', ') + ') were not.');
  };
  var checkDupes = function (everything) {
    var sorted = sort$1(everything);
    var dupe = $_elh0pqwsje4cc0p4.find(sorted, function (s, i) {
      return i < sorted.length - 1 && s === sorted[i + 1];
    });
    dupe.each(function (d) {
      throw new Error('The field: ' + d + ' occurs more than once in the combined fields: [' + sorted.join(', ') + '].');
    });
  };
  var $_g7vdlvx7je4cc0qn = {
    sort: sort$1,
    reqMessage: reqMessage,
    unsuppMessage: unsuppMessage,
    validateStrArr: validateStrArr,
    invalidTypeMessage: invalidTypeMessage,
    checkDupes: checkDupes
  };

  function MixedBag (required, optional) {
    var everything = required.concat(optional);
    if (everything.length === 0)
      throw new Error('You must specify at least one required or optional field.');
    $_g7vdlvx7je4cc0qn.validateStrArr('required', required);
    $_g7vdlvx7je4cc0qn.validateStrArr('optional', optional);
    $_g7vdlvx7je4cc0qn.checkDupes(everything);
    return function (obj) {
      var keys = $_i587hx0je4cc0pw.keys(obj);
      var allReqd = $_elh0pqwsje4cc0p4.forall(required, function (req) {
        return $_elh0pqwsje4cc0p4.contains(keys, req);
      });
      if (!allReqd)
        $_g7vdlvx7je4cc0qn.reqMessage(required, keys);
      var unsupported = $_elh0pqwsje4cc0p4.filter(keys, function (key) {
        return !$_elh0pqwsje4cc0p4.contains(everything, key);
      });
      if (unsupported.length > 0)
        $_g7vdlvx7je4cc0qn.unsuppMessage(unsupported);
      var r = {};
      $_elh0pqwsje4cc0p4.each(required, function (req) {
        r[req] = $_aso7c6wjje4cc0om.constant(obj[req]);
      });
      $_elh0pqwsje4cc0p4.each(optional, function (opt) {
        r[opt] = $_aso7c6wjje4cc0om.constant(Object.prototype.hasOwnProperty.call(obj, opt) ? Option.some(obj[opt]) : Option.none());
      });
      return r;
    };
  }

  var $_ajn35gx4je4cc0qj = {
    immutable: Immutable,
    immutableBag: MixedBag
  };

  var toArray = function (target, f) {
    var r = [];
    var recurse = function (e) {
      r.push(e);
      return f(e);
    };
    var cur = f(target);
    do {
      cur = cur.bind(recurse);
    } while (cur.isSome());
    return r;
  };
  var $_g6q5ktx8je4cc0qp = { toArray: toArray };

  var global = typeof window !== 'undefined' ? window : Function('return this;')();

  var path = function (parts, scope) {
    var o = scope !== undefined && scope !== null ? scope : global;
    for (var i = 0; i < parts.length && o !== undefined && o !== null; ++i)
      o = o[parts[i]];
    return o;
  };
  var resolve = function (p, scope) {
    var parts = p.split('.');
    return path(parts, scope);
  };
  var step = function (o, part) {
    if (o[part] === undefined || o[part] === null)
      o[part] = {};
    return o[part];
  };
  var forge = function (parts, target) {
    var o = target !== undefined ? target : global;
    for (var i = 0; i < parts.length; ++i)
      o = step(o, parts[i]);
    return o;
  };
  var namespace = function (name, target) {
    var parts = name.split('.');
    return forge(parts, target);
  };
  var $_6z91r3xcje4cc0qy = {
    path: path,
    resolve: resolve,
    forge: forge,
    namespace: namespace
  };

  var unsafe = function (name, scope) {
    return $_6z91r3xcje4cc0qy.resolve(name, scope);
  };
  var getOrDie = function (name, scope) {
    var actual = unsafe(name, scope);
    if (actual === undefined || actual === null)
      throw name + ' not available on this browser';
    return actual;
  };
  var $_6a52kwxbje4cc0qw = { getOrDie: getOrDie };

  var node = function () {
    var f = $_6a52kwxbje4cc0qw.getOrDie('Node');
    return f;
  };
  var compareDocumentPosition = function (a, b, match) {
    return (a.compareDocumentPosition(b) & match) !== 0;
  };
  var documentPositionPreceding = function (a, b) {
    return compareDocumentPosition(a, b, node().DOCUMENT_POSITION_PRECEDING);
  };
  var documentPositionContainedBy = function (a, b) {
    return compareDocumentPosition(a, b, node().DOCUMENT_POSITION_CONTAINED_BY);
  };
  var $_34ktkhxaje4cc0qv = {
    documentPositionPreceding: documentPositionPreceding,
    documentPositionContainedBy: documentPositionContainedBy
  };

  var fromHtml = function (html, scope) {
    var doc = scope || document;
    var div = doc.createElement('div');
    div.innerHTML = html;
    if (!div.hasChildNodes() || div.childNodes.length > 1) {
      console.error('HTML does not have a single root node', html);
      throw 'HTML must have a single root node';
    }
    return fromDom(div.childNodes[0]);
  };
  var fromTag = function (tag, scope) {
    var doc = scope || document;
    var node = doc.createElement(tag);
    return fromDom(node);
  };
  var fromText = function (text, scope) {
    var doc = scope || document;
    var node = doc.createTextNode(text);
    return fromDom(node);
  };
  var fromDom = function (node) {
    if (node === null || node === undefined)
      throw new Error('Node cannot be null or undefined');
    return { dom: $_aso7c6wjje4cc0om.constant(node) };
  };
  var fromPoint = function (doc, x, y) {
    return Option.from(doc.dom().elementFromPoint(x, y)).map(fromDom);
  };
  var $_407ejqxfje4cc0rb = {
    fromHtml: fromHtml,
    fromTag: fromTag,
    fromText: fromText,
    fromDom: fromDom,
    fromPoint: fromPoint
  };

  var $_6qk72mxgje4cc0rf = {
    ATTRIBUTE: 2,
    CDATA_SECTION: 4,
    COMMENT: 8,
    DOCUMENT: 9,
    DOCUMENT_TYPE: 10,
    DOCUMENT_FRAGMENT: 11,
    ELEMENT: 1,
    TEXT: 3,
    PROCESSING_INSTRUCTION: 7,
    ENTITY_REFERENCE: 5,
    ENTITY: 6,
    NOTATION: 12
  };

  var ELEMENT = $_6qk72mxgje4cc0rf.ELEMENT;
  var DOCUMENT = $_6qk72mxgje4cc0rf.DOCUMENT;
  var is = function (element, selector) {
    var elem = element.dom();
    if (elem.nodeType !== ELEMENT)
      return false;
    else if (elem.matches !== undefined)
      return elem.matches(selector);
    else if (elem.msMatchesSelector !== undefined)
      return elem.msMatchesSelector(selector);
    else if (elem.webkitMatchesSelector !== undefined)
      return elem.webkitMatchesSelector(selector);
    else if (elem.mozMatchesSelector !== undefined)
      return elem.mozMatchesSelector(selector);
    else
      throw new Error('Browser lacks native selectors');
  };
  var bypassSelector = function (dom) {
    return dom.nodeType !== ELEMENT && dom.nodeType !== DOCUMENT || dom.childElementCount === 0;
  };
  var all = function (selector, scope) {
    var base = scope === undefined ? document : scope.dom();
    return bypassSelector(base) ? [] : $_elh0pqwsje4cc0p4.map(base.querySelectorAll(selector), $_407ejqxfje4cc0rb.fromDom);
  };
  var one = function (selector, scope) {
    var base = scope === undefined ? document : scope.dom();
    return bypassSelector(base) ? Option.none() : Option.from(base.querySelector(selector)).map($_407ejqxfje4cc0rb.fromDom);
  };
  var $_ab1wd9xeje4cc0r0 = {
    all: all,
    is: is,
    one: one
  };

  var eq = function (e1, e2) {
    return e1.dom() === e2.dom();
  };
  var isEqualNode = function (e1, e2) {
    return e1.dom().isEqualNode(e2.dom());
  };
  var member = function (element, elements) {
    return $_elh0pqwsje4cc0p4.exists(elements, $_aso7c6wjje4cc0om.curry(eq, element));
  };
  var regularContains = function (e1, e2) {
    var d1 = e1.dom(), d2 = e2.dom();
    return d1 === d2 ? false : d1.contains(d2);
  };
  var ieContains = function (e1, e2) {
    return $_34ktkhxaje4cc0qv.documentPositionContainedBy(e1.dom(), e2.dom());
  };
  var browser = $_2j4x3gwkje4cc0oo.detect().browser;
  var contains$2 = browser.isIE() ? ieContains : regularContains;
  var $_1stme4x9je4cc0qq = {
    eq: eq,
    isEqualNode: isEqualNode,
    member: member,
    contains: contains$2,
    is: $_ab1wd9xeje4cc0r0.is
  };

  var owner = function (element) {
    return $_407ejqxfje4cc0rb.fromDom(element.dom().ownerDocument);
  };
  var documentElement = function (element) {
    var doc = owner(element);
    return $_407ejqxfje4cc0rb.fromDom(doc.dom().documentElement);
  };
  var defaultView = function (element) {
    var el = element.dom();
    var defaultView = el.ownerDocument.defaultView;
    return $_407ejqxfje4cc0rb.fromDom(defaultView);
  };
  var parent = function (element) {
    var dom = element.dom();
    return Option.from(dom.parentNode).map($_407ejqxfje4cc0rb.fromDom);
  };
  var findIndex$1 = function (element) {
    return parent(element).bind(function (p) {
      var kin = children(p);
      return $_elh0pqwsje4cc0p4.findIndex(kin, function (elem) {
        return $_1stme4x9je4cc0qq.eq(element, elem);
      });
    });
  };
  var parents = function (element, isRoot) {
    var stop = $_eh3yfywzje4cc0pu.isFunction(isRoot) ? isRoot : $_aso7c6wjje4cc0om.constant(false);
    var dom = element.dom();
    var ret = [];
    while (dom.parentNode !== null && dom.parentNode !== undefined) {
      var rawParent = dom.parentNode;
      var parent = $_407ejqxfje4cc0rb.fromDom(rawParent);
      ret.push(parent);
      if (stop(parent) === true)
        break;
      else
        dom = rawParent;
    }
    return ret;
  };
  var siblings = function (element) {
    var filterSelf = function (elements) {
      return $_elh0pqwsje4cc0p4.filter(elements, function (x) {
        return !$_1stme4x9je4cc0qq.eq(element, x);
      });
    };
    return parent(element).map(children).map(filterSelf).getOr([]);
  };
  var offsetParent = function (element) {
    var dom = element.dom();
    return Option.from(dom.offsetParent).map($_407ejqxfje4cc0rb.fromDom);
  };
  var prevSibling = function (element) {
    var dom = element.dom();
    return Option.from(dom.previousSibling).map($_407ejqxfje4cc0rb.fromDom);
  };
  var nextSibling = function (element) {
    var dom = element.dom();
    return Option.from(dom.nextSibling).map($_407ejqxfje4cc0rb.fromDom);
  };
  var prevSiblings = function (element) {
    return $_elh0pqwsje4cc0p4.reverse($_g6q5ktx8je4cc0qp.toArray(element, prevSibling));
  };
  var nextSiblings = function (element) {
    return $_g6q5ktx8je4cc0qp.toArray(element, nextSibling);
  };
  var children = function (element) {
    var dom = element.dom();
    return $_elh0pqwsje4cc0p4.map(dom.childNodes, $_407ejqxfje4cc0rb.fromDom);
  };
  var child = function (element, index) {
    var children = element.dom().childNodes;
    return Option.from(children[index]).map($_407ejqxfje4cc0rb.fromDom);
  };
  var firstChild = function (element) {
    return child(element, 0);
  };
  var lastChild = function (element) {
    return child(element, element.dom().childNodes.length - 1);
  };
  var childNodesCount = function (element) {
    return element.dom().childNodes.length;
  };
  var hasChildNodes = function (element) {
    return element.dom().hasChildNodes();
  };
  var spot = $_ajn35gx4je4cc0qj.immutable('element', 'offset');
  var leaf = function (element, offset) {
    var cs = children(element);
    return cs.length > 0 && offset < cs.length ? spot(cs[offset], 0) : spot(element, offset);
  };
  var $_1jwy92x3je4cc0qa = {
    owner: owner,
    defaultView: defaultView,
    documentElement: documentElement,
    parent: parent,
    findIndex: findIndex$1,
    parents: parents,
    siblings: siblings,
    prevSibling: prevSibling,
    offsetParent: offsetParent,
    prevSiblings: prevSiblings,
    nextSibling: nextSibling,
    nextSiblings: nextSiblings,
    children: children,
    child: child,
    firstChild: firstChild,
    lastChild: lastChild,
    childNodesCount: childNodesCount,
    hasChildNodes: hasChildNodes,
    leaf: leaf
  };

  var before = function (marker, element) {
    var parent = $_1jwy92x3je4cc0qa.parent(marker);
    parent.each(function (v) {
      v.dom().insertBefore(element.dom(), marker.dom());
    });
  };
  var after = function (marker, element) {
    var sibling = $_1jwy92x3je4cc0qa.nextSibling(marker);
    sibling.fold(function () {
      var parent = $_1jwy92x3je4cc0qa.parent(marker);
      parent.each(function (v) {
        append(v, element);
      });
    }, function (v) {
      before(v, element);
    });
  };
  var prepend = function (parent, element) {
    var firstChild = $_1jwy92x3je4cc0qa.firstChild(parent);
    firstChild.fold(function () {
      append(parent, element);
    }, function (v) {
      parent.dom().insertBefore(element.dom(), v.dom());
    });
  };
  var append = function (parent, element) {
    parent.dom().appendChild(element.dom());
  };
  var appendAt = function (parent, element, index) {
    $_1jwy92x3je4cc0qa.child(parent, index).fold(function () {
      append(parent, element);
    }, function (v) {
      before(v, element);
    });
  };
  var wrap = function (element, wrapper) {
    before(element, wrapper);
    append(wrapper, element);
  };
  var $_9zp36ax2je4cc0q8 = {
    before: before,
    after: after,
    prepend: prepend,
    append: append,
    appendAt: appendAt,
    wrap: wrap
  };

  var before$1 = function (marker, elements) {
    $_elh0pqwsje4cc0p4.each(elements, function (x) {
      $_9zp36ax2je4cc0q8.before(marker, x);
    });
  };
  var after$1 = function (marker, elements) {
    $_elh0pqwsje4cc0p4.each(elements, function (x, i) {
      var e = i === 0 ? marker : elements[i - 1];
      $_9zp36ax2je4cc0q8.after(e, x);
    });
  };
  var prepend$1 = function (parent, elements) {
    $_elh0pqwsje4cc0p4.each(elements.slice().reverse(), function (x) {
      $_9zp36ax2je4cc0q8.prepend(parent, x);
    });
  };
  var append$1 = function (parent, elements) {
    $_elh0pqwsje4cc0p4.each(elements, function (x) {
      $_9zp36ax2je4cc0q8.append(parent, x);
    });
  };
  var $_dy5220xije4cc0rj = {
    before: before$1,
    after: after$1,
    prepend: prepend$1,
    append: append$1
  };

  var empty = function (element) {
    element.dom().textContent = '';
    $_elh0pqwsje4cc0p4.each($_1jwy92x3je4cc0qa.children(element), function (rogue) {
      remove(rogue);
    });
  };
  var remove = function (element) {
    var dom = element.dom();
    if (dom.parentNode !== null)
      dom.parentNode.removeChild(dom);
  };
  var unwrap = function (wrapper) {
    var children = $_1jwy92x3je4cc0qa.children(wrapper);
    if (children.length > 0)
      $_dy5220xije4cc0rj.before(wrapper, children);
    remove(wrapper);
  };
  var $_buyc6zxhje4cc0rh = {
    empty: empty,
    remove: remove,
    unwrap: unwrap
  };

  var name = function (element) {
    var r = element.dom().nodeName;
    return r.toLowerCase();
  };
  var type = function (element) {
    return element.dom().nodeType;
  };
  var value = function (element) {
    return element.dom().nodeValue;
  };
  var isType$1 = function (t) {
    return function (element) {
      return type(element) === t;
    };
  };
  var isComment = function (element) {
    return type(element) === $_6qk72mxgje4cc0rf.COMMENT || name(element) === '#comment';
  };
  var isElement = isType$1($_6qk72mxgje4cc0rf.ELEMENT);
  var isText = isType$1($_6qk72mxgje4cc0rf.TEXT);
  var isDocument = isType$1($_6qk72mxgje4cc0rf.DOCUMENT);
  var $_7qwxg2xkje4cc0ro = {
    name: name,
    type: type,
    value: value,
    isElement: isElement,
    isText: isText,
    isDocument: isDocument,
    isComment: isComment
  };

  var inBody = function (element) {
    var dom = $_7qwxg2xkje4cc0ro.isText(element) ? element.dom().parentNode : element.dom();
    return dom !== undefined && dom !== null && dom.ownerDocument.body.contains(dom);
  };
  var body = $_b57rigwlje4cc0oq.cached(function () {
    return getBody($_407ejqxfje4cc0rb.fromDom(document));
  });
  var getBody = function (doc) {
    var body = doc.dom().body;
    if (body === null || body === undefined)
      throw 'Body is not available yet';
    return $_407ejqxfje4cc0rb.fromDom(body);
  };
  var $_6jf42xxjje4cc0rm = {
    body: body,
    getBody: getBody,
    inBody: inBody
  };

  var fireDetaching = function (component) {
    $_3fionhwgje4cc0o9.emit(component, $_g6tooswhje4cc0of.detachedFromDom());
    var children = component.components();
    $_elh0pqwsje4cc0p4.each(children, fireDetaching);
  };
  var fireAttaching = function (component) {
    var children = component.components();
    $_elh0pqwsje4cc0p4.each(children, fireAttaching);
    $_3fionhwgje4cc0o9.emit(component, $_g6tooswhje4cc0of.attachedToDom());
  };
  var attach = function (parent, child) {
    attachWith(parent, child, $_9zp36ax2je4cc0q8.append);
  };
  var attachWith = function (parent, child, insertion) {
    parent.getSystem().addToWorld(child);
    insertion(parent.element(), child.element());
    if ($_6jf42xxjje4cc0rm.inBody(parent.element()))
      fireAttaching(child);
    parent.syncComponents();
  };
  var doDetach = function (component) {
    fireDetaching(component);
    $_buyc6zxhje4cc0rh.remove(component.element());
    component.getSystem().removeFromWorld(component);
  };
  var detach = function (component) {
    var parent = $_1jwy92x3je4cc0qa.parent(component.element()).bind(function (p) {
      return component.getSystem().getByDom(p).fold(Option.none, Option.some);
    });
    doDetach(component);
    parent.each(function (p) {
      p.syncComponents();
    });
  };
  var detachChildren = function (component) {
    var subs = component.components();
    $_elh0pqwsje4cc0p4.each(subs, doDetach);
    $_buyc6zxhje4cc0rh.empty(component.element());
    component.syncComponents();
  };
  var attachSystem = function (element, guiSystem) {
    $_9zp36ax2je4cc0q8.append(element, guiSystem.element());
    var children = $_1jwy92x3je4cc0qa.children(guiSystem.element());
    $_elh0pqwsje4cc0p4.each(children, function (child) {
      guiSystem.getByDom(child).each(fireAttaching);
    });
  };
  var detachSystem = function (guiSystem) {
    var children = $_1jwy92x3je4cc0qa.children(guiSystem.element());
    $_elh0pqwsje4cc0p4.each(children, function (child) {
      guiSystem.getByDom(child).each(fireDetaching);
    });
    $_buyc6zxhje4cc0rh.remove(guiSystem.element());
  };
  var $_b576ucx1je4cc0py = {
    attach: attach,
    attachWith: attachWith,
    detach: detach,
    detachChildren: detachChildren,
    attachSystem: attachSystem,
    detachSystem: detachSystem
  };

  var fromHtml$1 = function (html, scope) {
    var doc = scope || document;
    var div = doc.createElement('div');
    div.innerHTML = html;
    return $_1jwy92x3je4cc0qa.children($_407ejqxfje4cc0rb.fromDom(div));
  };
  var fromTags = function (tags, scope) {
    return $_elh0pqwsje4cc0p4.map(tags, function (x) {
      return $_407ejqxfje4cc0rb.fromTag(x, scope);
    });
  };
  var fromText$1 = function (texts, scope) {
    return $_elh0pqwsje4cc0p4.map(texts, function (x) {
      return $_407ejqxfje4cc0rb.fromText(x, scope);
    });
  };
  var fromDom$1 = function (nodes) {
    return $_elh0pqwsje4cc0p4.map(nodes, $_407ejqxfje4cc0rb.fromDom);
  };
  var $_4cgf3pxpje4cc0s5 = {
    fromHtml: fromHtml$1,
    fromTags: fromTags,
    fromText: fromText$1,
    fromDom: fromDom$1
  };

  var get = function (element) {
    return element.dom().innerHTML;
  };
  var set = function (element, content) {
    var owner = $_1jwy92x3je4cc0qa.owner(element);
    var docDom = owner.dom();
    var fragment = $_407ejqxfje4cc0rb.fromDom(docDom.createDocumentFragment());
    var contentElements = $_4cgf3pxpje4cc0s5.fromHtml(content, docDom);
    $_dy5220xije4cc0rj.append(fragment, contentElements);
    $_buyc6zxhje4cc0rh.empty(element);
    $_9zp36ax2je4cc0q8.append(element, fragment);
  };
  var getOuter = function (element) {
    var container = $_407ejqxfje4cc0rb.fromTag('div');
    var clone = $_407ejqxfje4cc0rb.fromDom(element.dom().cloneNode(true));
    $_9zp36ax2je4cc0q8.append(container, clone);
    return get(container);
  };
  var $_5scgvxoje4cc0s3 = {
    get: get,
    set: set,
    getOuter: getOuter
  };

  var rawSet = function (dom, key, value) {
    if ($_eh3yfywzje4cc0pu.isString(value) || $_eh3yfywzje4cc0pu.isBoolean(value) || $_eh3yfywzje4cc0pu.isNumber(value)) {
      dom.setAttribute(key, value + '');
    } else {
      console.error('Invalid call to Attr.set. Key ', key, ':: Value ', value, ':: Element ', dom);
      throw new Error('Attribute value was not simple');
    }
  };
  var set$1 = function (element, key, value) {
    rawSet(element.dom(), key, value);
  };
  var setAll = function (element, attrs) {
    var dom = element.dom();
    $_i587hx0je4cc0pw.each(attrs, function (v, k) {
      rawSet(dom, k, v);
    });
  };
  var get$1 = function (element, key) {
    var v = element.dom().getAttribute(key);
    return v === null ? undefined : v;
  };
  var has = function (element, key) {
    var dom = element.dom();
    return dom && dom.hasAttribute ? dom.hasAttribute(key) : false;
  };
  var remove$1 = function (element, key) {
    element.dom().removeAttribute(key);
  };
  var hasNone = function (element) {
    var attrs = element.dom().attributes;
    return attrs === undefined || attrs === null || attrs.length === 0;
  };
  var clone = function (element) {
    return $_elh0pqwsje4cc0p4.foldl(element.dom().attributes, function (acc, attr) {
      acc[attr.name] = attr.value;
      return acc;
    }, {});
  };
  var transferOne = function (source, destination, attr) {
    if (has(source, attr) && !has(destination, attr))
      set$1(destination, attr, get$1(source, attr));
  };
  var transfer = function (source, destination, attrs) {
    if (!$_7qwxg2xkje4cc0ro.isElement(source) || !$_7qwxg2xkje4cc0ro.isElement(destination))
      return;
    $_elh0pqwsje4cc0p4.each(attrs, function (attr) {
      transferOne(source, destination, attr);
    });
  };
  var $_bjjq6ixrje4cc0s9 = {
    clone: clone,
    set: set$1,
    setAll: setAll,
    get: get$1,
    has: has,
    remove: remove$1,
    hasNone: hasNone,
    transfer: transfer
  };

  var clone$1 = function (original, deep) {
    return $_407ejqxfje4cc0rb.fromDom(original.dom().cloneNode(deep));
  };
  var shallow$1 = function (original) {
    return clone$1(original, false);
  };
  var deep$1 = function (original) {
    return clone$1(original, true);
  };
  var shallowAs = function (original, tag) {
    var nu = $_407ejqxfje4cc0rb.fromTag(tag);
    var attributes = $_bjjq6ixrje4cc0s9.clone(original);
    $_bjjq6ixrje4cc0s9.setAll(nu, attributes);
    return nu;
  };
  var copy = function (original, tag) {
    var nu = shallowAs(original, tag);
    var cloneChildren = $_1jwy92x3je4cc0qa.children(deep$1(original));
    $_dy5220xije4cc0rj.append(nu, cloneChildren);
    return nu;
  };
  var mutate = function (original, tag) {
    var nu = shallowAs(original, tag);
    $_9zp36ax2je4cc0q8.before(original, nu);
    var children = $_1jwy92x3je4cc0qa.children(original);
    $_dy5220xije4cc0rj.append(nu, children);
    $_buyc6zxhje4cc0rh.remove(original);
    return nu;
  };
  var $_3vkckpxqje4cc0s8 = {
    shallow: shallow$1,
    shallowAs: shallowAs,
    deep: deep$1,
    copy: copy,
    mutate: mutate
  };

  var getHtml = function (element) {
    var clone = $_3vkckpxqje4cc0s8.shallow(element);
    return $_5scgvxoje4cc0s3.getOuter(clone);
  };
  var $_axh25dxnje4cc0s0 = { getHtml: getHtml };

  var element = function (elem) {
    return $_axh25dxnje4cc0s0.getHtml(elem);
  };
  var $_dfrb81xmje4cc0rz = { element: element };

  var value$1 = function (o) {
    var is = function (v) {
      return o === v;
    };
    var or = function (opt) {
      return value$1(o);
    };
    var orThunk = function (f) {
      return value$1(o);
    };
    var map = function (f) {
      return value$1(f(o));
    };
    var each = function (f) {
      f(o);
    };
    var bind = function (f) {
      return f(o);
    };
    var fold = function (_, onValue) {
      return onValue(o);
    };
    var exists = function (f) {
      return f(o);
    };
    var forall = function (f) {
      return f(o);
    };
    var toOption = function () {
      return Option.some(o);
    };
    return {
      is: is,
      isValue: $_aso7c6wjje4cc0om.always,
      isError: $_aso7c6wjje4cc0om.never,
      getOr: $_aso7c6wjje4cc0om.constant(o),
      getOrThunk: $_aso7c6wjje4cc0om.constant(o),
      getOrDie: $_aso7c6wjje4cc0om.constant(o),
      or: or,
      orThunk: orThunk,
      fold: fold,
      map: map,
      each: each,
      bind: bind,
      exists: exists,
      forall: forall,
      toOption: toOption
    };
  };
  var error = function (message) {
    var getOrThunk = function (f) {
      return f();
    };
    var getOrDie = function () {
      return $_aso7c6wjje4cc0om.die(message)();
    };
    var or = function (opt) {
      return opt;
    };
    var orThunk = function (f) {
      return f();
    };
    var map = function (f) {
      return error(message);
    };
    var bind = function (f) {
      return error(message);
    };
    var fold = function (onError, _) {
      return onError(message);
    };
    return {
      is: $_aso7c6wjje4cc0om.never,
      isValue: $_aso7c6wjje4cc0om.never,
      isError: $_aso7c6wjje4cc0om.always,
      getOr: $_aso7c6wjje4cc0om.identity,
      getOrThunk: getOrThunk,
      getOrDie: getOrDie,
      or: or,
      orThunk: orThunk,
      fold: fold,
      map: map,
      each: $_aso7c6wjje4cc0om.noop,
      bind: bind,
      exists: $_aso7c6wjje4cc0om.never,
      forall: $_aso7c6wjje4cc0om.always,
      toOption: Option.none
    };
  };
  var Result = {
    value: value$1,
    error: error
  };

  var generate = function (cases) {
    if (!$_eh3yfywzje4cc0pu.isArray(cases)) {
      throw new Error('cases must be an array');
    }
    if (cases.length === 0) {
      throw new Error('there must be at least one case');
    }
    var constructors = [];
    var adt = {};
    $_elh0pqwsje4cc0p4.each(cases, function (acase, count) {
      var keys = $_i587hx0je4cc0pw.keys(acase);
      if (keys.length !== 1) {
        throw new Error('one and only one name per case');
      }
      var key = keys[0];
      var value = acase[key];
      if (adt[key] !== undefined) {
        throw new Error('duplicate key detected:' + key);
      } else if (key === 'cata') {
        throw new Error('cannot have a case named cata (sorry)');
      } else if (!$_eh3yfywzje4cc0pu.isArray(value)) {
        throw new Error('case arguments must be an array');
      }
      constructors.push(key);
      adt[key] = function () {
        var argLength = arguments.length;
        if (argLength !== value.length) {
          throw new Error('Wrong number of arguments to case ' + key + '. Expected ' + value.length + ' (' + value + '), got ' + argLength);
        }
        var args = new Array(argLength);
        for (var i = 0; i < args.length; i++)
          args[i] = arguments[i];
        var match = function (branches) {
          var branchKeys = $_i587hx0je4cc0pw.keys(branches);
          if (constructors.length !== branchKeys.length) {
            throw new Error('Wrong number of arguments to match. Expected: ' + constructors.join(',') + '\nActual: ' + branchKeys.join(','));
          }
          var allReqd = $_elh0pqwsje4cc0p4.forall(constructors, function (reqKey) {
            return $_elh0pqwsje4cc0p4.contains(branchKeys, reqKey);
          });
          if (!allReqd)
            throw new Error('Not all branches were specified when using match. Specified: ' + branchKeys.join(', ') + '\nRequired: ' + constructors.join(', '));
          return branches[key].apply(null, args);
        };
        return {
          fold: function () {
            if (arguments.length !== cases.length) {
              throw new Error('Wrong number of arguments to fold. Expected ' + cases.length + ', got ' + arguments.length);
            }
            var target = arguments[count];
            return target.apply(null, args);
          },
          match: match,
          log: function (label) {
            console.log(label, {
              constructors: constructors,
              constructor: key,
              params: args
            });
          }
        };
      };
    });
    return adt;
  };
  var $_170ozvxwje4cc0st = { generate: generate };

  var comparison = $_170ozvxwje4cc0st.generate([
    {
      bothErrors: [
        'error1',
        'error2'
      ]
    },
    {
      firstError: [
        'error1',
        'value2'
      ]
    },
    {
      secondError: [
        'value1',
        'error2'
      ]
    },
    {
      bothValues: [
        'value1',
        'value2'
      ]
    }
  ]);
  var partition$1 = function (results) {
    var errors = [];
    var values = [];
    $_elh0pqwsje4cc0p4.each(results, function (result) {
      result.fold(function (err) {
        errors.push(err);
      }, function (value) {
        values.push(value);
      });
    });
    return {
      errors: errors,
      values: values
    };
  };
  var compare = function (result1, result2) {
    return result1.fold(function (err1) {
      return result2.fold(function (err2) {
        return comparison.bothErrors(err1, err2);
      }, function (val2) {
        return comparison.firstError(err1, val2);
      });
    }, function (val1) {
      return result2.fold(function (err2) {
        return comparison.secondError(val1, err2);
      }, function (val2) {
        return comparison.bothValues(val1, val2);
      });
    });
  };
  var $_ggdrfyxvje4cc0sr = {
    partition: partition$1,
    compare: compare
  };

  var mergeValues = function (values, base) {
    return Result.value($_dax12xwyje4cc0pt.deepMerge.apply(undefined, [base].concat(values)));
  };
  var mergeErrors = function (errors) {
    return $_aso7c6wjje4cc0om.compose(Result.error, $_elh0pqwsje4cc0p4.flatten)(errors);
  };
  var consolidateObj = function (objects, base) {
    var partitions = $_ggdrfyxvje4cc0sr.partition(objects);
    return partitions.errors.length > 0 ? mergeErrors(partitions.errors) : mergeValues(partitions.values, base);
  };
  var consolidateArr = function (objects) {
    var partitions = $_ggdrfyxvje4cc0sr.partition(objects);
    return partitions.errors.length > 0 ? mergeErrors(partitions.errors) : Result.value(partitions.values);
  };
  var $_d029txxtje4cc0si = {
    consolidateObj: consolidateObj,
    consolidateArr: consolidateArr
  };

  var narrow = function (obj, fields) {
    var r = {};
    $_elh0pqwsje4cc0p4.each(fields, function (field) {
      if (obj[field] !== undefined && obj.hasOwnProperty(field))
        r[field] = obj[field];
    });
    return r;
  };
  var indexOnKey = function (array, key) {
    var obj = {};
    $_elh0pqwsje4cc0p4.each(array, function (a) {
      var keyValue = a[key];
      obj[keyValue] = a;
    });
    return obj;
  };
  var exclude = function (obj, fields) {
    var r = {};
    $_i587hx0je4cc0pw.each(obj, function (v, k) {
      if (!$_elh0pqwsje4cc0p4.contains(fields, k)) {
        r[k] = v;
      }
    });
    return r;
  };
  var $_afcvv3xxje4cc0sv = {
    narrow: narrow,
    exclude: exclude,
    indexOnKey: indexOnKey
  };

  var readOpt = function (key) {
    return function (obj) {
      return obj.hasOwnProperty(key) ? Option.from(obj[key]) : Option.none();
    };
  };
  var readOr = function (key, fallback) {
    return function (obj) {
      return readOpt(key)(obj).getOr(fallback);
    };
  };
  var readOptFrom = function (obj, key) {
    return readOpt(key)(obj);
  };
  var hasKey = function (obj, key) {
    return obj.hasOwnProperty(key) && obj[key] !== undefined && obj[key] !== null;
  };
  var $_8wo3s7xyje4cc0t7 = {
    readOpt: readOpt,
    readOr: readOr,
    readOptFrom: readOptFrom,
    hasKey: hasKey
  };

  var wrap$1 = function (key, value) {
    var r = {};
    r[key] = value;
    return r;
  };
  var wrapAll = function (keyvalues) {
    var r = {};
    $_elh0pqwsje4cc0p4.each(keyvalues, function (kv) {
      r[kv.key] = kv.value;
    });
    return r;
  };
  var $_g8mgb7xzje4cc0t9 = {
    wrap: wrap$1,
    wrapAll: wrapAll
  };

  var narrow$1 = function (obj, fields) {
    return $_afcvv3xxje4cc0sv.narrow(obj, fields);
  };
  var exclude$1 = function (obj, fields) {
    return $_afcvv3xxje4cc0sv.exclude(obj, fields);
  };
  var readOpt$1 = function (key) {
    return $_8wo3s7xyje4cc0t7.readOpt(key);
  };
  var readOr$1 = function (key, fallback) {
    return $_8wo3s7xyje4cc0t7.readOr(key, fallback);
  };
  var readOptFrom$1 = function (obj, key) {
    return $_8wo3s7xyje4cc0t7.readOptFrom(obj, key);
  };
  var wrap$2 = function (key, value) {
    return $_g8mgb7xzje4cc0t9.wrap(key, value);
  };
  var wrapAll$1 = function (keyvalues) {
    return $_g8mgb7xzje4cc0t9.wrapAll(keyvalues);
  };
  var indexOnKey$1 = function (array, key) {
    return $_afcvv3xxje4cc0sv.indexOnKey(array, key);
  };
  var consolidate = function (objs, base) {
    return $_d029txxtje4cc0si.consolidateObj(objs, base);
  };
  var hasKey$1 = function (obj, key) {
    return $_8wo3s7xyje4cc0t7.hasKey(obj, key);
  };
  var $_d08hr1xsje4cc0sg = {
    narrow: narrow$1,
    exclude: exclude$1,
    readOpt: readOpt$1,
    readOr: readOr$1,
    readOptFrom: readOptFrom$1,
    wrap: wrap$2,
    wrapAll: wrapAll$1,
    indexOnKey: indexOnKey$1,
    hasKey: hasKey$1,
    consolidate: consolidate
  };

  var cat = function (arr) {
    var r = [];
    var push = function (x) {
      r.push(x);
    };
    for (var i = 0; i < arr.length; i++) {
      arr[i].each(push);
    }
    return r;
  };
  var findMap = function (arr, f) {
    for (var i = 0; i < arr.length; i++) {
      var r = f(arr[i], i);
      if (r.isSome()) {
        return r;
      }
    }
    return Option.none();
  };
  var liftN = function (arr, f) {
    var r = [];
    for (var i = 0; i < arr.length; i++) {
      var x = arr[i];
      if (x.isSome()) {
        r.push(x.getOrDie());
      } else {
        return Option.none();
      }
    }
    return Option.some(f.apply(null, r));
  };
  var $_egl8u6y0je4cc0tb = {
    cat: cat,
    findMap: findMap,
    liftN: liftN
  };

  var unknown$3 = 'unknown';
  var debugging = true;
  var CHROME_INSPECTOR_GLOBAL = '__CHROME_INSPECTOR_CONNECTION_TO_ALLOY__';
  var eventsMonitored = [];
  var path$1 = [
    'alloy/data/Fields',
    'alloy/debugging/Debugging'
  ];
  var getTrace = function () {
    if (debugging === false)
      return unknown$3;
    var err = new Error();
    if (err.stack !== undefined) {
      var lines = err.stack.split('\n');
      return $_elh0pqwsje4cc0p4.find(lines, function (line) {
        return line.indexOf('alloy') > 0 && !$_elh0pqwsje4cc0p4.exists(path$1, function (p) {
          return line.indexOf(p) > -1;
        });
      }).getOr(unknown$3);
    } else {
      return unknown$3;
    }
  };
  var logHandler = function (label, handlerName, trace) {
  };
  var ignoreEvent = {
    logEventCut: $_aso7c6wjje4cc0om.noop,
    logEventStopped: $_aso7c6wjje4cc0om.noop,
    logNoParent: $_aso7c6wjje4cc0om.noop,
    logEventNoHandlers: $_aso7c6wjje4cc0om.noop,
    logEventResponse: $_aso7c6wjje4cc0om.noop,
    write: $_aso7c6wjje4cc0om.noop
  };
  var monitorEvent = function (eventName, initialTarget, f) {
    var logger = debugging && (eventsMonitored === '*' || $_elh0pqwsje4cc0p4.contains(eventsMonitored, eventName)) ? function () {
      var sequence = [];
      return {
        logEventCut: function (name, target, purpose) {
          sequence.push({
            outcome: 'cut',
            target: target,
            purpose: purpose
          });
        },
        logEventStopped: function (name, target, purpose) {
          sequence.push({
            outcome: 'stopped',
            target: target,
            purpose: purpose
          });
        },
        logNoParent: function (name, target, purpose) {
          sequence.push({
            outcome: 'no-parent',
            target: target,
            purpose: purpose
          });
        },
        logEventNoHandlers: function (name, target) {
          sequence.push({
            outcome: 'no-handlers-left',
            target: target
          });
        },
        logEventResponse: function (name, target, purpose) {
          sequence.push({
            outcome: 'response',
            purpose: purpose,
            target: target
          });
        },
        write: function () {
          if ($_elh0pqwsje4cc0p4.contains([
              'mousemove',
              'mouseover',
              'mouseout',
              $_g6tooswhje4cc0of.systemInit()
            ], eventName))
            return;
          console.log(eventName, {
            event: eventName,
            target: initialTarget.dom(),
            sequence: $_elh0pqwsje4cc0p4.map(sequence, function (s) {
              if (!$_elh0pqwsje4cc0p4.contains([
                  'cut',
                  'stopped',
                  'response'
                ], s.outcome))
                return s.outcome;
              else
                return '{' + s.purpose + '} ' + s.outcome + ' at (' + $_dfrb81xmje4cc0rz.element(s.target) + ')';
            })
          });
        }
      };
    }() : ignoreEvent;
    var output = f(logger);
    logger.write();
    return output;
  };
  var inspectorInfo = function (comp) {
    var go = function (c) {
      var cSpec = c.spec();
      return {
        '(original.spec)': cSpec,
        '(dom.ref)': c.element().dom(),
        '(element)': $_dfrb81xmje4cc0rz.element(c.element()),
        '(initComponents)': $_elh0pqwsje4cc0p4.map(cSpec.components !== undefined ? cSpec.components : [], go),
        '(components)': $_elh0pqwsje4cc0p4.map(c.components(), go),
        '(bound.events)': $_i587hx0je4cc0pw.mapToArray(c.events(), function (v, k) {
          return [k];
        }).join(', '),
        '(behaviours)': cSpec.behaviours !== undefined ? $_i587hx0je4cc0pw.map(cSpec.behaviours, function (v, k) {
          return v === undefined ? '--revoked--' : {
            config: v.configAsRaw(),
            'original-config': v.initialConfig,
            state: c.readState(k)
          };
        }) : 'none'
      };
    };
    return go(comp);
  };
  var getOrInitConnection = function () {
    if (window[CHROME_INSPECTOR_GLOBAL] !== undefined)
      return window[CHROME_INSPECTOR_GLOBAL];
    else {
      window[CHROME_INSPECTOR_GLOBAL] = {
        systems: {},
        lookup: function (uid) {
          var systems = window[CHROME_INSPECTOR_GLOBAL].systems;
          var connections = $_i587hx0je4cc0pw.keys(systems);
          return $_egl8u6y0je4cc0tb.findMap(connections, function (conn) {
            var connGui = systems[conn];
            return connGui.getByUid(uid).toOption().map(function (comp) {
              return $_d08hr1xsje4cc0sg.wrap($_dfrb81xmje4cc0rz.element(comp.element()), inspectorInfo(comp));
            });
          });
        }
      };
      return window[CHROME_INSPECTOR_GLOBAL];
    }
  };
  var registerInspector = function (name, gui) {
    var connection = getOrInitConnection();
    connection.systems[name] = gui;
  };
  var $_g603ovxlje4cc0rq = {
    logHandler: logHandler,
    noLogger: $_aso7c6wjje4cc0om.constant(ignoreEvent),
    getTrace: getTrace,
    monitorEvent: monitorEvent,
    isDebugging: $_aso7c6wjje4cc0om.constant(debugging),
    registerInspector: registerInspector
  };

  var isSource = function (component, simulatedEvent) {
    return $_1stme4x9je4cc0qq.eq(component.element(), simulatedEvent.event().target());
  };
  var $_cum750y5je4cc0u1 = { isSource: isSource };

  var adt = $_170ozvxwje4cc0st.generate([
    { strict: [] },
    { defaultedThunk: ['fallbackThunk'] },
    { asOption: [] },
    { asDefaultedOptionThunk: ['fallbackThunk'] },
    { mergeWithThunk: ['baseThunk'] }
  ]);
  var defaulted = function (fallback) {
    return adt.defaultedThunk($_aso7c6wjje4cc0om.constant(fallback));
  };
  var asDefaultedOption = function (fallback) {
    return adt.asDefaultedOptionThunk($_aso7c6wjje4cc0om.constant(fallback));
  };
  var mergeWith = function (base) {
    return adt.mergeWithThunk($_aso7c6wjje4cc0om.constant(base));
  };
  var $_b7tb36y8je4cc0uh = {
    strict: adt.strict,
    asOption: adt.asOption,
    defaulted: defaulted,
    defaultedThunk: adt.defaultedThunk,
    asDefaultedOption: asDefaultedOption,
    asDefaultedOptionThunk: adt.asDefaultedOptionThunk,
    mergeWith: mergeWith,
    mergeWithThunk: adt.mergeWithThunk
  };

  var typeAdt = $_170ozvxwje4cc0st.generate([
    {
      setOf: [
        'validator',
        'valueType'
      ]
    },
    { arrOf: ['valueType'] },
    { objOf: ['fields'] },
    { itemOf: ['validator'] },
    {
      choiceOf: [
        'key',
        'branches'
      ]
    },
    { thunk: ['description'] },
    {
      func: [
        'args',
        'outputSchema'
      ]
    }
  ]);
  var fieldAdt = $_170ozvxwje4cc0st.generate([
    {
      field: [
        'name',
        'presence',
        'type'
      ]
    },
    { state: ['name'] }
  ]);
  var $_6u1t5ayaje4cc0v9 = {
    typeAdt: typeAdt,
    fieldAdt: fieldAdt
  };

  var json = function () {
    return $_6a52kwxbje4cc0qw.getOrDie('JSON');
  };
  var parse = function (obj) {
    return json().parse(obj);
  };
  var stringify = function (obj, replacer, space) {
    return json().stringify(obj, replacer, space);
  };
  var $_bsgtdtydje4cc0vk = {
    parse: parse,
    stringify: stringify
  };

  var formatObj = function (input) {
    return $_eh3yfywzje4cc0pu.isObject(input) && $_i587hx0je4cc0pw.keys(input).length > 100 ? ' removed due to size' : $_bsgtdtydje4cc0vk.stringify(input, null, 2);
  };
  var formatErrors = function (errors) {
    var es = errors.length > 10 ? errors.slice(0, 10).concat([{
        path: [],
        getErrorInfo: function () {
          return '... (only showing first ten failures)';
        }
      }]) : errors;
    return $_elh0pqwsje4cc0p4.map(es, function (e) {
      return 'Failed path: (' + e.path.join(' > ') + ')\n' + e.getErrorInfo();
    });
  };
  var $_ezauspycje4cc0ve = {
    formatObj: formatObj,
    formatErrors: formatErrors
  };

  var nu$3 = function (path, getErrorInfo) {
    return Result.error([{
        path: path,
        getErrorInfo: getErrorInfo
      }]);
  };
  var missingStrict = function (path, key, obj) {
    return nu$3(path, function () {
      return 'Could not find valid *strict* value for "' + key + '" in ' + $_ezauspycje4cc0ve.formatObj(obj);
    });
  };
  var missingKey = function (path, key) {
    return nu$3(path, function () {
      return 'Choice schema did not contain choice key: "' + key + '"';
    });
  };
  var missingBranch = function (path, branches, branch) {
    return nu$3(path, function () {
      return 'The chosen schema: "' + branch + '" did not exist in branches: ' + $_ezauspycje4cc0ve.formatObj(branches);
    });
  };
  var unsupportedFields = function (path, unsupported) {
    return nu$3(path, function () {
      return 'There are unsupported fields: [' + unsupported.join(', ') + '] specified';
    });
  };
  var custom = function (path, err) {
    return nu$3(path, function () {
      return err;
    });
  };
  var toString = function (error) {
    return 'Failed path: (' + error.path.join(' > ') + ')\n' + error.getErrorInfo();
  };
  var $_4f5o0yybje4cc0vb = {
    missingStrict: missingStrict,
    missingKey: missingKey,
    missingBranch: missingBranch,
    unsupportedFields: unsupportedFields,
    custom: custom,
    toString: toString
  };

  var adt$1 = $_170ozvxwje4cc0st.generate([
    {
      field: [
        'key',
        'okey',
        'presence',
        'prop'
      ]
    },
    {
      state: [
        'okey',
        'instantiator'
      ]
    }
  ]);
  var output = function (okey, value) {
    return adt$1.state(okey, $_aso7c6wjje4cc0om.constant(value));
  };
  var snapshot = function (okey) {
    return adt$1.state(okey, $_aso7c6wjje4cc0om.identity);
  };
  var strictAccess = function (path, obj, key) {
    return $_8wo3s7xyje4cc0t7.readOptFrom(obj, key).fold(function () {
      return $_4f5o0yybje4cc0vb.missingStrict(path, key, obj);
    }, Result.value);
  };
  var fallbackAccess = function (obj, key, fallbackThunk) {
    var v = $_8wo3s7xyje4cc0t7.readOptFrom(obj, key).fold(function () {
      return fallbackThunk(obj);
    }, $_aso7c6wjje4cc0om.identity);
    return Result.value(v);
  };
  var optionAccess = function (obj, key) {
    return Result.value($_8wo3s7xyje4cc0t7.readOptFrom(obj, key));
  };
  var optionDefaultedAccess = function (obj, key, fallback) {
    var opt = $_8wo3s7xyje4cc0t7.readOptFrom(obj, key).map(function (val) {
      return val === true ? fallback(obj) : val;
    });
    return Result.value(opt);
  };
  var cExtractOne = function (path, obj, field, strength) {
    return field.fold(function (key, okey, presence, prop) {
      var bundle = function (av) {
        return prop.extract(path.concat([key]), strength, av).map(function (res) {
          return $_g8mgb7xzje4cc0t9.wrap(okey, strength(res));
        });
      };
      var bundleAsOption = function (optValue) {
        return optValue.fold(function () {
          var outcome = $_g8mgb7xzje4cc0t9.wrap(okey, strength(Option.none()));
          return Result.value(outcome);
        }, function (ov) {
          return prop.extract(path.concat([key]), strength, ov).map(function (res) {
            return $_g8mgb7xzje4cc0t9.wrap(okey, strength(Option.some(res)));
          });
        });
      };
      return function () {
        return presence.fold(function () {
          return strictAccess(path, obj, key).bind(bundle);
        }, function (fallbackThunk) {
          return fallbackAccess(obj, key, fallbackThunk).bind(bundle);
        }, function () {
          return optionAccess(obj, key).bind(bundleAsOption);
        }, function (fallbackThunk) {
          return optionDefaultedAccess(obj, key, fallbackThunk).bind(bundleAsOption);
        }, function (baseThunk) {
          var base = baseThunk(obj);
          return fallbackAccess(obj, key, $_aso7c6wjje4cc0om.constant({})).map(function (v) {
            return $_dax12xwyje4cc0pt.deepMerge(base, v);
          }).bind(bundle);
        });
      }();
    }, function (okey, instantiator) {
      var state = instantiator(obj);
      return Result.value($_g8mgb7xzje4cc0t9.wrap(okey, strength(state)));
    });
  };
  var cExtract = function (path, obj, fields, strength) {
    var results = $_elh0pqwsje4cc0p4.map(fields, function (field) {
      return cExtractOne(path, obj, field, strength);
    });
    return $_d029txxtje4cc0si.consolidateObj(results, {});
  };
  var value$2 = function (validator) {
    var extract = function (path, strength, val) {
      return validator(val, strength).fold(function (err) {
        return $_4f5o0yybje4cc0vb.custom(path, err);
      }, Result.value);
    };
    var toString = function () {
      return 'val';
    };
    var toDsl = function () {
      return $_6u1t5ayaje4cc0v9.typeAdt.itemOf(validator);
    };
    return {
      extract: extract,
      toString: toString,
      toDsl: toDsl
    };
  };
  var getSetKeys = function (obj) {
    var keys = $_i587hx0je4cc0pw.keys(obj);
    return $_elh0pqwsje4cc0p4.filter(keys, function (k) {
      return $_d08hr1xsje4cc0sg.hasKey(obj, k);
    });
  };
  var objOnly = function (fields) {
    var delegate = obj(fields);
    var fieldNames = $_elh0pqwsje4cc0p4.foldr(fields, function (acc, f) {
      return f.fold(function (key) {
        return $_dax12xwyje4cc0pt.deepMerge(acc, $_d08hr1xsje4cc0sg.wrap(key, true));
      }, $_aso7c6wjje4cc0om.constant(acc));
    }, {});
    var extract = function (path, strength, o) {
      var keys = $_eh3yfywzje4cc0pu.isBoolean(o) ? [] : getSetKeys(o);
      var extra = $_elh0pqwsje4cc0p4.filter(keys, function (k) {
        return !$_d08hr1xsje4cc0sg.hasKey(fieldNames, k);
      });
      return extra.length === 0 ? delegate.extract(path, strength, o) : $_4f5o0yybje4cc0vb.unsupportedFields(path, extra);
    };
    return {
      extract: extract,
      toString: delegate.toString,
      toDsl: delegate.toDsl
    };
  };
  var obj = function (fields) {
    var extract = function (path, strength, o) {
      return cExtract(path, o, fields, strength);
    };
    var toString = function () {
      var fieldStrings = $_elh0pqwsje4cc0p4.map(fields, function (field) {
        return field.fold(function (key, okey, presence, prop) {
          return key + ' -> ' + prop.toString();
        }, function (okey, instantiator) {
          return 'state(' + okey + ')';
        });
      });
      return 'obj{\n' + fieldStrings.join('\n') + '}';
    };
    var toDsl = function () {
      return $_6u1t5ayaje4cc0v9.typeAdt.objOf($_elh0pqwsje4cc0p4.map(fields, function (f) {
        return f.fold(function (key, okey, presence, prop) {
          return $_6u1t5ayaje4cc0v9.fieldAdt.field(key, presence, prop);
        }, function (okey, instantiator) {
          return $_6u1t5ayaje4cc0v9.fieldAdt.state(okey);
        });
      }));
    };
    return {
      extract: extract,
      toString: toString,
      toDsl: toDsl
    };
  };
  var arr = function (prop) {
    var extract = function (path, strength, array) {
      var results = $_elh0pqwsje4cc0p4.map(array, function (a, i) {
        return prop.extract(path.concat(['[' + i + ']']), strength, a);
      });
      return $_d029txxtje4cc0si.consolidateArr(results);
    };
    var toString = function () {
      return 'array(' + prop.toString() + ')';
    };
    var toDsl = function () {
      return $_6u1t5ayaje4cc0v9.typeAdt.arrOf(prop);
    };
    return {
      extract: extract,
      toString: toString,
      toDsl: toDsl
    };
  };
  var setOf = function (validator, prop) {
    var validateKeys = function (path, keys) {
      return arr(value$2(validator)).extract(path, $_aso7c6wjje4cc0om.identity, keys);
    };
    var extract = function (path, strength, o) {
      var keys = $_i587hx0je4cc0pw.keys(o);
      return validateKeys(path, keys).bind(function (validKeys) {
        var schema = $_elh0pqwsje4cc0p4.map(validKeys, function (vk) {
          return adt$1.field(vk, vk, $_b7tb36y8je4cc0uh.strict(), prop);
        });
        return obj(schema).extract(path, strength, o);
      });
    };
    var toString = function () {
      return 'setOf(' + prop.toString() + ')';
    };
    var toDsl = function () {
      return $_6u1t5ayaje4cc0v9.typeAdt.setOf(validator, prop);
    };
    return {
      extract: extract,
      toString: toString,
      toDsl: toDsl
    };
  };
  var func = function (args, schema, retriever) {
    var delegate = value$2(function (f, strength) {
      return $_eh3yfywzje4cc0pu.isFunction(f) ? Result.value(function () {
        var gArgs = Array.prototype.slice.call(arguments, 0);
        var allowedArgs = gArgs.slice(0, args.length);
        var o = f.apply(null, allowedArgs);
        return retriever(o, strength);
      }) : Result.error('Not a function');
    });
    return {
      extract: delegate.extract,
      toString: function () {
        return 'function';
      },
      toDsl: function () {
        return $_6u1t5ayaje4cc0v9.typeAdt.func(args, schema);
      }
    };
  };
  var thunk = function (desc, processor) {
    var getP = $_b57rigwlje4cc0oq.cached(function () {
      return processor();
    });
    var extract = function (path, strength, val) {
      return getP().extract(path, strength, val);
    };
    var toString = function () {
      return getP().toString();
    };
    var toDsl = function () {
      return $_6u1t5ayaje4cc0v9.typeAdt.thunk(desc);
    };
    return {
      extract: extract,
      toString: toString,
      toDsl: toDsl
    };
  };
  var anyValue = value$2(Result.value);
  var arrOfObj = $_aso7c6wjje4cc0om.compose(arr, obj);
  var $_e1weaby9je4cc0un = {
    anyValue: $_aso7c6wjje4cc0om.constant(anyValue),
    value: value$2,
    obj: obj,
    objOnly: objOnly,
    arr: arr,
    setOf: setOf,
    arrOfObj: arrOfObj,
    state: adt$1.state,
    field: adt$1.field,
    output: output,
    snapshot: snapshot,
    thunk: thunk,
    func: func
  };

  var strict = function (key) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.strict(), $_e1weaby9je4cc0un.anyValue());
  };
  var strictOf = function (key, schema) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.strict(), schema);
  };
  var strictFunction = function (key) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.strict(), $_e1weaby9je4cc0un.value(function (f) {
      return $_eh3yfywzje4cc0pu.isFunction(f) ? Result.value(f) : Result.error('Not a function');
    }));
  };
  var forbid = function (key, message) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.asOption(), $_e1weaby9je4cc0un.value(function (v) {
      return Result.error('The field: ' + key + ' is forbidden. ' + message);
    }));
  };
  var strictArrayOf = function (key, prop) {
    return strictOf(key, prop);
  };
  var strictObjOf = function (key, objSchema) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.strict(), $_e1weaby9je4cc0un.obj(objSchema));
  };
  var strictArrayOfObj = function (key, objFields) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.strict(), $_e1weaby9je4cc0un.arrOfObj(objFields));
  };
  var option = function (key) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.asOption(), $_e1weaby9je4cc0un.anyValue());
  };
  var optionOf = function (key, schema) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.asOption(), schema);
  };
  var optionObjOf = function (key, objSchema) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.asOption(), $_e1weaby9je4cc0un.obj(objSchema));
  };
  var optionObjOfOnly = function (key, objSchema) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.asOption(), $_e1weaby9je4cc0un.objOnly(objSchema));
  };
  var defaulted$1 = function (key, fallback) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.defaulted(fallback), $_e1weaby9je4cc0un.anyValue());
  };
  var defaultedOf = function (key, fallback, schema) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.defaulted(fallback), schema);
  };
  var defaultedObjOf = function (key, fallback, objSchema) {
    return $_e1weaby9je4cc0un.field(key, key, $_b7tb36y8je4cc0uh.defaulted(fallback), $_e1weaby9je4cc0un.obj(objSchema));
  };
  var field = function (key, okey, presence, prop) {
    return $_e1weaby9je4cc0un.field(key, okey, presence, prop);
  };
  var state = function (okey, instantiator) {
    return $_e1weaby9je4cc0un.state(okey, instantiator);
  };
  var $_c4iqkly7je4cc0ud = {
    strict: strict,
    strictOf: strictOf,
    strictObjOf: strictObjOf,
    strictArrayOf: strictArrayOf,
    strictArrayOfObj: strictArrayOfObj,
    strictFunction: strictFunction,
    forbid: forbid,
    option: option,
    optionOf: optionOf,
    optionObjOf: optionObjOf,
    optionObjOfOnly: optionObjOfOnly,
    defaulted: defaulted$1,
    defaultedOf: defaultedOf,
    defaultedObjOf: defaultedObjOf,
    field: field,
    state: state
  };

  var chooseFrom = function (path, strength, input, branches, ch) {
    var fields = $_d08hr1xsje4cc0sg.readOptFrom(branches, ch);
    return fields.fold(function () {
      return $_4f5o0yybje4cc0vb.missingBranch(path, branches, ch);
    }, function (fs) {
      return $_e1weaby9je4cc0un.obj(fs).extract(path.concat(['branch: ' + ch]), strength, input);
    });
  };
  var choose = function (key, branches) {
    var extract = function (path, strength, input) {
      var choice = $_d08hr1xsje4cc0sg.readOptFrom(input, key);
      return choice.fold(function () {
        return $_4f5o0yybje4cc0vb.missingKey(path, key);
      }, function (chosen) {
        return chooseFrom(path, strength, input, branches, chosen);
      });
    };
    var toString = function () {
      return 'chooseOn(' + key + '). Possible values: ' + $_i587hx0je4cc0pw.keys(branches);
    };
    var toDsl = function () {
      return $_6u1t5ayaje4cc0v9.typeAdt.choiceOf(key, branches);
    };
    return {
      extract: extract,
      toString: toString,
      toDsl: toDsl
    };
  };
  var $_a1e756yfje4cc0vq = { choose: choose };

  var anyValue$1 = $_e1weaby9je4cc0un.value(Result.value);
  var arrOfObj$1 = function (objFields) {
    return $_e1weaby9je4cc0un.arrOfObj(objFields);
  };
  var arrOfVal = function () {
    return $_e1weaby9je4cc0un.arr(anyValue$1);
  };
  var arrOf = $_e1weaby9je4cc0un.arr;
  var objOf = $_e1weaby9je4cc0un.obj;
  var objOfOnly = $_e1weaby9je4cc0un.objOnly;
  var setOf$1 = $_e1weaby9je4cc0un.setOf;
  var valueOf = function (validator) {
    return $_e1weaby9je4cc0un.value(function (v) {
      return validator(v);
    });
  };
  var extract = function (label, prop, strength, obj) {
    return prop.extract([label], strength, obj).fold(function (errs) {
      return Result.error({
        input: obj,
        errors: errs
      });
    }, Result.value);
  };
  var asStruct = function (label, prop, obj) {
    return extract(label, prop, $_aso7c6wjje4cc0om.constant, obj);
  };
  var asRaw = function (label, prop, obj) {
    return extract(label, prop, $_aso7c6wjje4cc0om.identity, obj);
  };
  var getOrDie$1 = function (extraction) {
    return extraction.fold(function (errInfo) {
      throw new Error(formatError(errInfo));
    }, $_aso7c6wjje4cc0om.identity);
  };
  var asRawOrDie = function (label, prop, obj) {
    return getOrDie$1(asRaw(label, prop, obj));
  };
  var asStructOrDie = function (label, prop, obj) {
    return getOrDie$1(asStruct(label, prop, obj));
  };
  var formatError = function (errInfo) {
    return 'Errors: \n' + $_ezauspycje4cc0ve.formatErrors(errInfo.errors) + '\n\nInput object: ' + $_ezauspycje4cc0ve.formatObj(errInfo.input);
  };
  var choose$1 = function (key, branches) {
    return $_a1e756yfje4cc0vq.choose(key, branches);
  };
  var thunkOf = function (desc, schema) {
    return $_e1weaby9je4cc0un.thunk(desc, schema);
  };
  var funcOrDie = function (args, schema) {
    var retriever = function (output, strength) {
      return getOrDie$1(extract('()', schema, strength, output));
    };
    return $_e1weaby9je4cc0un.func(args, schema, retriever);
  };
  var $_lbfbgyeje4cc0vl = {
    anyValue: $_aso7c6wjje4cc0om.constant(anyValue$1),
    arrOfObj: arrOfObj$1,
    arrOf: arrOf,
    arrOfVal: arrOfVal,
    valueOf: valueOf,
    setOf: setOf$1,
    objOf: objOf,
    objOfOnly: objOfOnly,
    asStruct: asStruct,
    asRaw: asRaw,
    asStructOrDie: asStructOrDie,
    asRawOrDie: asRawOrDie,
    getOrDie: getOrDie$1,
    formatError: formatError,
    choose: choose$1,
    thunkOf: thunkOf,
    funcOrDie: funcOrDie
  };

  var nu$4 = function (parts) {
    if (!$_d08hr1xsje4cc0sg.hasKey(parts, 'can') && !$_d08hr1xsje4cc0sg.hasKey(parts, 'abort') && !$_d08hr1xsje4cc0sg.hasKey(parts, 'run'))
      throw new Error('EventHandler defined by: ' + $_bsgtdtydje4cc0vk.stringify(parts, null, 2) + ' does not have can, abort, or run!');
    return $_lbfbgyeje4cc0vl.asRawOrDie('Extracting event.handler', $_lbfbgyeje4cc0vl.objOfOnly([
      $_c4iqkly7je4cc0ud.defaulted('can', $_aso7c6wjje4cc0om.constant(true)),
      $_c4iqkly7je4cc0ud.defaulted('abort', $_aso7c6wjje4cc0om.constant(false)),
      $_c4iqkly7je4cc0ud.defaulted('run', $_aso7c6wjje4cc0om.noop)
    ]), parts);
  };
  var all$1 = function (handlers, f) {
    return function () {
      var args = Array.prototype.slice.call(arguments, 0);
      return $_elh0pqwsje4cc0p4.foldl(handlers, function (acc, handler) {
        return acc && f(handler).apply(undefined, args);
      }, true);
    };
  };
  var any = function (handlers, f) {
    return function () {
      var args = Array.prototype.slice.call(arguments, 0);
      return $_elh0pqwsje4cc0p4.foldl(handlers, function (acc, handler) {
        return acc || f(handler).apply(undefined, args);
      }, false);
    };
  };
  var read = function (handler) {
    return $_eh3yfywzje4cc0pu.isFunction(handler) ? {
      can: $_aso7c6wjje4cc0om.constant(true),
      abort: $_aso7c6wjje4cc0om.constant(false),
      run: handler
    } : handler;
  };
  var fuse = function (handlers) {
    var can = all$1(handlers, function (handler) {
      return handler.can;
    });
    var abort = any(handlers, function (handler) {
      return handler.abort;
    });
    var run = function () {
      var args = Array.prototype.slice.call(arguments, 0);
      $_elh0pqwsje4cc0p4.each(handlers, function (handler) {
        handler.run.apply(undefined, args);
      });
    };
    return nu$4({
      can: can,
      abort: abort,
      run: run
    });
  };
  var $_agxnjiy6je4cc0u4 = {
    read: read,
    fuse: fuse,
    nu: nu$4
  };

  var derive = $_d08hr1xsje4cc0sg.wrapAll;
  var abort = function (name, predicate) {
    return {
      key: name,
      value: $_agxnjiy6je4cc0u4.nu({ abort: predicate })
    };
  };
  var can = function (name, predicate) {
    return {
      key: name,
      value: $_agxnjiy6je4cc0u4.nu({ can: predicate })
    };
  };
  var preventDefault = function (name) {
    return {
      key: name,
      value: $_agxnjiy6je4cc0u4.nu({
        run: function (component, simulatedEvent) {
          simulatedEvent.event().prevent();
        }
      })
    };
  };
  var run = function (name, handler) {
    return {
      key: name,
      value: $_agxnjiy6je4cc0u4.nu({ run: handler })
    };
  };
  var runActionExtra = function (name, action, extra) {
    return {
      key: name,
      value: $_agxnjiy6je4cc0u4.nu({
        run: function (component) {
          action.apply(undefined, [component].concat(extra));
        }
      })
    };
  };
  var runOnName = function (name) {
    return function (handler) {
      return run(name, handler);
    };
  };
  var runOnSourceName = function (name) {
    return function (handler) {
      return {
        key: name,
        value: $_agxnjiy6je4cc0u4.nu({
          run: function (component, simulatedEvent) {
            if ($_cum750y5je4cc0u1.isSource(component, simulatedEvent))
              handler(component, simulatedEvent);
          }
        })
      };
    };
  };
  var redirectToUid = function (name, uid) {
    return run(name, function (component, simulatedEvent) {
      component.getSystem().getByUid(uid).each(function (redirectee) {
        $_3fionhwgje4cc0o9.dispatchEvent(redirectee, redirectee.element(), name, simulatedEvent);
      });
    });
  };
  var redirectToPart = function (name, detail, partName) {
    var uid = detail.partUids()[partName];
    return redirectToUid(name, uid);
  };
  var runWithTarget = function (name, f) {
    return run(name, function (component, simulatedEvent) {
      component.getSystem().getByDom(simulatedEvent.event().target()).each(function (target) {
        f(component, target, simulatedEvent);
      });
    });
  };
  var cutter = function (name) {
    return run(name, function (component, simulatedEvent) {
      simulatedEvent.cut();
    });
  };
  var stopper = function (name) {
    return run(name, function (component, simulatedEvent) {
      simulatedEvent.stop();
    });
  };
  var $_ehtdq4y4je4cc0tx = {
    derive: derive,
    run: run,
    preventDefault: preventDefault,
    runActionExtra: runActionExtra,
    runOnAttached: runOnSourceName($_g6tooswhje4cc0of.attachedToDom()),
    runOnDetached: runOnSourceName($_g6tooswhje4cc0of.detachedFromDom()),
    runOnInit: runOnSourceName($_g6tooswhje4cc0of.systemInit()),
    runOnExecute: runOnName($_g6tooswhje4cc0of.execute()),
    redirectToUid: redirectToUid,
    redirectToPart: redirectToPart,
    runWithTarget: runWithTarget,
    abort: abort,
    can: can,
    cutter: cutter,
    stopper: stopper
  };

  var markAsBehaviourApi = function (f, apiName, apiFunction) {
    return f;
  };
  var markAsExtraApi = function (f, extraName) {
    return f;
  };
  var markAsSketchApi = function (f, apiFunction) {
    return f;
  };
  var getAnnotation = Option.none;
  var $_g16p40ygje4cc0vu = {
    markAsBehaviourApi: markAsBehaviourApi,
    markAsExtraApi: markAsExtraApi,
    markAsSketchApi: markAsSketchApi,
    getAnnotation: getAnnotation
  };

  var nu$5 = $_ajn35gx4je4cc0qj.immutableBag(['tag'], [
    'classes',
    'attributes',
    'styles',
    'value',
    'innerHtml',
    'domChildren',
    'defChildren'
  ]);
  var defToStr = function (defn) {
    var raw = defToRaw(defn);
    return $_bsgtdtydje4cc0vk.stringify(raw, null, 2);
  };
  var defToRaw = function (defn) {
    return {
      tag: defn.tag(),
      classes: defn.classes().getOr([]),
      attributes: defn.attributes().getOr({}),
      styles: defn.styles().getOr({}),
      value: defn.value().getOr('<none>'),
      innerHtml: defn.innerHtml().getOr('<none>'),
      defChildren: defn.defChildren().getOr('<none>'),
      domChildren: defn.domChildren().fold(function () {
        return '<none>';
      }, function (children) {
        return children.length === 0 ? '0 children, but still specified' : String(children.length);
      })
    };
  };
  var $_e0jxopyije4cc0w5 = {
    nu: nu$5,
    defToStr: defToStr,
    defToRaw: defToRaw
  };

  var fields = [
    'classes',
    'attributes',
    'styles',
    'value',
    'innerHtml',
    'defChildren',
    'domChildren'
  ];
  var nu$6 = $_ajn35gx4je4cc0qj.immutableBag([], fields);
  var derive$1 = function (settings) {
    var r = {};
    var keys = $_i587hx0je4cc0pw.keys(settings);
    $_elh0pqwsje4cc0p4.each(keys, function (key) {
      settings[key].each(function (v) {
        r[key] = v;
      });
    });
    return nu$6(r);
  };
  var modToStr = function (mod) {
    var raw = modToRaw(mod);
    return $_bsgtdtydje4cc0vk.stringify(raw, null, 2);
  };
  var modToRaw = function (mod) {
    return {
      classes: mod.classes().getOr('<none>'),
      attributes: mod.attributes().getOr('<none>'),
      styles: mod.styles().getOr('<none>'),
      value: mod.value().getOr('<none>'),
      innerHtml: mod.innerHtml().getOr('<none>'),
      defChildren: mod.defChildren().getOr('<none>'),
      domChildren: mod.domChildren().fold(function () {
        return '<none>';
      }, function (children) {
        return children.length === 0 ? '0 children, but still specified' : String(children.length);
      })
    };
  };
  var clashingOptArrays = function (key, oArr1, oArr2) {
    return oArr1.fold(function () {
      return oArr2.fold(function () {
        return {};
      }, function (arr2) {
        return $_d08hr1xsje4cc0sg.wrap(key, arr2);
      });
    }, function (arr1) {
      return oArr2.fold(function () {
        return $_d08hr1xsje4cc0sg.wrap(key, arr1);
      }, function (arr2) {
        return $_d08hr1xsje4cc0sg.wrap(key, arr2);
      });
    });
  };
  var merge$1 = function (defnA, mod) {
    var raw = $_dax12xwyje4cc0pt.deepMerge({
      tag: defnA.tag(),
      classes: mod.classes().getOr([]).concat(defnA.classes().getOr([])),
      attributes: $_dax12xwyje4cc0pt.merge(defnA.attributes().getOr({}), mod.attributes().getOr({})),
      styles: $_dax12xwyje4cc0pt.merge(defnA.styles().getOr({}), mod.styles().getOr({}))
    }, mod.innerHtml().or(defnA.innerHtml()).map(function (innerHtml) {
      return $_d08hr1xsje4cc0sg.wrap('innerHtml', innerHtml);
    }).getOr({}), clashingOptArrays('domChildren', mod.domChildren(), defnA.domChildren()), clashingOptArrays('defChildren', mod.defChildren(), defnA.defChildren()), mod.value().or(defnA.value()).map(function (value) {
      return $_d08hr1xsje4cc0sg.wrap('value', value);
    }).getOr({}));
    return $_e0jxopyije4cc0w5.nu(raw);
  };
  var $_5yi74lyhje4cc0vw = {
    nu: nu$6,
    derive: derive$1,
    merge: merge$1,
    modToStr: modToStr,
    modToRaw: modToRaw
  };

  var executeEvent = function (bConfig, bState, executor) {
    return $_ehtdq4y4je4cc0tx.runOnExecute(function (component) {
      executor(component, bConfig, bState);
    });
  };
  var loadEvent = function (bConfig, bState, f) {
    return $_ehtdq4y4je4cc0tx.runOnInit(function (component, simulatedEvent) {
      f(component, bConfig, bState);
    });
  };
  var create = function (schema, name, active, apis, extra, state) {
    var configSchema = $_lbfbgyeje4cc0vl.objOfOnly(schema);
    var schemaSchema = $_c4iqkly7je4cc0ud.optionObjOf(name, [$_c4iqkly7je4cc0ud.optionObjOfOnly('config', schema)]);
    return doCreate(configSchema, schemaSchema, name, active, apis, extra, state);
  };
  var createModes = function (modes, name, active, apis, extra, state) {
    var configSchema = modes;
    var schemaSchema = $_c4iqkly7je4cc0ud.optionObjOf(name, [$_c4iqkly7je4cc0ud.optionOf('config', modes)]);
    return doCreate(configSchema, schemaSchema, name, active, apis, extra, state);
  };
  var wrapApi = function (bName, apiFunction, apiName) {
    var f = function (component) {
      var args = arguments;
      return component.config({ name: $_aso7c6wjje4cc0om.constant(bName) }).fold(function () {
        throw new Error('We could not find any behaviour configuration for: ' + bName + '. Using API: ' + apiName);
      }, function (info) {
        var rest = Array.prototype.slice.call(args, 1);
        return apiFunction.apply(undefined, [
          component,
          info.config,
          info.state
        ].concat(rest));
      });
    };
    return $_g16p40ygje4cc0vu.markAsBehaviourApi(f, apiName, apiFunction);
  };
  var revokeBehaviour = function (name) {
    return {
      key: name,
      value: undefined
    };
  };
  var doCreate = function (configSchema, schemaSchema, name, active, apis, extra, state) {
    var getConfig = function (info) {
      return $_d08hr1xsje4cc0sg.hasKey(info, name) ? info[name]() : Option.none();
    };
    var wrappedApis = $_i587hx0je4cc0pw.map(apis, function (apiF, apiName) {
      return wrapApi(name, apiF, apiName);
    });
    var wrappedExtra = $_i587hx0je4cc0pw.map(extra, function (extraF, extraName) {
      return $_g16p40ygje4cc0vu.markAsExtraApi(extraF, extraName);
    });
    var me = $_dax12xwyje4cc0pt.deepMerge(wrappedExtra, wrappedApis, {
      revoke: $_aso7c6wjje4cc0om.curry(revokeBehaviour, name),
      config: function (spec) {
        var prepared = $_lbfbgyeje4cc0vl.asStructOrDie(name + '-config', configSchema, spec);
        return {
          key: name,
          value: {
            config: prepared,
            me: me,
            configAsRaw: $_b57rigwlje4cc0oq.cached(function () {
              return $_lbfbgyeje4cc0vl.asRawOrDie(name + '-config', configSchema, spec);
            }),
            initialConfig: spec,
            state: state
          }
        };
      },
      schema: function () {
        return schemaSchema;
      },
      exhibit: function (info, base) {
        return getConfig(info).bind(function (behaviourInfo) {
          return $_d08hr1xsje4cc0sg.readOptFrom(active, 'exhibit').map(function (exhibitor) {
            return exhibitor(base, behaviourInfo.config, behaviourInfo.state);
          });
        }).getOr($_5yi74lyhje4cc0vw.nu({}));
      },
      name: function () {
        return name;
      },
      handlers: function (info) {
        return getConfig(info).bind(function (behaviourInfo) {
          return $_d08hr1xsje4cc0sg.readOptFrom(active, 'events').map(function (events) {
            return events(behaviourInfo.config, behaviourInfo.state);
          });
        }).getOr({});
      }
    });
    return me;
  };
  var $_fc6d0uy3je4cc0tl = {
    executeEvent: executeEvent,
    loadEvent: loadEvent,
    create: create,
    createModes: createModes
  };

  var base = function (handleUnsupported, required) {
    return baseWith(handleUnsupported, required, {
      validate: $_eh3yfywzje4cc0pu.isFunction,
      label: 'function'
    });
  };
  var baseWith = function (handleUnsupported, required, pred) {
    if (required.length === 0)
      throw new Error('You must specify at least one required field.');
    $_g7vdlvx7je4cc0qn.validateStrArr('required', required);
    $_g7vdlvx7je4cc0qn.checkDupes(required);
    return function (obj) {
      var keys = $_i587hx0je4cc0pw.keys(obj);
      var allReqd = $_elh0pqwsje4cc0p4.forall(required, function (req) {
        return $_elh0pqwsje4cc0p4.contains(keys, req);
      });
      if (!allReqd)
        $_g7vdlvx7je4cc0qn.reqMessage(required, keys);
      handleUnsupported(required, keys);
      var invalidKeys = $_elh0pqwsje4cc0p4.filter(required, function (key) {
        return !pred.validate(obj[key], key);
      });
      if (invalidKeys.length > 0)
        $_g7vdlvx7je4cc0qn.invalidTypeMessage(invalidKeys, pred.label);
      return obj;
    };
  };
  var handleExact = function (required, keys) {
    var unsupported = $_elh0pqwsje4cc0p4.filter(keys, function (key) {
      return !$_elh0pqwsje4cc0p4.contains(required, key);
    });
    if (unsupported.length > 0)
      $_g7vdlvx7je4cc0qn.unsuppMessage(unsupported);
  };
  var allowExtra = $_aso7c6wjje4cc0om.noop;
  var $_arrccnylje4cc0wc = {
    exactly: $_aso7c6wjje4cc0om.curry(base, handleExact),
    ensure: $_aso7c6wjje4cc0om.curry(base, allowExtra),
    ensureWith: $_aso7c6wjje4cc0om.curry(baseWith, allowExtra)
  };

  var BehaviourState = $_arrccnylje4cc0wc.ensure(['readState']);

  var init = function () {
    return BehaviourState({
      readState: function () {
        return 'No State required';
      }
    });
  };
  var $_a5zdlbyjje4cc0w9 = { init: init };

  var derive$2 = function (capabilities) {
    return $_d08hr1xsje4cc0sg.wrapAll(capabilities);
  };
  var simpleSchema = $_lbfbgyeje4cc0vl.objOfOnly([
    $_c4iqkly7je4cc0ud.strict('fields'),
    $_c4iqkly7je4cc0ud.strict('name'),
    $_c4iqkly7je4cc0ud.defaulted('active', {}),
    $_c4iqkly7je4cc0ud.defaulted('apis', {}),
    $_c4iqkly7je4cc0ud.defaulted('extra', {}),
    $_c4iqkly7je4cc0ud.defaulted('state', $_a5zdlbyjje4cc0w9)
  ]);
  var create$1 = function (data) {
    var value = $_lbfbgyeje4cc0vl.asRawOrDie('Creating behaviour: ' + data.name, simpleSchema, data);
    return $_fc6d0uy3je4cc0tl.create(value.fields, value.name, value.active, value.apis, value.extra, value.state);
  };
  var modeSchema = $_lbfbgyeje4cc0vl.objOfOnly([
    $_c4iqkly7je4cc0ud.strict('branchKey'),
    $_c4iqkly7je4cc0ud.strict('branches'),
    $_c4iqkly7je4cc0ud.strict('name'),
    $_c4iqkly7je4cc0ud.defaulted('active', {}),
    $_c4iqkly7je4cc0ud.defaulted('apis', {}),
    $_c4iqkly7je4cc0ud.defaulted('extra', {}),
    $_c4iqkly7je4cc0ud.defaulted('state', $_a5zdlbyjje4cc0w9)
  ]);
  var createModes$1 = function (data) {
    var value = $_lbfbgyeje4cc0vl.asRawOrDie('Creating behaviour: ' + data.name, modeSchema, data);
    return $_fc6d0uy3je4cc0tl.createModes($_lbfbgyeje4cc0vl.choose(value.branchKey, value.branches), value.name, value.active, value.apis, value.extra, value.state);
  };
  var $_lsmliy2je4cc0te = {
    derive: derive$2,
    revoke: $_aso7c6wjje4cc0om.constant(undefined),
    noActive: $_aso7c6wjje4cc0om.constant({}),
    noApis: $_aso7c6wjje4cc0om.constant({}),
    noExtra: $_aso7c6wjje4cc0om.constant({}),
    noState: $_aso7c6wjje4cc0om.constant($_a5zdlbyjje4cc0w9),
    create: create$1,
    createModes: createModes$1
  };

  function Toggler (turnOff, turnOn, initial) {
    var active = initial || false;
    var on = function () {
      turnOn();
      active = true;
    };
    var off = function () {
      turnOff();
      active = false;
    };
    var toggle = function () {
      var f = active ? off : on;
      f();
    };
    var isOn = function () {
      return active;
    };
    return {
      on: on,
      off: off,
      toggle: toggle,
      isOn: isOn
    };
  }

  var read$1 = function (element, attr) {
    var value = $_bjjq6ixrje4cc0s9.get(element, attr);
    return value === undefined || value === '' ? [] : value.split(' ');
  };
  var add = function (element, attr, id) {
    var old = read$1(element, attr);
    var nu = old.concat([id]);
    $_bjjq6ixrje4cc0s9.set(element, attr, nu.join(' '));
  };
  var remove$2 = function (element, attr, id) {
    var nu = $_elh0pqwsje4cc0p4.filter(read$1(element, attr), function (v) {
      return v !== id;
    });
    if (nu.length > 0)
      $_bjjq6ixrje4cc0s9.set(element, attr, nu.join(' '));
    else
      $_bjjq6ixrje4cc0s9.remove(element, attr);
  };
  var $_fnupnjyqje4cc0wt = {
    read: read$1,
    add: add,
    remove: remove$2
  };

  var supports = function (element) {
    return element.dom().classList !== undefined;
  };
  var get$2 = function (element) {
    return $_fnupnjyqje4cc0wt.read(element, 'class');
  };
  var add$1 = function (element, clazz) {
    return $_fnupnjyqje4cc0wt.add(element, 'class', clazz);
  };
  var remove$3 = function (element, clazz) {
    return $_fnupnjyqje4cc0wt.remove(element, 'class', clazz);
  };
  var toggle = function (element, clazz) {
    if ($_elh0pqwsje4cc0p4.contains(get$2(element), clazz)) {
      remove$3(element, clazz);
    } else {
      add$1(element, clazz);
    }
  };
  var $_6xyio7ypje4cc0wl = {
    get: get$2,
    add: add$1,
    remove: remove$3,
    toggle: toggle,
    supports: supports
  };

  var add$2 = function (element, clazz) {
    if ($_6xyio7ypje4cc0wl.supports(element))
      element.dom().classList.add(clazz);
    else
      $_6xyio7ypje4cc0wl.add(element, clazz);
  };
  var cleanClass = function (element) {
    var classList = $_6xyio7ypje4cc0wl.supports(element) ? element.dom().classList : $_6xyio7ypje4cc0wl.get(element);
    if (classList.length === 0) {
      $_bjjq6ixrje4cc0s9.remove(element, 'class');
    }
  };
  var remove$4 = function (element, clazz) {
    if ($_6xyio7ypje4cc0wl.supports(element)) {
      var classList = element.dom().classList;
      classList.remove(clazz);
    } else
      $_6xyio7ypje4cc0wl.remove(element, clazz);
    cleanClass(element);
  };
  var toggle$1 = function (element, clazz) {
    return $_6xyio7ypje4cc0wl.supports(element) ? element.dom().classList.toggle(clazz) : $_6xyio7ypje4cc0wl.toggle(element, clazz);
  };
  var toggler = function (element, clazz) {
    var hasClasslist = $_6xyio7ypje4cc0wl.supports(element);
    var classList = element.dom().classList;
    var off = function () {
      if (hasClasslist)
        classList.remove(clazz);
      else
        $_6xyio7ypje4cc0wl.remove(element, clazz);
    };
    var on = function () {
      if (hasClasslist)
        classList.add(clazz);
      else
        $_6xyio7ypje4cc0wl.add(element, clazz);
    };
    return Toggler(off, on, has$1(element, clazz));
  };
  var has$1 = function (element, clazz) {
    return $_6xyio7ypje4cc0wl.supports(element) && element.dom().classList.contains(clazz);
  };
  var $_3x5q3zynje4cc0wi = {
    add: add$2,
    remove: remove$4,
    toggle: toggle$1,
    toggler: toggler,
    has: has$1
  };

  var swap = function (element, addCls, removeCls) {
    $_3x5q3zynje4cc0wi.remove(element, removeCls);
    $_3x5q3zynje4cc0wi.add(element, addCls);
  };
  var toAlpha = function (component, swapConfig, swapState) {
    swap(component.element(), swapConfig.alpha(), swapConfig.omega());
  };
  var toOmega = function (component, swapConfig, swapState) {
    swap(component.element(), swapConfig.omega(), swapConfig.alpha());
  };
  var clear = function (component, swapConfig, swapState) {
    $_3x5q3zynje4cc0wi.remove(component.element(), swapConfig.alpha());
    $_3x5q3zynje4cc0wi.remove(component.element(), swapConfig.omega());
  };
  var isAlpha = function (component, swapConfig, swapState) {
    return $_3x5q3zynje4cc0wi.has(component.element(), swapConfig.alpha());
  };
  var isOmega = function (component, swapConfig, swapState) {
    return $_3x5q3zynje4cc0wi.has(component.element(), swapConfig.omega());
  };
  var $_88cf65ymje4cc0wf = {
    toAlpha: toAlpha,
    toOmega: toOmega,
    isAlpha: isAlpha,
    isOmega: isOmega,
    clear: clear
  };

  var SwapSchema = [
    $_c4iqkly7je4cc0ud.strict('alpha'),
    $_c4iqkly7je4cc0ud.strict('omega')
  ];

  var Swapping = $_lsmliy2je4cc0te.create({
    fields: SwapSchema,
    name: 'swapping',
    apis: $_88cf65ymje4cc0wf
  });

  var Cell = function (initial) {
    var value = initial;
    var get = function () {
      return value;
    };
    var set = function (v) {
      value = v;
    };
    var clone = function () {
      return Cell(get());
    };
    return {
      get: get,
      set: set,
      clone: clone
    };
  };

  function ClosestOrAncestor (is, ancestor, scope, a, isRoot) {
    return is(scope, a) ? Option.some(scope) : $_eh3yfywzje4cc0pu.isFunction(isRoot) && isRoot(scope) ? Option.none() : ancestor(scope, a, isRoot);
  }

  var first$1 = function (predicate) {
    return descendant($_6jf42xxjje4cc0rm.body(), predicate);
  };
  var ancestor = function (scope, predicate, isRoot) {
    var element = scope.dom();
    var stop = $_eh3yfywzje4cc0pu.isFunction(isRoot) ? isRoot : $_aso7c6wjje4cc0om.constant(false);
    while (element.parentNode) {
      element = element.parentNode;
      var el = $_407ejqxfje4cc0rb.fromDom(element);
      if (predicate(el))
        return Option.some(el);
      else if (stop(el))
        break;
    }
    return Option.none();
  };
  var closest = function (scope, predicate, isRoot) {
    var is = function (scope) {
      return predicate(scope);
    };
    return ClosestOrAncestor(is, ancestor, scope, predicate, isRoot);
  };
  var sibling = function (scope, predicate) {
    var element = scope.dom();
    if (!element.parentNode)
      return Option.none();
    return child$1($_407ejqxfje4cc0rb.fromDom(element.parentNode), function (x) {
      return !$_1stme4x9je4cc0qq.eq(scope, x) && predicate(x);
    });
  };
  var child$1 = function (scope, predicate) {
    var result = $_elh0pqwsje4cc0p4.find(scope.dom().childNodes, $_aso7c6wjje4cc0om.compose(predicate, $_407ejqxfje4cc0rb.fromDom));
    return result.map($_407ejqxfje4cc0rb.fromDom);
  };
  var descendant = function (scope, predicate) {
    var descend = function (element) {
      for (var i = 0; i < element.childNodes.length; i++) {
        if (predicate($_407ejqxfje4cc0rb.fromDom(element.childNodes[i])))
          return Option.some($_407ejqxfje4cc0rb.fromDom(element.childNodes[i]));
        var res = descend(element.childNodes[i]);
        if (res.isSome())
          return res;
      }
      return Option.none();
    };
    return descend(scope.dom());
  };
  var $_40elmhyvje4cc0x6 = {
    first: first$1,
    ancestor: ancestor,
    closest: closest,
    sibling: sibling,
    child: child$1,
    descendant: descendant
  };

  var any$1 = function (predicate) {
    return $_40elmhyvje4cc0x6.first(predicate).isSome();
  };
  var ancestor$1 = function (scope, predicate, isRoot) {
    return $_40elmhyvje4cc0x6.ancestor(scope, predicate, isRoot).isSome();
  };
  var closest$1 = function (scope, predicate, isRoot) {
    return $_40elmhyvje4cc0x6.closest(scope, predicate, isRoot).isSome();
  };
  var sibling$1 = function (scope, predicate) {
    return $_40elmhyvje4cc0x6.sibling(scope, predicate).isSome();
  };
  var child$2 = function (scope, predicate) {
    return $_40elmhyvje4cc0x6.child(scope, predicate).isSome();
  };
  var descendant$1 = function (scope, predicate) {
    return $_40elmhyvje4cc0x6.descendant(scope, predicate).isSome();
  };
  var $_ah1hauyuje4cc0x4 = {
    any: any$1,
    ancestor: ancestor$1,
    closest: closest$1,
    sibling: sibling$1,
    child: child$2,
    descendant: descendant$1
  };

  var focus = function (element) {
    element.dom().focus();
  };
  var blur = function (element) {
    element.dom().blur();
  };
  var hasFocus = function (element) {
    var doc = $_1jwy92x3je4cc0qa.owner(element).dom();
    return element.dom() === doc.activeElement;
  };
  var active = function (_doc) {
    var doc = _doc !== undefined ? _doc.dom() : document;
    return Option.from(doc.activeElement).map($_407ejqxfje4cc0rb.fromDom);
  };
  var focusInside = function (element) {
    var doc = $_1jwy92x3je4cc0qa.owner(element);
    var inside = active(doc).filter(function (a) {
      return $_ah1hauyuje4cc0x4.closest(a, $_aso7c6wjje4cc0om.curry($_1stme4x9je4cc0qq.eq, element));
    });
    inside.fold(function () {
      focus(element);
    }, $_aso7c6wjje4cc0om.noop);
  };
  var search = function (element) {
    return active($_1jwy92x3je4cc0qa.owner(element)).filter(function (e) {
      return element.dom().contains(e.dom());
    });
  };
  var $_4hvdjzytje4cc0wz = {
    hasFocus: hasFocus,
    focus: focus,
    blur: blur,
    active: active,
    search: search,
    focusInside: focusInside
  };

  var DOMUtils = tinymce.util.Tools.resolve('tinymce.dom.DOMUtils');

  var ThemeManager = tinymce.util.Tools.resolve('tinymce.ThemeManager');

  var openLink = function (target) {
    var link = document.createElement('a');
    link.target = '_blank';
    link.href = target.href;
    link.rel = 'noreferrer noopener';
    var nuEvt = document.createEvent('MouseEvents');
    nuEvt.initMouseEvent('click', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
    document.body.appendChild(link);
    link.dispatchEvent(nuEvt);
    document.body.removeChild(link);
  };
  var $_1cg62byzje4cc0xg = { openLink: openLink };

  var isSkinDisabled = function (editor) {
    return editor.settings.skin === false;
  };
  var $_daxuk0z0je4cc0xh = { isSkinDisabled: isSkinDisabled };

  var formatChanged = 'formatChanged';
  var orientationChanged = 'orientationChanged';
  var dropupDismissed = 'dropupDismissed';
  var $_2txnauz1je4cc0xi = {
    formatChanged: $_aso7c6wjje4cc0om.constant(formatChanged),
    orientationChanged: $_aso7c6wjje4cc0om.constant(orientationChanged),
    dropupDismissed: $_aso7c6wjje4cc0om.constant(dropupDismissed)
  };

  var chooseChannels = function (channels, message) {
    return message.universal() ? channels : $_elh0pqwsje4cc0p4.filter(channels, function (ch) {
      return $_elh0pqwsje4cc0p4.contains(message.channels(), ch);
    });
  };
  var events = function (receiveConfig) {
    return $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.run($_g6tooswhje4cc0of.receive(), function (component, message) {
        var channelMap = receiveConfig.channels();
        var channels = $_i587hx0je4cc0pw.keys(channelMap);
        var targetChannels = chooseChannels(channels, message);
        $_elh0pqwsje4cc0p4.each(targetChannels, function (ch) {
          var channelInfo = channelMap[ch]();
          var channelSchema = channelInfo.schema();
          var data = $_lbfbgyeje4cc0vl.asStructOrDie('channel[' + ch + '] data\nReceiver: ' + $_dfrb81xmje4cc0rz.element(component.element()), channelSchema, message.data());
          channelInfo.onReceive()(component, data);
        });
      })]);
  };
  var $_17wm6vz4je4cc0xu = { events: events };

  var menuFields = [
    $_c4iqkly7je4cc0ud.strict('menu'),
    $_c4iqkly7je4cc0ud.strict('selectedMenu')
  ];
  var itemFields = [
    $_c4iqkly7je4cc0ud.strict('item'),
    $_c4iqkly7je4cc0ud.strict('selectedItem')
  ];
  var schema = $_lbfbgyeje4cc0vl.objOfOnly(itemFields.concat(menuFields));
  var itemSchema = $_lbfbgyeje4cc0vl.objOfOnly(itemFields);
  var $_1a6no2z7je4cc0ye = {
    menuFields: $_aso7c6wjje4cc0om.constant(menuFields),
    itemFields: $_aso7c6wjje4cc0om.constant(itemFields),
    schema: $_aso7c6wjje4cc0om.constant(schema),
    itemSchema: $_aso7c6wjje4cc0om.constant(itemSchema)
  };

  var initSize = $_c4iqkly7je4cc0ud.strictObjOf('initSize', [
    $_c4iqkly7je4cc0ud.strict('numColumns'),
    $_c4iqkly7je4cc0ud.strict('numRows')
  ]);
  var itemMarkers = function () {
    return $_c4iqkly7je4cc0ud.strictOf('markers', $_1a6no2z7je4cc0ye.itemSchema());
  };
  var menuMarkers = function () {
    return $_c4iqkly7je4cc0ud.strictOf('markers', $_1a6no2z7je4cc0ye.schema());
  };
  var tieredMenuMarkers = function () {
    return $_c4iqkly7je4cc0ud.strictObjOf('markers', [$_c4iqkly7je4cc0ud.strict('backgroundMenu')].concat($_1a6no2z7je4cc0ye.menuFields()).concat($_1a6no2z7je4cc0ye.itemFields()));
  };
  var markers = function (required) {
    return $_c4iqkly7je4cc0ud.strictObjOf('markers', $_elh0pqwsje4cc0p4.map(required, $_c4iqkly7je4cc0ud.strict));
  };
  var onPresenceHandler = function (label, fieldName, presence) {
    var trace = $_g603ovxlje4cc0rq.getTrace();
    return $_c4iqkly7je4cc0ud.field(fieldName, fieldName, presence, $_lbfbgyeje4cc0vl.valueOf(function (f) {
      return Result.value(function () {
        $_g603ovxlje4cc0rq.logHandler(label, fieldName, trace);
        return f.apply(undefined, arguments);
      });
    }));
  };
  var onHandler = function (fieldName) {
    return onPresenceHandler('onHandler', fieldName, $_b7tb36y8je4cc0uh.defaulted($_aso7c6wjje4cc0om.noop));
  };
  var onKeyboardHandler = function (fieldName) {
    return onPresenceHandler('onKeyboardHandler', fieldName, $_b7tb36y8je4cc0uh.defaulted(Option.none));
  };
  var onStrictHandler = function (fieldName) {
    return onPresenceHandler('onHandler', fieldName, $_b7tb36y8je4cc0uh.strict());
  };
  var onStrictKeyboardHandler = function (fieldName) {
    return onPresenceHandler('onKeyboardHandler', fieldName, $_b7tb36y8je4cc0uh.strict());
  };
  var output$1 = function (name, value) {
    return $_c4iqkly7je4cc0ud.state(name, $_aso7c6wjje4cc0om.constant(value));
  };
  var snapshot$1 = function (name) {
    return $_c4iqkly7je4cc0ud.state(name, $_aso7c6wjje4cc0om.identity);
  };
  var $_546z94z6je4cc0y4 = {
    initSize: $_aso7c6wjje4cc0om.constant(initSize),
    itemMarkers: itemMarkers,
    menuMarkers: menuMarkers,
    tieredMenuMarkers: tieredMenuMarkers,
    markers: markers,
    onHandler: onHandler,
    onKeyboardHandler: onKeyboardHandler,
    onStrictHandler: onStrictHandler,
    onStrictKeyboardHandler: onStrictKeyboardHandler,
    output: output$1,
    snapshot: snapshot$1
  };

  var ReceivingSchema = [$_c4iqkly7je4cc0ud.strictOf('channels', $_lbfbgyeje4cc0vl.setOf(Result.value, $_lbfbgyeje4cc0vl.objOfOnly([
      $_546z94z6je4cc0y4.onStrictHandler('onReceive'),
      $_c4iqkly7je4cc0ud.defaulted('schema', $_lbfbgyeje4cc0vl.anyValue())
    ])))];

  var Receiving = $_lsmliy2je4cc0te.create({
    fields: ReceivingSchema,
    name: 'receiving',
    active: $_17wm6vz4je4cc0xu
  });

  var updateAriaState = function (component, toggleConfig) {
    var pressed = isOn(component, toggleConfig);
    var ariaInfo = toggleConfig.aria();
    ariaInfo.update()(component, ariaInfo, pressed);
  };
  var toggle$2 = function (component, toggleConfig, toggleState) {
    $_3x5q3zynje4cc0wi.toggle(component.element(), toggleConfig.toggleClass());
    updateAriaState(component, toggleConfig);
  };
  var on = function (component, toggleConfig, toggleState) {
    $_3x5q3zynje4cc0wi.add(component.element(), toggleConfig.toggleClass());
    updateAriaState(component, toggleConfig);
  };
  var off = function (component, toggleConfig, toggleState) {
    $_3x5q3zynje4cc0wi.remove(component.element(), toggleConfig.toggleClass());
    updateAriaState(component, toggleConfig);
  };
  var isOn = function (component, toggleConfig) {
    return $_3x5q3zynje4cc0wi.has(component.element(), toggleConfig.toggleClass());
  };
  var onLoad = function (component, toggleConfig, toggleState) {
    var api = toggleConfig.selected() ? on : off;
    api(component, toggleConfig, toggleState);
  };
  var $_bobjihzaje4cc0yt = {
    onLoad: onLoad,
    toggle: toggle$2,
    isOn: isOn,
    on: on,
    off: off
  };

  var exhibit = function (base, toggleConfig, toggleState) {
    return $_5yi74lyhje4cc0vw.nu({});
  };
  var events$1 = function (toggleConfig, toggleState) {
    var execute = $_fc6d0uy3je4cc0tl.executeEvent(toggleConfig, toggleState, $_bobjihzaje4cc0yt.toggle);
    var load = $_fc6d0uy3je4cc0tl.loadEvent(toggleConfig, toggleState, $_bobjihzaje4cc0yt.onLoad);
    return $_ehtdq4y4je4cc0tx.derive($_elh0pqwsje4cc0p4.flatten([
      toggleConfig.toggleOnExecute() ? [execute] : [],
      [load]
    ]));
  };
  var $_dqz1ofz9je4cc0yq = {
    exhibit: exhibit,
    events: events$1
  };

  var updatePressed = function (component, ariaInfo, status) {
    $_bjjq6ixrje4cc0s9.set(component.element(), 'aria-pressed', status);
    if (ariaInfo.syncWithExpanded())
      updateExpanded(component, ariaInfo, status);
  };
  var updateSelected = function (component, ariaInfo, status) {
    $_bjjq6ixrje4cc0s9.set(component.element(), 'aria-selected', status);
  };
  var updateChecked = function (component, ariaInfo, status) {
    $_bjjq6ixrje4cc0s9.set(component.element(), 'aria-checked', status);
  };
  var updateExpanded = function (component, ariaInfo, status) {
    $_bjjq6ixrje4cc0s9.set(component.element(), 'aria-expanded', status);
  };
  var tagAttributes = {
    button: ['aria-pressed'],
    'input:checkbox': ['aria-checked']
  };
  var roleAttributes = {
    'button': ['aria-pressed'],
    'listbox': [
      'aria-pressed',
      'aria-expanded'
    ],
    'menuitemcheckbox': ['aria-checked']
  };
  var detectFromTag = function (component) {
    var elem = component.element();
    var rawTag = $_7qwxg2xkje4cc0ro.name(elem);
    var suffix = rawTag === 'input' && $_bjjq6ixrje4cc0s9.has(elem, 'type') ? ':' + $_bjjq6ixrje4cc0s9.get(elem, 'type') : '';
    return $_d08hr1xsje4cc0sg.readOptFrom(tagAttributes, rawTag + suffix);
  };
  var detectFromRole = function (component) {
    var elem = component.element();
    if (!$_bjjq6ixrje4cc0s9.has(elem, 'role'))
      return Option.none();
    else {
      var role = $_bjjq6ixrje4cc0s9.get(elem, 'role');
      return $_d08hr1xsje4cc0sg.readOptFrom(roleAttributes, role);
    }
  };
  var updateAuto = function (component, ariaInfo, status) {
    var attributes = detectFromRole(component).orThunk(function () {
      return detectFromTag(component);
    }).getOr([]);
    $_elh0pqwsje4cc0p4.each(attributes, function (attr) {
      $_bjjq6ixrje4cc0s9.set(component.element(), attr, status);
    });
  };
  var $_a8andizcje4cc0z1 = {
    updatePressed: updatePressed,
    updateSelected: updateSelected,
    updateChecked: updateChecked,
    updateExpanded: updateExpanded,
    updateAuto: updateAuto
  };

  var ToggleSchema = [
    $_c4iqkly7je4cc0ud.defaulted('selected', false),
    $_c4iqkly7je4cc0ud.strict('toggleClass'),
    $_c4iqkly7je4cc0ud.defaulted('toggleOnExecute', true),
    $_c4iqkly7je4cc0ud.defaultedOf('aria', { mode: 'none' }, $_lbfbgyeje4cc0vl.choose('mode', {
      'pressed': [
        $_c4iqkly7je4cc0ud.defaulted('syncWithExpanded', false),
        $_546z94z6je4cc0y4.output('update', $_a8andizcje4cc0z1.updatePressed)
      ],
      'checked': [$_546z94z6je4cc0y4.output('update', $_a8andizcje4cc0z1.updateChecked)],
      'expanded': [$_546z94z6je4cc0y4.output('update', $_a8andizcje4cc0z1.updateExpanded)],
      'selected': [$_546z94z6je4cc0y4.output('update', $_a8andizcje4cc0z1.updateSelected)],
      'none': [$_546z94z6je4cc0y4.output('update', $_aso7c6wjje4cc0om.noop)]
    }))
  ];

  var Toggling = $_lsmliy2je4cc0te.create({
    fields: ToggleSchema,
    name: 'toggling',
    active: $_dqz1ofz9je4cc0yq,
    apis: $_bobjihzaje4cc0yt
  });

  var format = function (command, update) {
    return Receiving.config({
      channels: $_d08hr1xsje4cc0sg.wrap($_2txnauz1je4cc0xi.formatChanged(), {
        onReceive: function (button, data) {
          if (data.command === command) {
            update(button, data.state);
          }
        }
      })
    });
  };
  var orientation = function (onReceive) {
    return Receiving.config({ channels: $_d08hr1xsje4cc0sg.wrap($_2txnauz1je4cc0xi.orientationChanged(), { onReceive: onReceive }) });
  };
  var receive = function (channel, onReceive) {
    return {
      key: channel,
      value: { onReceive: onReceive }
    };
  };
  var $_g6oxx7zdje4cc0za = {
    format: format,
    orientation: orientation,
    receive: receive
  };

  var prefix = 'tinymce-mobile';
  var resolve$1 = function (p) {
    return prefix + '-' + p;
  };
  var $_gfh4lpzeje4cc0zd = {
    resolve: resolve$1,
    prefix: $_aso7c6wjje4cc0om.constant(prefix)
  };

  var focus$1 = function (component, focusConfig) {
    if (!focusConfig.ignore()) {
      $_4hvdjzytje4cc0wz.focus(component.element());
      focusConfig.onFocus()(component);
    }
  };
  var blur$1 = function (component, focusConfig) {
    if (!focusConfig.ignore()) {
      $_4hvdjzytje4cc0wz.blur(component.element());
    }
  };
  var isFocused = function (component) {
    return $_4hvdjzytje4cc0wz.hasFocus(component.element());
  };
  var $_3nqn5jzjje4cc0zs = {
    focus: focus$1,
    blur: blur$1,
    isFocused: isFocused
  };

  var exhibit$1 = function (base, focusConfig) {
    if (focusConfig.ignore())
      return $_5yi74lyhje4cc0vw.nu({});
    else
      return $_5yi74lyhje4cc0vw.nu({ attributes: { 'tabindex': '-1' } });
  };
  var events$2 = function (focusConfig) {
    return $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.run($_g6tooswhje4cc0of.focus(), function (component, simulatedEvent) {
        $_3nqn5jzjje4cc0zs.focus(component, focusConfig);
        simulatedEvent.stop();
      })]);
  };
  var $_7s5mgizije4cc0zr = {
    exhibit: exhibit$1,
    events: events$2
  };

  var FocusSchema = [
    $_546z94z6je4cc0y4.onHandler('onFocus'),
    $_c4iqkly7je4cc0ud.defaulted('ignore', false)
  ];

  var Focusing = $_lsmliy2je4cc0te.create({
    fields: FocusSchema,
    name: 'focusing',
    active: $_7s5mgizije4cc0zr,
    apis: $_3nqn5jzjje4cc0zs
  });

  var $_3a3tymzpje4cc10f = {
    BACKSPACE: $_aso7c6wjje4cc0om.constant([8]),
    TAB: $_aso7c6wjje4cc0om.constant([9]),
    ENTER: $_aso7c6wjje4cc0om.constant([13]),
    SHIFT: $_aso7c6wjje4cc0om.constant([16]),
    CTRL: $_aso7c6wjje4cc0om.constant([17]),
    ALT: $_aso7c6wjje4cc0om.constant([18]),
    CAPSLOCK: $_aso7c6wjje4cc0om.constant([20]),
    ESCAPE: $_aso7c6wjje4cc0om.constant([27]),
    SPACE: $_aso7c6wjje4cc0om.constant([32]),
    PAGEUP: $_aso7c6wjje4cc0om.constant([33]),
    PAGEDOWN: $_aso7c6wjje4cc0om.constant([34]),
    END: $_aso7c6wjje4cc0om.constant([35]),
    HOME: $_aso7c6wjje4cc0om.constant([36]),
    LEFT: $_aso7c6wjje4cc0om.constant([37]),
    UP: $_aso7c6wjje4cc0om.constant([38]),
    RIGHT: $_aso7c6wjje4cc0om.constant([39]),
    DOWN: $_aso7c6wjje4cc0om.constant([40]),
    INSERT: $_aso7c6wjje4cc0om.constant([45]),
    DEL: $_aso7c6wjje4cc0om.constant([46]),
    META: $_aso7c6wjje4cc0om.constant([
      91,
      93,
      224
    ]),
    F10: $_aso7c6wjje4cc0om.constant([121])
  };

  var cycleBy = function (value, delta, min, max) {
    var r = value + delta;
    if (r > max)
      return min;
    else
      return r < min ? max : r;
  };
  var cap = function (value, min, max) {
    if (value <= min)
      return min;
    else
      return value >= max ? max : value;
  };
  var $_8sgjfkzuje4cc117 = {
    cycleBy: cycleBy,
    cap: cap
  };

  var all$2 = function (predicate) {
    return descendants($_6jf42xxjje4cc0rm.body(), predicate);
  };
  var ancestors = function (scope, predicate, isRoot) {
    return $_elh0pqwsje4cc0p4.filter($_1jwy92x3je4cc0qa.parents(scope, isRoot), predicate);
  };
  var siblings$1 = function (scope, predicate) {
    return $_elh0pqwsje4cc0p4.filter($_1jwy92x3je4cc0qa.siblings(scope), predicate);
  };
  var children$1 = function (scope, predicate) {
    return $_elh0pqwsje4cc0p4.filter($_1jwy92x3je4cc0qa.children(scope), predicate);
  };
  var descendants = function (scope, predicate) {
    var result = [];
    $_elh0pqwsje4cc0p4.each($_1jwy92x3je4cc0qa.children(scope), function (x) {
      if (predicate(x)) {
        result = result.concat([x]);
      }
      result = result.concat(descendants(x, predicate));
    });
    return result;
  };
  var $_2yfzl0zwje4cc11a = {
    all: all$2,
    ancestors: ancestors,
    siblings: siblings$1,
    children: children$1,
    descendants: descendants
  };

  var all$3 = function (selector) {
    return $_ab1wd9xeje4cc0r0.all(selector);
  };
  var ancestors$1 = function (scope, selector, isRoot) {
    return $_2yfzl0zwje4cc11a.ancestors(scope, function (e) {
      return $_ab1wd9xeje4cc0r0.is(e, selector);
    }, isRoot);
  };
  var siblings$2 = function (scope, selector) {
    return $_2yfzl0zwje4cc11a.siblings(scope, function (e) {
      return $_ab1wd9xeje4cc0r0.is(e, selector);
    });
  };
  var children$2 = function (scope, selector) {
    return $_2yfzl0zwje4cc11a.children(scope, function (e) {
      return $_ab1wd9xeje4cc0r0.is(e, selector);
    });
  };
  var descendants$1 = function (scope, selector) {
    return $_ab1wd9xeje4cc0r0.all(selector, scope);
  };
  var $_1n1hkzzvje4cc119 = {
    all: all$3,
    ancestors: ancestors$1,
    siblings: siblings$2,
    children: children$2,
    descendants: descendants$1
  };

  var first$2 = function (selector) {
    return $_ab1wd9xeje4cc0r0.one(selector);
  };
  var ancestor$2 = function (scope, selector, isRoot) {
    return $_40elmhyvje4cc0x6.ancestor(scope, function (e) {
      return $_ab1wd9xeje4cc0r0.is(e, selector);
    }, isRoot);
  };
  var sibling$2 = function (scope, selector) {
    return $_40elmhyvje4cc0x6.sibling(scope, function (e) {
      return $_ab1wd9xeje4cc0r0.is(e, selector);
    });
  };
  var child$3 = function (scope, selector) {
    return $_40elmhyvje4cc0x6.child(scope, function (e) {
      return $_ab1wd9xeje4cc0r0.is(e, selector);
    });
  };
  var descendant$2 = function (scope, selector) {
    return $_ab1wd9xeje4cc0r0.one(selector, scope);
  };
  var closest$2 = function (scope, selector, isRoot) {
    return ClosestOrAncestor($_ab1wd9xeje4cc0r0.is, ancestor$2, scope, selector, isRoot);
  };
  var $_by9cdqzxje4cc11d = {
    first: first$2,
    ancestor: ancestor$2,
    sibling: sibling$2,
    child: child$3,
    descendant: descendant$2,
    closest: closest$2
  };

  var dehighlightAll = function (component, hConfig, hState) {
    var highlighted = $_1n1hkzzvje4cc119.descendants(component.element(), '.' + hConfig.highlightClass());
    $_elh0pqwsje4cc0p4.each(highlighted, function (h) {
      $_3x5q3zynje4cc0wi.remove(h, hConfig.highlightClass());
      component.getSystem().getByDom(h).each(function (target) {
        hConfig.onDehighlight()(component, target);
      });
    });
  };
  var dehighlight = function (component, hConfig, hState, target) {
    var wasHighlighted = isHighlighted(component, hConfig, hState, target);
    $_3x5q3zynje4cc0wi.remove(target.element(), hConfig.highlightClass());
    if (wasHighlighted)
      hConfig.onDehighlight()(component, target);
  };
  var highlight = function (component, hConfig, hState, target) {
    var wasHighlighted = isHighlighted(component, hConfig, hState, target);
    dehighlightAll(component, hConfig, hState);
    $_3x5q3zynje4cc0wi.add(target.element(), hConfig.highlightClass());
    if (!wasHighlighted)
      hConfig.onHighlight()(component, target);
  };
  var highlightFirst = function (component, hConfig, hState) {
    getFirst(component, hConfig, hState).each(function (firstComp) {
      highlight(component, hConfig, hState, firstComp);
    });
  };
  var highlightLast = function (component, hConfig, hState) {
    getLast(component, hConfig, hState).each(function (lastComp) {
      highlight(component, hConfig, hState, lastComp);
    });
  };
  var highlightAt = function (component, hConfig, hState, index) {
    getByIndex(component, hConfig, hState, index).fold(function (err) {
      throw new Error(err);
    }, function (firstComp) {
      highlight(component, hConfig, hState, firstComp);
    });
  };
  var highlightBy = function (component, hConfig, hState, predicate) {
    var items = $_1n1hkzzvje4cc119.descendants(component.element(), '.' + hConfig.itemClass());
    var itemComps = $_egl8u6y0je4cc0tb.cat($_elh0pqwsje4cc0p4.map(items, function (i) {
      return component.getSystem().getByDom(i).toOption();
    }));
    var targetComp = $_elh0pqwsje4cc0p4.find(itemComps, predicate);
    targetComp.each(function (c) {
      highlight(component, hConfig, hState, c);
    });
  };
  var isHighlighted = function (component, hConfig, hState, queryTarget) {
    return $_3x5q3zynje4cc0wi.has(queryTarget.element(), hConfig.highlightClass());
  };
  var getHighlighted = function (component, hConfig, hState) {
    return $_by9cdqzxje4cc11d.descendant(component.element(), '.' + hConfig.highlightClass()).bind(component.getSystem().getByDom);
  };
  var getByIndex = function (component, hConfig, hState, index) {
    var items = $_1n1hkzzvje4cc119.descendants(component.element(), '.' + hConfig.itemClass());
    return Option.from(items[index]).fold(function () {
      return Result.error('No element found with index ' + index);
    }, component.getSystem().getByDom);
  };
  var getFirst = function (component, hConfig, hState) {
    return $_by9cdqzxje4cc11d.descendant(component.element(), '.' + hConfig.itemClass()).bind(component.getSystem().getByDom);
  };
  var getLast = function (component, hConfig, hState) {
    var items = $_1n1hkzzvje4cc119.descendants(component.element(), '.' + hConfig.itemClass());
    var last = items.length > 0 ? Option.some(items[items.length - 1]) : Option.none();
    return last.bind(component.getSystem().getByDom);
  };
  var getDelta = function (component, hConfig, hState, delta) {
    var items = $_1n1hkzzvje4cc119.descendants(component.element(), '.' + hConfig.itemClass());
    var current = $_elh0pqwsje4cc0p4.findIndex(items, function (item) {
      return $_3x5q3zynje4cc0wi.has(item, hConfig.highlightClass());
    });
    return current.bind(function (selected) {
      var dest = $_8sgjfkzuje4cc117.cycleBy(selected, delta, 0, items.length - 1);
      return component.getSystem().getByDom(items[dest]);
    });
  };
  var getPrevious = function (component, hConfig, hState) {
    return getDelta(component, hConfig, hState, -1);
  };
  var getNext = function (component, hConfig, hState) {
    return getDelta(component, hConfig, hState, +1);
  };
  var $_3eq8naztje4cc10w = {
    dehighlightAll: dehighlightAll,
    dehighlight: dehighlight,
    highlight: highlight,
    highlightFirst: highlightFirst,
    highlightLast: highlightLast,
    highlightAt: highlightAt,
    highlightBy: highlightBy,
    isHighlighted: isHighlighted,
    getHighlighted: getHighlighted,
    getFirst: getFirst,
    getLast: getLast,
    getPrevious: getPrevious,
    getNext: getNext
  };

  var HighlightSchema = [
    $_c4iqkly7je4cc0ud.strict('highlightClass'),
    $_c4iqkly7je4cc0ud.strict('itemClass'),
    $_546z94z6je4cc0y4.onHandler('onHighlight'),
    $_546z94z6je4cc0y4.onHandler('onDehighlight')
  ];

  var Highlighting = $_lsmliy2je4cc0te.create({
    fields: HighlightSchema,
    name: 'highlighting',
    apis: $_3eq8naztje4cc10w
  });

  var dom = function () {
    var get = function (component) {
      return $_4hvdjzytje4cc0wz.search(component.element());
    };
    var set = function (component, focusee) {
      component.getSystem().triggerFocus(focusee, component.element());
    };
    return {
      get: get,
      set: set
    };
  };
  var highlights = function () {
    var get = function (component) {
      return Highlighting.getHighlighted(component).map(function (item) {
        return item.element();
      });
    };
    var set = function (component, element) {
      component.getSystem().getByDom(element).fold($_aso7c6wjje4cc0om.noop, function (item) {
        Highlighting.highlight(component, item);
      });
    };
    return {
      get: get,
      set: set
    };
  };
  var $_17gb62zrje4cc10r = {
    dom: dom,
    highlights: highlights
  };

  var inSet = function (keys) {
    return function (event) {
      return $_elh0pqwsje4cc0p4.contains(keys, event.raw().which);
    };
  };
  var and = function (preds) {
    return function (event) {
      return $_elh0pqwsje4cc0p4.forall(preds, function (pred) {
        return pred(event);
      });
    };
  };
  var is$1 = function (key) {
    return function (event) {
      return event.raw().which === key;
    };
  };
  var isShift = function (event) {
    return event.raw().shiftKey === true;
  };
  var isControl = function (event) {
    return event.raw().ctrlKey === true;
  };
  var $_3v9fm1100je4cc11j = {
    inSet: inSet,
    and: and,
    is: is$1,
    isShift: isShift,
    isNotShift: $_aso7c6wjje4cc0om.not(isShift),
    isControl: isControl,
    isNotControl: $_aso7c6wjje4cc0om.not(isControl)
  };

  var basic = function (key, action) {
    return {
      matches: $_3v9fm1100je4cc11j.is(key),
      classification: action
    };
  };
  var rule = function (matches, action) {
    return {
      matches: matches,
      classification: action
    };
  };
  var choose$2 = function (transitions, event) {
    var transition = $_elh0pqwsje4cc0p4.find(transitions, function (t) {
      return t.matches(event);
    });
    return transition.map(function (t) {
      return t.classification;
    });
  };
  var $_7cg182zzje4cc11h = {
    basic: basic,
    rule: rule,
    choose: choose$2
  };

  var typical = function (infoSchema, stateInit, getRules, getEvents, getApis, optFocusIn) {
    var schema = function () {
      return infoSchema.concat([
        $_c4iqkly7je4cc0ud.defaulted('focusManager', $_17gb62zrje4cc10r.dom()),
        $_546z94z6je4cc0y4.output('handler', me),
        $_546z94z6je4cc0y4.output('state', stateInit)
      ]);
    };
    var processKey = function (component, simulatedEvent, keyingConfig, keyingState) {
      var rules = getRules(component, simulatedEvent, keyingConfig, keyingState);
      return $_7cg182zzje4cc11h.choose(rules, simulatedEvent.event()).bind(function (rule) {
        return rule(component, simulatedEvent, keyingConfig, keyingState);
      });
    };
    var toEvents = function (keyingConfig, keyingState) {
      var otherEvents = getEvents(keyingConfig, keyingState);
      var keyEvents = $_ehtdq4y4je4cc0tx.derive(optFocusIn.map(function (focusIn) {
        return $_ehtdq4y4je4cc0tx.run($_g6tooswhje4cc0of.focus(), function (component, simulatedEvent) {
          focusIn(component, keyingConfig, keyingState, simulatedEvent);
          simulatedEvent.stop();
        });
      }).toArray().concat([$_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.keydown(), function (component, simulatedEvent) {
          processKey(component, simulatedEvent, keyingConfig, keyingState).each(function (_) {
            simulatedEvent.stop();
          });
        })]));
      return $_dax12xwyje4cc0pt.deepMerge(otherEvents, keyEvents);
    };
    var me = {
      schema: schema,
      processKey: processKey,
      toEvents: toEvents,
      toApis: getApis
    };
    return me;
  };
  var $_81y96nzqje4cc10n = { typical: typical };

  var cyclePrev = function (values, index, predicate) {
    var before = $_elh0pqwsje4cc0p4.reverse(values.slice(0, index));
    var after = $_elh0pqwsje4cc0p4.reverse(values.slice(index + 1));
    return $_elh0pqwsje4cc0p4.find(before.concat(after), predicate);
  };
  var tryPrev = function (values, index, predicate) {
    var before = $_elh0pqwsje4cc0p4.reverse(values.slice(0, index));
    return $_elh0pqwsje4cc0p4.find(before, predicate);
  };
  var cycleNext = function (values, index, predicate) {
    var before = values.slice(0, index);
    var after = values.slice(index + 1);
    return $_elh0pqwsje4cc0p4.find(after.concat(before), predicate);
  };
  var tryNext = function (values, index, predicate) {
    var after = values.slice(index + 1);
    return $_elh0pqwsje4cc0p4.find(after, predicate);
  };
  var $_fo3syd101je4cc11n = {
    cyclePrev: cyclePrev,
    cycleNext: cycleNext,
    tryPrev: tryPrev,
    tryNext: tryNext
  };

  var isSupported = function (dom) {
    return dom.style !== undefined;
  };
  var $_9o716r104je4cc120 = { isSupported: isSupported };

  var internalSet = function (dom, property, value) {
    if (!$_eh3yfywzje4cc0pu.isString(value)) {
      console.error('Invalid call to CSS.set. Property ', property, ':: Value ', value, ':: Element ', dom);
      throw new Error('CSS value must be a string: ' + value);
    }
    if ($_9o716r104je4cc120.isSupported(dom))
      dom.style.setProperty(property, value);
  };
  var internalRemove = function (dom, property) {
    if ($_9o716r104je4cc120.isSupported(dom))
      dom.style.removeProperty(property);
  };
  var set$2 = function (element, property, value) {
    var dom = element.dom();
    internalSet(dom, property, value);
  };
  var setAll$1 = function (element, css) {
    var dom = element.dom();
    $_i587hx0je4cc0pw.each(css, function (v, k) {
      internalSet(dom, k, v);
    });
  };
  var setOptions = function (element, css) {
    var dom = element.dom();
    $_i587hx0je4cc0pw.each(css, function (v, k) {
      v.fold(function () {
        internalRemove(dom, k);
      }, function (value) {
        internalSet(dom, k, value);
      });
    });
  };
  var get$3 = function (element, property) {
    var dom = element.dom();
    var styles = window.getComputedStyle(dom);
    var r = styles.getPropertyValue(property);
    var v = r === '' && !$_6jf42xxjje4cc0rm.inBody(element) ? getUnsafeProperty(dom, property) : r;
    return v === null ? undefined : v;
  };
  var getUnsafeProperty = function (dom, property) {
    return $_9o716r104je4cc120.isSupported(dom) ? dom.style.getPropertyValue(property) : '';
  };
  var getRaw = function (element, property) {
    var dom = element.dom();
    var raw = getUnsafeProperty(dom, property);
    return Option.from(raw).filter(function (r) {
      return r.length > 0;
    });
  };
  var getAllRaw = function (element) {
    var css = {};
    var dom = element.dom();
    if ($_9o716r104je4cc120.isSupported(dom)) {
      for (var i = 0; i < dom.style.length; i++) {
        var ruleName = dom.style.item(i);
        css[ruleName] = dom.style[ruleName];
      }
    }
    return css;
  };
  var isValidValue = function (tag, property, value) {
    var element = $_407ejqxfje4cc0rb.fromTag(tag);
    set$2(element, property, value);
    var style = getRaw(element, property);
    return style.isSome();
  };
  var remove$5 = function (element, property) {
    var dom = element.dom();
    internalRemove(dom, property);
    if ($_bjjq6ixrje4cc0s9.has(element, 'style') && $_eehsrpwvje4cc0pp.trim($_bjjq6ixrje4cc0s9.get(element, 'style')) === '') {
      $_bjjq6ixrje4cc0s9.remove(element, 'style');
    }
  };
  var preserve = function (element, f) {
    var oldStyles = $_bjjq6ixrje4cc0s9.get(element, 'style');
    var result = f(element);
    var restore = oldStyles === undefined ? $_bjjq6ixrje4cc0s9.remove : $_bjjq6ixrje4cc0s9.set;
    restore(element, 'style', oldStyles);
    return result;
  };
  var copy$1 = function (source, target) {
    var sourceDom = source.dom();
    var targetDom = target.dom();
    if ($_9o716r104je4cc120.isSupported(sourceDom) && $_9o716r104je4cc120.isSupported(targetDom)) {
      targetDom.style.cssText = sourceDom.style.cssText;
    }
  };
  var reflow = function (e) {
    return e.dom().offsetWidth;
  };
  var transferOne$1 = function (source, destination, style) {
    getRaw(source, style).each(function (value) {
      if (getRaw(destination, style).isNone())
        set$2(destination, style, value);
    });
  };
  var transfer$1 = function (source, destination, styles) {
    if (!$_7qwxg2xkje4cc0ro.isElement(source) || !$_7qwxg2xkje4cc0ro.isElement(destination))
      return;
    $_elh0pqwsje4cc0p4.each(styles, function (style) {
      transferOne$1(source, destination, style);
    });
  };
  var $_7ojow7103je4cc11s = {
    copy: copy$1,
    set: set$2,
    preserve: preserve,
    setAll: setAll$1,
    setOptions: setOptions,
    remove: remove$5,
    get: get$3,
    getRaw: getRaw,
    getAllRaw: getAllRaw,
    isValidValue: isValidValue,
    reflow: reflow,
    transfer: transfer$1
  };

  function Dimension (name, getOffset) {
    var set = function (element, h) {
      if (!$_eh3yfywzje4cc0pu.isNumber(h) && !h.match(/^[0-9]+$/))
        throw name + '.set accepts only positive integer values. Value was ' + h;
      var dom = element.dom();
      if ($_9o716r104je4cc120.isSupported(dom))
        dom.style[name] = h + 'px';
    };
    var get = function (element) {
      var r = getOffset(element);
      if (r <= 0 || r === null) {
        var css = $_7ojow7103je4cc11s.get(element, name);
        return parseFloat(css) || 0;
      }
      return r;
    };
    var getOuter = get;
    var aggregate = function (element, properties) {
      return $_elh0pqwsje4cc0p4.foldl(properties, function (acc, property) {
        var val = $_7ojow7103je4cc11s.get(element, property);
        var value = val === undefined ? 0 : parseInt(val, 10);
        return isNaN(value) ? acc : acc + value;
      }, 0);
    };
    var max = function (element, value, properties) {
      var cumulativeInclusions = aggregate(element, properties);
      var absoluteMax = value > cumulativeInclusions ? value - cumulativeInclusions : 0;
      return absoluteMax;
    };
    return {
      set: set,
      get: get,
      getOuter: getOuter,
      aggregate: aggregate,
      max: max
    };
  }

  var api = Dimension('height', function (element) {
    return $_6jf42xxjje4cc0rm.inBody(element) ? element.dom().getBoundingClientRect().height : element.dom().offsetHeight;
  });
  var set$3 = function (element, h) {
    api.set(element, h);
  };
  var get$4 = function (element) {
    return api.get(element);
  };
  var getOuter$1 = function (element) {
    return api.getOuter(element);
  };
  var setMax = function (element, value) {
    var inclusions = [
      'margin-top',
      'border-top-width',
      'padding-top',
      'padding-bottom',
      'border-bottom-width',
      'margin-bottom'
    ];
    var absMax = api.max(element, value, inclusions);
    $_7ojow7103je4cc11s.set(element, 'max-height', absMax + 'px');
  };
  var $_eftfr9102je4cc11q = {
    set: set$3,
    get: get$4,
    getOuter: getOuter$1,
    setMax: setMax
  };

  var create$2 = function (cyclicField) {
    var schema = [
      $_c4iqkly7je4cc0ud.option('onEscape'),
      $_c4iqkly7je4cc0ud.option('onEnter'),
      $_c4iqkly7je4cc0ud.defaulted('selector', '[data-alloy-tabstop="true"]'),
      $_c4iqkly7je4cc0ud.defaulted('firstTabstop', 0),
      $_c4iqkly7je4cc0ud.defaulted('useTabstopAt', $_aso7c6wjje4cc0om.constant(true)),
      $_c4iqkly7je4cc0ud.option('visibilitySelector')
    ].concat([cyclicField]);
    var isVisible = function (tabbingConfig, element) {
      var target = tabbingConfig.visibilitySelector().bind(function (sel) {
        return $_by9cdqzxje4cc11d.closest(element, sel);
      }).getOr(element);
      return $_eftfr9102je4cc11q.get(target) > 0;
    };
    var findInitial = function (component, tabbingConfig) {
      var tabstops = $_1n1hkzzvje4cc119.descendants(component.element(), tabbingConfig.selector());
      var visibles = $_elh0pqwsje4cc0p4.filter(tabstops, function (elem) {
        return isVisible(tabbingConfig, elem);
      });
      return Option.from(visibles[tabbingConfig.firstTabstop()]);
    };
    var findCurrent = function (component, tabbingConfig) {
      return tabbingConfig.focusManager().get(component).bind(function (elem) {
        return $_by9cdqzxje4cc11d.closest(elem, tabbingConfig.selector());
      });
    };
    var isTabstop = function (tabbingConfig, element) {
      return isVisible(tabbingConfig, element) && tabbingConfig.useTabstopAt()(element);
    };
    var focusIn = function (component, tabbingConfig, tabbingState) {
      findInitial(component, tabbingConfig).each(function (target) {
        tabbingConfig.focusManager().set(component, target);
      });
    };
    var goFromTabstop = function (component, tabstops, stopIndex, tabbingConfig, cycle) {
      return cycle(tabstops, stopIndex, function (elem) {
        return isTabstop(tabbingConfig, elem);
      }).fold(function () {
        return tabbingConfig.cyclic() ? Option.some(true) : Option.none();
      }, function (target) {
        tabbingConfig.focusManager().set(component, target);
        return Option.some(true);
      });
    };
    var go = function (component, simulatedEvent, tabbingConfig, cycle) {
      var tabstops = $_1n1hkzzvje4cc119.descendants(component.element(), tabbingConfig.selector());
      return findCurrent(component, tabbingConfig).bind(function (tabstop) {
        var optStopIndex = $_elh0pqwsje4cc0p4.findIndex(tabstops, $_aso7c6wjje4cc0om.curry($_1stme4x9je4cc0qq.eq, tabstop));
        return optStopIndex.bind(function (stopIndex) {
          return goFromTabstop(component, tabstops, stopIndex, tabbingConfig, cycle);
        });
      });
    };
    var goBackwards = function (component, simulatedEvent, tabbingConfig, tabbingState) {
      var navigate = tabbingConfig.cyclic() ? $_fo3syd101je4cc11n.cyclePrev : $_fo3syd101je4cc11n.tryPrev;
      return go(component, simulatedEvent, tabbingConfig, navigate);
    };
    var goForwards = function (component, simulatedEvent, tabbingConfig, tabbingState) {
      var navigate = tabbingConfig.cyclic() ? $_fo3syd101je4cc11n.cycleNext : $_fo3syd101je4cc11n.tryNext;
      return go(component, simulatedEvent, tabbingConfig, navigate);
    };
    var execute = function (component, simulatedEvent, tabbingConfig, tabbingState) {
      return tabbingConfig.onEnter().bind(function (f) {
        return f(component, simulatedEvent);
      });
    };
    var exit = function (component, simulatedEvent, tabbingConfig, tabbingState) {
      return tabbingConfig.onEscape().bind(function (f) {
        return f(component, simulatedEvent);
      });
    };
    var getRules = $_aso7c6wjje4cc0om.constant([
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
        $_3v9fm1100je4cc11j.isShift,
        $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB())
      ]), goBackwards),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB()), goForwards),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ESCAPE()), exit),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
        $_3v9fm1100je4cc11j.isNotShift,
        $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ENTER())
      ]), execute)
    ]);
    var getEvents = $_aso7c6wjje4cc0om.constant({});
    var getApis = $_aso7c6wjje4cc0om.constant({});
    return $_81y96nzqje4cc10n.typical(schema, $_a5zdlbyjje4cc0w9.init, getRules, getEvents, getApis, Option.some(focusIn));
  };
  var $_bg6qvdzoje4cc103 = { create: create$2 };

  var AcyclicType = $_bg6qvdzoje4cc103.create($_c4iqkly7je4cc0ud.state('cyclic', $_aso7c6wjje4cc0om.constant(false)));

  var CyclicType = $_bg6qvdzoje4cc103.create($_c4iqkly7je4cc0ud.state('cyclic', $_aso7c6wjje4cc0om.constant(true)));

  var inside = function (target) {
    return $_7qwxg2xkje4cc0ro.name(target) === 'input' && $_bjjq6ixrje4cc0s9.get(target, 'type') !== 'radio' || $_7qwxg2xkje4cc0ro.name(target) === 'textarea';
  };
  var $_e3dsll108je4cc12k = { inside: inside };

  var doDefaultExecute = function (component, simulatedEvent, focused) {
    $_3fionhwgje4cc0o9.dispatch(component, focused, $_g6tooswhje4cc0of.execute());
    return Option.some(true);
  };
  var defaultExecute = function (component, simulatedEvent, focused) {
    return $_e3dsll108je4cc12k.inside(focused) && $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.SPACE())(simulatedEvent.event()) ? Option.none() : doDefaultExecute(component, simulatedEvent, focused);
  };
  var $_2pd4ek109je4cc12p = { defaultExecute: defaultExecute };

  var schema$1 = [
    $_c4iqkly7je4cc0ud.defaulted('execute', $_2pd4ek109je4cc12p.defaultExecute),
    $_c4iqkly7je4cc0ud.defaulted('useSpace', false),
    $_c4iqkly7je4cc0ud.defaulted('useEnter', true),
    $_c4iqkly7je4cc0ud.defaulted('useControlEnter', false),
    $_c4iqkly7je4cc0ud.defaulted('useDown', false)
  ];
  var execute = function (component, simulatedEvent, executeConfig, executeState) {
    return executeConfig.execute()(component, simulatedEvent, component.element());
  };
  var getRules = function (component, simulatedEvent, executeConfig, executeState) {
    var spaceExec = executeConfig.useSpace() && !$_e3dsll108je4cc12k.inside(component.element()) ? $_3a3tymzpje4cc10f.SPACE() : [];
    var enterExec = executeConfig.useEnter() ? $_3a3tymzpje4cc10f.ENTER() : [];
    var downExec = executeConfig.useDown() ? $_3a3tymzpje4cc10f.DOWN() : [];
    var execKeys = spaceExec.concat(enterExec).concat(downExec);
    return [$_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet(execKeys), execute)].concat(executeConfig.useControlEnter() ? [$_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
        $_3v9fm1100je4cc11j.isControl,
        $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ENTER())
      ]), execute)] : []);
  };
  var getEvents = $_aso7c6wjje4cc0om.constant({});
  var getApis = $_aso7c6wjje4cc0om.constant({});
  var ExecutionType = $_81y96nzqje4cc10n.typical(schema$1, $_a5zdlbyjje4cc0w9.init, getRules, getEvents, getApis, Option.none());

  var flatgrid = function (spec) {
    var dimensions = Cell(Option.none());
    var setGridSize = function (numRows, numColumns) {
      dimensions.set(Option.some({
        numRows: $_aso7c6wjje4cc0om.constant(numRows),
        numColumns: $_aso7c6wjje4cc0om.constant(numColumns)
      }));
    };
    var getNumRows = function () {
      return dimensions.get().map(function (d) {
        return d.numRows();
      });
    };
    var getNumColumns = function () {
      return dimensions.get().map(function (d) {
        return d.numColumns();
      });
    };
    return BehaviourState({
      readState: $_aso7c6wjje4cc0om.constant({}),
      setGridSize: setGridSize,
      getNumRows: getNumRows,
      getNumColumns: getNumColumns
    });
  };
  var init$1 = function (spec) {
    return spec.state()(spec);
  };
  var $_fch41710bje4cc12z = {
    flatgrid: flatgrid,
    init: init$1
  };

  var onDirection = function (isLtr, isRtl) {
    return function (element) {
      return getDirection(element) === 'rtl' ? isRtl : isLtr;
    };
  };
  var getDirection = function (element) {
    return $_7ojow7103je4cc11s.get(element, 'direction') === 'rtl' ? 'rtl' : 'ltr';
  };
  var $_e2xxqr10dje4cc137 = {
    onDirection: onDirection,
    getDirection: getDirection
  };

  var useH = function (movement) {
    return function (component, simulatedEvent, config, state) {
      var move = movement(component.element());
      return use(move, component, simulatedEvent, config, state);
    };
  };
  var west = function (moveLeft, moveRight) {
    var movement = $_e2xxqr10dje4cc137.onDirection(moveLeft, moveRight);
    return useH(movement);
  };
  var east = function (moveLeft, moveRight) {
    var movement = $_e2xxqr10dje4cc137.onDirection(moveRight, moveLeft);
    return useH(movement);
  };
  var useV = function (move) {
    return function (component, simulatedEvent, config, state) {
      return use(move, component, simulatedEvent, config, state);
    };
  };
  var use = function (move, component, simulatedEvent, config, state) {
    var outcome = config.focusManager().get(component).bind(function (focused) {
      return move(component.element(), focused, config, state);
    });
    return outcome.map(function (newFocus) {
      config.focusManager().set(component, newFocus);
      return true;
    });
  };
  var $_6jl8vn10cje4cc134 = {
    east: east,
    west: west,
    north: useV,
    south: useV,
    move: useV
  };

  var indexInfo = $_ajn35gx4je4cc0qj.immutableBag([
    'index',
    'candidates'
  ], []);
  var locate = function (candidates, predicate) {
    return $_elh0pqwsje4cc0p4.findIndex(candidates, predicate).map(function (index) {
      return indexInfo({
        index: index,
        candidates: candidates
      });
    });
  };
  var $_3uovfv10fje4cc13f = { locate: locate };

  var visibilityToggler = function (element, property, hiddenValue, visibleValue) {
    var initial = $_7ojow7103je4cc11s.get(element, property);
    if (initial === undefined)
      initial = '';
    var value = initial === hiddenValue ? visibleValue : hiddenValue;
    var off = $_aso7c6wjje4cc0om.curry($_7ojow7103je4cc11s.set, element, property, initial);
    var on = $_aso7c6wjje4cc0om.curry($_7ojow7103je4cc11s.set, element, property, value);
    return Toggler(off, on, false);
  };
  var toggler$1 = function (element) {
    return visibilityToggler(element, 'visibility', 'hidden', 'visible');
  };
  var displayToggler = function (element, value) {
    return visibilityToggler(element, 'display', 'none', value);
  };
  var isHidden = function (dom) {
    return dom.offsetWidth <= 0 && dom.offsetHeight <= 0;
  };
  var isVisible = function (element) {
    var dom = element.dom();
    return !isHidden(dom);
  };
  var $_5lbsg210gje4cc13i = {
    toggler: toggler$1,
    displayToggler: displayToggler,
    isVisible: isVisible
  };

  var locateVisible = function (container, current, selector) {
    var filter = $_5lbsg210gje4cc13i.isVisible;
    return locateIn(container, current, selector, filter);
  };
  var locateIn = function (container, current, selector, filter) {
    var predicate = $_aso7c6wjje4cc0om.curry($_1stme4x9je4cc0qq.eq, current);
    var candidates = $_1n1hkzzvje4cc119.descendants(container, selector);
    var visible = $_elh0pqwsje4cc0p4.filter(candidates, $_5lbsg210gje4cc13i.isVisible);
    return $_3uovfv10fje4cc13f.locate(visible, predicate);
  };
  var findIndex$2 = function (elements, target) {
    return $_elh0pqwsje4cc0p4.findIndex(elements, function (elem) {
      return $_1stme4x9je4cc0qq.eq(target, elem);
    });
  };
  var $_95dyv610eje4cc138 = {
    locateVisible: locateVisible,
    locateIn: locateIn,
    findIndex: findIndex$2
  };

  var withGrid = function (values, index, numCols, f) {
    var oldRow = Math.floor(index / numCols);
    var oldColumn = index % numCols;
    return f(oldRow, oldColumn).bind(function (address) {
      var newIndex = address.row() * numCols + address.column();
      return newIndex >= 0 && newIndex < values.length ? Option.some(values[newIndex]) : Option.none();
    });
  };
  var cycleHorizontal = function (values, index, numRows, numCols, delta) {
    return withGrid(values, index, numCols, function (oldRow, oldColumn) {
      var onLastRow = oldRow === numRows - 1;
      var colsInRow = onLastRow ? values.length - oldRow * numCols : numCols;
      var newColumn = $_8sgjfkzuje4cc117.cycleBy(oldColumn, delta, 0, colsInRow - 1);
      return Option.some({
        row: $_aso7c6wjje4cc0om.constant(oldRow),
        column: $_aso7c6wjje4cc0om.constant(newColumn)
      });
    });
  };
  var cycleVertical = function (values, index, numRows, numCols, delta) {
    return withGrid(values, index, numCols, function (oldRow, oldColumn) {
      var newRow = $_8sgjfkzuje4cc117.cycleBy(oldRow, delta, 0, numRows - 1);
      var onLastRow = newRow === numRows - 1;
      var colsInRow = onLastRow ? values.length - newRow * numCols : numCols;
      var newCol = $_8sgjfkzuje4cc117.cap(oldColumn, 0, colsInRow - 1);
      return Option.some({
        row: $_aso7c6wjje4cc0om.constant(newRow),
        column: $_aso7c6wjje4cc0om.constant(newCol)
      });
    });
  };
  var cycleRight = function (values, index, numRows, numCols) {
    return cycleHorizontal(values, index, numRows, numCols, +1);
  };
  var cycleLeft = function (values, index, numRows, numCols) {
    return cycleHorizontal(values, index, numRows, numCols, -1);
  };
  var cycleUp = function (values, index, numRows, numCols) {
    return cycleVertical(values, index, numRows, numCols, -1);
  };
  var cycleDown = function (values, index, numRows, numCols) {
    return cycleVertical(values, index, numRows, numCols, +1);
  };
  var $_9jmlpx10hje4cc13n = {
    cycleDown: cycleDown,
    cycleUp: cycleUp,
    cycleLeft: cycleLeft,
    cycleRight: cycleRight
  };

  var schema$2 = [
    $_c4iqkly7je4cc0ud.strict('selector'),
    $_c4iqkly7je4cc0ud.defaulted('execute', $_2pd4ek109je4cc12p.defaultExecute),
    $_546z94z6je4cc0y4.onKeyboardHandler('onEscape'),
    $_c4iqkly7je4cc0ud.defaulted('captureTab', false),
    $_546z94z6je4cc0y4.initSize()
  ];
  var focusIn = function (component, gridConfig, gridState) {
    $_by9cdqzxje4cc11d.descendant(component.element(), gridConfig.selector()).each(function (first) {
      gridConfig.focusManager().set(component, first);
    });
  };
  var findCurrent = function (component, gridConfig) {
    return gridConfig.focusManager().get(component).bind(function (elem) {
      return $_by9cdqzxje4cc11d.closest(elem, gridConfig.selector());
    });
  };
  var execute$1 = function (component, simulatedEvent, gridConfig, gridState) {
    return findCurrent(component, gridConfig).bind(function (focused) {
      return gridConfig.execute()(component, simulatedEvent, focused);
    });
  };
  var doMove = function (cycle) {
    return function (element, focused, gridConfig, gridState) {
      return $_95dyv610eje4cc138.locateVisible(element, focused, gridConfig.selector()).bind(function (identified) {
        return cycle(identified.candidates(), identified.index(), gridState.getNumRows().getOr(gridConfig.initSize().numRows()), gridState.getNumColumns().getOr(gridConfig.initSize().numColumns()));
      });
    };
  };
  var handleTab = function (component, simulatedEvent, gridConfig, gridState) {
    return gridConfig.captureTab() ? Option.some(true) : Option.none();
  };
  var doEscape = function (component, simulatedEvent, gridConfig, gridState) {
    return gridConfig.onEscape()(component, simulatedEvent);
  };
  var moveLeft = doMove($_9jmlpx10hje4cc13n.cycleLeft);
  var moveRight = doMove($_9jmlpx10hje4cc13n.cycleRight);
  var moveNorth = doMove($_9jmlpx10hje4cc13n.cycleUp);
  var moveSouth = doMove($_9jmlpx10hje4cc13n.cycleDown);
  var getRules$1 = $_aso7c6wjje4cc0om.constant([
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.LEFT()), $_6jl8vn10cje4cc134.west(moveLeft, moveRight)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.RIGHT()), $_6jl8vn10cje4cc134.east(moveLeft, moveRight)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.UP()), $_6jl8vn10cje4cc134.north(moveNorth)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.DOWN()), $_6jl8vn10cje4cc134.south(moveSouth)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
      $_3v9fm1100je4cc11j.isShift,
      $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB())
    ]), handleTab),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
      $_3v9fm1100je4cc11j.isNotShift,
      $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB())
    ]), handleTab),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ESCAPE()), doEscape),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.SPACE().concat($_3a3tymzpje4cc10f.ENTER())), execute$1)
  ]);
  var getEvents$1 = $_aso7c6wjje4cc0om.constant({});
  var getApis$1 = {};
  var FlatgridType = $_81y96nzqje4cc10n.typical(schema$2, $_fch41710bje4cc12z.flatgrid, getRules$1, getEvents$1, getApis$1, Option.some(focusIn));

  var horizontal = function (container, selector, current, delta) {
    return $_95dyv610eje4cc138.locateVisible(container, current, selector, $_aso7c6wjje4cc0om.constant(true)).bind(function (identified) {
      var index = identified.index();
      var candidates = identified.candidates();
      var newIndex = $_8sgjfkzuje4cc117.cycleBy(index, delta, 0, candidates.length - 1);
      return Option.from(candidates[newIndex]);
    });
  };
  var $_gilp1s10jje4cc13z = { horizontal: horizontal };

  var schema$3 = [
    $_c4iqkly7je4cc0ud.strict('selector'),
    $_c4iqkly7je4cc0ud.defaulted('getInitial', Option.none),
    $_c4iqkly7je4cc0ud.defaulted('execute', $_2pd4ek109je4cc12p.defaultExecute),
    $_c4iqkly7je4cc0ud.defaulted('executeOnMove', false)
  ];
  var findCurrent$1 = function (component, flowConfig) {
    return flowConfig.focusManager().get(component).bind(function (elem) {
      return $_by9cdqzxje4cc11d.closest(elem, flowConfig.selector());
    });
  };
  var execute$2 = function (component, simulatedEvent, flowConfig) {
    return findCurrent$1(component, flowConfig).bind(function (focused) {
      return flowConfig.execute()(component, simulatedEvent, focused);
    });
  };
  var focusIn$1 = function (component, flowConfig) {
    flowConfig.getInitial()(component).or($_by9cdqzxje4cc11d.descendant(component.element(), flowConfig.selector())).each(function (first) {
      flowConfig.focusManager().set(component, first);
    });
  };
  var moveLeft$1 = function (element, focused, info) {
    return $_gilp1s10jje4cc13z.horizontal(element, info.selector(), focused, -1);
  };
  var moveRight$1 = function (element, focused, info) {
    return $_gilp1s10jje4cc13z.horizontal(element, info.selector(), focused, +1);
  };
  var doMove$1 = function (movement) {
    return function (component, simulatedEvent, flowConfig) {
      return movement(component, simulatedEvent, flowConfig).bind(function () {
        return flowConfig.executeOnMove() ? execute$2(component, simulatedEvent, flowConfig) : Option.some(true);
      });
    };
  };
  var getRules$2 = function (_) {
    return [
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.LEFT().concat($_3a3tymzpje4cc10f.UP())), doMove$1($_6jl8vn10cje4cc134.west(moveLeft$1, moveRight$1))),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.RIGHT().concat($_3a3tymzpje4cc10f.DOWN())), doMove$1($_6jl8vn10cje4cc134.east(moveLeft$1, moveRight$1))),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ENTER()), execute$2),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.SPACE()), execute$2)
    ];
  };
  var getEvents$2 = $_aso7c6wjje4cc0om.constant({});
  var getApis$2 = $_aso7c6wjje4cc0om.constant({});
  var FlowType = $_81y96nzqje4cc10n.typical(schema$3, $_a5zdlbyjje4cc0w9.init, getRules$2, getEvents$2, getApis$2, Option.some(focusIn$1));

  var outcome = $_ajn35gx4je4cc0qj.immutableBag([
    'rowIndex',
    'columnIndex',
    'cell'
  ], []);
  var toCell = function (matrix, rowIndex, columnIndex) {
    return Option.from(matrix[rowIndex]).bind(function (row) {
      return Option.from(row[columnIndex]).map(function (cell) {
        return outcome({
          rowIndex: rowIndex,
          columnIndex: columnIndex,
          cell: cell
        });
      });
    });
  };
  var cycleHorizontal$1 = function (matrix, rowIndex, startCol, deltaCol) {
    var row = matrix[rowIndex];
    var colsInRow = row.length;
    var newColIndex = $_8sgjfkzuje4cc117.cycleBy(startCol, deltaCol, 0, colsInRow - 1);
    return toCell(matrix, rowIndex, newColIndex);
  };
  var cycleVertical$1 = function (matrix, colIndex, startRow, deltaRow) {
    var nextRowIndex = $_8sgjfkzuje4cc117.cycleBy(startRow, deltaRow, 0, matrix.length - 1);
    var colsInNextRow = matrix[nextRowIndex].length;
    var nextColIndex = $_8sgjfkzuje4cc117.cap(colIndex, 0, colsInNextRow - 1);
    return toCell(matrix, nextRowIndex, nextColIndex);
  };
  var moveHorizontal = function (matrix, rowIndex, startCol, deltaCol) {
    var row = matrix[rowIndex];
    var colsInRow = row.length;
    var newColIndex = $_8sgjfkzuje4cc117.cap(startCol + deltaCol, 0, colsInRow - 1);
    return toCell(matrix, rowIndex, newColIndex);
  };
  var moveVertical = function (matrix, colIndex, startRow, deltaRow) {
    var nextRowIndex = $_8sgjfkzuje4cc117.cap(startRow + deltaRow, 0, matrix.length - 1);
    var colsInNextRow = matrix[nextRowIndex].length;
    var nextColIndex = $_8sgjfkzuje4cc117.cap(colIndex, 0, colsInNextRow - 1);
    return toCell(matrix, nextRowIndex, nextColIndex);
  };
  var cycleRight$1 = function (matrix, startRow, startCol) {
    return cycleHorizontal$1(matrix, startRow, startCol, +1);
  };
  var cycleLeft$1 = function (matrix, startRow, startCol) {
    return cycleHorizontal$1(matrix, startRow, startCol, -1);
  };
  var cycleUp$1 = function (matrix, startRow, startCol) {
    return cycleVertical$1(matrix, startCol, startRow, -1);
  };
  var cycleDown$1 = function (matrix, startRow, startCol) {
    return cycleVertical$1(matrix, startCol, startRow, +1);
  };
  var moveLeft$2 = function (matrix, startRow, startCol) {
    return moveHorizontal(matrix, startRow, startCol, -1);
  };
  var moveRight$2 = function (matrix, startRow, startCol) {
    return moveHorizontal(matrix, startRow, startCol, +1);
  };
  var moveUp = function (matrix, startRow, startCol) {
    return moveVertical(matrix, startCol, startRow, -1);
  };
  var moveDown = function (matrix, startRow, startCol) {
    return moveVertical(matrix, startCol, startRow, +1);
  };
  var $_b6xwwg10lje4cc14k = {
    cycleRight: cycleRight$1,
    cycleLeft: cycleLeft$1,
    cycleUp: cycleUp$1,
    cycleDown: cycleDown$1,
    moveLeft: moveLeft$2,
    moveRight: moveRight$2,
    moveUp: moveUp,
    moveDown: moveDown
  };

  var schema$4 = [
    $_c4iqkly7je4cc0ud.strictObjOf('selectors', [
      $_c4iqkly7je4cc0ud.strict('row'),
      $_c4iqkly7je4cc0ud.strict('cell')
    ]),
    $_c4iqkly7je4cc0ud.defaulted('cycles', true),
    $_c4iqkly7je4cc0ud.defaulted('previousSelector', Option.none),
    $_c4iqkly7je4cc0ud.defaulted('execute', $_2pd4ek109je4cc12p.defaultExecute)
  ];
  var focusIn$2 = function (component, matrixConfig) {
    var focused = matrixConfig.previousSelector()(component).orThunk(function () {
      var selectors = matrixConfig.selectors();
      return $_by9cdqzxje4cc11d.descendant(component.element(), selectors.cell());
    });
    focused.each(function (cell) {
      matrixConfig.focusManager().set(component, cell);
    });
  };
  var execute$3 = function (component, simulatedEvent, matrixConfig) {
    return $_4hvdjzytje4cc0wz.search(component.element()).bind(function (focused) {
      return matrixConfig.execute()(component, simulatedEvent, focused);
    });
  };
  var toMatrix = function (rows, matrixConfig) {
    return $_elh0pqwsje4cc0p4.map(rows, function (row) {
      return $_1n1hkzzvje4cc119.descendants(row, matrixConfig.selectors().cell());
    });
  };
  var doMove$2 = function (ifCycle, ifMove) {
    return function (element, focused, matrixConfig) {
      var move = matrixConfig.cycles() ? ifCycle : ifMove;
      return $_by9cdqzxje4cc11d.closest(focused, matrixConfig.selectors().row()).bind(function (inRow) {
        var cellsInRow = $_1n1hkzzvje4cc119.descendants(inRow, matrixConfig.selectors().cell());
        return $_95dyv610eje4cc138.findIndex(cellsInRow, focused).bind(function (colIndex) {
          var allRows = $_1n1hkzzvje4cc119.descendants(element, matrixConfig.selectors().row());
          return $_95dyv610eje4cc138.findIndex(allRows, inRow).bind(function (rowIndex) {
            var matrix = toMatrix(allRows, matrixConfig);
            return move(matrix, rowIndex, colIndex).map(function (next) {
              return next.cell();
            });
          });
        });
      });
    };
  };
  var moveLeft$3 = doMove$2($_b6xwwg10lje4cc14k.cycleLeft, $_b6xwwg10lje4cc14k.moveLeft);
  var moveRight$3 = doMove$2($_b6xwwg10lje4cc14k.cycleRight, $_b6xwwg10lje4cc14k.moveRight);
  var moveNorth$1 = doMove$2($_b6xwwg10lje4cc14k.cycleUp, $_b6xwwg10lje4cc14k.moveUp);
  var moveSouth$1 = doMove$2($_b6xwwg10lje4cc14k.cycleDown, $_b6xwwg10lje4cc14k.moveDown);
  var getRules$3 = $_aso7c6wjje4cc0om.constant([
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.LEFT()), $_6jl8vn10cje4cc134.west(moveLeft$3, moveRight$3)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.RIGHT()), $_6jl8vn10cje4cc134.east(moveLeft$3, moveRight$3)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.UP()), $_6jl8vn10cje4cc134.north(moveNorth$1)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.DOWN()), $_6jl8vn10cje4cc134.south(moveSouth$1)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.SPACE().concat($_3a3tymzpje4cc10f.ENTER())), execute$3)
  ]);
  var getEvents$3 = $_aso7c6wjje4cc0om.constant({});
  var getApis$3 = $_aso7c6wjje4cc0om.constant({});
  var MatrixType = $_81y96nzqje4cc10n.typical(schema$4, $_a5zdlbyjje4cc0w9.init, getRules$3, getEvents$3, getApis$3, Option.some(focusIn$2));

  var schema$5 = [
    $_c4iqkly7je4cc0ud.strict('selector'),
    $_c4iqkly7je4cc0ud.defaulted('execute', $_2pd4ek109je4cc12p.defaultExecute),
    $_c4iqkly7je4cc0ud.defaulted('moveOnTab', false)
  ];
  var execute$4 = function (component, simulatedEvent, menuConfig) {
    return menuConfig.focusManager().get(component).bind(function (focused) {
      return menuConfig.execute()(component, simulatedEvent, focused);
    });
  };
  var focusIn$3 = function (component, menuConfig, simulatedEvent) {
    $_by9cdqzxje4cc11d.descendant(component.element(), menuConfig.selector()).each(function (first) {
      menuConfig.focusManager().set(component, first);
    });
  };
  var moveUp$1 = function (element, focused, info) {
    return $_gilp1s10jje4cc13z.horizontal(element, info.selector(), focused, -1);
  };
  var moveDown$1 = function (element, focused, info) {
    return $_gilp1s10jje4cc13z.horizontal(element, info.selector(), focused, +1);
  };
  var fireShiftTab = function (component, simulatedEvent, menuConfig) {
    return menuConfig.moveOnTab() ? $_6jl8vn10cje4cc134.move(moveUp$1)(component, simulatedEvent, menuConfig) : Option.none();
  };
  var fireTab = function (component, simulatedEvent, menuConfig) {
    return menuConfig.moveOnTab() ? $_6jl8vn10cje4cc134.move(moveDown$1)(component, simulatedEvent, menuConfig) : Option.none();
  };
  var getRules$4 = $_aso7c6wjje4cc0om.constant([
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.UP()), $_6jl8vn10cje4cc134.move(moveUp$1)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.DOWN()), $_6jl8vn10cje4cc134.move(moveDown$1)),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
      $_3v9fm1100je4cc11j.isShift,
      $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB())
    ]), fireShiftTab),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
      $_3v9fm1100je4cc11j.isNotShift,
      $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB())
    ]), fireTab),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ENTER()), execute$4),
    $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.SPACE()), execute$4)
  ]);
  var getEvents$4 = $_aso7c6wjje4cc0om.constant({});
  var getApis$4 = $_aso7c6wjje4cc0om.constant({});
  var MenuType = $_81y96nzqje4cc10n.typical(schema$5, $_a5zdlbyjje4cc0w9.init, getRules$4, getEvents$4, getApis$4, Option.some(focusIn$3));

  var schema$6 = [
    $_546z94z6je4cc0y4.onKeyboardHandler('onSpace'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onEnter'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onShiftEnter'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onLeft'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onRight'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onTab'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onShiftTab'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onUp'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onDown'),
    $_546z94z6je4cc0y4.onKeyboardHandler('onEscape'),
    $_c4iqkly7je4cc0ud.option('focusIn')
  ];
  var getRules$5 = function (component, simulatedEvent, executeInfo) {
    return [
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.SPACE()), executeInfo.onSpace()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
        $_3v9fm1100je4cc11j.isNotShift,
        $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ENTER())
      ]), executeInfo.onEnter()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
        $_3v9fm1100je4cc11j.isShift,
        $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ENTER())
      ]), executeInfo.onShiftEnter()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
        $_3v9fm1100je4cc11j.isShift,
        $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB())
      ]), executeInfo.onShiftTab()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.and([
        $_3v9fm1100je4cc11j.isNotShift,
        $_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.TAB())
      ]), executeInfo.onTab()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.UP()), executeInfo.onUp()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.DOWN()), executeInfo.onDown()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.LEFT()), executeInfo.onLeft()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.RIGHT()), executeInfo.onRight()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.SPACE()), executeInfo.onSpace()),
      $_7cg182zzje4cc11h.rule($_3v9fm1100je4cc11j.inSet($_3a3tymzpje4cc10f.ESCAPE()), executeInfo.onEscape())
    ];
  };
  var focusIn$4 = function (component, executeInfo) {
    return executeInfo.focusIn().bind(function (f) {
      return f(component, executeInfo);
    });
  };
  var getEvents$5 = $_aso7c6wjje4cc0om.constant({});
  var getApis$5 = $_aso7c6wjje4cc0om.constant({});
  var SpecialType = $_81y96nzqje4cc10n.typical(schema$6, $_a5zdlbyjje4cc0w9.init, getRules$5, getEvents$5, getApis$5, Option.some(focusIn$4));

  var $_jtlgezmje4cc0zz = {
    acyclic: AcyclicType.schema(),
    cyclic: CyclicType.schema(),
    flow: FlowType.schema(),
    flatgrid: FlatgridType.schema(),
    matrix: MatrixType.schema(),
    execution: ExecutionType.schema(),
    menu: MenuType.schema(),
    special: SpecialType.schema()
  };

  var Keying = $_lsmliy2je4cc0te.createModes({
    branchKey: 'mode',
    branches: $_jtlgezmje4cc0zz,
    name: 'keying',
    active: {
      events: function (keyingConfig, keyingState) {
        var handler = keyingConfig.handler();
        return handler.toEvents(keyingConfig, keyingState);
      }
    },
    apis: {
      focusIn: function (component) {
        component.getSystem().triggerFocus(component.element(), component.element());
      },
      setGridSize: function (component, keyConfig, keyState, numRows, numColumns) {
        if (!$_d08hr1xsje4cc0sg.hasKey(keyState, 'setGridSize')) {
          console.error('Layout does not support setGridSize');
        } else {
          keyState.setGridSize(numRows, numColumns);
        }
      }
    },
    state: $_fch41710bje4cc12z
  });

  var field$1 = function (name, forbidden) {
    return $_c4iqkly7je4cc0ud.defaultedObjOf(name, {}, $_elh0pqwsje4cc0p4.map(forbidden, function (f) {
      return $_c4iqkly7je4cc0ud.forbid(f.name(), 'Cannot configure ' + f.name() + ' for ' + name);
    }).concat([$_c4iqkly7je4cc0ud.state('dump', $_aso7c6wjje4cc0om.identity)]));
  };
  var get$5 = function (data) {
    return data.dump();
  };
  var $_2sk6q710oje4cc152 = {
    field: field$1,
    get: get$5
  };

  var unique = 0;
  var generate$1 = function (prefix) {
    var date = new Date();
    var time = date.getTime();
    var random = Math.floor(Math.random() * 1000000000);
    unique++;
    return prefix + '_' + random + unique + String(time);
  };
  var $_eb3s8410rje4cc15l = { generate: generate$1 };

  var premadeTag = $_eb3s8410rje4cc15l.generate('alloy-premade');
  var apiConfig = $_eb3s8410rje4cc15l.generate('api');
  var premade = function (comp) {
    return $_d08hr1xsje4cc0sg.wrap(premadeTag, comp);
  };
  var getPremade = function (spec) {
    return $_d08hr1xsje4cc0sg.readOptFrom(spec, premadeTag);
  };
  var makeApi = function (f) {
    return $_g16p40ygje4cc0vu.markAsSketchApi(function (component) {
      var args = Array.prototype.slice.call(arguments, 0);
      var spi = component.config(apiConfig);
      return f.apply(undefined, [spi].concat(args));
    }, f);
  };
  var $_3c5fj310qje4cc15g = {
    apiConfig: $_aso7c6wjje4cc0om.constant(apiConfig),
    makeApi: makeApi,
    premade: premade,
    getPremade: getPremade
  };

  var adt$2 = $_170ozvxwje4cc0st.generate([
    { required: ['data'] },
    { external: ['data'] },
    { optional: ['data'] },
    { group: ['data'] }
  ]);
  var fFactory = $_c4iqkly7je4cc0ud.defaulted('factory', { sketch: $_aso7c6wjje4cc0om.identity });
  var fSchema = $_c4iqkly7je4cc0ud.defaulted('schema', []);
  var fName = $_c4iqkly7je4cc0ud.strict('name');
  var fPname = $_c4iqkly7je4cc0ud.field('pname', 'pname', $_b7tb36y8je4cc0uh.defaultedThunk(function (typeSpec) {
    return '<alloy.' + $_eb3s8410rje4cc15l.generate(typeSpec.name) + '>';
  }), $_lbfbgyeje4cc0vl.anyValue());
  var fDefaults = $_c4iqkly7je4cc0ud.defaulted('defaults', $_aso7c6wjje4cc0om.constant({}));
  var fOverrides = $_c4iqkly7je4cc0ud.defaulted('overrides', $_aso7c6wjje4cc0om.constant({}));
  var requiredSpec = $_lbfbgyeje4cc0vl.objOf([
    fFactory,
    fSchema,
    fName,
    fPname,
    fDefaults,
    fOverrides
  ]);
  var externalSpec = $_lbfbgyeje4cc0vl.objOf([
    fFactory,
    fSchema,
    fName,
    fDefaults,
    fOverrides
  ]);
  var optionalSpec = $_lbfbgyeje4cc0vl.objOf([
    fFactory,
    fSchema,
    fName,
    fPname,
    fDefaults,
    fOverrides
  ]);
  var groupSpec = $_lbfbgyeje4cc0vl.objOf([
    fFactory,
    fSchema,
    fName,
    $_c4iqkly7je4cc0ud.strict('unit'),
    fPname,
    fDefaults,
    fOverrides
  ]);
  var asNamedPart = function (part) {
    return part.fold(Option.some, Option.none, Option.some, Option.some);
  };
  var name$1 = function (part) {
    var get = function (data) {
      return data.name();
    };
    return part.fold(get, get, get, get);
  };
  var asCommon = function (part) {
    return part.fold($_aso7c6wjje4cc0om.identity, $_aso7c6wjje4cc0om.identity, $_aso7c6wjje4cc0om.identity, $_aso7c6wjje4cc0om.identity);
  };
  var convert = function (adtConstructor, partSpec) {
    return function (spec) {
      var data = $_lbfbgyeje4cc0vl.asStructOrDie('Converting part type', partSpec, spec);
      return adtConstructor(data);
    };
  };
  var $_chxroy10vje4cc16d = {
    required: convert(adt$2.required, requiredSpec),
    external: convert(adt$2.external, externalSpec),
    optional: convert(adt$2.optional, optionalSpec),
    group: convert(adt$2.group, groupSpec),
    asNamedPart: asNamedPart,
    name: name$1,
    asCommon: asCommon,
    original: $_aso7c6wjje4cc0om.constant('entirety')
  };

  var placeholder = 'placeholder';
  var adt$3 = $_170ozvxwje4cc0st.generate([
    {
      single: [
        'required',
        'valueThunk'
      ]
    },
    {
      multiple: [
        'required',
        'valueThunks'
      ]
    }
  ]);
  var isSubstitute = function (uiType) {
    return $_elh0pqwsje4cc0p4.contains([placeholder], uiType);
  };
  var subPlaceholder = function (owner, detail, compSpec, placeholders) {
    if (owner.exists(function (o) {
        return o !== compSpec.owner;
      }))
      return adt$3.single(true, $_aso7c6wjje4cc0om.constant(compSpec));
    return $_d08hr1xsje4cc0sg.readOptFrom(placeholders, compSpec.name).fold(function () {
      throw new Error('Unknown placeholder component: ' + compSpec.name + '\nKnown: [' + $_i587hx0je4cc0pw.keys(placeholders) + ']\nNamespace: ' + owner.getOr('none') + '\nSpec: ' + $_bsgtdtydje4cc0vk.stringify(compSpec, null, 2));
    }, function (newSpec) {
      return newSpec.replace();
    });
  };
  var scan = function (owner, detail, compSpec, placeholders) {
    if (compSpec.uiType === placeholder)
      return subPlaceholder(owner, detail, compSpec, placeholders);
    else
      return adt$3.single(false, $_aso7c6wjje4cc0om.constant(compSpec));
  };
  var substitute = function (owner, detail, compSpec, placeholders) {
    var base = scan(owner, detail, compSpec, placeholders);
    return base.fold(function (req, valueThunk) {
      var value = valueThunk(detail, compSpec.config, compSpec.validated);
      var childSpecs = $_d08hr1xsje4cc0sg.readOptFrom(value, 'components').getOr([]);
      var substituted = $_elh0pqwsje4cc0p4.bind(childSpecs, function (c) {
        return substitute(owner, detail, c, placeholders);
      });
      return [$_dax12xwyje4cc0pt.deepMerge(value, { components: substituted })];
    }, function (req, valuesThunk) {
      var values = valuesThunk(detail, compSpec.config, compSpec.validated);
      return values;
    });
  };
  var substituteAll = function (owner, detail, components, placeholders) {
    return $_elh0pqwsje4cc0p4.bind(components, function (c) {
      return substitute(owner, detail, c, placeholders);
    });
  };
  var oneReplace = function (label, replacements) {
    var called = false;
    var used = function () {
      return called;
    };
    var replace = function () {
      if (called === true)
        throw new Error('Trying to use the same placeholder more than once: ' + label);
      called = true;
      return replacements;
    };
    var required = function () {
      return replacements.fold(function (req, _) {
        return req;
      }, function (req, _) {
        return req;
      });
    };
    return {
      name: $_aso7c6wjje4cc0om.constant(label),
      required: required,
      used: used,
      replace: replace
    };
  };
  var substitutePlaces = function (owner, detail, components, placeholders) {
    var ps = $_i587hx0je4cc0pw.map(placeholders, function (ph, name) {
      return oneReplace(name, ph);
    });
    var outcome = substituteAll(owner, detail, components, ps);
    $_i587hx0je4cc0pw.each(ps, function (p) {
      if (p.used() === false && p.required()) {
        throw new Error('Placeholder: ' + p.name() + ' was not found in components list\nNamespace: ' + owner.getOr('none') + '\nComponents: ' + $_bsgtdtydje4cc0vk.stringify(detail.components(), null, 2));
      }
    });
    return outcome;
  };
  var singleReplace = function (detail, p) {
    var replacement = p;
    return replacement.fold(function (req, valueThunk) {
      return [valueThunk(detail)];
    }, function (req, valuesThunk) {
      return valuesThunk(detail);
    });
  };
  var $_wr13210wje4cc16q = {
    single: adt$3.single,
    multiple: adt$3.multiple,
    isSubstitute: isSubstitute,
    placeholder: $_aso7c6wjje4cc0om.constant(placeholder),
    substituteAll: substituteAll,
    substitutePlaces: substitutePlaces,
    singleReplace: singleReplace
  };

  var combine = function (detail, data, partSpec, partValidated) {
    var spec = partSpec;
    return $_dax12xwyje4cc0pt.deepMerge(data.defaults()(detail, partSpec, partValidated), partSpec, { uid: detail.partUids()[data.name()] }, data.overrides()(detail, partSpec, partValidated), { 'debug.sketcher': $_d08hr1xsje4cc0sg.wrap('part-' + data.name(), spec) });
  };
  var subs = function (owner, detail, parts) {
    var internals = {};
    var externals = {};
    $_elh0pqwsje4cc0p4.each(parts, function (part) {
      part.fold(function (data) {
        internals[data.pname()] = $_wr13210wje4cc16q.single(true, function (detail, partSpec, partValidated) {
          return data.factory().sketch(combine(detail, data, partSpec, partValidated));
        });
      }, function (data) {
        var partSpec = detail.parts()[data.name()]();
        externals[data.name()] = $_aso7c6wjje4cc0om.constant(combine(detail, data, partSpec[$_chxroy10vje4cc16d.original()]()));
      }, function (data) {
        internals[data.pname()] = $_wr13210wje4cc16q.single(false, function (detail, partSpec, partValidated) {
          return data.factory().sketch(combine(detail, data, partSpec, partValidated));
        });
      }, function (data) {
        internals[data.pname()] = $_wr13210wje4cc16q.multiple(true, function (detail, _partSpec, _partValidated) {
          var units = detail[data.name()]();
          return $_elh0pqwsje4cc0p4.map(units, function (u) {
            return data.factory().sketch($_dax12xwyje4cc0pt.deepMerge(data.defaults()(detail, u), u, data.overrides()(detail, u)));
          });
        });
      });
    });
    return {
      internals: $_aso7c6wjje4cc0om.constant(internals),
      externals: $_aso7c6wjje4cc0om.constant(externals)
    };
  };
  var $_a3tuy010uje4cc167 = { subs: subs };

  var generate$2 = function (owner, parts) {
    var r = {};
    $_elh0pqwsje4cc0p4.each(parts, function (part) {
      $_chxroy10vje4cc16d.asNamedPart(part).each(function (np) {
        var g = doGenerateOne(owner, np.pname());
        r[np.name()] = function (config) {
          var validated = $_lbfbgyeje4cc0vl.asRawOrDie('Part: ' + np.name() + ' in ' + owner, $_lbfbgyeje4cc0vl.objOf(np.schema()), config);
          return $_dax12xwyje4cc0pt.deepMerge(g, {
            config: config,
            validated: validated
          });
        };
      });
    });
    return r;
  };
  var doGenerateOne = function (owner, pname) {
    return {
      uiType: $_wr13210wje4cc16q.placeholder(),
      owner: owner,
      name: pname
    };
  };
  var generateOne = function (owner, pname, config) {
    return {
      uiType: $_wr13210wje4cc16q.placeholder(),
      owner: owner,
      name: pname,
      config: config,
      validated: {}
    };
  };
  var schemas = function (parts) {
    return $_elh0pqwsje4cc0p4.bind(parts, function (part) {
      return part.fold(Option.none, Option.some, Option.none, Option.none).map(function (data) {
        return $_c4iqkly7je4cc0ud.strictObjOf(data.name(), data.schema().concat([$_546z94z6je4cc0y4.snapshot($_chxroy10vje4cc16d.original())]));
      }).toArray();
    });
  };
  var names = function (parts) {
    return $_elh0pqwsje4cc0p4.map(parts, $_chxroy10vje4cc16d.name);
  };
  var substitutes = function (owner, detail, parts) {
    return $_a3tuy010uje4cc167.subs(owner, detail, parts);
  };
  var components = function (owner, detail, internals) {
    return $_wr13210wje4cc16q.substitutePlaces(Option.some(owner), detail, detail.components(), internals);
  };
  var getPart = function (component, detail, partKey) {
    var uid = detail.partUids()[partKey];
    return component.getSystem().getByUid(uid).toOption();
  };
  var getPartOrDie = function (component, detail, partKey) {
    return getPart(component, detail, partKey).getOrDie('Could not find part: ' + partKey);
  };
  var getParts = function (component, detail, partKeys) {
    var r = {};
    var uids = detail.partUids();
    var system = component.getSystem();
    $_elh0pqwsje4cc0p4.each(partKeys, function (pk) {
      r[pk] = system.getByUid(uids[pk]);
    });
    return $_i587hx0je4cc0pw.map(r, $_aso7c6wjje4cc0om.constant);
  };
  var getAllParts = function (component, detail) {
    var system = component.getSystem();
    return $_i587hx0je4cc0pw.map(detail.partUids(), function (pUid, k) {
      return $_aso7c6wjje4cc0om.constant(system.getByUid(pUid));
    });
  };
  var getPartsOrDie = function (component, detail, partKeys) {
    var r = {};
    var uids = detail.partUids();
    var system = component.getSystem();
    $_elh0pqwsje4cc0p4.each(partKeys, function (pk) {
      r[pk] = system.getByUid(uids[pk]).getOrDie();
    });
    return $_i587hx0je4cc0pw.map(r, $_aso7c6wjje4cc0om.constant);
  };
  var defaultUids = function (baseUid, partTypes) {
    var partNames = names(partTypes);
    return $_d08hr1xsje4cc0sg.wrapAll($_elh0pqwsje4cc0p4.map(partNames, function (pn) {
      return {
        key: pn,
        value: baseUid + '-' + pn
      };
    }));
  };
  var defaultUidsSchema = function (partTypes) {
    return $_c4iqkly7je4cc0ud.field('partUids', 'partUids', $_b7tb36y8je4cc0uh.mergeWithThunk(function (spec) {
      return defaultUids(spec.uid, partTypes);
    }), $_lbfbgyeje4cc0vl.anyValue());
  };
  var $_ed9ak010tje4cc15q = {
    generate: generate$2,
    generateOne: generateOne,
    schemas: schemas,
    names: names,
    substitutes: substitutes,
    components: components,
    defaultUids: defaultUids,
    defaultUidsSchema: defaultUidsSchema,
    getAllParts: getAllParts,
    getPart: getPart,
    getPartOrDie: getPartOrDie,
    getParts: getParts,
    getPartsOrDie: getPartsOrDie
  };

  var prefix$1 = 'alloy-id-';
  var idAttr = 'data-alloy-id';
  var $_1zkymr10yje4cc17a = {
    prefix: $_aso7c6wjje4cc0om.constant(prefix$1),
    idAttr: $_aso7c6wjje4cc0om.constant(idAttr)
  };

  var prefix$2 = $_1zkymr10yje4cc17a.prefix();
  var idAttr$1 = $_1zkymr10yje4cc17a.idAttr();
  var write = function (label, elem) {
    var id = $_eb3s8410rje4cc15l.generate(prefix$2 + label);
    $_bjjq6ixrje4cc0s9.set(elem, idAttr$1, id);
    return id;
  };
  var writeOnly = function (elem, uid) {
    $_bjjq6ixrje4cc0s9.set(elem, idAttr$1, uid);
  };
  var read$2 = function (elem) {
    var id = $_7qwxg2xkje4cc0ro.isElement(elem) ? $_bjjq6ixrje4cc0s9.get(elem, idAttr$1) : null;
    return Option.from(id);
  };
  var find$3 = function (container, id) {
    return $_by9cdqzxje4cc11d.descendant(container, id);
  };
  var generate$3 = function (prefix) {
    return $_eb3s8410rje4cc15l.generate(prefix);
  };
  var revoke = function (elem) {
    $_bjjq6ixrje4cc0s9.remove(elem, idAttr$1);
  };
  var $_37lg5b10xje4cc173 = {
    revoke: revoke,
    write: write,
    writeOnly: writeOnly,
    read: read$2,
    find: find$3,
    generate: generate$3,
    attribute: $_aso7c6wjje4cc0om.constant(idAttr$1)
  };

  var getPartsSchema = function (partNames, _optPartNames, _owner) {
    var owner = _owner !== undefined ? _owner : 'Unknown owner';
    var fallbackThunk = function () {
      return [$_546z94z6je4cc0y4.output('partUids', {})];
    };
    var optPartNames = _optPartNames !== undefined ? _optPartNames : fallbackThunk();
    if (partNames.length === 0 && optPartNames.length === 0)
      return fallbackThunk();
    var partsSchema = $_c4iqkly7je4cc0ud.strictObjOf('parts', $_elh0pqwsje4cc0p4.flatten([
      $_elh0pqwsje4cc0p4.map(partNames, $_c4iqkly7je4cc0ud.strict),
      $_elh0pqwsje4cc0p4.map(optPartNames, function (optPart) {
        return $_c4iqkly7je4cc0ud.defaulted(optPart, $_wr13210wje4cc16q.single(false, function () {
          throw new Error('The optional part: ' + optPart + ' was not specified in the config, but it was used in components');
        }));
      })
    ]));
    var partUidsSchema = $_c4iqkly7je4cc0ud.state('partUids', function (spec) {
      if (!$_d08hr1xsje4cc0sg.hasKey(spec, 'parts')) {
        throw new Error('Part uid definition for owner: ' + owner + ' requires "parts"\nExpected parts: ' + partNames.join(', ') + '\nSpec: ' + $_bsgtdtydje4cc0vk.stringify(spec, null, 2));
      }
      var uids = $_i587hx0je4cc0pw.map(spec.parts, function (v, k) {
        return $_d08hr1xsje4cc0sg.readOptFrom(v, 'uid').getOrThunk(function () {
          return spec.uid + '-' + k;
        });
      });
      return uids;
    });
    return [
      partsSchema,
      partUidsSchema
    ];
  };
  var base$1 = function (label, partSchemas, partUidsSchemas, spec) {
    var ps = partSchemas.length > 0 ? [$_c4iqkly7je4cc0ud.strictObjOf('parts', partSchemas)] : [];
    return ps.concat([
      $_c4iqkly7je4cc0ud.strict('uid'),
      $_c4iqkly7je4cc0ud.defaulted('dom', {}),
      $_c4iqkly7je4cc0ud.defaulted('components', []),
      $_546z94z6je4cc0y4.snapshot('originalSpec'),
      $_c4iqkly7je4cc0ud.defaulted('debug.sketcher', {})
    ]).concat(partUidsSchemas);
  };
  var asRawOrDie$1 = function (label, schema, spec, partSchemas, partUidsSchemas) {
    var baseS = base$1(label, partSchemas, spec, partUidsSchemas);
    return $_lbfbgyeje4cc0vl.asRawOrDie(label + ' [SpecSchema]', $_lbfbgyeje4cc0vl.objOfOnly(baseS.concat(schema)), spec);
  };
  var asStructOrDie$1 = function (label, schema, spec, partSchemas, partUidsSchemas) {
    var baseS = base$1(label, partSchemas, partUidsSchemas, spec);
    return $_lbfbgyeje4cc0vl.asStructOrDie(label + ' [SpecSchema]', $_lbfbgyeje4cc0vl.objOfOnly(baseS.concat(schema)), spec);
  };
  var extend = function (builder, original, nu) {
    var newSpec = $_dax12xwyje4cc0pt.deepMerge(original, nu);
    return builder(newSpec);
  };
  var addBehaviours = function (original, behaviours) {
    return $_dax12xwyje4cc0pt.deepMerge(original, behaviours);
  };
  var $_a5fvlc10zje4cc17d = {
    asRawOrDie: asRawOrDie$1,
    asStructOrDie: asStructOrDie$1,
    addBehaviours: addBehaviours,
    getPartsSchema: getPartsSchema,
    extend: extend
  };

  var single = function (owner, schema, factory, spec) {
    var specWithUid = supplyUid(spec);
    var detail = $_a5fvlc10zje4cc17d.asStructOrDie(owner, schema, specWithUid, [], []);
    return $_dax12xwyje4cc0pt.deepMerge(factory(detail, specWithUid), { 'debug.sketcher': $_d08hr1xsje4cc0sg.wrap(owner, spec) });
  };
  var composite = function (owner, schema, partTypes, factory, spec) {
    var specWithUid = supplyUid(spec);
    var partSchemas = $_ed9ak010tje4cc15q.schemas(partTypes);
    var partUidsSchema = $_ed9ak010tje4cc15q.defaultUidsSchema(partTypes);
    var detail = $_a5fvlc10zje4cc17d.asStructOrDie(owner, schema, specWithUid, partSchemas, [partUidsSchema]);
    var subs = $_ed9ak010tje4cc15q.substitutes(owner, detail, partTypes);
    var components = $_ed9ak010tje4cc15q.components(owner, detail, subs.internals());
    return $_dax12xwyje4cc0pt.deepMerge(factory(detail, components, specWithUid, subs.externals()), { 'debug.sketcher': $_d08hr1xsje4cc0sg.wrap(owner, spec) });
  };
  var supplyUid = function (spec) {
    return $_dax12xwyje4cc0pt.deepMerge({ uid: $_37lg5b10xje4cc173.generate('uid') }, spec);
  };
  var $_47q9oj10sje4cc15m = {
    supplyUid: supplyUid,
    single: single,
    composite: composite
  };

  var singleSchema = $_lbfbgyeje4cc0vl.objOfOnly([
    $_c4iqkly7je4cc0ud.strict('name'),
    $_c4iqkly7je4cc0ud.strict('factory'),
    $_c4iqkly7je4cc0ud.strict('configFields'),
    $_c4iqkly7je4cc0ud.defaulted('apis', {}),
    $_c4iqkly7je4cc0ud.defaulted('extraApis', {})
  ]);
  var compositeSchema = $_lbfbgyeje4cc0vl.objOfOnly([
    $_c4iqkly7je4cc0ud.strict('name'),
    $_c4iqkly7je4cc0ud.strict('factory'),
    $_c4iqkly7je4cc0ud.strict('configFields'),
    $_c4iqkly7je4cc0ud.strict('partFields'),
    $_c4iqkly7je4cc0ud.defaulted('apis', {}),
    $_c4iqkly7je4cc0ud.defaulted('extraApis', {})
  ]);
  var single$1 = function (rawConfig) {
    var config = $_lbfbgyeje4cc0vl.asRawOrDie('Sketcher for ' + rawConfig.name, singleSchema, rawConfig);
    var sketch = function (spec) {
      return $_47q9oj10sje4cc15m.single(config.name, config.configFields, config.factory, spec);
    };
    var apis = $_i587hx0je4cc0pw.map(config.apis, $_3c5fj310qje4cc15g.makeApi);
    var extraApis = $_i587hx0je4cc0pw.map(config.extraApis, function (f, k) {
      return $_g16p40ygje4cc0vu.markAsExtraApi(f, k);
    });
    return $_dax12xwyje4cc0pt.deepMerge({
      name: $_aso7c6wjje4cc0om.constant(config.name),
      partFields: $_aso7c6wjje4cc0om.constant([]),
      configFields: $_aso7c6wjje4cc0om.constant(config.configFields),
      sketch: sketch
    }, apis, extraApis);
  };
  var composite$1 = function (rawConfig) {
    var config = $_lbfbgyeje4cc0vl.asRawOrDie('Sketcher for ' + rawConfig.name, compositeSchema, rawConfig);
    var sketch = function (spec) {
      return $_47q9oj10sje4cc15m.composite(config.name, config.configFields, config.partFields, config.factory, spec);
    };
    var parts = $_ed9ak010tje4cc15q.generate(config.name, config.partFields);
    var apis = $_i587hx0je4cc0pw.map(config.apis, $_3c5fj310qje4cc15g.makeApi);
    var extraApis = $_i587hx0je4cc0pw.map(config.extraApis, function (f, k) {
      return $_g16p40ygje4cc0vu.markAsExtraApi(f, k);
    });
    return $_dax12xwyje4cc0pt.deepMerge({
      name: $_aso7c6wjje4cc0om.constant(config.name),
      partFields: $_aso7c6wjje4cc0om.constant(config.partFields),
      configFields: $_aso7c6wjje4cc0om.constant(config.configFields),
      sketch: sketch,
      parts: $_aso7c6wjje4cc0om.constant(parts)
    }, apis, extraApis);
  };
  var $_cx5fhm10pje4cc157 = {
    single: single$1,
    composite: composite$1
  };

  var events$3 = function (optAction) {
    var executeHandler = function (action) {
      return $_ehtdq4y4je4cc0tx.run($_g6tooswhje4cc0of.execute(), function (component, simulatedEvent) {
        action(component);
        simulatedEvent.stop();
      });
    };
    var onClick = function (component, simulatedEvent) {
      simulatedEvent.stop();
      $_3fionhwgje4cc0o9.emitExecute(component);
    };
    var onMousedown = function (component, simulatedEvent) {
      simulatedEvent.cut();
    };
    var pointerEvents = $_2j4x3gwkje4cc0oo.detect().deviceType.isTouch() ? [$_ehtdq4y4je4cc0tx.run($_g6tooswhje4cc0of.tap(), onClick)] : [
      $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.click(), onClick),
      $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.mousedown(), onMousedown)
    ];
    return $_ehtdq4y4je4cc0tx.derive($_elh0pqwsje4cc0p4.flatten([
      optAction.map(executeHandler).toArray(),
      pointerEvents
    ]));
  };
  var $_1tsjqh110je4cc17p = { events: events$3 };

  var factory = function (detail, spec) {
    var events = $_1tsjqh110je4cc17p.events(detail.action());
    var optType = $_d08hr1xsje4cc0sg.readOptFrom(detail.dom(), 'attributes').bind($_d08hr1xsje4cc0sg.readOpt('type'));
    var optTag = $_d08hr1xsje4cc0sg.readOptFrom(detail.dom(), 'tag');
    return {
      uid: detail.uid(),
      dom: detail.dom(),
      components: detail.components(),
      events: events,
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([
        Focusing.config({}),
        Keying.config({
          mode: 'execution',
          useSpace: true,
          useEnter: true
        })
      ]), $_2sk6q710oje4cc152.get(detail.buttonBehaviours())),
      domModification: {
        attributes: $_dax12xwyje4cc0pt.deepMerge(optType.fold(function () {
          return optTag.is('button') ? { type: 'button' } : {};
        }, function (t) {
          return {};
        }), { role: detail.role().getOr('button') })
      },
      eventOrder: detail.eventOrder()
    };
  };
  var Button = $_cx5fhm10pje4cc157.single({
    name: 'Button',
    factory: factory,
    configFields: [
      $_c4iqkly7je4cc0ud.defaulted('uid', undefined),
      $_c4iqkly7je4cc0ud.strict('dom'),
      $_c4iqkly7je4cc0ud.defaulted('components', []),
      $_2sk6q710oje4cc152.field('buttonBehaviours', [
        Focusing,
        Keying
      ]),
      $_c4iqkly7je4cc0ud.option('action'),
      $_c4iqkly7je4cc0ud.option('role'),
      $_c4iqkly7je4cc0ud.defaulted('eventOrder', {})
    ]
  });

  var exhibit$2 = function (base, unselectConfig) {
    return $_5yi74lyhje4cc0vw.nu({
      styles: {
        '-webkit-user-select': 'none',
        'user-select': 'none',
        '-ms-user-select': 'none',
        '-moz-user-select': '-moz-none'
      },
      attributes: { 'unselectable': 'on' }
    });
  };
  var events$4 = function (unselectConfig) {
    return $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.abort($_g7q1k3wije4cc0oi.selectstart(), $_aso7c6wjje4cc0om.constant(true))]);
  };
  var $_7spaw6112je4cc17u = {
    events: events$4,
    exhibit: exhibit$2
  };

  var Unselecting = $_lsmliy2je4cc0te.create({
    fields: [],
    name: 'unselecting',
    active: $_7spaw6112je4cc17u
  });

  var getAttrs = function (elem) {
    var attributes = elem.dom().attributes !== undefined ? elem.dom().attributes : [];
    return $_elh0pqwsje4cc0p4.foldl(attributes, function (b, attr) {
      if (attr.name === 'class')
        return b;
      else
        return $_dax12xwyje4cc0pt.deepMerge(b, $_d08hr1xsje4cc0sg.wrap(attr.name, attr.value));
    }, {});
  };
  var getClasses = function (elem) {
    return Array.prototype.slice.call(elem.dom().classList, 0);
  };
  var fromHtml$2 = function (html) {
    var elem = $_407ejqxfje4cc0rb.fromHtml(html);
    var children = $_1jwy92x3je4cc0qa.children(elem);
    var attrs = getAttrs(elem);
    var classes = getClasses(elem);
    var contents = children.length === 0 ? {} : { innerHtml: $_5scgvxoje4cc0s3.get(elem) };
    return $_dax12xwyje4cc0pt.deepMerge({
      tag: $_7qwxg2xkje4cc0ro.name(elem),
      classes: classes,
      attributes: attrs
    }, contents);
  };
  var sketch = function (sketcher, html, config) {
    return sketcher.sketch($_dax12xwyje4cc0pt.deepMerge({ dom: fromHtml$2(html) }, config));
  };
  var $_fjtki1114je4cc187 = {
    fromHtml: fromHtml$2,
    sketch: sketch
  };

  var dom$1 = function (rawHtml) {
    var html = $_eehsrpwvje4cc0pp.supplant(rawHtml, { prefix: $_gfh4lpzeje4cc0zd.prefix() });
    return $_fjtki1114je4cc187.fromHtml(html);
  };
  var spec = function (rawHtml) {
    var sDom = dom$1(rawHtml);
    return { dom: sDom };
  };
  var $_8147ns113je4cc183 = {
    dom: dom$1,
    spec: spec
  };

  var forToolbarCommand = function (editor, command) {
    return forToolbar(command, function () {
      editor.execCommand(command);
    }, {});
  };
  var getToggleBehaviours = function (command) {
    return $_lsmliy2je4cc0te.derive([
      Toggling.config({
        toggleClass: $_gfh4lpzeje4cc0zd.resolve('toolbar-button-selected'),
        toggleOnExecute: false,
        aria: { mode: 'pressed' }
      }),
      $_g6oxx7zdje4cc0za.format(command, function (button, status) {
        var toggle = status ? Toggling.on : Toggling.off;
        toggle(button);
      })
    ]);
  };
  var forToolbarStateCommand = function (editor, command) {
    var extraBehaviours = getToggleBehaviours(command);
    return forToolbar(command, function () {
      editor.execCommand(command);
    }, extraBehaviours);
  };
  var forToolbarStateAction = function (editor, clazz, command, action) {
    var extraBehaviours = getToggleBehaviours(command);
    return forToolbar(clazz, action, extraBehaviours);
  };
  var forToolbar = function (clazz, action, extraBehaviours) {
    return Button.sketch({
      dom: $_8147ns113je4cc183.dom('<span class="${prefix}-toolbar-button ${prefix}-icon-' + clazz + ' ${prefix}-icon"></span>'),
      action: action,
      buttonBehaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([Unselecting.config({})]), extraBehaviours)
    });
  };
  var $_afg6uzzfje4cc0zf = {
    forToolbar: forToolbar,
    forToolbarCommand: forToolbarCommand,
    forToolbarStateAction: forToolbarStateAction,
    forToolbarStateCommand: forToolbarStateCommand
  };

  var reduceBy = function (value, min, max, step) {
    if (value < min)
      return value;
    else if (value > max)
      return max;
    else if (value === min)
      return min - 1;
    else
      return Math.max(min, value - step);
  };
  var increaseBy = function (value, min, max, step) {
    if (value > max)
      return value;
    else if (value < min)
      return min;
    else if (value === max)
      return max + 1;
    else
      return Math.min(max, value + step);
  };
  var capValue = function (value, min, max) {
    return Math.max(min, Math.min(max, value));
  };
  var snapValueOfX = function (bounds, value, min, max, step, snapStart) {
    return snapStart.fold(function () {
      var initValue = value - min;
      var extraValue = Math.round(initValue / step) * step;
      return capValue(min + extraValue, min - 1, max + 1);
    }, function (start) {
      var remainder = (value - start) % step;
      var adjustment = Math.round(remainder / step);
      var rawSteps = Math.floor((value - start) / step);
      var maxSteps = Math.floor((max - start) / step);
      var numSteps = Math.min(maxSteps, rawSteps + adjustment);
      var r = start + numSteps * step;
      return Math.max(start, r);
    });
  };
  var findValueOfX = function (bounds, min, max, xValue, step, snapToGrid, snapStart) {
    var range = max - min;
    if (xValue < bounds.left)
      return min - 1;
    else if (xValue > bounds.right)
      return max + 1;
    else {
      var xOffset = Math.min(bounds.right, Math.max(xValue, bounds.left)) - bounds.left;
      var newValue = capValue(xOffset / bounds.width * range + min, min - 1, max + 1);
      var roundedValue = Math.round(newValue);
      return snapToGrid && newValue >= min && newValue <= max ? snapValueOfX(bounds, newValue, min, max, step, snapStart) : roundedValue;
    }
  };
  var $_7ahxm7119je4cc193 = {
    reduceBy: reduceBy,
    increaseBy: increaseBy,
    findValueOfX: findValueOfX
  };

  var changeEvent = 'slider.change.value';
  var isTouch = $_2j4x3gwkje4cc0oo.detect().deviceType.isTouch();
  var getEventSource = function (simulatedEvent) {
    var evt = simulatedEvent.event().raw();
    if (isTouch && evt.touches !== undefined && evt.touches.length === 1)
      return Option.some(evt.touches[0]);
    else if (isTouch && evt.touches !== undefined)
      return Option.none();
    else if (!isTouch && evt.clientX !== undefined)
      return Option.some(evt);
    else
      return Option.none();
  };
  var getEventX = function (simulatedEvent) {
    var spot = getEventSource(simulatedEvent);
    return spot.map(function (s) {
      return s.clientX;
    });
  };
  var fireChange = function (component, value) {
    $_3fionhwgje4cc0o9.emitWith(component, changeEvent, { value: value });
  };
  var moveRightFromLedge = function (ledge, detail) {
    fireChange(ledge, detail.min());
  };
  var moveLeftFromRedge = function (redge, detail) {
    fireChange(redge, detail.max());
  };
  var setToRedge = function (redge, detail) {
    fireChange(redge, detail.max() + 1);
  };
  var setToLedge = function (ledge, detail) {
    fireChange(ledge, detail.min() - 1);
  };
  var setToX = function (spectrum, spectrumBounds, detail, xValue) {
    var value = $_7ahxm7119je4cc193.findValueOfX(spectrumBounds, detail.min(), detail.max(), xValue, detail.stepSize(), detail.snapToGrid(), detail.snapStart());
    fireChange(spectrum, value);
  };
  var setXFromEvent = function (spectrum, detail, spectrumBounds, simulatedEvent) {
    return getEventX(simulatedEvent).map(function (xValue) {
      setToX(spectrum, spectrumBounds, detail, xValue);
      return xValue;
    });
  };
  var moveLeft$4 = function (spectrum, detail) {
    var newValue = $_7ahxm7119je4cc193.reduceBy(detail.value().get(), detail.min(), detail.max(), detail.stepSize());
    fireChange(spectrum, newValue);
  };
  var moveRight$4 = function (spectrum, detail) {
    var newValue = $_7ahxm7119je4cc193.increaseBy(detail.value().get(), detail.min(), detail.max(), detail.stepSize());
    fireChange(spectrum, newValue);
  };
  var $_ae8kz4118je4cc18x = {
    setXFromEvent: setXFromEvent,
    setToLedge: setToLedge,
    setToRedge: setToRedge,
    moveLeftFromRedge: moveLeftFromRedge,
    moveRightFromLedge: moveRightFromLedge,
    moveLeft: moveLeft$4,
    moveRight: moveRight$4,
    changeEvent: $_aso7c6wjje4cc0om.constant(changeEvent)
  };

  var platform = $_2j4x3gwkje4cc0oo.detect();
  var isTouch$1 = platform.deviceType.isTouch();
  var edgePart = function (name, action) {
    return $_chxroy10vje4cc16d.optional({
      name: '' + name + '-edge',
      overrides: function (detail) {
        var touchEvents = $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.runActionExtra($_g7q1k3wije4cc0oi.touchstart(), action, [detail])]);
        var mouseEvents = $_ehtdq4y4je4cc0tx.derive([
          $_ehtdq4y4je4cc0tx.runActionExtra($_g7q1k3wije4cc0oi.mousedown(), action, [detail]),
          $_ehtdq4y4je4cc0tx.runActionExtra($_g7q1k3wije4cc0oi.mousemove(), function (l, det) {
            if (det.mouseIsDown().get())
              action(l, det);
          }, [detail])
        ]);
        return { events: isTouch$1 ? touchEvents : mouseEvents };
      }
    });
  };
  var ledgePart = edgePart('left', $_ae8kz4118je4cc18x.setToLedge);
  var redgePart = edgePart('right', $_ae8kz4118je4cc18x.setToRedge);
  var thumbPart = $_chxroy10vje4cc16d.required({
    name: 'thumb',
    defaults: $_aso7c6wjje4cc0om.constant({ dom: { styles: { position: 'absolute' } } }),
    overrides: function (detail) {
      return {
        events: $_ehtdq4y4je4cc0tx.derive([
          $_ehtdq4y4je4cc0tx.redirectToPart($_g7q1k3wije4cc0oi.touchstart(), detail, 'spectrum'),
          $_ehtdq4y4je4cc0tx.redirectToPart($_g7q1k3wije4cc0oi.touchmove(), detail, 'spectrum'),
          $_ehtdq4y4je4cc0tx.redirectToPart($_g7q1k3wije4cc0oi.touchend(), detail, 'spectrum')
        ])
      };
    }
  });
  var spectrumPart = $_chxroy10vje4cc16d.required({
    schema: [$_c4iqkly7je4cc0ud.state('mouseIsDown', function () {
        return Cell(false);
      })],
    name: 'spectrum',
    overrides: function (detail) {
      var moveToX = function (spectrum, simulatedEvent) {
        var spectrumBounds = spectrum.element().dom().getBoundingClientRect();
        $_ae8kz4118je4cc18x.setXFromEvent(spectrum, detail, spectrumBounds, simulatedEvent);
      };
      var touchEvents = $_ehtdq4y4je4cc0tx.derive([
        $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.touchstart(), moveToX),
        $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.touchmove(), moveToX)
      ]);
      var mouseEvents = $_ehtdq4y4je4cc0tx.derive([
        $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.mousedown(), moveToX),
        $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.mousemove(), function (spectrum, se) {
          if (detail.mouseIsDown().get())
            moveToX(spectrum, se);
        })
      ]);
      return {
        behaviours: $_lsmliy2je4cc0te.derive(isTouch$1 ? [] : [
          Keying.config({
            mode: 'special',
            onLeft: function (spectrum) {
              $_ae8kz4118je4cc18x.moveLeft(spectrum, detail);
              return Option.some(true);
            },
            onRight: function (spectrum) {
              $_ae8kz4118je4cc18x.moveRight(spectrum, detail);
              return Option.some(true);
            }
          }),
          Focusing.config({})
        ]),
        events: isTouch$1 ? touchEvents : mouseEvents
      };
    }
  });
  var SliderParts = [
    ledgePart,
    redgePart,
    thumbPart,
    spectrumPart
  ];

  var onLoad$1 = function (component, repConfig, repState) {
    repConfig.store().manager().onLoad(component, repConfig, repState);
  };
  var onUnload = function (component, repConfig, repState) {
    repConfig.store().manager().onUnload(component, repConfig, repState);
  };
  var setValue = function (component, repConfig, repState, data) {
    repConfig.store().manager().setValue(component, repConfig, repState, data);
  };
  var getValue = function (component, repConfig, repState) {
    return repConfig.store().manager().getValue(component, repConfig, repState);
  };
  var $_6fjhe011dje4cc19f = {
    onLoad: onLoad$1,
    onUnload: onUnload,
    setValue: setValue,
    getValue: getValue
  };

  var events$5 = function (repConfig, repState) {
    var es = repConfig.resetOnDom() ? [
      $_ehtdq4y4je4cc0tx.runOnAttached(function (comp, se) {
        $_6fjhe011dje4cc19f.onLoad(comp, repConfig, repState);
      }),
      $_ehtdq4y4je4cc0tx.runOnDetached(function (comp, se) {
        $_6fjhe011dje4cc19f.onUnload(comp, repConfig, repState);
      })
    ] : [$_fc6d0uy3je4cc0tl.loadEvent(repConfig, repState, $_6fjhe011dje4cc19f.onLoad)];
    return $_ehtdq4y4je4cc0tx.derive(es);
  };
  var $_1w723h11cje4cc19d = { events: events$5 };

  var memory = function () {
    var data = Cell(null);
    var readState = function () {
      return {
        mode: 'memory',
        value: data.get()
      };
    };
    var isNotSet = function () {
      return data.get() === null;
    };
    var clear = function () {
      data.set(null);
    };
    return BehaviourState({
      set: data.set,
      get: data.get,
      isNotSet: isNotSet,
      clear: clear,
      readState: readState
    });
  };
  var manual = function () {
    var readState = function () {
    };
    return BehaviourState({ readState: readState });
  };
  var dataset = function () {
    var data = Cell({});
    var readState = function () {
      return {
        mode: 'dataset',
        dataset: data.get()
      };
    };
    return BehaviourState({
      readState: readState,
      set: data.set,
      get: data.get
    });
  };
  var init$2 = function (spec) {
    return spec.store().manager().state(spec);
  };
  var $_3ee6cm11gje4cc19n = {
    memory: memory,
    dataset: dataset,
    manual: manual,
    init: init$2
  };

  var setValue$1 = function (component, repConfig, repState, data) {
    var dataKey = repConfig.store().getDataKey();
    repState.set({});
    repConfig.store().setData()(component, data);
    repConfig.onSetValue()(component, data);
  };
  var getValue$1 = function (component, repConfig, repState) {
    var key = repConfig.store().getDataKey()(component);
    var dataset = repState.get();
    return $_d08hr1xsje4cc0sg.readOptFrom(dataset, key).fold(function () {
      return repConfig.store().getFallbackEntry()(key);
    }, function (data) {
      return data;
    });
  };
  var onLoad$2 = function (component, repConfig, repState) {
    repConfig.store().initialValue().each(function (data) {
      setValue$1(component, repConfig, repState, data);
    });
  };
  var onUnload$1 = function (component, repConfig, repState) {
    repState.set({});
  };
  var DatasetStore = [
    $_c4iqkly7je4cc0ud.option('initialValue'),
    $_c4iqkly7je4cc0ud.strict('getFallbackEntry'),
    $_c4iqkly7je4cc0ud.strict('getDataKey'),
    $_c4iqkly7je4cc0ud.strict('setData'),
    $_546z94z6je4cc0y4.output('manager', {
      setValue: setValue$1,
      getValue: getValue$1,
      onLoad: onLoad$2,
      onUnload: onUnload$1,
      state: $_3ee6cm11gje4cc19n.dataset
    })
  ];

  var getValue$2 = function (component, repConfig, repState) {
    return repConfig.store().getValue()(component);
  };
  var setValue$2 = function (component, repConfig, repState, data) {
    repConfig.store().setValue()(component, data);
    repConfig.onSetValue()(component, data);
  };
  var onLoad$3 = function (component, repConfig, repState) {
    repConfig.store().initialValue().each(function (data) {
      repConfig.store().setValue()(component, data);
    });
  };
  var ManualStore = [
    $_c4iqkly7je4cc0ud.strict('getValue'),
    $_c4iqkly7je4cc0ud.defaulted('setValue', $_aso7c6wjje4cc0om.noop),
    $_c4iqkly7je4cc0ud.option('initialValue'),
    $_546z94z6je4cc0y4.output('manager', {
      setValue: setValue$2,
      getValue: getValue$2,
      onLoad: onLoad$3,
      onUnload: $_aso7c6wjje4cc0om.noop,
      state: $_a5zdlbyjje4cc0w9.init
    })
  ];

  var setValue$3 = function (component, repConfig, repState, data) {
    repState.set(data);
    repConfig.onSetValue()(component, data);
  };
  var getValue$3 = function (component, repConfig, repState) {
    return repState.get();
  };
  var onLoad$4 = function (component, repConfig, repState) {
    repConfig.store().initialValue().each(function (initVal) {
      if (repState.isNotSet())
        repState.set(initVal);
    });
  };
  var onUnload$2 = function (component, repConfig, repState) {
    repState.clear();
  };
  var MemoryStore = [
    $_c4iqkly7je4cc0ud.option('initialValue'),
    $_546z94z6je4cc0y4.output('manager', {
      setValue: setValue$3,
      getValue: getValue$3,
      onLoad: onLoad$4,
      onUnload: onUnload$2,
      state: $_3ee6cm11gje4cc19n.memory
    })
  ];

  var RepresentSchema = [
    $_c4iqkly7je4cc0ud.defaultedOf('store', { mode: 'memory' }, $_lbfbgyeje4cc0vl.choose('mode', {
      memory: MemoryStore,
      manual: ManualStore,
      dataset: DatasetStore
    })),
    $_546z94z6je4cc0y4.onHandler('onSetValue'),
    $_c4iqkly7je4cc0ud.defaulted('resetOnDom', false)
  ];

  var me = $_lsmliy2je4cc0te.create({
    fields: RepresentSchema,
    name: 'representing',
    active: $_1w723h11cje4cc19d,
    apis: $_6fjhe011dje4cc19f,
    extra: {
      setValueFrom: function (component, source) {
        var value = me.getValue(source);
        me.setValue(component, value);
      }
    },
    state: $_3ee6cm11gje4cc19n
  });

  var isTouch$2 = $_2j4x3gwkje4cc0oo.detect().deviceType.isTouch();
  var SliderSchema = [
    $_c4iqkly7je4cc0ud.strict('min'),
    $_c4iqkly7je4cc0ud.strict('max'),
    $_c4iqkly7je4cc0ud.defaulted('stepSize', 1),
    $_c4iqkly7je4cc0ud.defaulted('onChange', $_aso7c6wjje4cc0om.noop),
    $_c4iqkly7je4cc0ud.defaulted('onInit', $_aso7c6wjje4cc0om.noop),
    $_c4iqkly7je4cc0ud.defaulted('onDragStart', $_aso7c6wjje4cc0om.noop),
    $_c4iqkly7je4cc0ud.defaulted('onDragEnd', $_aso7c6wjje4cc0om.noop),
    $_c4iqkly7je4cc0ud.defaulted('snapToGrid', false),
    $_c4iqkly7je4cc0ud.option('snapStart'),
    $_c4iqkly7je4cc0ud.strict('getInitialValue'),
    $_2sk6q710oje4cc152.field('sliderBehaviours', [
      Keying,
      me
    ]),
    $_c4iqkly7je4cc0ud.state('value', function (spec) {
      return Cell(spec.min);
    })
  ].concat(!isTouch$2 ? [$_c4iqkly7je4cc0ud.state('mouseIsDown', function () {
      return Cell(false);
    })] : []);

  var api$1 = Dimension('width', function (element) {
    return element.dom().offsetWidth;
  });
  var set$4 = function (element, h) {
    api$1.set(element, h);
  };
  var get$6 = function (element) {
    return api$1.get(element);
  };
  var getOuter$2 = function (element) {
    return api$1.getOuter(element);
  };
  var setMax$1 = function (element, value) {
    var inclusions = [
      'margin-left',
      'border-left-width',
      'padding-left',
      'padding-right',
      'border-right-width',
      'margin-right'
    ];
    var absMax = api$1.max(element, value, inclusions);
    $_7ojow7103je4cc11s.set(element, 'max-width', absMax + 'px');
  };
  var $_ad4jyp11kje4cc1ai = {
    set: set$4,
    get: get$6,
    getOuter: getOuter$2,
    setMax: setMax$1
  };

  var isTouch$3 = $_2j4x3gwkje4cc0oo.detect().deviceType.isTouch();
  var sketch$1 = function (detail, components, spec, externals) {
    var range = detail.max() - detail.min();
    var getXCentre = function (component) {
      var rect = component.element().dom().getBoundingClientRect();
      return (rect.left + rect.right) / 2;
    };
    var getThumb = function (component) {
      return $_ed9ak010tje4cc15q.getPartOrDie(component, detail, 'thumb');
    };
    var getXOffset = function (slider, spectrumBounds, detail) {
      var v = detail.value().get();
      if (v < detail.min()) {
        return $_ed9ak010tje4cc15q.getPart(slider, detail, 'left-edge').fold(function () {
          return 0;
        }, function (ledge) {
          return getXCentre(ledge) - spectrumBounds.left;
        });
      } else if (v > detail.max()) {
        return $_ed9ak010tje4cc15q.getPart(slider, detail, 'right-edge').fold(function () {
          return spectrumBounds.width;
        }, function (redge) {
          return getXCentre(redge) - spectrumBounds.left;
        });
      } else {
        return (detail.value().get() - detail.min()) / range * spectrumBounds.width;
      }
    };
    var getXPos = function (slider) {
      var spectrum = $_ed9ak010tje4cc15q.getPartOrDie(slider, detail, 'spectrum');
      var spectrumBounds = spectrum.element().dom().getBoundingClientRect();
      var sliderBounds = slider.element().dom().getBoundingClientRect();
      var xOffset = getXOffset(slider, spectrumBounds, detail);
      return spectrumBounds.left - sliderBounds.left + xOffset;
    };
    var refresh = function (component) {
      var pos = getXPos(component);
      var thumb = getThumb(component);
      var thumbRadius = $_ad4jyp11kje4cc1ai.get(thumb.element()) / 2;
      $_7ojow7103je4cc11s.set(thumb.element(), 'left', pos - thumbRadius + 'px');
    };
    var changeValue = function (component, newValue) {
      var oldValue = detail.value().get();
      var thumb = getThumb(component);
      if (oldValue !== newValue || $_7ojow7103je4cc11s.getRaw(thumb.element(), 'left').isNone()) {
        detail.value().set(newValue);
        refresh(component);
        detail.onChange()(component, thumb, newValue);
        return Option.some(true);
      } else {
        return Option.none();
      }
    };
    var resetToMin = function (slider) {
      changeValue(slider, detail.min());
    };
    var resetToMax = function (slider) {
      changeValue(slider, detail.max());
    };
    var uiEventsArr = isTouch$3 ? [
      $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.touchstart(), function (slider, simulatedEvent) {
        detail.onDragStart()(slider, getThumb(slider));
      }),
      $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.touchend(), function (slider, simulatedEvent) {
        detail.onDragEnd()(slider, getThumb(slider));
      })
    ] : [
      $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.mousedown(), function (slider, simulatedEvent) {
        simulatedEvent.stop();
        detail.onDragStart()(slider, getThumb(slider));
        detail.mouseIsDown().set(true);
      }),
      $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.mouseup(), function (slider, simulatedEvent) {
        detail.onDragEnd()(slider, getThumb(slider));
        detail.mouseIsDown().set(false);
      })
    ];
    return {
      uid: detail.uid(),
      dom: detail.dom(),
      components: components,
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive($_elh0pqwsje4cc0p4.flatten([
        !isTouch$3 ? [Keying.config({
            mode: 'special',
            focusIn: function (slider) {
              return $_ed9ak010tje4cc15q.getPart(slider, detail, 'spectrum').map(Keying.focusIn).map($_aso7c6wjje4cc0om.constant(true));
            }
          })] : [],
        [me.config({
            store: {
              mode: 'manual',
              getValue: function (_) {
                return detail.value().get();
              }
            }
          })]
      ])), $_2sk6q710oje4cc152.get(detail.sliderBehaviours())),
      events: $_ehtdq4y4je4cc0tx.derive([
        $_ehtdq4y4je4cc0tx.run($_ae8kz4118je4cc18x.changeEvent(), function (slider, simulatedEvent) {
          changeValue(slider, simulatedEvent.event().value());
        }),
        $_ehtdq4y4je4cc0tx.runOnAttached(function (slider, simulatedEvent) {
          detail.value().set(detail.getInitialValue()());
          var thumb = getThumb(slider);
          refresh(slider);
          detail.onInit()(slider, thumb, detail.value().get());
        })
      ].concat(uiEventsArr)),
      apis: {
        resetToMin: resetToMin,
        resetToMax: resetToMax,
        refresh: refresh
      },
      domModification: { styles: { position: 'relative' } }
    };
  };
  var $_dnh3wm11jje4cc1a4 = { sketch: sketch$1 };

  var Slider = $_cx5fhm10pje4cc157.composite({
    name: 'Slider',
    configFields: SliderSchema,
    partFields: SliderParts,
    factory: $_dnh3wm11jje4cc1a4.sketch,
    apis: {
      resetToMin: function (apis, slider) {
        apis.resetToMin(slider);
      },
      resetToMax: function (apis, slider) {
        apis.resetToMax(slider);
      },
      refresh: function (apis, slider) {
        apis.refresh(slider);
      }
    }
  });

  var button = function (realm, clazz, makeItems) {
    return $_afg6uzzfje4cc0zf.forToolbar(clazz, function () {
      var items = makeItems();
      realm.setContextToolbar([{
          label: clazz + ' group',
          items: items
        }]);
    }, {});
  };
  var $_ewecuq11lje4cc1ak = { button: button };

  var BLACK = -1;
  var makeSlider = function (spec) {
    var getColor = function (hue) {
      if (hue < 0) {
        return 'black';
      } else if (hue > 360) {
        return 'white';
      } else {
        return 'hsl(' + hue + ', 100%, 50%)';
      }
    };
    var onInit = function (slider, thumb, value) {
      var color = getColor(value);
      $_7ojow7103je4cc11s.set(thumb.element(), 'background-color', color);
    };
    var onChange = function (slider, thumb, value) {
      var color = getColor(value);
      $_7ojow7103je4cc11s.set(thumb.element(), 'background-color', color);
      spec.onChange(slider, thumb, color);
    };
    return Slider.sketch({
      dom: $_8147ns113je4cc183.dom('<div class="${prefix}-slider ${prefix}-hue-slider-container"></div>'),
      components: [
        Slider.parts()['left-edge']($_8147ns113je4cc183.spec('<div class="${prefix}-hue-slider-black"></div>')),
        Slider.parts().spectrum({
          dom: $_8147ns113je4cc183.dom('<div class="${prefix}-slider-gradient-container"></div>'),
          components: [$_8147ns113je4cc183.spec('<div class="${prefix}-slider-gradient"></div>')],
          behaviours: $_lsmliy2je4cc0te.derive([Toggling.config({ toggleClass: $_gfh4lpzeje4cc0zd.resolve('thumb-active') })])
        }),
        Slider.parts()['right-edge']($_8147ns113je4cc183.spec('<div class="${prefix}-hue-slider-white"></div>')),
        Slider.parts().thumb({
          dom: $_8147ns113je4cc183.dom('<div class="${prefix}-slider-thumb"></div>'),
          behaviours: $_lsmliy2je4cc0te.derive([Toggling.config({ toggleClass: $_gfh4lpzeje4cc0zd.resolve('thumb-active') })])
        })
      ],
      onChange: onChange,
      onDragStart: function (slider, thumb) {
        Toggling.on(thumb);
      },
      onDragEnd: function (slider, thumb) {
        Toggling.off(thumb);
      },
      onInit: onInit,
      stepSize: 10,
      min: 0,
      max: 360,
      getInitialValue: spec.getInitialValue,
      sliderBehaviours: $_lsmliy2je4cc0te.derive([$_g6oxx7zdje4cc0za.orientation(Slider.refresh)])
    });
  };
  var makeItems = function (spec) {
    return [makeSlider(spec)];
  };
  var sketch$2 = function (realm, editor) {
    var spec = {
      onChange: function (slider, thumb, color) {
        editor.undoManager.transact(function () {
          editor.formatter.apply('forecolor', { value: color });
          editor.nodeChanged();
        });
      },
      getInitialValue: function () {
        return BLACK;
      }
    };
    return $_ewecuq11lje4cc1ak.button(realm, 'color', function () {
      return makeItems(spec);
    });
  };
  var $_bj7zmp115je4cc18h = {
    makeItems: makeItems,
    sketch: sketch$2
  };

  var schema$7 = $_lbfbgyeje4cc0vl.objOfOnly([
    $_c4iqkly7je4cc0ud.strict('getInitialValue'),
    $_c4iqkly7je4cc0ud.strict('onChange'),
    $_c4iqkly7je4cc0ud.strict('category'),
    $_c4iqkly7je4cc0ud.strict('sizes')
  ]);
  var sketch$3 = function (rawSpec) {
    var spec = $_lbfbgyeje4cc0vl.asRawOrDie('SizeSlider', schema$7, rawSpec);
    var isValidValue = function (valueIndex) {
      return valueIndex >= 0 && valueIndex < spec.sizes.length;
    };
    var onChange = function (slider, thumb, valueIndex) {
      if (isValidValue(valueIndex)) {
        spec.onChange(valueIndex);
      }
    };
    return Slider.sketch({
      dom: {
        tag: 'div',
        classes: [
          $_gfh4lpzeje4cc0zd.resolve('slider-' + spec.category + '-size-container'),
          $_gfh4lpzeje4cc0zd.resolve('slider'),
          $_gfh4lpzeje4cc0zd.resolve('slider-size-container')
        ]
      },
      onChange: onChange,
      onDragStart: function (slider, thumb) {
        Toggling.on(thumb);
      },
      onDragEnd: function (slider, thumb) {
        Toggling.off(thumb);
      },
      min: 0,
      max: spec.sizes.length - 1,
      stepSize: 1,
      getInitialValue: spec.getInitialValue,
      snapToGrid: true,
      sliderBehaviours: $_lsmliy2je4cc0te.derive([$_g6oxx7zdje4cc0za.orientation(Slider.refresh)]),
      components: [
        Slider.parts().spectrum({
          dom: $_8147ns113je4cc183.dom('<div class="${prefix}-slider-size-container"></div>'),
          components: [$_8147ns113je4cc183.spec('<div class="${prefix}-slider-size-line"></div>')]
        }),
        Slider.parts().thumb({
          dom: $_8147ns113je4cc183.dom('<div class="${prefix}-slider-thumb"></div>'),
          behaviours: $_lsmliy2je4cc0te.derive([Toggling.config({ toggleClass: $_gfh4lpzeje4cc0zd.resolve('thumb-active') })])
        })
      ]
    });
  };
  var $_1ybtq611nje4cc1am = { sketch: sketch$3 };

  var ancestor$3 = function (scope, transform, isRoot) {
    var element = scope.dom();
    var stop = $_eh3yfywzje4cc0pu.isFunction(isRoot) ? isRoot : $_aso7c6wjje4cc0om.constant(false);
    while (element.parentNode) {
      element = element.parentNode;
      var el = $_407ejqxfje4cc0rb.fromDom(element);
      var transformed = transform(el);
      if (transformed.isSome())
        return transformed;
      else if (stop(el))
        break;
    }
    return Option.none();
  };
  var closest$3 = function (scope, transform, isRoot) {
    var current = transform(scope);
    return current.orThunk(function () {
      return isRoot(scope) ? Option.none() : ancestor$3(scope, transform, isRoot);
    });
  };
  var $_7qa8xn11pje4cc1b2 = {
    ancestor: ancestor$3,
    closest: closest$3
  };

  var candidates = [
    '9px',
    '10px',
    '11px',
    '12px',
    '14px',
    '16px',
    '18px',
    '20px',
    '24px',
    '32px',
    '36px'
  ];
  var defaultSize = 'medium';
  var defaultIndex = 2;
  var indexToSize = function (index) {
    return Option.from(candidates[index]);
  };
  var sizeToIndex = function (size) {
    return $_elh0pqwsje4cc0p4.findIndex(candidates, function (v) {
      return v === size;
    });
  };
  var getRawOrComputed = function (isRoot, rawStart) {
    var optStart = $_7qwxg2xkje4cc0ro.isElement(rawStart) ? Option.some(rawStart) : $_1jwy92x3je4cc0qa.parent(rawStart);
    return optStart.map(function (start) {
      var inline = $_7qa8xn11pje4cc1b2.closest(start, function (elem) {
        return $_7ojow7103je4cc11s.getRaw(elem, 'font-size');
      }, isRoot);
      return inline.getOrThunk(function () {
        return $_7ojow7103je4cc11s.get(start, 'font-size');
      });
    }).getOr('');
  };
  var getSize = function (editor) {
    var node = editor.selection.getStart();
    var elem = $_407ejqxfje4cc0rb.fromDom(node);
    var root = $_407ejqxfje4cc0rb.fromDom(editor.getBody());
    var isRoot = function (e) {
      return $_1stme4x9je4cc0qq.eq(root, e);
    };
    var elemSize = getRawOrComputed(isRoot, elem);
    return $_elh0pqwsje4cc0p4.find(candidates, function (size) {
      return elemSize === size;
    }).getOr(defaultSize);
  };
  var applySize = function (editor, value) {
    var currentValue = getSize(editor);
    if (currentValue !== value) {
      editor.execCommand('fontSize', false, value);
    }
  };
  var get$7 = function (editor) {
    var size = getSize(editor);
    return sizeToIndex(size).getOr(defaultIndex);
  };
  var apply$1 = function (editor, index) {
    indexToSize(index).each(function (size) {
      applySize(editor, size);
    });
  };
  var $_7xevga11oje4cc1at = {
    candidates: $_aso7c6wjje4cc0om.constant(candidates),
    get: get$7,
    apply: apply$1
  };

  var sizes = $_7xevga11oje4cc1at.candidates();
  var makeSlider$1 = function (spec) {
    return $_1ybtq611nje4cc1am.sketch({
      onChange: spec.onChange,
      sizes: sizes,
      category: 'font',
      getInitialValue: spec.getInitialValue
    });
  };
  var makeItems$1 = function (spec) {
    return [
      $_8147ns113je4cc183.spec('<span class="${prefix}-toolbar-button ${prefix}-icon-small-font ${prefix}-icon"></span>'),
      makeSlider$1(spec),
      $_8147ns113je4cc183.spec('<span class="${prefix}-toolbar-button ${prefix}-icon-large-font ${prefix}-icon"></span>')
    ];
  };
  var sketch$4 = function (realm, editor) {
    var spec = {
      onChange: function (value) {
        $_7xevga11oje4cc1at.apply(editor, value);
      },
      getInitialValue: function () {
        return $_7xevga11oje4cc1at.get(editor);
      }
    };
    return $_ewecuq11lje4cc1ak.button(realm, 'font-size', function () {
      return makeItems$1(spec);
    });
  };
  var $_6y1sf211mje4cc1al = {
    makeItems: makeItems$1,
    sketch: sketch$4
  };

  var record = function (spec) {
    var uid = $_d08hr1xsje4cc0sg.hasKey(spec, 'uid') ? spec.uid : $_37lg5b10xje4cc173.generate('memento');
    var get = function (any) {
      return any.getSystem().getByUid(uid).getOrDie();
    };
    var getOpt = function (any) {
      return any.getSystem().getByUid(uid).fold(Option.none, Option.some);
    };
    var asSpec = function () {
      return $_dax12xwyje4cc0pt.deepMerge(spec, { uid: uid });
    };
    return {
      get: get,
      getOpt: getOpt,
      asSpec: asSpec
    };
  };
  var $_ceck8i11rje4cc1bf = { record: record };

  function create$3(width, height) {
    return resize(document.createElement('canvas'), width, height);
  }
  function clone$2(canvas) {
    var tCanvas, ctx;
    tCanvas = create$3(canvas.width, canvas.height);
    ctx = get2dContext(tCanvas);
    ctx.drawImage(canvas, 0, 0);
    return tCanvas;
  }
  function get2dContext(canvas) {
    return canvas.getContext('2d');
  }
  function get3dContext(canvas) {
    var gl = null;
    try {
      gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
    } catch (e) {
    }
    if (!gl) {
      gl = null;
    }
    return gl;
  }
  function resize(canvas, width, height) {
    canvas.width = width;
    canvas.height = height;
    return canvas;
  }
  var $_8h37nb11uje4cc1c3 = {
    create: create$3,
    clone: clone$2,
    resize: resize,
    get2dContext: get2dContext,
    get3dContext: get3dContext
  };

  function getWidth(image) {
    return image.naturalWidth || image.width;
  }
  function getHeight(image) {
    return image.naturalHeight || image.height;
  }
  var $_aun8cy11vje4cc1c5 = {
    getWidth: getWidth,
    getHeight: getHeight
  };

  var promise = function () {
    var Promise = function (fn) {
      if (typeof this !== 'object')
        throw new TypeError('Promises must be constructed via new');
      if (typeof fn !== 'function')
        throw new TypeError('not a function');
      this._state = null;
      this._value = null;
      this._deferreds = [];
      doResolve(fn, bind(resolve, this), bind(reject, this));
    };
    var asap = Promise.immediateFn || typeof setImmediate === 'function' && setImmediate || function (fn) {
      setTimeout(fn, 1);
    };
    function bind(fn, thisArg) {
      return function () {
        fn.apply(thisArg, arguments);
      };
    }
    var isArray = Array.isArray || function (value) {
      return Object.prototype.toString.call(value) === '[object Array]';
    };
    function handle(deferred) {
      var me = this;
      if (this._state === null) {
        this._deferreds.push(deferred);
        return;
      }
      asap(function () {
        var cb = me._state ? deferred.onFulfilled : deferred.onRejected;
        if (cb === null) {
          (me._state ? deferred.resolve : deferred.reject)(me._value);
          return;
        }
        var ret;
        try {
          ret = cb(me._value);
        } catch (e) {
          deferred.reject(e);
          return;
        }
        deferred.resolve(ret);
      });
    }
    function resolve(newValue) {
      try {
        if (newValue === this)
          throw new TypeError('A promise cannot be resolved with itself.');
        if (newValue && (typeof newValue === 'object' || typeof newValue === 'function')) {
          var then = newValue.then;
          if (typeof then === 'function') {
            doResolve(bind(then, newValue), bind(resolve, this), bind(reject, this));
            return;
          }
        }
        this._state = true;
        this._value = newValue;
        finale.call(this);
      } catch (e) {
        reject.call(this, e);
      }
    }
    function reject(newValue) {
      this._state = false;
      this._value = newValue;
      finale.call(this);
    }
    function finale() {
      for (var i = 0, len = this._deferreds.length; i < len; i++) {
        handle.call(this, this._deferreds[i]);
      }
      this._deferreds = null;
    }
    function Handler(onFulfilled, onRejected, resolve, reject) {
      this.onFulfilled = typeof onFulfilled === 'function' ? onFulfilled : null;
      this.onRejected = typeof onRejected === 'function' ? onRejected : null;
      this.resolve = resolve;
      this.reject = reject;
    }
    function doResolve(fn, onFulfilled, onRejected) {
      var done = false;
      try {
        fn(function (value) {
          if (done)
            return;
          done = true;
          onFulfilled(value);
        }, function (reason) {
          if (done)
            return;
          done = true;
          onRejected(reason);
        });
      } catch (ex) {
        if (done)
          return;
        done = true;
        onRejected(ex);
      }
    }
    Promise.prototype['catch'] = function (onRejected) {
      return this.then(null, onRejected);
    };
    Promise.prototype.then = function (onFulfilled, onRejected) {
      var me = this;
      return new Promise(function (resolve, reject) {
        handle.call(me, new Handler(onFulfilled, onRejected, resolve, reject));
      });
    };
    Promise.all = function () {
      var args = Array.prototype.slice.call(arguments.length === 1 && isArray(arguments[0]) ? arguments[0] : arguments);
      return new Promise(function (resolve, reject) {
        if (args.length === 0)
          return resolve([]);
        var remaining = args.length;
        function res(i, val) {
          try {
            if (val && (typeof val === 'object' || typeof val === 'function')) {
              var then = val.then;
              if (typeof then === 'function') {
                then.call(val, function (val) {
                  res(i, val);
                }, reject);
                return;
              }
            }
            args[i] = val;
            if (--remaining === 0) {
              resolve(args);
            }
          } catch (ex) {
            reject(ex);
          }
        }
        for (var i = 0; i < args.length; i++) {
          res(i, args[i]);
        }
      });
    };
    Promise.resolve = function (value) {
      if (value && typeof value === 'object' && value.constructor === Promise) {
        return value;
      }
      return new Promise(function (resolve) {
        resolve(value);
      });
    };
    Promise.reject = function (value) {
      return new Promise(function (resolve, reject) {
        reject(value);
      });
    };
    Promise.race = function (values) {
      return new Promise(function (resolve, reject) {
        for (var i = 0, len = values.length; i < len; i++) {
          values[i].then(resolve, reject);
        }
      });
    };
    return Promise;
  };
  var Promise = window.Promise ? window.Promise : promise();

  function Blob (parts, properties) {
    var f = $_6a52kwxbje4cc0qw.getOrDie('Blob');
    return new f(parts, properties);
  }

  function FileReader () {
    var f = $_6a52kwxbje4cc0qw.getOrDie('FileReader');
    return new f();
  }

  function Uint8Array (arr) {
    var f = $_6a52kwxbje4cc0qw.getOrDie('Uint8Array');
    return new f(arr);
  }

  var requestAnimationFrame = function (callback) {
    var f = $_6a52kwxbje4cc0qw.getOrDie('requestAnimationFrame');
    f(callback);
  };
  var atob = function (base64) {
    var f = $_6a52kwxbje4cc0qw.getOrDie('atob');
    return f(base64);
  };
  var $_g2gdm5120je4cc1cd = {
    atob: atob,
    requestAnimationFrame: requestAnimationFrame
  };

  function loadImage(image) {
    return new Promise(function (resolve) {
      function loaded() {
        image.removeEventListener('load', loaded);
        resolve(image);
      }
      if (image.complete) {
        resolve(image);
      } else {
        image.addEventListener('load', loaded);
      }
    });
  }
  function imageToBlob(image) {
    return loadImage(image).then(function (image) {
      var src = image.src;
      if (src.indexOf('blob:') === 0) {
        return anyUriToBlob(src);
      }
      if (src.indexOf('data:') === 0) {
        return dataUriToBlob(src);
      }
      return anyUriToBlob(src);
    });
  }
  function blobToImage(blob) {
    return new Promise(function (resolve, reject) {
      var blobUrl = URL.createObjectURL(blob);
      var image = new Image();
      var removeListeners = function () {
        image.removeEventListener('load', loaded);
        image.removeEventListener('error', error);
      };
      function loaded() {
        removeListeners();
        resolve(image);
      }
      function error() {
        removeListeners();
        reject('Unable to load data of type ' + blob.type + ': ' + blobUrl);
      }
      image.addEventListener('load', loaded);
      image.addEventListener('error', error);
      image.src = blobUrl;
      if (image.complete) {
        loaded();
      }
    });
  }
  function anyUriToBlob(url) {
    return new Promise(function (resolve, reject) {
      var xhr = new XMLHttpRequest();
      xhr.open('GET', url, true);
      xhr.responseType = 'blob';
      xhr.onload = function () {
        if (this.status == 200) {
          resolve(this.response);
        }
      };
      xhr.onerror = function () {
        var _this = this;
        var corsError = function () {
          var obj = new Error('No access to download image');
          obj.code = 18;
          obj.name = 'SecurityError';
          return obj;
        };
        var genericError = function () {
          return new Error('Error ' + _this.status + ' downloading image');
        };
        reject(this.status === 0 ? corsError() : genericError());
      };
      xhr.send();
    });
  }
  function dataUriToBlobSync(uri) {
    var data = uri.split(',');
    var matches = /data:([^;]+)/.exec(data[0]);
    if (!matches)
      return Option.none();
    var mimetype = matches[1];
    var base64 = data[1];
    var sliceSize = 1024;
    var byteCharacters = $_g2gdm5120je4cc1cd.atob(base64);
    var bytesLength = byteCharacters.length;
    var slicesCount = Math.ceil(bytesLength / sliceSize);
    var byteArrays = new Array(slicesCount);
    for (var sliceIndex = 0; sliceIndex < slicesCount; ++sliceIndex) {
      var begin = sliceIndex * sliceSize;
      var end = Math.min(begin + sliceSize, bytesLength);
      var bytes = new Array(end - begin);
      for (var offset = begin, i = 0; offset < end; ++i, ++offset) {
        bytes[i] = byteCharacters[offset].charCodeAt(0);
      }
      byteArrays[sliceIndex] = Uint8Array(bytes);
    }
    return Option.some(Blob(byteArrays, { type: mimetype }));
  }
  function dataUriToBlob(uri) {
    return new Promise(function (resolve, reject) {
      dataUriToBlobSync(uri).fold(function () {
        reject('uri is not base64: ' + uri);
      }, resolve);
    });
  }
  function uriToBlob(url) {
    if (url.indexOf('blob:') === 0) {
      return anyUriToBlob(url);
    }
    if (url.indexOf('data:') === 0) {
      return dataUriToBlob(url);
    }
    return null;
  }
  function canvasToBlob(canvas, type, quality) {
    type = type || 'image/png';
    if (HTMLCanvasElement.prototype.toBlob) {
      return new Promise(function (resolve) {
        canvas.toBlob(function (blob) {
          resolve(blob);
        }, type, quality);
      });
    } else {
      return dataUriToBlob(canvas.toDataURL(type, quality));
    }
  }
  function canvasToDataURL(getCanvas, type, quality) {
    type = type || 'image/png';
    return getCanvas.then(function (canvas) {
      return canvas.toDataURL(type, quality);
    });
  }
  function blobToCanvas(blob) {
    return blobToImage(blob).then(function (image) {
      revokeImageUrl(image);
      var context, canvas;
      canvas = $_8h37nb11uje4cc1c3.create($_aun8cy11vje4cc1c5.getWidth(image), $_aun8cy11vje4cc1c5.getHeight(image));
      context = $_8h37nb11uje4cc1c3.get2dContext(canvas);
      context.drawImage(image, 0, 0);
      return canvas;
    });
  }
  function blobToDataUri(blob) {
    return new Promise(function (resolve) {
      var reader = new FileReader();
      reader.onloadend = function () {
        resolve(reader.result);
      };
      reader.readAsDataURL(blob);
    });
  }
  function blobToArrayBuffer(blob) {
    return new Promise(function (resolve) {
      var reader = new FileReader();
      reader.onloadend = function () {
        resolve(reader.result);
      };
      reader.readAsArrayBuffer(blob);
    });
  }
  function blobToBase64(blob) {
    return blobToDataUri(blob).then(function (dataUri) {
      return dataUri.split(',')[1];
    });
  }
  function revokeImageUrl(image) {
    URL.revokeObjectURL(image.src);
  }
  var $_2t1f4711tje4cc1bt = {
    blobToImage: blobToImage,
    imageToBlob: imageToBlob,
    blobToArrayBuffer: blobToArrayBuffer,
    blobToDataUri: blobToDataUri,
    blobToBase64: blobToBase64,
    dataUriToBlobSync: dataUriToBlobSync,
    canvasToBlob: canvasToBlob,
    canvasToDataURL: canvasToDataURL,
    blobToCanvas: blobToCanvas,
    uriToBlob: uriToBlob
  };

  var blobToImage$1 = function (image) {
    return $_2t1f4711tje4cc1bt.blobToImage(image);
  };
  var imageToBlob$1 = function (blob) {
    return $_2t1f4711tje4cc1bt.imageToBlob(blob);
  };
  var blobToDataUri$1 = function (blob) {
    return $_2t1f4711tje4cc1bt.blobToDataUri(blob);
  };
  var blobToBase64$1 = function (blob) {
    return $_2t1f4711tje4cc1bt.blobToBase64(blob);
  };
  var dataUriToBlobSync$1 = function (uri) {
    return $_2t1f4711tje4cc1bt.dataUriToBlobSync(uri);
  };
  var uriToBlob$1 = function (uri) {
    return Option.from($_2t1f4711tje4cc1bt.uriToBlob(uri));
  };
  var $_7d9m0y11sje4cc1bk = {
    blobToImage: blobToImage$1,
    imageToBlob: imageToBlob$1,
    blobToDataUri: blobToDataUri$1,
    blobToBase64: blobToBase64$1,
    dataUriToBlobSync: dataUriToBlobSync$1,
    uriToBlob: uriToBlob$1
  };

  var addImage = function (editor, blob) {
    $_7d9m0y11sje4cc1bk.blobToBase64(blob).then(function (base64) {
      editor.undoManager.transact(function () {
        var cache = editor.editorUpload.blobCache;
        var info = cache.create($_eb3s8410rje4cc15l.generate('mceu'), blob, base64);
        cache.add(info);
        var img = editor.dom.createHTML('img', { src: info.blobUri() });
        editor.insertContent(img);
      });
    });
  };
  var extractBlob = function (simulatedEvent) {
    var event = simulatedEvent.event();
    var files = event.raw().target.files || event.raw().dataTransfer.files;
    return Option.from(files[0]);
  };
  var sketch$5 = function (editor) {
    var pickerDom = {
      tag: 'input',
      attributes: {
        accept: 'image/*',
        type: 'file',
        title: ''
      },
      styles: {
        visibility: 'hidden',
        position: 'absolute'
      }
    };
    var memPicker = $_ceck8i11rje4cc1bf.record({
      dom: pickerDom,
      events: $_ehtdq4y4je4cc0tx.derive([
        $_ehtdq4y4je4cc0tx.cutter($_g7q1k3wije4cc0oi.click()),
        $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.change(), function (picker, simulatedEvent) {
          extractBlob(simulatedEvent).each(function (blob) {
            addImage(editor, blob);
          });
        })
      ])
    });
    return Button.sketch({
      dom: $_8147ns113je4cc183.dom('<span class="${prefix}-toolbar-button ${prefix}-icon-image ${prefix}-icon"></span>'),
      components: [memPicker.asSpec()],
      action: function (button) {
        var picker = memPicker.get(button);
        picker.element().dom().click();
      }
    });
  };
  var $_fqw8kp11qje4cc1b7 = { sketch: sketch$5 };

  var get$8 = function (element) {
    return element.dom().textContent;
  };
  var set$5 = function (element, value) {
    element.dom().textContent = value;
  };
  var $_6fzzqg123je4cc1cq = {
    get: get$8,
    set: set$5
  };

  var isNotEmpty = function (val) {
    return val.length > 0;
  };
  var defaultToEmpty = function (str) {
    return str === undefined || str === null ? '' : str;
  };
  var noLink = function (editor) {
    var text = editor.selection.getContent({ format: 'text' });
    return {
      url: '',
      text: text,
      title: '',
      target: '',
      link: Option.none()
    };
  };
  var fromLink = function (link) {
    var text = $_6fzzqg123je4cc1cq.get(link);
    var url = $_bjjq6ixrje4cc0s9.get(link, 'href');
    var title = $_bjjq6ixrje4cc0s9.get(link, 'title');
    var target = $_bjjq6ixrje4cc0s9.get(link, 'target');
    return {
      url: defaultToEmpty(url),
      text: text !== url ? defaultToEmpty(text) : '',
      title: defaultToEmpty(title),
      target: defaultToEmpty(target),
      link: Option.some(link)
    };
  };
  var getInfo = function (editor) {
    return query(editor).fold(function () {
      return noLink(editor);
    }, function (link) {
      return fromLink(link);
    });
  };
  var wasSimple = function (link) {
    var prevHref = $_bjjq6ixrje4cc0s9.get(link, 'href');
    var prevText = $_6fzzqg123je4cc1cq.get(link);
    return prevHref === prevText;
  };
  var getTextToApply = function (link, url, info) {
    return info.text.filter(isNotEmpty).fold(function () {
      return wasSimple(link) ? Option.some(url) : Option.none();
    }, Option.some);
  };
  var unlinkIfRequired = function (editor, info) {
    var activeLink = info.link.bind($_aso7c6wjje4cc0om.identity);
    activeLink.each(function (link) {
      editor.execCommand('unlink');
    });
  };
  var getAttrs$1 = function (url, info) {
    var attrs = {};
    attrs.href = url;
    info.title.filter(isNotEmpty).each(function (title) {
      attrs.title = title;
    });
    info.target.filter(isNotEmpty).each(function (target) {
      attrs.target = target;
    });
    return attrs;
  };
  var applyInfo = function (editor, info) {
    info.url.filter(isNotEmpty).fold(function () {
      unlinkIfRequired(editor, info);
    }, function (url) {
      var attrs = getAttrs$1(url, info);
      var activeLink = info.link.bind($_aso7c6wjje4cc0om.identity);
      activeLink.fold(function () {
        var text = info.text.filter(isNotEmpty).getOr(url);
        editor.insertContent(editor.dom.createHTML('a', attrs, editor.dom.encode(text)));
      }, function (link) {
        var text = getTextToApply(link, url, info);
        $_bjjq6ixrje4cc0s9.setAll(link, attrs);
        text.each(function (newText) {
          $_6fzzqg123je4cc1cq.set(link, newText);
        });
      });
    });
  };
  var query = function (editor) {
    var start = $_407ejqxfje4cc0rb.fromDom(editor.selection.getStart());
    return $_by9cdqzxje4cc11d.closest(start, 'a');
  };
  var $_6zjnes122je4cc1cj = {
    getInfo: getInfo,
    applyInfo: applyInfo,
    query: query
  };

  var platform$1 = $_2j4x3gwkje4cc0oo.detect();
  var preserve$1 = function (f, editor) {
    var rng = editor.selection.getRng();
    f();
    editor.selection.setRng(rng);
  };
  var forAndroid = function (editor, f) {
    var wrapper = platform$1.os.isAndroid() ? preserve$1 : $_aso7c6wjje4cc0om.apply;
    wrapper(f, editor);
  };
  var $_7hikxr124je4cc1cr = { forAndroid: forAndroid };

  var events$6 = function (name, eventHandlers) {
    var events = $_ehtdq4y4je4cc0tx.derive(eventHandlers);
    return $_lsmliy2je4cc0te.create({
      fields: [$_c4iqkly7je4cc0ud.strict('enabled')],
      name: name,
      active: { events: $_aso7c6wjje4cc0om.constant(events) }
    });
  };
  var config = function (name, eventHandlers) {
    var me = events$6(name, eventHandlers);
    return {
      key: name,
      value: {
        config: {},
        me: me,
        configAsRaw: $_aso7c6wjje4cc0om.constant({}),
        initialConfig: {},
        state: $_lsmliy2je4cc0te.noState()
      }
    };
  };
  var $_4dgb1i126je4cc1d8 = {
    events: events$6,
    config: config
  };

  var getCurrent = function (component, composeConfig, composeState) {
    return composeConfig.find()(component);
  };
  var $_cwm373128je4cc1dc = { getCurrent: getCurrent };

  var ComposeSchema = [$_c4iqkly7je4cc0ud.strict('find')];

  var Composing = $_lsmliy2je4cc0te.create({
    fields: ComposeSchema,
    name: 'composing',
    apis: $_cwm373128je4cc1dc
  });

  var factory$1 = function (detail, spec) {
    return {
      uid: detail.uid(),
      dom: $_dax12xwyje4cc0pt.deepMerge({
        tag: 'div',
        attributes: { role: 'presentation' }
      }, detail.dom()),
      components: detail.components(),
      behaviours: $_2sk6q710oje4cc152.get(detail.containerBehaviours()),
      events: detail.events(),
      domModification: detail.domModification(),
      eventOrder: detail.eventOrder()
    };
  };
  var Container = $_cx5fhm10pje4cc157.single({
    name: 'Container',
    factory: factory$1,
    configFields: [
      $_c4iqkly7je4cc0ud.defaulted('components', []),
      $_2sk6q710oje4cc152.field('containerBehaviours', []),
      $_c4iqkly7je4cc0ud.defaulted('events', {}),
      $_c4iqkly7je4cc0ud.defaulted('domModification', {}),
      $_c4iqkly7je4cc0ud.defaulted('eventOrder', {})
    ]
  });

  var factory$2 = function (detail, spec) {
    return {
      uid: detail.uid(),
      dom: detail.dom(),
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([
        me.config({
          store: {
            mode: 'memory',
            initialValue: detail.getInitialValue()()
          }
        }),
        Composing.config({ find: Option.some })
      ]), $_2sk6q710oje4cc152.get(detail.dataBehaviours())),
      events: $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.runOnAttached(function (component, simulatedEvent) {
          me.setValue(component, detail.getInitialValue()());
        })])
    };
  };
  var DataField = $_cx5fhm10pje4cc157.single({
    name: 'DataField',
    factory: factory$2,
    configFields: [
      $_c4iqkly7je4cc0ud.strict('uid'),
      $_c4iqkly7je4cc0ud.strict('dom'),
      $_c4iqkly7je4cc0ud.strict('getInitialValue'),
      $_2sk6q710oje4cc152.field('dataBehaviours', [
        me,
        Composing
      ])
    ]
  });

  var get$9 = function (element) {
    return element.dom().value;
  };
  var set$6 = function (element, value) {
    if (value === undefined)
      throw new Error('Value.set was undefined');
    element.dom().value = value;
  };
  var $_gh6flk12eje4cc1e6 = {
    set: set$6,
    get: get$9
  };

  var schema$8 = [
    $_c4iqkly7je4cc0ud.option('data'),
    $_c4iqkly7je4cc0ud.defaulted('inputAttributes', {}),
    $_c4iqkly7je4cc0ud.defaulted('inputStyles', {}),
    $_c4iqkly7je4cc0ud.defaulted('type', 'input'),
    $_c4iqkly7je4cc0ud.defaulted('tag', 'input'),
    $_c4iqkly7je4cc0ud.defaulted('inputClasses', []),
    $_546z94z6je4cc0y4.onHandler('onSetValue'),
    $_c4iqkly7je4cc0ud.defaulted('styles', {}),
    $_c4iqkly7je4cc0ud.option('placeholder'),
    $_c4iqkly7je4cc0ud.defaulted('eventOrder', {}),
    $_2sk6q710oje4cc152.field('inputBehaviours', [
      me,
      Focusing
    ]),
    $_c4iqkly7je4cc0ud.defaulted('selectOnFocus', true)
  ];
  var behaviours = function (detail) {
    return $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([
      me.config({
        store: {
          mode: 'manual',
          initialValue: detail.data().getOr(undefined),
          getValue: function (input) {
            return $_gh6flk12eje4cc1e6.get(input.element());
          },
          setValue: function (input, data) {
            var current = $_gh6flk12eje4cc1e6.get(input.element());
            if (current !== data) {
              $_gh6flk12eje4cc1e6.set(input.element(), data);
            }
          }
        },
        onSetValue: detail.onSetValue()
      }),
      Focusing.config({
        onFocus: detail.selectOnFocus() === false ? $_aso7c6wjje4cc0om.noop : function (component) {
          var input = component.element();
          var value = $_gh6flk12eje4cc1e6.get(input);
          input.dom().setSelectionRange(0, value.length);
        }
      })
    ]), $_2sk6q710oje4cc152.get(detail.inputBehaviours()));
  };
  var dom$2 = function (detail) {
    return {
      tag: detail.tag(),
      attributes: $_dax12xwyje4cc0pt.deepMerge($_d08hr1xsje4cc0sg.wrapAll([{
          key: 'type',
          value: detail.type()
        }].concat(detail.placeholder().map(function (pc) {
        return {
          key: 'placeholder',
          value: pc
        };
      }).toArray())), detail.inputAttributes()),
      styles: detail.inputStyles(),
      classes: detail.inputClasses()
    };
  };
  var $_fhbwlk12dje4cc1dx = {
    schema: $_aso7c6wjje4cc0om.constant(schema$8),
    behaviours: behaviours,
    dom: dom$2
  };

  var factory$3 = function (detail, spec) {
    return {
      uid: detail.uid(),
      dom: $_fhbwlk12dje4cc1dx.dom(detail),
      components: [],
      behaviours: $_fhbwlk12dje4cc1dx.behaviours(detail),
      eventOrder: detail.eventOrder()
    };
  };
  var Input = $_cx5fhm10pje4cc157.single({
    name: 'Input',
    configFields: $_fhbwlk12dje4cc1dx.schema(),
    factory: factory$3
  });

  var exhibit$3 = function (base, tabConfig) {
    return $_5yi74lyhje4cc0vw.nu({
      attributes: $_d08hr1xsje4cc0sg.wrapAll([{
          key: tabConfig.tabAttr(),
          value: 'true'
        }])
    });
  };
  var $_1xvat412gje4cc1e8 = { exhibit: exhibit$3 };

  var TabstopSchema = [$_c4iqkly7je4cc0ud.defaulted('tabAttr', 'data-alloy-tabstop')];

  var Tabstopping = $_lsmliy2je4cc0te.create({
    fields: TabstopSchema,
    name: 'tabstopping',
    active: $_1xvat412gje4cc1e8
  });

  var clearInputBehaviour = 'input-clearing';
  var field$2 = function (name, placeholder) {
    var inputSpec = $_ceck8i11rje4cc1bf.record(Input.sketch({
      placeholder: placeholder,
      onSetValue: function (input, data) {
        $_3fionhwgje4cc0o9.emit(input, $_g7q1k3wije4cc0oi.input());
      },
      inputBehaviours: $_lsmliy2je4cc0te.derive([
        Composing.config({ find: Option.some }),
        Tabstopping.config({}),
        Keying.config({ mode: 'execution' })
      ]),
      selectOnFocus: false
    }));
    var buttonSpec = $_ceck8i11rje4cc1bf.record(Button.sketch({
      dom: $_8147ns113je4cc183.dom('<button class="${prefix}-input-container-x ${prefix}-icon-cancel-circle ${prefix}-icon"></button>'),
      action: function (button) {
        var input = inputSpec.get(button);
        me.setValue(input, '');
      }
    }));
    return {
      name: name,
      spec: Container.sketch({
        dom: $_8147ns113je4cc183.dom('<div class="${prefix}-input-container"></div>'),
        components: [
          inputSpec.asSpec(),
          buttonSpec.asSpec()
        ],
        containerBehaviours: $_lsmliy2je4cc0te.derive([
          Toggling.config({ toggleClass: $_gfh4lpzeje4cc0zd.resolve('input-container-empty') }),
          Composing.config({
            find: function (comp) {
              return Option.some(inputSpec.get(comp));
            }
          }),
          $_4dgb1i126je4cc1d8.config(clearInputBehaviour, [$_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.input(), function (iContainer) {
              var input = inputSpec.get(iContainer);
              var val = me.getValue(input);
              var f = val.length > 0 ? Toggling.off : Toggling.on;
              f(iContainer);
            })])
        ])
      })
    };
  };
  var hidden = function (name) {
    return {
      name: name,
      spec: DataField.sketch({
        dom: {
          tag: 'span',
          styles: { display: 'none' }
        },
        getInitialValue: function () {
          return Option.none();
        }
      })
    };
  };
  var $_81sg5u125je4cc1cu = {
    field: field$2,
    hidden: hidden
  };

  var nativeDisabled = [
    'input',
    'button',
    'textarea'
  ];
  var onLoad$5 = function (component, disableConfig, disableState) {
    if (disableConfig.disabled())
      disable(component, disableConfig, disableState);
  };
  var hasNative = function (component) {
    return $_elh0pqwsje4cc0p4.contains(nativeDisabled, $_7qwxg2xkje4cc0ro.name(component.element()));
  };
  var nativeIsDisabled = function (component) {
    return $_bjjq6ixrje4cc0s9.has(component.element(), 'disabled');
  };
  var nativeDisable = function (component) {
    $_bjjq6ixrje4cc0s9.set(component.element(), 'disabled', 'disabled');
  };
  var nativeEnable = function (component) {
    $_bjjq6ixrje4cc0s9.remove(component.element(), 'disabled');
  };
  var ariaIsDisabled = function (component) {
    return $_bjjq6ixrje4cc0s9.get(component.element(), 'aria-disabled') === 'true';
  };
  var ariaDisable = function (component) {
    $_bjjq6ixrje4cc0s9.set(component.element(), 'aria-disabled', 'true');
  };
  var ariaEnable = function (component) {
    $_bjjq6ixrje4cc0s9.set(component.element(), 'aria-disabled', 'false');
  };
  var disable = function (component, disableConfig, disableState) {
    disableConfig.disableClass().each(function (disableClass) {
      $_3x5q3zynje4cc0wi.add(component.element(), disableClass);
    });
    var f = hasNative(component) ? nativeDisable : ariaDisable;
    f(component);
  };
  var enable = function (component, disableConfig, disableState) {
    disableConfig.disableClass().each(function (disableClass) {
      $_3x5q3zynje4cc0wi.remove(component.element(), disableClass);
    });
    var f = hasNative(component) ? nativeEnable : ariaEnable;
    f(component);
  };
  var isDisabled = function (component) {
    return hasNative(component) ? nativeIsDisabled(component) : ariaIsDisabled(component);
  };
  var $_axsfhf12lje4cc1f4 = {
    enable: enable,
    disable: disable,
    isDisabled: isDisabled,
    onLoad: onLoad$5
  };

  var exhibit$4 = function (base, disableConfig, disableState) {
    return $_5yi74lyhje4cc0vw.nu({ classes: disableConfig.disabled() ? disableConfig.disableClass().map($_elh0pqwsje4cc0p4.pure).getOr([]) : [] });
  };
  var events$7 = function (disableConfig, disableState) {
    return $_ehtdq4y4je4cc0tx.derive([
      $_ehtdq4y4je4cc0tx.abort($_g6tooswhje4cc0of.execute(), function (component, simulatedEvent) {
        return $_axsfhf12lje4cc1f4.isDisabled(component, disableConfig, disableState);
      }),
      $_fc6d0uy3je4cc0tl.loadEvent(disableConfig, disableState, $_axsfhf12lje4cc1f4.onLoad)
    ]);
  };
  var $_8nkaz312kje4cc1f1 = {
    exhibit: exhibit$4,
    events: events$7
  };

  var DisableSchema = [
    $_c4iqkly7je4cc0ud.defaulted('disabled', false),
    $_c4iqkly7je4cc0ud.option('disableClass')
  ];

  var Disabling = $_lsmliy2je4cc0te.create({
    fields: DisableSchema,
    name: 'disabling',
    active: $_8nkaz312kje4cc1f1,
    apis: $_axsfhf12lje4cc1f4
  });

  var owner$1 = 'form';
  var schema$9 = [$_2sk6q710oje4cc152.field('formBehaviours', [me])];
  var getPartName = function (name) {
    return '<alloy.field.' + name + '>';
  };
  var sketch$6 = function (fSpec) {
    var parts = function () {
      var record = [];
      var field = function (name, config) {
        record.push(name);
        return $_ed9ak010tje4cc15q.generateOne(owner$1, getPartName(name), config);
      };
      return {
        field: field,
        record: function () {
          return record;
        }
      };
    }();
    var spec = fSpec(parts);
    var partNames = parts.record();
    var fieldParts = $_elh0pqwsje4cc0p4.map(partNames, function (n) {
      return $_chxroy10vje4cc16d.required({
        name: n,
        pname: getPartName(n)
      });
    });
    return $_47q9oj10sje4cc15m.composite(owner$1, schema$9, fieldParts, make, spec);
  };
  var make = function (detail, components, spec) {
    return $_dax12xwyje4cc0pt.deepMerge({
      'debug.sketcher': { 'Form': spec },
      uid: detail.uid(),
      dom: detail.dom(),
      components: components,
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([me.config({
          store: {
            mode: 'manual',
            getValue: function (form) {
              var optPs = $_ed9ak010tje4cc15q.getAllParts(form, detail);
              return $_i587hx0je4cc0pw.map(optPs, function (optPThunk, pName) {
                return optPThunk().bind(Composing.getCurrent).map(me.getValue);
              });
            },
            setValue: function (form, values) {
              $_i587hx0je4cc0pw.each(values, function (newValue, key) {
                $_ed9ak010tje4cc15q.getPart(form, detail, key).each(function (wrapper) {
                  Composing.getCurrent(wrapper).each(function (field) {
                    me.setValue(field, newValue);
                  });
                });
              });
            }
          }
        })]), $_2sk6q710oje4cc152.get(detail.formBehaviours())),
      apis: {
        getField: function (form, key) {
          return $_ed9ak010tje4cc15q.getPart(form, detail, key).bind(Composing.getCurrent);
        }
      }
    });
  };
  var $_ckgsqd12nje4cc1fd = {
    getField: $_3c5fj310qje4cc15g.makeApi(function (apis, component, key) {
      return apis.getField(component, key);
    }),
    sketch: sketch$6
  };

  var revocable = function (doRevoke) {
    var subject = Cell(Option.none());
    var revoke = function () {
      subject.get().each(doRevoke);
    };
    var clear = function () {
      revoke();
      subject.set(Option.none());
    };
    var set = function (s) {
      revoke();
      subject.set(Option.some(s));
    };
    var isSet = function () {
      return subject.get().isSome();
    };
    return {
      clear: clear,
      isSet: isSet,
      set: set
    };
  };
  var destroyable = function () {
    return revocable(function (s) {
      s.destroy();
    });
  };
  var unbindable = function () {
    return revocable(function (s) {
      s.unbind();
    });
  };
  var api$2 = function () {
    var subject = Cell(Option.none());
    var revoke = function () {
      subject.get().each(function (s) {
        s.destroy();
      });
    };
    var clear = function () {
      revoke();
      subject.set(Option.none());
    };
    var set = function (s) {
      revoke();
      subject.set(Option.some(s));
    };
    var run = function (f) {
      subject.get().each(f);
    };
    var isSet = function () {
      return subject.get().isSome();
    };
    return {
      clear: clear,
      isSet: isSet,
      set: set,
      run: run
    };
  };
  var value$3 = function () {
    var subject = Cell(Option.none());
    var clear = function () {
      subject.set(Option.none());
    };
    var set = function (s) {
      subject.set(Option.some(s));
    };
    var on = function (f) {
      subject.get().each(f);
    };
    var isSet = function () {
      return subject.get().isSome();
    };
    return {
      clear: clear,
      set: set,
      isSet: isSet,
      on: on
    };
  };
  var $_7qzgbo12oje4cc1fr = {
    destroyable: destroyable,
    unbindable: unbindable,
    api: api$2,
    value: value$3
  };

  var SWIPING_LEFT = 1;
  var SWIPING_RIGHT = -1;
  var SWIPING_NONE = 0;
  var init$3 = function (xValue) {
    return {
      xValue: xValue,
      points: []
    };
  };
  var move = function (model, xValue) {
    if (xValue === model.xValue) {
      return model;
    }
    var currentDirection = xValue - model.xValue > 0 ? SWIPING_LEFT : SWIPING_RIGHT;
    var newPoint = {
      direction: currentDirection,
      xValue: xValue
    };
    var priorPoints = function () {
      if (model.points.length === 0) {
        return [];
      } else {
        var prev = model.points[model.points.length - 1];
        return prev.direction === currentDirection ? model.points.slice(0, model.points.length - 1) : model.points;
      }
    }();
    return {
      xValue: xValue,
      points: priorPoints.concat([newPoint])
    };
  };
  var complete = function (model) {
    if (model.points.length === 0) {
      return SWIPING_NONE;
    } else {
      var firstDirection = model.points[0].direction;
      var lastDirection = model.points[model.points.length - 1].direction;
      return firstDirection === SWIPING_RIGHT && lastDirection === SWIPING_RIGHT ? SWIPING_RIGHT : firstDirection === SWIPING_LEFT && lastDirection === SWIPING_LEFT ? SWIPING_LEFT : SWIPING_NONE;
    }
  };
  var $_6qqius12pje4cc1fu = {
    init: init$3,
    move: move,
    complete: complete
  };

  var sketch$7 = function (rawSpec) {
    var navigateEvent = 'navigateEvent';
    var wrapperAdhocEvents = 'serializer-wrapper-events';
    var formAdhocEvents = 'form-events';
    var schema = $_lbfbgyeje4cc0vl.objOf([
      $_c4iqkly7je4cc0ud.strict('fields'),
      $_c4iqkly7je4cc0ud.defaulted('maxFieldIndex', rawSpec.fields.length - 1),
      $_c4iqkly7je4cc0ud.strict('onExecute'),
      $_c4iqkly7je4cc0ud.strict('getInitialValue'),
      $_c4iqkly7je4cc0ud.state('state', function () {
        return {
          dialogSwipeState: $_7qzgbo12oje4cc1fr.value(),
          currentScreen: Cell(0)
        };
      })
    ]);
    var spec = $_lbfbgyeje4cc0vl.asRawOrDie('SerialisedDialog', schema, rawSpec);
    var navigationButton = function (direction, directionName, enabled) {
      return Button.sketch({
        dom: $_8147ns113je4cc183.dom('<span class="${prefix}-icon-' + directionName + ' ${prefix}-icon"></span>'),
        action: function (button) {
          $_3fionhwgje4cc0o9.emitWith(button, navigateEvent, { direction: direction });
        },
        buttonBehaviours: $_lsmliy2je4cc0te.derive([Disabling.config({
            disableClass: $_gfh4lpzeje4cc0zd.resolve('toolbar-navigation-disabled'),
            disabled: !enabled
          })])
      });
    };
    var reposition = function (dialog, message) {
      $_by9cdqzxje4cc11d.descendant(dialog.element(), '.' + $_gfh4lpzeje4cc0zd.resolve('serialised-dialog-chain')).each(function (parent) {
        $_7ojow7103je4cc11s.set(parent, 'left', -spec.state.currentScreen.get() * message.width + 'px');
      });
    };
    var navigate = function (dialog, direction) {
      var screens = $_1n1hkzzvje4cc119.descendants(dialog.element(), '.' + $_gfh4lpzeje4cc0zd.resolve('serialised-dialog-screen'));
      $_by9cdqzxje4cc11d.descendant(dialog.element(), '.' + $_gfh4lpzeje4cc0zd.resolve('serialised-dialog-chain')).each(function (parent) {
        if (spec.state.currentScreen.get() + direction >= 0 && spec.state.currentScreen.get() + direction < screens.length) {
          $_7ojow7103je4cc11s.getRaw(parent, 'left').each(function (left) {
            var currentLeft = parseInt(left, 10);
            var w = $_ad4jyp11kje4cc1ai.get(screens[0]);
            $_7ojow7103je4cc11s.set(parent, 'left', currentLeft - direction * w + 'px');
          });
          spec.state.currentScreen.set(spec.state.currentScreen.get() + direction);
        }
      });
    };
    var focusInput = function (dialog) {
      var inputs = $_1n1hkzzvje4cc119.descendants(dialog.element(), 'input');
      var optInput = Option.from(inputs[spec.state.currentScreen.get()]);
      optInput.each(function (input) {
        dialog.getSystem().getByDom(input).each(function (inputComp) {
          $_3fionhwgje4cc0o9.dispatchFocus(dialog, inputComp.element());
        });
      });
      var dotitems = memDots.get(dialog);
      Highlighting.highlightAt(dotitems, spec.state.currentScreen.get());
    };
    var resetState = function () {
      spec.state.currentScreen.set(0);
      spec.state.dialogSwipeState.clear();
    };
    var memForm = $_ceck8i11rje4cc1bf.record($_ckgsqd12nje4cc1fd.sketch(function (parts) {
      return {
        dom: $_8147ns113je4cc183.dom('<div class="${prefix}-serialised-dialog"></div>'),
        components: [Container.sketch({
            dom: $_8147ns113je4cc183.dom('<div class="${prefix}-serialised-dialog-chain" style="left: 0px; position: absolute;"></div>'),
            components: $_elh0pqwsje4cc0p4.map(spec.fields, function (field, i) {
              return i <= spec.maxFieldIndex ? Container.sketch({
                dom: $_8147ns113je4cc183.dom('<div class="${prefix}-serialised-dialog-screen"></div>'),
                components: $_elh0pqwsje4cc0p4.flatten([
                  [navigationButton(-1, 'previous', i > 0)],
                  [parts.field(field.name, field.spec)],
                  [navigationButton(+1, 'next', i < spec.maxFieldIndex)]
                ])
              }) : parts.field(field.name, field.spec);
            })
          })],
        formBehaviours: $_lsmliy2je4cc0te.derive([
          $_g6oxx7zdje4cc0za.orientation(function (dialog, message) {
            reposition(dialog, message);
          }),
          Keying.config({
            mode: 'special',
            focusIn: function (dialog) {
              focusInput(dialog);
            },
            onTab: function (dialog) {
              navigate(dialog, +1);
              return Option.some(true);
            },
            onShiftTab: function (dialog) {
              navigate(dialog, -1);
              return Option.some(true);
            }
          }),
          $_4dgb1i126je4cc1d8.config(formAdhocEvents, [
            $_ehtdq4y4je4cc0tx.runOnAttached(function (dialog, simulatedEvent) {
              resetState();
              var dotitems = memDots.get(dialog);
              Highlighting.highlightFirst(dotitems);
              spec.getInitialValue(dialog).each(function (v) {
                me.setValue(dialog, v);
              });
            }),
            $_ehtdq4y4je4cc0tx.runOnExecute(spec.onExecute),
            $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.transitionend(), function (dialog, simulatedEvent) {
              if (simulatedEvent.event().raw().propertyName === 'left') {
                focusInput(dialog);
              }
            }),
            $_ehtdq4y4je4cc0tx.run(navigateEvent, function (dialog, simulatedEvent) {
              var direction = simulatedEvent.event().direction();
              navigate(dialog, direction);
            })
          ])
        ])
      };
    }));
    var memDots = $_ceck8i11rje4cc1bf.record({
      dom: $_8147ns113je4cc183.dom('<div class="${prefix}-dot-container"></div>'),
      behaviours: $_lsmliy2je4cc0te.derive([Highlighting.config({
          highlightClass: $_gfh4lpzeje4cc0zd.resolve('dot-active'),
          itemClass: $_gfh4lpzeje4cc0zd.resolve('dot-item')
        })]),
      components: $_elh0pqwsje4cc0p4.bind(spec.fields, function (_f, i) {
        return i <= spec.maxFieldIndex ? [$_8147ns113je4cc183.spec('<div class="${prefix}-dot-item ${prefix}-icon-full-dot ${prefix}-icon"></div>')] : [];
      })
    });
    return {
      dom: $_8147ns113je4cc183.dom('<div class="${prefix}-serializer-wrapper"></div>'),
      components: [
        memForm.asSpec(),
        memDots.asSpec()
      ],
      behaviours: $_lsmliy2je4cc0te.derive([
        Keying.config({
          mode: 'special',
          focusIn: function (wrapper) {
            var form = memForm.get(wrapper);
            Keying.focusIn(form);
          }
        }),
        $_4dgb1i126je4cc1d8.config(wrapperAdhocEvents, [
          $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.touchstart(), function (wrapper, simulatedEvent) {
            spec.state.dialogSwipeState.set($_6qqius12pje4cc1fu.init(simulatedEvent.event().raw().touches[0].clientX));
          }),
          $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.touchmove(), function (wrapper, simulatedEvent) {
            spec.state.dialogSwipeState.on(function (state) {
              simulatedEvent.event().prevent();
              spec.state.dialogSwipeState.set($_6qqius12pje4cc1fu.move(state, simulatedEvent.event().raw().touches[0].clientX));
            });
          }),
          $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.touchend(), function (wrapper) {
            spec.state.dialogSwipeState.on(function (state) {
              var dialog = memForm.get(wrapper);
              var direction = -1 * $_6qqius12pje4cc1fu.complete(state);
              navigate(dialog, direction);
            });
          })
        ])
      ])
    };
  };
  var $_gd02hk12ije4cc1ee = { sketch: sketch$7 };

  var getGroups = $_b57rigwlje4cc0oq.cached(function (realm, editor) {
    return [{
        label: 'the link group',
        items: [$_gd02hk12ije4cc1ee.sketch({
            fields: [
              $_81sg5u125je4cc1cu.field('url', 'Type or paste URL'),
              $_81sg5u125je4cc1cu.field('text', 'Link text'),
              $_81sg5u125je4cc1cu.field('title', 'Link title'),
              $_81sg5u125je4cc1cu.field('target', 'Link target'),
              $_81sg5u125je4cc1cu.hidden('link')
            ],
            maxFieldIndex: [
              'url',
              'text',
              'title',
              'target'
            ].length - 1,
            getInitialValue: function () {
              return Option.some($_6zjnes122je4cc1cj.getInfo(editor));
            },
            onExecute: function (dialog) {
              var info = me.getValue(dialog);
              $_6zjnes122je4cc1cj.applyInfo(editor, info);
              realm.restoreToolbar();
              editor.focus();
            }
          })]
      }];
  });
  var sketch$8 = function (realm, editor) {
    return $_afg6uzzfje4cc0zf.forToolbarStateAction(editor, 'link', 'link', function () {
      var groups = getGroups(realm, editor);
      realm.setContextToolbar(groups);
      $_7hikxr124je4cc1cr.forAndroid(editor, function () {
        realm.focusToolbar();
      });
      $_6zjnes122je4cc1cj.query(editor).each(function (link) {
        editor.selection.select(link.dom());
      });
    });
  };
  var $_c59rbc121je4cc1cf = { sketch: sketch$8 };

  var DefaultStyleFormats = [
    {
      title: 'Headings',
      items: [
        {
          title: 'Heading 1',
          format: 'h1'
        },
        {
          title: 'Heading 2',
          format: 'h2'
        },
        {
          title: 'Heading 3',
          format: 'h3'
        },
        {
          title: 'Heading 4',
          format: 'h4'
        },
        {
          title: 'Heading 5',
          format: 'h5'
        },
        {
          title: 'Heading 6',
          format: 'h6'
        }
      ]
    },
    {
      title: 'Inline',
      items: [
        {
          title: 'Bold',
          icon: 'bold',
          format: 'bold'
        },
        {
          title: 'Italic',
          icon: 'italic',
          format: 'italic'
        },
        {
          title: 'Underline',
          icon: 'underline',
          format: 'underline'
        },
        {
          title: 'Strikethrough',
          icon: 'strikethrough',
          format: 'strikethrough'
        },
        {
          title: 'Superscript',
          icon: 'superscript',
          format: 'superscript'
        },
        {
          title: 'Subscript',
          icon: 'subscript',
          format: 'subscript'
        },
        {
          title: 'Code',
          icon: 'code',
          format: 'code'
        }
      ]
    },
    {
      title: 'Blocks',
      items: [
        {
          title: 'Paragraph',
          format: 'p'
        },
        {
          title: 'Blockquote',
          format: 'blockquote'
        },
        {
          title: 'Div',
          format: 'div'
        },
        {
          title: 'Pre',
          format: 'pre'
        }
      ]
    },
    {
      title: 'Alignment',
      items: [
        {
          title: 'Left',
          icon: 'alignleft',
          format: 'alignleft'
        },
        {
          title: 'Center',
          icon: 'aligncenter',
          format: 'aligncenter'
        },
        {
          title: 'Right',
          icon: 'alignright',
          format: 'alignright'
        },
        {
          title: 'Justify',
          icon: 'alignjustify',
          format: 'alignjustify'
        }
      ]
    }
  ];

  var generateFrom = function (spec, all) {
    var schema = $_elh0pqwsje4cc0p4.map(all, function (a) {
      return $_c4iqkly7je4cc0ud.field(a.name(), a.name(), $_b7tb36y8je4cc0uh.asOption(), $_lbfbgyeje4cc0vl.objOf([
        $_c4iqkly7je4cc0ud.strict('config'),
        $_c4iqkly7je4cc0ud.defaulted('state', $_a5zdlbyjje4cc0w9)
      ]));
    });
    var validated = $_lbfbgyeje4cc0vl.asStruct('component.behaviours', $_lbfbgyeje4cc0vl.objOf(schema), spec.behaviours).fold(function (errInfo) {
      throw new Error($_lbfbgyeje4cc0vl.formatError(errInfo) + '\nComplete spec:\n' + $_bsgtdtydje4cc0vk.stringify(spec, null, 2));
    }, $_aso7c6wjje4cc0om.identity);
    return {
      list: all,
      data: $_i587hx0je4cc0pw.map(validated, function (blobOptionThunk) {
        var blobOption = blobOptionThunk();
        return $_aso7c6wjje4cc0om.constant(blobOption.map(function (blob) {
          return {
            config: blob.config(),
            state: blob.state().init(blob.config())
          };
        }));
      })
    };
  };
  var getBehaviours = function (bData) {
    return bData.list;
  };
  var getData = function (bData) {
    return bData.data;
  };
  var $_94hulh12wje4cc1ht = {
    generateFrom: generateFrom,
    getBehaviours: getBehaviours,
    getData: getData
  };

  var getBehaviours$1 = function (spec) {
    var behaviours = $_d08hr1xsje4cc0sg.readOptFrom(spec, 'behaviours').getOr({});
    var keys = $_elh0pqwsje4cc0p4.filter($_i587hx0je4cc0pw.keys(behaviours), function (k) {
      return behaviours[k] !== undefined;
    });
    return $_elh0pqwsje4cc0p4.map(keys, function (k) {
      return spec.behaviours[k].me;
    });
  };
  var generateFrom$1 = function (spec, all) {
    return $_94hulh12wje4cc1ht.generateFrom(spec, all);
  };
  var generate$4 = function (spec) {
    var all = getBehaviours$1(spec);
    return generateFrom$1(spec, all);
  };
  var $_9n5a0312vje4cc1ho = {
    generate: generate$4,
    generateFrom: generateFrom$1
  };

  var ComponentApi = $_arrccnylje4cc0wc.exactly([
    'getSystem',
    'config',
    'hasConfigured',
    'spec',
    'connect',
    'disconnect',
    'element',
    'syncComponents',
    'readState',
    'components',
    'events'
  ]);

  var SystemApi = $_arrccnylje4cc0wc.exactly([
    'debugInfo',
    'triggerFocus',
    'triggerEvent',
    'triggerEscape',
    'addToWorld',
    'removeFromWorld',
    'addToGui',
    'removeFromGui',
    'build',
    'getByUid',
    'getByDom',
    'broadcast',
    'broadcastOn'
  ]);

  function NoContextApi (getComp) {
    var fail = function (event) {
      return function () {
        throw new Error('The component must be in a context to send: ' + event + '\n' + $_dfrb81xmje4cc0rz.element(getComp().element()) + ' is not in context.');
      };
    };
    return SystemApi({
      debugInfo: $_aso7c6wjje4cc0om.constant('fake'),
      triggerEvent: fail('triggerEvent'),
      triggerFocus: fail('triggerFocus'),
      triggerEscape: fail('triggerEscape'),
      build: fail('build'),
      addToWorld: fail('addToWorld'),
      removeFromWorld: fail('removeFromWorld'),
      addToGui: fail('addToGui'),
      removeFromGui: fail('removeFromGui'),
      getByUid: fail('getByUid'),
      getByDom: fail('getByDom'),
      broadcast: fail('broadcast'),
      broadcastOn: fail('broadcastOn')
    });
  }

  var byInnerKey = function (data, tuple) {
    var r = {};
    $_i587hx0je4cc0pw.each(data, function (detail, key) {
      $_i587hx0je4cc0pw.each(detail, function (value, indexKey) {
        var chain = $_d08hr1xsje4cc0sg.readOr(indexKey, [])(r);
        r[indexKey] = chain.concat([tuple(key, value)]);
      });
    });
    return r;
  };
  var $_5drsfv131je4cc1im = { byInnerKey: byInnerKey };

  var behaviourDom = function (name, modification) {
    return {
      name: $_aso7c6wjje4cc0om.constant(name),
      modification: modification
    };
  };
  var concat = function (chain, aspect) {
    var values = $_elh0pqwsje4cc0p4.bind(chain, function (c) {
      return c.modification().getOr([]);
    });
    return Result.value($_d08hr1xsje4cc0sg.wrap(aspect, values));
  };
  var onlyOne = function (chain, aspect, order) {
    if (chain.length > 1)
      return Result.error('Multiple behaviours have tried to change DOM "' + aspect + '". The guilty behaviours are: ' + $_bsgtdtydje4cc0vk.stringify($_elh0pqwsje4cc0p4.map(chain, function (b) {
        return b.name();
      })) + '. At this stage, this ' + 'is not supported. Future releases might provide strategies for resolving this.');
    else if (chain.length === 0)
      return Result.value({});
    else
      return Result.value(chain[0].modification().fold(function () {
        return {};
      }, function (m) {
        return $_d08hr1xsje4cc0sg.wrap(aspect, m);
      }));
  };
  var duplicate = function (aspect, k, obj, behaviours) {
    return Result.error('Mulitple behaviours have tried to change the _' + k + '_ "' + aspect + '"' + '. The guilty behaviours are: ' + $_bsgtdtydje4cc0vk.stringify($_elh0pqwsje4cc0p4.bind(behaviours, function (b) {
      return b.modification().getOr({})[k] !== undefined ? [b.name()] : [];
    }), null, 2) + '. This is not currently supported.');
  };
  var safeMerge = function (chain, aspect) {
    var y = $_elh0pqwsje4cc0p4.foldl(chain, function (acc, c) {
      var obj = c.modification().getOr({});
      return acc.bind(function (accRest) {
        var parts = $_i587hx0je4cc0pw.mapToArray(obj, function (v, k) {
          return accRest[k] !== undefined ? duplicate(aspect, k, obj, chain) : Result.value($_d08hr1xsje4cc0sg.wrap(k, v));
        });
        return $_d08hr1xsje4cc0sg.consolidate(parts, accRest);
      });
    }, Result.value({}));
    return y.map(function (yValue) {
      return $_d08hr1xsje4cc0sg.wrap(aspect, yValue);
    });
  };
  var mergeTypes = {
    classes: concat,
    attributes: safeMerge,
    styles: safeMerge,
    domChildren: onlyOne,
    defChildren: onlyOne,
    innerHtml: onlyOne,
    value: onlyOne
  };
  var combine$1 = function (info, baseMod, behaviours, base) {
    var behaviourDoms = $_dax12xwyje4cc0pt.deepMerge({}, baseMod);
    $_elh0pqwsje4cc0p4.each(behaviours, function (behaviour) {
      behaviourDoms[behaviour.name()] = behaviour.exhibit(info, base);
    });
    var byAspect = $_5drsfv131je4cc1im.byInnerKey(behaviourDoms, behaviourDom);
    var usedAspect = $_i587hx0je4cc0pw.map(byAspect, function (values, aspect) {
      return $_elh0pqwsje4cc0p4.bind(values, function (value) {
        return value.modification().fold(function () {
          return [];
        }, function (v) {
          return [value];
        });
      });
    });
    var modifications = $_i587hx0je4cc0pw.mapToArray(usedAspect, function (values, aspect) {
      return $_d08hr1xsje4cc0sg.readOptFrom(mergeTypes, aspect).fold(function () {
        return Result.error('Unknown field type: ' + aspect);
      }, function (merger) {
        return merger(values, aspect);
      });
    });
    var consolidated = $_d08hr1xsje4cc0sg.consolidate(modifications, {});
    return consolidated.map($_5yi74lyhje4cc0vw.nu);
  };
  var $_7ab8nd130je4cc1ib = { combine: combine$1 };

  var sortKeys = function (label, keyName, array, order) {
    var sliced = array.slice(0);
    try {
      var sorted = sliced.sort(function (a, b) {
        var aKey = a[keyName]();
        var bKey = b[keyName]();
        var aIndex = order.indexOf(aKey);
        var bIndex = order.indexOf(bKey);
        if (aIndex === -1)
          throw new Error('The ordering for ' + label + ' does not have an entry for ' + aKey + '.\nOrder specified: ' + $_bsgtdtydje4cc0vk.stringify(order, null, 2));
        if (bIndex === -1)
          throw new Error('The ordering for ' + label + ' does not have an entry for ' + bKey + '.\nOrder specified: ' + $_bsgtdtydje4cc0vk.stringify(order, null, 2));
        if (aIndex < bIndex)
          return -1;
        else if (bIndex < aIndex)
          return 1;
        else
          return 0;
      });
      return Result.value(sorted);
    } catch (err) {
      return Result.error([err]);
    }
  };
  var $_fkgiie133je4cc1j0 = { sortKeys: sortKeys };

  var nu$7 = function (handler, purpose) {
    return {
      handler: handler,
      purpose: $_aso7c6wjje4cc0om.constant(purpose)
    };
  };
  var curryArgs = function (descHandler, extraArgs) {
    return {
      handler: $_aso7c6wjje4cc0om.curry.apply(undefined, [descHandler.handler].concat(extraArgs)),
      purpose: descHandler.purpose
    };
  };
  var getHandler = function (descHandler) {
    return descHandler.handler;
  };
  var $_4x8s3a134je4cc1j4 = {
    nu: nu$7,
    curryArgs: curryArgs,
    getHandler: getHandler
  };

  var behaviourTuple = function (name, handler) {
    return {
      name: $_aso7c6wjje4cc0om.constant(name),
      handler: $_aso7c6wjje4cc0om.constant(handler)
    };
  };
  var nameToHandlers = function (behaviours, info) {
    var r = {};
    $_elh0pqwsje4cc0p4.each(behaviours, function (behaviour) {
      r[behaviour.name()] = behaviour.handlers(info);
    });
    return r;
  };
  var groupByEvents = function (info, behaviours, base) {
    var behaviourEvents = $_dax12xwyje4cc0pt.deepMerge(base, nameToHandlers(behaviours, info));
    return $_5drsfv131je4cc1im.byInnerKey(behaviourEvents, behaviourTuple);
  };
  var combine$2 = function (info, eventOrder, behaviours, base) {
    var byEventName = groupByEvents(info, behaviours, base);
    return combineGroups(byEventName, eventOrder);
  };
  var assemble = function (rawHandler) {
    var handler = $_agxnjiy6je4cc0u4.read(rawHandler);
    return function (component, simulatedEvent) {
      var args = Array.prototype.slice.call(arguments, 0);
      if (handler.abort.apply(undefined, args)) {
        simulatedEvent.stop();
      } else if (handler.can.apply(undefined, args)) {
        handler.run.apply(undefined, args);
      }
    };
  };
  var missingOrderError = function (eventName, tuples) {
    return Result.error(['The event (' + eventName + ') has more than one behaviour that listens to it.\nWhen this occurs, you must ' + 'specify an event ordering for the behaviours in your spec (e.g. [ "listing", "toggling" ]).\nThe behaviours that ' + 'can trigger it are: ' + $_bsgtdtydje4cc0vk.stringify($_elh0pqwsje4cc0p4.map(tuples, function (c) {
        return c.name();
      }), null, 2)]);
  };
  var fuse$1 = function (tuples, eventOrder, eventName) {
    var order = eventOrder[eventName];
    if (!order)
      return missingOrderError(eventName, tuples);
    else
      return $_fkgiie133je4cc1j0.sortKeys('Event: ' + eventName, 'name', tuples, order).map(function (sortedTuples) {
        var handlers = $_elh0pqwsje4cc0p4.map(sortedTuples, function (tuple) {
          return tuple.handler();
        });
        return $_agxnjiy6je4cc0u4.fuse(handlers);
      });
  };
  var combineGroups = function (byEventName, eventOrder) {
    var r = $_i587hx0je4cc0pw.mapToArray(byEventName, function (tuples, eventName) {
      var combined = tuples.length === 1 ? Result.value(tuples[0].handler()) : fuse$1(tuples, eventOrder, eventName);
      return combined.map(function (handler) {
        var assembled = assemble(handler);
        var purpose = tuples.length > 1 ? $_elh0pqwsje4cc0p4.filter(eventOrder, function (o) {
          return $_elh0pqwsje4cc0p4.contains(tuples, function (t) {
            return t.name() === o;
          });
        }).join(' > ') : tuples[0].name();
        return $_d08hr1xsje4cc0sg.wrap(eventName, $_4x8s3a134je4cc1j4.nu(assembled, purpose));
      });
    });
    return $_d08hr1xsje4cc0sg.consolidate(r, {});
  };
  var $_d6tkhu132je4cc1iq = { combine: combine$2 };

  var toInfo = function (spec) {
    return $_lbfbgyeje4cc0vl.asStruct('custom.definition', $_lbfbgyeje4cc0vl.objOfOnly([
      $_c4iqkly7je4cc0ud.field('dom', 'dom', $_b7tb36y8je4cc0uh.strict(), $_lbfbgyeje4cc0vl.objOfOnly([
        $_c4iqkly7je4cc0ud.strict('tag'),
        $_c4iqkly7je4cc0ud.defaulted('styles', {}),
        $_c4iqkly7je4cc0ud.defaulted('classes', []),
        $_c4iqkly7je4cc0ud.defaulted('attributes', {}),
        $_c4iqkly7je4cc0ud.option('value'),
        $_c4iqkly7je4cc0ud.option('innerHtml')
      ])),
      $_c4iqkly7je4cc0ud.strict('components'),
      $_c4iqkly7je4cc0ud.strict('uid'),
      $_c4iqkly7je4cc0ud.defaulted('events', {}),
      $_c4iqkly7je4cc0ud.defaulted('apis', $_aso7c6wjje4cc0om.constant({})),
      $_c4iqkly7je4cc0ud.field('eventOrder', 'eventOrder', $_b7tb36y8je4cc0uh.mergeWith({
        'alloy.execute': [
          'disabling',
          'alloy.base.behaviour',
          'toggling'
        ],
        'alloy.focus': [
          'alloy.base.behaviour',
          'focusing',
          'keying'
        ],
        'alloy.system.init': [
          'alloy.base.behaviour',
          'disabling',
          'toggling',
          'representing'
        ],
        'input': [
          'alloy.base.behaviour',
          'representing',
          'streaming',
          'invalidating'
        ],
        'alloy.system.detached': [
          'alloy.base.behaviour',
          'representing'
        ]
      }), $_lbfbgyeje4cc0vl.anyValue()),
      $_c4iqkly7je4cc0ud.option('domModification'),
      $_546z94z6je4cc0y4.snapshot('originalSpec'),
      $_c4iqkly7je4cc0ud.defaulted('debug.sketcher', 'unknown')
    ]), spec);
  };
  var getUid = function (info) {
    return $_d08hr1xsje4cc0sg.wrap($_1zkymr10yje4cc17a.idAttr(), info.uid());
  };
  var toDefinition = function (info) {
    var base = {
      tag: info.dom().tag(),
      classes: info.dom().classes(),
      attributes: $_dax12xwyje4cc0pt.deepMerge(getUid(info), info.dom().attributes()),
      styles: info.dom().styles(),
      domChildren: $_elh0pqwsje4cc0p4.map(info.components(), function (comp) {
        return comp.element();
      })
    };
    return $_e0jxopyije4cc0w5.nu($_dax12xwyje4cc0pt.deepMerge(base, info.dom().innerHtml().map(function (h) {
      return $_d08hr1xsje4cc0sg.wrap('innerHtml', h);
    }).getOr({}), info.dom().value().map(function (h) {
      return $_d08hr1xsje4cc0sg.wrap('value', h);
    }).getOr({})));
  };
  var toModification = function (info) {
    return info.domModification().fold(function () {
      return $_5yi74lyhje4cc0vw.nu({});
    }, $_5yi74lyhje4cc0vw.nu);
  };
  var toApis = function (info) {
    return info.apis();
  };
  var toEvents = function (info) {
    return info.events();
  };
  var $_feqs17135je4cc1j7 = {
    toInfo: toInfo,
    toDefinition: toDefinition,
    toModification: toModification,
    toApis: toApis,
    toEvents: toEvents
  };

  var add$3 = function (element, classes) {
    $_elh0pqwsje4cc0p4.each(classes, function (x) {
      $_3x5q3zynje4cc0wi.add(element, x);
    });
  };
  var remove$6 = function (element, classes) {
    $_elh0pqwsje4cc0p4.each(classes, function (x) {
      $_3x5q3zynje4cc0wi.remove(element, x);
    });
  };
  var toggle$3 = function (element, classes) {
    $_elh0pqwsje4cc0p4.each(classes, function (x) {
      $_3x5q3zynje4cc0wi.toggle(element, x);
    });
  };
  var hasAll = function (element, classes) {
    return $_elh0pqwsje4cc0p4.forall(classes, function (clazz) {
      return $_3x5q3zynje4cc0wi.has(element, clazz);
    });
  };
  var hasAny = function (element, classes) {
    return $_elh0pqwsje4cc0p4.exists(classes, function (clazz) {
      return $_3x5q3zynje4cc0wi.has(element, clazz);
    });
  };
  var getNative = function (element) {
    var classList = element.dom().classList;
    var r = new Array(classList.length);
    for (var i = 0; i < classList.length; i++) {
      r[i] = classList.item(i);
    }
    return r;
  };
  var get$10 = function (element) {
    return $_6xyio7ypje4cc0wl.supports(element) ? getNative(element) : $_6xyio7ypje4cc0wl.get(element);
  };
  var $_7oz06b137je4cc1jy = {
    add: add$3,
    remove: remove$6,
    toggle: toggle$3,
    hasAll: hasAll,
    hasAny: hasAny,
    get: get$10
  };

  var getChildren = function (definition) {
    if (definition.domChildren().isSome() && definition.defChildren().isSome()) {
      throw new Error('Cannot specify children and child specs! Must be one or the other.\nDef: ' + $_e0jxopyije4cc0w5.defToStr(definition));
    } else {
      return definition.domChildren().fold(function () {
        var defChildren = definition.defChildren().getOr([]);
        return $_elh0pqwsje4cc0p4.map(defChildren, renderDef);
      }, function (domChildren) {
        return domChildren;
      });
    }
  };
  var renderToDom = function (definition) {
    var subject = $_407ejqxfje4cc0rb.fromTag(definition.tag());
    $_bjjq6ixrje4cc0s9.setAll(subject, definition.attributes().getOr({}));
    $_7oz06b137je4cc1jy.add(subject, definition.classes().getOr([]));
    $_7ojow7103je4cc11s.setAll(subject, definition.styles().getOr({}));
    $_5scgvxoje4cc0s3.set(subject, definition.innerHtml().getOr(''));
    var children = getChildren(definition);
    $_dy5220xije4cc0rj.append(subject, children);
    definition.value().each(function (value) {
      $_gh6flk12eje4cc1e6.set(subject, value);
    });
    return subject;
  };
  var renderDef = function (spec) {
    var definition = $_e0jxopyije4cc0w5.nu(spec);
    return renderToDom(definition);
  };
  var $_3n9ykl136je4cc1jn = { renderToDom: renderToDom };

  var build = function (spec) {
    var getMe = function () {
      return me;
    };
    var systemApi = Cell(NoContextApi(getMe));
    var info = $_lbfbgyeje4cc0vl.getOrDie($_feqs17135je4cc1j7.toInfo($_dax12xwyje4cc0pt.deepMerge(spec, { behaviours: undefined })));
    var bBlob = $_9n5a0312vje4cc1ho.generate(spec);
    var bList = $_94hulh12wje4cc1ht.getBehaviours(bBlob);
    var bData = $_94hulh12wje4cc1ht.getData(bBlob);
    var definition = $_feqs17135je4cc1j7.toDefinition(info);
    var baseModification = { 'alloy.base.modification': $_feqs17135je4cc1j7.toModification(info) };
    var modification = $_7ab8nd130je4cc1ib.combine(bData, baseModification, bList, definition).getOrDie();
    var modDefinition = $_5yi74lyhje4cc0vw.merge(definition, modification);
    var item = $_3n9ykl136je4cc1jn.renderToDom(modDefinition);
    var baseEvents = { 'alloy.base.behaviour': $_feqs17135je4cc1j7.toEvents(info) };
    var events = $_d6tkhu132je4cc1iq.combine(bData, info.eventOrder(), bList, baseEvents).getOrDie();
    var subcomponents = Cell(info.components());
    var connect = function (newApi) {
      systemApi.set(newApi);
    };
    var disconnect = function () {
      systemApi.set(NoContextApi(getMe));
    };
    var syncComponents = function () {
      var children = $_1jwy92x3je4cc0qa.children(item);
      var subs = $_elh0pqwsje4cc0p4.bind(children, function (child) {
        return systemApi.get().getByDom(child).fold(function () {
          return [];
        }, function (c) {
          return [c];
        });
      });
      subcomponents.set(subs);
    };
    var config = function (behaviour) {
      if (behaviour === $_3c5fj310qje4cc15g.apiConfig())
        return info.apis();
      var b = bData;
      var f = $_eh3yfywzje4cc0pu.isFunction(b[behaviour.name()]) ? b[behaviour.name()] : function () {
        throw new Error('Could not find ' + behaviour.name() + ' in ' + $_bsgtdtydje4cc0vk.stringify(spec, null, 2));
      };
      return f();
    };
    var hasConfigured = function (behaviour) {
      return $_eh3yfywzje4cc0pu.isFunction(bData[behaviour.name()]);
    };
    var readState = function (behaviourName) {
      return bData[behaviourName]().map(function (b) {
        return b.state.readState();
      }).getOr('not enabled');
    };
    var me = ComponentApi({
      getSystem: systemApi.get,
      config: config,
      hasConfigured: hasConfigured,
      spec: $_aso7c6wjje4cc0om.constant(spec),
      readState: readState,
      connect: connect,
      disconnect: disconnect,
      element: $_aso7c6wjje4cc0om.constant(item),
      syncComponents: syncComponents,
      components: subcomponents.get,
      events: $_aso7c6wjje4cc0om.constant(events)
    });
    return me;
  };
  var $_7f227g12uje4cc1h8 = { build: build };

  var isRecursive = function (component, originator, target) {
    return $_1stme4x9je4cc0qq.eq(originator, component.element()) && !$_1stme4x9je4cc0qq.eq(originator, target);
  };
  var $_4ewk81138je4cc1k3 = {
    events: $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.can($_g6tooswhje4cc0of.focus(), function (component, simulatedEvent) {
        var originator = simulatedEvent.event().originator();
        var target = simulatedEvent.event().target();
        if (isRecursive(component, originator, target)) {
          console.warn($_g6tooswhje4cc0of.focus() + ' did not get interpreted by the desired target. ' + '\nOriginator: ' + $_dfrb81xmje4cc0rz.element(originator) + '\nTarget: ' + $_dfrb81xmje4cc0rz.element(target) + '\nCheck the ' + $_g6tooswhje4cc0of.focus() + ' event handlers');
          return false;
        } else {
          return true;
        }
      })])
  };

  var make$1 = function (spec) {
    return spec;
  };
  var $_3shxe2139je4cc1k6 = { make: make$1 };

  var buildSubcomponents = function (spec) {
    var components = $_d08hr1xsje4cc0sg.readOr('components', [])(spec);
    return $_elh0pqwsje4cc0p4.map(components, build$1);
  };
  var buildFromSpec = function (userSpec) {
    var spec = $_3shxe2139je4cc1k6.make(userSpec);
    var components = buildSubcomponents(spec);
    var completeSpec = $_dax12xwyje4cc0pt.deepMerge($_4ewk81138je4cc1k3, spec, $_d08hr1xsje4cc0sg.wrap('components', components));
    return Result.value($_7f227g12uje4cc1h8.build(completeSpec));
  };
  var text = function (textContent) {
    var element = $_407ejqxfje4cc0rb.fromText(textContent);
    return external({ element: element });
  };
  var external = function (spec) {
    var extSpec = $_lbfbgyeje4cc0vl.asStructOrDie('external.component', $_lbfbgyeje4cc0vl.objOfOnly([
      $_c4iqkly7je4cc0ud.strict('element'),
      $_c4iqkly7je4cc0ud.option('uid')
    ]), spec);
    var systemApi = Cell(NoContextApi());
    var connect = function (newApi) {
      systemApi.set(newApi);
    };
    var disconnect = function () {
      systemApi.set(NoContextApi(function () {
        return me;
      }));
    };
    extSpec.uid().each(function (uid) {
      $_37lg5b10xje4cc173.writeOnly(extSpec.element(), uid);
    });
    var me = ComponentApi({
      getSystem: systemApi.get,
      config: Option.none,
      hasConfigured: $_aso7c6wjje4cc0om.constant(false),
      connect: connect,
      disconnect: disconnect,
      element: $_aso7c6wjje4cc0om.constant(extSpec.element()),
      spec: $_aso7c6wjje4cc0om.constant(spec),
      readState: $_aso7c6wjje4cc0om.constant('No state'),
      syncComponents: $_aso7c6wjje4cc0om.noop,
      components: $_aso7c6wjje4cc0om.constant([]),
      events: $_aso7c6wjje4cc0om.constant({})
    });
    return $_3c5fj310qje4cc15g.premade(me);
  };
  var build$1 = function (rawUserSpec) {
    return $_3c5fj310qje4cc15g.getPremade(rawUserSpec).fold(function () {
      var userSpecWithUid = $_dax12xwyje4cc0pt.deepMerge({ uid: $_37lg5b10xje4cc173.generate('') }, rawUserSpec);
      return buildFromSpec(userSpecWithUid).getOrDie();
    }, function (prebuilt) {
      return prebuilt;
    });
  };
  var $_ft6ka112tje4cc1gu = {
    build: build$1,
    premade: $_3c5fj310qje4cc15g.premade,
    external: external,
    text: text
  };

  var hoverEvent = 'alloy.item-hover';
  var focusEvent = 'alloy.item-focus';
  var onHover = function (item) {
    if ($_4hvdjzytje4cc0wz.search(item.element()).isNone() || Focusing.isFocused(item)) {
      if (!Focusing.isFocused(item))
        Focusing.focus(item);
      $_3fionhwgje4cc0o9.emitWith(item, hoverEvent, { item: item });
    }
  };
  var onFocus = function (item) {
    $_3fionhwgje4cc0o9.emitWith(item, focusEvent, { item: item });
  };
  var $_1p435713dje4cc1kl = {
    hover: $_aso7c6wjje4cc0om.constant(hoverEvent),
    focus: $_aso7c6wjje4cc0om.constant(focusEvent),
    onHover: onHover,
    onFocus: onFocus
  };

  var builder = function (info) {
    return {
      dom: $_dax12xwyje4cc0pt.deepMerge(info.dom(), { attributes: { role: info.toggling().isSome() ? 'menuitemcheckbox' : 'menuitem' } }),
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([
        info.toggling().fold(Toggling.revoke, function (tConfig) {
          return Toggling.config($_dax12xwyje4cc0pt.deepMerge({ aria: { mode: 'checked' } }, tConfig));
        }),
        Focusing.config({
          ignore: info.ignoreFocus(),
          onFocus: function (component) {
            $_1p435713dje4cc1kl.onFocus(component);
          }
        }),
        Keying.config({ mode: 'execution' }),
        me.config({
          store: {
            mode: 'memory',
            initialValue: info.data()
          }
        })
      ]), info.itemBehaviours()),
      events: $_ehtdq4y4je4cc0tx.derive([
        $_ehtdq4y4je4cc0tx.runWithTarget($_g6tooswhje4cc0of.tapOrClick(), $_3fionhwgje4cc0o9.emitExecute),
        $_ehtdq4y4je4cc0tx.cutter($_g7q1k3wije4cc0oi.mousedown()),
        $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.mouseover(), $_1p435713dje4cc1kl.onHover),
        $_ehtdq4y4je4cc0tx.run($_g6tooswhje4cc0of.focusItem(), Focusing.focus)
      ]),
      components: info.components(),
      domModification: info.domModification()
    };
  };
  var schema$10 = [
    $_c4iqkly7je4cc0ud.strict('data'),
    $_c4iqkly7je4cc0ud.strict('components'),
    $_c4iqkly7je4cc0ud.strict('dom'),
    $_c4iqkly7je4cc0ud.option('toggling'),
    $_c4iqkly7je4cc0ud.defaulted('itemBehaviours', {}),
    $_c4iqkly7je4cc0ud.defaulted('ignoreFocus', false),
    $_c4iqkly7je4cc0ud.defaulted('domModification', {}),
    $_546z94z6je4cc0y4.output('builder', builder)
  ];

  var builder$1 = function (detail) {
    return {
      dom: detail.dom(),
      components: detail.components(),
      events: $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.stopper($_g6tooswhje4cc0of.focusItem())])
    };
  };
  var schema$11 = [
    $_c4iqkly7je4cc0ud.strict('dom'),
    $_c4iqkly7je4cc0ud.strict('components'),
    $_546z94z6je4cc0y4.output('builder', builder$1)
  ];

  var owner$2 = 'item-widget';
  var partTypes = [$_chxroy10vje4cc16d.required({
      name: 'widget',
      overrides: function (detail) {
        return {
          behaviours: $_lsmliy2je4cc0te.derive([me.config({
              store: {
                mode: 'manual',
                getValue: function (component) {
                  return detail.data();
                },
                setValue: function () {
                }
              }
            })])
        };
      }
    })];
  var $_1nrr2013gje4cc1ky = {
    owner: $_aso7c6wjje4cc0om.constant(owner$2),
    parts: $_aso7c6wjje4cc0om.constant(partTypes)
  };

  var builder$2 = function (info) {
    var subs = $_ed9ak010tje4cc15q.substitutes($_1nrr2013gje4cc1ky.owner(), info, $_1nrr2013gje4cc1ky.parts());
    var components = $_ed9ak010tje4cc15q.components($_1nrr2013gje4cc1ky.owner(), info, subs.internals());
    var focusWidget = function (component) {
      return $_ed9ak010tje4cc15q.getPart(component, info, 'widget').map(function (widget) {
        Keying.focusIn(widget);
        return widget;
      });
    };
    var onHorizontalArrow = function (component, simulatedEvent) {
      return $_e3dsll108je4cc12k.inside(simulatedEvent.event().target()) ? Option.none() : function () {
        if (info.autofocus()) {
          simulatedEvent.setSource(component.element());
          return Option.none();
        } else {
          return Option.none();
        }
      }();
    };
    return $_dax12xwyje4cc0pt.deepMerge({
      dom: info.dom(),
      components: components,
      domModification: info.domModification(),
      events: $_ehtdq4y4je4cc0tx.derive([
        $_ehtdq4y4je4cc0tx.runOnExecute(function (component, simulatedEvent) {
          focusWidget(component).each(function (widget) {
            simulatedEvent.stop();
          });
        }),
        $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.mouseover(), $_1p435713dje4cc1kl.onHover),
        $_ehtdq4y4je4cc0tx.run($_g6tooswhje4cc0of.focusItem(), function (component, simulatedEvent) {
          if (info.autofocus())
            focusWidget(component);
          else
            Focusing.focus(component);
        })
      ]),
      behaviours: $_lsmliy2je4cc0te.derive([
        me.config({
          store: {
            mode: 'memory',
            initialValue: info.data()
          }
        }),
        Focusing.config({
          onFocus: function (component) {
            $_1p435713dje4cc1kl.onFocus(component);
          }
        }),
        Keying.config({
          mode: 'special',
          onLeft: onHorizontalArrow,
          onRight: onHorizontalArrow,
          onEscape: function (component, simulatedEvent) {
            if (!Focusing.isFocused(component) && !info.autofocus()) {
              Focusing.focus(component);
              return Option.some(true);
            } else if (info.autofocus()) {
              simulatedEvent.setSource(component.element());
              return Option.none();
            } else {
              return Option.none();
            }
          }
        })
      ])
    });
  };
  var schema$12 = [
    $_c4iqkly7je4cc0ud.strict('uid'),
    $_c4iqkly7je4cc0ud.strict('data'),
    $_c4iqkly7je4cc0ud.strict('components'),
    $_c4iqkly7je4cc0ud.strict('dom'),
    $_c4iqkly7je4cc0ud.defaulted('autofocus', false),
    $_c4iqkly7je4cc0ud.defaulted('domModification', {}),
    $_ed9ak010tje4cc15q.defaultUidsSchema($_1nrr2013gje4cc1ky.parts()),
    $_546z94z6je4cc0y4.output('builder', builder$2)
  ];

  var itemSchema$1 = $_lbfbgyeje4cc0vl.choose('type', {
    widget: schema$12,
    item: schema$10,
    separator: schema$11
  });
  var configureGrid = function (detail, movementInfo) {
    return {
      mode: 'flatgrid',
      selector: '.' + detail.markers().item(),
      initSize: {
        numColumns: movementInfo.initSize().numColumns(),
        numRows: movementInfo.initSize().numRows()
      },
      focusManager: detail.focusManager()
    };
  };
  var configureMenu = function (detail, movementInfo) {
    return {
      mode: 'menu',
      selector: '.' + detail.markers().item(),
      moveOnTab: movementInfo.moveOnTab(),
      focusManager: detail.focusManager()
    };
  };
  var parts = [$_chxroy10vje4cc16d.group({
      factory: {
        sketch: function (spec) {
          var itemInfo = $_lbfbgyeje4cc0vl.asStructOrDie('menu.spec item', itemSchema$1, spec);
          return itemInfo.builder()(itemInfo);
        }
      },
      name: 'items',
      unit: 'item',
      defaults: function (detail, u) {
        var fallbackUid = $_37lg5b10xje4cc173.generate('');
        return $_dax12xwyje4cc0pt.deepMerge({ uid: fallbackUid }, u);
      },
      overrides: function (detail, u) {
        return {
          type: u.type,
          ignoreFocus: detail.fakeFocus(),
          domModification: { classes: [detail.markers().item()] }
        };
      }
    })];
  var schema$13 = [
    $_c4iqkly7je4cc0ud.strict('value'),
    $_c4iqkly7je4cc0ud.strict('items'),
    $_c4iqkly7je4cc0ud.strict('dom'),
    $_c4iqkly7je4cc0ud.strict('components'),
    $_c4iqkly7je4cc0ud.defaulted('eventOrder', {}),
    $_2sk6q710oje4cc152.field('menuBehaviours', [
      Highlighting,
      me,
      Composing,
      Keying
    ]),
    $_c4iqkly7je4cc0ud.defaultedOf('movement', {
      mode: 'menu',
      moveOnTab: true
    }, $_lbfbgyeje4cc0vl.choose('mode', {
      grid: [
        $_546z94z6je4cc0y4.initSize(),
        $_546z94z6je4cc0y4.output('config', configureGrid)
      ],
      menu: [
        $_c4iqkly7je4cc0ud.defaulted('moveOnTab', true),
        $_546z94z6je4cc0y4.output('config', configureMenu)
      ]
    })),
    $_546z94z6je4cc0y4.itemMarkers(),
    $_c4iqkly7je4cc0ud.defaulted('fakeFocus', false),
    $_c4iqkly7je4cc0ud.defaulted('focusManager', $_17gb62zrje4cc10r.dom()),
    $_546z94z6je4cc0y4.onHandler('onHighlight')
  ];
  var $_9dk08k13bje4cc1k8 = {
    name: $_aso7c6wjje4cc0om.constant('Menu'),
    schema: $_aso7c6wjje4cc0om.constant(schema$13),
    parts: $_aso7c6wjje4cc0om.constant(parts)
  };

  var focusEvent$1 = 'alloy.menu-focus';
  var $_ep1onm13ije4cc1lc = { focus: $_aso7c6wjje4cc0om.constant(focusEvent$1) };

  var make$2 = function (detail, components, spec, externals) {
    return $_dax12xwyje4cc0pt.deepMerge({
      dom: $_dax12xwyje4cc0pt.deepMerge(detail.dom(), { attributes: { role: 'menu' } }),
      uid: detail.uid(),
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([
        Highlighting.config({
          highlightClass: detail.markers().selectedItem(),
          itemClass: detail.markers().item(),
          onHighlight: detail.onHighlight()
        }),
        me.config({
          store: {
            mode: 'memory',
            initialValue: detail.value()
          }
        }),
        Composing.config({ find: $_aso7c6wjje4cc0om.identity }),
        Keying.config(detail.movement().config()(detail, detail.movement()))
      ]), $_2sk6q710oje4cc152.get(detail.menuBehaviours())),
      events: $_ehtdq4y4je4cc0tx.derive([
        $_ehtdq4y4je4cc0tx.run($_1p435713dje4cc1kl.focus(), function (menu, simulatedEvent) {
          var event = simulatedEvent.event();
          menu.getSystem().getByDom(event.target()).each(function (item) {
            Highlighting.highlight(menu, item);
            simulatedEvent.stop();
            $_3fionhwgje4cc0o9.emitWith(menu, $_ep1onm13ije4cc1lc.focus(), {
              menu: menu,
              item: item
            });
          });
        }),
        $_ehtdq4y4je4cc0tx.run($_1p435713dje4cc1kl.hover(), function (menu, simulatedEvent) {
          var item = simulatedEvent.event().item();
          Highlighting.highlight(menu, item);
        })
      ]),
      components: components,
      eventOrder: detail.eventOrder()
    });
  };
  var $_fazacr13hje4cc1l7 = { make: make$2 };

  var Menu = $_cx5fhm10pje4cc157.composite({
    name: 'Menu',
    configFields: $_9dk08k13bje4cc1k8.schema(),
    partFields: $_9dk08k13bje4cc1k8.parts(),
    factory: $_fazacr13hje4cc1l7.make
  });

  var preserve$2 = function (f, container) {
    var ownerDoc = $_1jwy92x3je4cc0qa.owner(container);
    var refocus = $_4hvdjzytje4cc0wz.active(ownerDoc).bind(function (focused) {
      var hasFocus = function (elem) {
        return $_1stme4x9je4cc0qq.eq(focused, elem);
      };
      return hasFocus(container) ? Option.some(container) : $_40elmhyvje4cc0x6.descendant(container, hasFocus);
    });
    var result = f(container);
    refocus.each(function (oldFocus) {
      $_4hvdjzytje4cc0wz.active(ownerDoc).filter(function (newFocus) {
        return $_1stme4x9je4cc0qq.eq(newFocus, oldFocus);
      }).orThunk(function () {
        $_4hvdjzytje4cc0wz.focus(oldFocus);
      });
    });
    return result;
  };
  var $_bj2jvl13mje4cc1lq = { preserve: preserve$2 };

  var set$7 = function (component, replaceConfig, replaceState, data) {
    $_b576ucx1je4cc0py.detachChildren(component);
    $_bj2jvl13mje4cc1lq.preserve(function () {
      var children = $_elh0pqwsje4cc0p4.map(data, component.getSystem().build);
      $_elh0pqwsje4cc0p4.each(children, function (l) {
        $_b576ucx1je4cc0py.attach(component, l);
      });
    }, component.element());
  };
  var insert = function (component, replaceConfig, insertion, childSpec) {
    var child = component.getSystem().build(childSpec);
    $_b576ucx1je4cc0py.attachWith(component, child, insertion);
  };
  var append$2 = function (component, replaceConfig, replaceState, appendee) {
    insert(component, replaceConfig, $_9zp36ax2je4cc0q8.append, appendee);
  };
  var prepend$2 = function (component, replaceConfig, replaceState, prependee) {
    insert(component, replaceConfig, $_9zp36ax2je4cc0q8.prepend, prependee);
  };
  var remove$7 = function (component, replaceConfig, replaceState, removee) {
    var children = contents(component, replaceConfig);
    var foundChild = $_elh0pqwsje4cc0p4.find(children, function (child) {
      return $_1stme4x9je4cc0qq.eq(removee.element(), child.element());
    });
    foundChild.each($_b576ucx1je4cc0py.detach);
  };
  var contents = function (component, replaceConfig) {
    return component.components();
  };
  var $_bx30vi13lje4cc1ll = {
    append: append$2,
    prepend: prepend$2,
    remove: remove$7,
    set: set$7,
    contents: contents
  };

  var Replacing = $_lsmliy2je4cc0te.create({
    fields: [],
    name: 'replacing',
    apis: $_bx30vi13lje4cc1ll
  });

  var transpose = function (obj) {
    return $_i587hx0je4cc0pw.tupleMap(obj, function (v, k) {
      return {
        k: v,
        v: k
      };
    });
  };
  var trace = function (items, byItem, byMenu, finish) {
    return $_d08hr1xsje4cc0sg.readOptFrom(byMenu, finish).bind(function (triggerItem) {
      return $_d08hr1xsje4cc0sg.readOptFrom(items, triggerItem).bind(function (triggerMenu) {
        var rest = trace(items, byItem, byMenu, triggerMenu);
        return Option.some([triggerMenu].concat(rest));
      });
    }).getOr([]);
  };
  var generate$5 = function (menus, expansions) {
    var items = {};
    $_i587hx0je4cc0pw.each(menus, function (menuItems, menu) {
      $_elh0pqwsje4cc0p4.each(menuItems, function (item) {
        items[item] = menu;
      });
    });
    var byItem = expansions;
    var byMenu = transpose(expansions);
    var menuPaths = $_i587hx0je4cc0pw.map(byMenu, function (triggerItem, submenu) {
      return [submenu].concat(trace(items, byItem, byMenu, submenu));
    });
    return $_i587hx0je4cc0pw.map(items, function (path) {
      return $_d08hr1xsje4cc0sg.readOptFrom(menuPaths, path).getOr([path]);
    });
  };
  var $_3i4zr13pje4cc1ms = { generate: generate$5 };

  function LayeredState () {
    var expansions = Cell({});
    var menus = Cell({});
    var paths = Cell({});
    var primary = Cell(Option.none());
    var toItemValues = Cell($_aso7c6wjje4cc0om.constant([]));
    var clear = function () {
      expansions.set({});
      menus.set({});
      paths.set({});
      primary.set(Option.none());
    };
    var isClear = function () {
      return primary.get().isNone();
    };
    var setContents = function (sPrimary, sMenus, sExpansions, sToItemValues) {
      primary.set(Option.some(sPrimary));
      expansions.set(sExpansions);
      menus.set(sMenus);
      toItemValues.set(sToItemValues);
      var menuValues = sToItemValues(sMenus);
      var sPaths = $_3i4zr13pje4cc1ms.generate(menuValues, sExpansions);
      paths.set(sPaths);
    };
    var expand = function (itemValue) {
      return $_d08hr1xsje4cc0sg.readOptFrom(expansions.get(), itemValue).map(function (menu) {
        var current = $_d08hr1xsje4cc0sg.readOptFrom(paths.get(), itemValue).getOr([]);
        return [menu].concat(current);
      });
    };
    var collapse = function (itemValue) {
      return $_d08hr1xsje4cc0sg.readOptFrom(paths.get(), itemValue).bind(function (path) {
        return path.length > 1 ? Option.some(path.slice(1)) : Option.none();
      });
    };
    var refresh = function (itemValue) {
      return $_d08hr1xsje4cc0sg.readOptFrom(paths.get(), itemValue);
    };
    var lookupMenu = function (menuValue) {
      return $_d08hr1xsje4cc0sg.readOptFrom(menus.get(), menuValue);
    };
    var otherMenus = function (path) {
      var menuValues = toItemValues.get()(menus.get());
      return $_elh0pqwsje4cc0p4.difference($_i587hx0je4cc0pw.keys(menuValues), path);
    };
    var getPrimary = function () {
      return primary.get().bind(lookupMenu);
    };
    var getMenus = function () {
      return menus.get();
    };
    return {
      setContents: setContents,
      expand: expand,
      refresh: refresh,
      collapse: collapse,
      lookupMenu: lookupMenu,
      otherMenus: otherMenus,
      getPrimary: getPrimary,
      getMenus: getMenus,
      clear: clear,
      isClear: isClear
    };
  }

  var make$3 = function (detail, rawUiSpec) {
    var buildMenus = function (container, menus) {
      return $_i587hx0je4cc0pw.map(menus, function (spec, name) {
        var data = Menu.sketch($_dax12xwyje4cc0pt.deepMerge(spec, {
          value: name,
          items: spec.items,
          markers: $_d08hr1xsje4cc0sg.narrow(rawUiSpec.markers, [
            'item',
            'selectedItem'
          ]),
          fakeFocus: detail.fakeFocus(),
          onHighlight: detail.onHighlight(),
          focusManager: detail.fakeFocus() ? $_17gb62zrje4cc10r.highlights() : $_17gb62zrje4cc10r.dom()
        }));
        return container.getSystem().build(data);
      });
    };
    var state = LayeredState();
    var setup = function (container) {
      var componentMap = buildMenus(container, detail.data().menus());
      state.setContents(detail.data().primary(), componentMap, detail.data().expansions(), function (sMenus) {
        return toMenuValues(container, sMenus);
      });
      return state.getPrimary();
    };
    var getItemValue = function (item) {
      return me.getValue(item).value;
    };
    var toMenuValues = function (container, sMenus) {
      return $_i587hx0je4cc0pw.map(detail.data().menus(), function (data, menuName) {
        return $_elh0pqwsje4cc0p4.bind(data.items, function (item) {
          return item.type === 'separator' ? [] : [item.data.value];
        });
      });
    };
    var setActiveMenu = function (container, menu) {
      Highlighting.highlight(container, menu);
      Highlighting.getHighlighted(menu).orThunk(function () {
        return Highlighting.getFirst(menu);
      }).each(function (item) {
        $_3fionhwgje4cc0o9.dispatch(container, item.element(), $_g6tooswhje4cc0of.focusItem());
      });
    };
    var getMenus = function (state, menuValues) {
      return $_egl8u6y0je4cc0tb.cat($_elh0pqwsje4cc0p4.map(menuValues, state.lookupMenu));
    };
    var updateMenuPath = function (container, state, path) {
      return Option.from(path[0]).bind(state.lookupMenu).map(function (activeMenu) {
        var rest = getMenus(state, path.slice(1));
        $_elh0pqwsje4cc0p4.each(rest, function (r) {
          $_3x5q3zynje4cc0wi.add(r.element(), detail.markers().backgroundMenu());
        });
        if (!$_6jf42xxjje4cc0rm.inBody(activeMenu.element())) {
          Replacing.append(container, $_ft6ka112tje4cc1gu.premade(activeMenu));
        }
        $_7oz06b137je4cc1jy.remove(activeMenu.element(), [detail.markers().backgroundMenu()]);
        setActiveMenu(container, activeMenu);
        var others = getMenus(state, state.otherMenus(path));
        $_elh0pqwsje4cc0p4.each(others, function (o) {
          $_7oz06b137je4cc1jy.remove(o.element(), [detail.markers().backgroundMenu()]);
          if (!detail.stayInDom())
            Replacing.remove(container, o);
        });
        return activeMenu;
      });
    };
    var expandRight = function (container, item) {
      var value = getItemValue(item);
      return state.expand(value).bind(function (path) {
        Option.from(path[0]).bind(state.lookupMenu).each(function (activeMenu) {
          if (!$_6jf42xxjje4cc0rm.inBody(activeMenu.element())) {
            Replacing.append(container, $_ft6ka112tje4cc1gu.premade(activeMenu));
          }
          detail.onOpenSubmenu()(container, item, activeMenu);
          Highlighting.highlightFirst(activeMenu);
        });
        return updateMenuPath(container, state, path);
      });
    };
    var collapseLeft = function (container, item) {
      var value = getItemValue(item);
      return state.collapse(value).bind(function (path) {
        return updateMenuPath(container, state, path).map(function (activeMenu) {
          detail.onCollapseMenu()(container, item, activeMenu);
          return activeMenu;
        });
      });
    };
    var updateView = function (container, item) {
      var value = getItemValue(item);
      return state.refresh(value).bind(function (path) {
        return updateMenuPath(container, state, path);
      });
    };
    var onRight = function (container, item) {
      return $_e3dsll108je4cc12k.inside(item.element()) ? Option.none() : expandRight(container, item);
    };
    var onLeft = function (container, item) {
      return $_e3dsll108je4cc12k.inside(item.element()) ? Option.none() : collapseLeft(container, item);
    };
    var onEscape = function (container, item) {
      return collapseLeft(container, item).orThunk(function () {
        return detail.onEscape()(container, item);
      });
    };
    var keyOnItem = function (f) {
      return function (container, simulatedEvent) {
        return $_by9cdqzxje4cc11d.closest(simulatedEvent.getSource(), '.' + detail.markers().item()).bind(function (target) {
          return container.getSystem().getByDom(target).bind(function (item) {
            return f(container, item);
          });
        });
      };
    };
    var events = $_ehtdq4y4je4cc0tx.derive([
      $_ehtdq4y4je4cc0tx.run($_ep1onm13ije4cc1lc.focus(), function (sandbox, simulatedEvent) {
        var menu = simulatedEvent.event().menu();
        Highlighting.highlight(sandbox, menu);
      }),
      $_ehtdq4y4je4cc0tx.runOnExecute(function (sandbox, simulatedEvent) {
        var target = simulatedEvent.event().target();
        return sandbox.getSystem().getByDom(target).bind(function (item) {
          var itemValue = getItemValue(item);
          if (itemValue.indexOf('collapse-item') === 0) {
            return collapseLeft(sandbox, item);
          }
          return expandRight(sandbox, item).orThunk(function () {
            return detail.onExecute()(sandbox, item);
          });
        });
      }),
      $_ehtdq4y4je4cc0tx.runOnAttached(function (container, simulatedEvent) {
        setup(container).each(function (primary) {
          Replacing.append(container, $_ft6ka112tje4cc1gu.premade(primary));
          if (detail.openImmediately()) {
            setActiveMenu(container, primary);
            detail.onOpenMenu()(container, primary);
          }
        });
      })
    ].concat(detail.navigateOnHover() ? [$_ehtdq4y4je4cc0tx.run($_1p435713dje4cc1kl.hover(), function (sandbox, simulatedEvent) {
        var item = simulatedEvent.event().item();
        updateView(sandbox, item);
        expandRight(sandbox, item);
        detail.onHover()(sandbox, item);
      })] : []));
    var collapseMenuApi = function (container) {
      Highlighting.getHighlighted(container).each(function (currentMenu) {
        Highlighting.getHighlighted(currentMenu).each(function (currentItem) {
          collapseLeft(container, currentItem);
        });
      });
    };
    return {
      uid: detail.uid(),
      dom: detail.dom(),
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([
        Keying.config({
          mode: 'special',
          onRight: keyOnItem(onRight),
          onLeft: keyOnItem(onLeft),
          onEscape: keyOnItem(onEscape),
          focusIn: function (container, keyInfo) {
            state.getPrimary().each(function (primary) {
              $_3fionhwgje4cc0o9.dispatch(container, primary.element(), $_g6tooswhje4cc0of.focusItem());
            });
          }
        }),
        Highlighting.config({
          highlightClass: detail.markers().selectedMenu(),
          itemClass: detail.markers().menu()
        }),
        Composing.config({
          find: function (container) {
            return Highlighting.getHighlighted(container);
          }
        }),
        Replacing.config({})
      ]), $_2sk6q710oje4cc152.get(detail.tmenuBehaviours())),
      eventOrder: detail.eventOrder(),
      apis: { collapseMenu: collapseMenuApi },
      events: events
    };
  };
  var $_dzds8c13nje4cc1lz = {
    make: make$3,
    collapseItem: $_aso7c6wjje4cc0om.constant('collapse-item')
  };

  var tieredData = function (primary, menus, expansions) {
    return {
      primary: primary,
      menus: menus,
      expansions: expansions
    };
  };
  var singleData = function (name, menu) {
    return {
      primary: name,
      menus: $_d08hr1xsje4cc0sg.wrap(name, menu),
      expansions: {}
    };
  };
  var collapseItem = function (text) {
    return {
      value: $_eb3s8410rje4cc15l.generate($_dzds8c13nje4cc1lz.collapseItem()),
      text: text
    };
  };
  var TieredMenu = $_cx5fhm10pje4cc157.single({
    name: 'TieredMenu',
    configFields: [
      $_546z94z6je4cc0y4.onStrictKeyboardHandler('onExecute'),
      $_546z94z6je4cc0y4.onStrictKeyboardHandler('onEscape'),
      $_546z94z6je4cc0y4.onStrictHandler('onOpenMenu'),
      $_546z94z6je4cc0y4.onStrictHandler('onOpenSubmenu'),
      $_546z94z6je4cc0y4.onHandler('onCollapseMenu'),
      $_c4iqkly7je4cc0ud.defaulted('openImmediately', true),
      $_c4iqkly7je4cc0ud.strictObjOf('data', [
        $_c4iqkly7je4cc0ud.strict('primary'),
        $_c4iqkly7je4cc0ud.strict('menus'),
        $_c4iqkly7je4cc0ud.strict('expansions')
      ]),
      $_c4iqkly7je4cc0ud.defaulted('fakeFocus', false),
      $_546z94z6je4cc0y4.onHandler('onHighlight'),
      $_546z94z6je4cc0y4.onHandler('onHover'),
      $_546z94z6je4cc0y4.tieredMenuMarkers(),
      $_c4iqkly7je4cc0ud.strict('dom'),
      $_c4iqkly7je4cc0ud.defaulted('navigateOnHover', true),
      $_c4iqkly7je4cc0ud.defaulted('stayInDom', false),
      $_2sk6q710oje4cc152.field('tmenuBehaviours', [
        Keying,
        Highlighting,
        Composing,
        Replacing
      ]),
      $_c4iqkly7je4cc0ud.defaulted('eventOrder', {})
    ],
    apis: {
      collapseMenu: function (apis, tmenu) {
        apis.collapseMenu(tmenu);
      }
    },
    factory: $_dzds8c13nje4cc1lz.make,
    extraApis: {
      tieredData: tieredData,
      singleData: singleData,
      collapseItem: collapseItem
    }
  });

  var findRoute = function (component, transConfig, transState, route) {
    return $_d08hr1xsje4cc0sg.readOptFrom(transConfig.routes(), route.start()).map($_aso7c6wjje4cc0om.apply).bind(function (sConfig) {
      return $_d08hr1xsje4cc0sg.readOptFrom(sConfig, route.destination()).map($_aso7c6wjje4cc0om.apply);
    });
  };
  var getTransition = function (comp, transConfig, transState) {
    var route = getCurrentRoute(comp, transConfig, transState);
    return route.bind(function (r) {
      return getTransitionOf(comp, transConfig, transState, r);
    });
  };
  var getTransitionOf = function (comp, transConfig, transState, route) {
    return findRoute(comp, transConfig, transState, route).bind(function (r) {
      return r.transition().map(function (t) {
        return {
          transition: $_aso7c6wjje4cc0om.constant(t),
          route: $_aso7c6wjje4cc0om.constant(r)
        };
      });
    });
  };
  var disableTransition = function (comp, transConfig, transState) {
    getTransition(comp, transConfig, transState).each(function (routeTransition) {
      var t = routeTransition.transition();
      $_3x5q3zynje4cc0wi.remove(comp.element(), t.transitionClass());
      $_bjjq6ixrje4cc0s9.remove(comp.element(), transConfig.destinationAttr());
    });
  };
  var getNewRoute = function (comp, transConfig, transState, destination) {
    return {
      start: $_aso7c6wjje4cc0om.constant($_bjjq6ixrje4cc0s9.get(comp.element(), transConfig.stateAttr())),
      destination: $_aso7c6wjje4cc0om.constant(destination)
    };
  };
  var getCurrentRoute = function (comp, transConfig, transState) {
    var el = comp.element();
    return $_bjjq6ixrje4cc0s9.has(el, transConfig.destinationAttr()) ? Option.some({
      start: $_aso7c6wjje4cc0om.constant($_bjjq6ixrje4cc0s9.get(comp.element(), transConfig.stateAttr())),
      destination: $_aso7c6wjje4cc0om.constant($_bjjq6ixrje4cc0s9.get(comp.element(), transConfig.destinationAttr()))
    }) : Option.none();
  };
  var jumpTo = function (comp, transConfig, transState, destination) {
    disableTransition(comp, transConfig, transState);
    if ($_bjjq6ixrje4cc0s9.has(comp.element(), transConfig.stateAttr()) && $_bjjq6ixrje4cc0s9.get(comp.element(), transConfig.stateAttr()) !== destination)
      transConfig.onFinish()(comp, destination);
    $_bjjq6ixrje4cc0s9.set(comp.element(), transConfig.stateAttr(), destination);
  };
  var fasttrack = function (comp, transConfig, transState, destination) {
    if ($_bjjq6ixrje4cc0s9.has(comp.element(), transConfig.destinationAttr())) {
      $_bjjq6ixrje4cc0s9.set(comp.element(), transConfig.stateAttr(), $_bjjq6ixrje4cc0s9.get(comp.element(), transConfig.destinationAttr()));
      $_bjjq6ixrje4cc0s9.remove(comp.element(), transConfig.destinationAttr());
    }
  };
  var progressTo = function (comp, transConfig, transState, destination) {
    fasttrack(comp, transConfig, transState, destination);
    var route = getNewRoute(comp, transConfig, transState, destination);
    getTransitionOf(comp, transConfig, transState, route).fold(function () {
      jumpTo(comp, transConfig, transState, destination);
    }, function (routeTransition) {
      disableTransition(comp, transConfig, transState);
      var t = routeTransition.transition();
      $_3x5q3zynje4cc0wi.add(comp.element(), t.transitionClass());
      $_bjjq6ixrje4cc0s9.set(comp.element(), transConfig.destinationAttr(), destination);
    });
  };
  var getState = function (comp, transConfig, transState) {
    var e = comp.element();
    return $_bjjq6ixrje4cc0s9.has(e, transConfig.stateAttr()) ? Option.some($_bjjq6ixrje4cc0s9.get(e, transConfig.stateAttr())) : Option.none();
  };
  var $_8qmrht13sje4cc1nd = {
    findRoute: findRoute,
    disableTransition: disableTransition,
    getCurrentRoute: getCurrentRoute,
    jumpTo: jumpTo,
    progressTo: progressTo,
    getState: getState
  };

  var events$8 = function (transConfig, transState) {
    return $_ehtdq4y4je4cc0tx.derive([
      $_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.transitionend(), function (component, simulatedEvent) {
        var raw = simulatedEvent.event().raw();
        $_8qmrht13sje4cc1nd.getCurrentRoute(component, transConfig, transState).each(function (route) {
          $_8qmrht13sje4cc1nd.findRoute(component, transConfig, transState, route).each(function (rInfo) {
            rInfo.transition().each(function (rTransition) {
              if (raw.propertyName === rTransition.property()) {
                $_8qmrht13sje4cc1nd.jumpTo(component, transConfig, transState, route.destination());
                transConfig.onTransition()(component, route);
              }
            });
          });
        });
      }),
      $_ehtdq4y4je4cc0tx.runOnAttached(function (comp, se) {
        $_8qmrht13sje4cc1nd.jumpTo(comp, transConfig, transState, transConfig.initialState());
      })
    ]);
  };
  var $_c7riph13rje4cc1nb = { events: events$8 };

  var TransitionSchema = [
    $_c4iqkly7je4cc0ud.defaulted('destinationAttr', 'data-transitioning-destination'),
    $_c4iqkly7je4cc0ud.defaulted('stateAttr', 'data-transitioning-state'),
    $_c4iqkly7je4cc0ud.strict('initialState'),
    $_546z94z6je4cc0y4.onHandler('onTransition'),
    $_546z94z6je4cc0y4.onHandler('onFinish'),
    $_c4iqkly7je4cc0ud.strictOf('routes', $_lbfbgyeje4cc0vl.setOf(Result.value, $_lbfbgyeje4cc0vl.setOf(Result.value, $_lbfbgyeje4cc0vl.objOfOnly([$_c4iqkly7je4cc0ud.optionObjOfOnly('transition', [
        $_c4iqkly7je4cc0ud.strict('property'),
        $_c4iqkly7je4cc0ud.strict('transitionClass')
      ])]))))
  ];

  var createRoutes = function (routes) {
    var r = {};
    $_i587hx0je4cc0pw.each(routes, function (v, k) {
      var waypoints = k.split('<->');
      r[waypoints[0]] = $_d08hr1xsje4cc0sg.wrap(waypoints[1], v);
      r[waypoints[1]] = $_d08hr1xsje4cc0sg.wrap(waypoints[0], v);
    });
    return r;
  };
  var createBistate = function (first, second, transitions) {
    return $_d08hr1xsje4cc0sg.wrapAll([
      {
        key: first,
        value: $_d08hr1xsje4cc0sg.wrap(second, transitions)
      },
      {
        key: second,
        value: $_d08hr1xsje4cc0sg.wrap(first, transitions)
      }
    ]);
  };
  var createTristate = function (first, second, third, transitions) {
    return $_d08hr1xsje4cc0sg.wrapAll([
      {
        key: first,
        value: $_d08hr1xsje4cc0sg.wrapAll([
          {
            key: second,
            value: transitions
          },
          {
            key: third,
            value: transitions
          }
        ])
      },
      {
        key: second,
        value: $_d08hr1xsje4cc0sg.wrapAll([
          {
            key: first,
            value: transitions
          },
          {
            key: third,
            value: transitions
          }
        ])
      },
      {
        key: third,
        value: $_d08hr1xsje4cc0sg.wrapAll([
          {
            key: first,
            value: transitions
          },
          {
            key: second,
            value: transitions
          }
        ])
      }
    ]);
  };
  var Transitioning = $_lsmliy2je4cc0te.create({
    fields: TransitionSchema,
    name: 'transitioning',
    active: $_c7riph13rje4cc1nb,
    apis: $_8qmrht13sje4cc1nd,
    extra: {
      createRoutes: createRoutes,
      createBistate: createBistate,
      createTristate: createTristate
    }
  });

  var scrollable = $_gfh4lpzeje4cc0zd.resolve('scrollable');
  var register = function (element) {
    $_3x5q3zynje4cc0wi.add(element, scrollable);
  };
  var deregister = function (element) {
    $_3x5q3zynje4cc0wi.remove(element, scrollable);
  };
  var $_4d6st213uje4cc1ns = {
    register: register,
    deregister: deregister,
    scrollable: $_aso7c6wjje4cc0om.constant(scrollable)
  };

  var getValue$4 = function (item) {
    return $_d08hr1xsje4cc0sg.readOptFrom(item, 'format').getOr(item.title);
  };
  var convert$1 = function (formats, memMenuThunk) {
    var mainMenu = makeMenu('Styles', [].concat($_elh0pqwsje4cc0p4.map(formats.items, function (k) {
      return makeItem(getValue$4(k), k.title, k.isSelected(), k.getPreview(), $_d08hr1xsje4cc0sg.hasKey(formats.expansions, getValue$4(k)));
    })), memMenuThunk, false);
    var submenus = $_i587hx0je4cc0pw.map(formats.menus, function (menuItems, menuName) {
      var items = $_elh0pqwsje4cc0p4.map(menuItems, function (item) {
        return makeItem(getValue$4(item), item.title, item.isSelected !== undefined ? item.isSelected() : false, item.getPreview !== undefined ? item.getPreview() : '', $_d08hr1xsje4cc0sg.hasKey(formats.expansions, getValue$4(item)));
      });
      return makeMenu(menuName, items, memMenuThunk, true);
    });
    var menus = $_dax12xwyje4cc0pt.deepMerge(submenus, $_d08hr1xsje4cc0sg.wrap('styles', mainMenu));
    var tmenu = TieredMenu.tieredData('styles', menus, formats.expansions);
    return { tmenu: tmenu };
  };
  var makeItem = function (value, text, selected, preview, isMenu) {
    return {
      data: {
        value: value,
        text: text
      },
      type: 'item',
      dom: {
        tag: 'div',
        classes: isMenu ? [$_gfh4lpzeje4cc0zd.resolve('styles-item-is-menu')] : []
      },
      toggling: {
        toggleOnExecute: false,
        toggleClass: $_gfh4lpzeje4cc0zd.resolve('format-matches'),
        selected: selected
      },
      itemBehaviours: $_lsmliy2je4cc0te.derive(isMenu ? [] : [$_g6oxx7zdje4cc0za.format(value, function (comp, status) {
          var toggle = status ? Toggling.on : Toggling.off;
          toggle(comp);
        })]),
      components: [{
          dom: {
            tag: 'div',
            attributes: { style: preview },
            innerHtml: text
          }
        }]
    };
  };
  var makeMenu = function (value, items, memMenuThunk, collapsable) {
    return {
      value: value,
      dom: { tag: 'div' },
      components: [
        Button.sketch({
          dom: {
            tag: 'div',
            classes: [$_gfh4lpzeje4cc0zd.resolve('styles-collapser')]
          },
          components: collapsable ? [
            {
              dom: {
                tag: 'span',
                classes: [$_gfh4lpzeje4cc0zd.resolve('styles-collapse-icon')]
              }
            },
            $_ft6ka112tje4cc1gu.text(value)
          ] : [$_ft6ka112tje4cc1gu.text(value)],
          action: function (item) {
            if (collapsable) {
              var comp = memMenuThunk().get(item);
              TieredMenu.collapseMenu(comp);
            }
          }
        }),
        {
          dom: {
            tag: 'div',
            classes: [$_gfh4lpzeje4cc0zd.resolve('styles-menu-items-container')]
          },
          components: [Menu.parts().items({})],
          behaviours: $_lsmliy2je4cc0te.derive([$_4dgb1i126je4cc1d8.config('adhoc-scrollable-menu', [
              $_ehtdq4y4je4cc0tx.runOnAttached(function (component, simulatedEvent) {
                $_7ojow7103je4cc11s.set(component.element(), 'overflow-y', 'auto');
                $_7ojow7103je4cc11s.set(component.element(), '-webkit-overflow-scrolling', 'touch');
                $_4d6st213uje4cc1ns.register(component.element());
              }),
              $_ehtdq4y4je4cc0tx.runOnDetached(function (component) {
                $_7ojow7103je4cc11s.remove(component.element(), 'overflow-y');
                $_7ojow7103je4cc11s.remove(component.element(), '-webkit-overflow-scrolling');
                $_4d6st213uje4cc1ns.deregister(component.element());
              })
            ])])
        }
      ],
      items: items,
      menuBehaviours: $_lsmliy2je4cc0te.derive([Transitioning.config({
          initialState: 'after',
          routes: Transitioning.createTristate('before', 'current', 'after', {
            transition: {
              property: 'transform',
              transitionClass: 'transitioning'
            }
          })
        })])
    };
  };
  var sketch$9 = function (settings) {
    var dataset = convert$1(settings.formats, function () {
      return memMenu;
    });
    var memMenu = $_ceck8i11rje4cc1bf.record(TieredMenu.sketch({
      dom: {
        tag: 'div',
        classes: [$_gfh4lpzeje4cc0zd.resolve('styles-menu')]
      },
      components: [],
      fakeFocus: true,
      stayInDom: true,
      onExecute: function (tmenu, item) {
        var v = me.getValue(item);
        settings.handle(item, v.value);
      },
      onEscape: function () {
      },
      onOpenMenu: function (container, menu) {
        var w = $_ad4jyp11kje4cc1ai.get(container.element());
        $_ad4jyp11kje4cc1ai.set(menu.element(), w);
        Transitioning.jumpTo(menu, 'current');
      },
      onOpenSubmenu: function (container, item, submenu) {
        var w = $_ad4jyp11kje4cc1ai.get(container.element());
        var menu = $_by9cdqzxje4cc11d.ancestor(item.element(), '[role="menu"]').getOrDie('hacky');
        var menuComp = container.getSystem().getByDom(menu).getOrDie();
        $_ad4jyp11kje4cc1ai.set(submenu.element(), w);
        Transitioning.progressTo(menuComp, 'before');
        Transitioning.jumpTo(submenu, 'after');
        Transitioning.progressTo(submenu, 'current');
      },
      onCollapseMenu: function (container, item, menu) {
        var submenu = $_by9cdqzxje4cc11d.ancestor(item.element(), '[role="menu"]').getOrDie('hacky');
        var submenuComp = container.getSystem().getByDom(submenu).getOrDie();
        Transitioning.progressTo(submenuComp, 'after');
        Transitioning.progressTo(menu, 'current');
      },
      navigateOnHover: false,
      openImmediately: true,
      data: dataset.tmenu,
      markers: {
        backgroundMenu: $_gfh4lpzeje4cc0zd.resolve('styles-background-menu'),
        menu: $_gfh4lpzeje4cc0zd.resolve('styles-menu'),
        selectedMenu: $_gfh4lpzeje4cc0zd.resolve('styles-selected-menu'),
        item: $_gfh4lpzeje4cc0zd.resolve('styles-item'),
        selectedItem: $_gfh4lpzeje4cc0zd.resolve('styles-selected-item')
      }
    }));
    return memMenu.asSpec();
  };
  var $_dlkafq12sje4cc1g5 = { sketch: sketch$9 };

  var getFromExpandingItem = function (item) {
    var newItem = $_dax12xwyje4cc0pt.deepMerge($_d08hr1xsje4cc0sg.exclude(item, ['items']), { menu: true });
    var rest = expand(item.items);
    var newMenus = $_dax12xwyje4cc0pt.deepMerge(rest.menus, $_d08hr1xsje4cc0sg.wrap(item.title, rest.items));
    var newExpansions = $_dax12xwyje4cc0pt.deepMerge(rest.expansions, $_d08hr1xsje4cc0sg.wrap(item.title, item.title));
    return {
      item: newItem,
      menus: newMenus,
      expansions: newExpansions
    };
  };
  var getFromItem = function (item) {
    return $_d08hr1xsje4cc0sg.hasKey(item, 'items') ? getFromExpandingItem(item) : {
      item: item,
      menus: {},
      expansions: {}
    };
  };
  var expand = function (items) {
    return $_elh0pqwsje4cc0p4.foldr(items, function (acc, item) {
      var newData = getFromItem(item);
      return {
        menus: $_dax12xwyje4cc0pt.deepMerge(acc.menus, newData.menus),
        items: [newData.item].concat(acc.items),
        expansions: $_dax12xwyje4cc0pt.deepMerge(acc.expansions, newData.expansions)
      };
    }, {
      menus: {},
      expansions: {},
      items: []
    });
  };
  var $_1h8pqc13vje4cc1nw = { expand: expand };

  var register$1 = function (editor, settings) {
    var isSelectedFor = function (format) {
      return function () {
        return editor.formatter.match(format);
      };
    };
    var getPreview = function (format) {
      return function () {
        var styles = editor.formatter.getCssText(format);
        return styles;
      };
    };
    var enrichSupported = function (item) {
      return $_dax12xwyje4cc0pt.deepMerge(item, {
        isSelected: isSelectedFor(item.format),
        getPreview: getPreview(item.format)
      });
    };
    var enrichMenu = function (item) {
      return $_dax12xwyje4cc0pt.deepMerge(item, {
        isSelected: $_aso7c6wjje4cc0om.constant(false),
        getPreview: $_aso7c6wjje4cc0om.constant('')
      });
    };
    var enrichCustom = function (item) {
      var formatName = $_eb3s8410rje4cc15l.generate(item.title);
      var newItem = $_dax12xwyje4cc0pt.deepMerge(item, {
        format: formatName,
        isSelected: isSelectedFor(formatName),
        getPreview: getPreview(formatName)
      });
      editor.formatter.register(formatName, newItem);
      return newItem;
    };
    var formats = $_d08hr1xsje4cc0sg.readOptFrom(settings, 'style_formats').getOr(DefaultStyleFormats);
    var doEnrich = function (items) {
      return $_elh0pqwsje4cc0p4.map(items, function (item) {
        if ($_d08hr1xsje4cc0sg.hasKey(item, 'items')) {
          var newItems = doEnrich(item.items);
          return $_dax12xwyje4cc0pt.deepMerge(enrichMenu(item), { items: newItems });
        } else if ($_d08hr1xsje4cc0sg.hasKey(item, 'format')) {
          return enrichSupported(item);
        } else {
          return enrichCustom(item);
        }
      });
    };
    return doEnrich(formats);
  };
  var prune = function (editor, formats) {
    var doPrune = function (items) {
      return $_elh0pqwsje4cc0p4.bind(items, function (item) {
        if (item.items !== undefined) {
          var newItems = doPrune(item.items);
          return newItems.length > 0 ? [item] : [];
        } else {
          var keep = $_d08hr1xsje4cc0sg.hasKey(item, 'format') ? editor.formatter.canApply(item.format) : true;
          return keep ? [item] : [];
        }
      });
    };
    var prunedItems = doPrune(formats);
    return $_1h8pqc13vje4cc1nw.expand(prunedItems);
  };
  var ui = function (editor, formats, onDone) {
    var pruned = prune(editor, formats);
    return $_dlkafq12sje4cc1g5.sketch({
      formats: pruned,
      handle: function (item, value) {
        editor.undoManager.transact(function () {
          if (Toggling.isOn(item)) {
            editor.formatter.remove(value);
          } else {
            editor.formatter.apply(value);
          }
        });
        onDone();
      }
    });
  };
  var $_do553912qje4cc1fw = {
    register: register$1,
    ui: ui
  };

  var defaults = [
    'undo',
    'bold',
    'italic',
    'link',
    'image',
    'bullist',
    'styleselect'
  ];
  var extract$1 = function (rawToolbar) {
    var toolbar = rawToolbar.replace(/\|/g, ' ').trim();
    return toolbar.length > 0 ? toolbar.split(/\s+/) : [];
  };
  var identifyFromArray = function (toolbar) {
    return $_elh0pqwsje4cc0p4.bind(toolbar, function (item) {
      return $_eh3yfywzje4cc0pu.isArray(item) ? identifyFromArray(item) : extract$1(item);
    });
  };
  var identify = function (settings) {
    var toolbar = settings.toolbar !== undefined ? settings.toolbar : defaults;
    return $_eh3yfywzje4cc0pu.isArray(toolbar) ? identifyFromArray(toolbar) : extract$1(toolbar);
  };
  var setup = function (realm, editor) {
    var commandSketch = function (name) {
      return function () {
        return $_afg6uzzfje4cc0zf.forToolbarCommand(editor, name);
      };
    };
    var stateCommandSketch = function (name) {
      return function () {
        return $_afg6uzzfje4cc0zf.forToolbarStateCommand(editor, name);
      };
    };
    var actionSketch = function (name, query, action) {
      return function () {
        return $_afg6uzzfje4cc0zf.forToolbarStateAction(editor, name, query, action);
      };
    };
    var undo = commandSketch('undo');
    var redo = commandSketch('redo');
    var bold = stateCommandSketch('bold');
    var italic = stateCommandSketch('italic');
    var underline = stateCommandSketch('underline');
    var removeformat = commandSketch('removeformat');
    var link = function () {
      return $_c59rbc121je4cc1cf.sketch(realm, editor);
    };
    var unlink = actionSketch('unlink', 'link', function () {
      editor.execCommand('unlink', null, false);
    });
    var image = function () {
      return $_fqw8kp11qje4cc1b7.sketch(editor);
    };
    var bullist = actionSketch('unordered-list', 'ul', function () {
      editor.execCommand('InsertUnorderedList', null, false);
    });
    var numlist = actionSketch('ordered-list', 'ol', function () {
      editor.execCommand('InsertOrderedList', null, false);
    });
    var fontsizeselect = function () {
      return $_6y1sf211mje4cc1al.sketch(realm, editor);
    };
    var forecolor = function () {
      return $_bj7zmp115je4cc18h.sketch(realm, editor);
    };
    var styleFormats = $_do553912qje4cc1fw.register(editor, editor.settings);
    var styleFormatsMenu = function () {
      return $_do553912qje4cc1fw.ui(editor, styleFormats, function () {
        editor.fire('scrollIntoView');
      });
    };
    var styleselect = function () {
      return $_afg6uzzfje4cc0zf.forToolbar('style-formats', function (button) {
        editor.fire('toReading');
        realm.dropup().appear(styleFormatsMenu, Toggling.on, button);
      }, $_lsmliy2je4cc0te.derive([
        Toggling.config({
          toggleClass: $_gfh4lpzeje4cc0zd.resolve('toolbar-button-selected'),
          toggleOnExecute: false,
          aria: { mode: 'pressed' }
        }),
        Receiving.config({
          channels: $_d08hr1xsje4cc0sg.wrapAll([
            $_g6oxx7zdje4cc0za.receive($_2txnauz1je4cc0xi.orientationChanged(), Toggling.off),
            $_g6oxx7zdje4cc0za.receive($_2txnauz1je4cc0xi.dropupDismissed(), Toggling.off)
          ])
        })
      ]));
    };
    var feature = function (prereq, sketch) {
      return {
        isSupported: function () {
          return prereq.forall(function (p) {
            return $_d08hr1xsje4cc0sg.hasKey(editor.buttons, p);
          });
        },
        sketch: sketch
      };
    };
    return {
      undo: feature(Option.none(), undo),
      redo: feature(Option.none(), redo),
      bold: feature(Option.none(), bold),
      italic: feature(Option.none(), italic),
      underline: feature(Option.none(), underline),
      removeformat: feature(Option.none(), removeformat),
      link: feature(Option.none(), link),
      unlink: feature(Option.none(), unlink),
      image: feature(Option.none(), image),
      bullist: feature(Option.some('bullist'), bullist),
      numlist: feature(Option.some('numlist'), numlist),
      fontsizeselect: feature(Option.none(), fontsizeselect),
      forecolor: feature(Option.none(), forecolor),
      styleselect: feature(Option.none(), styleselect)
    };
  };
  var detect$4 = function (settings, features) {
    var itemNames = identify(settings);
    var present = {};
    return $_elh0pqwsje4cc0p4.bind(itemNames, function (iName) {
      var r = !$_d08hr1xsje4cc0sg.hasKey(present, iName) && $_d08hr1xsje4cc0sg.hasKey(features, iName) && features[iName].isSupported() ? [features[iName].sketch()] : [];
      present[iName] = true;
      return r;
    });
  };
  var $_1zl9q0z2je4cc0xk = {
    identify: identify,
    setup: setup,
    detect: detect$4
  };

  var mkEvent = function (target, x, y, stop, prevent, kill, raw) {
    return {
      'target': $_aso7c6wjje4cc0om.constant(target),
      'x': $_aso7c6wjje4cc0om.constant(x),
      'y': $_aso7c6wjje4cc0om.constant(y),
      'stop': stop,
      'prevent': prevent,
      'kill': kill,
      'raw': $_aso7c6wjje4cc0om.constant(raw)
    };
  };
  var handle = function (filter, handler) {
    return function (rawEvent) {
      if (!filter(rawEvent))
        return;
      var target = $_407ejqxfje4cc0rb.fromDom(rawEvent.target);
      var stop = function () {
        rawEvent.stopPropagation();
      };
      var prevent = function () {
        rawEvent.preventDefault();
      };
      var kill = $_aso7c6wjje4cc0om.compose(prevent, stop);
      var evt = mkEvent(target, rawEvent.clientX, rawEvent.clientY, stop, prevent, kill, rawEvent);
      handler(evt);
    };
  };
  var binder = function (element, event, filter, handler, useCapture) {
    var wrapped = handle(filter, handler);
    element.dom().addEventListener(event, wrapped, useCapture);
    return { unbind: $_aso7c6wjje4cc0om.curry(unbind, element, event, wrapped, useCapture) };
  };
  var bind$1 = function (element, event, filter, handler) {
    return binder(element, event, filter, handler, false);
  };
  var capture = function (element, event, filter, handler) {
    return binder(element, event, filter, handler, true);
  };
  var unbind = function (element, event, handler, useCapture) {
    element.dom().removeEventListener(event, handler, useCapture);
  };
  var $_8qi5w213yje4cc1o8 = {
    bind: bind$1,
    capture: capture
  };

  var filter$1 = $_aso7c6wjje4cc0om.constant(true);
  var bind$2 = function (element, event, handler) {
    return $_8qi5w213yje4cc1o8.bind(element, event, filter$1, handler);
  };
  var capture$1 = function (element, event, handler) {
    return $_8qi5w213yje4cc1o8.capture(element, event, filter$1, handler);
  };
  var $_24b4y113xje4cc1o6 = {
    bind: bind$2,
    capture: capture$1
  };

  var INTERVAL = 50;
  var INSURANCE = 1000 / INTERVAL;
  var get$11 = function (outerWindow) {
    var isPortrait = outerWindow.matchMedia('(orientation: portrait)').matches;
    return { isPortrait: $_aso7c6wjje4cc0om.constant(isPortrait) };
  };
  var getActualWidth = function (outerWindow) {
    var isIos = $_2j4x3gwkje4cc0oo.detect().os.isiOS();
    var isPortrait = get$11(outerWindow).isPortrait();
    return isIos && !isPortrait ? outerWindow.screen.height : outerWindow.screen.width;
  };
  var onChange = function (outerWindow, listeners) {
    var win = $_407ejqxfje4cc0rb.fromDom(outerWindow);
    var poller = null;
    var change = function () {
      clearInterval(poller);
      var orientation = get$11(outerWindow);
      listeners.onChange(orientation);
      onAdjustment(function () {
        listeners.onReady(orientation);
      });
    };
    var orientationHandle = $_24b4y113xje4cc1o6.bind(win, 'orientationchange', change);
    var onAdjustment = function (f) {
      clearInterval(poller);
      var flag = outerWindow.innerHeight;
      var insurance = 0;
      poller = setInterval(function () {
        if (flag !== outerWindow.innerHeight) {
          clearInterval(poller);
          f(Option.some(outerWindow.innerHeight));
        } else if (insurance > INSURANCE) {
          clearInterval(poller);
          f(Option.none());
        }
        insurance++;
      }, INTERVAL);
    };
    var destroy = function () {
      orientationHandle.unbind();
    };
    return {
      onAdjustment: onAdjustment,
      destroy: destroy
    };
  };
  var $_9hryc13wje4cc1o0 = {
    get: get$11,
    onChange: onChange,
    getActualWidth: getActualWidth
  };

  function DelayedFunction (fun, delay) {
    var ref = null;
    var schedule = function () {
      var args = arguments;
      ref = setTimeout(function () {
        fun.apply(null, args);
        ref = null;
      }, delay);
    };
    var cancel = function () {
      if (ref !== null) {
        clearTimeout(ref);
        ref = null;
      }
    };
    return {
      cancel: cancel,
      schedule: schedule
    };
  }

  var SIGNIFICANT_MOVE = 5;
  var LONGPRESS_DELAY = 400;
  var getTouch = function (event) {
    if (event.raw().touches === undefined || event.raw().touches.length !== 1)
      return Option.none();
    return Option.some(event.raw().touches[0]);
  };
  var isFarEnough = function (touch, data) {
    var distX = Math.abs(touch.clientX - data.x());
    var distY = Math.abs(touch.clientY - data.y());
    return distX > SIGNIFICANT_MOVE || distY > SIGNIFICANT_MOVE;
  };
  var monitor = function (settings) {
    var startData = Cell(Option.none());
    var longpress = DelayedFunction(function (event) {
      startData.set(Option.none());
      settings.triggerEvent($_g6tooswhje4cc0of.longpress(), event);
    }, LONGPRESS_DELAY);
    var handleTouchstart = function (event) {
      getTouch(event).each(function (touch) {
        longpress.cancel();
        var data = {
          x: $_aso7c6wjje4cc0om.constant(touch.clientX),
          y: $_aso7c6wjje4cc0om.constant(touch.clientY),
          target: event.target
        };
        longpress.schedule(data);
        startData.set(Option.some(data));
      });
      return Option.none();
    };
    var handleTouchmove = function (event) {
      longpress.cancel();
      getTouch(event).each(function (touch) {
        startData.get().each(function (data) {
          if (isFarEnough(touch, data))
            startData.set(Option.none());
        });
      });
      return Option.none();
    };
    var handleTouchend = function (event) {
      longpress.cancel();
      var isSame = function (data) {
        return $_1stme4x9je4cc0qq.eq(data.target(), event.target());
      };
      return startData.get().filter(isSame).map(function (data) {
        return settings.triggerEvent($_g6tooswhje4cc0of.tap(), event);
      });
    };
    var handlers = $_d08hr1xsje4cc0sg.wrapAll([
      {
        key: $_g7q1k3wije4cc0oi.touchstart(),
        value: handleTouchstart
      },
      {
        key: $_g7q1k3wije4cc0oi.touchmove(),
        value: handleTouchmove
      },
      {
        key: $_g7q1k3wije4cc0oi.touchend(),
        value: handleTouchend
      }
    ]);
    var fireIfReady = function (event, type) {
      return $_d08hr1xsje4cc0sg.readOptFrom(handlers, type).bind(function (handler) {
        return handler(event);
      });
    };
    return { fireIfReady: fireIfReady };
  };
  var $_7io6r5144je4cc1pa = { monitor: monitor };

  var monitor$1 = function (editorApi) {
    var tapEvent = $_7io6r5144je4cc1pa.monitor({
      triggerEvent: function (type, evt) {
        editorApi.onTapContent(evt);
      }
    });
    var onTouchend = function () {
      return $_24b4y113xje4cc1o6.bind(editorApi.body(), 'touchend', function (evt) {
        tapEvent.fireIfReady(evt, 'touchend');
      });
    };
    var onTouchmove = function () {
      return $_24b4y113xje4cc1o6.bind(editorApi.body(), 'touchmove', function (evt) {
        tapEvent.fireIfReady(evt, 'touchmove');
      });
    };
    var fireTouchstart = function (evt) {
      tapEvent.fireIfReady(evt, 'touchstart');
    };
    return {
      fireTouchstart: fireTouchstart,
      onTouchend: onTouchend,
      onTouchmove: onTouchmove
    };
  };
  var $_67oir1143je4cc1p7 = { monitor: monitor$1 };

  var isAndroid6 = $_2j4x3gwkje4cc0oo.detect().os.version.major >= 6;
  var initEvents = function (editorApi, toolstrip, alloy) {
    var tapping = $_67oir1143je4cc1p7.monitor(editorApi);
    var outerDoc = $_1jwy92x3je4cc0qa.owner(toolstrip);
    var isRanged = function (sel) {
      return !$_1stme4x9je4cc0qq.eq(sel.start(), sel.finish()) || sel.soffset() !== sel.foffset();
    };
    var hasRangeInUi = function () {
      return $_4hvdjzytje4cc0wz.active(outerDoc).filter(function (input) {
        return $_7qwxg2xkje4cc0ro.name(input) === 'input';
      }).exists(function (input) {
        return input.dom().selectionStart !== input.dom().selectionEnd;
      });
    };
    var updateMargin = function () {
      var rangeInContent = editorApi.doc().dom().hasFocus() && editorApi.getSelection().exists(isRanged);
      alloy.getByDom(toolstrip).each((rangeInContent || hasRangeInUi()) === true ? Toggling.on : Toggling.off);
    };
    var listeners = [
      $_24b4y113xje4cc1o6.bind(editorApi.body(), 'touchstart', function (evt) {
        editorApi.onTouchContent();
        tapping.fireTouchstart(evt);
      }),
      tapping.onTouchmove(),
      tapping.onTouchend(),
      $_24b4y113xje4cc1o6.bind(toolstrip, 'touchstart', function (evt) {
        editorApi.onTouchToolstrip();
      }),
      editorApi.onToReading(function () {
        $_4hvdjzytje4cc0wz.blur(editorApi.body());
      }),
      editorApi.onToEditing($_aso7c6wjje4cc0om.noop),
      editorApi.onScrollToCursor(function (tinyEvent) {
        tinyEvent.preventDefault();
        editorApi.getCursorBox().each(function (bounds) {
          var cWin = editorApi.win();
          var isOutside = bounds.top() > cWin.innerHeight || bounds.bottom() > cWin.innerHeight;
          var cScrollBy = isOutside ? bounds.bottom() - cWin.innerHeight + 50 : 0;
          if (cScrollBy !== 0) {
            cWin.scrollTo(cWin.pageXOffset, cWin.pageYOffset + cScrollBy);
          }
        });
      })
    ].concat(isAndroid6 === true ? [] : [
      $_24b4y113xje4cc1o6.bind($_407ejqxfje4cc0rb.fromDom(editorApi.win()), 'blur', function () {
        alloy.getByDom(toolstrip).each(Toggling.off);
      }),
      $_24b4y113xje4cc1o6.bind(outerDoc, 'select', updateMargin),
      $_24b4y113xje4cc1o6.bind(editorApi.doc(), 'selectionchange', updateMargin)
    ]);
    var destroy = function () {
      $_elh0pqwsje4cc0p4.each(listeners, function (l) {
        l.unbind();
      });
    };
    return { destroy: destroy };
  };
  var $_871jst142je4cc1ow = { initEvents: initEvents };

  var safeParse = function (element, attribute) {
    var parsed = parseInt($_bjjq6ixrje4cc0s9.get(element, attribute), 10);
    return isNaN(parsed) ? 0 : parsed;
  };
  var $_1cxrdw147je4cc1pz = { safeParse: safeParse };

  function NodeValue (is, name) {
    var get = function (element) {
      if (!is(element))
        throw new Error('Can only get ' + name + ' value of a ' + name + ' node');
      return getOption(element).getOr('');
    };
    var getOptionIE10 = function (element) {
      try {
        return getOptionSafe(element);
      } catch (e) {
        return Option.none();
      }
    };
    var getOptionSafe = function (element) {
      return is(element) ? Option.from(element.dom().nodeValue) : Option.none();
    };
    var browser = $_2j4x3gwkje4cc0oo.detect().browser;
    var getOption = browser.isIE() && browser.version.major === 10 ? getOptionIE10 : getOptionSafe;
    var set = function (element, value) {
      if (!is(element))
        throw new Error('Can only set raw ' + name + ' value of a ' + name + ' node');
      element.dom().nodeValue = value;
    };
    return {
      get: get,
      getOption: getOption,
      set: set
    };
  }

  var api$3 = NodeValue($_7qwxg2xkje4cc0ro.isText, 'text');
  var get$12 = function (element) {
    return api$3.get(element);
  };
  var getOption = function (element) {
    return api$3.getOption(element);
  };
  var set$8 = function (element, value) {
    api$3.set(element, value);
  };
  var $_6wdxlj14aje4cc1qb = {
    get: get$12,
    getOption: getOption,
    set: set$8
  };

  var getEnd = function (element) {
    return $_7qwxg2xkje4cc0ro.name(element) === 'img' ? 1 : $_6wdxlj14aje4cc1qb.getOption(element).fold(function () {
      return $_1jwy92x3je4cc0qa.children(element).length;
    }, function (v) {
      return v.length;
    });
  };
  var isEnd = function (element, offset) {
    return getEnd(element) === offset;
  };
  var isStart = function (element, offset) {
    return offset === 0;
  };
  var NBSP = '\xA0';
  var isTextNodeWithCursorPosition = function (el) {
    return $_6wdxlj14aje4cc1qb.getOption(el).filter(function (text) {
      return text.trim().length !== 0 || text.indexOf(NBSP) > -1;
    }).isSome();
  };
  var elementsWithCursorPosition = [
    'img',
    'br'
  ];
  var isCursorPosition = function (elem) {
    var hasCursorPosition = isTextNodeWithCursorPosition(elem);
    return hasCursorPosition || $_elh0pqwsje4cc0p4.contains(elementsWithCursorPosition, $_7qwxg2xkje4cc0ro.name(elem));
  };
  var $_fyw8ur149je4cc1q9 = {
    getEnd: getEnd,
    isEnd: isEnd,
    isStart: isStart,
    isCursorPosition: isCursorPosition
  };

  var adt$4 = $_170ozvxwje4cc0st.generate([
    { 'before': ['element'] },
    {
      'on': [
        'element',
        'offset'
      ]
    },
    { after: ['element'] }
  ]);
  var cata = function (subject, onBefore, onOn, onAfter) {
    return subject.fold(onBefore, onOn, onAfter);
  };
  var getStart = function (situ) {
    return situ.fold($_aso7c6wjje4cc0om.identity, $_aso7c6wjje4cc0om.identity, $_aso7c6wjje4cc0om.identity);
  };
  var $_cx2i2a14dje4cc1ql = {
    before: adt$4.before,
    on: adt$4.on,
    after: adt$4.after,
    cata: cata,
    getStart: getStart
  };

  var type$1 = $_170ozvxwje4cc0st.generate([
    { domRange: ['rng'] },
    {
      relative: [
        'startSitu',
        'finishSitu'
      ]
    },
    {
      exact: [
        'start',
        'soffset',
        'finish',
        'foffset'
      ]
    }
  ]);
  var range$1 = $_ajn35gx4je4cc0qj.immutable('start', 'soffset', 'finish', 'foffset');
  var exactFromRange = function (simRange) {
    return type$1.exact(simRange.start(), simRange.soffset(), simRange.finish(), simRange.foffset());
  };
  var getStart$1 = function (selection) {
    return selection.match({
      domRange: function (rng) {
        return $_407ejqxfje4cc0rb.fromDom(rng.startContainer);
      },
      relative: function (startSitu, finishSitu) {
        return $_cx2i2a14dje4cc1ql.getStart(startSitu);
      },
      exact: function (start, soffset, finish, foffset) {
        return start;
      }
    });
  };
  var getWin = function (selection) {
    var start = getStart$1(selection);
    return $_1jwy92x3je4cc0qa.defaultView(start);
  };
  var $_9ecb4k14cje4cc1qg = {
    domRange: type$1.domRange,
    relative: type$1.relative,
    exact: type$1.exact,
    exactFromRange: exactFromRange,
    range: range$1,
    getWin: getWin
  };

  var makeRange = function (start, soffset, finish, foffset) {
    var doc = $_1jwy92x3je4cc0qa.owner(start);
    var rng = doc.dom().createRange();
    rng.setStart(start.dom(), soffset);
    rng.setEnd(finish.dom(), foffset);
    return rng;
  };
  var commonAncestorContainer = function (start, soffset, finish, foffset) {
    var r = makeRange(start, soffset, finish, foffset);
    return $_407ejqxfje4cc0rb.fromDom(r.commonAncestorContainer);
  };
  var after$2 = function (start, soffset, finish, foffset) {
    var r = makeRange(start, soffset, finish, foffset);
    var same = $_1stme4x9je4cc0qq.eq(start, finish) && soffset === foffset;
    return r.collapsed && !same;
  };
  var $_6yrp1u14fje4cc1qz = {
    after: after$2,
    commonAncestorContainer: commonAncestorContainer
  };

  var fromElements = function (elements, scope) {
    var doc = scope || document;
    var fragment = doc.createDocumentFragment();
    $_elh0pqwsje4cc0p4.each(elements, function (element) {
      fragment.appendChild(element.dom());
    });
    return $_407ejqxfje4cc0rb.fromDom(fragment);
  };
  var $_28k39a14gje4cc1r1 = { fromElements: fromElements };

  var selectNodeContents = function (win, element) {
    var rng = win.document.createRange();
    selectNodeContentsUsing(rng, element);
    return rng;
  };
  var selectNodeContentsUsing = function (rng, element) {
    rng.selectNodeContents(element.dom());
  };
  var isWithin = function (outerRange, innerRange) {
    return innerRange.compareBoundaryPoints(outerRange.END_TO_START, outerRange) < 1 && innerRange.compareBoundaryPoints(outerRange.START_TO_END, outerRange) > -1;
  };
  var create$4 = function (win) {
    return win.document.createRange();
  };
  var setStart = function (rng, situ) {
    situ.fold(function (e) {
      rng.setStartBefore(e.dom());
    }, function (e, o) {
      rng.setStart(e.dom(), o);
    }, function (e) {
      rng.setStartAfter(e.dom());
    });
  };
  var setFinish = function (rng, situ) {
    situ.fold(function (e) {
      rng.setEndBefore(e.dom());
    }, function (e, o) {
      rng.setEnd(e.dom(), o);
    }, function (e) {
      rng.setEndAfter(e.dom());
    });
  };
  var replaceWith = function (rng, fragment) {
    deleteContents(rng);
    rng.insertNode(fragment.dom());
  };
  var relativeToNative = function (win, startSitu, finishSitu) {
    var range = win.document.createRange();
    setStart(range, startSitu);
    setFinish(range, finishSitu);
    return range;
  };
  var exactToNative = function (win, start, soffset, finish, foffset) {
    var rng = win.document.createRange();
    rng.setStart(start.dom(), soffset);
    rng.setEnd(finish.dom(), foffset);
    return rng;
  };
  var deleteContents = function (rng) {
    rng.deleteContents();
  };
  var cloneFragment = function (rng) {
    var fragment = rng.cloneContents();
    return $_407ejqxfje4cc0rb.fromDom(fragment);
  };
  var toRect = function (rect) {
    return {
      left: $_aso7c6wjje4cc0om.constant(rect.left),
      top: $_aso7c6wjje4cc0om.constant(rect.top),
      right: $_aso7c6wjje4cc0om.constant(rect.right),
      bottom: $_aso7c6wjje4cc0om.constant(rect.bottom),
      width: $_aso7c6wjje4cc0om.constant(rect.width),
      height: $_aso7c6wjje4cc0om.constant(rect.height)
    };
  };
  var getFirstRect = function (rng) {
    var rects = rng.getClientRects();
    var rect = rects.length > 0 ? rects[0] : rng.getBoundingClientRect();
    return rect.width > 0 || rect.height > 0 ? Option.some(rect).map(toRect) : Option.none();
  };
  var getBounds = function (rng) {
    var rect = rng.getBoundingClientRect();
    return rect.width > 0 || rect.height > 0 ? Option.some(rect).map(toRect) : Option.none();
  };
  var toString$1 = function (rng) {
    return rng.toString();
  };
  var $_8st22b14hje4cc1r4 = {
    create: create$4,
    replaceWith: replaceWith,
    selectNodeContents: selectNodeContents,
    selectNodeContentsUsing: selectNodeContentsUsing,
    relativeToNative: relativeToNative,
    exactToNative: exactToNative,
    deleteContents: deleteContents,
    cloneFragment: cloneFragment,
    getFirstRect: getFirstRect,
    getBounds: getBounds,
    isWithin: isWithin,
    toString: toString$1
  };

  var adt$5 = $_170ozvxwje4cc0st.generate([
    {
      ltr: [
        'start',
        'soffset',
        'finish',
        'foffset'
      ]
    },
    {
      rtl: [
        'start',
        'soffset',
        'finish',
        'foffset'
      ]
    }
  ]);
  var fromRange = function (win, type, range) {
    return type($_407ejqxfje4cc0rb.fromDom(range.startContainer), range.startOffset, $_407ejqxfje4cc0rb.fromDom(range.endContainer), range.endOffset);
  };
  var getRanges = function (win, selection) {
    return selection.match({
      domRange: function (rng) {
        return {
          ltr: $_aso7c6wjje4cc0om.constant(rng),
          rtl: Option.none
        };
      },
      relative: function (startSitu, finishSitu) {
        return {
          ltr: $_b57rigwlje4cc0oq.cached(function () {
            return $_8st22b14hje4cc1r4.relativeToNative(win, startSitu, finishSitu);
          }),
          rtl: $_b57rigwlje4cc0oq.cached(function () {
            return Option.some($_8st22b14hje4cc1r4.relativeToNative(win, finishSitu, startSitu));
          })
        };
      },
      exact: function (start, soffset, finish, foffset) {
        return {
          ltr: $_b57rigwlje4cc0oq.cached(function () {
            return $_8st22b14hje4cc1r4.exactToNative(win, start, soffset, finish, foffset);
          }),
          rtl: $_b57rigwlje4cc0oq.cached(function () {
            return Option.some($_8st22b14hje4cc1r4.exactToNative(win, finish, foffset, start, soffset));
          })
        };
      }
    });
  };
  var doDiagnose = function (win, ranges) {
    var rng = ranges.ltr();
    if (rng.collapsed) {
      var reversed = ranges.rtl().filter(function (rev) {
        return rev.collapsed === false;
      });
      return reversed.map(function (rev) {
        return adt$5.rtl($_407ejqxfje4cc0rb.fromDom(rev.endContainer), rev.endOffset, $_407ejqxfje4cc0rb.fromDom(rev.startContainer), rev.startOffset);
      }).getOrThunk(function () {
        return fromRange(win, adt$5.ltr, rng);
      });
    } else {
      return fromRange(win, adt$5.ltr, rng);
    }
  };
  var diagnose = function (win, selection) {
    var ranges = getRanges(win, selection);
    return doDiagnose(win, ranges);
  };
  var asLtrRange = function (win, selection) {
    var diagnosis = diagnose(win, selection);
    return diagnosis.match({
      ltr: function (start, soffset, finish, foffset) {
        var rng = win.document.createRange();
        rng.setStart(start.dom(), soffset);
        rng.setEnd(finish.dom(), foffset);
        return rng;
      },
      rtl: function (start, soffset, finish, foffset) {
        var rng = win.document.createRange();
        rng.setStart(finish.dom(), foffset);
        rng.setEnd(start.dom(), soffset);
        return rng;
      }
    });
  };
  var $_aae7lc14ije4cc1r9 = {
    ltr: adt$5.ltr,
    rtl: adt$5.rtl,
    diagnose: diagnose,
    asLtrRange: asLtrRange
  };

  var searchForPoint = function (rectForOffset, x, y, maxX, length) {
    if (length === 0)
      return 0;
    else if (x === maxX)
      return length - 1;
    var xDelta = maxX;
    for (var i = 1; i < length; i++) {
      var rect = rectForOffset(i);
      var curDeltaX = Math.abs(x - rect.left);
      if (y > rect.bottom) {
      } else if (y < rect.top || curDeltaX > xDelta) {
        return i - 1;
      } else {
        xDelta = curDeltaX;
      }
    }
    return 0;
  };
  var inRect = function (rect, x, y) {
    return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
  };
  var $_u5yug14lje4cc1ro = {
    inRect: inRect,
    searchForPoint: searchForPoint
  };

  var locateOffset = function (doc, textnode, x, y, rect) {
    var rangeForOffset = function (offset) {
      var r = doc.dom().createRange();
      r.setStart(textnode.dom(), offset);
      r.collapse(true);
      return r;
    };
    var rectForOffset = function (offset) {
      var r = rangeForOffset(offset);
      return r.getBoundingClientRect();
    };
    var length = $_6wdxlj14aje4cc1qb.get(textnode).length;
    var offset = $_u5yug14lje4cc1ro.searchForPoint(rectForOffset, x, y, rect.right, length);
    return rangeForOffset(offset);
  };
  var locate$1 = function (doc, node, x, y) {
    var r = doc.dom().createRange();
    r.selectNode(node.dom());
    var rects = r.getClientRects();
    var foundRect = $_egl8u6y0je4cc0tb.findMap(rects, function (rect) {
      return $_u5yug14lje4cc1ro.inRect(rect, x, y) ? Option.some(rect) : Option.none();
    });
    return foundRect.map(function (rect) {
      return locateOffset(doc, node, x, y, rect);
    });
  };
  var $_eyvyvq14mje4cc1rp = { locate: locate$1 };

  var searchInChildren = function (doc, node, x, y) {
    var r = doc.dom().createRange();
    var nodes = $_1jwy92x3je4cc0qa.children(node);
    return $_egl8u6y0je4cc0tb.findMap(nodes, function (n) {
      r.selectNode(n.dom());
      return $_u5yug14lje4cc1ro.inRect(r.getBoundingClientRect(), x, y) ? locateNode(doc, n, x, y) : Option.none();
    });
  };
  var locateNode = function (doc, node, x, y) {
    var locator = $_7qwxg2xkje4cc0ro.isText(node) ? $_eyvyvq14mje4cc1rp.locate : searchInChildren;
    return locator(doc, node, x, y);
  };
  var locate$2 = function (doc, node, x, y) {
    var r = doc.dom().createRange();
    r.selectNode(node.dom());
    var rect = r.getBoundingClientRect();
    var boundedX = Math.max(rect.left, Math.min(rect.right, x));
    var boundedY = Math.max(rect.top, Math.min(rect.bottom, y));
    return locateNode(doc, node, boundedX, boundedY);
  };
  var $_13obzg14kje4cc1rk = { locate: locate$2 };

  var first$3 = function (element) {
    return $_40elmhyvje4cc0x6.descendant(element, $_fyw8ur149je4cc1q9.isCursorPosition);
  };
  var last$2 = function (element) {
    return descendantRtl(element, $_fyw8ur149je4cc1q9.isCursorPosition);
  };
  var descendantRtl = function (scope, predicate) {
    var descend = function (element) {
      var children = $_1jwy92x3je4cc0qa.children(element);
      for (var i = children.length - 1; i >= 0; i--) {
        var child = children[i];
        if (predicate(child))
          return Option.some(child);
        var res = descend(child);
        if (res.isSome())
          return res;
      }
      return Option.none();
    };
    return descend(scope);
  };
  var $_br4nja14oje4cc1rw = {
    first: first$3,
    last: last$2
  };

  var COLLAPSE_TO_LEFT = true;
  var COLLAPSE_TO_RIGHT = false;
  var getCollapseDirection = function (rect, x) {
    return x - rect.left < rect.right - x ? COLLAPSE_TO_LEFT : COLLAPSE_TO_RIGHT;
  };
  var createCollapsedNode = function (doc, target, collapseDirection) {
    var r = doc.dom().createRange();
    r.selectNode(target.dom());
    r.collapse(collapseDirection);
    return r;
  };
  var locateInElement = function (doc, node, x) {
    var cursorRange = doc.dom().createRange();
    cursorRange.selectNode(node.dom());
    var rect = cursorRange.getBoundingClientRect();
    var collapseDirection = getCollapseDirection(rect, x);
    var f = collapseDirection === COLLAPSE_TO_LEFT ? $_br4nja14oje4cc1rw.first : $_br4nja14oje4cc1rw.last;
    return f(node).map(function (target) {
      return createCollapsedNode(doc, target, collapseDirection);
    });
  };
  var locateInEmpty = function (doc, node, x) {
    var rect = node.dom().getBoundingClientRect();
    var collapseDirection = getCollapseDirection(rect, x);
    return Option.some(createCollapsedNode(doc, node, collapseDirection));
  };
  var search$1 = function (doc, node, x) {
    var f = $_1jwy92x3je4cc0qa.children(node).length === 0 ? locateInEmpty : locateInElement;
    return f(doc, node, x);
  };
  var $_bk90wj14nje4cc1rt = { search: search$1 };

  var caretPositionFromPoint = function (doc, x, y) {
    return Option.from(doc.dom().caretPositionFromPoint(x, y)).bind(function (pos) {
      if (pos.offsetNode === null)
        return Option.none();
      var r = doc.dom().createRange();
      r.setStart(pos.offsetNode, pos.offset);
      r.collapse();
      return Option.some(r);
    });
  };
  var caretRangeFromPoint = function (doc, x, y) {
    return Option.from(doc.dom().caretRangeFromPoint(x, y));
  };
  var searchTextNodes = function (doc, node, x, y) {
    var r = doc.dom().createRange();
    r.selectNode(node.dom());
    var rect = r.getBoundingClientRect();
    var boundedX = Math.max(rect.left, Math.min(rect.right, x));
    var boundedY = Math.max(rect.top, Math.min(rect.bottom, y));
    return $_13obzg14kje4cc1rk.locate(doc, node, boundedX, boundedY);
  };
  var searchFromPoint = function (doc, x, y) {
    return $_407ejqxfje4cc0rb.fromPoint(doc, x, y).bind(function (elem) {
      var fallback = function () {
        return $_bk90wj14nje4cc1rt.search(doc, elem, x);
      };
      return $_1jwy92x3je4cc0qa.children(elem).length === 0 ? fallback() : searchTextNodes(doc, elem, x, y).orThunk(fallback);
    });
  };
  var availableSearch = document.caretPositionFromPoint ? caretPositionFromPoint : document.caretRangeFromPoint ? caretRangeFromPoint : searchFromPoint;
  var fromPoint$1 = function (win, x, y) {
    var doc = $_407ejqxfje4cc0rb.fromDom(win.document);
    return availableSearch(doc, x, y).map(function (rng) {
      return $_9ecb4k14cje4cc1qg.range($_407ejqxfje4cc0rb.fromDom(rng.startContainer), rng.startOffset, $_407ejqxfje4cc0rb.fromDom(rng.endContainer), rng.endOffset);
    });
  };
  var $_9sdnuy14jje4cc1rh = { fromPoint: fromPoint$1 };

  var withinContainer = function (win, ancestor, outerRange, selector) {
    var innerRange = $_8st22b14hje4cc1r4.create(win);
    var self = $_ab1wd9xeje4cc0r0.is(ancestor, selector) ? [ancestor] : [];
    var elements = self.concat($_1n1hkzzvje4cc119.descendants(ancestor, selector));
    return $_elh0pqwsje4cc0p4.filter(elements, function (elem) {
      $_8st22b14hje4cc1r4.selectNodeContentsUsing(innerRange, elem);
      return $_8st22b14hje4cc1r4.isWithin(outerRange, innerRange);
    });
  };
  var find$4 = function (win, selection, selector) {
    var outerRange = $_aae7lc14ije4cc1r9.asLtrRange(win, selection);
    var ancestor = $_407ejqxfje4cc0rb.fromDom(outerRange.commonAncestorContainer);
    return $_7qwxg2xkje4cc0ro.isElement(ancestor) ? withinContainer(win, ancestor, outerRange, selector) : [];
  };
  var $_56jurj14pje4cc1rz = { find: find$4 };

  var beforeSpecial = function (element, offset) {
    var name = $_7qwxg2xkje4cc0ro.name(element);
    if ('input' === name)
      return $_cx2i2a14dje4cc1ql.after(element);
    else if (!$_elh0pqwsje4cc0p4.contains([
        'br',
        'img'
      ], name))
      return $_cx2i2a14dje4cc1ql.on(element, offset);
    else
      return offset === 0 ? $_cx2i2a14dje4cc1ql.before(element) : $_cx2i2a14dje4cc1ql.after(element);
  };
  var preprocessRelative = function (startSitu, finishSitu) {
    var start = startSitu.fold($_cx2i2a14dje4cc1ql.before, beforeSpecial, $_cx2i2a14dje4cc1ql.after);
    var finish = finishSitu.fold($_cx2i2a14dje4cc1ql.before, beforeSpecial, $_cx2i2a14dje4cc1ql.after);
    return $_9ecb4k14cje4cc1qg.relative(start, finish);
  };
  var preprocessExact = function (start, soffset, finish, foffset) {
    var startSitu = beforeSpecial(start, soffset);
    var finishSitu = beforeSpecial(finish, foffset);
    return $_9ecb4k14cje4cc1qg.relative(startSitu, finishSitu);
  };
  var preprocess = function (selection) {
    return selection.match({
      domRange: function (rng) {
        var start = $_407ejqxfje4cc0rb.fromDom(rng.startContainer);
        var finish = $_407ejqxfje4cc0rb.fromDom(rng.endContainer);
        return preprocessExact(start, rng.startOffset, finish, rng.endOffset);
      },
      relative: preprocessRelative,
      exact: preprocessExact
    });
  };
  var $_3j4xf414qje4cc1s2 = {
    beforeSpecial: beforeSpecial,
    preprocess: preprocess,
    preprocessRelative: preprocessRelative,
    preprocessExact: preprocessExact
  };

  var doSetNativeRange = function (win, rng) {
    Option.from(win.getSelection()).each(function (selection) {
      selection.removeAllRanges();
      selection.addRange(rng);
    });
  };
  var doSetRange = function (win, start, soffset, finish, foffset) {
    var rng = $_8st22b14hje4cc1r4.exactToNative(win, start, soffset, finish, foffset);
    doSetNativeRange(win, rng);
  };
  var findWithin = function (win, selection, selector) {
    return $_56jurj14pje4cc1rz.find(win, selection, selector);
  };
  var setRangeFromRelative = function (win, relative) {
    return $_aae7lc14ije4cc1r9.diagnose(win, relative).match({
      ltr: function (start, soffset, finish, foffset) {
        doSetRange(win, start, soffset, finish, foffset);
      },
      rtl: function (start, soffset, finish, foffset) {
        var selection = win.getSelection();
        if (selection.setBaseAndExtent) {
          selection.setBaseAndExtent(start.dom(), soffset, finish.dom(), foffset);
        } else if (selection.extend) {
          selection.collapse(start.dom(), soffset);
          selection.extend(finish.dom(), foffset);
        } else {
          doSetRange(win, finish, foffset, start, soffset);
        }
      }
    });
  };
  var setExact = function (win, start, soffset, finish, foffset) {
    var relative = $_3j4xf414qje4cc1s2.preprocessExact(start, soffset, finish, foffset);
    setRangeFromRelative(win, relative);
  };
  var setRelative = function (win, startSitu, finishSitu) {
    var relative = $_3j4xf414qje4cc1s2.preprocessRelative(startSitu, finishSitu);
    setRangeFromRelative(win, relative);
  };
  var toNative = function (selection) {
    var win = $_9ecb4k14cje4cc1qg.getWin(selection).dom();
    var getDomRange = function (start, soffset, finish, foffset) {
      return $_8st22b14hje4cc1r4.exactToNative(win, start, soffset, finish, foffset);
    };
    var filtered = $_3j4xf414qje4cc1s2.preprocess(selection);
    return $_aae7lc14ije4cc1r9.diagnose(win, filtered).match({
      ltr: getDomRange,
      rtl: getDomRange
    });
  };
  var readRange = function (selection) {
    if (selection.rangeCount > 0) {
      var firstRng = selection.getRangeAt(0);
      var lastRng = selection.getRangeAt(selection.rangeCount - 1);
      return Option.some($_9ecb4k14cje4cc1qg.range($_407ejqxfje4cc0rb.fromDom(firstRng.startContainer), firstRng.startOffset, $_407ejqxfje4cc0rb.fromDom(lastRng.endContainer), lastRng.endOffset));
    } else {
      return Option.none();
    }
  };
  var doGetExact = function (selection) {
    var anchorNode = $_407ejqxfje4cc0rb.fromDom(selection.anchorNode);
    var focusNode = $_407ejqxfje4cc0rb.fromDom(selection.focusNode);
    return $_6yrp1u14fje4cc1qz.after(anchorNode, selection.anchorOffset, focusNode, selection.focusOffset) ? Option.some($_9ecb4k14cje4cc1qg.range($_407ejqxfje4cc0rb.fromDom(selection.anchorNode), selection.anchorOffset, $_407ejqxfje4cc0rb.fromDom(selection.focusNode), selection.focusOffset)) : readRange(selection);
  };
  var setToElement = function (win, element) {
    var rng = $_8st22b14hje4cc1r4.selectNodeContents(win, element);
    doSetNativeRange(win, rng);
  };
  var forElement = function (win, element) {
    var rng = $_8st22b14hje4cc1r4.selectNodeContents(win, element);
    return $_9ecb4k14cje4cc1qg.range($_407ejqxfje4cc0rb.fromDom(rng.startContainer), rng.startOffset, $_407ejqxfje4cc0rb.fromDom(rng.endContainer), rng.endOffset);
  };
  var getExact = function (win) {
    var selection = win.getSelection();
    return selection.rangeCount > 0 ? doGetExact(selection) : Option.none();
  };
  var get$13 = function (win) {
    return getExact(win).map(function (range) {
      return $_9ecb4k14cje4cc1qg.exact(range.start(), range.soffset(), range.finish(), range.foffset());
    });
  };
  var getFirstRect$1 = function (win, selection) {
    var rng = $_aae7lc14ije4cc1r9.asLtrRange(win, selection);
    return $_8st22b14hje4cc1r4.getFirstRect(rng);
  };
  var getBounds$1 = function (win, selection) {
    var rng = $_aae7lc14ije4cc1r9.asLtrRange(win, selection);
    return $_8st22b14hje4cc1r4.getBounds(rng);
  };
  var getAtPoint = function (win, x, y) {
    return $_9sdnuy14jje4cc1rh.fromPoint(win, x, y);
  };
  var getAsString = function (win, selection) {
    var rng = $_aae7lc14ije4cc1r9.asLtrRange(win, selection);
    return $_8st22b14hje4cc1r4.toString(rng);
  };
  var clear$1 = function (win) {
    var selection = win.getSelection();
    selection.removeAllRanges();
  };
  var clone$3 = function (win, selection) {
    var rng = $_aae7lc14ije4cc1r9.asLtrRange(win, selection);
    return $_8st22b14hje4cc1r4.cloneFragment(rng);
  };
  var replace = function (win, selection, elements) {
    var rng = $_aae7lc14ije4cc1r9.asLtrRange(win, selection);
    var fragment = $_28k39a14gje4cc1r1.fromElements(elements, win.document);
    $_8st22b14hje4cc1r4.replaceWith(rng, fragment);
  };
  var deleteAt = function (win, selection) {
    var rng = $_aae7lc14ije4cc1r9.asLtrRange(win, selection);
    $_8st22b14hje4cc1r4.deleteContents(rng);
  };
  var isCollapsed = function (start, soffset, finish, foffset) {
    return $_1stme4x9je4cc0qq.eq(start, finish) && soffset === foffset;
  };
  var $_9119bq14eje4cc1qp = {
    setExact: setExact,
    getExact: getExact,
    get: get$13,
    setRelative: setRelative,
    toNative: toNative,
    setToElement: setToElement,
    clear: clear$1,
    clone: clone$3,
    replace: replace,
    deleteAt: deleteAt,
    forElement: forElement,
    getFirstRect: getFirstRect$1,
    getBounds: getBounds$1,
    getAtPoint: getAtPoint,
    findWithin: findWithin,
    getAsString: getAsString,
    isCollapsed: isCollapsed
  };

  var COLLAPSED_WIDTH = 2;
  var collapsedRect = function (rect) {
    return {
      left: rect.left,
      top: rect.top,
      right: rect.right,
      bottom: rect.bottom,
      width: $_aso7c6wjje4cc0om.constant(COLLAPSED_WIDTH),
      height: rect.height
    };
  };
  var toRect$1 = function (rawRect) {
    return {
      left: $_aso7c6wjje4cc0om.constant(rawRect.left),
      top: $_aso7c6wjje4cc0om.constant(rawRect.top),
      right: $_aso7c6wjje4cc0om.constant(rawRect.right),
      bottom: $_aso7c6wjje4cc0om.constant(rawRect.bottom),
      width: $_aso7c6wjje4cc0om.constant(rawRect.width),
      height: $_aso7c6wjje4cc0om.constant(rawRect.height)
    };
  };
  var getRectsFromRange = function (range) {
    if (!range.collapsed) {
      return $_elh0pqwsje4cc0p4.map(range.getClientRects(), toRect$1);
    } else {
      var start_1 = $_407ejqxfje4cc0rb.fromDom(range.startContainer);
      return $_1jwy92x3je4cc0qa.parent(start_1).bind(function (parent) {
        var selection = $_9ecb4k14cje4cc1qg.exact(start_1, range.startOffset, parent, $_fyw8ur149je4cc1q9.getEnd(parent));
        var optRect = $_9119bq14eje4cc1qp.getFirstRect(range.startContainer.ownerDocument.defaultView, selection);
        return optRect.map(collapsedRect).map($_elh0pqwsje4cc0p4.pure);
      }).getOr([]);
    }
  };
  var getRectangles = function (cWin) {
    var sel = cWin.getSelection();
    return sel !== undefined && sel.rangeCount > 0 ? getRectsFromRange(sel.getRangeAt(0)) : [];
  };
  var $_c6xmxq148je4cc1q1 = { getRectangles: getRectangles };

  var autocompleteHack = function () {
    return function (f) {
      setTimeout(function () {
        f();
      }, 0);
    };
  };
  var resume = function (cWin) {
    cWin.focus();
    var iBody = $_407ejqxfje4cc0rb.fromDom(cWin.document.body);
    var inInput = $_4hvdjzytje4cc0wz.active().exists(function (elem) {
      return $_elh0pqwsje4cc0p4.contains([
        'input',
        'textarea'
      ], $_7qwxg2xkje4cc0ro.name(elem));
    });
    var transaction = inInput ? autocompleteHack() : $_aso7c6wjje4cc0om.apply;
    transaction(function () {
      $_4hvdjzytje4cc0wz.active().each($_4hvdjzytje4cc0wz.blur);
      $_4hvdjzytje4cc0wz.focus(iBody);
    });
  };
  var $_ctdjxq14rje4cc1s6 = { resume: resume };

  var EXTRA_SPACING = 50;
  var data = 'data-' + $_gfh4lpzeje4cc0zd.resolve('last-outer-height');
  var setLastHeight = function (cBody, value) {
    $_bjjq6ixrje4cc0s9.set(cBody, data, value);
  };
  var getLastHeight = function (cBody) {
    return $_1cxrdw147je4cc1pz.safeParse(cBody, data);
  };
  var getBoundsFrom = function (rect) {
    return {
      top: $_aso7c6wjje4cc0om.constant(rect.top()),
      bottom: $_aso7c6wjje4cc0om.constant(rect.top() + rect.height())
    };
  };
  var getBounds$2 = function (cWin) {
    var rects = $_c6xmxq148je4cc1q1.getRectangles(cWin);
    return rects.length > 0 ? Option.some(rects[0]).map(getBoundsFrom) : Option.none();
  };
  var findDelta = function (outerWindow, cBody) {
    var last = getLastHeight(cBody);
    var current = outerWindow.innerHeight;
    return last > current ? Option.some(last - current) : Option.none();
  };
  var calculate = function (cWin, bounds, delta) {
    var isOutside = bounds.top() > cWin.innerHeight || bounds.bottom() > cWin.innerHeight;
    return isOutside ? Math.min(delta, bounds.bottom() - cWin.innerHeight + EXTRA_SPACING) : 0;
  };
  var setup$1 = function (outerWindow, cWin) {
    var cBody = $_407ejqxfje4cc0rb.fromDom(cWin.document.body);
    var toEditing = function () {
      $_ctdjxq14rje4cc1s6.resume(cWin);
    };
    var onResize = $_24b4y113xje4cc1o6.bind($_407ejqxfje4cc0rb.fromDom(outerWindow), 'resize', function () {
      findDelta(outerWindow, cBody).each(function (delta) {
        getBounds$2(cWin).each(function (bounds) {
          var cScrollBy = calculate(cWin, bounds, delta);
          if (cScrollBy !== 0) {
            cWin.scrollTo(cWin.pageXOffset, cWin.pageYOffset + cScrollBy);
          }
        });
      });
      setLastHeight(cBody, outerWindow.innerHeight);
    });
    setLastHeight(cBody, outerWindow.innerHeight);
    var destroy = function () {
      onResize.unbind();
    };
    return {
      toEditing: toEditing,
      destroy: destroy
    };
  };
  var $_1t4zgy146je4cc1pk = { setup: setup$1 };

  var getBodyFromFrame = function (frame) {
    return Option.some($_407ejqxfje4cc0rb.fromDom(frame.dom().contentWindow.document.body));
  };
  var getDocFromFrame = function (frame) {
    return Option.some($_407ejqxfje4cc0rb.fromDom(frame.dom().contentWindow.document));
  };
  var getWinFromFrame = function (frame) {
    return Option.from(frame.dom().contentWindow);
  };
  var getSelectionFromFrame = function (frame) {
    var optWin = getWinFromFrame(frame);
    return optWin.bind($_9119bq14eje4cc1qp.getExact);
  };
  var getFrame = function (editor) {
    return editor.getFrame();
  };
  var getOrDerive = function (name, f) {
    return function (editor) {
      var g = editor[name].getOrThunk(function () {
        var frame = getFrame(editor);
        return function () {
          return f(frame);
        };
      });
      return g();
    };
  };
  var getOrListen = function (editor, doc, name, type) {
    return editor[name].getOrThunk(function () {
      return function (handler) {
        return $_24b4y113xje4cc1o6.bind(doc, type, handler);
      };
    });
  };
  var toRect$2 = function (rect) {
    return {
      left: $_aso7c6wjje4cc0om.constant(rect.left),
      top: $_aso7c6wjje4cc0om.constant(rect.top),
      right: $_aso7c6wjje4cc0om.constant(rect.right),
      bottom: $_aso7c6wjje4cc0om.constant(rect.bottom),
      width: $_aso7c6wjje4cc0om.constant(rect.width),
      height: $_aso7c6wjje4cc0om.constant(rect.height)
    };
  };
  var getActiveApi = function (editor) {
    var frame = getFrame(editor);
    var tryFallbackBox = function (win) {
      var isCollapsed = function (sel) {
        return $_1stme4x9je4cc0qq.eq(sel.start(), sel.finish()) && sel.soffset() === sel.foffset();
      };
      var toStartRect = function (sel) {
        var rect = sel.start().dom().getBoundingClientRect();
        return rect.width > 0 || rect.height > 0 ? Option.some(rect).map(toRect$2) : Option.none();
      };
      return $_9119bq14eje4cc1qp.getExact(win).filter(isCollapsed).bind(toStartRect);
    };
    return getBodyFromFrame(frame).bind(function (body) {
      return getDocFromFrame(frame).bind(function (doc) {
        return getWinFromFrame(frame).map(function (win) {
          var html = $_407ejqxfje4cc0rb.fromDom(doc.dom().documentElement);
          var getCursorBox = editor.getCursorBox.getOrThunk(function () {
            return function () {
              return $_9119bq14eje4cc1qp.get(win).bind(function (sel) {
                return $_9119bq14eje4cc1qp.getFirstRect(win, sel).orThunk(function () {
                  return tryFallbackBox(win);
                });
              });
            };
          });
          var setSelection = editor.setSelection.getOrThunk(function () {
            return function (start, soffset, finish, foffset) {
              $_9119bq14eje4cc1qp.setExact(win, start, soffset, finish, foffset);
            };
          });
          var clearSelection = editor.clearSelection.getOrThunk(function () {
            return function () {
              $_9119bq14eje4cc1qp.clear(win);
            };
          });
          return {
            body: $_aso7c6wjje4cc0om.constant(body),
            doc: $_aso7c6wjje4cc0om.constant(doc),
            win: $_aso7c6wjje4cc0om.constant(win),
            html: $_aso7c6wjje4cc0om.constant(html),
            getSelection: $_aso7c6wjje4cc0om.curry(getSelectionFromFrame, frame),
            setSelection: setSelection,
            clearSelection: clearSelection,
            frame: $_aso7c6wjje4cc0om.constant(frame),
            onKeyup: getOrListen(editor, doc, 'onKeyup', 'keyup'),
            onNodeChanged: getOrListen(editor, doc, 'onNodeChanged', 'selectionchange'),
            onDomChanged: editor.onDomChanged,
            onScrollToCursor: editor.onScrollToCursor,
            onScrollToElement: editor.onScrollToElement,
            onToReading: editor.onToReading,
            onToEditing: editor.onToEditing,
            onToolbarScrollStart: editor.onToolbarScrollStart,
            onTouchContent: editor.onTouchContent,
            onTapContent: editor.onTapContent,
            onTouchToolstrip: editor.onTouchToolstrip,
            getCursorBox: getCursorBox
          };
        });
      });
    });
  };
  var $_e05b014sje4cc1sc = {
    getBody: getOrDerive('getBody', getBodyFromFrame),
    getDoc: getOrDerive('getDoc', getDocFromFrame),
    getWin: getOrDerive('getWin', getWinFromFrame),
    getSelection: getOrDerive('getSelection', getSelectionFromFrame),
    getFrame: getFrame,
    getActiveApi: getActiveApi
  };

  var attr = 'data-ephox-mobile-fullscreen-style';
  var siblingStyles = 'display:none!important;';
  var ancestorPosition = 'position:absolute!important;';
  var ancestorStyles = 'top:0!important;left:0!important;margin:0' + '!important;padding:0!important;width:100%!important;';
  var bgFallback = 'background-color:rgb(255,255,255)!important;';
  var isAndroid = $_2j4x3gwkje4cc0oo.detect().os.isAndroid();
  var matchColor = function (editorBody) {
    var color = $_7ojow7103je4cc11s.get(editorBody, 'background-color');
    return color !== undefined && color !== '' ? 'background-color:' + color + '!important' : bgFallback;
  };
  var clobberStyles = function (container, editorBody) {
    var gatherSibilings = function (element) {
      var siblings = $_1n1hkzzvje4cc119.siblings(element, '*');
      return siblings;
    };
    var clobber = function (clobberStyle) {
      return function (element) {
        var styles = $_bjjq6ixrje4cc0s9.get(element, 'style');
        var backup = styles === undefined ? 'no-styles' : styles.trim();
        if (backup === clobberStyle) {
          return;
        } else {
          $_bjjq6ixrje4cc0s9.set(element, attr, backup);
          $_bjjq6ixrje4cc0s9.set(element, 'style', clobberStyle);
        }
      };
    };
    var ancestors = $_1n1hkzzvje4cc119.ancestors(container, '*');
    var siblings = $_elh0pqwsje4cc0p4.bind(ancestors, gatherSibilings);
    var bgColor = matchColor(editorBody);
    $_elh0pqwsje4cc0p4.each(siblings, clobber(siblingStyles));
    $_elh0pqwsje4cc0p4.each(ancestors, clobber(ancestorPosition + ancestorStyles + bgColor));
    var containerStyles = isAndroid === true ? '' : ancestorPosition;
    clobber(containerStyles + ancestorStyles + bgColor)(container);
  };
  var restoreStyles = function () {
    var clobberedEls = $_1n1hkzzvje4cc119.all('[' + attr + ']');
    $_elh0pqwsje4cc0p4.each(clobberedEls, function (element) {
      var restore = $_bjjq6ixrje4cc0s9.get(element, attr);
      if (restore !== 'no-styles') {
        $_bjjq6ixrje4cc0s9.set(element, 'style', restore);
      } else {
        $_bjjq6ixrje4cc0s9.remove(element, 'style');
      }
      $_bjjq6ixrje4cc0s9.remove(element, attr);
    });
  };
  var $_aa7o5s14tje4cc1sk = {
    clobberStyles: clobberStyles,
    restoreStyles: restoreStyles
  };

  var tag = function () {
    var head = $_by9cdqzxje4cc11d.first('head').getOrDie();
    var nu = function () {
      var meta = $_407ejqxfje4cc0rb.fromTag('meta');
      $_bjjq6ixrje4cc0s9.set(meta, 'name', 'viewport');
      $_9zp36ax2je4cc0q8.append(head, meta);
      return meta;
    };
    var element = $_by9cdqzxje4cc11d.first('meta[name="viewport"]').getOrThunk(nu);
    var backup = $_bjjq6ixrje4cc0s9.get(element, 'content');
    var maximize = function () {
      $_bjjq6ixrje4cc0s9.set(element, 'content', 'width=device-width, initial-scale=1.0, user-scalable=no, maximum-scale=1.0');
    };
    var restore = function () {
      if (backup !== undefined && backup !== null && backup.length > 0) {
        $_bjjq6ixrje4cc0s9.set(element, 'content', backup);
      } else {
        $_bjjq6ixrje4cc0s9.set(element, 'content', 'user-scalable=yes');
      }
    };
    return {
      maximize: maximize,
      restore: restore
    };
  };
  var $_6it1w14uje4cc1sx = { tag: tag };

  var create$5 = function (platform, mask) {
    var meta = $_6it1w14uje4cc1sx.tag();
    var androidApi = $_7qzgbo12oje4cc1fr.api();
    var androidEvents = $_7qzgbo12oje4cc1fr.api();
    var enter = function () {
      mask.hide();
      $_3x5q3zynje4cc0wi.add(platform.container, $_gfh4lpzeje4cc0zd.resolve('fullscreen-maximized'));
      $_3x5q3zynje4cc0wi.add(platform.container, $_gfh4lpzeje4cc0zd.resolve('android-maximized'));
      meta.maximize();
      $_3x5q3zynje4cc0wi.add(platform.body, $_gfh4lpzeje4cc0zd.resolve('android-scroll-reload'));
      androidApi.set($_1t4zgy146je4cc1pk.setup(platform.win, $_e05b014sje4cc1sc.getWin(platform.editor).getOrDie('no')));
      $_e05b014sje4cc1sc.getActiveApi(platform.editor).each(function (editorApi) {
        $_aa7o5s14tje4cc1sk.clobberStyles(platform.container, editorApi.body());
        androidEvents.set($_871jst142je4cc1ow.initEvents(editorApi, platform.toolstrip, platform.alloy));
      });
    };
    var exit = function () {
      meta.restore();
      mask.show();
      $_3x5q3zynje4cc0wi.remove(platform.container, $_gfh4lpzeje4cc0zd.resolve('fullscreen-maximized'));
      $_3x5q3zynje4cc0wi.remove(platform.container, $_gfh4lpzeje4cc0zd.resolve('android-maximized'));
      $_aa7o5s14tje4cc1sk.restoreStyles();
      $_3x5q3zynje4cc0wi.remove(platform.body, $_gfh4lpzeje4cc0zd.resolve('android-scroll-reload'));
      androidEvents.clear();
      androidApi.clear();
    };
    return {
      enter: enter,
      exit: exit
    };
  };
  var $_feamks141je4cc1ol = { create: create$5 };

  var adaptable = function (fn, rate) {
    var timer = null;
    var args = null;
    var cancel = function () {
      if (timer !== null) {
        clearTimeout(timer);
        timer = null;
        args = null;
      }
    };
    var throttle = function () {
      args = arguments;
      if (timer === null) {
        timer = setTimeout(function () {
          fn.apply(null, args);
          timer = null;
          args = null;
        }, rate);
      }
    };
    return {
      cancel: cancel,
      throttle: throttle
    };
  };
  var first$4 = function (fn, rate) {
    var timer = null;
    var cancel = function () {
      if (timer !== null) {
        clearTimeout(timer);
        timer = null;
      }
    };
    var throttle = function () {
      var args = arguments;
      if (timer === null) {
        timer = setTimeout(function () {
          fn.apply(null, args);
          timer = null;
          args = null;
        }, rate);
      }
    };
    return {
      cancel: cancel,
      throttle: throttle
    };
  };
  var last$3 = function (fn, rate) {
    var timer = null;
    var cancel = function () {
      if (timer !== null) {
        clearTimeout(timer);
        timer = null;
      }
    };
    var throttle = function () {
      var args = arguments;
      if (timer !== null)
        clearTimeout(timer);
      timer = setTimeout(function () {
        fn.apply(null, args);
        timer = null;
        args = null;
      }, rate);
    };
    return {
      cancel: cancel,
      throttle: throttle
    };
  };
  var $_703q9814wje4cc1t9 = {
    adaptable: adaptable,
    first: first$4,
    last: last$3
  };

  var sketch$10 = function (onView, translate) {
    var memIcon = $_ceck8i11rje4cc1bf.record(Container.sketch({
      dom: $_8147ns113je4cc183.dom('<div aria-hidden="true" class="${prefix}-mask-tap-icon"></div>'),
      containerBehaviours: $_lsmliy2je4cc0te.derive([Toggling.config({
          toggleClass: $_gfh4lpzeje4cc0zd.resolve('mask-tap-icon-selected'),
          toggleOnExecute: false
        })])
    }));
    var onViewThrottle = $_703q9814wje4cc1t9.first(onView, 200);
    return Container.sketch({
      dom: $_8147ns113je4cc183.dom('<div class="${prefix}-disabled-mask"></div>'),
      components: [Container.sketch({
          dom: $_8147ns113je4cc183.dom('<div class="${prefix}-content-container"></div>'),
          components: [Button.sketch({
              dom: $_8147ns113je4cc183.dom('<div class="${prefix}-content-tap-section"></div>'),
              components: [memIcon.asSpec()],
              action: function (button) {
                onViewThrottle.throttle();
              },
              buttonBehaviours: $_lsmliy2je4cc0te.derive([Toggling.config({ toggleClass: $_gfh4lpzeje4cc0zd.resolve('mask-tap-icon-selected') })])
            })]
        })]
    });
  };
  var $_djv5li14vje4cc1t2 = { sketch: sketch$10 };

  var MobileSchema = $_lbfbgyeje4cc0vl.objOf([
    $_c4iqkly7je4cc0ud.strictObjOf('editor', [
      $_c4iqkly7je4cc0ud.strict('getFrame'),
      $_c4iqkly7je4cc0ud.option('getBody'),
      $_c4iqkly7je4cc0ud.option('getDoc'),
      $_c4iqkly7je4cc0ud.option('getWin'),
      $_c4iqkly7je4cc0ud.option('getSelection'),
      $_c4iqkly7je4cc0ud.option('setSelection'),
      $_c4iqkly7je4cc0ud.option('clearSelection'),
      $_c4iqkly7je4cc0ud.option('cursorSaver'),
      $_c4iqkly7je4cc0ud.option('onKeyup'),
      $_c4iqkly7je4cc0ud.option('onNodeChanged'),
      $_c4iqkly7je4cc0ud.option('getCursorBox'),
      $_c4iqkly7je4cc0ud.strict('onDomChanged'),
      $_c4iqkly7je4cc0ud.defaulted('onTouchContent', $_aso7c6wjje4cc0om.noop),
      $_c4iqkly7je4cc0ud.defaulted('onTapContent', $_aso7c6wjje4cc0om.noop),
      $_c4iqkly7je4cc0ud.defaulted('onTouchToolstrip', $_aso7c6wjje4cc0om.noop),
      $_c4iqkly7je4cc0ud.defaulted('onScrollToCursor', $_aso7c6wjje4cc0om.constant({ unbind: $_aso7c6wjje4cc0om.noop })),
      $_c4iqkly7je4cc0ud.defaulted('onScrollToElement', $_aso7c6wjje4cc0om.constant({ unbind: $_aso7c6wjje4cc0om.noop })),
      $_c4iqkly7je4cc0ud.defaulted('onToEditing', $_aso7c6wjje4cc0om.constant({ unbind: $_aso7c6wjje4cc0om.noop })),
      $_c4iqkly7je4cc0ud.defaulted('onToReading', $_aso7c6wjje4cc0om.constant({ unbind: $_aso7c6wjje4cc0om.noop })),
      $_c4iqkly7je4cc0ud.defaulted('onToolbarScrollStart', $_aso7c6wjje4cc0om.identity)
    ]),
    $_c4iqkly7je4cc0ud.strict('socket'),
    $_c4iqkly7je4cc0ud.strict('toolstrip'),
    $_c4iqkly7je4cc0ud.strict('dropup'),
    $_c4iqkly7je4cc0ud.strict('toolbar'),
    $_c4iqkly7je4cc0ud.strict('container'),
    $_c4iqkly7je4cc0ud.strict('alloy'),
    $_c4iqkly7je4cc0ud.state('win', function (spec) {
      return $_1jwy92x3je4cc0qa.owner(spec.socket).dom().defaultView;
    }),
    $_c4iqkly7je4cc0ud.state('body', function (spec) {
      return $_407ejqxfje4cc0rb.fromDom(spec.socket.dom().ownerDocument.body);
    }),
    $_c4iqkly7je4cc0ud.defaulted('translate', $_aso7c6wjje4cc0om.identity),
    $_c4iqkly7je4cc0ud.defaulted('setReadOnly', $_aso7c6wjje4cc0om.noop)
  ]);

  var produce = function (raw) {
    var mobile = $_lbfbgyeje4cc0vl.asRawOrDie('Getting AndroidWebapp schema', MobileSchema, raw);
    $_7ojow7103je4cc11s.set(mobile.toolstrip, 'width', '100%');
    var onTap = function () {
      mobile.setReadOnly(true);
      mode.enter();
    };
    var mask = $_ft6ka112tje4cc1gu.build($_djv5li14vje4cc1t2.sketch(onTap, mobile.translate));
    mobile.alloy.add(mask);
    var maskApi = {
      show: function () {
        mobile.alloy.add(mask);
      },
      hide: function () {
        mobile.alloy.remove(mask);
      }
    };
    $_9zp36ax2je4cc0q8.append(mobile.container, mask.element());
    var mode = $_feamks141je4cc1ol.create(mobile, maskApi);
    return {
      setReadOnly: mobile.setReadOnly,
      refreshStructure: $_aso7c6wjje4cc0om.noop,
      enter: mode.enter,
      exit: mode.exit,
      destroy: $_aso7c6wjje4cc0om.noop
    };
  };
  var $_2q3asa140je4cc1og = { produce: produce };

  var schema$14 = [
    $_c4iqkly7je4cc0ud.defaulted('shell', true),
    $_2sk6q710oje4cc152.field('toolbarBehaviours', [Replacing])
  ];
  var enhanceGroups = function (detail) {
    return { behaviours: $_lsmliy2je4cc0te.derive([Replacing.config({})]) };
  };
  var partTypes$1 = [$_chxroy10vje4cc16d.optional({
      name: 'groups',
      overrides: enhanceGroups
    })];
  var $_czkls4150je4cc1tz = {
    name: $_aso7c6wjje4cc0om.constant('Toolbar'),
    schema: $_aso7c6wjje4cc0om.constant(schema$14),
    parts: $_aso7c6wjje4cc0om.constant(partTypes$1)
  };

  var factory$4 = function (detail, components, spec, _externals) {
    var setGroups = function (toolbar, groups) {
      getGroupContainer(toolbar).fold(function () {
        console.error('Toolbar was defined to not be a shell, but no groups container was specified in components');
        throw new Error('Toolbar was defined to not be a shell, but no groups container was specified in components');
      }, function (container) {
        Replacing.set(container, groups);
      });
    };
    var getGroupContainer = function (component) {
      return detail.shell() ? Option.some(component) : $_ed9ak010tje4cc15q.getPart(component, detail, 'groups');
    };
    var extra = detail.shell() ? {
      behaviours: [Replacing.config({})],
      components: []
    } : {
      behaviours: [],
      components: components
    };
    return {
      uid: detail.uid(),
      dom: detail.dom(),
      components: extra.components,
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive(extra.behaviours), $_2sk6q710oje4cc152.get(detail.toolbarBehaviours())),
      apis: { setGroups: setGroups },
      domModification: { attributes: { role: 'group' } }
    };
  };
  var Toolbar = $_cx5fhm10pje4cc157.composite({
    name: 'Toolbar',
    configFields: $_czkls4150je4cc1tz.schema(),
    partFields: $_czkls4150je4cc1tz.parts(),
    factory: factory$4,
    apis: {
      setGroups: function (apis, toolbar, groups) {
        apis.setGroups(toolbar, groups);
      }
    }
  });

  var schema$15 = [
    $_c4iqkly7je4cc0ud.strict('items'),
    $_546z94z6je4cc0y4.markers(['itemClass']),
    $_2sk6q710oje4cc152.field('tgroupBehaviours', [Keying])
  ];
  var partTypes$2 = [$_chxroy10vje4cc16d.group({
      name: 'items',
      unit: 'item',
      overrides: function (detail) {
        return { domModification: { classes: [detail.markers().itemClass()] } };
      }
    })];
  var $_cguo8e152je4cc1u6 = {
    name: $_aso7c6wjje4cc0om.constant('ToolbarGroup'),
    schema: $_aso7c6wjje4cc0om.constant(schema$15),
    parts: $_aso7c6wjje4cc0om.constant(partTypes$2)
  };

  var factory$5 = function (detail, components, spec, _externals) {
    return $_dax12xwyje4cc0pt.deepMerge({ dom: { attributes: { role: 'toolbar' } } }, {
      uid: detail.uid(),
      dom: detail.dom(),
      components: components,
      behaviours: $_dax12xwyje4cc0pt.deepMerge($_lsmliy2je4cc0te.derive([Keying.config({
          mode: 'flow',
          selector: '.' + detail.markers().itemClass()
        })]), $_2sk6q710oje4cc152.get(detail.tgroupBehaviours())),
      'debug.sketcher': spec['debug.sketcher']
    });
  };
  var ToolbarGroup = $_cx5fhm10pje4cc157.composite({
    name: 'ToolbarGroup',
    configFields: $_cguo8e152je4cc1u6.schema(),
    partFields: $_cguo8e152je4cc1u6.parts(),
    factory: factory$5
  });

  var dataHorizontal = 'data-' + $_gfh4lpzeje4cc0zd.resolve('horizontal-scroll');
  var canScrollVertically = function (container) {
    container.dom().scrollTop = 1;
    var result = container.dom().scrollTop !== 0;
    container.dom().scrollTop = 0;
    return result;
  };
  var canScrollHorizontally = function (container) {
    container.dom().scrollLeft = 1;
    var result = container.dom().scrollLeft !== 0;
    container.dom().scrollLeft = 0;
    return result;
  };
  var hasVerticalScroll = function (container) {
    return container.dom().scrollTop > 0 || canScrollVertically(container);
  };
  var hasHorizontalScroll = function (container) {
    return container.dom().scrollLeft > 0 || canScrollHorizontally(container);
  };
  var markAsHorizontal = function (container) {
    $_bjjq6ixrje4cc0s9.set(container, dataHorizontal, 'true');
  };
  var hasScroll = function (container) {
    return $_bjjq6ixrje4cc0s9.get(container, dataHorizontal) === 'true' ? hasHorizontalScroll : hasVerticalScroll;
  };
  var exclusive = function (scope, selector) {
    return $_24b4y113xje4cc1o6.bind(scope, 'touchmove', function (event) {
      $_by9cdqzxje4cc11d.closest(event.target(), selector).filter(hasScroll).fold(function () {
        event.raw().preventDefault();
      }, $_aso7c6wjje4cc0om.noop);
    });
  };
  var $_82wif6153je4cc1ua = {
    exclusive: exclusive,
    markAsHorizontal: markAsHorizontal
  };

  function ScrollingToolbar () {
    var makeGroup = function (gSpec) {
      var scrollClass = gSpec.scrollable === true ? '${prefix}-toolbar-scrollable-group' : '';
      return {
        dom: $_8147ns113je4cc183.dom('<div aria-label="' + gSpec.label + '" class="${prefix}-toolbar-group ' + scrollClass + '"></div>'),
        tgroupBehaviours: $_lsmliy2je4cc0te.derive([$_4dgb1i126je4cc1d8.config('adhoc-scrollable-toolbar', gSpec.scrollable === true ? [$_ehtdq4y4je4cc0tx.runOnInit(function (component, simulatedEvent) {
              $_7ojow7103je4cc11s.set(component.element(), 'overflow-x', 'auto');
              $_82wif6153je4cc1ua.markAsHorizontal(component.element());
              $_4d6st213uje4cc1ns.register(component.element());
            })] : [])]),
        components: [Container.sketch({ components: [ToolbarGroup.parts().items({})] })],
        markers: { itemClass: $_gfh4lpzeje4cc0zd.resolve('toolbar-group-item') },
        items: gSpec.items
      };
    };
    var toolbar = $_ft6ka112tje4cc1gu.build(Toolbar.sketch({
      dom: $_8147ns113je4cc183.dom('<div class="${prefix}-toolbar"></div>'),
      components: [Toolbar.parts().groups({})],
      toolbarBehaviours: $_lsmliy2je4cc0te.derive([
        Toggling.config({
          toggleClass: $_gfh4lpzeje4cc0zd.resolve('context-toolbar'),
          toggleOnExecute: false,
          aria: { mode: 'none' }
        }),
        Keying.config({ mode: 'cyclic' })
      ]),
      shell: true
    }));
    var wrapper = $_ft6ka112tje4cc1gu.build(Container.sketch({
      dom: { classes: [$_gfh4lpzeje4cc0zd.resolve('toolstrip')] },
      components: [$_ft6ka112tje4cc1gu.premade(toolbar)],
      containerBehaviours: $_lsmliy2je4cc0te.derive([Toggling.config({
          toggleClass: $_gfh4lpzeje4cc0zd.resolve('android-selection-context-toolbar'),
          toggleOnExecute: false
        })])
    }));
    var resetGroups = function () {
      Toolbar.setGroups(toolbar, initGroups.get());
      Toggling.off(toolbar);
    };
    var initGroups = Cell([]);
    var setGroups = function (gs) {
      initGroups.set(gs);
      resetGroups();
    };
    var createGroups = function (gs) {
      return $_elh0pqwsje4cc0p4.map(gs, $_aso7c6wjje4cc0om.compose(ToolbarGroup.sketch, makeGroup));
    };
    var refresh = function () {
      Toolbar.refresh(toolbar);
    };
    var setContextToolbar = function (gs) {
      Toggling.on(toolbar);
      Toolbar.setGroups(toolbar, gs);
    };
    var restoreToolbar = function () {
      if (Toggling.isOn(toolbar)) {
        resetGroups();
      }
    };
    var focus = function () {
      Keying.focusIn(toolbar);
    };
    return {
      wrapper: $_aso7c6wjje4cc0om.constant(wrapper),
      toolbar: $_aso7c6wjje4cc0om.constant(toolbar),
      createGroups: createGroups,
      setGroups: setGroups,
      setContextToolbar: setContextToolbar,
      restoreToolbar: restoreToolbar,
      refresh: refresh,
      focus: focus
    };
  }

  var makeEditSwitch = function (webapp) {
    return $_ft6ka112tje4cc1gu.build(Button.sketch({
      dom: $_8147ns113je4cc183.dom('<div class="${prefix}-mask-edit-icon ${prefix}-icon"></div>'),
      action: function () {
        webapp.run(function (w) {
          w.setReadOnly(false);
        });
      }
    }));
  };
  var makeSocket = function () {
    return $_ft6ka112tje4cc1gu.build(Container.sketch({
      dom: $_8147ns113je4cc183.dom('<div class="${prefix}-editor-socket"></div>'),
      components: [],
      containerBehaviours: $_lsmliy2je4cc0te.derive([Replacing.config({})])
    }));
  };
  var showEdit = function (socket, switchToEdit) {
    Replacing.append(socket, $_ft6ka112tje4cc1gu.premade(switchToEdit));
  };
  var hideEdit = function (socket, switchToEdit) {
    Replacing.remove(socket, switchToEdit);
  };
  var updateMode = function (socket, switchToEdit, readOnly, root) {
    var swap = readOnly === true ? Swapping.toAlpha : Swapping.toOmega;
    swap(root);
    var f = readOnly ? showEdit : hideEdit;
    f(socket, switchToEdit);
  };
  var $_1hef6n154je4cc1um = {
    makeEditSwitch: makeEditSwitch,
    makeSocket: makeSocket,
    updateMode: updateMode
  };

  var getAnimationRoot = function (component, slideConfig) {
    return slideConfig.getAnimationRoot().fold(function () {
      return component.element();
    }, function (get) {
      return get(component);
    });
  };
  var getDimensionProperty = function (slideConfig) {
    return slideConfig.dimension().property();
  };
  var getDimension = function (slideConfig, elem) {
    return slideConfig.dimension().getDimension()(elem);
  };
  var disableTransitions = function (component, slideConfig) {
    var root = getAnimationRoot(component, slideConfig);
    $_7oz06b137je4cc1jy.remove(root, [
      slideConfig.shrinkingClass(),
      slideConfig.growingClass()
    ]);
  };
  var setShrunk = function (component, slideConfig) {
    $_3x5q3zynje4cc0wi.remove(component.element(), slideConfig.openClass());
    $_3x5q3zynje4cc0wi.add(component.element(), slideConfig.closedClass());
    $_7ojow7103je4cc11s.set(component.element(), getDimensionProperty(slideConfig), '0px');
    $_7ojow7103je4cc11s.reflow(component.element());
  };
  var measureTargetSize = function (component, slideConfig) {
    setGrown(component, slideConfig);
    var expanded = getDimension(slideConfig, component.element());
    setShrunk(component, slideConfig);
    return expanded;
  };
  var setGrown = function (component, slideConfig) {
    $_3x5q3zynje4cc0wi.remove(component.element(), slideConfig.closedClass());
    $_3x5q3zynje4cc0wi.add(component.element(), slideConfig.openClass());
    $_7ojow7103je4cc11s.remove(component.element(), getDimensionProperty(slideConfig));
  };
  var doImmediateShrink = function (component, slideConfig, slideState) {
    slideState.setCollapsed();
    $_7ojow7103je4cc11s.set(component.element(), getDimensionProperty(slideConfig), getDimension(slideConfig, component.element()));
    $_7ojow7103je4cc11s.reflow(component.element());
    disableTransitions(component, slideConfig);
    setShrunk(component, slideConfig);
    slideConfig.onStartShrink()(component);
    slideConfig.onShrunk()(component);
  };
  var doStartShrink = function (component, slideConfig, slideState) {
    slideState.setCollapsed();
    $_7ojow7103je4cc11s.set(component.element(), getDimensionProperty(slideConfig), getDimension(slideConfig, component.element()));
    $_7ojow7103je4cc11s.reflow(component.element());
    var root = getAnimationRoot(component, slideConfig);
    $_3x5q3zynje4cc0wi.add(root, slideConfig.shrinkingClass());
    setShrunk(component, slideConfig);
    slideConfig.onStartShrink()(component);
  };
  var doStartGrow = function (component, slideConfig, slideState) {
    var fullSize = measureTargetSize(component, slideConfig);
    var root = getAnimationRoot(component, slideConfig);
    $_3x5q3zynje4cc0wi.add(root, slideConfig.growingClass());
    setGrown(component, slideConfig);
    $_7ojow7103je4cc11s.set(component.element(), getDimensionProperty(slideConfig), fullSize);
    slideState.setExpanded();
    slideConfig.onStartGrow()(component);
  };
  var grow = function (component, slideConfig, slideState) {
    if (!slideState.isExpanded())
      doStartGrow(component, slideConfig, slideState);
  };
  var shrink = function (component, slideConfig, slideState) {
    if (slideState.isExpanded())
      doStartShrink(component, slideConfig, slideState);
  };
  var immediateShrink = function (component, slideConfig, slideState) {
    if (slideState.isExpanded())
      doImmediateShrink(component, slideConfig, slideState);
  };
  var hasGrown = function (component, slideConfig, slideState) {
    return slideState.isExpanded();
  };
  var hasShrunk = function (component, slideConfig, slideState) {
    return slideState.isCollapsed();
  };
  var isGrowing = function (component, slideConfig, slideState) {
    var root = getAnimationRoot(component, slideConfig);
    return $_3x5q3zynje4cc0wi.has(root, slideConfig.growingClass()) === true;
  };
  var isShrinking = function (component, slideConfig, slideState) {
    var root = getAnimationRoot(component, slideConfig);
    return $_3x5q3zynje4cc0wi.has(root, slideConfig.shrinkingClass()) === true;
  };
  var isTransitioning = function (component, slideConfig, slideState) {
    return isGrowing(component, slideConfig, slideState) === true || isShrinking(component, slideConfig, slideState) === true;
  };
  var toggleGrow = function (component, slideConfig, slideState) {
    var f = slideState.isExpanded() ? doStartShrink : doStartGrow;
    f(component, slideConfig, slideState);
  };
  var $_91nbho158je4cc1vb = {
    grow: grow,
    shrink: shrink,
    immediateShrink: immediateShrink,
    hasGrown: hasGrown,
    hasShrunk: hasShrunk,
    isGrowing: isGrowing,
    isShrinking: isShrinking,
    isTransitioning: isTransitioning,
    toggleGrow: toggleGrow,
    disableTransitions: disableTransitions
  };

  var exhibit$5 = function (base, slideConfig) {
    var expanded = slideConfig.expanded();
    return expanded ? $_5yi74lyhje4cc0vw.nu({
      classes: [slideConfig.openClass()],
      styles: {}
    }) : $_5yi74lyhje4cc0vw.nu({
      classes: [slideConfig.closedClass()],
      styles: $_d08hr1xsje4cc0sg.wrap(slideConfig.dimension().property(), '0px')
    });
  };
  var events$9 = function (slideConfig, slideState) {
    return $_ehtdq4y4je4cc0tx.derive([$_ehtdq4y4je4cc0tx.run($_g7q1k3wije4cc0oi.transitionend(), function (component, simulatedEvent) {
        var raw = simulatedEvent.event().raw();
        if (raw.propertyName === slideConfig.dimension().property()) {
          $_91nbho158je4cc1vb.disableTransitions(component, slideConfig, slideState);
          if (slideState.isExpanded())
            $_7ojow7103je4cc11s.remove(component.element(), slideConfig.dimension().property());
          var notify = slideState.isExpanded() ? slideConfig.onGrown() : slideConfig.onShrunk();
          notify(component, simulatedEvent);
        }
      })]);
  };
  var $_16igjz157je4cc1v6 = {
    exhibit: exhibit$5,
    events: events$9
  };

  var SlidingSchema = [
    $_c4iqkly7je4cc0ud.strict('closedClass'),
    $_c4iqkly7je4cc0ud.strict('openClass'),
    $_c4iqkly7je4cc0ud.strict('shrinkingClass'),
    $_c4iqkly7je4cc0ud.strict('growingClass'),
    $_c4iqkly7je4cc0ud.option('getAnimationRoot'),
    $_546z94z6je4cc0y4.onHandler('onShrunk'),
    $_546z94z6je4cc0y4.onHandler('onStartShrink'),
    $_546z94z6je4cc0y4.onHandler('onGrown'),
    $_546z94z6je4cc0y4.onHandler('onStartGrow'),
    $_c4iqkly7je4cc0ud.defaulted('expanded', false),
    $_c4iqkly7je4cc0ud.strictOf('dimension', $_lbfbgyeje4cc0vl.choose('property', {
      width: [
        $_546z94z6je4cc0y4.output('property', 'width'),
        $_546z94z6je4cc0y4.output('getDimension', function (elem) {
          return $_ad4jyp11kje4cc1ai.get(elem) + 'px';
        })
      ],
      height: [
        $_546z94z6je4cc0y4.output('property', 'height'),
        $_546z94z6je4cc0y4.output('getDimension', function (elem) {
          return $_eftfr9102je4cc11q.get(elem) + 'px';
        })
      ]
    }))
  ];

  var init$4 = function (spec) {
    var state = Cell(spec.expanded());
    var readState = function () {
      return 'expanded: ' + state.get();
    };
    return BehaviourState({
      isExpanded: function () {
        return state.get() === true;
      },
      isCollapsed: function () {
        return state.get() === false;
      },
      setCollapsed: $_aso7c6wjje4cc0om.curry(state.set, false),
      setExpanded: $_aso7c6wjje4cc0om.curry(state.set, true),
      readState: readState
    });
  };
  var $_8iarnq15aje4cc1vv = { init: init$4 };

  var Sliding = $_lsmliy2je4cc0te.create({
    fields: SlidingSchema,
    name: 'sliding',
    active: $_16igjz157je4cc1v6,
    apis: $_91nbho158je4cc1vb,
    state: $_8iarnq15aje4cc1vv
  });

  var build$2 = function (refresh, scrollIntoView) {
    var dropup = $_ft6ka112tje4cc1gu.build(Container.sketch({
      dom: {
        tag: 'div',
        classes: $_gfh4lpzeje4cc0zd.resolve('dropup')
      },
      components: [],
      containerBehaviours: $_lsmliy2je4cc0te.derive([
        Replacing.config({}),
        Sliding.config({
          closedClass: $_gfh4lpzeje4cc0zd.resolve('dropup-closed'),
          openClass: $_gfh4lpzeje4cc0zd.resolve('dropup-open'),
          shrinkingClass: $_gfh4lpzeje4cc0zd.resolve('dropup-shrinking'),
          growingClass: $_gfh4lpzeje4cc0zd.resolve('dropup-growing'),
          dimension: { property: 'height' },
          onShrunk: function (component) {
            refresh();
            scrollIntoView();
            Replacing.set(component, []);
          },
          onGrown: function (component) {
            refresh();
            scrollIntoView();
          }
        }),
        $_g6oxx7zdje4cc0za.orientation(function (component, data) {
          disappear($_aso7c6wjje4cc0om.noop);
        })
      ])
    }));
    var appear = function (menu, update, component) {
      if (Sliding.hasShrunk(dropup) === true && Sliding.isTransitioning(dropup) === false) {
        window.requestAnimationFrame(function () {
          update(component);
          Replacing.set(dropup, [menu()]);
          Sliding.grow(dropup);
        });
      }
    };
    var disappear = function (onReadyToShrink) {
      window.requestAnimationFrame(function () {
        onReadyToShrink();
        Sliding.shrink(dropup);
      });
    };
    return {
      appear: appear,
      disappear: disappear,
      component: $_aso7c6wjje4cc0om.constant(dropup),
      element: dropup.element
    };
  };
  var $_1rw6u155je4cc1ut = { build: build$2 };

  var isDangerous = function (event) {
    return event.raw().which === $_3a3tymzpje4cc10f.BACKSPACE()[0] && !$_elh0pqwsje4cc0p4.contains([
      'input',
      'textarea'
    ], $_7qwxg2xkje4cc0ro.name(event.target()));
  };
  var isFirefox = $_2j4x3gwkje4cc0oo.detect().browser.isFirefox();
  var settingsSchema = $_lbfbgyeje4cc0vl.objOfOnly([
    $_c4iqkly7je4cc0ud.strictFunction('triggerEvent'),
    $_c4iqkly7je4cc0ud.strictFunction('broadcastEvent'),
    $_c4iqkly7je4cc0ud.defaulted('stopBackspace', true)
  ]);
  var bindFocus = function (container, handler) {
    if (isFirefox) {
      return $_24b4y113xje4cc1o6.capture(container, 'focus', handler);
    } else {
      return $_24b4y113xje4cc1o6.bind(container, 'focusin', handler);
    }
  };
  var bindBlur = function (container, handler) {
    if (isFirefox) {
      return $_24b4y113xje4cc1o6.capture(container, 'blur', handler);
    } else {
      return $_24b4y113xje4cc1o6.bind(container, 'focusout', handler);
    }
  };
  var setup$2 = function (container, rawSettings) {
    var settings = $_lbfbgyeje4cc0vl.asRawOrDie('Getting GUI events settings', settingsSchema, rawSettings);
    var pointerEvents = $_2j4x3gwkje4cc0oo.detect().deviceType.isTouch() ? [
      'touchstart',
      'touchmove',
      'touchend',
      'gesturestart'
    ] : [
      'mousedown',
      'mouseup',
      'mouseover',
      'mousemove',
      'mouseout',
      'click'
    ];
    var tapEvent = $_7io6r5144je4cc1pa.monitor(settings);
    var simpleEvents = $_elh0pqwsje4cc0p4.map(pointerEvents.concat([
      'selectstart',
      'input',
      'contextmenu',
      'change',
      'transitionend',
      'dragstart',
      'dragover',
      'drop'
    ]), function (type) {
      return $_24b4y113xje4cc1o6.bind(container, type, function (event) {
        tapEvent.fireIfReady(event, type).each(function (tapStopped) {
          if (tapStopped)
            event.kill();
        });
        var stopped = settings.triggerEvent(type, event);
        if (stopped)
          event.kill();
      });
    });
    var onKeydown = $_24b4y113xje4cc1o6.bind(container, 'keydown', function (event) {
      var stopped = settings.triggerEvent('keydown', event);
      if (stopped)
        event.kill();
      else if (settings.stopBackspace === true && isDangerous(event)) {
        event.prevent();
      }
    });
    var onFocusIn = bindFocus(container, function (event) {
      var stopped = settings.triggerEvent('focusin', event);
      if (stopped)
        event.kill();
    });
    var onFocusOut = bindBlur(container, function (event) {
      var stopped = settings.triggerEvent('focusout', event);
      if (stopped)
        event.kill();
      setTimeout(function () {
        settings.triggerEvent($_g6tooswhje4cc0of.postBlur(), event);
      }, 0);
    });
    var defaultView = $_1jwy92x3je4cc0qa.defaultView(container);
    var onWindowScroll = $_24b4y113xje4cc1o6.bind(defaultView, 'scroll', function (event) {
      var stopped = settings.broadcastEvent($_g6tooswhje4cc0of.windowScroll(), event);
      if (stopped)
        event.kill();
    });
    var unbind = function () {
      $_elh0pqwsje4cc0p4.each(simpleEvents, function (e) {
        e.unbind();
      });
      onKeydown.unbind();
      onFocusIn.unbind();
      onFocusOut.unbind();
      onWindowScroll.unbind();
    };
    return { unbind: unbind };
  };
  var $_aocs7215dje4cc1wx = { setup: setup$2 };

  var derive$3 = function (rawEvent, rawTarget) {
    var source = $_d08hr1xsje4cc0sg.readOptFrom(rawEvent, 'target').map(function (getTarget) {
      return getTarget();
    }).getOr(rawTarget);
    return Cell(source);
  };
  var $_9nnnej15fje4cc1xu = { derive: derive$3 };

  var fromSource = function (event, source) {
    var stopper = Cell(false);
    var cutter = Cell(false);
    var stop = function () {
      stopper.set(true);
    };
    var cut = function () {
      cutter.set(true);
    };
    return {
      stop: stop,
      cut: cut,
      isStopped: stopper.get,
      isCut: cutter.get,
      event: $_aso7c6wjje4cc0om.constant(event),
      setSource: source.set,
      getSource: source.get
    };
  };
  var fromExternal = function (event) {
    var stopper = Cell(false);
    var stop = function () {
      stopper.set(true);
    };
    return {
      stop: stop,
      cut: $_aso7c6wjje4cc0om.noop,
      isStopped: stopper.get,
      isCut: $_aso7c6wjje4cc0om.constant(false),
      event: $_aso7c6wjje4cc0om.constant(event),
      setTarget: $_aso7c6wjje4cc0om.die(new Error('Cannot set target of a broadcasted event')),
      getTarget: $_aso7c6wjje4cc0om.die(new Error('Cannot get target of a broadcasted event'))
    };
  };
  var fromTarget = function (event, target) {
    var source = Cell(target);
    return fromSource(event, source);
  };
  var $_9kx8uq15gje4cc1xz = {
    fromSource: fromSource,
    fromExternal: fromExternal,
    fromTarget: fromTarget
  };

  var adt$6 = $_170ozvxwje4cc0st.generate([
    { stopped: [] },
    { resume: ['element'] },
    { complete: [] }
  ]);
  var doTriggerHandler = function (lookup, eventType, rawEvent, target, source, logger) {
    var handler = lookup(eventType, target);
    var simulatedEvent = $_9kx8uq15gje4cc1xz.fromSource(rawEvent, source);
    return handler.fold(function () {
      logger.logEventNoHandlers(eventType, target);
      return adt$6.complete();
    }, function (handlerInfo) {
      var descHandler = handlerInfo.descHandler();
      var eventHandler = $_4x8s3a134je4cc1j4.getHandler(descHandler);
      eventHandler(simulatedEvent);
      if (simulatedEvent.isStopped()) {
        logger.logEventStopped(eventType, handlerInfo.element(), descHandler.purpose());
        return adt$6.stopped();
      } else if (simulatedEvent.isCut()) {
        logger.logEventCut(eventType, handlerInfo.element(), descHandler.purpose());
        return adt$6.complete();
      } else
        return $_1jwy92x3je4cc0qa.parent(handlerInfo.element()).fold(function () {
          logger.logNoParent(eventType, handlerInfo.element(), descHandler.purpose());
          return adt$6.complete();
        }, function (parent) {
          logger.logEventResponse(eventType, handlerInfo.element(), descHandler.purpose());
          return adt$6.resume(parent);
        });
    });
  };
  var doTriggerOnUntilStopped = function (lookup, eventType, rawEvent, rawTarget, source, logger) {
    return doTriggerHandler(lookup, eventType, rawEvent, rawTarget, source, logger).fold(function () {
      return true;
    }, function (parent) {
      return doTriggerOnUntilStopped(lookup, eventType, rawEvent, parent, source, logger);
    }, function () {
      return false;
    });
  };
  var triggerHandler = function (lookup, eventType, rawEvent, target, logger) {
    var source = $_9nnnej15fje4cc1xu.derive(rawEvent, target);
    return doTriggerHandler(lookup, eventType, rawEvent, target, source, logger);
  };
  var broadcast = function (listeners, rawEvent, logger) {
    var simulatedEvent = $_9kx8uq15gje4cc1xz.fromExternal(rawEvent);
    $_elh0pqwsje4cc0p4.each(listeners, function (listener) {
      var descHandler = listener.descHandler();
      var handler = $_4x8s3a134je4cc1j4.getHandler(descHandler);
      handler(simulatedEvent);
    });
    return simulatedEvent.isStopped();
  };
  var triggerUntilStopped = function (lookup, eventType, rawEvent, logger) {
    var rawTarget = rawEvent.target();
    return triggerOnUntilStopped(lookup, eventType, rawEvent, rawTarget, logger);
  };
  var triggerOnUntilStopped = function (lookup, eventType, rawEvent, rawTarget, logger) {
    var source = $_9nnnej15fje4cc1xu.derive(rawEvent, rawTarget);
    return doTriggerOnUntilStopped(lookup, eventType, rawEvent, rawTarget, source, logger);
  };
  var $_bvi7sn15eje4cc1xl = {
    triggerHandler: triggerHandler,
    triggerUntilStopped: triggerUntilStopped,
    triggerOnUntilStopped: triggerOnUntilStopped,
    broadcast: broadcast
  };

  var closest$4 = function (target, transform, isRoot) {
    var delegate = $_40elmhyvje4cc0x6.closest(target, function (elem) {
      return transform(elem).isSome();
    }, isRoot);
    return delegate.bind(transform);
  };
  var $_1fwmzz15jje4cc1yo = { closest: closest$4 };

  var eventHandler = $_ajn35gx4je4cc0qj.immutable('element', 'descHandler');
  var messageHandler = function (id, handler) {
    return {
      id: $_aso7c6wjje4cc0om.constant(id),
      descHandler: $_aso7c6wjje4cc0om.constant(handler)
    };
  };
  function EventRegistry () {
    var registry = {};
    var registerId = function (extraArgs, id, events) {
      $_i587hx0je4cc0pw.each(events, function (v, k) {
        var handlers = registry[k] !== undefined ? registry[k] : {};
        handlers[id] = $_4x8s3a134je4cc1j4.curryArgs(v, extraArgs);
        registry[k] = handlers;
      });
    };
    var findHandler = function (handlers, elem) {
      return $_37lg5b10xje4cc173.read(elem).fold(function (err) {
        return Option.none();
      }, function (id) {
        var reader = $_d08hr1xsje4cc0sg.readOpt(id);
        return handlers.bind(reader).map(function (descHandler) {
          return eventHandler(elem, descHandler);
        });
      });
    };
    var filterByType = function (type) {
      return $_d08hr1xsje4cc0sg.readOptFrom(registry, type).map(function (handlers) {
        return $_i587hx0je4cc0pw.mapToArray(handlers, function (f, id) {
          return messageHandler(id, f);
        });
      }).getOr([]);
    };
    var find = function (isAboveRoot, type, target) {
      var readType = $_d08hr1xsje4cc0sg.readOpt(type);
      var handlers = readType(registry);
      return $_1fwmzz15jje4cc1yo.closest(target, function (elem) {
        return findHandler(handlers, elem);
      }, isAboveRoot);
    };
    var unregisterId = function (id) {
      $_i587hx0je4cc0pw.each(registry, function (handlersById, eventName) {
        if (handlersById.hasOwnProperty(id))
          delete handlersById[id];
      });
    };
    return {
      registerId: registerId,
      unregisterId: unregisterId,
      filterByType: filterByType,
      find: find
    };
  }

  function Registry () {
    var events = EventRegistry();
    var components = {};
    var readOrTag = function (component) {
      var elem = component.element();
      return $_37lg5b10xje4cc173.read(elem).fold(function () {
        return $_37lg5b10xje4cc173.write('uid-', component.element());
      }, function (uid) {
        return uid;
      });
    };
    var failOnDuplicate = function (component, tagId) {
      var conflict = components[tagId];
      if (conflict === component)
        unregister(component);
      else
        throw new Error('The tagId "' + tagId + '" is already used by: ' + $_dfrb81xmje4cc0rz.element(conflict.element()) + '\nCannot use it for: ' + $_dfrb81xmje4cc0rz.element(component.element()) + '\n' + 'The conflicting element is' + ($_6jf42xxjje4cc0rm.inBody(conflict.element()) ? ' ' : ' not ') + 'already in the DOM');
    };
    var register = function (component) {
      var tagId = readOrTag(component);
      if ($_d08hr1xsje4cc0sg.hasKey(components, tagId))
        failOnDuplicate(component, tagId);
      var extraArgs = [component];
      events.registerId(extraArgs, tagId, component.events());
      components[tagId] = component;
    };
    var unregister = function (component) {
      $_37lg5b10xje4cc173.read(component.element()).each(function (tagId) {
        components[tagId] = undefined;
        events.unregisterId(tagId);
      });
    };
    var filter = function (type) {
      return events.filterByType(type);
    };
    var find = function (isAboveRoot, type, target) {
      return events.find(isAboveRoot, type, target);
    };
    var getById = function (id) {
      return $_d08hr1xsje4cc0sg.readOpt(id)(components);
    };
    return {
      find: find,
      filter: filter,
      register: register,
      unregister: unregister,
      getById: getById
    };
  }

  var create$6 = function () {
    var root = $_ft6ka112tje4cc1gu.build(Container.sketch({ dom: { tag: 'div' } }));
    return takeover(root);
  };
  var takeover = function (root) {
    var isAboveRoot = function (el) {
      return $_1jwy92x3je4cc0qa.parent(root.element()).fold(function () {
        return true;
      }, function (parent) {
        return $_1stme4x9je4cc0qq.eq(el, parent);
      });
    };
    var registry = Registry();
    var lookup = function (eventName, target) {
      return registry.find(isAboveRoot, eventName, target);
    };
    var domEvents = $_aocs7215dje4cc1wx.setup(root.element(), {
      triggerEvent: function (eventName, event) {
        return $_g603ovxlje4cc0rq.monitorEvent(eventName, event.target(), function (logger) {
          return $_bvi7sn15eje4cc1xl.triggerUntilStopped(lookup, eventName, event, logger);
        });
      },
      broadcastEvent: function (eventName, event) {
        var listeners = registry.filter(eventName);
        return $_bvi7sn15eje4cc1xl.broadcast(listeners, event);
      }
    });
    var systemApi = SystemApi({
      debugInfo: $_aso7c6wjje4cc0om.constant('real'),
      triggerEvent: function (customType, target, data) {
        $_g603ovxlje4cc0rq.monitorEvent(customType, target, function (logger) {
          $_bvi7sn15eje4cc1xl.triggerOnUntilStopped(lookup, customType, data, target, logger);
        });
      },
      triggerFocus: function (target, originator) {
        $_37lg5b10xje4cc173.read(target).fold(function () {
          $_4hvdjzytje4cc0wz.focus(target);
        }, function (_alloyId) {
          $_g603ovxlje4cc0rq.monitorEvent($_g6tooswhje4cc0of.focus(), target, function (logger) {
            $_bvi7sn15eje4cc1xl.triggerHandler(lookup, $_g6tooswhje4cc0of.focus(), {
              originator: $_aso7c6wjje4cc0om.constant(originator),
              target: $_aso7c6wjje4cc0om.constant(target)
            }, target, logger);
          });
        });
      },
      triggerEscape: function (comp, simulatedEvent) {
        systemApi.triggerEvent('keydown', comp.element(), simulatedEvent.event());
      },
      getByUid: function (uid) {
        return getByUid(uid);
      },
      getByDom: function (elem) {
        return getByDom(elem);
      },
      build: $_ft6ka112tje4cc1gu.build,
      addToGui: function (c) {
        add(c);
      },
      removeFromGui: function (c) {
        remove(c);
      },
      addToWorld: function (c) {
        addToWorld(c);
      },
      removeFromWorld: function (c) {
        removeFromWorld(c);
      },
      broadcast: function (message) {
        broadcast(message);
      },
      broadcastOn: function (channels, message) {
        broadcastOn(channels, message);
      }
    });
    var addToWorld = function (component) {
      component.connect(systemApi);
      if (!$_7qwxg2xkje4cc0ro.isText(component.element())) {
        registry.register(component);
        $_elh0pqwsje4cc0p4.each(component.components(), addToWorld);
        systemApi.triggerEvent($_g6tooswhje4cc0of.systemInit(), component.element(), { target: $_aso7c6wjje4cc0om.constant(component.element()) });
      }
    };
    var removeFromWorld = function (component) {
      if (!$_7qwxg2xkje4cc0ro.isText(component.element())) {
        $_elh0pqwsje4cc0p4.each(component.components(), removeFromWorld);
        registry.unregister(component);
      }
      component.disconnect();
    };
    var add = function (component) {
      $_b576ucx1je4cc0py.attach(root, component);
    };
    var remove = function (component) {
      $_b576ucx1je4cc0py.detach(component);
    };
    var destroy = function () {
      domEvents.unbind();
      $_buyc6zxhje4cc0rh.remove(root.element());
    };
    var broadcastData = function (data) {
      var receivers = registry.filter($_g6tooswhje4cc0of.receive());
      $_elh0pqwsje4cc0p4.each(receivers, function (receiver) {
        var descHandler = receiver.descHandler();
        var handler = $_4x8s3a134je4cc1j4.getHandler(descHandler);
        handler(data);
      });
    };
    var broadcast = function (message) {
      broadcastData({
        universal: $_aso7c6wjje4cc0om.constant(true),
        data: $_aso7c6wjje4cc0om.constant(message)
      });
    };
    var broadcastOn = function (channels, message) {
      broadcastData({
        universal: $_aso7c6wjje4cc0om.constant(false),
        channels: $_aso7c6wjje4cc0om.constant(channels),
        data: $_aso7c6wjje4cc0om.constant(message)
      });
    };
    var getByUid = function (uid) {
      return registry.getById(uid).fold(function () {
        return Result.error(new Error('Could not find component with uid: "' + uid + '" in system.'));
      }, Result.value);
    };
    var getByDom = function (elem) {
      return $_37lg5b10xje4cc173.read(elem).bind(getByUid);
    };
    addToWorld(root);
    return {
      root: $_aso7c6wjje4cc0om.constant(root),
      element: root.element,
      destroy: destroy,
      add: add,
      remove: remove,
      getByUid: getByUid,
      getByDom: getByDom,
      addToWorld: addToWorld,
      removeFromWorld: removeFromWorld,
      broadcast: broadcast,
      broadcastOn: broadcastOn
    };
  };
  var $_20rn2r15cje4cc1wb = {
    create: create$6,
    takeover: takeover
  };

  var READ_ONLY_MODE_CLASS = $_aso7c6wjje4cc0om.constant($_gfh4lpzeje4cc0zd.resolve('readonly-mode'));
  var EDIT_MODE_CLASS = $_aso7c6wjje4cc0om.constant($_gfh4lpzeje4cc0zd.resolve('edit-mode'));
  function OuterContainer (spec) {
    var root = $_ft6ka112tje4cc1gu.build(Container.sketch({
      dom: { classes: [$_gfh4lpzeje4cc0zd.resolve('outer-container')].concat(spec.classes) },
      containerBehaviours: $_lsmliy2je4cc0te.derive([Swapping.config({
          alpha: READ_ONLY_MODE_CLASS(),
          omega: EDIT_MODE_CLASS()
        })])
    }));
    return $_20rn2r15cje4cc1wb.takeover(root);
  }

  function AndroidRealm (scrollIntoView) {
    var alloy = OuterContainer({ classes: [$_gfh4lpzeje4cc0zd.resolve('android-container')] });
    var toolbar = ScrollingToolbar();
    var webapp = $_7qzgbo12oje4cc1fr.api();
    var switchToEdit = $_1hef6n154je4cc1um.makeEditSwitch(webapp);
    var socket = $_1hef6n154je4cc1um.makeSocket();
    var dropup = $_1rw6u155je4cc1ut.build($_aso7c6wjje4cc0om.noop, scrollIntoView);
    alloy.add(toolbar.wrapper());
    alloy.add(socket);
    alloy.add(dropup.component());
    var setToolbarGroups = function (rawGroups) {
      var groups = toolbar.createGroups(rawGroups);
      toolbar.setGroups(groups);
    };
    var setContextToolbar = function (rawGroups) {
      var groups = toolbar.createGroups(rawGroups);
      toolbar.setContextToolbar(groups);
    };
    var focusToolbar = function () {
      toolbar.focus();
    };
    var restoreToolbar = function () {
      toolbar.restoreToolbar();
    };
    var init = function (spec) {
      webapp.set($_2q3asa140je4cc1og.produce(spec));
    };
    var exit = function () {
      webapp.run(function (w) {
        w.exit();
        Replacing.remove(socket, switchToEdit);
      });
    };
    var updateMode = function (readOnly) {
      $_1hef6n154je4cc1um.updateMode(socket, switchToEdit, readOnly, alloy.root());
    };
    return {
      system: $_aso7c6wjje4cc0om.constant(alloy),
      element: alloy.element,
      init: init,
      exit: exit,
      setToolbarGroups: setToolbarGroups,
      setContextToolbar: setContextToolbar,
      focusToolbar: focusToolbar,
      restoreToolbar: restoreToolbar,
      updateMode: updateMode,
      socket: $_aso7c6wjje4cc0om.constant(socket),
      dropup: $_aso7c6wjje4cc0om.constant(dropup)
    };
  }

  var input = function (parent, operation) {
    var input = $_407ejqxfje4cc0rb.fromTag('input');
    $_7ojow7103je4cc11s.setAll(input, {
      opacity: '0',
      position: 'absolute',
      top: '-1000px',
      left: '-1000px'
    });
    $_9zp36ax2je4cc0q8.append(parent, input);
    $_4hvdjzytje4cc0wz.focus(input);
    operation(input);
    $_buyc6zxhje4cc0rh.remove(input);
  };
  var $_vlzgx15oje4cc205 = { input: input };

  var refreshInput = function (input) {
    var start = input.dom().selectionStart;
    var end = input.dom().selectionEnd;
    var dir = input.dom().selectionDirection;
    setTimeout(function () {
      input.dom().setSelectionRange(start, end, dir);
      $_4hvdjzytje4cc0wz.focus(input);
    }, 50);
  };
  var refresh = function (winScope) {
    var sel = winScope.getSelection();
    if (sel.rangeCount > 0) {
      var br = sel.getRangeAt(0);
      var r = winScope.document.createRange();
      r.setStart(br.startContainer, br.startOffset);
      r.setEnd(br.endContainer, br.endOffset);
      sel.removeAllRanges();
      sel.addRange(r);
    }
  };
  var $_24urki15qje4cc20k = {
    refreshInput: refreshInput,
    refresh: refresh
  };

  var resume$1 = function (cWin, frame) {
    $_4hvdjzytje4cc0wz.active().each(function (active) {
      if (!$_1stme4x9je4cc0qq.eq(active, frame)) {
        $_4hvdjzytje4cc0wz.blur(active);
      }
    });
    cWin.focus();
    $_4hvdjzytje4cc0wz.focus($_407ejqxfje4cc0rb.fromDom(cWin.document.body));
    $_24urki15qje4cc20k.refresh(cWin);
  };
  var $_7ssugs15pje4cc20e = { resume: resume$1 };

  var stubborn = function (outerBody, cWin, page, frame) {
    var toEditing = function () {
      $_7ssugs15pje4cc20e.resume(cWin, frame);
    };
    var toReading = function () {
      $_vlzgx15oje4cc205.input(outerBody, $_4hvdjzytje4cc0wz.blur);
    };
    var captureInput = $_24b4y113xje4cc1o6.bind(page, 'keydown', function (evt) {
      if (!$_elh0pqwsje4cc0p4.contains([
          'input',
          'textarea'
        ], $_7qwxg2xkje4cc0ro.name(evt.target()))) {
        toEditing();
      }
    });
    var onToolbarTouch = function () {
    };
    var destroy = function () {
      captureInput.unbind();
    };
    return {
      toReading: toReading,
      toEditing: toEditing,
      onToolbarTouch: onToolbarTouch,
      destroy: destroy
    };
  };
  var timid = function (outerBody, cWin, page, frame) {
    var dismissKeyboard = function () {
      $_4hvdjzytje4cc0wz.blur(frame);
    };
    var onToolbarTouch = function () {
      dismissKeyboard();
    };
    var toReading = function () {
      dismissKeyboard();
    };
    var toEditing = function () {
      $_7ssugs15pje4cc20e.resume(cWin, frame);
    };
    return {
      toReading: toReading,
      toEditing: toEditing,
      onToolbarTouch: onToolbarTouch,
      destroy: $_aso7c6wjje4cc0om.noop
    };
  };
  var $_chauq615nje4cc1zo = {
    stubborn: stubborn,
    timid: timid
  };

  var initEvents$1 = function (editorApi, iosApi, toolstrip, socket, dropup) {
    var saveSelectionFirst = function () {
      iosApi.run(function (api) {
        api.highlightSelection();
      });
    };
    var refreshIosSelection = function () {
      iosApi.run(function (api) {
        api.refreshSelection();
      });
    };
    var scrollToY = function (yTop, height) {
      var y = yTop - socket.dom().scrollTop;
      iosApi.run(function (api) {
        api.scrollIntoView(y, y + height);
      });
    };
    var scrollToElement = function (target) {
      scrollToY(iosApi, socket);
    };
    var scrollToCursor = function () {
      editorApi.getCursorBox().each(function (box) {
        scrollToY(box.top(), box.height());
      });
    };
    var clearSelection = function () {
      iosApi.run(function (api) {
        api.clearSelection();
      });
    };
    var clearAndRefresh = function () {
      clearSelection();
      refreshThrottle.throttle();
    };
    var refreshView = function () {
      scrollToCursor();
      iosApi.run(function (api) {
        api.syncHeight();
      });
    };
    var reposition = function () {
      var toolbarHeight = $_eftfr9102je4cc11q.get(toolstrip);
      iosApi.run(function (api) {
        api.setViewportOffset(toolbarHeight);
      });
      refreshIosSelection();
      refreshView();
    };
    var toEditing = function () {
      iosApi.run(function (api) {
        api.toEditing();
      });
    };
    var toReading = function () {
      iosApi.run(function (api) {
        api.toReading();
      });
    };
    var onToolbarTouch = function (event) {
      iosApi.run(function (api) {
        api.onToolbarTouch(event);
      });
    };
    var tapping = $_67oir1143je4cc1p7.monitor(editorApi);
    var refreshThrottle = $_703q9814wje4cc1t9.last(refreshView, 300);
    var listeners = [
      editorApi.onKeyup(clearAndRefresh),
      editorApi.onNodeChanged(refreshIosSelection),
      editorApi.onDomChanged(refreshThrottle.throttle),
      editorApi.onDomChanged(refreshIosSelection),
      editorApi.onScrollToCursor(function (tinyEvent) {
        tinyEvent.preventDefault();
        refreshThrottle.throttle();
      }),
      editorApi.onScrollToElement(function (event) {
        scrollToElement(event.element());
      }),
      editorApi.onToEditing(toEditing),
      editorApi.onToReading(toReading),
      $_24b4y113xje4cc1o6.bind(editorApi.doc(), 'touchend', function (touchEvent) {
        if ($_1stme4x9je4cc0qq.eq(editorApi.html(), touchEvent.target()) || $_1stme4x9je4cc0qq.eq(editorApi.body(), touchEvent.target())) {
        }
      }),
      $_24b4y113xje4cc1o6.bind(toolstrip, 'transitionend', function (transitionEvent) {
        if (transitionEvent.raw().propertyName === 'height') {
          reposition();
        }
      }),
      $_24b4y113xje4cc1o6.capture(toolstrip, 'touchstart', function (touchEvent) {
        saveSelectionFirst();
        onToolbarTouch(touchEvent);
        editorApi.onTouchToolstrip();
      }),
      $_24b4y113xje4cc1o6.bind(editorApi.body(), 'touchstart', function (evt) {
        clearSelection();
        editorApi.onTouchContent();
        tapping.fireTouchstart(evt);
      }),
      tapping.onTouchmove(),
      tapping.onTouchend(),
      $_24b4y113xje4cc1o6.bind(editorApi.body(), 'click', function (event) {
        event.kill();
      }),
      $_24b4y113xje4cc1o6.bind(toolstrip, 'touchmove', function () {
        editorApi.onToolbarScrollStart();
      })
    ];
    var destroy = function () {
      $_elh0pqwsje4cc0p4.each(listeners, function (l) {
        l.unbind();
      });
    };
    return { destroy: destroy };
  };
  var $_69zea915rje4cc20o = { initEvents: initEvents$1 };

  function FakeSelection (win, frame) {
    var doc = win.document;
    var container = $_407ejqxfje4cc0rb.fromTag('div');
    $_3x5q3zynje4cc0wi.add(container, $_gfh4lpzeje4cc0zd.resolve('unfocused-selections'));
    $_9zp36ax2je4cc0q8.append($_407ejqxfje4cc0rb.fromDom(doc.documentElement), container);
    var onTouch = $_24b4y113xje4cc1o6.bind(container, 'touchstart', function (event) {
      event.prevent();
      $_7ssugs15pje4cc20e.resume(win, frame);
      clear();
    });
    var make = function (rectangle) {
      var span = $_407ejqxfje4cc0rb.fromTag('span');
      $_7oz06b137je4cc1jy.add(span, [
        $_gfh4lpzeje4cc0zd.resolve('layer-editor'),
        $_gfh4lpzeje4cc0zd.resolve('unfocused-selection')
      ]);
      $_7ojow7103je4cc11s.setAll(span, {
        left: rectangle.left() + 'px',
        top: rectangle.top() + 'px',
        width: rectangle.width() + 'px',
        height: rectangle.height() + 'px'
      });
      return span;
    };
    var update = function () {
      clear();
      var rectangles = $_c6xmxq148je4cc1q1.getRectangles(win);
      var spans = $_elh0pqwsje4cc0p4.map(rectangles, make);
      $_dy5220xije4cc0rj.append(container, spans);
    };
    var clear = function () {
      $_buyc6zxhje4cc0rh.empty(container);
    };
    var destroy = function () {
      onTouch.unbind();
      $_buyc6zxhje4cc0rh.remove(container);
    };
    var isActive = function () {
      return $_1jwy92x3je4cc0qa.children(container).length > 0;
    };
    return {
      update: update,
      isActive: isActive,
      destroy: destroy,
      clear: clear
    };
  }

  var nu$8 = function (baseFn) {
    var data = Option.none();
    var callbacks = [];
    var map = function (f) {
      return nu$8(function (nCallback) {
        get(function (data) {
          nCallback(f(data));
        });
      });
    };
    var get = function (nCallback) {
      if (isReady())
        call(nCallback);
      else
        callbacks.push(nCallback);
    };
    var set = function (x) {
      data = Option.some(x);
      run(callbacks);
      callbacks = [];
    };
    var isReady = function () {
      return data.isSome();
    };
    var run = function (cbs) {
      $_elh0pqwsje4cc0p4.each(cbs, call);
    };
    var call = function (cb) {
      data.each(function (x) {
        setTimeout(function () {
          cb(x);
        }, 0);
      });
    };
    baseFn(set);
    return {
      get: get,
      map: map,
      isReady: isReady
    };
  };
  var pure$1 = function (a) {
    return nu$8(function (callback) {
      callback(a);
    });
  };
  var LazyValue = {
    nu: nu$8,
    pure: pure$1
  };

  var bounce = function (f) {
    return function () {
      var args = Array.prototype.slice.call(arguments);
      var me = this;
      setTimeout(function () {
        f.apply(me, args);
      }, 0);
    };
  };
  var $_9bxwop15xje4cc22g = { bounce: bounce };

  var nu$9 = function (baseFn) {
    var get = function (callback) {
      baseFn($_9bxwop15xje4cc22g.bounce(callback));
    };
    var map = function (fab) {
      return nu$9(function (callback) {
        get(function (a) {
          var value = fab(a);
          callback(value);
        });
      });
    };
    var bind = function (aFutureB) {
      return nu$9(function (callback) {
        get(function (a) {
          aFutureB(a).get(callback);
        });
      });
    };
    var anonBind = function (futureB) {
      return nu$9(function (callback) {
        get(function (a) {
          futureB.get(callback);
        });
      });
    };
    var toLazy = function () {
      return LazyValue.nu(get);
    };
    return {
      map: map,
      bind: bind,
      anonBind: anonBind,
      toLazy: toLazy,
      get: get
    };
  };
  var pure$2 = function (a) {
    return nu$9(function (callback) {
      callback(a);
    });
  };
  var Future = {
    nu: nu$9,
    pure: pure$2
  };

  var adjust = function (value, destination, amount) {
    if (Math.abs(value - destination) <= amount) {
      return Option.none();
    } else if (value < destination) {
      return Option.some(value + amount);
    } else {
      return Option.some(value - amount);
    }
  };
  var create$7 = function () {
    var interval = null;
    var animate = function (getCurrent, destination, amount, increment, doFinish, rate) {
      var finished = false;
      var finish = function (v) {
        finished = true;
        doFinish(v);
      };
      clearInterval(interval);
      var abort = function (v) {
        clearInterval(interval);
        finish(v);
      };
      interval = setInterval(function () {
        var value = getCurrent();
        adjust(value, destination, amount).fold(function () {
          clearInterval(interval);
          finish(destination);
        }, function (s) {
          increment(s, abort);
          if (!finished) {
            var newValue = getCurrent();
            if (newValue !== s || Math.abs(newValue - destination) > Math.abs(value - destination)) {
              clearInterval(interval);
              finish(destination);
            }
          }
        });
      }, rate);
    };
    return { animate: animate };
  };
  var $_1a41rw15yje4cc22i = {
    create: create$7,
    adjust: adjust
  };

  var findDevice = function (deviceWidth, deviceHeight) {
    var devices = [
      {
        width: 320,
        height: 480,
        keyboard: {
          portrait: 300,
          landscape: 240
        }
      },
      {
        width: 320,
        height: 568,
        keyboard: {
          portrait: 300,
          landscape: 240
        }
      },
      {
        width: 375,
        height: 667,
        keyboard: {
          portrait: 305,
          landscape: 240
        }
      },
      {
        width: 414,
        height: 736,
        keyboard: {
          portrait: 320,
          landscape: 240
        }
      },
      {
        width: 768,
        height: 1024,
        keyboard: {
          portrait: 320,
          landscape: 400
        }
      },
      {
        width: 1024,
        height: 1366,
        keyboard: {
          portrait: 380,
          landscape: 460
        }
      }
    ];
    return $_egl8u6y0je4cc0tb.findMap(devices, function (device) {
      return deviceWidth <= device.width && deviceHeight <= device.height ? Option.some(device.keyboard) : Option.none();
    }).getOr({
      portrait: deviceHeight / 5,
      landscape: deviceWidth / 4
    });
  };
  var $_2e1em8161je4cc23n = { findDevice: findDevice };

  var softKeyboardLimits = function (outerWindow) {
    return $_2e1em8161je4cc23n.findDevice(outerWindow.screen.width, outerWindow.screen.height);
  };
  var accountableKeyboardHeight = function (outerWindow) {
    var portrait = $_9hryc13wje4cc1o0.get(outerWindow).isPortrait();
    var limits = softKeyboardLimits(outerWindow);
    var keyboard = portrait ? limits.portrait : limits.landscape;
    var visualScreenHeight = portrait ? outerWindow.screen.height : outerWindow.screen.width;
    return visualScreenHeight - outerWindow.innerHeight > keyboard ? 0 : keyboard;
  };
  var getGreenzone = function (socket, dropup) {
    var outerWindow = $_1jwy92x3je4cc0qa.owner(socket).dom().defaultView;
    var viewportHeight = $_eftfr9102je4cc11q.get(socket) + $_eftfr9102je4cc11q.get(dropup);
    var acc = accountableKeyboardHeight(outerWindow);
    return viewportHeight - acc;
  };
  var updatePadding = function (contentBody, socket, dropup) {
    var greenzoneHeight = getGreenzone(socket, dropup);
    var deltaHeight = $_eftfr9102je4cc11q.get(socket) + $_eftfr9102je4cc11q.get(dropup) - greenzoneHeight;
    $_7ojow7103je4cc11s.set(contentBody, 'padding-bottom', deltaHeight + 'px');
  };
  var $_cvv7kp160je4cc23e = {
    getGreenzone: getGreenzone,
    updatePadding: updatePadding
  };

  var fixture = $_170ozvxwje4cc0st.generate([
    {
      fixed: [
        'element',
        'property',
        'offsetY'
      ]
    },
    {
      scroller: [
        'element',
        'offsetY'
      ]
    }
  ]);
  var yFixedData = 'data-' + $_gfh4lpzeje4cc0zd.resolve('position-y-fixed');
  var yFixedProperty = 'data-' + $_gfh4lpzeje4cc0zd.resolve('y-property');
  var yScrollingData = 'data-' + $_gfh4lpzeje4cc0zd.resolve('scrolling');
  var windowSizeData = 'data-' + $_gfh4lpzeje4cc0zd.resolve('last-window-height');
  var getYFixedData = function (element) {
    return $_1cxrdw147je4cc1pz.safeParse(element, yFixedData);
  };
  var getYFixedProperty = function (element) {
    return $_bjjq6ixrje4cc0s9.get(element, yFixedProperty);
  };
  var getLastWindowSize = function (element) {
    return $_1cxrdw147je4cc1pz.safeParse(element, windowSizeData);
  };
  var classifyFixed = function (element, offsetY) {
    var prop = getYFixedProperty(element);
    return fixture.fixed(element, prop, offsetY);
  };
  var classifyScrolling = function (element, offsetY) {
    return fixture.scroller(element, offsetY);
  };
  var classify = function (element) {
    var offsetY = getYFixedData(element);
    var classifier = $_bjjq6ixrje4cc0s9.get(element, yScrollingData) === 'true' ? classifyScrolling : classifyFixed;
    return classifier(element, offsetY);
  };
  var findFixtures = function (container) {
    var candidates = $_1n1hkzzvje4cc119.descendants(container, '[' + yFixedData + ']');
    return $_elh0pqwsje4cc0p4.map(candidates, classify);
  };
  var takeoverToolbar = function (toolbar) {
    var oldToolbarStyle = $_bjjq6ixrje4cc0s9.get(toolbar, 'style');
    $_7ojow7103je4cc11s.setAll(toolbar, {
      position: 'absolute',
      top: '0px'
    });
    $_bjjq6ixrje4cc0s9.set(toolbar, yFixedData, '0px');
    $_bjjq6ixrje4cc0s9.set(toolbar, yFixedProperty, 'top');
    var restore = function () {
      $_bjjq6ixrje4cc0s9.set(toolbar, 'style', oldToolbarStyle || '');
      $_bjjq6ixrje4cc0s9.remove(toolbar, yFixedData);
      $_bjjq6ixrje4cc0s9.remove(toolbar, yFixedProperty);
    };
    return { restore: restore };
  };
  var takeoverViewport = function (toolbarHeight, height, viewport) {
    var oldViewportStyle = $_bjjq6ixrje4cc0s9.get(viewport, 'style');
    $_4d6st213uje4cc1ns.register(viewport);
    $_7ojow7103je4cc11s.setAll(viewport, {
      position: 'absolute',
      height: height + 'px',
      width: '100%',
      top: toolbarHeight + 'px'
    });
    $_bjjq6ixrje4cc0s9.set(viewport, yFixedData, toolbarHeight + 'px');
    $_bjjq6ixrje4cc0s9.set(viewport, yScrollingData, 'true');
    $_bjjq6ixrje4cc0s9.set(viewport, yFixedProperty, 'top');
    var restore = function () {
      $_4d6st213uje4cc1ns.deregister(viewport);
      $_bjjq6ixrje4cc0s9.set(viewport, 'style', oldViewportStyle || '');
      $_bjjq6ixrje4cc0s9.remove(viewport, yFixedData);
      $_bjjq6ixrje4cc0s9.remove(viewport, yScrollingData);
      $_bjjq6ixrje4cc0s9.remove(viewport, yFixedProperty);
    };
    return { restore: restore };
  };
  var takeoverDropup = function (dropup, toolbarHeight, viewportHeight) {
    var oldDropupStyle = $_bjjq6ixrje4cc0s9.get(dropup, 'style');
    $_7ojow7103je4cc11s.setAll(dropup, {
      position: 'absolute',
      bottom: '0px'
    });
    $_bjjq6ixrje4cc0s9.set(dropup, yFixedData, '0px');
    $_bjjq6ixrje4cc0s9.set(dropup, yFixedProperty, 'bottom');
    var restore = function () {
      $_bjjq6ixrje4cc0s9.set(dropup, 'style', oldDropupStyle || '');
      $_bjjq6ixrje4cc0s9.remove(dropup, yFixedData);
      $_bjjq6ixrje4cc0s9.remove(dropup, yFixedProperty);
    };
    return { restore: restore };
  };
  var deriveViewportHeight = function (viewport, toolbarHeight, dropupHeight) {
    var outerWindow = $_1jwy92x3je4cc0qa.owner(viewport).dom().defaultView;
    var winH = outerWindow.innerHeight;
    $_bjjq6ixrje4cc0s9.set(viewport, windowSizeData, winH + 'px');
    return winH - toolbarHeight - dropupHeight;
  };
  var takeover$1 = function (viewport, contentBody, toolbar, dropup) {
    var outerWindow = $_1jwy92x3je4cc0qa.owner(viewport).dom().defaultView;
    var toolbarSetup = takeoverToolbar(toolbar);
    var toolbarHeight = $_eftfr9102je4cc11q.get(toolbar);
    var dropupHeight = $_eftfr9102je4cc11q.get(dropup);
    var viewportHeight = deriveViewportHeight(viewport, toolbarHeight, dropupHeight);
    var viewportSetup = takeoverViewport(toolbarHeight, viewportHeight, viewport);
    var dropupSetup = takeoverDropup(dropup, toolbarHeight, viewportHeight);
    var isActive = true;
    var restore = function () {
      isActive = false;
      toolbarSetup.restore();
      viewportSetup.restore();
      dropupSetup.restore();
    };
    var isExpanding = function () {
      var currentWinHeight = outerWindow.innerHeight;
      var lastWinHeight = getLastWindowSize(viewport);
      return currentWinHeight > lastWinHeight;
    };
    var refresh = function () {
      if (isActive) {
        var newToolbarHeight = $_eftfr9102je4cc11q.get(toolbar);
        var dropupHeight_1 = $_eftfr9102je4cc11q.get(dropup);
        var newHeight = deriveViewportHeight(viewport, newToolbarHeight, dropupHeight_1);
        $_bjjq6ixrje4cc0s9.set(viewport, yFixedData, newToolbarHeight + 'px');
        $_7ojow7103je4cc11s.set(viewport, 'height', newHeight + 'px');
        $_7ojow7103je4cc11s.set(dropup, 'bottom', -(newToolbarHeight + newHeight + dropupHeight_1) + 'px');
        $_cvv7kp160je4cc23e.updatePadding(contentBody, viewport, dropup);
      }
    };
    var setViewportOffset = function (newYOffset) {
      var offsetPx = newYOffset + 'px';
      $_bjjq6ixrje4cc0s9.set(viewport, yFixedData, offsetPx);
      refresh();
    };
    $_cvv7kp160je4cc23e.updatePadding(contentBody, viewport, dropup);
    return {
      setViewportOffset: setViewportOffset,
      isExpanding: isExpanding,
      isShrinking: $_aso7c6wjje4cc0om.not(isExpanding),
      refresh: refresh,
      restore: restore
    };
  };
  var $_f1hghx15zje4cc22n = {
    findFixtures: findFixtures,
    takeover: takeover$1,
    getYFixedData: getYFixedData
  };

  var animator = $_1a41rw15yje4cc22i.create();
  var ANIMATION_STEP = 15;
  var NUM_TOP_ANIMATION_FRAMES = 10;
  var ANIMATION_RATE = 10;
  var lastScroll = 'data-' + $_gfh4lpzeje4cc0zd.resolve('last-scroll-top');
  var getTop = function (element) {
    var raw = $_7ojow7103je4cc11s.getRaw(element, 'top').getOr(0);
    return parseInt(raw, 10);
  };
  var getScrollTop = function (element) {
    return parseInt(element.dom().scrollTop, 10);
  };
  var moveScrollAndTop = function (element, destination, finalTop) {
    return Future.nu(function (callback) {
      var getCurrent = $_aso7c6wjje4cc0om.curry(getScrollTop, element);
      var update = function (newScroll) {
        element.dom().scrollTop = newScroll;
        $_7ojow7103je4cc11s.set(element, 'top', getTop(element) + ANIMATION_STEP + 'px');
      };
      var finish = function () {
        element.dom().scrollTop = destination;
        $_7ojow7103je4cc11s.set(element, 'top', finalTop + 'px');
        callback(destination);
      };
      animator.animate(getCurrent, destination, ANIMATION_STEP, update, finish, ANIMATION_RATE);
    });
  };
  var moveOnlyScroll = function (element, destination) {
    return Future.nu(function (callback) {
      var getCurrent = $_aso7c6wjje4cc0om.curry(getScrollTop, element);
      $_bjjq6ixrje4cc0s9.set(element, lastScroll, getCurrent());
      var update = function (newScroll, abort) {
        var previous = $_1cxrdw147je4cc1pz.safeParse(element, lastScroll);
        if (previous !== element.dom().scrollTop) {
          abort(element.dom().scrollTop);
        } else {
          element.dom().scrollTop = newScroll;
          $_bjjq6ixrje4cc0s9.set(element, lastScroll, newScroll);
        }
      };
      var finish = function () {
        element.dom().scrollTop = destination;
        $_bjjq6ixrje4cc0s9.set(element, lastScroll, destination);
        callback(destination);
      };
      var distance = Math.abs(destination - getCurrent());
      var step = Math.ceil(distance / NUM_TOP_ANIMATION_FRAMES);
      animator.animate(getCurrent, destination, step, update, finish, ANIMATION_RATE);
    });
  };
  var moveOnlyTop = function (element, destination) {
    return Future.nu(function (callback) {
      var getCurrent = $_aso7c6wjje4cc0om.curry(getTop, element);
      var update = function (newTop) {
        $_7ojow7103je4cc11s.set(element, 'top', newTop + 'px');
      };
      var finish = function () {
        update(destination);
        callback(destination);
      };
      var distance = Math.abs(destination - getCurrent());
      var step = Math.ceil(distance / NUM_TOP_ANIMATION_FRAMES);
      animator.animate(getCurrent, destination, step, update, finish, ANIMATION_RATE);
    });
  };
  var updateTop = function (element, amount) {
    var newTop = amount + $_f1hghx15zje4cc22n.getYFixedData(element) + 'px';
    $_7ojow7103je4cc11s.set(element, 'top', newTop);
  };
  var moveWindowScroll = function (toolbar, viewport, destY) {
    var outerWindow = $_1jwy92x3je4cc0qa.owner(toolbar).dom().defaultView;
    return Future.nu(function (callback) {
      updateTop(toolbar, destY);
      updateTop(viewport, destY);
      outerWindow.scrollTo(0, destY);
      callback(destY);
    });
  };
  var $_26bqw515uje4cc220 = {
    moveScrollAndTop: moveScrollAndTop,
    moveOnlyScroll: moveOnlyScroll,
    moveOnlyTop: moveOnlyTop,
    moveWindowScroll: moveWindowScroll
  };

  function BackgroundActivity (doAction) {
    var action = Cell(LazyValue.pure({}));
    var start = function (value) {
      var future = LazyValue.nu(function (callback) {
        return doAction(value).get(callback);
      });
      action.set(future);
    };
    var idle = function (g) {
      action.get().get(function () {
        g();
      });
    };
    return {
      start: start,
      idle: idle
    };
  }

  var scrollIntoView = function (cWin, socket, dropup, top, bottom) {
    var greenzone = $_cvv7kp160je4cc23e.getGreenzone(socket, dropup);
    var refreshCursor = $_aso7c6wjje4cc0om.curry($_24urki15qje4cc20k.refresh, cWin);
    if (top > greenzone || bottom > greenzone) {
      $_26bqw515uje4cc220.moveOnlyScroll(socket, socket.dom().scrollTop - greenzone + bottom).get(refreshCursor);
    } else if (top < 0) {
      $_26bqw515uje4cc220.moveOnlyScroll(socket, socket.dom().scrollTop + top).get(refreshCursor);
    } else {
    }
  };
  var $_3mb8xa163je4cc23x = { scrollIntoView: scrollIntoView };

  var par = function (asyncValues, nu) {
    return nu(function (callback) {
      var r = [];
      var count = 0;
      var cb = function (i) {
        return function (value) {
          r[i] = value;
          count++;
          if (count >= asyncValues.length) {
            callback(r);
          }
        };
      };
      if (asyncValues.length === 0) {
        callback([]);
      } else {
        $_elh0pqwsje4cc0p4.each(asyncValues, function (asyncValue, i) {
          asyncValue.get(cb(i));
        });
      }
    });
  };
  var $_5msl9t166je4cc24c = { par: par };

  var par$1 = function (futures) {
    return $_5msl9t166je4cc24c.par(futures, Future.nu);
  };
  var mapM = function (array, fn) {
    var futures = $_elh0pqwsje4cc0p4.map(array, fn);
    return par$1(futures);
  };
  var compose$1 = function (f, g) {
    return function (a) {
      return g(a).bind(f);
    };
  };
  var $_ebtaf6165je4cc249 = {
    par: par$1,
    mapM: mapM,
    compose: compose$1
  };

  var updateFixed = function (element, property, winY, offsetY) {
    var destination = winY + offsetY;
    $_7ojow7103je4cc11s.set(element, property, destination + 'px');
    return Future.pure(offsetY);
  };
  var updateScrollingFixed = function (element, winY, offsetY) {
    var destTop = winY + offsetY;
    var oldProp = $_7ojow7103je4cc11s.getRaw(element, 'top').getOr(offsetY);
    var delta = destTop - parseInt(oldProp, 10);
    var destScroll = element.dom().scrollTop + delta;
    return $_26bqw515uje4cc220.moveScrollAndTop(element, destScroll, destTop);
  };
  var updateFixture = function (fixture, winY) {
    return fixture.fold(function (element, property, offsetY) {
      return updateFixed(element, property, winY, offsetY);
    }, function (element, offsetY) {
      return updateScrollingFixed(element, winY, offsetY);
    });
  };
  var updatePositions = function (container, winY) {
    var fixtures = $_f1hghx15zje4cc22n.findFixtures(container);
    var updates = $_elh0pqwsje4cc0p4.map(fixtures, function (fixture) {
      return updateFixture(fixture, winY);
    });
    return $_ebtaf6165je4cc249.par(updates);
  };
  var $_ydemc164je4cc241 = { updatePositions: updatePositions };

  var VIEW_MARGIN = 5;
  var register$2 = function (toolstrip, socket, container, outerWindow, structure, cWin) {
    var scroller = BackgroundActivity(function (y) {
      return $_26bqw515uje4cc220.moveWindowScroll(toolstrip, socket, y);
    });
    var scrollBounds = function () {
      var rects = $_c6xmxq148je4cc1q1.getRectangles(cWin);
      return Option.from(rects[0]).bind(function (rect) {
        var viewTop = rect.top() - socket.dom().scrollTop;
        var outside = viewTop > outerWindow.innerHeight + VIEW_MARGIN || viewTop < -VIEW_MARGIN;
        return outside ? Option.some({
          top: $_aso7c6wjje4cc0om.constant(viewTop),
          bottom: $_aso7c6wjje4cc0om.constant(viewTop + rect.height())
        }) : Option.none();
      });
    };
    var scrollThrottle = $_703q9814wje4cc1t9.last(function () {
      scroller.idle(function () {
        $_ydemc164je4cc241.updatePositions(container, outerWindow.pageYOffset).get(function () {
          var extraScroll = scrollBounds();
          extraScroll.each(function (extra) {
            socket.dom().scrollTop = socket.dom().scrollTop + extra.top();
          });
          scroller.start(0);
          structure.refresh();
        });
      });
    }, 1000);
    var onScroll = $_24b4y113xje4cc1o6.bind($_407ejqxfje4cc0rb.fromDom(outerWindow), 'scroll', function () {
      if (outerWindow.pageYOffset < 0) {
        return;
      }
      scrollThrottle.throttle();
    });
    $_ydemc164je4cc241.updatePositions(container, outerWindow.pageYOffset).get($_aso7c6wjje4cc0om.identity);
    return { unbind: onScroll.unbind };
  };
  var setup$3 = function (bag) {
    var cWin = bag.cWin();
    var ceBody = bag.ceBody();
    var socket = bag.socket();
    var toolstrip = bag.toolstrip();
    var toolbar = bag.toolbar();
    var contentElement = bag.contentElement();
    var keyboardType = bag.keyboardType();
    var outerWindow = bag.outerWindow();
    var dropup = bag.dropup();
    var structure = $_f1hghx15zje4cc22n.takeover(socket, ceBody, toolstrip, dropup);
    var keyboardModel = keyboardType(bag.outerBody(), cWin, $_6jf42xxjje4cc0rm.body(), contentElement, toolstrip, toolbar);
    var toEditing = function () {
      keyboardModel.toEditing();
      clearSelection();
    };
    var toReading = function () {
      keyboardModel.toReading();
    };
    var onToolbarTouch = function (event) {
      keyboardModel.onToolbarTouch(event);
    };
    var onOrientation = $_9hryc13wje4cc1o0.onChange(outerWindow, {
      onChange: $_aso7c6wjje4cc0om.noop,
      onReady: structure.refresh
    });
    onOrientation.onAdjustment(function () {
      structure.refresh();
    });
    var onResize = $_24b4y113xje4cc1o6.bind($_407ejqxfje4cc0rb.fromDom(outerWindow), 'resize', function () {
      if (structure.isExpanding()) {
        structure.refresh();
      }
    });
    var onScroll = register$2(toolstrip, socket, bag.outerBody(), outerWindow, structure, cWin);
    var unfocusedSelection = FakeSelection(cWin, contentElement);
    var refreshSelection = function () {
      if (unfocusedSelection.isActive()) {
        unfocusedSelection.update();
      }
    };
    var highlightSelection = function () {
      unfocusedSelection.update();
    };
    var clearSelection = function () {
      unfocusedSelection.clear();
    };
    var scrollIntoView = function (top, bottom) {
      $_3mb8xa163je4cc23x.scrollIntoView(cWin, socket, dropup, top, bottom);
    };
    var syncHeight = function () {
      $_7ojow7103je4cc11s.set(contentElement, 'height', contentElement.dom().contentWindow.document.body.scrollHeight + 'px');
    };
    var setViewportOffset = function (newYOffset) {
      structure.setViewportOffset(newYOffset);
      $_26bqw515uje4cc220.moveOnlyTop(socket, newYOffset).get($_aso7c6wjje4cc0om.identity);
    };
    var destroy = function () {
      structure.restore();
      onOrientation.destroy();
      onScroll.unbind();
      onResize.unbind();
      keyboardModel.destroy();
      unfocusedSelection.destroy();
      $_vlzgx15oje4cc205.input($_6jf42xxjje4cc0rm.body(), $_4hvdjzytje4cc0wz.blur);
    };
    return {
      toEditing: toEditing,
      toReading: toReading,
      onToolbarTouch: onToolbarTouch,
      refreshSelection: refreshSelection,
      clearSelection: clearSelection,
      highlightSelection: highlightSelection,
      scrollIntoView: scrollIntoView,
      updateToolbarPadding: $_aso7c6wjje4cc0om.noop,
      setViewportOffset: setViewportOffset,
      syncHeight: syncHeight,
      refreshStructure: structure.refresh,
      destroy: destroy
    };
  };
  var $_1ua6bu15sje4cc211 = { setup: setup$3 };

  var create$8 = function (platform, mask) {
    var meta = $_6it1w14uje4cc1sx.tag();
    var priorState = $_7qzgbo12oje4cc1fr.value();
    var scrollEvents = $_7qzgbo12oje4cc1fr.value();
    var iosApi = $_7qzgbo12oje4cc1fr.api();
    var iosEvents = $_7qzgbo12oje4cc1fr.api();
    var enter = function () {
      mask.hide();
      var doc = $_407ejqxfje4cc0rb.fromDom(document);
      $_e05b014sje4cc1sc.getActiveApi(platform.editor).each(function (editorApi) {
        priorState.set({
          socketHeight: $_7ojow7103je4cc11s.getRaw(platform.socket, 'height'),
          iframeHeight: $_7ojow7103je4cc11s.getRaw(editorApi.frame(), 'height'),
          outerScroll: document.body.scrollTop
        });
        scrollEvents.set({ exclusives: $_82wif6153je4cc1ua.exclusive(doc, '.' + $_4d6st213uje4cc1ns.scrollable()) });
        $_3x5q3zynje4cc0wi.add(platform.container, $_gfh4lpzeje4cc0zd.resolve('fullscreen-maximized'));
        $_aa7o5s14tje4cc1sk.clobberStyles(platform.container, editorApi.body());
        meta.maximize();
        $_7ojow7103je4cc11s.set(platform.socket, 'overflow', 'scroll');
        $_7ojow7103je4cc11s.set(platform.socket, '-webkit-overflow-scrolling', 'touch');
        $_4hvdjzytje4cc0wz.focus(editorApi.body());
        var setupBag = $_ajn35gx4je4cc0qj.immutableBag([
          'cWin',
          'ceBody',
          'socket',
          'toolstrip',
          'toolbar',
          'dropup',
          'contentElement',
          'cursor',
          'keyboardType',
          'isScrolling',
          'outerWindow',
          'outerBody'
        ], []);
        iosApi.set($_1ua6bu15sje4cc211.setup(setupBag({
          cWin: editorApi.win(),
          ceBody: editorApi.body(),
          socket: platform.socket,
          toolstrip: platform.toolstrip,
          toolbar: platform.toolbar,
          dropup: platform.dropup.element(),
          contentElement: editorApi.frame(),
          cursor: $_aso7c6wjje4cc0om.noop,
          outerBody: platform.body,
          outerWindow: platform.win,
          keyboardType: $_chauq615nje4cc1zo.stubborn,
          isScrolling: function () {
            return scrollEvents.get().exists(function (s) {
              return s.socket.isScrolling();
            });
          }
        })));
        iosApi.run(function (api) {
          api.syncHeight();
        });
        iosEvents.set($_69zea915rje4cc20o.initEvents(editorApi, iosApi, platform.toolstrip, platform.socket, platform.dropup));
      });
    };
    var exit = function () {
      meta.restore();
      iosEvents.clear();
      iosApi.clear();
      mask.show();
      priorState.on(function (s) {
        s.socketHeight.each(function (h) {
          $_7ojow7103je4cc11s.set(platform.socket, 'height', h);
        });
        s.iframeHeight.each(function (h) {
          $_7ojow7103je4cc11s.set(platform.editor.getFrame(), 'height', h);
        });
        document.body.scrollTop = s.scrollTop;
      });
      priorState.clear();
      scrollEvents.on(function (s) {
        s.exclusives.unbind();
      });
      scrollEvents.clear();
      $_3x5q3zynje4cc0wi.remove(platform.container, $_gfh4lpzeje4cc0zd.resolve('fullscreen-maximized'));
      $_aa7o5s14tje4cc1sk.restoreStyles();
      $_4d6st213uje4cc1ns.deregister(platform.toolbar);
      $_7ojow7103je4cc11s.remove(platform.socket, 'overflow');
      $_7ojow7103je4cc11s.remove(platform.socket, '-webkit-overflow-scrolling');
      $_4hvdjzytje4cc0wz.blur(platform.editor.getFrame());
      $_e05b014sje4cc1sc.getActiveApi(platform.editor).each(function (editorApi) {
        editorApi.clearSelection();
      });
    };
    var refreshStructure = function () {
      iosApi.run(function (api) {
        api.refreshStructure();
      });
    };
    return {
      enter: enter,
      refreshStructure: refreshStructure,
      exit: exit
    };
  };
  var $_10yzrg15mje4cc1z9 = { create: create$8 };

  var produce$1 = function (raw) {
    var mobile = $_lbfbgyeje4cc0vl.asRawOrDie('Getting IosWebapp schema', MobileSchema, raw);
    $_7ojow7103je4cc11s.set(mobile.toolstrip, 'width', '100%');
    $_7ojow7103je4cc11s.set(mobile.container, 'position', 'relative');
    var onView = function () {
      mobile.setReadOnly(true);
      mode.enter();
    };
    var mask = $_ft6ka112tje4cc1gu.build($_djv5li14vje4cc1t2.sketch(onView, mobile.translate));
    mobile.alloy.add(mask);
    var maskApi = {
      show: function () {
        mobile.alloy.add(mask);
      },
      hide: function () {
        mobile.alloy.remove(mask);
      }
    };
    var mode = $_10yzrg15mje4cc1z9.create(mobile, maskApi);
    return {
      setReadOnly: mobile.setReadOnly,
      refreshStructure: mode.refreshStructure,
      enter: mode.enter,
      exit: mode.exit,
      destroy: $_aso7c6wjje4cc0om.noop
    };
  };
  var $_9ebwdu15lje4cc1z1 = { produce: produce$1 };

  function IosRealm (scrollIntoView) {
    var alloy = OuterContainer({ classes: [$_gfh4lpzeje4cc0zd.resolve('ios-container')] });
    var toolbar = ScrollingToolbar();
    var webapp = $_7qzgbo12oje4cc1fr.api();
    var switchToEdit = $_1hef6n154je4cc1um.makeEditSwitch(webapp);
    var socket = $_1hef6n154je4cc1um.makeSocket();
    var dropup = $_1rw6u155je4cc1ut.build(function () {
      webapp.run(function (w) {
        w.refreshStructure();
      });
    }, scrollIntoView);
    alloy.add(toolbar.wrapper());
    alloy.add(socket);
    alloy.add(dropup.component());
    var setToolbarGroups = function (rawGroups) {
      var groups = toolbar.createGroups(rawGroups);
      toolbar.setGroups(groups);
    };
    var setContextToolbar = function (rawGroups) {
      var groups = toolbar.createGroups(rawGroups);
      toolbar.setContextToolbar(groups);
    };
    var focusToolbar = function () {
      toolbar.focus();
    };
    var restoreToolbar = function () {
      toolbar.restoreToolbar();
    };
    var init = function (spec) {
      webapp.set($_9ebwdu15lje4cc1z1.produce(spec));
    };
    var exit = function () {
      webapp.run(function (w) {
        Replacing.remove(socket, switchToEdit);
        w.exit();
      });
    };
    var updateMode = function (readOnly) {
      $_1hef6n154je4cc1um.updateMode(socket, switchToEdit, readOnly, alloy.root());
    };
    return {
      system: $_aso7c6wjje4cc0om.constant(alloy),
      element: alloy.element,
      init: init,
      exit: exit,
      setToolbarGroups: setToolbarGroups,
      setContextToolbar: setContextToolbar,
      focusToolbar: focusToolbar,
      restoreToolbar: restoreToolbar,
      updateMode: updateMode,
      socket: $_aso7c6wjje4cc0om.constant(socket),
      dropup: $_aso7c6wjje4cc0om.constant(dropup)
    };
  }

  var EditorManager = tinymce.util.Tools.resolve('tinymce.EditorManager');

  var derive$4 = function (editor) {
    var base = $_d08hr1xsje4cc0sg.readOptFrom(editor.settings, 'skin_url').fold(function () {
      return EditorManager.baseURL + '/skins/' + 'lightgray';
    }, function (url) {
      return url;
    });
    return {
      content: base + '/content.mobile.min.css',
      ui: base + '/skin.mobile.min.css'
    };
  };
  var $_5nhwnz167je4cc24f = { derive: derive$4 };

  var fontSizes = [
    'x-small',
    'small',
    'medium',
    'large',
    'x-large'
  ];
  var fireChange$1 = function (realm, command, state) {
    realm.system().broadcastOn([$_2txnauz1je4cc0xi.formatChanged()], {
      command: command,
      state: state
    });
  };
  var init$5 = function (realm, editor) {
    var allFormats = $_i587hx0je4cc0pw.keys(editor.formatter.get());
    $_elh0pqwsje4cc0p4.each(allFormats, function (command) {
      editor.formatter.formatChanged(command, function (state) {
        fireChange$1(realm, command, state);
      });
    });
    $_elh0pqwsje4cc0p4.each([
      'ul',
      'ol'
    ], function (command) {
      editor.selection.selectorChanged(command, function (state, data) {
        fireChange$1(realm, command, state);
      });
    });
  };
  var $_ftmdaq169je4cc24i = {
    init: init$5,
    fontSizes: $_aso7c6wjje4cc0om.constant(fontSizes)
  };

  var fireSkinLoaded = function (editor) {
    var done = function () {
      editor._skinLoaded = true;
      editor.fire('SkinLoaded');
    };
    return function () {
      if (editor.initialized) {
        done();
      } else {
        editor.on('init', done);
      }
    };
  };
  var $_4wvnim16aje4cc24o = { fireSkinLoaded: fireSkinLoaded };

  var READING = $_aso7c6wjje4cc0om.constant('toReading');
  var EDITING = $_aso7c6wjje4cc0om.constant('toEditing');
  ThemeManager.add('mobile', function (editor) {
    var renderUI = function (args) {
      var cssUrls = $_5nhwnz167je4cc24f.derive(editor);
      if ($_daxuk0z0je4cc0xh.isSkinDisabled(editor) === false) {
        editor.contentCSS.push(cssUrls.content);
        DOMUtils.DOM.styleSheetLoader.load(cssUrls.ui, $_4wvnim16aje4cc24o.fireSkinLoaded(editor));
      } else {
        $_4wvnim16aje4cc24o.fireSkinLoaded(editor)();
      }
      var doScrollIntoView = function () {
        editor.fire('scrollIntoView');
      };
      var wrapper = $_407ejqxfje4cc0rb.fromTag('div');
      var realm = $_2j4x3gwkje4cc0oo.detect().os.isAndroid() ? AndroidRealm(doScrollIntoView) : IosRealm(doScrollIntoView);
      var original = $_407ejqxfje4cc0rb.fromDom(args.targetNode);
      $_9zp36ax2je4cc0q8.after(original, wrapper);
      $_b576ucx1je4cc0py.attachSystem(wrapper, realm.system());
      var findFocusIn = function (elem) {
        return $_4hvdjzytje4cc0wz.search(elem).bind(function (focused) {
          return realm.system().getByDom(focused).toOption();
        });
      };
      var outerWindow = args.targetNode.ownerDocument.defaultView;
      var orientation = $_9hryc13wje4cc1o0.onChange(outerWindow, {
        onChange: function () {
          var alloy = realm.system();
          alloy.broadcastOn([$_2txnauz1je4cc0xi.orientationChanged()], { width: $_9hryc13wje4cc1o0.getActualWidth(outerWindow) });
        },
        onReady: $_aso7c6wjje4cc0om.noop
      });
      var setReadOnly = function (readOnlyGroups, mainGroups, ro) {
        if (ro === false) {
          editor.selection.collapse();
        }
        realm.setToolbarGroups(ro ? readOnlyGroups.get() : mainGroups.get());
        editor.setMode(ro === true ? 'readonly' : 'design');
        editor.fire(ro === true ? READING() : EDITING());
        realm.updateMode(ro);
      };
      var bindHandler = function (label, handler) {
        editor.on(label, handler);
        return {
          unbind: function () {
            editor.off(label);
          }
        };
      };
      editor.on('init', function () {
        realm.init({
          editor: {
            getFrame: function () {
              return $_407ejqxfje4cc0rb.fromDom(editor.contentAreaContainer.querySelector('iframe'));
            },
            onDomChanged: function () {
              return { unbind: $_aso7c6wjje4cc0om.noop };
            },
            onToReading: function (handler) {
              return bindHandler(READING(), handler);
            },
            onToEditing: function (handler) {
              return bindHandler(EDITING(), handler);
            },
            onScrollToCursor: function (handler) {
              editor.on('scrollIntoView', function (tinyEvent) {
                handler(tinyEvent);
              });
              var unbind = function () {
                editor.off('scrollIntoView');
                orientation.destroy();
              };
              return { unbind: unbind };
            },
            onTouchToolstrip: function () {
              hideDropup();
            },
            onTouchContent: function () {
              var toolbar = $_407ejqxfje4cc0rb.fromDom(editor.editorContainer.querySelector('.' + $_gfh4lpzeje4cc0zd.resolve('toolbar')));
              findFocusIn(toolbar).each($_3fionhwgje4cc0o9.emitExecute);
              realm.restoreToolbar();
              hideDropup();
            },
            onTapContent: function (evt) {
              var target = evt.target();
              if ($_7qwxg2xkje4cc0ro.name(target) === 'img') {
                editor.selection.select(target.dom());
                evt.kill();
              } else if ($_7qwxg2xkje4cc0ro.name(target) === 'a') {
                var component = realm.system().getByDom($_407ejqxfje4cc0rb.fromDom(editor.editorContainer));
                component.each(function (container) {
                  if (Swapping.isAlpha(container)) {
                    $_1cg62byzje4cc0xg.openLink(target.dom());
                  }
                });
              }
            }
          },
          container: $_407ejqxfje4cc0rb.fromDom(editor.editorContainer),
          socket: $_407ejqxfje4cc0rb.fromDom(editor.contentAreaContainer),
          toolstrip: $_407ejqxfje4cc0rb.fromDom(editor.editorContainer.querySelector('.' + $_gfh4lpzeje4cc0zd.resolve('toolstrip'))),
          toolbar: $_407ejqxfje4cc0rb.fromDom(editor.editorContainer.querySelector('.' + $_gfh4lpzeje4cc0zd.resolve('toolbar'))),
          dropup: realm.dropup(),
          alloy: realm.system(),
          translate: $_aso7c6wjje4cc0om.noop,
          setReadOnly: function (ro) {
            setReadOnly(readOnlyGroups, mainGroups, ro);
          }
        });
        var hideDropup = function () {
          realm.dropup().disappear(function () {
            realm.system().broadcastOn([$_2txnauz1je4cc0xi.dropupDismissed()], {});
          });
        };
        $_g603ovxlje4cc0rq.registerInspector('remove this', realm.system());
        var backToMaskGroup = {
          label: 'The first group',
          scrollable: false,
          items: [$_afg6uzzfje4cc0zf.forToolbar('back', function () {
              editor.selection.collapse();
              realm.exit();
            }, {})]
        };
        var backToReadOnlyGroup = {
          label: 'Back to read only',
          scrollable: false,
          items: [$_afg6uzzfje4cc0zf.forToolbar('readonly-back', function () {
              setReadOnly(readOnlyGroups, mainGroups, true);
            }, {})]
        };
        var readOnlyGroup = {
          label: 'The read only mode group',
          scrollable: true,
          items: []
        };
        var features = $_1zl9q0z2je4cc0xk.setup(realm, editor);
        var items = $_1zl9q0z2je4cc0xk.detect(editor.settings, features);
        var actionGroup = {
          label: 'the action group',
          scrollable: true,
          items: items
        };
        var extraGroup = {
          label: 'The extra group',
          scrollable: false,
          items: []
        };
        var mainGroups = Cell([
          backToReadOnlyGroup,
          actionGroup,
          extraGroup
        ]);
        var readOnlyGroups = Cell([
          backToMaskGroup,
          readOnlyGroup,
          extraGroup
        ]);
        $_ftmdaq169je4cc24i.init(realm, editor);
      });
      return {
        iframeContainer: realm.socket().element().dom(),
        editorContainer: realm.element().dom()
      };
    };
    return {
      getNotificationManagerImpl: function () {
        return {
          open: $_aso7c6wjje4cc0om.identity,
          close: $_aso7c6wjje4cc0om.noop,
          reposition: $_aso7c6wjje4cc0om.noop,
          getArgs: $_aso7c6wjje4cc0om.identity
        };
      },
      renderUI: renderUI
    };
  });
  function Theme () {
  }

  return Theme;

}());
})();
