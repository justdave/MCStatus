package net.justdave.mcstatus;

import java.util.ArrayList;
import java.util.ListIterator;

import android.R.color;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ServerListViewAdapter extends ArrayAdapter<MinecraftServer> implements View.OnClickListener {
	private static final String TAG = ServerListViewAdapter.class
			.getSimpleName();

	private final Context context;
	private final ArrayList<MinecraftServer> values;

	public ServerListViewAdapter(Context context,
			ArrayList<MinecraftServer> serverList) {
		super(context, R.layout.server_listitem, serverList);
		Log.i(TAG, "ServerListViewAdapter constructor called");
		this.context = context;
		this.values = serverList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Log.i(TAG, "getView(".concat(Integer.toString(position)).concat(")"));
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = convertView;
		if (rowView == null) {
			rowView = inflater.inflate(R.layout.server_listitem, parent, false);
		}
		if (values.get(position).isSelected()) {
			Log.i(TAG, "View is selected.");
			rowView.setBackgroundColor(color.holo_blue_dark);
		} else {
			Log.i(TAG, "View is not selected.");
		}
		TextView server_name = (TextView) rowView
				.findViewById(R.id.server_name);
		Log.i(TAG, "Setting servername to ".concat(values.get(position)
				.serverName()));
		server_name.setText(values.get(position).serverName());
		WebView server_description = (WebView) rowView
				.findViewById(R.id.server_description);
		Log.i(TAG, "Setting description to ".concat(values.get(position)
				.description()));
		server_description.loadData(values.get(position).description(),
				"text/html", "utf8");
		server_description.setTag(String.valueOf(position));
		server_description.setOnClickListener(this);
		server_description.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return true;
			}
		});
		ImageView server_icon = (ImageView) rowView
				.findViewById(R.id.server_icon);
		server_icon.setImageBitmap(values.get(position).image());
		TextView server_usercount = (TextView) rowView
				.findViewById(R.id.server_usercount);
		server_usercount.setText(Integer
				.toString(values.get(position).onlinePlayers()).concat("/")
				.concat(Integer.toString(values.get(position).maxPlayers())));
		TextView server_playerlist = (TextView) rowView
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
		TextView server_userlist_header = (TextView) rowView
				.findViewById(R.id.server_userlist_header);
		if (playerlist.length() > 0) {
			server_userlist_header.setVisibility(View.VISIBLE);
			server_playerlist.setVisibility(View.VISIBLE);
		} else {
			server_userlist_header.setVisibility(View.GONE);
			server_playerlist.setVisibility(View.GONE);
		}
		TextView server_serverversion = (TextView) rowView
				.findViewById(R.id.server_serverversion);
		server_serverversion.setText(values.get(position).serverVersion());
		rowView.setClickable(true);
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
		Log.i(TAG, "onClick() got called.");
		values.get(Integer.valueOf((String) v.getTag())).setSelected(true);
		notifyDataSetChanged();
	}

}