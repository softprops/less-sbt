#!/bin/sh

bower install bootstrap#2.3.2
rm -rf src/main/jquery
mkdir -p src/main/less
mv src/main/bootstrap/less/* src/main/less
rm -rf src/main/bootstrap
lessc src/main/less/bootstrap.less fixtures/bootstrap.css

echo "Bootstrapped."
