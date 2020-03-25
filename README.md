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

# Adding a new receptor Docker container to the service

The Docker image for a new receptor may be deployed using https://github.com/pharmbio/dpaas. Pick any receptor yaml file as template from the link provided and make a new yaml file for the new receptor Docker image accordingly.  

Then if you want to view and use the newly deployed Docker container in the Web service UI, add the PDB ID of the new receptor to the cpvs ui https://github.com/laeeq80/cpvs-ui/blob/master/index.html#L173

Then create new Docker image for the cpvs ui https://github.com/laeeq80/dpaasDockerfiles/tree/master/cpvsUIDocker

Deploy the new Docker image for UI as done in https://github.com/pharmbio/dpaas
