package net.justdave.mcstatus;

import java.util.ArrayList;
import java.util.ListIterator;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity implements OnItemClickListener {
	private static final String TAG = MainActivity.class.getSimpleName();

	private ArrayList<MinecraftServer> serverlist = new ArrayList<MinecraftServer>();
	private ServerListViewAdapter serverlist_adapter;
	private ServerDB database;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		database = new ServerDB(this);
		database.open();

		database.getAllServers(serverlist);
		serverlist_adapter = new ServerListViewAdapter(getApplicationContext(),
				serverlist);
		setContentView(R.layout.activity_main);
		ListView listView = (ListView) findViewById(R.id.server_list);
		listView.setAdapter(serverlist_adapter);
		// listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		listView.setOnItemClickListener(this);
		listView.setItemsCanFocus(false);
		listView.setDescendantFocusability(ListView.FOCUS_BLOCK_DESCENDANTS);
		listView.setEmptyView(findViewById(R.id.empty));
		listView.addStatesFromChildren();
		refresh();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Log.i(TAG, "onItemClick() called");
		parent.getItemAtPosition(position);
	}

	public void refresh() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				ListIterator<MinecraftServer> iterator = serverlist
						.listIterator();
				while (iterator.hasNext()) {
					iterator.next().query();
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						serverlist_adapter.notifyDataSetChanged();
					}
				});
			}
		});
		thread.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// just hide the delete button for now, eventually we need to show it
		// when there's a selection
		ListView server_list = (ListView) findViewById(R.id.server_list);
		MenuItem deleteServer = menu.findItem(R.id.action_deleteserver);
		deleteServer.setVisible(server_list.getCheckedItemCount() > 0);
		return true;
	}

	@SuppressLint("InflateParams")
	@Override
	public boolean onOptionsItemSelected(MenuItem menuitem) {
		switch (menuitem.getItemId()) {
		case R.id.action_addserver:
			View promptsView = LayoutInflater.from(this).inflate(
					R.layout.addserver_dialog, null);
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
					this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
			alertDialogBuilder.setView(promptsView);

			final EditText serverName = (EditText) promptsView
					.findViewById(R.id.edit_server_name);
			final EditText serverAddress = (EditText) promptsView
					.findViewById(R.id.edit_server_address);

			alertDialogBuilder
					.setCancelable(false)
					.setTitle("Add a Server")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// get user input
									database.create(serverName.getText()
											.toString(), serverAddress
											.getText().toString());
									database.getAllServers(serverlist);
									refresh();
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			return true;
		case R.id.action_refresh:
			refresh();
			return true;
		}
		return false;
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume() called");
		database.open();
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause() called");
		database.close();
		super.onPause();
	}
}
