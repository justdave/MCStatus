package net.justdave.mcstatus;

import android.R.color;
import android.content.Context;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Locale;

public class ServerListViewAdapter extends ArrayAdapter<MinecraftServer>
        implements View.OnLongClickListener, View.OnClickListener {
    private static final String TAG = ServerListViewAdapter.class
            .getSimpleName();

    private final Context context;
    private final ArrayList<MinecraftServer> values;
    private ListView listView;

    public ServerListViewAdapter(Context context,
                                 ArrayList<MinecraftServer> serverList) {
        super(context, R.layout.server_listitem, serverList);
        Log.i(TAG, "ServerListViewAdapter constructor called");
        this.context = context;
        this.values = serverList;
    }

    public void setListView(ListView mView) {
        listView = mView;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
//		Log.i(TAG, "getView(".concat(Integer.toString(position)).concat(")"));
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView;
        if (rowView == null) {
            assert inflater != null;
            rowView = inflater.inflate(R.layout.server_listitem, parent, false);
        }
        SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
        if (checkedItems.get(position)) {
//			Log.i(TAG, "View is selected.");
            rowView.setBackgroundColor(Color.parseColor("#AAAAAA"));
        } else {
//			Log.i(TAG, "View is not selected.");
            rowView.setBackgroundColor(ContextCompat.getColor(context, color.background_dark));
        }
        TextView server_name = rowView
                .findViewById(R.id.server_name);
        server_name.setText(values.get(position).serverName());
        WebView server_description = rowView
                .findViewById(R.id.server_description);
        server_description.loadData(Base64.encodeToString(values.get(position).description().getBytes(), Base64.DEFAULT),
                "text/html", "base64");
        server_description.setBackgroundColor(0x00000000);
        server_description.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        server_description.setFocusable(false);
        server_description.setFocusableInTouchMode(false);
        server_description.setClickable(false);
        server_description.setLongClickable(false);
        server_description.setTag(String.valueOf(position));
        server_description.setOnLongClickListener(this);
        server_description.setOnClickListener(this);

        ImageView server_icon = rowView
                .findViewById(R.id.server_icon);
        server_icon.setImageBitmap(values.get(position).image());
        TextView server_usercount = rowView
                .findViewById(R.id.server_usercount);
        server_usercount.setText(String.format(Locale.getDefault(), "%d", values.get(position).onlinePlayers()).concat("/")
                .concat(Integer.toString(values.get(position).maxPlayers())));
        TextView server_playerlist = rowView
                .findViewById(R.id.server_playerlist);
        StringBuilder playerlist = new StringBuilder();
        ListIterator<String> playerIterator = values.get(position).playerList()
                .listIterator();
        while (playerIterator.hasNext()) {
            playerlist.append(playerIterator.next());
            if (playerIterator.hasNext()) {
                playerlist.append("\n");
            }
        }
        server_playerlist.setText(playerlist.toString());
        TextView server_userlist_header = rowView
                .findViewById(R.id.server_userlist_header);
        if (playerlist.length() > 0) {
            server_userlist_header.setVisibility(View.VISIBLE);
            server_playerlist.setVisibility(View.VISIBLE);
        } else {
            server_userlist_header.setVisibility(View.GONE);
            server_playerlist.setVisibility(View.GONE);
        }
        TextView server_serverversion = rowView
                .findViewById(R.id.server_serverversion);
        server_serverversion.setText(values.get(position).serverVersion());
        rowView.setClickable(false);
        rowView.setOnLongClickListener(this);
        rowView.setOnClickListener(this);
        rowView.setTag(String.valueOf(position));
        return rowView;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public void onClick(View v) {
//		Log.i(TAG, "onClick() got called.");
        if (listView.getCheckedItemCount() > 0) {
            SparseBooleanArray checkedItems = listView
                    .getCheckedItemPositions();
            boolean checked = checkedItems.get(Integer.parseInt((String) v
                    .getTag()));
            listView.setItemChecked(Integer.parseInt((String) v.getTag()),
                    !checked);
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean onLongClick(View v) {
//		Log.i(TAG, "onLongClick() got called.");
        if (listView.getCheckedItemCount() == 0) {
            listView.setItemChecked(Integer.parseInt((String) v.getTag()), true);
            notifyDataSetChanged();
        }
        return true;
    }
}