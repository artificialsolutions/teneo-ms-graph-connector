/**
 * This class manages the connections to the Graph API and handles the responses.
 *
 */
class GraphConnector {
    /**
     * This variable stores the method that needs to be called to get a current auth token to connect to the Graph API.
     * It will be called every time a request is made to make sure the token is the current one.
     * See example in Main for how to bind the azureAuthHandler.getAuthToken method to this variable.
     */
    static def getAuthToken

    /**
     * This subclass is meant to hold static toolkit methods to explore and manipulate returning MS Graph data.
     */
    class DataUtils {
        /**
         * When the "$expand" filter is used in a query to graph it will contain nested objects that need to be processed into a list.
         * For example, if the query contains $expand: 'manager(levels=max)', the returning object can be made into a sequential list of managers, starting from the most immediate one and up the chain to the CEO.
         *
         * @param node - The object that contains the nested keys
         * @param recursionKey - The key in the nested objects to traverse
         * @param list - The list that contains the reordered objects.
         * @return list
         */
        public static List listValueToDepth(node, String recursionKey, ArrayList list = []) {
            list.push(node)
            if (node.containsKey(recursionKey)) {
                this.listValueToDepth(node[recursionKey], recursionKey, list)
            } else {
                return list
            }
        }

        public static String addSlashesToPaths(path){
            if (!path.startsWith('/')) {
                path = '/' + path
            }
            if (!path.endsWith('/')) {
                path += '/'
            }

            return path
        }
    }
/**
 *
 * @param user - either the userPrincipalName, userId, groups/groupId or 'me'
 * @param graphResource - The path to the Graph resource requested. Full documentation at https://docs.microsoft.com/en-us/graph/api/overview?view=graph-rest-1.0
 * @param connectionProperties - This object contains the details of the connection.
 * Other than the mandatory "method" field, it can contain a "query", "body" or "headers" fields with key:value pairs.
 * @throws MissingPropertyException - If the user parameter is empty or the connectionProperties do not contain mandatory "method" and "authToken" fields.
 * @return Map with field "body", which contains the body of the response or error, and "status", which can be one of
 * 'ok' : the requested operation was completed successfully
 * 'auth_fail': Authentication failed, get new token.
 * 'forbidden' : User is not allowed to make the request, probably a permission scope issue
 * 'not_found' : The requested resource was not found in the provided path
 * 'temp_error': The request failed but it can be attempted again.
 * 'generic_fail': The request failed and will probably fail again if attempted. A more detailed 'Error Code: ' is printed.
 */
    public static def connect(String user, String graphResource, Map<String, ?> connectionProperties) {
        if (!user || !connectionProperties.containsKey('method')) {
            throw new MissingPropertyException("User: " + user + " \nMethod: " + connectionProperties?.method)
        }

        String graphApiBaseUrl = 'graph.microsoft.com'
        String graphApiVersion = '1.0'
        //If user in not 'me' add 'users/' to username.
        if (user != 'me' && !user.contains('group')) {
            user = 'users/' + user
        }

        String connUrl = 'https://' + graphApiBaseUrl + '/v' + graphApiVersion + '/' + user + '/' + graphResource

        String authToken = getAuthToken()
        Map<String, String> headers = ['Authorization': 'Bearer ' + authToken, 'Accept': '*/*']

        if (connectionProperties.containsKey('headers')) {
            headers << connectionProperties.headers
        }

        def graphResp
        switch (connectionProperties.method.toLowerCase()) {
            case 'get':
                headers.put('ConsistencyLevel', 'eventual')
                graphResp = RestClient.get(connUrl, connectionProperties.queries ?: [:], headers);
                break
            case 'post':
                graphResp = RestClient.post(connUrl, [:], headers, connectionProperties.body)
                break
            case 'patch':
                graphResp = RestClient.patch(connUrl, [:], headers, connectionProperties.body)
                break
            case 'delete':
                graphResp = RestClient.delete(connUrl, headers);
                break
            case 'put':
                graphResp = RestClient.put(connUrl, [:], headers, connectionProperties.body)
        }

        String status = ''
        switch (graphResp.responseCode) {
            case 200:
            case 201:
            case 202:
            case 204:
                status = 'ok'
                break
            case 400:
            case 406:
            case 411:
            case 412:
            case 413:
            case 415:
            case 416:
            case 422:
            case 500:
            case 501:
            case 507:
                status = 'generic_fail'
                println('Error Code: ' + graphResp.responseCode)
                break
            case 401:
                status = 'auth_fail'
                break
            case 403:
                status = 'forbidden'
                break
            case 404:
            case 409:
            case 410:
                status = 'not_found'
                break
            case 423:
            case 429:
            case 503:
            case 504:
            case 509:
                status = 'temp_error'
                break
        }

        return [status: status, body: graphResp.responseBody]
    }


}