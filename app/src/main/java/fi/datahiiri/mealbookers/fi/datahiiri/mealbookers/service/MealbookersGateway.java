package fi.datahiiri.mealbookers.fi.datahiiri.mealbookers.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import fi.datahiiri.mealbookers.LoginActivity;
import fi.datahiiri.mealbookers.exceptions.ConflictException;
import fi.datahiiri.mealbookers.exceptions.ForbiddenException;
import fi.datahiiri.mealbookers.exceptions.MealbookersException;
import fi.datahiiri.mealbookers.exceptions.NotSucceededException;
import fi.datahiiri.mealbookers.models.AcceptSuggestionResult;
import fi.datahiiri.mealbookers.models.BasicResult;

/**
 * Created by Lisa och Ilkka on 19.2.2015.
 */
public class MealbookersGateway {
    static final String TAG = "MealbookersGateway";

    /**
     * Try to login to the server
     * @param email
     * @param password
     * @return
     */
    public static boolean login(String email, String password, Context context) throws NotSucceededException, IOException, MealbookersException {

        try {
            Log.d("MealbookersGateway", "http://mealbookers.net/mealbookers/api/1.0/user/login");
            // Build the request
            HttpPost request = new HttpPost("http://mealbookers.net/mealbookers/api/1.0/user/login");

            JSONObject jsonObj = new JSONObject();
            jsonObj.put("email", email);
            jsonObj.put("password", password);
            jsonObj.put("remember", true);

            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            request.setEntity(entity);

            String result;

            try {
                result = sendRequest(request, context);
            }
            catch (ConflictException e) {
                Log.w("MealbookersGateway", "login failed");
                return false;
            }

            if (result == null) {
                throw new IOException("result was null");
            }
            JSONObject user = new JSONObject(result);
            if (user == null) {
                throw new IOException("json user was null");
            }
            else {
                String status = user.getString("status");
                if (status == null || !status.equals("ok")) {
                    throw new MealbookersException("status was not \"ok\"");
                }
                else {
                    return true;
                }
            }
        }
        catch (JSONException e) {
            Log.e("MealbookersGateway", "error", e);
            throw new IOException("Json exception", e);
        }
        catch (UnsupportedEncodingException e) {
            Log.e("MealbookersGateway", "error", e);
            throw new IOException("Unsuportted encoding", e);
        }
    }

    /**
     * Try to get user from ther server
     * @return
     */
    public static String getUser(Context context) throws NotSucceededException, IOException, MealbookersException {

        try {
            Log.d("MealbookersGateway", "http://mealbookers.net/mealbookers/api/1.0/user");
            // Build the request
            HttpGet request = new HttpGet("http://mealbookers.net/mealbookers/api/1.0/user");

            String result;

            result = sendRequest(request, context);

            if (result == null) {
                throw new IOException("result was null");
            }

            return result;
        }
        catch (UnsupportedEncodingException e) {
            Log.e("MealbookersGateway", "error", e);
            throw new IOException("Unsuportted encoding", e);
        }
    }

    /**
     * Removes the cookies
     */
    public static void logout(Context context) {
        removeCookies(context);
    }

    /**
     * Accepts a suggestion
     * @return suggestion time as hh:mm
     */
    public static String acceptSuggestion(String token, Context context) throws IOException, NotSucceededException {
        Log.d("MealbookersGateway", "sending");

        // Build the request
        HttpPost request = new HttpPost("http://mealbookers.net/mealbookers/api/1.0/suggestion/" + token);

        String result;
        try {
            result = sendRequest(request, context);
        } catch (ForbiddenException e) {
            Log.w("MealbookersGateway", "status code was 403");
            throw e;
        } catch (NotSucceededException e) {
            Log.w("MealbookersGateway", "status code was not 200");
            throw e;
        }

        if (result == null) {
            return "";
        }

        AcceptSuggestionResult acceptSuggestionResult = getGson().fromJson(result, AcceptSuggestionResult.class);
        if (acceptSuggestionResult != null) {
            return acceptSuggestionResult.time;
        }
        else {
            return "";
        }
    }

    /**
     * Sends GCM id to server
     * @return
     */
    public static void sendGCMRegid(String regid, Context context) throws NotSucceededException, IOException, MealbookersException {

        try {
            Log.d("MealbookersGateway", "sending");

            // Build the request
            HttpPost request = new HttpPost("http://mealbookers.net/mealbookers/api/1.0/user/registerAndroidGCM");

            JSONObject jsonObj = new JSONObject();
            jsonObj.put("regid", regid);

            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            request.setEntity(entity);

            String result;
            try {
                result = sendRequest(request, context);
            } catch (ForbiddenException e) {
                Log.w("MealbookersGateway.sendGCMRegid", "status code was 403");
                throw e;
            } catch (NotSucceededException e) {
                Log.w("MealbookersGateway.sendGCMRegid", "status code was not 200");
                throw e;
            }

            if (result == null) {
                throw new IOException("sendGCMRegid result was null");
            }

            BasicResult registerGCMResult = getGson().fromJson(result, BasicResult.class);

            if (registerGCMResult == null) {
                throw new IOException("registerGCMResult was null after json decode");
            } else {
                if (!registerGCMResult.status.equals("ok")) {
                    throw new MealbookersException("status was not ok");
                }
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "Json Exception", e);
            throw new IOException(e);
        }
    }

    /**
     * Notifies about a received notification
     * @return
     */
    public static void markNotificationReceived(int notificationId, Context context) throws NotSucceededException, IOException, MealbookersException {

        try {
            Log.d("MealbookersGateway.sendGCMRegid", "sending");

            // Build the request
            HttpPost request = new HttpPost("http://mealbookers.net/mealbookers/api/1.0/user/notificationReceived");

            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", notificationId);

            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            request.setEntity(entity);

            String result;
            try {
                result = sendRequest(request, context);
            } catch (ForbiddenException e) {
                Log.w("MealbookersGateway.markNotificationReceived", "status code was 403");
                throw e;
            } catch (NotSucceededException e) {
                Log.w("MealbookersGateway.markNotificationReceived", "status code was not 200");
                throw e;
            }

            if (result == null) {
                throw new IOException("markNotificationReceived result was null");
            }

            BasicResult markNotificationResult = getGson().fromJson(result, BasicResult.class);

            if (markNotificationResult == null) {
                throw new IOException("markNotificationResult was null after json decode");
            } else {
                if (!markNotificationResult.status.equals("ok")) {
                    throw new MealbookersException("status was not ok");
                }
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "Json Exception", e);
            throw new IOException(e);
        }
    }

    /**
     * Sends a request to the server and returns the result as a string. Saves the cookies.
     * @param request
     * @return
     */
    private static String sendRequest(HttpUriRequest request, Context context) throws NotSucceededException, IOException {
        InputStream inputStream;
        String result;

        // Create a local instance of cookie store
        CookieStore cookieStore = importCookies(context);

        // Create local HTTP context
        HttpContext localContext = new BasicHttpContext();
        // Bind custom cookie store to the local context
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        // create HttpClient
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.setCookieStore(cookieStore);

        // make GET request to the given URL
        HttpResponse httpResponse = httpclient.execute(request, localContext);

        // receive response as inputStream
        inputStream = httpResponse.getEntity().getContent();

        // convert inputstream to string
        if (inputStream != null) {
            result = convertInputStreamToString(inputStream);
            Log.d("mealbookers gateway", "result text was " + result);

            // get response status code
            if (httpResponse.getStatusLine().getStatusCode() == 403) {
                throw new ForbiddenException();
            }
            else if (httpResponse.getStatusLine().getStatusCode() == 409) {
                throw new ConflictException();
            }
            else if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new NotSucceededException();
            }

            saveCookies(cookieStore, context);
            Log.d("mealbookers gateway", "got cookies " + Integer.toString(cookieStore.getCookies().size()));
        } else {
            throw new IOException("Input stream was null");
        }

        return result;
    }

    /**
     * Saves the cookies to shared preferences
     * @param cookieStore
     * @param context
     */
    private static void saveCookies(CookieStore cookieStore, Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(LoginActivity.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        prefs.edit().putString("mealbookers-cookies", getCookieExportString(cookieStore.getCookies())).apply();
    }

    /**
     * Removes the cookies
     * @param context
     */
    private static void removeCookies(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(LoginActivity.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        prefs.edit().remove("mealbookers-cookies").apply();
    }

    /**
     * Formats cookies into an exportable string
     * @param cookies
     * @return
     */
    private static String getCookieExportString(List<Cookie> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Cookie cookie : cookies) {
            Date expiryDate = cookie.getExpiryDate();
            if (expiryDate == null) {
                expiryDate = new Date(2098957120000L);
            }
            sb.append(cookie.getDomain());
            sb.append("|");
            sb.append(cookie.getName());
            sb.append("|");
            sb.append(cookie.getValue());
            sb.append("|");
            sb.append(expiryDate.getTime());
            sb.append("||");
        }
        String string = sb.toString();
        return string.substring(0, string.length() - 2);
    }

    /**
     * Imports cookies from the exported cookie string
     * @param context
     * @return
     */
    private static CookieStore importCookies(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(LoginActivity.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        String exportedCookies = prefs.getString("mealbookers-cookies", null);

        CookieStore cookieStore = new BasicCookieStore();

        if (exportedCookies == null) {
            return cookieStore;
        }

        String[] cookiesString = exportedCookies.split("\\|\\|");
        for (String cookieString : cookiesString) {
            String[] cookieParts = cookieString.split("\\|");
            String domain = cookieParts[0];
            String name = cookieParts[1];
            String value = cookieParts[2];
            Date expiryDate = new Date(Long.parseLong(cookieParts[3]));

            BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setExpiryDate(expiryDate);
            cookie.setDomain(domain);

            cookieStore.addCookie(cookie);
        }
        return cookieStore;
    }

    /**
     * Helper function for receiving data from network.
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }

    /**
     * Get gson object
     * @return
     */
    protected static Gson getGson() {
        return new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
    }
}
