/**
 * The purpose of this class is to provide methods for the exchange of  oauth codes for oauth tokens and the refresh of tokens when they expire.
 * These tokens will allow for subsequent requests to Azure services to carry an Authorization header that will identify the user and authorize the requests.
 * If a token and refresh code is procured elsewhere, this class includes support so that they can also be set and refreshed.
  */

class AzureAuthHandler {
        private String authToken
        private String refreshToken
        private int tokenExpireSafetyBuffer
        private Timer refreshTimer = new Timer();
        private LinkedHashMap<String, String> queryMap = new LinkedHashMap<String, String>()
        private String authUrl
        private String loginUrl
        private LinkedHashMap<String, String> headerMap = ["Content-Type": "application/x-www-form-urlencoded"]
        private LinkedHashMap<String, Object> body

        /**
         *
         * @param aadOptions - This object contains the settings for the connection to the Azure Active Directory.
         * @description - The aadOptions object requires the following fields
         * 
         redirectUri            : The URI setup in the application in Azure -> Home -> App Registrations -> [Your App] -> Redirect URIs. This should be the URL of the page the user is redirected to after login.
         clientId               : The ID of the application as it appears  in Azure -> Home -> App Registrations -> [Your App]  -> Overview
         clientSecret           : The client secret gotten from Azure -> Home -> App Registrations -> [Your App] -> Certificates & secrets
         tenantId               : Your organization's Tenant ID, also from the overview. To authenticate against the universal Microsoft AD (to allow @hotmail, @live and @outlook accounts to authenticate), use 'common' as the tenantID.
         scopes                 : This is a List of scopes the token is being authorized for. The contents of this list can be copied from Azure -> Home -> App Registrations -> [Your App] -> API Permissions. Make sure to convert all the characters to lower-case.
         tokenExpireSafetyBuffer: Defaults to 60000. The amount of milliseconds that will be shaved off the Token Expiry time so that refresh requests are always within the valid time range.
         ]
         */
    AzureAuthHandler(Object aadOptions) {
            this.authUrl = 'https://login.microsoftonline.com/' + aadOptions.tenantId + '/oauth2/v2.0/'
            this.body = [client_id: aadOptions.clientId, redirect_uri: aadOptions.redirectUri, client_secret: aadOptions.clientSecret]
            this.tokenExpireSafetyBuffer = aadOptions.tokenExpireSafetyBuffer ?: 60000
            this.loginUrl = this.authUrl + 'authorize' + '?client_id=' + aadOptions.clientId + '&response_type=code&redirect_uri=' + aadOptions.redirectUri + '&response_mode=query&scope=' + aadOptions.scopes.join('%20')
        }

/**
 * This method is the main way of getting tokens from this class.
 * @param authCode - This is the long alphanumeric string that a successful login returns. This auth code will then be exchanged for an oauth token.
 * @param refresh - Defaults to true. This switch will start the automatic refresh process for the token, which means that after X seconds (where X is the Expiry Time minus de Safety Buffer) the system will automatically refresh the auth token and set a new one in it's place so the user is never interrupted. Use false if no token refresh is needed.
 * @return True if code successfully exchanged for token, false if failed.
 */
        public def exchangeCodeForToken(String authCode, Boolean refresh = true) {
            def _body = this.body
            _body.grant_type = "authorization_code"
            _body.code = authCode
            def authResponse = RestClient.post(authUrl + 'token', queryMap, headerMap, _body)
            if (authResponse.responseCode == 200) {
                this.authToken = authResponse.responseBody["access_token"].toString()
                if (refresh) {
                    this.startTokenRefreshCycle(authResponse.responseBody["access_token"].toString(), authResponse.responseBody["refresh_token"].toString(), authResponse.responseBody["expires_in"] as int * 1000)
                }
                return true
            }
            else{
                return false
            }
        }

/**
 * This method allows for authorization and refresh tokens to be set so that a new auth token is procured when the old one is about to expire.
 * @param authToken - The current oauth token
 * @param refreshToken - The current refresh token
 * @param tokenExp - The token expiration time in milliseconds.
 */
        public void startTokenRefreshCycle(String authToken, String refreshToken, int tokenExp) {
            tokenExp -= this.tokenExpireSafetyBuffer as int

            this.authToken = authToken
            this.refreshToken = refreshToken
            this.refreshTimer.scheduleAtFixedRate(
                    new TimerTask() {
                        @Override
                        void run() {
                            refreshTokens()
                        }
                    },
                    tokenExp,
                    tokenExp
            )
        }

        /**
         *
         * @return The URL where the user can log into their AAD acount.
         */
        public String getLoginUrl() {
            return this.loginUrl
        }

        /**
         *
         * @return The value of the current oauth token.
         */
        public String getAuthToken() {
            return this.authToken
        }

/**
 * Blanks the oauth token value and stops the refresh cycle.
 *
 */
        public void clearAuth() {
            authToken = ''
            this.refreshToken = ''
            this.refreshTimer.cancel()
        }


        /**
         * Sends the refresh token to AAD to get a new set of tokens with an extended expiry time. Will clear auth if it fails.
         */
        private void refreshTokens() {
            def _body = this.body
            _body.grant_type = "refresh_token"
            _body.refresh_token = this.refreshToken
            def authResponse = RestClient.post(authUrl + 'token', queryMap, headerMap, _body)
            if (authResponse.responseCode == 200) {
                this.authToken = authResponse.responseBody["access_token"]
                this.refreshToken = authResponse.responseBody["refresh_token"]
            } else {
                clearAuth()
            }
        }
    }



