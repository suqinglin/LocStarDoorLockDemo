## 1.接入流程
#### 1.1 Android Studio集成 （推荐）
    
    (1) 从本Demo中下载/app/libs/LocStarSDK_xxx.jar，在Android Studio的项目工程libs目录中拷入已下载的jar包
    (2) 右键Android Studio的项目工程—>选择Open Module Settings —>在 Project Structure弹出框中 —>选择 Dependencies选项卡 —>点击左下“＋”—>选择Jar Dependency—>引入libs目录下的jar包
如果有老版本jar文件存在，请删除
#### 1.2 相关代码设置
        // 初始化配置(必须配置，不然会异常)
        LocStarManager.getInstance().init(this, IP, HOTEL_NUM);
#### 1.3 日志设置(可选)
        // 默认打开
        LocStarManager.getInstance().setDebugMode(true);
#### 1.4 添加相关权限
```xml
    <!--网络权限-->
    <uses-permission android:name="android.permission.INTERNET"/>
    <!--蓝牙权限-->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <!-- 这个权限用于访问GPS定位 -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```
#### 1.5 其他必须依赖
```xml
    //rxandroid
    implementation 'io.reactivex.rxjava2:rxandroid:2.0.1'
    //rxjava
    implementation 'io.reactivex.rxjava2:rxjava:2.1.5'
```
### 具体方法调用请参考 com.nexless.locstar.MainActivity