# CPVSAPI


This is Rest API for conformal prediction based virtual screening. Play framework is being used for developing this API.


# ENV VAR

MARIADB_IP	This needs to be set to the machine where mariadb is running
We are using mariadb default port i.e. 3306

<pre>
MARIADB_IP     		    //IP of machine where mariadb is running. We are using mariadb default port i.e. 3306

MARIADB_PASSWORD	    //Mariadb root password	

VINA_CONF                   //Configuration file used for AutoDock Vina

RECEPTOR_NAME               //Name of the Receptor

RECEPTOR_PDBCODE            //Pdb Code of the Receptor

RESOURCES_HOME              //Direct to resources file in cpvsapi
</pre>
One can also use resources.sh to set up environment variables, which is convenient, using the following command. 

*source resources.sh receptor_name*

# Creating distribution

If you want to create distribution to be use with https://github.com/laeeq80/cpvsDocker, please run following command in the cpvsAPI directory

*sbt dist*

# Step by step process for running cpvsapi on a local system

#### Note: 
For this material, we assume that Docker and Git are already installed on your local system.  If you want to prepare the Docker images yourself, then first perform section 2 and then start from section 1.2. Section 3 contains some extra docker commands which can be useful if you want to test both ready made and custom build docker images.

##  0. Test your Docker installation

<pre>docker run hello-world</pre>

You would need to use sudo in front of all the docker commands if post docker installation was not followed. Details are available here https://docs.docker.com/install/linux/linux-postinstall/

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

#### Note: 
Make sure to write it down, you will need it in step 1.11.

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.6	Logging into mariadb container and start a bash environment
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker exec -it test-mariadb bash</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.7	Logging into mariadb in the Docker container by using the following command
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mysql -uroot -pmariadb_root</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.8	Allowing other Docker containers to access MariaDB
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;GRANT ALL PRIVILEGES ON *.* TO 'root'@'172.17.0.%' IDENTIFIED BY 'mariadb_root' WITH GRANT OPTION;</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.9	Exit from MariaDB
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;exit</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.10	Exit from MariaDB container
<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;exit</pre>

### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1.11 	Running the CPVSAPI Docker container for 1QCF receptor and linking to MariaDB container

#### Note:
You must use the correct MARIADB_IP you found in the section 1.5, otherwise the cpvsapi docker container won’t be able to connect the mariadb database.

<pre>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;docker run --detach --name test-cpvs -e MARIADB_IP='172.17.0.2' -e MARIADB_PASSWORD='mariadb_root' -e RECEPTOR_NAME='HCK Tyrosine kinase' -e RECEPTOR_PDBCODE='1QCF' --link test-mariadb:mariadb -p 9000:9000 laeeq/cpvsapi:1QCF-0.0.1</pre>

So what we have done here? We executed new container test-cpvs and using *--link</pre>*  we linked it to the test-mariadb docker container which was already running. *-p 9000:9000</pre>* is used to publish test-cpvs port to localhost.

# Adding a new receptor Docker container to the service

The Docker image for a new receptor may be deployed using https://github.com/pharmbio/dpaas. Pick any receptor yaml file as template from the link provided and make a new yaml file for the new receptor Docker image accordingly.  

Then if you want to view and use the newly deployed Docker container in the Web service UI, add the PDB ID of the new receptor to the cpvs ui https://github.com/laeeq80/cpvs-ui/blob/master/index.html#L173

Then create new Docker image for the cpvs ui https://github.com/laeeq80/dpaasDockerfiles/tree/master/cpvsUIDocker

Deploy the new Docker image for UI as done in https://github.com/pharmbio/dpaas
