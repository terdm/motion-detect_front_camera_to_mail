package com.diter.motiondetection;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static android.os.BatteryManager.BATTERY_PLUGGED_AC;
import static android.os.BatteryManager.BATTERY_PLUGGED_USB;


/** Takes a single photo on service start. */
public class SendMailIntentService extends IntentService {

    String LOG_TAG = "myTag";
    String aFile;
    String pwd;
    String emailFrom;
    String emailTo;

    public void appendLog(String tag,String text)
    {

        Log.d("MyTag",text);
        File logDir = new File("/storage/extSdCard/log");
        if (!logDir.exists())
        {
            try
            {
                logDir.mkdirs();
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.d("MyTag","error1 logDir " + e.toString());
            }
        }
        File logFile = new File(logDir + File.separator +  "log.file");
        if (!logFile.exists())
        {
            Log.d("MyTag","NOT logFile.exists");
            try
            {
                logFile.createNewFile();
                Log.d("MyTag","after logFile.createNewFile");
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.d("MyTag","error1 " + e.toString());
            }
        }
         try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            Date currentTime = Calendar.getInstance().getTime();

            buf.append(tag + " " + currentTime.toString() + "    PID: "+
                    android.os.Process.myPid()+ "    TID: "+android.os.Process.myTid()+text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d("MyTag","error2 " + e.toString());
        }
    }


    public String getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BATTERY_PLUGGED_AC;



        return " level " + level + " scale " + scale + " usbCharge " + usbCharge + " acCharge " + acCharge;
    }

    public SendMailIntentService() {
        super("myname");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        aFile = intent.getStringExtra("file");
        pwd = intent.getStringExtra("pwd");
        emailFrom = intent.getStringExtra("emailFrom");
        emailTo = intent.getStringExtra("emailTo");
        appendLog(LOG_TAG, "SendMailIntentService onHandleIntent start file " + aFile.toString() );
        //sendMail(this);
        sendMailWithAttach(this);
        appendLog(LOG_TAG, "SendMailIntentService onHandleIntent end " );
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        showMessage("SendMailIntentService onCreate");

    }

    @SuppressWarnings("deprecation")
    private /*static*/ void sendMail(final Context context) {
                                                   try {


                                                       GMailSender sender = new GMailSender("mail", "pwd");
                                                       appendLog("myTag", "Before sending mail");
                                                       sender.sendMail("This is Subject",
                                                               "This is Body",
                                                               "from mail",
                                                               "to mail");
                                                       appendLog("myTag", "After sending mail");
                                                   } catch (Exception e) {
                                                       appendLog("myTag", e.toString());
                                                   }
    }

    @SuppressWarnings("deprecation")
    private /*static*/ void sendMailWithAttach(final Context context) {



        appendLog(LOG_TAG, "sendMailWithAttach pwd " + pwd);
        Mail m = new Mail(emailFrom, pwd);

        String[] toArr = {emailTo};
        m.setTo(toArr);
        m.setFrom("diterentev@gmail.com");
        m.setSubject("MotionDetectorTriggered " + getBatteryLevel());
        m.setBody("Email body.");

        try {
            m.addAttachment(aFile);

            if(m.send()) {
                appendLog(LOG_TAG, "Email was sent successfully.");
            } else {
                appendLog(LOG_TAG, "Email was not sent.");
            }
        } catch(Exception e) {
            //Toast.makeText(MailApp.this, "There was a problem sending the email.", Toast.LENGTH_LONG).show();
            appendLog(LOG_TAG, "Could not send email " + e.toString());
        }
        appendLog(LOG_TAG, "Battary " + getBatteryLevel());
    }


    private static void showMessage(String message) {
        Log.d("myTag", message);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}