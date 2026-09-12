package com.lhht.xiaozhi.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

/**
 * 晨曦定位工具：获取手机位置并尝试解析城市名
 */
public class LocationHelper {

    public interface LocationCallback {
        void onLocation(double latitude, double longitude, String city);
        void onError(String error);
    }

    public static void getLocation(Context context, LocationCallback callback) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onError("无定位权限");
            return;
        }
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location loc = null;
            if (lm != null) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) {
                    loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
            if (loc == null) {
                callback.onError("获取位置失败，请开启定位");
                return;
            }
            final double lat = loc.getLatitude();
            final double lon = loc.getLongitude();
            final Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                String city = "";
                try {
                    Geocoder gc = new Geocoder(context, Locale.CHINA);
                    List<Address> addrs = gc.getFromLocation(lat, lon, 1);
                    if (addrs != null && !addrs.isEmpty()) {
                        Address a = addrs.get(0);
                        city = a.getLocality();
                        if (city == null) city = a.getSubAdminArea();
                        if (city == null) city = a.getAdminArea();
                    }
                } catch (Exception e) {
                    // 地理编码失败时只上报经纬度
                }
                final String c = (city == null) ? "" : city;
                handler.post(() -> callback.onLocation(lat, lon, c));
            }).start();
        } catch (Exception e) {
            callback.onError("定位失败: " + e.getMessage());
        }
    }
}
