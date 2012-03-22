What we are testing here:

For a package containing one command
1. Validate, Install then Uninstall of a package (each command in the install script should be tested)
2. Validate for initial conditions that will generate errors
3. Rollback when a command is failing.

To tests this - a CommandTester implementation will be used for each command class.

A CommandTest must implement:

// get an instance of the command to test
Command getCommand();

// update the package that will be used with resources needed by the command
updatePackage(PackageBuilder pkg);

For a package containing several commands
1. Validate, Install then Uninstall
2. Rollback at the middle of the install chain
3. Install then modify the install to have an uninstall that cannot be performed (validate on uninstall)



