package com.crazecoder.openfile;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.content.FileProvider;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

import java.io.File;

public class OpenFilePlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

    private @Nullable FlutterPluginBinding flutterPluginBinding;
    private Context context;
    private Activity activity;
    private MethodChannel channel;
    private Result result;
    private String filePath;
    private String typeString;
    private boolean isResultSubmitted = false;

    private static final int REQUEST_CODE = 33432;

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        isResultSubmitted = false;
        if (call.method.equals("open_file")) {
            this.result = result;
            filePath = call.argument("file_path");
            typeString = call.argument("type") != null ? call.argument("type") : "*/*";

            if (pathRequiresPermission()) {
                if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    startActivity();
                }
            } else {
                startActivity();
            }
        } else {
            result.notImplemented();
            isResultSubmitted = true;
        }
    }

    private boolean pathRequiresPermission() {
        // Implement the logic for checking whether a file path needs special permissions.
        return filePath != null && filePath.contains(Environment.getExternalStorageDirectory().getPath());
    }

    private boolean isMediaStorePath() {
        // Implement logic for checking if the file path is within MediaStore (e.g., for images, videos, etc.)
        return filePath != null && filePath.contains("MediaStore");
    }

    private void requestPermission(String permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, REQUEST_CODE);
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
    }

    private void startActivity() {
        if (filePath == null) {
            result(-4, "the file path cannot be null");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            result(-2, "The file does not exist");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
        intent.setDataAndType(uri, typeString);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            result(-1, "No application found to open the file.");
        }
    }

    private void result(int type, String message) {
        if (result != null && !isResultSubmitted) {
            result.success("Type: " + type + " Message: " + message);
            isResultSubmitted = true;
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.flutterPluginBinding = binding;
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "open_file");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }
}
