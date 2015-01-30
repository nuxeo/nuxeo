var m = mapper.callMe({
  'p1': 1,
  'p2': 'str',
  'p3': [1, 2, {
    'a': 1,
    'b': 2
  }]
});

print(m.p1);
print(m["p1"]);
print(m.p2);
print(m.p3);
print(m.p4);

if (typeof m.p1 != 'string') {
  throw "Marshaling Error";
}
if (typeof m.p2 != 'number') {
  throw "Marshaling Error";
}
if (!m.p3 instanceof Array) {
  throw "Marshaling Error";
}
if (typeof m.p4 != 'object') {
  throw "Marshaling Error";
}

print("done");