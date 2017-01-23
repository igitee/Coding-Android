package net.coding.program;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.baidu.mapapi.SDKInitializer;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import net.coding.program.common.Global;
import net.coding.program.common.PhoneType;
import net.coding.program.common.RedPointTip;
import net.coding.program.common.Unread;
import net.coding.program.common.network.MyAsyncHttpClient;
import net.coding.program.common.ui.GlobalUnit;
import net.coding.program.common.util.FileUtil;
import net.coding.program.model.AccountInfo;
import net.coding.program.model.UserObject;
import net.coding.program.third.MyImageDownloader;

import java.util.List;

/**
 * Created by cc191954 on 14-8-9.
 * 用来做一些初始化工作，比如设置 host，
 * 初始化图片库配置
 */
public class MyApp extends MultiDexApplication {

    public static float sScale;
    public static int sWidthDp;
    public static int sWidthPix;
    public static int sHeightPix;

    public static int sEmojiNormal;
    public static int sEmojiMonkey;

    public static UserObject sUserObject;
    public static Unread sUnread;

    private static int sMainCreate = 0;

    public static boolean getMainActivityState() {
        return sMainCreate > 0;
    }

    public static void setMainActivityState(boolean create) {
        if (create) {
            ++sMainCreate;
        } else {
            --sMainCreate;
        }
        Log.d("", "showsss " + sMainCreate);
    }

    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCacheSize(50 * 1024 * 1024) // 50 Mb
                .diskCacheFileCount(300)
                .imageDownloader(new MyImageDownloader(context))
                .tasksProcessingOrder(QueueProcessingType.LIFO)
//                .writeDebugLogs() // Remove for release app
                .diskCacheExtraOptions(sWidthPix / 3, sWidthPix / 3, null)
                .build();

        ImageLoader.getInstance().init(config);
    }

    private static String getProcessName(Context context) {
        ActivityManager actMgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appList = actMgr.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : appList) {
            if (info.pid == android.os.Process.myPid()) {
                return info.processName;
            }
        }
        return "";
    }

    // 应对修改企业版路径以 /p/project 开头的问题
    public static String transformEnterpriseUri(String uri) {
        if (uri.startsWith("/p/")) {
            uri = String.format("/u/%s%s", enterpriseGK, uri);
        } else if (uri.startsWith(Global.HOST + "/p/")) {
            int pathStart = Global.HOST.length();
            String uriPath = uri.substring(pathStart, uri.length());
            uri = String.format("/u/%s%s", enterpriseGK, uriPath);
        }

        return uri;
    }

    private static String enterpriseGK = "";

    public static String getEnterpriseGK() {
        return enterpriseGK;
    }

    public static void setEnterpriseGK(String enterpriseGK) {
        MyApp.enterpriseGK = enterpriseGK;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        AccountInfo.CustomHost customHost = AccountInfo.getCustomHost(this);
        String host = customHost.getHost();
        if (host.isEmpty()) {
            host = Global.DEFAULT_HOST;
        }
        Global.HOST = host;
        Global.HOST_API = Global.HOST + "/api";

        try {
            Global.sVoiceDir = FileUtil.getDestinationInExternalFilesDir(this, Environment.DIRECTORY_MUSIC, FileUtil.DOWNLOAD_FOLDER).getAbsolutePath();
            Log.w("VoiceDir", Global.sVoiceDir);
        } catch (Exception e) {
            Global.errorLog(e);
        }


        MyAsyncHttpClient.init(this);

        initImageLoader(this);

        loadBaiduMap();

        sScale = getResources().getDisplayMetrics().density;
        sWidthPix = getResources().getDisplayMetrics().widthPixels;
        sHeightPix = getResources().getDisplayMetrics().heightPixels;
        sWidthDp = (int) (sWidthPix / sScale);

        sEmojiNormal = getResources().getDimensionPixelSize(R.dimen.emoji_normal);
        sEmojiMonkey = getResources().getDimensionPixelSize(R.dimen.emoji_monkey);

        sUserObject = AccountInfo.loadAccount(this);
        sUnread = new Unread();

        RedPointTip.init(this);
        GlobalUnit.init();
    }

    private void loadBaiduMap() {
        if (!PhoneType.isX86or64()) {
            // x86的机器上会抛异常，因为百度没有提供x86的.so文件
            // 64 位的机器也不行
            // 只在主进程初始化lbs
            if (this.getPackageName().equals(getProcessName(this))) {
                SDKInitializer.initialize(this);
            }
        }
    }
}
