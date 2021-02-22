package com.example.homequick;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.icu.text.SymbolTable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    static String RUNAPPNAME_INI = new String("RunAppName.ini");
    static String RUNAPPNAME = new String("");

    EditText editTextRunAppName;
    Button buttonRunAppNameSave;
    Button buttonRunSettings;
    Button buttonUninstall;
    EditText editTextTextPassword;
    Button buttonCheckPassowrd;

    public static boolean functionEnable = false;
    public static long cnt = 0;
    public static long destoryTime = 0;
    public static long endTime = 0;
    public static boolean debugEnabule = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitLaunchApp();
        InitView();
        InitReceiver();//注册广播

        Toast.makeText(getApplicationContext(), "Start Home Quick", 1).show();

        debugEnabule = true;
        RunAppEvent(RUNAPPNAME);
        debugEnabule = false;
    }

    private void InitReceiver() {
        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(
                Intent.ACTION_BOOT_COMPLETED));
    }

    private void InitLaunchApp() {
        try {
            RUNAPPNAME = readLocalToString(RUNAPPNAME_INI);
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    public void RunAppEvent(String appName) {
        if (IsTimeRumApp()) {
            if (RunAppName(appName)) {
                ShowTime("launch(ok): ");
            } else {
                ShowTime("launch(fail): ");
            }
        }
    }

    public boolean writeToLocal(String fileName, String data) throws FileNotFoundException {
        try {
            FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
            osw.write(data);

            osw.flush();
            fos.flush();  //flush是为了输出缓冲区中所有的内容

            osw.close();
            fos.close();  //写入完成后，将两个输出关闭

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String saveCheck = new String("");
        try {
            saveCheck = readLocalToString(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (data.equals(saveCheck)) {
            return true;
        } else {
            return false;
        }
    };

    public String readLocalToString(String fileName) throws FileNotFoundException {
        String result = new String("");
        try {
            FileInputStream fis = openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
            char[] input = new char[fis.available()];  //available()用于获取filename内容的长度
            isr.read(input);  //读取并存储到input中

            isr.close();
            fis.close();//读取完成后关闭

            result = new String(input); //将读取并存放在input中的数据转换成String输出
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    };

    private void InitView() {
        InitLaunchApp();

        editTextRunAppName = (EditText) findViewById(R.id.editTextRunAppName);
        buttonRunAppNameSave = (Button) findViewById(R.id.buttonRunAppNameSave);
        buttonRunSettings = (Button) findViewById(R.id.buttonRunSettings);
        buttonUninstall = (Button) findViewById(R.id.buttonUninstall);

        editTextTextPassword = (EditText)findViewById(R.id.editTextTextPassword);
        buttonCheckPassowrd = (Button)findViewById(R.id.buttonCheckPassowrd);


        editTextRunAppName.setText(RUNAPPNAME);

        buttonRunAppNameSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!CheckPassword(editTextTextPassword.getText().toString())) {
                    return;
                }

                RUNAPPNAME = editTextRunAppName.getText().toString();

                try {
                    if (writeToLocal(RUNAPPNAME_INI, RUNAPPNAME)) {
                        Toast.makeText(getApplicationContext(), "Save success", 1).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Save false", 1).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        buttonRunSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CheckPassword(editTextTextPassword.getText().toString())) {
                    return;
                }

                RunAppName("com.android.settings");
                Toast.makeText(getApplicationContext(), "run Settings success", 1).show();
            }
        });
        buttonUninstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CheckPassword(editTextTextPassword.getText().toString())) {
                    return;
                }

                Uri uri = Uri.parse(MainActivity.this.getPackageName());
                Intent intent = new Intent(Intent. ACTION_DELETE ,uri);
                startActivity(intent);

                Toast.makeText(getApplicationContext(), "Uninstall success", 1).show();
            }
        });

        buttonCheckPassowrd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CheckPassword(editTextTextPassword.getText().toString())) {
                    Toast.makeText(getApplicationContext(), "password is true", 1).show();
                }
            }
        });

        editTextTextPassword.setFocusable(true);
        editTextTextPassword.requestFocus();
    };

    /**
     * 监听是否点击了home键将客户端推到后台
     */
    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_HOME_KEY_LONG = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)) {
                    //表示按了home键,程序到了后台
                    UpdateDestoryTime();
                    RunAppEvent(RUNAPPNAME);
                } else if (TextUtils.equals(reason, SYSTEM_HOME_KEY_LONG)) {
                    //表示长按home键,显示最近使用的程序列表
                }
            }
            if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Intent thisIntent = new Intent(context, MainActivity.class);//设置要启动的app
                thisIntent.setAction("android.intent.action.MAIN");
                thisIntent.addCategory("android.intent.category.LAUNCHER");
                thisIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(thisIntent);
            }
        };
    };

    // 点击事件判断，是否达到
    public boolean IsTimeRumApp() {
        if (destoryTime < 1500) {
            if (cnt >= 1) {
                cnt = 0;
                return false;
            }
            cnt++;
            return true;
        }
        cnt = 0;
        return true;
    };

    // 运行某个APP
    public boolean RunAppName(String appName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(appName);
        if (intent != null) {
            intent.putExtra("type", "110");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//销毁目标Activity和它之上的所有Activity，重新创建目标Activity
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    };

    public void ShowTime(String info) {
        if (!debugEnabule) {
            return;
        }

        String showtest = new String();
        showtest = info + "StopTime:" + String.valueOf(cnt).toString() +
                "/1, dTime:" + String.valueOf(destoryTime).toString()
                + "/1000" + ", run:" + RUNAPPNAME;

        Toast.makeText(getApplicationContext(), showtest, 1).show();
    };

    public void UpdateDestoryTime() {
        destoryTime = SystemClock.elapsedRealtime() - endTime;
        endTime = SystemClock.elapsedRealtime();
    };

    public boolean CheckPassword(String inputPassword) {
        String PASSWORD = new String("kei");
        if (inputPassword.equals(PASSWORD)) {
            debugEnabule = true;
            return functionEnable = true;
        } else {
            Toast.makeText(getApplicationContext(), "password is false", 1).show();
            debugEnabule = false;
            return functionEnable = false;
        }
    }


    /**
      * 悬浮窗功能支持
      */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            // 获取WindowManager服务
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            // 新建悬浮窗控件
            Button button = new Button(getApplicationContext());
            //button.setText("Floating Window");
            button.setBackgroundColor(Color.GREEN);
            button.setAlpha(0.1f);

            // 根据android版本设置悬浮窗类型
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            layoutParams.format = PixelFormat.RGBA_8888;

            //设置大小
            layoutParams.width = 140;
            layoutParams.height = 140;

            //设置位置
            layoutParams.x = 0;
            layoutParams.y = 788;

            // 将悬浮窗控件添加到WindowManager
            windowManager.addView(button, layoutParams);
        }
    };
};