# Clean the scene
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete()

if args.input == None:
    sys.exit()

infile = args.input
# process the input filename
dirpath, basename = os.path.split(infile)
basename, ext = os.path.splitext(basename)
ext = ext.lower()

print("Importing %s " % (ext))
if ext.startswith("."):
    ext = ext[1:]

if ext == 'dae':
    bpy.ops.wm.collada_import(filepath=infile)
elif ext == '3ds':
    bpy.ops.import_scene.autodesk_3ds(filepath=infile)
elif ext == 'fbx':
    bpy.ops.import_scene.fbx(filepath=infile)
elif ext == 'ply':
    bpy.ops.import_mesh.ply(filepath=infile)
elif ext == 'obj':
    bpy.ops.import_scene.obj(filepath=infile)
elif ext == 'x3d':
    bpy.ops.import_scene.x3d(filepath=infile)
elif ext == 'stl':
    bpy.ops.import_mesh.stl(filepath=infile)

# triangulate each mesh in the scene
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        bm = bmesh.new()
        bm.from_mesh(ob.data)
        bmesh.ops.triangulate(bm, faces=bm.faces)
        bm.to_mesh(ob.data)
        bm.free()