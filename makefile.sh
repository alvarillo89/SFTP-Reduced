#!/bin/bash

# Remove old class files
rm -f Server/*.class >> /dev/null
rm -f Protocols/*.class >> /dev/null

# Compile Protocol Classes
javac Protocols/*.java


######## Server Compilation ########
# Compile files
javac Server/*.java -cp Protocols/

# Create .jar file
jar cvfm Server.jar Server/manifest.mf Server/*.class
cp Server/Server.jar .


# ######## Client Compilation ########
# # Compile files
# javac Client/*.java -cp Protocols/
#
# # Create .jar file
# cd Client
# jar cvfm Client.jar manifest.mf *.class
# cp Client/Client.jar .
# cd ..


# Instructions
printf "
Modo de empleo:
            Servidor: java -jar Server.jar
            Cliente: java -jar Cliente.jar
"
