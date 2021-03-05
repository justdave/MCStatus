package net.justdave.mcstatus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.justdave.mcstatus.db.ServerDB;
import net.justdave.mcstatus.dialogs.AboutDialog;
import net.justdave.mcstatus.dialogs.HelpDialog;
import net.justdave.mcstatus.dialogs.PrivacyDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.ListIterator;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final ArrayList<MinecraftServer> serverlist = new ArrayList<>();
    private final Object refreshLock = new Object();
    private final ImageGetter imgGetter = source -> {
        Drawable drawable;
        Log.i(TAG, "Drawable source: " + source);
        int rid = getResources().getIdentifier(source, null, null);
        if (rid > 0) {
            drawable = getResources().getDrawable(rid);
        } else {
            drawable = getResources().getDrawable(android.R.drawable.stat_notify_error);
        }
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        return drawable;
    };
    private ServerListViewAdapter adapter;
    private ListView listView;
    private ServerDB database;
    private MenuItem refreshItem;
    private int currentlyRefreshing = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new ServerDB(this);
        database.open();

        database.getAllServers(serverlist);
        adapter = new ServerListViewAdapter(getApplicationContext(), serverlist);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.server_list);

        listView.setAdapter(adapter);
        adapter.setListView(listView);

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setItemsCanFocus(false);
        listView.setDescendantFocusability(ListView.FOCUS_BLOCK_DESCENDANTS);
        listView.setEmptyView(findViewById(R.id.empty));
        View emptyView = listView.getEmptyView();
        TextView helpView = emptyView.findViewById(R.id.helptext);
        Log.i(TAG, "helpView: " + helpView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            /* Nugat and up can actually process images! */
            helpView.setText(Html.fromHtml(readRawTextFile(R.raw.main_help), 0, imgGetter, null));
        } else {
            /* below Nugat requires one hell of a hack of a workaround. */
            String text = readRawTextFile(R.raw.main_help);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            int index = text.indexOf("<img") -1;
            int index2 = text.indexOf("\">") + 2;
            SpannableString string1 = new SpannableString(Html.fromHtml(text.substring(0,index)));
            ImageSpan is = new ImageSpan(getApplicationContext(), android.R.drawable.ic_menu_add);
            SpannableString string2 = new SpannableString(Html.fromHtml(text.substring(index2)));
            ssb.append(string1).append(" ");
            ssb.setSpan(is, ssb.length()-1, ssb.length(), 0);
            ssb.append(string2);
            helpView.setText(ssb);
        }
        helpView.setBackgroundColor(0x00000000);
//        listView.setEmptyView(emptyView);

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
                int itemId = item.getItemId();
                if (itemId == R.id.action_editserver) {
                    Log.i(TAG, "EDIT selection:");
                    @SuppressLint("InflateParams") View promptsView = LayoutInflater.from(MainActivity.this).inflate(
                            R.layout.addserver_dialog, null);
                    AlertDialog.Builder alertDialogBuilder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        alertDialogBuilder = new AlertDialog.Builder(
                                MainActivity.this,
                                android.R.style.Theme_DeviceDefault_Dialog_Alert);
                    } else {
                        alertDialogBuilder = new AlertDialog.Builder(
                                MainActivity.this,
                                AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                    }
                    alertDialogBuilder.setView(promptsView);

                    final EditText serverName = promptsView
                            .findViewById(R.id.edit_server_name);
                    final EditText serverAddress = promptsView
                            .findViewById(R.id.edit_server_address);
                    SparseBooleanArray selected = listView
                            .getCheckedItemPositions();
                    for (final MinecraftServer thisServer : serverlist) {
                        if (selected.get(serverlist
                                .indexOf(thisServer))) {
                            serverName.setText(thisServer.serverName());
                            serverAddress.setText(thisServer.serverAddress());
                            alertDialogBuilder
                                    .setCancelable(false)
                                    .setTitle(getResources().getString(R.string.action_editserver))
                                    .setPositiveButton(getResources().getString(R.string.ok),
                                            (dialog, id) -> {
                                                // get user input
                                                database.update(thisServer.serverAddress(),
                                                        serverAddress.getText().toString(),
                                                        serverName.getText().toString());
                                                database.getAllServers(serverlist);
                                                refresh();
                                            })
                                    .setNegativeButton(getResources().getString(R.string.cancel),
                                            (dialog, id) -> dialog.cancel());
                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                            listView.setItemChecked(
                                    serverlist
                                            .indexOf(thisServer),
                                    false);
                        }
                    }
                } else if (itemId == R.id.action_deleteserver) {
                    AlertDialog.Builder myAlertDialog;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        myAlertDialog = new AlertDialog.Builder(
                                MainActivity.this,
                                android.R.style.Theme_DeviceDefault_Dialog_Alert);
                    } else {
                        myAlertDialog = new AlertDialog.Builder(
                                MainActivity.this,
                                AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                    }
                    myAlertDialog
                            .setCancelable(false)
                            .setTitle(getResources().getString(R.string.action_deleteserver))
                            .setMessage(
                                    getResources().getString(R.string.delete_info))
                            .setPositiveButton(getResources().getString(R.string.cancel),
                                    (arg0, arg1) -> {
                                        // do something when the Cancel
                                        // button
                                        // is clicked
                                        Log.i(TAG, "Cancel picked");
                                    })
                            .setNegativeButton(getResources().getString(R.string.delete),
                                    (arg0, arg1) -> {
                                        // do something when the Delete
                                        // button is clicked
                                        Log.i(TAG, "Delete picked");
                                        SparseBooleanArray selected1 = listView
                                                .getCheckedItemPositions();
                                        for (MinecraftServer thisServer : serverlist) {
                                            if (selected1.get(serverlist
                                                    .indexOf(thisServer))) {
                                                database.delete(thisServer
                                                        .serverAddress());
                                                listView.setItemChecked(
                                                        serverlist
                                                                .indexOf(thisServer),
                                                        false);
                                            }
                                        }
                                        database.getAllServers(serverlist);
                                        refresh();
                                        adapter.notifyDataSetInvalidated();
                                    }).create().show();
                }
                return false;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id, boolean checked) {
                // listView.setItemChecked(position, checked);
                mode.getMenu().findItem(R.id.action_editserver)
                        .setVisible(listView.getCheckedItemCount() == 1);
                mode.setTitle(listView.getCheckedItemCount()
                        + " selected");
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                listView.clearChoices();
            }
        });
    }

    public void refresh() {
        ImageView iv = (ImageView) View.inflate(getApplicationContext(), R.layout.refresh_animation,
                null);

        Animation rotation = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.refresh_rotate);
        rotation.setRepeatCount(Animation.INFINITE);

        ListIterator<MinecraftServer> iterator = serverlist.listIterator();
        if (iterator.hasNext()) {
            if (refreshItem != null) {
                refreshItem.setActionView(iv);
            }
            iv.startAnimation(rotation);
        }
        while (iterator.hasNext()) {
            iterator.next().setDescription("Loading...           ");
        }
        adapter.notifyDataSetChanged();
        iterator = serverlist.listIterator();
        while (iterator.hasNext()) {
            final MinecraftServer mcs = iterator.next();
            Thread thread = new Thread(() -> {
                synchronized (refreshLock) {
                    currentlyRefreshing++;
                }
                mcs.query();
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    synchronized (refreshLock) {
                        currentlyRefreshing--;
                    }
                    if (currentlyRefreshing == 0) {
                        if (refreshItem != null
                                && refreshItem.getActionView() != null) {
                            refreshItem.getActionView()
                                    .clearAnimation();
                            refreshItem.setActionView(null);
                        }
                    }
                });
            });
            thread.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        refreshItem = menu.findItem(R.id.action_refresh);
        refresh();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onOptionsItemSelected(MenuItem menuitem) {
        int itemId = menuitem.getItemId();
        if (itemId == R.id.action_about) {
            AboutDialog about = new AboutDialog(this);
            about.setTitle(getResources().getString(R.string.action_about));
            about.show();
        } else if (itemId == R.id.action_help) {
            HelpDialog help = new HelpDialog(this);
            help.setTitle(getResources().getString(R.string.action_help));
            help.show();
        } else if (itemId == R.id.action_privacy) {
            PrivacyDialog privacy = new PrivacyDialog(this);
            privacy.setTitle(getResources().getString(R.string.action_privacy));
            privacy.show();
        } else if (itemId == R.id.action_addserver) {
            View promptsView = LayoutInflater.from(this).inflate(
                    R.layout.addserver_dialog, null);
            AlertDialog.Builder alertDialogBuilder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                alertDialogBuilder = new AlertDialog.Builder(
                        this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
            } else {
                alertDialogBuilder = new AlertDialog.Builder(
                        this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            }
            alertDialogBuilder.setView(promptsView);

            final EditText serverName = promptsView
                    .findViewById(R.id.edit_server_name);
            final EditText serverAddress = promptsView
                    .findViewById(R.id.edit_server_address);

            alertDialogBuilder
                    .setCancelable(false)
                    .setTitle(getResources().getString(R.string.action_addserver))
                    .setPositiveButton(getResources().getString(R.string.ok),
                            (dialog, id) -> {
                                // get user input
                                database.create(serverName.getText()
                                        .toString(), serverAddress
                                        .getText().toString());
                                database.getAllServers(serverlist);
                                refresh();
                            })
                    .setNegativeButton(getResources().getString(R.string.cancel),
                            (dialog, id) -> dialog.cancel());
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return true;
        } else if (itemId == R.id.action_refresh) {
            refresh();
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
//        Log.i(TAG, "onResume() called");
        database.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
//        Log.i(TAG, "onPause() called");
        database.close();
        super.onPause();
    }

    public String readRawTextFile(int id) {

        InputStream inputStream = getApplicationContext().getResources().openRawResource(id);

        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader buf = new BufferedReader(in);

        String line;

        StringBuilder text = new StringBuilder();
        try {
            while ((line = buf.readLine()) != null)
                text.append(line);
        } catch (IOException e) {
            return null;
        }

        return text.toString();
    }
}
