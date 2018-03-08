package com.telenor.possumauth.example.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.telenor.possumauth.PossumAuth;
import com.telenor.possumauth.example.MainActivity;
import com.telenor.possumauth.example.Messaging;
import com.telenor.possumauth.example.R;
import com.telenor.possumauth.example.Send;
import com.telenor.possumauth.example.views.IconWheel;
import com.telenor.possumauth.example.views.TrustButton;
import com.telenor.possumcore.abstractdetectors.AbstractDetector;

public class MainFragment extends TrustFragment {
    private IconWheel iconWheel;
    private TrustButton trustButton;
    private boolean isRegistered;
    private TextView status;
    private PossumAuth possumAuth;
    private MyPagerAdapter adapter;
    private boolean startedAuth;
    private BroadcastReceiver receiver;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final String tag = MainFragment.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_main, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int msgType = intent.getIntExtra("msgType", 0);
                if (msgType != 0) {
                    switch (msgType) {
                        case Messaging.AUTH_RETURNED:
                        case Messaging.AUTH_START:
                            if (startedAuth) {
                                trustButton.authenticate();
                                status.setText("Collecting data");
                                possumAuth.startListening();
                            }
                            break;
                        case Messaging.AUTH_TERMINATE:
                            trustButton.stopAuthenticate();
                            status.setText("Stopped auth");
                            possumAuth.stopListening();
                            break;
                        case Messaging.AUTH_VERIFY:
                            status.setText("Communicating with server");
                            trustButton.stopAuthenticate();
                            possumAuth.stopListening();
                            possumAuth.authenticate();
                            break;
                        case Messaging.READY_TO_AUTH:
                            status.setText("Ready to auth");
                            trustButton.setEnabled(true);
                            break;
                        default:
                    }
                } else {
                    status.setText(intent.getStringExtra("message"));
                }
            }
        };
        possumAuth = ((MainActivity) getActivity()).possumAuth();
        status = view.findViewById(R.id.status);
        iconWheel = view.findViewById(R.id.iconWheel);
        trustButton = view.findViewById(R.id.trustWheel);
        if (((MainActivity) getActivity()).validId(myId())) {
            status.setText("Ready for auth");
            trustButton.setEnabled(true);
        } else {
            status.setText("Need a user id");
            trustButton.setEnabled(false);
        }
        trustButton.setOnClickListener(v -> {
            if (possumAuth.hasMissingPermissions(getContext())) {
                possumAuth.requestNeededPermissions(getActivity());
            } else {
                if (((MainActivity) getActivity()).validId(myId())) {
                    startedAuth = !startedAuth;
                    if (!startedAuth) {
                        Send.message(getContext(), Messaging.AUTH_TERMINATE);
                    } else {
                        Send.message(getContext(), Messaging.AUTH_START);
                    }
                } else {
                    ((MainActivity) getActivity()).showInvalidIdDialog();
                }
            }
        });
        ViewPager viewPager = view.findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(3);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        adapter = new MyPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(0);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override
    public void newTrustScore(String graphName, float newScore) {
        if (startedAuth) {
            if (newScore >= 0) {
                if ("default".equals(graphName)) {
                    handler.post(() -> trustButton.setTrustScore(newScore * 100, null));
                }
                adapter.myPages.get(0).newTrustScore(graphName, newScore);
                adapter.myPages.get(1).newTrustScore(graphName, newScore);
            } else {
                startedAuth = false;
                handler.post(() -> trustButton.setTrustScore(0, "Failed"));
            }
        }
    }

    @Override
    public void detectorValues(String detectorName, String dataSetName, float score, float training) {
        if (startedAuth) {
            iconWheel.updateSensorTrainingStatus(PossumAuth.detectorTypeFromName(detectorName), training);
            adapter.myPages.get(0).detectorValues(detectorName, dataSetName, score, training);
            adapter.myPages.get(1).detectorValues(detectorName, dataSetName, score, training);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isRegistered) {
            getContext().getApplicationContext().registerReceiver(receiver, new IntentFilter("PossumMessage"));
            isRegistered = true;
        }
        for (AbstractDetector detector : possumAuth.detectors())
            iconWheel.detectorChanged(detector);
        for (int i = 0; i < adapter.getCount(); i++)
            possumAuth.addChangeListener(adapter.getItem(i));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRegistered) {
            getContext().getApplicationContext().unregisterReceiver(receiver);
            isRegistered = false;
        }
        trustButton.stopAuthenticate();
        for (int i = 0; i < adapter.getCount(); i++)
            possumAuth.removeChangeListener(adapter.getItem(i));
    }

    private String myId() {
        return ((MainActivity) getActivity()).myId();
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {
        private SparseArray<TrustFragment> myPages = new SparseArray<>();

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public TrustFragment getItem(int position) {
            if (myPages.get(position) == null) {
                Fragment fragment;
                switch (position) {
                    case 0:
                        fragment = TrustFragment.instantiate(getContext(), AllDetectorsChartFragment.class.getName());
                        break;
                    default:
                        fragment = TrustFragment.instantiate(getContext(), CombinedTrustChart.class.getName());
                }
                myPages.put(position, (TrustFragment) fragment);
            }
            return myPages.get(position);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}