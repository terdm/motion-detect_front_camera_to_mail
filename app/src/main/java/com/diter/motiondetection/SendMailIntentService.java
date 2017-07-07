package com.diter.motiondetection;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


/** Takes a single photo on service start. */
public class SendMailIntentService extends IntentService {

    String LOG_TAG = "myTag";
    String aFile;
    String pwd;
    String emailFrom;
    String emailTo;

    public SendMailIntentService() {
        super("myname");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        aFile = intent.getStringExtra("file");
        pwd = intent.getStringExtra("pwd");
        emailFrom = intent.getStringExtra("emailFrom");
        emailTo = intent.getStringExtra("emailTo");
        Log.d(LOG_TAG, "SendMailIntentService onHandleIntent start file " + aFile.toString() );
        //sendMail(this);
        sendMailWithAttach(this);
        Log.d(LOG_TAG, "SendMailIntentService onHandleIntent end " );
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
                                                       Log.d("myTag", "Before sending mail");
                                                       sender.sendMail("This is Subject",
                                                               "This is Body",
                                                               "from mail",
                                                               "to mail");
                                                       Log.d("myTag", "After sending mail");
                                                   } catch (Exception e) {
                                                       Log.d("myTag", e.toString());
                                                   }
    }

    @SuppressWarnings("deprecation")
    private /*static*/ void sendMailWithAttach(final Context context) {

        Log.d(LOG_TAG, "sendMailWithAttach pwd " + pwd);
        Mail m = new Mail(emailFrom, pwd);


        String[] toArr = {emailTo};
        m.setTo(toArr);
        m.setFrom("diterentev@gmail.com");
        m.setSubject("This is an email sent using my Mail JavaMail wrapper from an Android device.");
        m.setBody("Email body.");

        try {
            m.addAttachment(aFile);

            if(m.send()) {
                Log.d(LOG_TAG, "Email was sent successfully.");
            } else {
                Log.d(LOG_TAG, "Email was not sent.");
            }
        } catch(Exception e) {
            //Toast.makeText(MailApp.this, "There was a problem sending the email.", Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "Could not send email", e);
        }
    }


    private static void showMessage(String message) {
        Log.d("myTag", message);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}