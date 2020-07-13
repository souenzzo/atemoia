#!/usr/bin/env make

.PHONY: clean

clean:
	rm -rf .cpcache target/ .nrepl-port .idea/ start.sh

start.sh:
	mkdir -p "${HOME}/.gitlibs" "${HOME}/.m2"
	echo "#!/usr/bin/env sh" > start.sh
	echo "export CURRENT_COMMIT='$$(git rev-parse HEAD)'"
	echo "java -cp '$$(clojure -Spath)' clojure.main --report stderr -m br.com.souenzzo.atemoia" \
    | tee -a start.sh
	chmod +x start.sh
