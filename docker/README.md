# Bindaas 3.0.0 Docker Container


## For the Bindaas User: Running the Docker Container

$ docker run --name bindaas-3 -p 8080:8080 -p 9099:9099 bindaas:3.0.0 


### To stop

Now stop using the assigned name:

$ docker stop bindaas-3


## For the Bindaas Developer: Building the Docker Container

From the <BINDAAS-SOURCE-ROOT>/docker directory,

$ docker build -t bindaas:3.0.0 .

You will get the output "Successfully tagged bindaas:3.0.0" if everything went fine.


Confirm that by running

$ docker image ls

REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE

bindaas             3.0.0               a9a81d677bb2        5 minutes ago       630MB


Before committing, make sure Bindaas runs fine in the container using the command above listed under the "For the Bindaas User" section.


### Tag the image with the user name:
 
 $ docker tag bindaas:3.0.0 pradeeban/bindaas:3.0.0


 ### Log in and push the image to the Docker repository:

 $ docker push pradeeban/bindaas:3.0.0