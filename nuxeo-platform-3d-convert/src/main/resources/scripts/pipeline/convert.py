if args.outdir == None:
    sys.exit()

outfile = args.outdir + "/conversion-" + str(lod) + ".dae"

# get the meshes
meshes = [obj for obj in bpy.data.objects if obj.type == 'MESH']

print("Found %d meshes" % len(meshes))

# process the input filename
out_dirpath, out_basename = os.path.split(outfile)
out_basename, out_ext = os.path.splitext(out_basename)
out_ext = out_ext.lower()
print("Exporting %s " % (out_ext))
if out_ext.startswith("."):
    out_ext = out_ext[1:]

if out_ext == 'stl':
    print("Exporting STL")
    # export an stl model
    bpy.ops.export_mesh.stl(filepath=outfile)

elif out_ext == 'obj':
    print("Exporting obj")
    # export an obj model
    bpy.ops.export_scene.obj(filepath=outfile, axis_forward='-Z', axis_up='Y')

elif out_ext == 'dae':
    print("EXporting COLLADA")
    # export a collada model
    bpy.ops.wm.collada_export(filepath=outfile)
elif out_ext == 'gltf':
    print("Exporting glTF")
    scene = {
        'actions': bpy.data.actions,
        'camera': bpy.data.cameras,
        'lamps': bpy.data.lamps,
        'images': bpy.data.images,
        'materials': bpy.data.materials,
        'meshes': bpy.data.meshes,
        'objects': bpy.data.objects,
        'scenes': bpy.data.scenes,
        'textures': bpy.data.textures,
    }
    # Copy properties to settings
    settings = blendergltf.default_settings.copy()
    # settings['materials_export_shader'] = BoolProperty(name='Export Shaders', default=False)
    # settings['images_embed_data'] = BoolProperty(name='Embed Image Data', default=False)

    gltf = blendergltf.export_gltf(scene, settings)
    with open(outfile, 'w') as fout:
        json.dump(gltf, fout, indent=4, sort_keys=True, check_circular=False)
