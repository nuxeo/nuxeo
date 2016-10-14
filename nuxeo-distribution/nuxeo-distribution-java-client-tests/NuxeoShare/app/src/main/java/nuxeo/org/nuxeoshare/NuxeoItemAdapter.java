package nuxeo.org.nuxeoshare;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.nuxeo.client.api.objects.Document;
import org.nuxeo.client.api.objects.Documents;

class NuxeoItemAdapter extends BaseAdapter {

    Context context;
    Documents data;
    private static LayoutInflater inflater = null;

    public NuxeoItemAdapter(Context context, Documents data) {
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.getTotalSize();
    }

    @Override
    public Object getItem(int position) {
        return data.getDocuments().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.activity_nuxeo_listing_item, null);
        TextView text = (TextView) vi.findViewById(R.id.docTitle);
        text.setText("    " + ((Document) getItem(position)).getTitle());
        return vi;
    }
}