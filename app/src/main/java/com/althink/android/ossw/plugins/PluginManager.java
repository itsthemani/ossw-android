package com.althink.android.ossw.plugins;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by krzysiek on 12/06/15.
 */
public class PluginManager {

    private final static String TAG = PluginManager.class.getSimpleName();

    private static final String API_COLUMN_ID = "_id";
    private static final String API_COLUMN_NAME = "name";
    private static final String API_COLUMN_DESCRIPTION = "description";

    private Context context;

    public PluginManager(Context context) {
        this.context = context;
    }

    public List<PluginDefinition> findPlugins(String packageName) {
        LinkedList<PluginDefinition> plugins = new LinkedList<>();
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA);
            if (packageInfo.providers == null)
                return plugins;
            for (ProviderInfo provider : packageInfo.providers) {
                addToListIfPlugin(packageManager, plugins, provider);
            }
            return plugins;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return plugins;
    }

    public List<PluginDefinition> findPlugins() {
        List<PluginDefinition> plugins = new LinkedList<>();
        PackageManager packageManager = context.getPackageManager();
        try {
            // This implementation tries to minimize the IPC data by analysing package by package
            // Workaround the crash when there are too much content providers
            List<PackageInfo> packages = packageManager.getInstalledPackages(0);
            for (PackageInfo p : packages) {
                PackageInfo packageInfo = packageManager.getPackageInfo(p.packageName, PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA);
                if (packageInfo.providers != null)
                    for (ProviderInfo provider : packageInfo.providers)
                        addToListIfPlugin(packageManager, plugins, provider);
            }
            Collections.sort(plugins);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return plugins;
    }

    private void addToListIfPlugin(PackageManager packageManager, List<PluginDefinition> plugins, ProviderInfo provider) {
        if (provider.metaData != null && provider.metaData.containsKey("com.althink.android.ossw.plugin")) {
            PluginDefinition plugin = new PluginDefinition(provider.authority, provider.loadLabel(packageManager).toString(), provider.packageName);
            fillPluginApi(plugin);
            plugins.add(plugin);
        }
    }

    private void fillPluginApi(PluginDefinition plugin) {

        List<PluginPropertyDefinition> properties = new LinkedList<>();
        List<PluginFunctionDefinition> functions = new LinkedList<>();

        try {
            Uri propertiesApiUri = Uri.parse("content://" + plugin.getPluginId() + "/api/properties");
            Cursor cursor = context.getContentResolver().query(propertiesApiUri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    properties.add(new PluginPropertyDefinition(cursor.getInt(0), cursor.getString(1), cursor.getString(2), PluginPropertyType.valueOf(cursor.getString(3))));
                }
            }
            cursor.close();
        } catch (Exception e) {
            //Log.e(TAG, "Failed to load plugin properties: " + plugin.getPluginId());
        }
        plugin.setProperties(properties);

        try {
            Uri functionsApiUri = Uri.parse("content://" + plugin.getPluginId() + "/api/functions");
            Cursor cursor = context.getContentResolver().query(functionsApiUri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    functions.add(new PluginFunctionDefinition(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
                }
            }
            cursor.close();
        } catch (Exception e) {
            //Log.e(TAG, "Failed to load plugin functions: " + plugin.getPluginId());
        }
        plugin.setFunctions(functions);
    }

}
