// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.webviewflutter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterFragmentActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.platform.PlatformViewRegistry;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.CookieManagerHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.DownloadListenerHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.FlutterAssetManagerHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.JavaObjectHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.JavaScriptChannelHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebChromeClientHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebSettingsHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebStorageHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebViewClientHostApi;
import io.flutter.plugins.webviewflutter.GeneratedAndroidWebView.WebViewHostApi;

/**
 * Java platform implementation of the webview_flutter plugin.
 *
 * <p>Register this in an add to app scenario to gracefully handle cacheDir and context changes.
 *
 * <p>Call {@link #registerWith} to use the stable {@code io.flutter.plugin.common} package instead.
 */
public class WebViewFlutterPlugin implements FlutterPlugin, ActivityAware {
  @Nullable private InstanceManager instanceManager;

  private FlutterPluginBinding pluginBinding;
  private WebViewHostApiImpl webViewHostApi;
  private JavaScriptChannelHostApiImpl javaScriptChannelHostApi;

  private static ActivityResultLauncher<String> _fileChooserLauncher;
  private static ActivityResultLauncher<String> _permissionLauncher;

  private static CompletableFuture<Uri> _fileChooserCompleter;
  private static CompletableFuture<Boolean> _cameraCompleter;

  @RequiresApi(api = Build.VERSION_CODES.N)
  static Future<Uri> openFileChooser(String s) {
    _fileChooserCompleter = new CompletableFuture<>();
    _fileChooserLauncher.launch(s);
    return _fileChooserCompleter;
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  static Future<Boolean> requestCameraPermission() {
    _cameraCompleter = new CompletableFuture<>();
    _permissionLauncher.launch(Manifest.permission.CAMERA);
    return _cameraCompleter;
  }

  /**
   * Add an instance of this to {@link io.flutter.embedding.engine.plugins.PluginRegistry} to
   * register it.
   *
   * <p>THIS PLUGIN CODE PATH DEPENDS ON A NEWER VERSION OF FLUTTER THAN THE ONE DEFINED IN THE
   * PUBSPEC.YAML. Text input will fail on some Android devices unless this is used with at least
   * flutter/flutter@1d4d63ace1f801a022ea9ec737bf8c15395588b9. Use the V1 embedding with {@link
   * #registerWith} to use this plugin with older Flutter versions.
   *
   * <p>Registration should eventually be handled automatically by v2 of the
   * GeneratedPluginRegistrant. https://github.com/flutter/flutter/issues/42694
   */
  public WebViewFlutterPlugin() {}

  /**
   * Registers a plugin implementation that uses the stable {@code io.flutter.plugin.common}
   * package.
   *
   * <p>Calling this automatically initializes the plugin. However plugins initialized this way
   * won't react to changes in cacheDir or context, unlike {@link WebViewFlutterPlugin}.
   */
  @SuppressWarnings({"unused", "deprecation"})
  public static void registerWith(io.flutter.plugin.common.PluginRegistry.Registrar registrar) {
    new WebViewFlutterPlugin()
        .setUp(
            registrar.messenger(),
            registrar.platformViewRegistry(),
            registrar.activity(),
            registrar.view(),
            new FlutterAssetManager.RegistrarFlutterAssetManager(
                registrar.context().getAssets(), registrar));
  }

  private void setUp(
      BinaryMessenger binaryMessenger,
      PlatformViewRegistry viewRegistry,
      Context context,
      View containerView,
      FlutterAssetManager flutterAssetManager) {
    instanceManager =
        InstanceManager.open(
            identifier ->
                new GeneratedAndroidWebView.JavaObjectFlutterApi(binaryMessenger)
                    .dispose(identifier, reply -> {}));

    viewRegistry.registerViewFactory(
        "plugins.flutter.io/webview", new FlutterWebViewFactory(instanceManager));

    webViewHostApi =
        new WebViewHostApiImpl(
            instanceManager,
            binaryMessenger,
            new WebViewHostApiImpl.WebViewProxy(),
            context,
            containerView);
    javaScriptChannelHostApi =
        new JavaScriptChannelHostApiImpl(
            instanceManager,
            new JavaScriptChannelHostApiImpl.JavaScriptChannelCreator(),
            new JavaScriptChannelFlutterApiImpl(binaryMessenger, instanceManager),
            new Handler(context.getMainLooper()));

    JavaObjectHostApi.setup(binaryMessenger, new JavaObjectHostApiImpl(instanceManager));
    WebViewHostApi.setup(binaryMessenger, webViewHostApi);
    JavaScriptChannelHostApi.setup(binaryMessenger, javaScriptChannelHostApi);
    WebViewClientHostApi.setup(
        binaryMessenger,
        new WebViewClientHostApiImpl(
            instanceManager,
            new WebViewClientHostApiImpl.WebViewClientCreator(),
            new WebViewClientFlutterApiImpl(binaryMessenger, instanceManager)));
    WebChromeClientHostApi.setup(
        binaryMessenger,
        new WebChromeClientHostApiImpl(
            instanceManager,
            new WebChromeClientHostApiImpl.WebChromeClientCreator(),
            new WebChromeClientFlutterApiImpl(binaryMessenger, instanceManager)));
    DownloadListenerHostApi.setup(
        binaryMessenger,
        new DownloadListenerHostApiImpl(
            instanceManager,
            new DownloadListenerHostApiImpl.DownloadListenerCreator(),
            new DownloadListenerFlutterApiImpl(binaryMessenger, instanceManager)));
    WebSettingsHostApi.setup(
        binaryMessenger,
        new WebSettingsHostApiImpl(
            instanceManager, new WebSettingsHostApiImpl.WebSettingsCreator()));
    FlutterAssetManagerHostApi.setup(
        binaryMessenger, new FlutterAssetManagerHostApiImpl(flutterAssetManager));
    CookieManagerHostApi.setup(binaryMessenger, new CookieManagerHostApiImpl());
    WebStorageHostApi.setup(
        binaryMessenger,
        new WebStorageHostApiImpl(instanceManager, new WebStorageHostApiImpl.WebStorageCreator()));
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    pluginBinding = binding;
    setUp(
        binding.getBinaryMessenger(),
        binding.getPlatformViewRegistry(),
        binding.getApplicationContext(),
        null,
        new FlutterAssetManager.PluginBindingFlutterAssetManager(
            binding.getApplicationContext().getAssets(), binding.getFlutterAssets()));
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (instanceManager != null) {
      instanceManager.close();
      instanceManager = null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
    updateContext(activityPluginBinding.getActivity());
    registerFileChooserActivityResult(activityPluginBinding.getActivity());
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  private void registerFileChooserActivityResult(Activity activity) {
    if (activity instanceof FlutterFragmentActivity) {
      _fileChooserLauncher =
          ((FlutterFragmentActivity) activity)
              .registerForActivityResult(
                  new FileChooserActivityResultContract(activity.getCacheDir()),
                  (Uri uri) -> _fileChooserCompleter.complete(uri));

      _permissionLauncher =
          ((FlutterFragmentActivity) activity)
              .registerForActivityResult(
                  new ActivityResultContracts.RequestPermission(),
                  (Boolean isGranted) -> _cameraCompleter.complete(isGranted));
    }
  }

  static class FileChooserActivityResultContract extends ActivityResultContract<String, Uri> {
    final File cacheDir;

    FileChooserActivityResultContract(File cacheDir) {
      this.cacheDir = cacheDir;
    }

    Uri fileUri;

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, String s) {
      Intent intent =
          Intent.createChooser(
              new Intent(Intent.ACTION_GET_CONTENT)
                  .addCategory(Intent.CATEGORY_OPENABLE)
                  .setType(s),
              "Choose files");

      if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
          == PackageManager.PERMISSION_GRANTED) {
        final File fileChooserDir = new File(context.getCacheDir(), "file_chooser");
        if (!fileChooserDir.exists()) {
          fileChooserDir.mkdir();
        }

        try {
          final File newFile =
              File.createTempFile("" + System.currentTimeMillis(), ".jpg", fileChooserDir);
          newFile.deleteOnExit();

          fileUri =
              FileProvider.getUriForFile(context, context.getPackageName() + ".provider", newFile);

          intent =
              intent.putExtra(
                  Intent.EXTRA_INITIAL_INTENTS,
                  new Intent[] {
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        .putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                  });
        } catch (IOException e) {
        }
      }

      return intent;
    }

    @Override
    public Uri parseResult(int resultCode, @Nullable Intent intent) {
      if (resultCode != Activity.RESULT_OK) {
        return null;
      }

      Uri result;
      if (intent != null) {
        result = intent.getData();
        if (result != null) {
          return result;
        }
      }

      result = fileUri;
      fileUri = null;
      return result;
    }
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    updateContext(pluginBinding.getApplicationContext());
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  @Override
  public void onReattachedToActivityForConfigChanges(
      @NonNull ActivityPluginBinding activityPluginBinding) {
    updateContext(activityPluginBinding.getActivity());
    registerFileChooserActivityResult(activityPluginBinding.getActivity());
  }

  @Override
  public void onDetachedFromActivity() {
    updateContext(pluginBinding.getApplicationContext());
    //    WebChromeClientHostApiImpl.SecureWebChromeClient.fileChooserLauncher = null;
  }

  private void updateContext(Context context) {
    webViewHostApi.setContext(context);
    javaScriptChannelHostApi.setPlatformThreadHandler(new Handler(context.getMainLooper()));
  }

  /** Maintains instances used to communicate with the corresponding objects in Dart. */
  @Nullable
  public InstanceManager getInstanceManager() {
    return instanceManager;
  }
}
