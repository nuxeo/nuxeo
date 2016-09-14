def remove_doubles():
    scene = bpy.context.scene
    for obj in scene.objects:
        if obj.type == 'MESH':
            scene.objects.active = obj
            bpy.ops.object.mode_set(mode='EDIT')
            bpy.ops.mesh.remove_doubles()
            bpy.ops.object.mode_set(mode='OBJECT')


def triangulate_meshes():
    for obj in bpy.context.scene.objects:
        if obj.type == 'MESH':
            bm = bmesh.new()
            bm.from_mesh(obj.data)
            bmesh.ops.triangulate(bm, faces=bm.faces)
            bm.to_mesh(obj.data)
            bm.free()


def clean_non_manifold():
    scene = bpy.context.scene
    for obj in scene.objects:
        if obj.type == 'MESH':
            scene.objects.active = obj
            try:
                bpy.ops.mesh.print3d_clean_non_manifold()
            except AttributeError:
                print('ERROR: 3D Print Toolbox addon is not installed. Cannot not clean non-manifold geometries.')

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
