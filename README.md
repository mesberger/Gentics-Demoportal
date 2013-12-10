# Gentics Demoportal #

## Install Eclipse Indigo JEE (Including egit, m2e, m2e-wtp) ##

## Preparation ##

Add the license information to your maven profile (settings.xml)
<code>
    <profiles>
        ...
        <profile>
            <id>gentics.license</id>
            <properties>
                    <gentics.gpn.licensekey>YOUR_LICENSE_KEY</gentics.gpn.licensekey>
            </properties>
        </profile>
        ...
    </profiles>
    <activeProfiles>
        <activeProfile>SDK</activeProfile>
        <activeProfile>gentics.license</activeProfile>
    </activeProfiles>
<code>

## Setup ##

1. Import all projects and invoke project clean

2. Create a new server
* Add the webapps during server creation
* Set the runtime server configuration path to: `/demoportal-config/target/eclipse_configuration'
  Please close and reopen the server configuration window before continuing.
* Add the following argument to your Server VM Arguments:
<code>
	-Dcom.gentics.portalnode.confpath=${workspace_loc}/demoportal/demoportal-config/target/demoportal_configuration
<code>
	
3. Make sure you invoked 'Publish' for your Server in order to update the used Server settings.