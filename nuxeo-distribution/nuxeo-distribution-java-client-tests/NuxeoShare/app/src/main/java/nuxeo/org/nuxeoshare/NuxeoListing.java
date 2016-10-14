package nuxeo.org.nuxeoshare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.nuxeo.client.api.NuxeoClient;
import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.Documents;
import org.nuxeo.client.api.objects.upload.BatchUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NuxeoListing extends AppCompatActivity {

    private NuxeoClient nuxeoClient;
    private Document currentDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_nuxeo_listing);
        // Get intent, action and MIME type
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        // Check data config
        SharedPreferences settings = getSharedPreferences(NuxeoShare.PREFS_NAME, 0);
        String login = settings.getString("login", "");
        String pwd = settings.getString("pwd", "");
        String url = settings.getString("url", "");
        if (login.isEmpty() || url.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(getApplicationContext(), "You should first configure your data.", Toast.LENGTH_SHORT).show();
            Intent newIntent = new Intent(this, NuxeoShare.class);
            startActivity(newIntent);
            finish();
            return;
        }

        // Prepare nx connection
        System.setProperty("log4j2.disable.jmx", "true");
        nuxeoClient = new NuxeoClient(url, login, pwd);
        currentDocument = nuxeoClient.repository().fetchDocumentRoot();

        // Handle listing
        Documents children = currentDocument.fetchChildren();
        final ListView listview = (ListView) findViewById(R.id.nxListView);
        NuxeoItemAdapter adapter = new NuxeoItemAdapter(this, children);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final Document item = (Document) parent.getItemAtPosition(position);
                Documents children = nuxeoClient.repository().fetchChildrenByPath(item.getPath());
                currentDocument = item;
                NuxeoItemAdapter adapter = new NuxeoItemAdapter(view.getContext(), children);
                listview.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        });

        final Button button = (Button) findViewById(R.id.nxbutton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Intent.ACTION_SEND.equals(action) && type != null) {
                    if ("text/plain".equals(type)) {
                        handleSendImage(intent); // Handle text being sent
                    } else if (type.startsWith("image/")) {
                        handleSendImage(intent); // Handle single image being sent
                    }
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
                    if (type.startsWith("image/")) {
                        handleSendMultipleImages(intent); // Handle multiple images being sent
                    }
                } else {
                    // Handle other intents, such as being started from the home screen
                }
            }
        });
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            String path = getPath(imageUri);
            File file = new File(path);

            // With batchupload
            BatchUpload batchUpload = nuxeoClient.fetchUploadManager();
            batchUpload = batchUpload.upload(file.getName(), file.length(), "", batchUpload.getBatchId(), "1", file);
            Document doc = new Document("file", "File");
            doc.setPropertyValue("dc:title", file.getName());
            Document androidFile = nuxeoClient.repository().createDocumentByPath(currentDocument.getPath(), doc);
            androidFile.setPropertyValue("file:content", batchUpload.getBatchBlob());
            androidFile = androidFile.updateDocument();

            if (androidFile != null) {
                Toast.makeText(getApplicationContext(), "File uploaded.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "An error occurs.", Toast.LENGTH_SHORT).show();
            }
            this.finish();
            // With Automation (but would be with a file already created - it's just for illustration)
            // Blob fileBlob = new Blob(file);
            // fileBlob = nuxeoClient.automation().newRequest("Blob.AttachOnDocument").param("document", "/default-domain/UserWorkspaces/vpasquier/android").input(fileBlob).execute();
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            // Update UI to reflect multiple images being shared
        }
    }
}
