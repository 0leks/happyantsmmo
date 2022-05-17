[![azure-deploy-client](https://github.com/0leks/happyantsmmo/actions/workflows/azure-static-web-apps-lively-rock-09a65931e.yml/badge.svg?branch=prodclient)](https://github.com/0leks/happyantsmmo/actions/workflows/azure-static-web-apps-lively-rock-09a65931e.yml) [![azure-deploy-server](https://github.com/0leks/happyantsmmo/actions/workflows/azure-deploy-server.yml/badge.svg)](https://github.com/0leks/happyantsmmo/actions/workflows/azure-deploy-server.yml)

# Instructions

use `monitor_sass.cmd` to generate css from the scss files

use `host_website.cmd` to host the website (requires python 3)

add the eclipse maven project under /server, install, run as java application with `Application.java` to host the server



## Useful links

- [Javalin Docs](https://javalin.io/documentation#context)
- [CORS Info](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Google developer dashboard](https://console.cloud.google.com/)
- [Google OAuth playground](https://developers.google.com/oauthplayground)


TODO https://docs.microsoft.com/en-us/azure/app-service/quickstart-java?tabs=javase&pivots=platform-windows
https://docs.microsoft.com/en-us/azure/static-web-apps/custom-domain-azure-dns

TODO:
	non-euclidean.
	1st room is x distance from center no matter which direction you go
	2nd room is 2x distance from center, so on and so forth
	
	make stuff super far apart, but have some sort of indicator when there is nothing up ahead
	
	not enough funding
	
	dont allow length 0 tunnels
	
	admin ability to give anyone coins
	fix google already logged in cant log out issue
	
	fix disconnect reconnect name becomes null
	
	add hotkeys for digging and collapsing tunnels
	
	