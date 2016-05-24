# A quick and temporary Makefile, for frequent tasks during Syncbase Java API
# development.

SHELL := /bin/bash -euo pipefail

# Note, javadoc complains about the self-closing <p/> tags that Android Studio
# creates.
javadoc:
	javadoc -d /tmp/javadocs -sourcepath src/main/java -subpackages io || true
	@echo "open file:///tmp/javadocs/index.html"
