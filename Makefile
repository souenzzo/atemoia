#!/usr/bin/env make

GIT_URL="https://github.com/souenzzo/atemoia.git"
.PHONY: clean

clean:
	rm -rf .cpcache target/ .nrepl-port .idea/ start.sh current-deps

.git/refs/heads/master:
	mkdir -p .git/refs/heads/
	git ls-remote $(GIT_URL) refs/heads/master | awk '{ print $$1 }' > .git/refs/heads/master

current-deps: .git/refs/heads/master
	echo "{:deps {br.com.souenzzo/atemoia {:git/url \"$(GIT_URL)\" :sha \"$$(head -1 .git/refs/heads/master)\"}}}" > current-deps

start.sh: current-deps
	echo "#!/usr/bin/env sh" > start.sh
	echo "export CURRENT_COMMIT='$$(head -1 .git/refs/heads/master)'" >> start.sh
	echo "java -cp '$$(clojure -Srepro -Spath -Sdeps "$$(cat current-deps)")' clojure.main --report stderr -m br.com.souenzzo.atemoia" \
    | tee -a start.sh
	chmod +x start.sh
