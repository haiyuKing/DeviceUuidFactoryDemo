package com.why.project.deviceuuidfactorydemo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Used 获取设备唯一标识码的UUID（加密）【需要运行时权限的处理的配合】
 * 以 DEVICE_ID为基础，在获取DEVICE_ID失败时以获取SimSerialNumber为备选方法，如果再失败，使用ANDROID_ID，如果再失败使用UUID的生成策略
 * @参考资料 获取Android设备唯一标识码:http://www.cnblogs.com/lvcha/p/3721091.html
 * [Android] 获取Android设备的唯一识别码｜设备号｜序号｜UUID:http://www.cnblogs.com/xiaowenji/archive/2011/01/11/1933087.html
 *  UUID（通用唯一标识符）http://blog.csdn.net/fanxiaobin577328725/article/details/51711062
 */
public class DeviceUuidFactory {

	//用于存储生成的设备的唯一标识码的UUID（加密）
	protected static final String PREFS_FILE = "device_id.xml";
    protected static final String PREFS_DEVICE_ID = "device_id";

    /**
     * 表示一个不变的通用唯一标识符(UUID)。以下是有关UUID的要点：
     * 一个UUID表示一个128位的值。
     * 它是用于创建随机文件名、Web应用程序的会话ID，事务ID等。
     * UUID有四种不同的基本类型：(1)基于时间，(2)DCE安全性，(3)基于名称，(4)伪随机生成的UUID。*/
    protected UUID uuid;

    public DeviceUuidFactory(Context context) {
    	if(uuid ==null ) {
            synchronized (DeviceUuidFactory.class) {
                if(uuid == null) {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE,Context.MODE_PRIVATE);
                    String oldId = prefs.getString(PREFS_DEVICE_ID, null);

                    if (oldId != null) {//如果存在存储的唯一标识码的UUID值，则直接读取
                        uuid = UUID.fromString(oldId);
                    } else {
						/*===============================================（1）获取DEVICE_ID===============================================================*/
						/* 缺陷：
						* 1、非手机设备： 如果只带有Wifi的设备或者音乐播放器没有通话的硬件功能的话(如平板电脑、电子书、电视、音乐播放器等)就没有这个DEVICE_ID
						* 2、权限： 获取DEVICE_ID需要READ_PHONE_STATE权限，但如果我们只为了获取它，没有用到其他的通话功能，那这个权限有点大才小用
						* 3、bug：在少数的一些手机设备上，该实现有漏洞，会返回垃圾，如:zeros或者asterisks的产品
						*/
                    	String deviceId = ((TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE )).getDeviceId();//根据不同的手机设备返回IMEI，MEID或者ESN码
						Log.w("DeviceUuidFactory","deviceId="+deviceId);
						try {
							if(deviceId != null){
								uuid = UUID.nameUUIDFromBytes(deviceId.getBytes("utf8"));//获取一个类型3（基于名称的），根据指定的字节数组的UUID。
							}else{
								/*===============================================（2）获取SimSerialNumber====================================================*/
								/*缺陷：
								* 装有SIM卡的设备，可以获取到数值；对于CDMA设备，返回的是一个空值！
								* */
								String SimSerialNumber = ((TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE )).getSimSerialNumber();

			                	if(SimSerialNumber != null){
			                		uuid = UUID.nameUUIDFromBytes(SimSerialNumber.getBytes("utf8"));
			                	}else{
									/*===============================================（3）获取ANDROID_ID====================================================*/
			                		/*缺陷：
			                		* 厂商定制系统的Bug：不同的设备可能会产生相同的ANDROID_ID：9774d56d682e549c。
			                		* 厂商定制系统的Bug：有些设备返回的值为null。
			                		* 设备差异：对于CDMA设备，ANDROID_ID和TelephonyManager.getDeviceId() 返回相同的值。
			                		* 在Android <=2.1 or Android >=2.3的版本是可靠、稳定的，但在2.2的版本并不是100%可靠的
			                		* ANDROID_ID是设备第一次启动时产生和存储的64bit的一个数，当设备被wipe（刷机）后该数重置
			                		* */
			                        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
			                        
			                        if (!"9774d56d682e549c".equals(androidId)) {
		                                uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
		                            } else {
										/*===============================================（4）Installtion ID : UUID====================================================*/
		                            	/*该方法无需访问设备的资源，也跟设备类型无关。
		                            	* 这种方式的原理是在程序安装后第一次运行时生成一个ID，该方式和设备唯一标识不一样，不同的应用程序会产生不同的ID，同一个程序重新安装也会不同。
		                            	* 所以这不是设备的唯一ID，但是可以保证每个用户的ID是不同的。可以说是用来标识每一份应用程序的唯一ID（即Installtion ID），可以用来跟踪应用的安装数量等。
		                            	* */
										uuid = UUID.nameUUIDFromBytes(new Installation().id(context).getBytes("utf8"));
		                            }
			                	}
							}
						} catch (UnsupportedEncodingException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                        
                        // Write the value out to the prefs file
                        prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString()).commit();
                    }

                }
            }
        }
    }
    
    /**获取UUID*/
    public UUID getUuid() {
		return uuid;
	}


    /**这种方式是通过在程序安装后第一次运行后生成一个ID实现的
     * 但该方式跟设备唯一标识不一样，不同的应用程序会产生不同的ID，同一个程序重新安装也会不同。所以这不是设备的唯一ID，但是可以保证每个用户的ID是不同的。
     * 因此经常用来标识在某个应用中的唯一ID（即Installtion ID），或者跟踪应用的安装数量。
     * 很幸运的，Google Developer Blog提供了这样的一个框架：*/
	class Installation {
        private String sID = null;
        private final String INSTALLATION = "INSTALLATION";

        public synchronized String id(Context context) {
            if (sID == null) {  
                File installation = new File(context.getFilesDir(), INSTALLATION);
                try {
                    if (!installation.exists())
                    {
                    	writeInstallationFile(installation);
                    }
                    sID = readInstallationFile(installation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return sID;
        }

        private String readInstallationFile(File installation) throws IOException {
            RandomAccessFile f = new RandomAccessFile(installation, "r");
            byte[] bytes = new byte[(int) f.length()];
            f.readFully(bytes);
            f.close();
            return new String(bytes);
        }

        private void writeInstallationFile(File installation) throws IOException {
            FileOutputStream out = new FileOutputStream(installation);
            String id = UUID.randomUUID().toString();
            out.write(id.getBytes());
            out.close();
        }
    }
}
