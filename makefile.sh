#!/bin/bash

# Remove old class files
rm -f Server/*.class >> /dev/null
rm -f Protocols/*.class >> /dev/null

# Compile Protocol Classes
javac Protocols/*.java


######## Server Compilation ########
# Compile files and create jar
javac Server/*.java -cp Protocols/
jar cvfm Server.jar Server/manifest.mf Server/*.class


######## Client Compilation ########
# Compile files and create jar
javac Client/*.java -cp Protocols/
jar cvfm Client.jar Client/manifest.mf Client/*.class


# Instructions
printf "
Modo de empleo:
            Servidor: java -jar Server.jar
            Cliente: java -jar Cliente.jar
"
