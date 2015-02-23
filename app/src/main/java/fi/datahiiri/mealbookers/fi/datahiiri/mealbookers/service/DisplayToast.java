package fi.datahiiri.mealbookers.fi.datahiiri.mealbookers.service;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Lisa och Ilkka on 20.2.2015.
 */
public class DisplayToast implements Runnable {
    private final Context mContext;
    String mText;

    public DisplayToast(Context mContext, String text){
        this.mContext = mContext;
        mText = text;
    }

    public void run(){
        Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
    }
}