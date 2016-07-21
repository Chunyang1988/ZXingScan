# ZXingScan

先说已知问题，扫描只能竖屏扫描，横屏很多问题未去解决。
动态获取权限稍后制作。（已完成）
需要添加

    <activity
        android:name=".PermissionsActivity"
        android:theme="@style/AppTheme.NoActionBar">
	</activity>
	

个性化定制，则直接在CaptureActivity将XML更换即可。


#说明

添加Activity代码

    <activity
        android:name="org.zxing.scan.ui.CaptureActivity"
        android:screenOrientation="portrait"
        android:theme="@style/AppTheme.NoActionBar">
	</activity>

	
添加权限

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.FLASHLIGHT" />

    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />

    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />


	

核心代码为两个Manager

##ScanManager扫描


	//SurfaceView.getHolder();将Camer预览的显示在SurfaceView中
	void openDriver(SurfaceHolder holder)
	
	//结束预览，关闭自动对焦，关闭扫描解码
	void stop()

	
主要是以上两个公开方法，再此Manager中还有两个内部类，一个是计算摄像尺寸，一个是自动对焦


##DecodeManager解码

	//开始解码，获取一帧图片转换成YUV
	void decode()
	
	//结束解码
	void unsubscribe()