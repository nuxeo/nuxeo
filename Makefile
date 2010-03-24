clean:
	find . "(" -name "*~" -or -name "*.orig" -or -name "*.rej" ")" -print0 | xargs -0 rm -f
	rm -rf */target */*/target */*/*/target

