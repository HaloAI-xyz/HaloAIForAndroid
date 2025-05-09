package xyz.haloai.haloai_android_productivity.services

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.fleeksoft.ksoup.Ksoup
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import xyz.haloai.haloai_android_productivity.R
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KSuspendFunction1
import kotlin.reflect.KSuspendFunction2
import kotlin.reflect.KSuspendFunction3

class MicrosoftGraphService(private val context: Context) {
    private var mMultipleAccountApp: IMultipleAccountPublicClientApplication? = null

    // private val TAG = MSGraphRequestWrapper::class.java.simpleName
    private val MS_GRAPH_ROOT_ENDPOINT = "https://graph.microsoft.com/"

    private var microsoftAccountList: MutableList<IAccount>? = null

    private val _allAccounts = MutableLiveData<MutableList<IAccount>>()
    val allAccounts: LiveData<MutableList<IAccount>>
        get() = _allAccounts

    /**
     * Use Volley to make an HTTP request with
     * 1) a given MSGraph resource URL
     * 2) an access token
     * to obtain MSGraph data.
     **/
    private fun callGraphAPIUsingVolley(
        context: Context,
        graphResourceUrl: String,
        accessToken: String,
        responseListener: Response.Listener<JSONObject>,
        errorListener: Response.ErrorListener
    ) {
        // Log.d(TAG, "Starting volley request to graph")

        /* Make sure we have a token to send to graph */
        if (accessToken.isEmpty()) {
            return
        }

        val queue: RequestQueue = Volley.newRequestQueue(context)
        val parameters = JSONObject()

        try {
            parameters.put("key", "value")
        } catch (e: Exception) {
            // Log.d(TAG, "Failed to put parameters: $e")
        }

        val request = object : JsonObjectRequest(Method.GET, graphResourceUrl, null, responseListener, errorListener) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $accessToken"
                return headers
            }
        }

        // Log.d(TAG, "Adding HTTP GET to Queue, Request: $request")

        request.retryPolicy = DefaultRetryPolicy(
            1000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(request)
    }

    init {
        setupMSAL(context)
    }
    fun setupMSAL(context: Context) {
        // Initialize the MSAL library
        PublicClientApplication.createMultipleAccountPublicClientApplication(
            context,
            R.raw.msal_config,
            object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                override fun onCreated(application: IMultipleAccountPublicClientApplication?) {
                    if (application != null) {
                        /*
                         * Acquire token interactively. It will also create an account object for the silent call as a result (to be obtained by getAccount()).
                         *
                         * If acquireTokenSilent() returns an error that requires an interaction,
                         * invoke acquireToken() to have the user resolve the interrupt interactively.
                         *
                         * Some example scenarios are
                         *  - password change
                         *  - the resource you're acquiring a token for has a stricter set of requirement than your SSO refresh token.
                         *  - you're introducing a new scope which the user has never consented for.
                         */
                        mMultipleAccountApp = application
                        // loadMicrosoftAccounts()
                    }
                }

                override fun onError(exception: MsalException) {
                    // Log.d(packageName, exception.exceptionName)
                    throw exception
                }
            })
    }

    suspend fun addMicrosoftAccount(context: Context, callback:
    KSuspendFunction1<Result<IAuthenticationResult>, Unit>, coroutineScope: CoroutineScope)
    {
        while (mMultipleAccountApp == null) {
            delay(1000)
        }
        // Acquire token interactively. It will also create an account object for the silent call as a result (to be obtained by getAccount())
        val parameters: AcquireTokenParameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(context as Activity)
            .withScopes(listOf("User.Read", "Calendars.Read", "Mail.Read"))
            .withCallback(
                object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    coroutineScope.launch(Dispatchers.IO) {
                        callback(Result.success(authenticationResult))
                    }
                }

                override fun onError(exception: MsalException) {
                    coroutineScope.launch(Dispatchers.IO) {
                        callback(Result.failure(exception))
                    }
                }

                override fun onCancel() {
                    coroutineScope.launch(Dispatchers.IO) {
                        callback(Result.failure(CancellationException("User cancelled authentication")))
                    }
                }
            })
            .build()
        mMultipleAccountApp!!.acquireToken(parameters)
    }

    suspend fun authenticateAccountForEventsFetch(context: Context, emailId: String, callback:
    KSuspendFunction3<Result<IAuthenticationResult>, Date?, Date?, Unit>, coroutineScope: CoroutineScope, startDate: Date?, endDate: Date?)
    {
        while (mMultipleAccountApp == null) {
            delay(1000)
        }
        val accountsList = getMicrosoftAccountsAdded(coroutineScope)
        val account = accountsList!!.find { it.username == emailId }
        // Acquire token interactively. It will also create an account object for the silent call as a result (to be obtained by getAccount())
        val parameters: AcquireTokenSilentParameters = AcquireTokenSilentParameters.Builder()
            .withScopes(listOf("User.Read", "Calendars.Read", "Mail.Read"))
            .withCallback(
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.success(authenticationResult), startDate, endDate)
                        }
                    }

                    override fun onError(exception: MsalException) {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.failure(exception), startDate, endDate)
                        }
                    }

                    override fun onCancel() {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.failure(CancellationException("User cancelled authentication")), startDate, endDate)
                        }
                    }
                })
            .forAccount(account)
            .fromAuthority("https://login.microsoftonline.com/common")
            .build()
        mMultipleAccountApp!!.acquireTokenSilentAsync(parameters)
    }

    suspend fun authenticateAccountForEmailsFetch(context: Context, emailId: String, callback:
    KSuspendFunction2<Result<IAuthenticationResult>, Date, Unit>, coroutineScope: CoroutineScope, date: Date)
    {
        while (mMultipleAccountApp == null) {
            delay(1000)
        }
        val accountsList = getMicrosoftAccountsAdded(coroutineScope)
        val account = accountsList!!.find { it.username == emailId }
        // Acquire token interactively. It will also create an account object for the silent call as a result (to be obtained by getAccount())
        val parameters: AcquireTokenSilentParameters = AcquireTokenSilentParameters.Builder()
            .withScopes(listOf("User.Read", "Calendars.Read", "Mail.Read"))
            .withCallback(
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.success(authenticationResult), date)
                        }
                    }

                    override fun onError(exception: MsalException) {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.failure(exception), date)
                        }
                    }

                    override fun onCancel() {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.failure(CancellationException("User cancelled authentication")), date)
                        }
                    }
                })
            .forAccount(account)
            .fromAuthority("https://login.microsoftonline.com/common")
            .build()
        mMultipleAccountApp!!.acquireTokenSilentAsync(parameters)
    }

    suspend fun authenticateAccountForConversationThreadFetch(context: Context, emailId: String,
                                                         callback:
    KSuspendFunction2<Result<IAuthenticationResult>, String, Unit>, coroutineScope: CoroutineScope,
                                                              conversationId: String)
    {
        while (mMultipleAccountApp == null) {
            delay(1000)
        }
        val accountsList = getMicrosoftAccountsAdded(coroutineScope)
        val account = accountsList!!.find { it.username == emailId }
        // Acquire token interactively. It will also create an account object for the silent call as a result (to be obtained by getAccount())
        val parameters: AcquireTokenSilentParameters = AcquireTokenSilentParameters.Builder()
            .withScopes(listOf("User.Read", "Calendars.Read", "Mail.Read"))
            .withCallback(
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.success(authenticationResult), conversationId)
                        }
                    }

                    override fun onError(exception: MsalException) {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.failure(exception), conversationId)
                        }
                    }

                    override fun onCancel() {
                        coroutineScope.launch(Dispatchers.IO) {
                            callback(Result.failure(CancellationException("User cancelled authentication")), conversationId)
                        }
                    }
                })
            .forAccount(account)
            .fromAuthority("https://login.microsoftonline.com/common")
            .build()
        mMultipleAccountApp!!.acquireTokenSilentAsync(parameters)
    }

    suspend fun callGraphAPIToFetchAllCalendarIDs(authenticationResult: IAuthenticationResult?):
            MutableList<Pair<String, String>> {

        if (authenticationResult == null) {
            throw Exception("Authentication result is null, please check if the user is authenticated.")
        }

        var future = CompletableFuture<JSONObject>()
        // Url: https://graph.microsoft.com/v1.0/me/calendars

        val url = URL(MS_GRAPH_ROOT_ENDPOINT + "v1.0/me/calendars")

        var finalResponseStrOfAllCalsJson = ""

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET" // Optional; default is GET
            setRequestProperty("Authorization", "Bearer ${authenticationResult.accessToken}")
            println("\nSent 'GET' request to URL: $url; Response Code: $responseCode")
            inputStream.bufferedReader().use { it.lines().forEach { line ->
                finalResponseStrOfAllCalsJson += line
            } }
        }

        // Parse the JSON response
        val parsedJson = JSONObject(finalResponseStrOfAllCalsJson)
        // Create a list of all calendar IDs and their names

        val calendarIdsAndNames = mutableListOf<Pair<String, String>>()
        for (i in 0 until parsedJson.getJSONArray("value").length()) {
            val calendar = parsedJson.getJSONArray("value").getJSONObject(i)
            calendarIdsAndNames.add(Pair(calendar.getString("id"), calendar.getString("name")))
        }
        return calendarIdsAndNames
    }

    suspend fun callGraphAPIToFetchAllEvents(authenticationResult: IAuthenticationResult?, calendarId: String, startDate: Date? = null, endDate: Date? = null):
            JSONObject {

        if (authenticationResult == null) {
            throw Exception("Authentication result is null, please check if the user is authenticated.")
        }

        val minDateTime: Date = startDate ?: Date()
        val maxDateTime: Date
        if (endDate == null) {
            maxDateTime = Date()
            maxDateTime.time = maxDateTime.time + 1000 * 60 * 60 * 24 * 14 // 14 days from now
        }
        else {
            maxDateTime = endDate
        }

        var allEventsJson: JSONObject? = null

        val url = URL(MS_GRAPH_ROOT_ENDPOINT + "v1.0/me/calendars/${calendarId}/calendarview" +
                "?startdatetime=${minDateTime.toInstant().toString()}" +
                "&enddatetime=${maxDateTime.toInstant().toString()}") // Replace with your API endpoint
        var finalResponseStrOfAllEventsJson = ""
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET" // Optional; default is GET
            setRequestProperty("Authorization", "Bearer ${authenticationResult.accessToken}")
            println("\nSent 'GET' request to URL: $url; Response Code: $responseCode")
            inputStream.bufferedReader().use { it.lines().forEach { line ->
                finalResponseStrOfAllEventsJson += line
            } }
        }
        // Parse the JSON response
        var parsedJsonOfEvents = JSONObject(finalResponseStrOfAllEventsJson)
        allEventsJson = JSONObject(finalResponseStrOfAllEventsJson)

        // Check if there are more events to fetch, using "odata.nextLink" presence check
        // Only make upto 5 API calls
        var maxApiCalls = 5
        var nextLink = ""
        var nextLinkUrl: URL
        while ((parsedJsonOfEvents.has("@odata.nextLink")) and (maxApiCalls > 0)) {
            // There are more events to fetch
            nextLink = parsedJsonOfEvents.getString("@odata.nextLink")
            nextLinkUrl = URL(nextLink)
            var finalResponseStrOfAllEventsJsonNext = ""
            with(nextLinkUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET" // Optional; default is GET
                setRequestProperty("Authorization", "Bearer ${authenticationResult.accessToken}")
                println("\nSent 'GET' request to URL: $nextLinkUrl; Response Code: $responseCode")
                inputStream.bufferedReader().use { it.lines().forEach { line ->
                    finalResponseStrOfAllEventsJsonNext += line
                } }
            }
            parsedJsonOfEvents = JSONObject(finalResponseStrOfAllEventsJsonNext)
            // Merge the two JSON objects
            val allEventsArray = allEventsJson.getJSONArray("value")
            val newEventsArray = parsedJsonOfEvents.getJSONArray("value")
            for (i in 0 until newEventsArray.length()) {
                allEventsArray.put(newEventsArray.getJSONObject(i))
            }
            allEventsJson.put("value", allEventsArray)
            maxApiCalls--
        }

        return allEventsJson
    }

    suspend fun callGraphAPIToFetchEmails(authenticationResult: IAuthenticationResult, date: Date, emailsToFetch: Int = 100): JSONObject {
        // Filter non-junk emails received after the given date
        // Url: https://graph.microsoft.com/v1.0/me/messages?$filter=(receivedDateTime gt 2024-07-11T00:00:00Z) and not (parentFolderId eq 'JunkEmail')

        val url = URL(MS_GRAPH_ROOT_ENDPOINT + "v1.0/me/messages" + "?\$filter=(receivedDateTime gt ${date.toInstant().toString()}) and not (parentFolderId eq 'JunkEmail')&\$top=${emailsToFetch}")
        var finalResponseStrOfAllEventsJson = ""
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET" // Optional; default is GET
            setRequestProperty("Authorization", "Bearer ${authenticationResult.accessToken}")
            println("\nSent 'GET' request to URL: $url; Response Code: $responseCode")
            inputStream.bufferedReader().use { it.lines().forEach { line ->
                finalResponseStrOfAllEventsJson += line
            } }
        }

        val allParsedEmails: JSONObject = JSONObject(finalResponseStrOfAllEventsJson)

        // Check if there are more emails to fetch, using "odata.nextLink" presence check
        // Only make upto 5 API calls
        var maxApiCalls = 5
        while ((allParsedEmails.has("@odata.nextLink")) and (maxApiCalls > 0)) {
            // There are more emails to fetch
            val nextLink = allParsedEmails.getString("@odata.nextLink")
            val nextLinkUrl = URL(nextLink)
            var finalResponseStrOfAllEventsJsonNext = ""
            with(nextLinkUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET" // Optional; default is GET
                setRequestProperty("Authorization", "Bearer ${authenticationResult.accessToken}")
                println("\nSent 'GET' request to URL: $nextLinkUrl; Response Code: $responseCode")
                inputStream.bufferedReader().use { it.lines().forEach { line ->
                    finalResponseStrOfAllEventsJsonNext += line
                } }
            }
            val parsedEmails = JSONObject(finalResponseStrOfAllEventsJsonNext)
            // Merge the two JSON objects
            val allEmailsArray = allParsedEmails.getJSONArray("value")
            val newEmailsArray = parsedEmails.getJSONArray("value")
            for (i in 0 until newEmailsArray.length()) {
                allEmailsArray.put(newEmailsArray.getJSONObject(i))
            }
            allParsedEmails.put("value", allEmailsArray)
            maxApiCalls--
        }

        val parsedEmails = JSONObject(finalResponseStrOfAllEventsJson)

        return parsedEmails
    }

    suspend fun callGraphAPIToFetchConversationThread(authenticationResult:
                                                      IAuthenticationResult, conversationId: String): JSONObject {
        // Filter non-junk emails received after the given date
        // Url: https://graph.microsoft.com/v1.0/me/messages?$filter=(receivedDateTime gt 2024-07-11T00:00:00Z) and not (parentFolderId eq 'JunkEmail')

        val url = URL(MS_GRAPH_ROOT_ENDPOINT + "v1.0/me/messages" + "?\$filter=conversationId eq " +
                "'${conversationId}'")
        var finalResponseStrOfAllEventsJson = ""
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET" // Optional; default is GET
            setRequestProperty("Authorization", "Bearer ${authenticationResult.accessToken}")
            println("\nSent 'GET' request to URL: $url; Response Code: $responseCode")
            inputStream.bufferedReader().use { it.lines().forEach { line ->
                finalResponseStrOfAllEventsJson += line
            } }
        }

        val allParsedEmails: JSONObject = JSONObject(finalResponseStrOfAllEventsJson)

        // Check if there are more emails to fetch, using "odata.nextLink" presence check
        // Only make upto 5 API calls
        var maxApiCalls = 5
        while ((allParsedEmails.has("@odata.nextLink")) and (maxApiCalls > 0)) {
            // There are more emails to fetch
            val nextLink = allParsedEmails.getString("@odata.nextLink")
            val nextLinkUrl = URL(nextLink)
            var finalResponseStrOfAllEventsJsonNext = ""
            with(nextLinkUrl.openConnection() as HttpURLConnection) {
                requestMethod = "GET" // Optional; default is GET
                setRequestProperty("Authorization", "Bearer ${authenticationResult.accessToken}")
                println("\nSent 'GET' request to URL: $nextLinkUrl; Response Code: $responseCode")
                inputStream.bufferedReader().use { it.lines().forEach { line ->
                    finalResponseStrOfAllEventsJsonNext += line
                } }
            }
            val parsedEmails = JSONObject(finalResponseStrOfAllEventsJsonNext)
            // Merge the two JSON objects
            val allEmailsArray = allParsedEmails.getJSONArray("value")
            val newEmailsArray = parsedEmails.getJSONArray("value")
            for (i in 0 until newEmailsArray.length()) {
                allEmailsArray.put(newEmailsArray.getJSONObject(i))
            }
            allParsedEmails.put("value", allEmailsArray)
            maxApiCalls--
        }

        val parsedConversationThread = JSONObject(finalResponseStrOfAllEventsJson)

        return parsedConversationThread
    }

    private suspend fun getMicrosoftAccountsAdded(coroutineScope: CoroutineScope): MutableList<IAccount>? {
        return suspendCancellableCoroutine<MutableList<IAccount>> { continuation ->
            // Authenticate user, then get calendar IDs
            coroutineScope.launch {
                mMultipleAccountApp!!.getAccounts(object : IPublicClientApplication.LoadAccountsCallback {
                    override fun onTaskCompleted(result: MutableList<IAccount>?) {
                        if (result != null) {
                            _allAccounts.postValue(result!!)
                        }
                    }

                    override fun onError(exception: MsalException?) {
                        // Log.d(packageName, exception!!.message!!)
                    }
                })
            }

            // Observe LiveData for results
            val observer = object : Observer<MutableList<IAccount>> {
                override fun onChanged(results: MutableList<IAccount>) {
                    _allAccounts.removeObserver(this)
                    if (continuation.isActive) {
                        continuation.resume(results) {
                            continuation.resumeWithException(it)
                        }
                    }
                }
            }

            coroutineScope.launch(Dispatchers.Main) {
                _allAccounts.observeForever(observer)
            }

            continuation.invokeOnCancellation {
                coroutineScope.launch(Dispatchers.Main) {
                    _allAccounts.removeObserver(observer)
                }
            }
        }
    }

    fun getEmailBody(email: JSONObject): String {
        var body = email.getJSONObject("body")
        var bodyType = body.getString("contentType")
        var bodyText = ""
        if (bodyType == "text") {
            bodyText = body.getString("content")
        }
        else if (bodyType == "html") {
            // Parse the html body
            bodyText = Ksoup.parse(body.getString("content")).text()
        }
        return bodyText
    }

    fun getLast3EmailsBody(conversation: JSONObject, emailId: String): String {
        var last3EmailsBody = ""
        val emails = conversation.getJSONArray("value")
        val allConversationIndexesLengthsToIdxMap = mutableMapOf<Int, Int>()
        for (i in 0 until emails.length()) {
            val email = emails.getJSONObject(i)
            val conversationId = email.getString("conversationIndex")
            allConversationIndexesLengthsToIdxMap[i] = conversationId.length
        }
        // Sort by length, pick top 3 (descending order)
        val sortedMap = allConversationIndexesLengthsToIdxMap.toList().sortedBy { (_, value) -> value }.toMap()
        val sortedMapDescending = sortedMap.toList().reversed().toMap()
        // Take subject of first email (smallest length conversationId)
        val subject = emails.getJSONObject(sortedMap.keys.toList()[0]).getString("subject")

        val last3EmailsIdx = sortedMapDescending.keys.toList().subList(0, 3.coerceAtMost(sortedMapDescending.size))
        last3EmailsBody += "Subject: $subject\n\n"

        for (i in last3EmailsIdx) {
            val email = emails.getJSONObject(i)
            val from = email.getJSONObject("sender").getJSONObject("emailAddress").getString("address")
            val to: String
            if (from != emailId)
            {
                to = emailId
            }
            else{
                to = email.getJSONArray("toRecipients").getJSONObject(0).getJSONObject("emailAddress").getString("address")
            }
            val body = getEmailBody(email)
            last3EmailsBody += "From: $from\nTo: $to\nBody: $body\n\n"
        }

        return last3EmailsBody
    }
}