/**
 * These subclasses are meant to be used as integrations in Teneo.
 * The integrations are only examples and do not have any error handling.
 */
class Integrations {
    class GraphAPI {
        static getSingleUser(String user) {
            def _graphResponse =
                    GraphConnector.connect(user, '',
                            [
                                    method   : 'get',
                                    'queries': ['$select': 'givenName, surname, jobTitle, userPrincipalName, officeLocation, mail']
                            ]
                    )
            if (_graphResponse.status == 'ok') {
                return _graphResponse.body
            } else {
                return []
            }

        }

        static getManagerChain(String user) {
            def _graphResponse = GraphConnector.connect(user, '',
                    [

                            method   : 'get',
                            'queries':
                                    ['$select': 'givenName, surname, jobTitle, userPrincipalName, officeLocation, mail',
                                     '$expand': 'manager($levels=max;$select=givenName, surname, jobTitle, userPrincipalName, officeLocation, mail)',
                                     '$count' : 'true'
                                    ]
                    ]
            )
            if (_graphResponse.status == 'ok') {
                return GraphConnector.DataUtils.listValueToDepth(_graphResponse.body, 'manager', [])
            } else {
                return []
            }


        }

        static getColleagues(String user) {
            def _graphResponse = GraphConnector.connect(user,
                    'people',
                    [
                            method   : 'get',
                            'queries': ['$select': 'givenName, surname, jobTitle, userPrincipalName, officeLocation']
                    ]
            )
            if (_graphResponse.status == 'ok') {
                return _graphResponse.body.value
            } else {
                return []
            }
        }

        static sendMail(String sender, String subject, String message, String recipient) {
            def _graphResponse = GraphConnector.connect(sender, 'sendMail',
                    [method : 'post',
                     headers: ['Content-Type': 'application/json'],
                     body   : [message: [
                             subject     : subject,
                             body        : [
                                     contentType: 'Text',
                                     content    : message
                             ],
                             toRecipients: [
                                     [
                                             emailAddress: [
                                                     address: recipient
                                             ]]
                             ],
                     ]]
                    ])

            return _graphResponse.status == 'ok'
        }

        static fetchMail(String user) {
            def _graphResponse = GraphConnector.connect(user, 'mailFolders/inbox/messages',
                    [
                            method   : 'get',
                            'queries': ['$filter': 'isRead eq false',
                                        '$select': 'sender, subject, importance, webLink'
                            ]
                    ]
            )

            if (_graphResponse.status == 'ok') {
                return _graphResponse.body.value
            } else {
                return []
            }
        }

        static getSharedCalendarEvents(String user, String calendarId, def startBound = null, def endBound = null) {
            def connOptions = [
                    method : 'get',
                    queries:
                            [
                                    $select: 'start,subject'
                            ]
            ]

            if (startBound) {
                connOptions.queries["\$filter"] = "start/dateTime ge '${startBound}'"
            }
            if (endBound) {
                connOptions.queries["\$filter"] += " and end/dateTime le '${endBound}'"
            }


            def _graphResponse = GraphConnector.connect(user,
                    'calendars/' + calendarId + '/events',
                    connOptions
            )

            if (_graphResponse.status == 'ok') {
                return _graphResponse.body.value
            } else {
                return []
            }

        }

        static getUserCalendarEvents(String user, def startBound = null, def endBound = null) {

            def connOptions = [
                    method : 'get',
                    queries:
                            [
                                    $select: 'start,end,organizer,location,subject, weblink, onlineMeeting'
                            ]
            ]

            if (startBound) {
                connOptions.queries['\$filter'] = "start/dateTime ge '${startBound}'"
            }
            if (endBound) {
                connOptions.queries['\$filter'] += " and end/dateTime le '${endBound}'"
            }

            def _graphResponse = GraphConnector.connect(user,
                    '/events',
                    connOptions)

            if (_graphResponse.status == 'ok') {
                return _graphResponse.body.value
            } else {
                return []
            }

        }

        static createCalendarEvent(String user, String timezone, String startDate, String startTime, String endDate, String endTime, String subject, String body, boolean allDay) {
            def _graphResponse = GraphConnector.connect(user, '/events',
                    [
                            method : 'post',
                            headers: [
                                    'Content-Type': 'application/json'
                            ],
                            body   : [
                                    start                : [
                                            dateTime: startDate + 'T' + startTime,
                                            timeZone: timezone
                                    ],
                                    end                  : [
                                            dateTime: endDate + 'T' + endTime,
                                            timeZone: timezone
                                    ],
                                    subject              : subject,
                                    isAllDay             : allDay as String,
                                    body                 : [
                                            contentType: 'HTML',
                                            content    : body

                                    ],
                                    isOnlineMeeting      : true,
                                    onlineMeetingProvider: "teamsForBusiness"
                            ]
                    ]
            )

            if (_graphResponse.status == 'ok') {
                return _graphResponse.body
            } else {
                return [:]
            }
        }

        static amendCalendarEvent(String user, String eventId, String timezone, String startDate, String startTime, String endDate, String endTime) {
            def _graphResponse = GraphConnector.connect(user, '/events/' + eventId,
                    [
                            method : 'patch',
                            headers: [
                                    'Content-Type': 'application/json'
                            ],
                            body   : [
                                    start: [
                                            dateTime: startDate + 'T' + startTime,
                                            timeZone: timezone
                                    ],
                                    end  : [
                                            dateTime: endDate + 'T' + endTime,
                                            timeZone: timezone
                                    ]
                            ]
                    ]
            )
            if (_graphResponse.status == 'ok') {
                return _graphResponse.body
            } else {
                return [:]
            }
        }

        static deleteCalendarEvent(String user, String eventId) {
            def _graphResponse = GraphConnector.connect(user, '/events/' + eventId,
                    [
                            method: 'delete'
                    ])
            return  _graphResponse.status == 'ok'
        }

        static getFileIdOneDrive(String user, String oneDrivePath, String oneDriveFilename) {
            oneDrivePath = GraphConnector.DataUtils.addSlashesToPaths(oneDrivePath)
           def _graphResponse = GraphConnector.connect(user, 'drive/root:' + oneDrivePath + oneDriveFilename,
                    [
                            method   : 'get',
                            'queries': ['$select': 'id']
                    ]
            )

            if (_graphResponse.status == 'ok') {
                return _graphResponse.body.id
            } else {
                return [:]
            }
        }

        static uploadFileOneDrive(String user, String oneDrivePath, String oneDriveFilename, String contentType, String localFilePath) {
            oneDrivePath = GraphConnector.DataUtils.addSlashesToPaths(oneDrivePath)

            def _graphResponse = GraphConnector.connect(user, '/drive/root:' + oneDrivePath + oneDriveFilename + ':/content', [
                    method : 'put',
                    headers: [
                            'Content-Type': contentType
                    ],
                    body   : localFilePath
            ])

            return  _graphResponse.status == 'ok'

        }

        static updateFileOneDrive(String user, String fileId, String contentType, String localFilePath) {
            def _graphResponse = GraphConnector.connect(user, '/drive/items/' + fileId + '/content', [
                    method : 'put',
                    headers: [
                            'Content-Type': contentType
                    ],
                    body   : localFilePath
            ])

            return  _graphResponse.status == 'ok'
        }

        static findMeetingTimes(String user, List<String> attendees) {
            def connOptions = [
                    method : 'post',
                    headers: ['Content-Type': 'application/json'],
                    body   : [attendees: []]
            ]

            attendees.each {
                connOptions.body.attendees.push([emailAddress: [address: it]])
            }

            def _graphResponse =  GraphConnector.connect(user, '/findMeetingTimes',
                    connOptions
            )

            if (_graphResponse.status == 'ok') {
                return _graphResponse.body.meetingTimeSuggestions
            } else {
                return [:]
            }
        }


    }

    class GraphTWCOutput {
        static makePersonCard(data) {
            return GraphTWCUtils.makePersonCard(data)
        }

        static makePeopleCarousel(data) {
            return GraphTWCUtils.makePeopleCarousel(data)
        }

        static makeEventTable(data, title) {
            return GraphTWCUtils.makeEventTable(data, [title: title])
        }

        static makeEventCards(data) {
            return GraphTWCUtils.makeEventCards(data)
        }

        static makeEmailList(data) {
            return GraphTWCUtils.makeEmailList(data)
        }

        static makeMeetingsTable(List meetings, String title) {
            return GraphTWCUtils.makeMeetingsTable(meetings, [title: title])
        }
    }

    class GraphTeamsOutput {
        static makeOutputJson(data) {
            return GraphTeamsUtils.output(data)
        }
    }
}
