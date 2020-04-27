# CPVSAPI


This is Rest API for conformal prediction based virtual screening. Play framework is being used for developing this API.


# ENV VAR

MARIADB_IP	This needs to be set to the machine where MariaDB is running
We are using mariadb default port i.e. 3306

<pre>
MARIADB_IP     		    //IP of machine where MariaDB is running. We are using MariaDB default port i.e. 3306

MARIADB_PASSWORD	    //MariaDB root password	

VINA_CONF                   //Configuration file used for AutoDock Vina

RECEPTOR_NAME               //Name of the Receptor

RECEPTOR_PDBCODE            //Pdb Code of the Receptor

RESOURCES_HOME              //Direct to resources file in cpvsapi
</pre>
One can also use resources.sh to set up environment variables, which is convenient, using the following command. 

*source resources.sh receptor_name*

# Step by step process for running CPVSAPI on a local system

#### Note: 
For this material, we assume that Docker and Git are already installed on your local system.  If you want to prepare the Docker images yourself, then first perform section 2 and then start from section 1.2. Section 3 contains some extra docker commands which can be useful if you want to test both ready made and custom build docker images.

##  0. Test your Docker installation

<pre>docker run hello-world</pre>

You would need to use **sudo** in front of all the docker commands if post docker installation was not followed. Details are available here https://docs.docker.com/install/linux/linux-postinstall/

## 1. Executing the Docker images

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.1 Pulling readymade Docker images

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.1.1 To pull MariaDB database

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker pull laeeq/ligandprofiledb:0.0.3</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.1.2 To pull CPVSAPI for 1QCF

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker pull laeeq/cpvsapi:1QCF-0.0.1</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.2 Starting MariaDB Container from Image in background (using detach)
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker run --detach --name test-mariadb -d laeeq/ligandprofiledb:0.0.3</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.3	Checking container status
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker ps</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.4	Want to check what happened
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker logs test-mariadb</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.5	Finding IP address of container
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker inspect test-mariadb | grep IPAddress</pre>

Now we know the IP Address where the database is running, so we would be able to connect it. In our case, it was 172.17.0.2. 

#### Note: Make sure to write the IP Address down, you will need it in step 1.11.

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.6	Logging into MariaDB container and start a bash environment
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker exec -it test-mariadb bash</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.7	Logging into MariaDB in the Docker container by using the following command
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mysql -uroot -pmariadb_root</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.8	Allowing other Docker containers to access MariaDB
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;GRANT ALL PRIVILEGES ON *.* TO 'root'@'172.17.0.%' IDENTIFIED BY 'mariadb_root' WITH GRANT OPTION;</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.9	Exit from MariaDB
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;exit</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.10	Exit from MariaDB container
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;exit</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.11 	Running the CPVSAPI Docker container for 1QCF receptor and linking to MariaDB container

#### Note:
You must use the correct MARIADB_IP you found in the section 1.5, otherwise the cpvsapi docker container won’t be able to connect the MariaDB database.

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker run --detach --name test-cpvs -e MARIADB_IP='172.17.0.2' -e MARIADB_PASSWORD='mariadb_root' -e RECEPTOR_NAME='HCK Tyrosine kinase' -e RECEPTOR_PDBCODE='1QCF' --link test-mariadb:mariadb -p 9000:9000 laeeq/cpvsapi:1QCF-0.0.1</pre>

So what we have done here? We executed new container test-cpvs and using `--link`</pre>  we linked it to the test-mariadb docker container which was already running. `-p 9000:9000</pre>` is used to publish test-cpvs port to localhost.

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.12 Trying out the cpvs rest api for one receptor i.e. 1QCF

Open any web browser and access the 9000 port of the running test-cpvs Docker container we published to local machine. Try the following in any web browser.

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;http://localhost:9000</pre>

If everything works, you should see a swagger rest ui for cpvs. There are three end points of the CPVS API that can be tested i.e. predictions, pvalues and docking. Click anyone by **try it out** by giving compound in SMILES format. A sample SMILE is given below:

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;CC(C)c1ccc(Nc2nccc(n2)c3cnn4ncccc34)cc1</pre>

## 2. Preparing Docker images

#### Note: Java 8, Maven and sbt 1.1.6 must be installed on local for section 2 and JAVA_HOME must be set

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.1 Installing Project Dependencies

Clone spark-cheminformatics utilities for tools like signature generation

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;git clone https://github.com/mcapuccini/spark-cheminformatics.git</pre>

Enter the newly cloned directory. There are two projects parsers and sg. Enter both of them and run the following maven command to install each one of them as local dependencies.

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mvn clean install -DskipTests</pre>

Clone spark-cpvs-vina project

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;git clone https://github.com/laeeq80/spark-cpvs-vina.git</pre>
	
Enter the project “vs” inside spark-cpvs-vina and run the command

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mvn clean install -DskipTests</pre>
  
### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.2 Building MariaDB image and copying the database that contains model
We need a **Docker container for MariaDB** that stores the models created using cpvs project.

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.2.1 Clone the ligandprofiledb repo

Use git clone to clone the repo https://github.com/laeeq80/ligandProfiledb to create MariaDB Docker image including the database with 1QCF model.

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;git clone https://github.com/laeeq80/ligandProfiledb.git</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.2.2 	Build the ligandprofiledb image
Use the following command to create the Docker image at CLI from inside the cloned directory. The Dockerfile also includes the database copying step.

Enter the ligandprofiledb directory and run the following command

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker build . -t laeeq/ligandprofiledb:0.0.3</pre>

**!! Step 6/8** in the Dockerfile can take upto 5 minutes. **Grab a coffee.**

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.3	Building cpvsapi Docker image for one of the receptor i.e. with PDB ID “1QCF”. The pdbqt for the 1QCF is already available in the cpvsapi repo.

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.3.1	Clone the cpvsapi repo

Get out of the ligandprofiledb directory and run the following command.

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;git clone https://github.com/laeeq80/cpvsAPI.git</pre>

The pdbqt for the 1QCF and other resources are already available in the this repo resources folder.	

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.3.2	Creating package

Enter the cpvsAPI and run the following command

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;sbt dist</pre>

The above command will create cpvsapi-1.0.zip file in the **cpvsAPI/target/universal/** directory that is needed while building cpvsapi Docker image in step 2.3.4. In addition, openbabel-2.4.1.tar.gz will also be required that the application uses to convert files into different formats. Openbabel can be downloaded online using

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;wget  https://sourceforge.net/projects/openbabel/files/openbabel/2.4.1/openbabel-2.4.1.tar.gz</pre>

If wget is not available on your system, you can use a web browser to get it.

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.3.3	Clone cpvsDocker Dockerfile
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;git clone https://github.com/laeeq80/cpvsDocker.git</pre>
	
### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.3.4	Build Docker image for cpvsapi
	
Copy cpvsapi-1.0.zip and openbabel-2.4.1.tar.gz inside the cpvsDocker directory and run the following command from cpvsDocker directory.
	
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker build . -t laeeq/cpvsapi:1QCF-0.0.1</pre>

This will take around **15 to 20 minutes. Grab another coffee. [:D]**

Goto Step 1.2 to test the newly created Docker images.

## 3.		Some helpful commands
		
### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3.1 	List all images
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker image ls</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3.2	Deleting a docker image
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker rmi imageID</pre>

### 3.3	List running docker containers
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker ps</pre>

### 3.4	List already stopped but unremoved containers
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker ps -a</pre>

### 3.5	Stopping and removing a running docker container
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker stop containerName</pre>

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker rm containerName</pre>

E.g. 

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker stop test-cpvs</pre>



# Adding a new receptor Docker container to the service

The Docker image for a new receptor may be deployed using https://github.com/pharmbio/dpaas. Pick any receptor yaml file as template from the link provided and make a new yaml file for the new receptor Docker image accordingly.  

Then if you want to view and use the newly deployed Docker container in the Web service UI, add the PDB ID of the new receptor to the cpvs ui https://github.com/laeeq80/cpvs-ui/blob/master/index.html#L173

Then create new Docker image for the cpvs ui https://github.com/laeeq80/dpaasDockerfiles/tree/master/cpvsUIDocker

Deploy the new Docker image for UI as done in https://github.com/pharmbio/dpaas
