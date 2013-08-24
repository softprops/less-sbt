#!/bin/sh

bower install bootstrap
rm -rf src/main/jquery
mv src/main/bootstrap/less/* src/main/less
rm -rf src/main/bootstrap
lessc src/main/less/bootstrap.less fixtures/bootstrap.css

echo "Bootstrapped."
