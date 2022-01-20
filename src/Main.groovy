class Main {
    static void main(String[] args) {

        /**Example implementations using the Teneo - Azure Active Directory connector and the Teneo - Microsoft Graph connector.
         *  This class uses Teneo Web Chat and Microsoft Teams as examples of outputs.
         *  */
        def aadOptions =
                ['redirectUri'            : 'https://yourdomain.com/redirected.html',
                 'clientId'               : 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx',
                 'clientSecret'           : 'xxxxx~xxxxxx_xxxxx-xxx-xx-xxxxxxxxxxx',
                 'tenantId'               : 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx',
                 'scopes'                 : ['xxxxxxx.xxxxxx', 'yyyyyyyyy.yyyy', 'zzzzzzzzz.zzzz'],
                 'tokenExpireSafetyBuffer': 60000
                ]

        //Instantiate the handler class with the options
        AzureAuthHandler azureAuthHandler = new AzureAuthHandler(aadOptions)
        //Get login URL and "send to user for login", for testing log in manually at the login URL
        // and copy code in the redirected url below
        println(azureAuthHandler.getLoginUrl())
        def authCode = '0.AXQ.......'

        //Send code to authentication server to get token. Returns true for a successful token
        def authResponse = azureAuthHandler.exchangeCodeForToken(authCode, true)
        println(authResponse)

        //Bind token getting method to GraphConnector.
        // In this example we bind the azureAuthHandler instance's getAuthToken method so that each request gets the current token regardless of token refreshes.
        //If you are using a different class or method to get tokens, bind the correct method here instead.
        GraphConnector.getAuthToken = azureAuthHandler::getAuthToken

// For simplicity, the different requests can be used as integrations in the solution. This will allow for easier management of flows with multiple channels. These are some examples:

        /**People examples**/
        /*Get a single user's data */
        def singleUserData = Integrations.GraphAPI.getSingleUser('me')
        println(singleUserData)
        def singleUserTWCJson = Integrations.GraphTWCOutput.makePersonCard(singleUserData)
        println(singleUserTWCJson)
        def singleUserTeamsJson = Integrations.GraphTeamsOutput.makeOutputJson(singleUserData)
        println(singleUserTeamsJson)

        /*Get a user's manager chain */
        def managerChainData = Integrations.GraphAPI.getManagerChain('me')
        println(managerChainData)
        def managerChainTWCJson = Integrations.GraphTWCOutput.makePeopleCarousel(managerChainData)
        println(managerChainTWCJson)
        def managerChainTeamsJson = Integrations.GraphTeamsOutput.makeOutputJson(managerChainData)
        println(managerChainTeamsJson)

        /*Get a user's closest colleagues*/
        def colleaguesData = Integrations.GraphAPI.getColleagues('me')
        println(colleaguesData)
        def colleaguesTWCJson = Integrations.GraphTWCOutput.makePeopleCarousel(colleaguesData)
        println(colleaguesTWCJson)
        def colleaguesTeamsJson = Integrations.GraphTeamsOutput.makeOutputJson(colleaguesData)
        println(colleaguesTeamsJson)

        /**Email examples**/
        /*Send an email */
        def sendEmailData = Integrations.GraphAPI.sendMail('me', 'Test', 'I can send emails now!', 'recipient@example.com')
        //Text output, no need to run through output integrations
        println(sendEmailData ? 'Message sent successfully' : 'Error sending message.')

        /*Fetch unread emails*/
        def fetchEmailData = Integrations.GraphAPI.fetchMail('me')
        println(fetchEmailData)
        def fetchEmailTWCJson = Integrations.GraphTWCOutput.makeEmailList(fetchEmailData)
        println(fetchEmailTWCJson)
        def fetchEmailTeamsJson = Integrations.GraphTeamsOutput.makeOutputJson(fetchEmailData)
        println(fetchEmailTeamsJson)

        /**Drive Examples examples**/
        //Text outputs, no need to run through output integrations
        /*Upload a file to One Drive */
        def uploadFileOneDriveData = Integrations.GraphAPI.uploadFileOneDrive(
                'me', 'oneDrive/path', 'image.jpg', 'image/jpeg', '/path/to/image.jpg'
        )
        println(uploadFileOneDriveData)
        //Give the file a little time to upload before getting the Id
        Thread.sleep(10000)

        /*Get File Id for download, deletion or replacement*/
        def getFileIdOneDriveData = Integrations.GraphAPI.getFileIdOneDrive('me','oneDrive/path','image.jpg')
        println(getFileIdOneDriveData)


        def updateFileOneDriveData = Integrations.GraphAPI.updateFileOneDrive('me',getFileIdOneDriveData,'image/jpeg','/path/to/newimage.jpg')
        println(updateFileOneDriveData)

        /**Calendar Examples*/
        /*Get Events from Shared Calendar*/
        //This String represents the ID of a shared calendar that hold the official holidays for Barcelona province. See API documentation to see how to get IDs programatically, although in most cases predefined calendars are more likely to be used
        String calendarId = 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-xxxxxxxxxxxxxxx='
        def sharedCalendarEventsData = Integrations.GraphAPI.getSharedCalendarEvents('me', calendarId, '2022-01-01','2022-12-01')
        println(sharedCalendarEventsData)
        //Print all events in a table
        def sharedCalendarEventsTWCJson = Integrations.GraphTWCOutput.makeEventTable(sharedCalendarEventsData, 'Table Title')
        println(sharedCalendarEventsTWCJson)
        def sharedCalendarEventsTeamsJson = Integrations.GraphTeamsOutput.makeOutputJson(sharedCalendarEventsData)
        println(sharedCalendarEventsTeamsJson)

        /*Get Events from User Calendar*/
        def getUserCalendarEventsData = Integrations.GraphAPI.getUserCalendarEvents('me')
        println(getUserCalendarEventsData)
        //Print all events in a table
        def getUserCalendarEventsTWCJson = Integrations.GraphTWCOutput.makeEventCards(getUserCalendarEventsData)
        println(getUserCalendarEventsTWCJson)
        def getUserCalendarEventsTeamsJson = Integrations.GraphTeamsOutput.makeOutputJson(getUserCalendarEventsData)
        println(getUserCalendarEventsTeamsJson)

        /*Create Event in User Calendar*/
        def createCalendarEventsData = Integrations.GraphAPI.createCalendarEvent('me', 'Europe/Paris','2022-01-20','11:00',  '2022-01-20','12:00',  'Bot Discussion', 'A meeting to discuss bots', false, )
        println(createCalendarEventsData)
        //Capture event Id from created event. Event Ids can also be procured using the getCalendarEvents method.
        String calendarEventId = createCalendarEventsData.id

        /*Amend Event in User Calendar*/
        def amendCalendarEventsData = Integrations.GraphAPI.amendCalendarEvent('me', calendarEventId, 'Europe/Paris','2022-01-20', '12:00', '2022-01-20','12:00' )
        println(amendCalendarEventsData)

        /*Delete Event in User Calendar*/
        def deleteCalendarEventsData = Integrations.GraphAPI.deleteCalendarEvent('me', calendarEventId )
        println(deleteCalendarEventsData)

        /*Find possible meeting times*/
        def findMeetingTimesData = Integrations.GraphAPI.findMeetingTimes('me',
                ['person1@example.com', 'person2@example.com'])
        println(findMeetingTimesData)
        def findMeetingTimesTWCJson = Integrations.GraphTWCOutput.makeMeetingsTable(findMeetingTimesData as List, 'Available Times')
        println(findMeetingTimesTWCJson)
        def findMeetingTimesTeamsJson = Integrations.GraphTeamsOutput.makeOutputJson(findMeetingTimesData)
        println(findMeetingTimesTeamsJson)

    }
}














