package com.orctech.dcdswarm1.Activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orctech.dcdswarm1.Helpers.CacheHelper;
import com.orctech.dcdswarm1.Helpers.CookieHelper;
import com.orctech.dcdswarm1.Models.Login;
import com.orctech.dcdswarm1.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import static com.orctech.dcdswarm1.Helpers.StringCropper.cropExclusive;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {
    public static final String USERNAME_PARAMETER = "com.example.DCDSwarm.USERNAME";
    public static final String PASSWORD_PARAMETER = "com.example.DCDSwarm.PASSWORD";
    
    private UserLoginTask mAuthTask = null;
    
    // UI references.
    private EditText mUsernameView;
    private EditText             mPasswordView;
    private View                 mProgressView;
    private View                 mLoginFormView;
    Toolbar t;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mProgressView = (ProgressBar) findViewById(R.id.login_progress);
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();//TODO: make actual login
                    return true;
                }
                return false;
            }
        });
        
        Button musernameSignInButton = (Button) findViewById(R.id.username_sign_in_button);
        musernameSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    
        t = (Toolbar) findViewById(com.orctech.dcdswarm1.R.id.toolbar_login);
        t.setTitle(getString(R.string.app_name));
        autoLogin();
    }
    
    private void autoLogin() {
        Login c = CacheHelper.getInstance().getLogin(this);
        mUsernameView.setText(c.getUsername());
        mPasswordView.setText(c.getPassword());
        if(!c.isEmpty())
            attemptLogin();
    }
    
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        
        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        
        // Store values at the time of the login attempt.
        
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        
        boolean cancel = false;
        View focusView = null;
        
        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        
        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }
        
        if (cancel) {
            focusView.requestFocus();
        } else {
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }
    
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        
        private final String mUsername;
        private final String mPassword;
        private String message = getString(R.string.error_failed_login);
        HttpURLConnection portal;
        
        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            
            try {
                URL url = new URL(getString(R.string.URL_schedule_request));
                portal = (HttpURLConnection) url.openConnection();
                portal.setUseCaches(true);
                portal.setRequestMethod("POST");
                portal.setConnectTimeout(30000);
                portal.setInstanceFollowRedirects(false);

                String urlParameters = String.format("do=login&p=413&username=%s&password=%s&submit=login", URLEncoder.encode(mUsername, "UTF-8"), URLEncoder.encode(mPassword, "UTF-8"));
                
                portal.setRequestProperty("Content-Length", Integer.toString(urlParameters.length()));
                
                portal.setDoInput(true);
                portal.setDoOutput(true);
                
                // Send post request
                DataOutputStream wr = new DataOutputStream(portal.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();
                
                int responseCode = portal.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + urlParameters);
                System.out.println("Response Code : " + responseCode);
                
                CookieHelper.getCookieHelper().storeCookies(portal);
                
                BufferedReader in = new BufferedReader(new InputStreamReader(portal.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                if (!checkLoggedIn(response.toString(), portal)) {
                    if (portal.getResponseCode() >= 300 || portal.getResponseCode() < 200) {
                        message = portal.getResponseMessage();
                    }
                    return false;
                } else {
                    return true;
                }
                
            } catch (ProtocolException e) {
                e.printStackTrace();
                return false;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                if (portal != null) {
                    portal.disconnect();
                }
            }
        }
        
        @Override
        protected void onPreExecute() {
            showProgress(true);
        }
        
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            
            if (success) {
                successfulLogin();
            } else {
                unsuccessfulLogin(message);
            }
        }
        
        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
    
    private void unsuccessfulLogin(String message) {
        CacheHelper.getInstance().clearLogin(this);
        mPasswordView.setError(message);
        mPasswordView.requestFocus();
    }
    
    private void showProgress(boolean b) {
        mProgressView.setVisibility((b ? View.VISIBLE : View.INVISIBLE));
    }
    
    public void successfulLogin() {
        Login c = new Login(mUsernameView.getText().toString(), mPasswordView.getText().toString());
        CacheHelper.getInstance().storeLogin(this, c);
        mUsernameView.setText("");
        mPasswordView.setText("");
        Intent intent = new Intent(this, com.orctech.dcdswarm1.Activities.MainActivity.class);
        //      intent.putExtra(USERNAME_PARAMETER, c.getUsername());
        //      intent.putExtra(PASSWORD_PARAMETER, c.getPassword());
        startActivity(intent);
    }
    
    static boolean checkLoggedIn(String htmlString, HttpURLConnection conn) {
        try {
            int code = conn.getResponseCode();
            if (code == 200) {
                String loginCheck = cropExclusive(htmlString, "<meta name=\"description\" content=\"", " - Detroit");
                if (loginCheck == null) {
                    return false;
                }
                switch (loginCheck) {
                    case "Member Login":
                        return false;
                    case "STUDENT PORTAL":
                        //TODO: check for parents too
                        return true;
                    case "Academics Individual Classes Calendar":
                        return true;
                }
                return false;
            } else if (code == 302 && conn.getHeaderField("Location").equals("https://www.dcds.edu/page.cfm?p=8256")) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}