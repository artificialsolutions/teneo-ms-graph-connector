/**
 * This class contains the methods necessary to convert the data coming from MS Graph into Teneo Web Chat templates so that the front-end.
 * The methods are static but for clarity's sake in the flow they should be accessed by integrations. 
 */
class GraphTWCUtils {
/**
 *
 * Creates a carousel template with information cards about each person.
 * @param peopleList A list of objects from MS Graph each of which includes the necessary data to create a card for each person using
 * the makePersonCard method, and make a carousel from them.
 * @see this.makePersonCard for the required data fields.
 * @return A TWC carousel template in stringified JSON format.
 */
    static public String makePeopleCarousel(List peopleList) {

        def carouselMap = [
                "type"          : "carousel",
                "carousel_items": []
        ]
        peopleList.each {
            if (it.userPrincipalName) {
                if (!it.hasProperty('mail')) {
                    it.mail = it.userPrincipalName
                }


                carouselMap.carousel_items.push(
                        makePersonCard(it, false)
                )
            }
        }
        return new groovy.json.JsonBuilder(carouselMap).toString()

    }
/**
 * 
 * Makes a card with a person's information for display.
 * For each user it will make a call to MS Graph to get the user's photo, if no photo is available, it fetches an image from a predetermined URL.
 * @param personProfile this object from MS Graph contains the data fields necessary to build the card, namely:
 userPrincipalName,
 givenName,
 surname,
 jobTitle,
 officeLocation,
 mail -> Optional. If not available userPrincipalName is used.
 * @param makeJson Determines if the resulting object is transformed to a JSON String
 * or if it is left as is for further processing, i.e., putting the card in a carousel or combo template.
 * @return Either an object with the card data or a TWC card template in stringified JSON format.
 */
    static public def makePersonCard(def personProfile, boolean makeJson = true) {

        def photo = GraphConnector.connect(
                personProfile.userPrincipalName,
                '/photo/$value',
                [
                        method   : 'get',
                        'queries': [:]
                ]
        )


        String fullname = personProfile.givenName + ' ' + personProfile.surname
        def personObj = [
                "type"            : "card",
                "image"           : [
                        "image_url": (photo.status == 'ok' ?
                                ('data:image/png;base64, ' + photo.body) :
                                'https://developers.artificial-solutions.com/user/themes/teneo-devs-purple/images/home/preloader_bot.gif')
                        ,
                        "alt"      : fullname
                ],
                "title"           : fullname,
                "subtitle"        : personProfile.jobTitle,
                "text"            : personProfile.officeLocation,
                "linkbutton_items": [
                        [
                                "title": personProfile.mail,
                                "url"  : 'mailto://' + personProfile.mail
                        ]
                ]
        ]
        if (makeJson) {
            return new groovy.json.JsonBuilder(personObj).toString()
        } else {
            return personObj
        }
    }
/**
 *
 * Makes a table template with the name and date range of an event as columns
 * @param events A list of event objects from MS Graph
 * @param options table template options, e.g. the title or footer of the table.
 * @return A TWC table template in stringified JSON format.
 */
    static public def makeEventTable(events, options = [:])     {
        def tableMap = [
                "type": "table",
                "body": []
        ]
        tableMap << options


        events.each {
            java.util.Date parsedEventDate = java.util.Date.parse("yyyy-MM-dd'T'hh:mm:ss.SSSSSSS", it.start.dateTime as String)

            tableMap.body.push(
                    [
                            it.subject,
                            parsedEventDate.toDayOfWeek().toString().toLowerCase().capitalize() + ', ' +
                                    parsedEventDate.toMonth().toString().toLowerCase().capitalize() + ' ' +
                                    parsedEventDate.getDate()
                    ])
        }
        return new groovy.json.JsonBuilder(tableMap).toString()
    }
    /**
     *
     * Creates a table of possible meeting times and the availability of each attendee
     * @param meetingTimes A list of meetingAvailability objects from MS Graph with time suggestions and each attendees status, as well as the confidence in the result.
     * @param options table template options, e.g. the title or footer of the table.
     * @return A TWC table template in stringified JSON format.
     */
    static public def makeMeetingsTable(List meetingTimes, options = [:]){
        def tableMap = [
                'type': 'table',
                'headers': [''],
                'body': [['You']]
        ]
        tableMap << options



        meetingTimes.eachWithIndex { it, i ->


            java.util.Date parsedMeetingStartDateTime = java.util.Date.parse("yyyy-MM-dd'T'hh:mm:ss.SSSSSSS", it.meetingTimeSlot.start.dateTime as String)
            java.util.Date parsedMeetingEndDateTime = java.util.Date.parse("yyyy-MM-dd'T'hh:mm:ss.SSSSSSS", it.meetingTimeSlot.end.dateTime as String)
            java.util.TimeZone timeZone = TimeZone.getTimeZone(it.meetingTimeSlot.start.timeZone)

            def meetingStartEnd =
                    parsedMeetingStartDateTime.format("dd/MMM/yyyy' at 'hh:mm", timeZone) +
                            ' - ' +
                            parsedMeetingEndDateTime.format(((parsedMeetingStartDateTime.toMonthDay() == parsedMeetingEndDateTime.toMonthDay()) ? "hh:mm" : "dd/MMM/yyyy' at 'hh:mm"),timeZone )

            tableMap.headers.add(meetingStartEnd)
            tableMap.body[0].add(it.organizerAvailability)
            for(int c = 0; c < it.attendeeAvailability.size(); c++){
                if(i==0){
                    tableMap.body.add([it.attendeeAvailability[c].attendee.emailAddress.address])
                }
                tableMap.body[c+1].add(it.attendeeAvailability[c].availability)
            }
        }

        return new groovy.json.JsonBuilder(tableMap).toString()
    }
/**
 * Makes a combo message with a series of cards showing details of events.
 * @param events list with the event objects from MS Graph to be used. The objects must contain the following fields:
 start
 end
 subject
 organizer
 location
 webLink
 attendees
 * @return A TWC combo template in stringified JSON format.
 */
    static public def makeEventCards(events) {


        def eventCombo = [
                "type"      : "combo",
                "components": []
        ]


        events.each {

            java.util.Date parsedEventStartDate = java.util.Date.parse("yyyy-MM-dd'T'hh:mm:ss.SSSSSSS", it.start.dateTime as String)
            java.util.Date parsedEventEndDate = java.util.Date.parse("yyyy-MM-dd'T'hh:mm:ss.SSSSSSS", it.end.dateTime as String)
            java.util.TimeZone timeZone = TimeZone.getTimeZone(it.start.timeZone)
            def eventStartEnd =
                    parsedEventStartDate.format("dd/MMM/yyyy' at 'hh:mm", timeZone) +
                    ' - ' +
                    parsedEventEndDate.format(((parsedEventStartDate.toMonthDay() == parsedEventEndDate.toMonthDay()) ? "hh:mm" : "dd/MMM/yyyy' at 'hh:mm"),timeZone )

def thisCard = [
        "type"            : "card",
        "title"           : it.subject,
        "subtitle"        : it.organizer.emailAddress.name,
        "text"            : eventStartEnd + '\nLocation: ' + it.location.displayName + '\nList of Attendees',
        "list_items": [],
        "linkbutton_items": [
                [
                        "title": "Open Event",
                        "url"  : it.webLink
                ]
        ]
]

            def joinUrl = it?.onlineMeeting?.joinUrl;
            if(joinUrl){
                thisCard.linkbutton_items.push(  [
                        "title": "Join Online",
                        "url"  : joinUrl
                ])
            }

            it.attendees.each{
                attendee ->
                    if(attendee.type != 'resource') {
                        thisCard.list_items.push(
                                [
                                        "title"     : attendee.emailAddress.name + '\n' + attendee.type + '\nresponse: ' + attendee.status.response,
                                        "postback"  : attendee.emailAddress.address,
                                        "parameters": ["command": "getUser"]
                                ]
                        )
                    }
            }
            eventCombo.components.push(
                    thisCard
            )
        }

        return new groovy.json.JsonBuilder(eventCombo).toString()
    }
/**
 * It makes a linkbuttons list with the Importance, subject and sender of an unread email.
 * The link itself points to the message in the outlook webapp.
 * @param emails list of email objects from MS Graph
 * @return A TWC linkbutton template in stringified JSON format.
 */
    static public def makeEmailList(emails) {

        def listMap = [
                "type"            : "linkbuttons",
                "title"           : "Unread Emails",
                "linkbutton_items": [

                ]
        ]
        emails.each {
            String[] titleStrings = ['Importance: ' + it.importance.toUpperCase(), it.sender.emailAddress.address, '\n----------\n', it.subject]
            titleStrings.eachWithIndex { titleString, i ->

            }
            listMap.linkbutton_items.push(
                    [
                            "title" : titleStrings.join('\n'),
                            "url"   : it.webLink,
                            "target": "_blank"
                    ]

            )
        }
        return new groovy.json.JsonBuilder(listMap).toString()

    }
}
