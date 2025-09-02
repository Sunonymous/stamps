#!/bin/bash

echo "[1/3] Compiling release version of application..."
npx shadow-cljs release app

echo "[2/3] Compilation complete. Copying necessary files..."
mkdir -p release
cp resources/public/index.html      release/
cp resources/public/app.css         release/
cp resources/public/styles.css      release/
cp resources/public/js/main.js      release/js/

echo "[3/3] Release complete. Files in release/ directory."
