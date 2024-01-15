# Install Teamcenter dependencies
As the teamcenter dependencies cannot be downloaded from public repository, please install the dependencies into your local maven repository manually.

# Install Teamcenter12 dependencies
In *current* directory, run below commands:

<pre>
cd ./teamcenter12
mvn install:install-file -Dfile=fccjavaclientproxy-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=fccjavaclientproxy -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=fmsclientcache-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=fmsclientcache -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=fmsutil-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=fmsutil -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=icctstubs-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=icctstubs -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=tcserverjavabinding-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=tcserverjavabinding -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaAdministrationStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaAdministrationStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaBomStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaBomStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCadStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCadStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaClassificationStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaClassificationStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaClient-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaClient -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCommon-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCommon -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCoreLoose-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCoreLoose -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCoreStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCoreStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaManufacturingStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaManufacturingStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaManufacturingStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaManufacturingStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaQueryStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaQueryStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaStrongModel-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaStrongModel -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaStructureManagementStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaStructureManagementStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaWorkflowStrong-12.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaWorkflowStrong -Dversion=12.2.0 -Dpackaging=jar -DgeneratePom=true
</pre>

# Install Teamcenter11 dependencies
In *current* directory, run below commands:

<pre>
cd ./teamcenter11
mvn install:install-file -Dfile=fccjavaclientproxy-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=fccjavaclientproxy -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=fmsclientcache-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=fmsclientcache -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=fmsutil-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=fmsutil -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=icctstubs-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=icctstubs -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=tcserverjavabinding-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=tcserverjavabinding -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaBomStrong-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaBomStrong -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaBomTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaBomTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaBomTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaBomTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaBomTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaBomTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaClassificationStrong-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaClassificationStrong -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaClassificationTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaClassificationTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaClient-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaClient -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCommon-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCommon -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCoreLoose-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCoreLoose -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCoreLoose-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCoreLoose -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaCoreTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaCoreTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaProjectManagementStrong-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaProjectManagementStrong -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaProjectManagementTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaProjectManagementTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaProjectManagementTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaProjectManagementTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaQueryTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaQueryTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaStrongModel-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaStrongModel -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaStructureManagementStrong-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaStructureManagementStrong -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaStructureManagementTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaStructureManagementTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaWorkflowStrong-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaWorkflowStrong -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=TcSoaWorkflowTypes-11.2.0.jar -DgroupId=com.teamcenter -DartifactId=TcSoaWorkflowTypes -Dversion=11.2.0 -Dpackaging=jar -DgeneratePom=true
</pre>