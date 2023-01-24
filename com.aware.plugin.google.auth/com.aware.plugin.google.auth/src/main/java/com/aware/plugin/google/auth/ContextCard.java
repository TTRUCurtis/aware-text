package com.aware.plugin.google.auth;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aware.utils.IContextCard;

public class ContextCard implements IContextCard {

    //Empty constructor used to instantiate this card_google_login
    public ContextCard(){}

    //You may use sContext on uiChanger to do queries to databases, etc.
    private Context sContext;

    //Declare here all the UI elements you'll be accessing
    private View card;
    private ImageView photo;
    private TextView name, email;

    @Override
    public View getContextCard(Context context) {
        sContext = context;

        //Load card_google_login information to memory
        LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = sInflater.inflate(R.layout.card_google_login, null);

        //Initialize UI elements from the card_google_login
        photo = (ImageView) card.findViewById(R.id.google_photo);
        name = (TextView) card.findViewById(R.id.google_name);
        email = (TextView) card.findViewById(R.id.google_email);

        Cursor account_info = sContext.getContentResolver().query(Provider.Google_Account.CONTENT_URI, null, null, null, null);
        if( account_info != null && account_info.moveToFirst() ) {
            photo.setImageBitmap( getCircleBitmap( BitmapFactory.decodeByteArray( account_info.getBlob(account_info.getColumnIndex(Provider.Google_Account.PICTURE)), 0, account_info.getBlob(account_info.getColumnIndex(Provider.Google_Account.PICTURE)).length) ));
            name.setText(account_info.getString(account_info.getColumnIndex(Provider.Google_Account.NAME)));
            email.setText(account_info.getString(account_info.getColumnIndex(Provider.Google_Account.EMAIL)));
        }
        if( account_info != null && ! account_info.isClosed() ) account_info.close();

        //Return the card_google_login to AWARE/apps
        return card;
    }

    private Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);
        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        bitmap.recycle();
        return output;
    }
}
