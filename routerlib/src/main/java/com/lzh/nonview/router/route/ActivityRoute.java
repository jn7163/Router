package com.lzh.nonview.router.route;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.lzh.nonview.router.Utils;
import com.lzh.nonview.router.exception.NotFoundException;
import com.lzh.nonview.router.module.RouteMap;
import com.lzh.nonview.router.parser.RouterParser;
import com.lzh.nonview.router.parser.URIParser;

import java.util.Map;
import java.util.Set;

/**
 * Created by lzh on 16/9/5.
 */
public class ActivityRoute implements IActivityRoute {
    Uri uri;
    Bundle bundle;
    ActivityRouteBundleExtras extras;
    RouteMap routeMap = null;
    RouteCallback callback = new EmptyCallback();

    public void setCallback (RouteCallback callback) {
        if (callback != null) {
            this.callback = callback;
        }
    }

    @Override
    public void replaceBundleExtras(ActivityRouteBundleExtras extras) {
        if (extras != null) {
            this.extras = extras;
        }
    }

    @Override
    public void open(Context context, Uri uri) {
        try {
            if (callback.interceptOpen(uri,context,extras)) return;

            ActivityRoute route = (ActivityRoute) getRoute(uri);
            route.openInternal(context);

            callback.onOpenSuccess(uri,routeMap.getClzName());
        } catch (Exception e) {
            if (e instanceof NotFoundException) {
                callback.notFound(uri,routeMap.getClzName());
            } else {
                callback.onOpenFailed(uri,e);
            }
        }
    }

    @Override
    public boolean canOpenRouter(Uri uri) {
        return getRouteMapByUri(new URIParser(uri)) != null;
    }

    RouteMap getRouteMapByUri (URIParser parser) {
        String route = parser.getScheme() + "://" + parser.getHost();
        return RouterParser.INSTANCE.getRouteMap().get(route);
    }

    @Override
    public IRoute getRoute(Uri uri) {
        this.uri = uri;
        this.extras = new ActivityRouteBundleExtras();

        URIParser parser = new URIParser(uri);
        routeMap = getRouteMapByUri(parser);
        Map<String, String> keyMap = routeMap.getParams();

        bundle = new Bundle();
        Map<String, String> params = parser.getParams();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            String type = keyMap.get(key);
            putExtraByType(bundle, params, key, type);
        }
        return this;
    }

    static void putExtraByType(Bundle extras, Map<String, String> params, String key, String type) {
        if (Utils.isEmpty(type))
            type = "S";// when not set this key in router.json,reset type to string
        switch (type) {
            case "b":
                extras.putByte(key,Byte.parseByte(params.get(key)));
                break;
            case "s":
                extras.putShort(key,Short.parseShort(params.get(key)));
                break;
            case "i":
                extras.putInt(key,Integer.parseInt(params.get(key)));
                break;
            case "l":
                extras.putLong(key,Long.parseLong(params.get(key)));
                break;
            case "f":
                extras.putFloat(key,Float.parseFloat(params.get(key)));
                break;
            case "d":
                extras.putDouble(key,Double.parseDouble(params.get(key)));
                break;
            case "c":
                extras.putChar(key,params.get(key).charAt(0));
                break;
            case "B":
                extras.putBoolean(key,Boolean.parseBoolean(params.get(key)));
                break;
            case "S":
            default://string
                extras.putString(key,params.get(key));
                break;
        }
    }

    @Override
    public void open(Context context) {
        try {
            if (callback.interceptOpen(uri,context,extras)) return;
            openInternal(context);
            callback.onOpenSuccess(uri,routeMap.getClzName());
        } catch (Exception e) {
            if (e instanceof NotFoundException) {
                callback.notFound(uri,routeMap.getClzName());
            } else {
                callback.onOpenFailed(this.uri,e);
            }
        }
    }

    private void openInternal(Context context) {
        String clzName = routeMap.getClzName();
        if (!Utils.isClassSupport(clzName)) {
            throw new NotFoundException();
        }
        Intent intent = new Intent();
        intent.setClassName(context,routeMap.getClzName());
        intent.putExtras(bundle);
        intent.putExtras(extras.getExtras());
        if (context instanceof Activity) {
            ((Activity) context).startActivityForResult(intent,extras.getRequestCode());
            int inAnimation = extras.getInAnimation();
            int outAnimation = extras.getOutAnimation();
            if (inAnimation > 0 && outAnimation > 0) {
                ((Activity) context).overridePendingTransition(inAnimation,outAnimation);
            }
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    @Override
    public IActivityRoute requestCode(int requestCode) {
        this.extras.setRequestCode(requestCode);
        return this;
    }

    @Override
    public Bundle getExtras() {
        return this.extras.getExtras();
    }

    @Override
    public IActivityRoute setAnim(int enterAnim, int exitAnim) {
        this.extras.setInAnimation(enterAnim);
        this.extras.setOutAnimation(exitAnim);
        return this;
    }

    @Override
    public IActivityRoute setExtras(Bundle extras) {
        this.extras.setExtras(extras);
        return this;
    }

}