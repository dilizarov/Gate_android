package com.unlock.gate.services;

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
import com.unlock.gate.CommentsActivity;
import com.unlock.gate.MainActivity;
import com.unlock.gate.R;
import com.unlock.gate.models.Post;
import com.unlock.gate.receivers.GcmBroadcastReceiver;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by davidilizarov on 12/23/14.
 */
public class GcmIntentService extends IntentService {
    private static final int POST_CREATED_NOTIFICATION = 42;
    private static final int COMMENT_CREATED_NOTIFICATION = 126;
    private static final int POST_LIKED_NOTIFICATION = 168;
    private static final int COMMENT_LIKED_NOTIFICATION = 210;
    private static final int GATES_UNLOCKED_NOTIFICATION = 252;
    private static final int FIRST_TIME_GPS_TURNED_OFF = 5000;

    private static final int GATE_NOTIFICATION_ID = 1;

    private static final int NOTIF_LIGHT_INTERVAL = 2000;

    private static int notification_type;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

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

        mBuilder = new NotificationCompat.Builder(this);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType) &&
                extras.containsKey("notification_type")) {

                notification_type = Integer.parseInt(extras.getString("notification_type"));

                // TODO Rename what all of these do because this ain't right
                switch (notification_type) {
                    case POST_CREATED_NOTIFICATION:
                    case GATES_UNLOCKED_NOTIFICATION:
                        sendToMainActivityNotification(extras);
                        break;
                    case COMMENT_CREATED_NOTIFICATION:
                    case POST_LIKED_NOTIFICATION:
                    case COMMENT_LIKED_NOTIFICATION:
                        sendToCommentsActivityNotification(extras);
                        break;
                }
            } else if (extras.containsKey("notification_type")) {

                notification_type = Integer.parseInt(extras.getString("notification_type"));

                switch (notification_type) {
                    case FIRST_TIME_GPS_TURNED_OFF:
                        sendFirstTimeGpsTurnedOffNotification();
                        break;
                }
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Takes care of when a Post is created
    private void sendToMainActivityNotification(Bundle extras) {

        mBuilder.setSmallIcon(R.drawable.actionbar_logo)
                .setContentTitle(extras.getString("title"))
                .setContentText(extras.getString("summary"))
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(extras.getString("extended_text"))
                )
                .setLights(Color.WHITE, NOTIF_LIGHT_INTERVAL, NOTIF_LIGHT_INTERVAL);

        if (notification_type == POST_CREATED_NOTIFICATION) {
            mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        } else if (notification_type == GATES_UNLOCKED_NOTIFICATION) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("mainActivityNotification", true);

        intent.putExtra("notification_type", extras.getString("notification_type"));
        intent.putExtra("gate_id", extras.getString("gate_id", ""));
        intent.putExtra("gate_name", extras.getString("gate_name", ""));

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 424242, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(contentIntent);

        final Notification notification = mBuilder.build();

        if (notification_type == POST_CREATED_NOTIFICATION) {
            notification.tickerText = extras.getString("title") + "\n" +
                    extras.getString("summary") + "\n" +
                    extras.getString("post_body");
        } else if (notification_type == GATES_UNLOCKED_NOTIFICATION) {
            notification.tickerText = extras.getString("title") + "\n" +
                    extras.getString("extended_text");
        }

        mNotificationManager.notify(GATE_NOTIFICATION_ID, notification);
    }

    // Takes care of when a Comment is created, a comment liked or post liked
    private void sendToCommentsActivityNotification(Bundle extras) {
        mBuilder.setSmallIcon(R.drawable.actionbar_logo)
                .setContentTitle(extras.getString("title"))
                .setContentText(extras.getString("summary"))
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(extras.getString("extended_text"))
                )
                .setLights(Color.WHITE, NOTIF_LIGHT_INTERVAL, NOTIF_LIGHT_INTERVAL)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra("commentsActivityNotification", true);

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

        final Notification notification = mBuilder.build();

        if (notification_type == COMMENT_CREATED_NOTIFICATION) {
            notification.tickerText = extras.getString("title") + "\n" +
                                      extras.getString("summary") + "\n" +
                                      extras.getString("comment_body");
        } else {
            notification.tickerText = extras.getString("title") + "\n" +
                                      extras.getString("summary");
        }

        mNotificationManager.notify(GATE_NOTIFICATION_ID, notification);
    }

    private void sendFirstTimeGpsTurnedOffNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.drawable.actionbar_logo)
                .setContentTitle("Message from Gate")
                .setContentText("Gate sent you a one-time message")
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("Gate sent you a one-time message about GPS dismissal"))
                .setLights(Color.WHITE, NOTIF_LIGHT_INTERVAL, NOTIF_LIGHT_INTERVAL)
                .setDefaults(Notification.
                        DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("gpsTurnedOffNotification", true);

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 424243, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentIntent);

        final Notification notification = builder.build();
        notification.tickerText = "Message from Gate" + "\n" +
                "Gate sent you a one-time message about GPS dismissal";

        // We will generate a separate Gate notification for this, as this is important enough and only happens once.
        mNotificationManager.notify(GATE_NOTIFICATION_ID + 1, notification);
    }
}
