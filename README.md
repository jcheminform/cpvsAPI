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
