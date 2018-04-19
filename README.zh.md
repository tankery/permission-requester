# Permission Requester

[EN](README.md)

Permission Reuquester 是一个可以让你用最少工作量，完成一整套权限请求的 Activity。

## 背景

[在运行时请求权限](https://developer.android.com/training/permissions/requesting.html) 是 Android 6.0 (API level 23) 开始引入的功能，用户可以在运行应用，而不是安装时来请求必要的权限。目标版本是 23 及以上的应用，都必须在运行时请求“危险”权限。

有很多事情需要我们考虑。

首先你需要[检查权限](https://developer.android.com/training/permissions/requesting.html#perm-check)来判断应用是否已经获取到所需权限。如果没有权限，那么你得[请求您需要的权限](https://developer.android.com/training/permissions/requesting.html#make-the-request)，而且通常你需要[解释应用为什么需要权限](https://developer.android.com/training/permissions/requesting.html#explain)。而请求权限以后，你可能会需要[处理权限请求响应](https://developer.android.com/training/permissions/requesting.html#handle-response)，这样你可以在根据权限请求结果来做不同的事情。更麻烦的是，如果用户点选了“不再提醒”，那么你的应用可能就失去获取权限的机会了，你只能选择引导用户到设置中去开启所需权限。

## 工作流

下图展示了 Permission Reuquester 是如何完成一次权限请求的：

![](art/permission-request-policy.png)

当你开始通过 `PermissionRequestActivity` 来请求权限时。它首先会检测是否已经有权限。有则返回，没有则会开始调用系统方法来直接请求权限。若用户拒绝授权。它会弹出你指定的解释说明，告诉用户你为何需要这些权限，用户确认后将会重新请求。若用户点选了“不再提醒”，它会弹出一个引导，指引用户到设置中去开启权限。

## 下载

**Gradle**

``` gradle
dependencies {
    compile 'me.tankery.lib:permission-requester:1.1.0'
}
```

**Gradle 3.0**

``` gradle
dependencies {
    implementation 'me.tankery.lib:permission-requester:1.1.0'
}
```

## 用法

要实现一个权限请求非常的容易。如果你不需要立刻获得权限请求结果，而仅仅是申请权限的话。用下面这一行代码就够了：

``` java
PermissionRequestActivity.start(context, PERMISSIONS, rationalMsg, goSettingsMsg);
```

你甚至可以在后台 Service 中开启权限请求。

而如果你关心请求结果。那么你可以在你的 Activity 中调用另外一行代码：

``` java
PermissionRequestActivity.start(activity, REQUEST_CODE, PERMISSIONS, rationalMsg, goSettingsMsg);
```

然后，重载 Activity 的 `onActivityResult` 来获取请求结果：

``` java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE) {
        if (resultCode == RESULT_OK) {
            // 授权成功
        } else {
            // 授权失败
        }
    }
}
```

你甚至可以通过重载 `showRationaleDialog` 来自定义解释说明对话框的展现形式：

``` java
/**
 * Override this method to show custom dialog.
 * @param canRequestAgain if true, show request again dialog, else, show go settings dialog
 * @param message dialog message
 * @param dialogResult always have a result for user action
 *                     (ok - > positive/cancel -> negative/dismiss -> negative)
 * @return The custom dialog shown.
 */
@Override
protected Dialog showRationaleDialog(final boolean canRequestAgain, String message,
                                     final @NonNull DialogResult dialogResult) {
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
    return alertDialog;
}
```

## 贡献一份力量

如果你遇到什么 bug，或是有什么建议，欢迎提 [issues](https://github.com/tankery/permission-requester/issues)。 并且 [Pull Request](https://github.com/tankery/permission-requester/pulls) 也是极好的。感谢你的关注~

