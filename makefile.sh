#!/bin/bash

# Remove old class files
rm -f Server/*.class
rm -f Protocols/*.class

# Compile Protocol Classes
javac Protocols/*.java


######## Server Compilation ########
# Compile files and create jar
echo "Compiling Client"
javac Server/*.java -cp Protocols/
jar cvfm Server.jar Server/manifest.mf -C Server/ . -C Protocols/ .


######## Client Compilation ########
# Compile files and create jar
echo "Compiling Client"
javac Client/*.java -cp Protocols/
jar cvfm Client.jar Client/manifest.mf -C Client/ . -C Protocols/ .


# Instructions
echo "Modo de empleo:"
echo "    Servidor: java -jar Server.jar"
echo "    Cliente: java -jar Cliente.jar"
