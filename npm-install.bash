#!/bin/bash -v
npm install phantomjs
npm install karma karma-cljs-test  --save-dev
npm install karma-junit-reporter   --save-dev
npm install karma-chrome-launcher  karma-firefox-launcher  karma-safari-launcher  --save-dev

# for react-bootstrap
npm install --save  react react-dom 
npm install --save  react-bootstrap 

