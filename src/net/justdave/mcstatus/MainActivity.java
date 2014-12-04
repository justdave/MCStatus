package net.justdave.mcstatus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private ArrayList<MinecraftServer> serverlist = new ArrayList<MinecraftServer>();
	private ServerListViewAdapter adapter;
	private ListView listView;
	private ServerDB database;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		database = new ServerDB(this);
		database.open();

		database.getAllServers(serverlist);
		adapter = new ServerListViewAdapter(getApplicationContext(), serverlist);
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.server_list);
		listView.setAdapter(adapter);
		adapter.setListView(listView);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setItemsCanFocus(false);
		listView.setDescendantFocusability(ListView.FOCUS_BLOCK_DESCENDANTS);
		listView.setEmptyView(findViewById(R.id.empty));
		listView.addStatesFromChildren();
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.selectionbar, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.action_editserver:
					Log.i(TAG, "EDIT selection:");
					break;
				case R.id.action_deleteserver:
					AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(
							MainActivity.this,
							AlertDialog.THEME_DEVICE_DEFAULT_DARK);
					myAlertDialog
							.setCancelable(false)
							.setTitle("Delete Server(s)")
							.setMessage(
									"You are about to delete the selected server(s).")
							.setPositiveButton("Cancel",
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface arg0, int arg1) {
											// do something when the Cancel
											// button
											// is clicked
											Log.i(TAG, "Cancel picked");
										}
									})
							.setNegativeButton("Delete",
									new DialogInterface.OnClickListener() {

										public void onClick(
												DialogInterface arg0, int arg1) {
											// do something when the Delete
											// button is clicked
											Log.i(TAG, "Delete picked");
											SparseBooleanArray selected = listView
													.getCheckedItemPositions();
											Iterator<MinecraftServer> it = serverlist
													.iterator();
											while (it.hasNext()) {
												MinecraftServer thisServer = it
														.next();
												if (selected.get(serverlist
														.indexOf(thisServer))) {
													database.delete(thisServer
															.serverAddress());
													listView.setItemChecked(serverlist.indexOf(thisServer), false);
												}
											}
											database.getAllServers(serverlist);
											refresh();
											adapter.notifyDataSetInvalidated();
										}
									}).create().show();
					break;
				}
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				// listView.setItemChecked(position, checked);
				mode.getMenu().findItem(R.id.action_editserver)
						.setVisible(listView.getCheckedItemCount() == 1);
				mode.setTitle(Integer.valueOf(listView.getCheckedItemCount())
						+ " selected");
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				listView.clearChoices();
			}
		});
		refresh();

	}

	public void refresh() {
		ListIterator<MinecraftServer> iterator = serverlist
				.listIterator();
		while (iterator.hasNext()) {
			iterator.next().setDescription("Loading...           ");
		}
		adapter.notifyDataSetChanged();

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
						adapter.notifyDataSetChanged();
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
