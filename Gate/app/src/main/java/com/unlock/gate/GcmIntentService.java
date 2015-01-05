package com.unlock.gate;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unlock.gate.models.Post;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by davidilizarov on 12/23/14.
 */
public class GcmIntentService extends IntentService {
    public static final int POST_CREATED_NOTIFICATION = 42;
    public static final int COMMENT_CREATED_NOTIFICATION = 126;
    public static final int POST_LIKED_NOTIFICATION = 168;
    public static final int COMMENT_LIKED_NOTIFICATION = 210;

    public static final int GATE_NOTIFICATION_ID = 1;

    public static int notification_type;

    private NotificationManager mNotificationManager;
    NotificationCompat.Builder mBuilder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType) &&
                extras.containsKey("notification_type")) {

                notification_type = Integer.parseInt(extras.getString("notification_type"));

                switch (notification_type) {
                    case POST_CREATED_NOTIFICATION:
                        sendToMainActivityNotification(extras);
                        break;
                    case COMMENT_CREATED_NOTIFICATION:
                    case POST_LIKED_NOTIFICATION:
                    case COMMENT_LIKED_NOTIFICATION:
                        sendToCommentsActivityNotification(extras);
                        break;
                }
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Takes care of when a Post is created
    private void sendToMainActivityNotification(Bundle extras) {
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.actionbar_logo)
                .setContentTitle(extras.getString("title"))
                .setContentText(extras.getString("summary"))
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(extras.getString("extended_text"))
                )
                .setLights(Color.WHITE, 2000, 2000)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("notification", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(contentIntent);

        Notification notification = mBuilder.build();
        notification.ledARGB = 0xffffff;
        notification.tickerText = extras.getString("gate") + "\n" +
                                  extras.getString("summary") + "\n" +
                                  extras.getString("extended_text");

        mNotificationManager.notify(GATE_NOTIFICATION_ID, notification);
    }

    // Takes care of when a Comment is created, a comment liked or post liked
    private void sendToCommentsActivityNotification(Bundle extras) {
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.actionbar_logo)
                .setContentTitle(extras.getString("title"))
                .setContentText(extras.getString("summary"))
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(extras.getString("extended_text"))
                )
                .setLights(Color.WHITE, 2000, 2000)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("notification", true);

        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> postData = new Gson().fromJson(extras.getString("post"), listType);

        Post post = new Post(
                postData.get(0),
                postData.get(1),
                postData.get(2),
                postData.get(3),
                postData.get(4),
                postData.get(5),
                postData.get(6),
                postData.get(7),
                postData.get(8)
        );

        intent.putExtra("post", post);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(CommentsActivity.class);
        stackBuilder.addNextIntent(intent);

        PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(contentIntent);

        Notification notification = mBuilder.build();

        if (notification_type == COMMENT_CREATED_NOTIFICATION) {
            notification.tickerText = extras.getString("title") + "\n" +
                                      extras.getString("summary") + "\n" +
                                      extras.getString("extended_text");
        } else {
            notification.tickerText = extras.getString("title") + "\n" +
                                      extras.getString("summary");
        }

        mNotificationManager.notify(GATE_NOTIFICATION_ID, notification);
    }
}