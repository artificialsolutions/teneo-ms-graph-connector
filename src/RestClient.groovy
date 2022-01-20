import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.codec.net.URLCodec
import org.apache.commons.io.IOUtils

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path

/**
 * This is a generic class to handle requests to any REST API. It can handle GET, POST, PUT, DELETE and PATCH methods.
 */
class RestClient {
    /**
     *
     * @param url - The full path of the REST service endpoint without query parameters
     * @param query - A Map with key-value pairs to be encoded in the query string
     * @param headers - A Map with the request headers and their values
     * @return A Map with fields 'responseCode' and 'responseBody'. If 'responseCode' is an error code, error body is passed in 'responseBody'
     */
    static def get(String url, Map<String,String> query, Map<String,String> headers) {
        def requestProperties = normalizeRequestProperties(url, query, headers, [:])
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestProperties.url)).GET()
        requestProperties.headers.each {
            requestBuilder.header(it.key as String, it.value as String)
        }
        HttpRequest request = requestBuilder.build()
        HttpClient client = HttpClient.newHttpClient()

        return handleResponse(client.send(request, HttpResponse.BodyHandlers.ofInputStream()))
    }

    /**
     *
     * @param url - The full path of the REST service endpoint
     * @param headers - A Map with the request headers and their values
     * @return A Map with fields 'responseCode' and 'responseBody'. If 'responseCode' is an error code, error body is passed in 'responseBody'
     */
    static def delete(String url, Map headers) {
        def requestProperties = normalizeRequestProperties(url, [:], headers, [:])
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestProperties.url)).DELETE()
        requestProperties.headers.each {
            requestBuilder.header(it.key as String, it.value as String)
        }
        HttpRequest request = requestBuilder.build()
        HttpClient client = HttpClient.newHttpClient()

        return handleResponse(client.send(request, HttpResponse.BodyHandlers.ofInputStream()))
    }

    /**
     *
     * @param url - The full path of the REST service endpoint without query parameters
     * @param query - A Map with key-value pairs to be encoded in the query string
     * @param headers - A Map with the request headers and their values
     * @param body - A Map with the elements that will make up the body of the request
     * @return A Map with fields 'responseCode' and 'responseBody'. If 'responseCode' is an error code, error body is passed in 'responseBody'
     */
    static def post(String url, Map<String, String> query, Map<String, String> headers, def body) {
        def requestProperties = normalizeRequestProperties(url, query, headers, body)
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestProperties.url)).POST(HttpRequest.BodyPublishers.ofString(requestProperties.body as String))
        requestProperties.headers.each {
            requestBuilder.header(it.key as String, it.value as String)
        }
        HttpRequest request = requestBuilder.build()
        HttpClient client = HttpClient.newHttpClient()

        return handleResponse(client.send(request, HttpResponse.BodyHandlers.ofInputStream()))
    }

    /**
     *
     * @param url - The full path of the REST service endpoint without query parameters
     * @param query - A Map with key-value pairs to be encoded in the query string
     * @param headers - A Map with the request headers and their values
     * @param body - A Map with the elements that will make up the body of the request
     * @return A Map with fields 'responseCode' and 'responseBody'. If 'responseCode' is an error code, error body is passed in 'responseBody'
     */
    static def put(String url, Map<String, String> query, Map<String, String> headers, def body) {
        def requestProperties = normalizeRequestProperties(url, query, headers, body)
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestProperties.url))
                .PUT( requestProperties.body == 'isFile' ?
                        HttpRequest.BodyPublishers.ofFile(Path.of(body)) :
                        HttpRequest.BodyPublishers.ofString(requestProperties.body as String)
                )
        requestProperties.headers.each {
            requestBuilder.header(it.key as String, it.value as String)
        }
        HttpRequest request = requestBuilder.build()
        HttpClient client = HttpClient.newHttpClient()

        return handleResponse(client.send(request, HttpResponse.BodyHandlers.ofInputStream()))
    }

    /**
     *
     * @param url - The full path of the REST service endpoint without query parameters
     * @param query - A Map with key-value pairs to be encoded in the query string
     * @param headers - A Map with the request headers and their values
     * @param body - A Map with the elements that will make up the body of the request
     * @return A Map with fields 'responseCode' and 'responseBody'. If 'responseCode' is an error code, error body is passed in 'responseBody'
     */
    static def patch(String url, Map<String, String> query, Map<String, String> headers, def body) {
        def requestProperties = normalizeRequestProperties(url, query, headers, body)
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(requestProperties.url)).method("PATCH", HttpRequest.BodyPublishers.ofString(requestProperties.body))
        requestProperties.headers.each {
            requestBuilder.header(it.key as String, it.value as String)
        }
        HttpRequest request = requestBuilder.build()

        HttpClient client = HttpClient.newHttpClient()
        HttpResponse<Object> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        return handleResponse(response)

    }


    /**
     * This method normalizes the properties of all the connection methods and builds them up according to the content-type header.
     * @param url - The full path of the REST service endpoint without query parameters. Must have a value.
     * @param query - A Map with key-value pairs to be encoded in the query string.  Can be empty object for no query string.
     * @param headers - A Map with the request headers and their values. Must have at least a 'Content-Type' header.
     * @param body - A Map with the elements that will make up the body of the request.  Can be empty object for no body.
     * @return A Map with fields String 'url' and Map<String,String> 'headers' for all requests. If parameters 'query' or 'body'
     * are not empty objects it will also return a 'query' or 'body' field in the right format.
     */
    private static def normalizeRequestProperties(String url, Map<String, String> query, Map<String, String> headers, def body) {
        Map<String, ?> normalizedProperties = [url: url, headers: headers]

        //Add query to url
        if (query.size() > 0) {
            normalizedProperties.put('url', url + '?' + makeEncodedString(query, true))
        }


        //Encode body
        if (body.size() > 0) {
            switch (headers['Content-Type']) {
                case 'application/x-www-form-urlencoded':
                    normalizedProperties.put('body', makeEncodedString(body, true))
                    break
                case ~/application\/json.*/:
                    normalizedProperties.put('body', new JsonBuilder(body).toString())
                    break
                case ~/(image|document).*/:
                    normalizedProperties.put('body', 'isFile')
                    break
                default:
                    normalizedProperties.put('body', body)
            }
        }

        return normalizedProperties

    }

    /**
     *
     * @param map Key-pair values to be encoded into a string for Query parameters or request Body.
     * @param encode If true it will encode the string using Apache Commons URL Encode. Otherwise it will just produce the string.
     * @return The string with the Body/Query params
     */
    private static def makeEncodedString(Map<String, ?>map, Boolean encode) {
        StringBuilder bodyString = new StringBuilder("")
        for (Map.Entry<String, Object> item : map.entrySet()) {
            if (bodyString.toString().length() != 0) {
                bodyString.append('&')
            }
            URLCodec urlCodec = new URLCodec()
            def currentItem = encode ? URLEncoder.encode(item.getKey(), "UTF-8") : item.getKey()
            def currentValue = encode ? URLEncoder.encode(item.getValue().toString(), "UTF-8") : urlCodec.encode(item.getValue().toString())
            bodyString.append(currentItem)
            bodyString.append('=')
            bodyString.append(currentValue)
        }
        return bodyString.toString()
    }

    /**
     *
     * @param response The server response as an Input Stream
     * @return A Map with the response code and the response/error body.
     */
    private static def handleResponse(HttpResponse<Object> response) {
        def responseCode = response.statusCode()
        InputStream inputStream = response.body()
        if (responseCode >= 200 && responseCode < 400) {
            def responseContentType = response.headers().allValues('content-type')[0]

            def responseBody
            switch (responseContentType) {
                case ~/application\/json.*/:
                    String bodyText = inputStream.getText()
                    def slurper = new JsonSlurper()
                    responseBody = slurper.parseText(bodyText)
                    break
                case ~/image.*/:
                    byte[] bytes = IOUtils.toByteArray(inputStream)
                    responseBody = Base64.getEncoder().encodeToString(bytes)
                    break
                default:
                    responseBody = inputStream.getText()
            }
            inputStream.close()

            println(response.request().method() + " call succeeded with code: " + responseCode.toString())
            return [responseCode: responseCode, responseBody: responseBody]
        } else {
            println(response.request().method() + " call failed with code: " + responseCode.toString())
            return [responseCode: responseCode, responseBody: inputStream.getText()]
        }
    }
}
