# A quick and temporary Makefile, for frequent tasks during Syncbase Java API
# development.

SHELL := /bin/bash -euo pipefail

.DELETE_ON_ERROR:

.PHONY: build
build:
	./gradlew build

# TODO(sadovsky): Avoid the "cp" hack.
.PHONY: test
test:
	cp ../lib/build/libs/libv23.dylib ./build/libs/
	./gradlew testDebug --tests=io.v.syncbase.SyncbaseTest --info

.PHONY: clean
clean:
	./gradlew clean

# Note, javadoc complains about the self-closing <p/> tags that Android Studio
# creates.
.PHONY: javadoc
javadoc:
	javadoc -d /tmp/javadocs -sourcepath src/main/java -subpackages io || true
	@echo "open file:///tmp/javadocs/index.html"
