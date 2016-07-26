
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

if ext == 'stl':
    # import an stl model
    bpy.ops.import_mesh.stl(filepath=infile)

elif ext == 'obj':
    # import an obj model
    bpy.ops.import_scene.obj(
        filepath=infile,
        use_smooth_groups=False,
        use_image_search=False,
        axis_forward="Y",
        axis_up="Z")

elif ext == 'dae':
    print("Importing COLLADA")
    # import a collada model
    bpy.ops.wm.collada_import(filepath=infile)
