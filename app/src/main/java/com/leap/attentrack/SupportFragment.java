package com.leap.attentrack;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class SupportFragment extends Fragment {
    private InterstitialAd myAd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragment_view = inflater.inflate(R.layout.fragment_support, container, false);
        myAd = new InterstitialAd(getContext());
//        myAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");     //test ads
        myAd.setAdUnitId("ca-app-pub-5022247600598681/3992615820");
        myAd.loadAd(new AdRequest.Builder().build());
        myAd.setAdListener(new AdListener(){
            @Override
            public void onAdClosed() {
                Toast.makeText(getContext(), "Thank You :)", Toast.LENGTH_SHORT).show();
                myAd.loadAd(new AdRequest.Builder().build());
            }
        });

        fragment_view.findViewById(R.id.ad_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myAd.isLoaded()) {
                    myAd.show();
                } else {
                    Log.i("mylog", "The interstitial wasn't loaded yet.");
                }
            }
        });

        fragment_view.findViewById(R.id.review_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("market://details?id=" + getContext().getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + getContext().getPackageName())));
                }
            }
        });

        return fragment_view;
    }
}
