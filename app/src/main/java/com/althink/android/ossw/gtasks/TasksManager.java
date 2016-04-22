package com.althink.android.ossw.gtasks;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.althink.android.ossw.OsswApp;
import com.althink.android.ossw.notifications.message.DialogSelectMessageBuilder;
import com.althink.android.ossw.notifications.message.NotificationMessageBuilder;
import com.althink.android.ossw.notifications.model.NotificationType;
import com.althink.android.ossw.service.OsswService;
import com.althink.android.ossw.utils.FunctionHandler;
import com.althink.android.ossw.watch.WatchConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class TasksManager implements EasyPermissions.PermissionCallbacks {
    private final static String TAG = TasksManager.class.getSimpleName();
    private static TasksManager instance;
    private static int level = 0;
    private static String account = "";
    private static String taskListId = "";
    GoogleAccountCredential mCredential;

    static final long LIST_MAX_RESULTS = 20;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    static final int BUTTON_UP = 1;
    static final int BUTTON_DOWN = 2;
    static final int BUTTON_SELECT = 4;
    static final int BUTTON_BACK = 8;
    static final int BUTTON_HOLD = 16;

    private static final String[] SCOPES = {TasksScopes.TASKS};

    List<Account> accountList;
    List<String> lastIds = new ArrayList<>();

    private TasksManager() {
        Context context = OsswApp.getContext().getApplicationContext();
        if (EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS)) {
            accountList = Arrays.asList(AccountManager.get(context).getAccountsByType("com.google"));
            mCredential = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    public static TasksManager getInstance() {
        if (instance == null)
            instance = new TasksManager();
        return instance;
    }

    public void handle(int buttons, int item) {
        Log.i(TAG, "Handling gtasks event with parameters: " + buttons + ", " + item);
        if (accountList == null || accountList.size() < 1 || !isGooglePlayServicesAvailable() || !isDeviceOnline())
            return;
        if (buttons == 0) {
            if (level == 0)
                showAccounts();
            else if (level == 1)
                showTaskLists();
            else if (level == 2)
                showTasks();
        } else {
            if (buttons == BUTTON_BACK) {
                if (level == 0) {
                    FunctionHandler.closeDialog();
                } else if (level == 1) {
                    level--;
                    showAccounts();
                } else if (level == 2) {
                    level--;
                    showTaskLists();
                }
            } else if (buttons == (BUTTON_BACK | BUTTON_HOLD)) {
//                FunctionHandler.closeDialog();
            } else if (buttons == BUTTON_SELECT) {
                if (level == 0) {
                    account = accountList.get(item).name;
                    level++;
                    showTaskLists();
                } else if (level == 1) {
                    taskListId = lastIds.get(item);
                    level++;
                    showTasks();
                } else if (level == 2) {
                    // TODO: implement toggling the task completion
                }
            } else if (buttons == (BUTTON_SELECT | BUTTON_HOLD)) {
                // TODO: add some functionality
            }
        }
    }

    public void showAccounts() {
        List<String> items = new ArrayList<>();
        for (Account account : accountList)
            items.add(account.name);
        Log.d(TAG, "Choose an account: " + items.toString());
        NotificationMessageBuilder builder = new DialogSelectMessageBuilder("Accounts", items, 0, WatchConstants.PHONE_FUNCTION_GTASKS, 0);
        OsswService.getInstance().uploadNotification(0, NotificationType.DIALOG_SELECT, builder.build(), 0, 0, null);
    }

    public void showTaskLists() {
        mCredential.setSelectedAccountName(account);
        new MakeRequestTask<List<TaskList>>(mCredential) {
            @Override
            protected List<TaskList> getDataFromApi(Tasks mService) throws IOException {
                TaskLists result = mService.tasklists().list().setFields("items(id,title)")
                        .setMaxResults(LIST_MAX_RESULTS)
                        .execute();
                return result.getItems();
            }

            protected void onPostExecute(List<TaskList> result) {
                lastIds.clear();
                if (result == null || result.size() == 0)
                    return;
                List<String> items = new ArrayList<>();
                for (TaskList list : result) {
                    items.add(list.getTitle());
                    lastIds.add(list.getId());
                }
                Log.d(TAG, "Choose a tasks list: " + items.toString());
                NotificationMessageBuilder builder = new DialogSelectMessageBuilder("Tasks lists", items, 0, WatchConstants.PHONE_FUNCTION_GTASKS, 0);
                OsswService.getInstance().uploadNotification(0, NotificationType.DIALOG_SELECT, builder.build(), 0, 0, null);
            }
        }.execute();
    }

    public void showTasks() {
        mCredential.setSelectedAccountName(account);
        new MakeRequestTask<List<Task>>(mCredential) {
            @Override
            protected List<Task> getDataFromApi(Tasks mService) throws IOException {
                com.google.api.services.tasks.model.Tasks result = mService.tasks().list(taskListId).setFields("items(id,title,status)")
                        .setMaxResults(LIST_MAX_RESULTS)
                        .execute();
                return result.getItems();
            }

            protected void onPostExecute(List<Task> result) {
                lastIds.clear();
                int itemsSize = result.size();
                if (result == null || itemsSize == 0)
                    return;
                List<String> items = new ArrayList<>();
                int bitSetLength = itemsSize >> 3;
                if ((itemsSize & 7) > 0)
                    bitSetLength++;
                byte[] bitset = new byte[bitSetLength];
                int bitCount = 0;
                for (Task task : result) {
                    items.add(task.getTitle());
                    lastIds.add(task.getId());
                    if (task.getStatus().equals("completed"))
                        bitset[bitCount>>3] |= 1 << (bitCount & 7);
                    bitCount++;
                }
                Log.d(TAG, "Choose a task: " + items.toString());
                NotificationMessageBuilder builder = new DialogSelectMessageBuilder("Tasks", items, 0,
                        WatchConstants.PHONE_FUNCTION_GTASKS, WatchConstants.STYLE_CHECK_BOX | WatchConstants.STYLE_STRIKE, bitset);
                OsswService.getInstance().uploadNotification(0, NotificationType.DIALOG_SELECT, builder.build(), 0, 0, null);
            }
        }.execute();
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) OsswApp.getContext().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(OsswApp.getContext().getApplicationContext());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }


    /**
     * An asynchronous task that handles the Google Tasks API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    abstract private class MakeRequestTask<Result> extends AsyncTask<Void, Void, Result> {
        private com.google.api.services.tasks.Tasks mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.tasks.Tasks.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Tasks API")
                    .build();
        }

        /**
         * Background task to call Google Tasks API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Result doInBackground(Void... params) {
            try {
                return getDataFromApi(mService);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        abstract protected Result getDataFromApi(com.google.api.services.tasks.Tasks mService) throws IOException;

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                Log.e(TAG, mLastError.toString());
            } else {
                Log.e(TAG, "Request cancelled.");
            }
        }
    }
}