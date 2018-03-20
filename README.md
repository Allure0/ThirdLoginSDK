# ThirdLoginSDK

之前封过一个登录+支付的案例,后来想想还是将其分开更为合理,这里借鉴[SocialSDKAndroid](https://github.com/tsy12321/SocialSDKAndroid)

此处主要为第三方登录SDK的再次封装,使用者无需关心宿主的一些注册实现方式,一句话调用...

此DEMO无法直接运行,需要将applicationId更改为你的项目包名,release签名更改为你的签名配置即可...

### 引用方式
工程根目录build.gralde

```
allprojects {
    repositories {
        maven { url "https://dl.bintray.com/thelasterstar/maven" }      //微博sdk maven库
    }
}
```

主项目
```
compile 'com.allure0:ThirdLoginSDK:1.0.0'
```

### 如何使用

#### 权限

```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
```

#### 初始化
```

        PlatformConfig.setWeixin(WX_APPID);
        PlatformConfig.setQQ(QQ_APPID);
        PlatformConfig.setSinaWB(SINA_WB_APPKEY);

        mSocialApi = SocialApi.get(getApplicationContext());
        
```

### 微信登录
```
  mSocialApi.doOauthVerify(this, PlatformType.WEIXIN, new AuthListener() {
            @Override
            public void onComplete(PlatformType platform_type, Map<String, String> map) {
                
            }

            @Override
            public void onError(PlatformType platform_type, String err_msg) {

            }

            @Override
            public void onCancel(PlatformType platform_type) {

            }
        });
```

### QQ登录

Java:

```  
mSocialApi.doOauthVerify(this, PlatformType.QQ, new AuthListener() {
            @Override
            public void onComplete(PlatformType platform_type, Map<String, String> map) {
                
            }

            @Override
            public void onError(PlatformType platform_type, String err_msg) {

            }

            @Override
            public void onCancel(PlatformType platform_type) {

            }
        });

```
AndroidManifest:

```
 <!--QQ-->
        <activity
            android:name="com.tencent.tauth.AuthActivity"
            android:noHistory="true"
            android:launchMode="singleTask" >
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="tencent1105787445" /> <!--1111111改为你的qq appid-->
            </intent-filter>
        </activity>
        <activity
            android:name="com.tencent.connect.common.AssistActivity"
            android:screenOrientation="portrait"
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />
        <!--QQ-->
```

### 新浪登录

```
mSocialApi.doOauthVerify(this, PlatformType.SINA_WB, new AuthListener() {
            @Override
            public void onComplete(PlatformType platform_type, Map<String, String> map) {
                
            }

            @Override
            public void onError(PlatformType platform_type, String err_msg) {

            }

            @Override
            public void onCancel(PlatformType platform_type) {

            }
        });

```

## 分享示例请看DEMO。。。
