@echo off

rem clone repo
cd ..
git clone https://github.com/TrollyLoki/neat-chess

rem install to maven local
cd neat-chess
gradlew publishToMavenLocal