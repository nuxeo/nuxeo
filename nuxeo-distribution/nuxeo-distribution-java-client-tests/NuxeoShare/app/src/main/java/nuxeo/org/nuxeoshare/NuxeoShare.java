package nuxeo.org.nuxeoshare;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.user.User;
import org.nuxeo.client.internals.spi.NuxeoClientException;

/**
 * A login screen that offers login via email/password.
 */
public class NuxeoShare extends AppCompatActivity {

    public static final String PREFS_NAME = "NuxeoPrefsFile";

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mUrlView;
    private NuxeoClient nuxeoClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_nuxeo_share);
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    store();
                    return true;
                }
                return false;
            }
        });
        mUrlView = (EditText) findViewById(R.id.url);
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                store();
            }
        });

        Button mClear = (Button) findViewById(R.id.clear);
        mClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                clear();
            }
        });

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String login = settings.getString("login", "");
        String pwd = settings.getString("pwd", "");
        String url = settings.getString("url", "");

        mEmailView.setText(login);
        mPasswordView.setText(pwd);
        mUrlView.setText(url);

    }

    private void clear() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        settings.edit().clear().commit();
        mPasswordView.setText("");
        mUrlView.setText("");
        mEmailView.setText("");
        Toast.makeText(getApplicationContext(), "Data Removed", Toast.LENGTH_SHORT).show();
    }

    private void store() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mUrlView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String url = mUrlView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
            return;
        }

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
            return;
        }

        if (TextUtils.isEmpty(url)) {
            mUrlView.setError(getString(R.string.error_field_required));
            focusView = mUrlView;
            cancel = true;
            return;
        }

        try {
            System.setProperty("log4j2.disable.jmx", "true");
            NuxeoClient nuxeoClient = new NuxeoClient(url, email, password);
            nuxeoClient.fetchCurrentUser();
        } catch (Exception reason) {
            Toast.makeText(getApplicationContext(), "Wrong information. Please change your credentials or url.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("login", email);
        editor.putString("pwd", password);
        editor.putString("url", url);
        // Message done.
        boolean success = editor.commit();
        if(success){
            Toast.makeText(getApplicationContext(), "Data Stored. You can now use the application when sharing documents.", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), "An error occured", Toast.LENGTH_SHORT).show();
        }
    }

}

