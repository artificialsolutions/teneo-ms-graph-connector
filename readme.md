# Microsoft Graph Connector

> [!IMPORTANT]
> This project has been retired and archived  
> If there is a need of continued use / development of this project for your own needs please feel free to fork the project - which will remain here in archived form.

## Documentation
Complete documentation of possible calls and methods at https://docs.microsoft.com/en-us/graph/api/resources/user?view=graph-rest-1.0

## Purpose
This connector will run in an instance of Teneo Engine and allows for the backend to retrieve information and perform actions on Microsoft Graph. 
The connector is bundled with the Azure Active Directory Authentication Connector, but each can be used separately.

## Azure Cloud
An application has to be registered on the Azure Cloud under App Registrations.
Once registered, create a client secret and a web redirect URL. 
See Azure Active Directory Authentication Handler below for how to use this information.


## Package parts and Installation
### Main.groovy -> 
Contains usage examples for different operations in Graph, from getting data on people to sending emails or amending events. This class emulates the script nodes you would find in flows in Teneo Studio.
### Integrations.groovy -> 
Its purpose is to emulate different integrations in Teneo. The GraphAPI subclass has methods that will build the requests for Graph, the other two will build output templates with that data. 
### All other classes in src directory -> 
These are the classes that need to be set up in the _Solution Loaded_ script in Teneo Studio. These provide the necessary functionalities to request and manipulate data fom Graph. Implementation details commented inline in each file.
### libs directory -> 
Contains two libraries that are used by the RestClient class. Apache Commons Codec and Apache Commons IO. These need to be uploaded to Teneo Engine through Studio under _Resources_ -> _File_ on the /scrip_libs path.
### readme.md ->
This file.

Below is a description of the

# Azure Active Directory Authentication Connector

## Usage
This connector has two main parts, the specific AzureAuthHandler class and the common RestClient class.
If you are already using the RestClient class through a different connector, there is no need to provide an additional copy.

### AzureAuthHandler
When the class is instantiated the constructor expects an object with the connection properties, which includes the following fields:

#### redirectUri:
The URI setup in the application in Azure -> Home -> App Registrations -> [Your App] -> Redirect URIs. This should be the URL of the page the user is redirected to after login.
#### clientId:
The ID of the application as it appears  in Azure -> Home -> App Registrations -> [Your App]  -> Overview
#### clientSecret:
The client secret gotten from Azure -> Home -> App Registrations -> [Your App] -> Certificates & secrets
#### tenantId:
Your organization's Tenant ID, also from the overview. To authenticate against the universal Microsoft AD (to allow @hotmail, @live and @outlook accounts to authenticate), use 'common' as the tenantID.
#### scopes:
This is a List of scopes the token is being authorized for. The contents of this list can be copied from Azure -> Home -> App Registrations -> [Your App] -> API Permissions. Make sure to convert all the characters to lower-case.
#### tokenExpireSafetyBuffer:
Defaults to 60000. The amount of milliseconds that will be shaved off the Token Expiry time so that refresh requests are always within the valid time range.
