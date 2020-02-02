Push-Location

Remove-Item "resources/public/cljs-out/" -Recurse -Force

lein fig -O advanced -bo dev

remove-item "resources/public/cljs-out-build" -Recurse -Force

lein uberjar

Pop-Location



