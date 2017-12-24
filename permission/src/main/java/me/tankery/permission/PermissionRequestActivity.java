package me.tankery.permission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * PermissionRequestActivity
 * An transparent theme activity for permissions request using a standard policy.
 *
 * Use both for Wear App and Mobile App.
 *
 * (Install "PlantUML integration" Plugin to see diagram.)
 * @startuml
 *
 * title Permission Request Policy:
 *
 * start
 * :onStart;
 * while (Has permission?) is (no)
 *     if (request permission from system) then (granted)
 *         stop
 *     else (denied)
 *         if (show rationale?) then (no)
 *             if (<b>go settings dialog) then (cancel)
 *                 end
 *             else (ok)
 *                 :go Settings;
 *                 stop
 *             endif
 *         else (yes)
 *             if (<b>rationale dialog) then (cancel)
 *                 end
 *             endif
 *         endif
 *     endif
 * endwhile (yes)
 * :Do anything you want;
 * stop
 *
 * @enduml
 *
 * Created by tankery on 07/09/2017.
 */
public class PermissionRequestActivity extends Activity {

    private static final String TAG = "PermissionRequestAct";

    public static final String EXTRAS_KEY_PERMISSIONS = "permissions";
    public static final String EXTRAS_KEY_RATIONALE_MSG = "rationale_msg";
    public static final String EXTRAS_KEY_GO_SETTINGS_MSG = "go_settings_msg";

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private String[] mPermissions;
    private String mRationaleMsg;
    private String mGoSettingsMsg;

    private boolean mFromPermissionRequest = false;

    /**
     * Start permission request with result.
     */
    public static void start(@NonNull Activity activity, int requestCode,
                             @NonNull String[] permissions,
                             @NonNull String rationaleMsg, @NonNull String goSettingsMsg) {
        Intent intent = fillIntent(new Intent(activity, PermissionRequestActivity.class),
                permissions, rationaleMsg, goSettingsMsg);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Start permission request, don't care about result.
     */
    public static void start(@NonNull Context context,
                             @NonNull String[] permissions,
                             @NonNull String rationaleMsg, @NonNull String goSettingsMsg) {
        Intent intent = fillIntent(new Intent(context, PermissionRequestActivity.class),
                permissions, rationaleMsg, goSettingsMsg);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @NonNull
    protected static Intent fillIntent(Intent intent, @NonNull String[] permissions,
                                     @NonNull String rationaleMsg, @NonNull String goSettingsMsg) {
        intent.putExtra(EXTRAS_KEY_PERMISSIONS, permissions);
        intent.putExtra(EXTRAS_KEY_RATIONALE_MSG, rationaleMsg);
        intent.putExtra(EXTRAS_KEY_GO_SETTINGS_MSG, goSettingsMsg);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.tag(TAG).d("onCreate %s", savedInstanceState);
        if (savedInstanceState != null) {
            // Don't accept restored activity (may restore after been force stopped in permission settings).
            finish();
            return;
        }
        if (getIntent() != null) {
            mPermissions = getIntent().getStringArrayExtra(EXTRAS_KEY_PERMISSIONS);
            mRationaleMsg = getIntent().getStringExtra(EXTRAS_KEY_RATIONALE_MSG);
            mGoSettingsMsg = getIntent().getStringExtra(EXTRAS_KEY_GO_SETTINGS_MSG);
        }
        mFromPermissionRequest = false;

        if (mPermissions == null ||
                TextUtils.isEmpty(mRationaleMsg) || TextUtils.isEmpty(mGoSettingsMsg)) {
            throw new IllegalArgumentException("Permissions and messages can NOT be empty.");
        }

        Timber.tag(TAG).d("Start permissions %s, with message %s | %s", Arrays.toString(mPermissions),
                mRationaleMsg, mGoSettingsMsg);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.tag(TAG).d("onStart");
        if (mFromPermissionRequest) {
            // We are back from system permission request Activity
            return;
        }

        if (mPermissions.length == 0) {
            okAndFinish();
            return;
        }

        mFromPermissionRequest = false;
        requestPermissionIfNeed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.tag(TAG).d("onStop");
        mFromPermissionRequest = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Timber.tag(TAG).d("onRequestPermissionsResult for %d", requestCode);
        mFromPermissionRequest = true;

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            checkResult(permissions, grantResults, true);
        }
    }

    private void requestPermissionIfNeed() {
        Timber.tag(TAG).i("Start permission request");
        int[] grantResults = new int[mPermissions.length];
        for (int i = 0; i < mPermissions.length; i++) {
            grantResults[i] = ActivityCompat.checkSelfPermission(this, mPermissions[i]);
        }
        checkResult(mPermissions, grantResults, false);
    }

    private void checkResult(@NonNull String[] permissions, @NonNull int[] grantResults,
                             boolean fromPermissionResult) {
        boolean allGranted = true;
        boolean shouldShowRationale = false;
        List<String> unGranted = new ArrayList<>(permissions.length);
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int result = grantResults[i];
            if (result == PackageManager.PERMISSION_DENIED) {
                allGranted = false;
                unGranted.add(permission);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    shouldShowRationale = true;
                }
            }
        }
        final String[] unGrantedPermissions = new String[unGranted.size()];
        unGranted.toArray(unGrantedPermissions);

        if (allGranted) {
            okAndFinish();
        } else {
            Timber.tag(TAG).d("Has un-granted permissions: %s, %s",
                    Arrays.toString(unGrantedPermissions),
                    fromPermissionResult?
                            (shouldShowRationale ? "rationale" : "go settings") :
                            "request permission");

            if (fromPermissionResult) {
                showPermissionDialog(shouldShowRationale);
            } else {
                ActivityCompat.requestPermissions(this,
                        unGrantedPermissions, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    private void showPermissionDialog(final boolean canRequestAgain) {
        showRationaleDialog(canRequestAgain,
                canRequestAgain ? mRationaleMsg : mGoSettingsMsg,
                new DialogResult() {
                    @Override
                    public void onPositive() {
                        if (canRequestAgain) {
                            Timber.tag(TAG).i("Restart request");
                            requestPermissionIfNeed();
                        } else {
                            Timber.tag(TAG).i("Go Settings");
                            gotoSettings();
                        }
                    }

                    @Override
                    public void onNegative() {
                        Timber.tag(TAG).i("Cancel request");
                        cancelAndFinish();
                    }
                });
    }

    /**
     * Override this method to show custom dialog.
     * @param canRequestAgain if true, show request again dialog, else, show go settings dialog
     * @param message dialog message
     * @param dialogResult always have a result for user action
     *                     (ok - > positive/cancel -> negative/dismiss -> negative)
     */
    protected void showRationaleDialog(final boolean canRequestAgain, String message,
                                       final @NonNull DialogResult dialogResult) {
        showStandardPermissionDialog(message, dialogResult);
    }

    private void showStandardPermissionDialog(String message,
                                              @NonNull final DialogResult dialogResult) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        dialogResult.onPositive();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        dialogInterface.dismiss();
                        dialogResult.onNegative();
                    }
                })
                .show();

        alertDialog.setCanceledOnTouchOutside(true);
    }

    private void gotoSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Timber.tag(TAG).e("Error to go Settings.");
            cancelAndFinish();
        }
    }

    private void okAndFinish() {
        Timber.tag(TAG).i("All granted, do anything you want~");
        setResult(RESULT_OK);
        finish();
    }

    private void cancelAndFinish() {
        Timber.tag(TAG).i("Some permissions are denied~");
        setResult(RESULT_CANCELED);
        finish();
    }

    protected interface DialogResult {
        void onPositive();
        void onNegative();
    }
}
