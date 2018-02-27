package com.telenor.possumauth.example.fragments;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.telenor.possumauth.PossumAuth;
import com.telenor.possumauth.example.MainActivity;
import com.telenor.possumauth.example.R;
import com.telenor.possumauth.example.views.TrustButton;

public class MainFragment extends Fragment {
    private TrustButton trustButton;
    private TextView status;
/*    private long clientGatherStart;
    private long clientGatherEnd;
    private long clientSendStart;
    private long clientSendEnd;
    private long serverWaitStart;
    private long serverWaitEnd;*/
    private PossumAuth possumAuth;
    private static final boolean isGathering = false; // Set to false if it should do authentication
    private MyPagerAdapter adapter;
    private static final String tag = MainFragment.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle) {
        return inflater.inflate(R.layout.fragment_main, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        possumAuth = ((MainActivity)getActivity()).possumAuth();
//        AwesomePossum.addTrustListener(getContext(), this);
        status = view.findViewById(R.id.status);
//        AwesomePossum.addMessageListener(getContext(), this);
        trustButton = view.findViewById(R.id.trustWheel);
        trustButton.setOnClickListener(v -> {
            if (((MainActivity) getActivity()).validId(myId())) {
                /*if (AwesomePossum.isAuthorized(getActivity(), myId())) {
                    if (trustButton.isAuthenticating()) {
                        trustButton.stopAuthenticate();
                    } else {
                        clientGatherStart = System.currentTimeMillis();
                        trustButton.authenticate(myId());
                    }
                } else {
                    Log.i(tag, "Show authorize dialog");
//                        AwesomePossum.getAuthorizeDialog(getActivity(), myId(), getString(R.string.identityPoolId), "Authorize AwesomePossum", "We need permission from you", "Granted", "Denied").show();
                }*/
            } else {
                ((MainActivity) getActivity()).showInvalidIdDialog();
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
        updateStatus();
/*        if (!((MainActivity) getActivity()).validId(myId())) {
            Send.messageIntent(getContext(), Messaging.MISSING_VALID_ID, null);
        } else {
            Send.messageIntent(getContext(), Messaging.READY_TO_AUTH, null);
        }*/
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
//        AwesomePossum.sendDetectorStatus(getContext());
        for (int i=0; i < adapter.getCount(); i++) {
            Log.i(tag, "Adding "+adapter.getItem(i)+" to trust listening");
//            AwesomePossum.addTrustListener(getContext(), (TrustFragment)adapter.getItem(i));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        trustButton.stopAuthenticate();
        for (int i=0; i < adapter.getCount(); i++) {
//            AwesomePossum.removeTrustListener((TrustFragment)adapter.getItem(i));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        AwesomePossum.removeTrustListener(this);
//        AwesomePossum.removeMessageListener(this);
    }

    private void updateStatus() {
  /*      if (!((MainActivity) getActivity()).validId(myId())) {
            Send.messageIntent(getActivity(), Messaging.MISSING_VALID_ID, null);
            return;
        }
        Send.messageIntent(getContext(), Messaging.READY_TO_AUTH, null);*/
    }

    private String myId() {
        return ((MainActivity) getActivity()).myId();
    }


/*    @Override
    public void changeInCombinedTrust(final float combinedTrustScore, final String status, String graphName) {
        Do.onMain(new Runnable() {
            @Override
            public void run() {
                Log.d(tag, "Trustscore: Combined trust:"+combinedTrustScore+", Status:"+status);
                trustButton.setTrustScore(combinedTrustScore * 100, null);
            }
        });
    }

    @Override
    public void changeInDetectorTrust(int detectorType, float newTrustScore, String status, String graphName) {
    }

    @Override
    public void failedToAscertainTrust(Exception exception) {
        Log.e(tag, "Failed to ascertain trust:", exception);
        Do.onMain(new Runnable() {
            @Override
            public void run() {
                trustButton.setTrustScore(0, "Failed");
            }
        });
    }

    @Override
    public void possumMessageReceived(final String msgType, final String message) {
        Do.onMain(new Runnable() {
            @Override
            public void run() {
                switch (msgType) {
                    case Messaging.GATHERING:
                        status.setText(getContext().getString(R.string.gathering));
                        status.setTextColor(Color.BLACK);
                        trustButton.setEnabled(true);
                        break;
                    case Messaging.MISSING_VALID_ID:
                        status.setText(R.string.error_too_short_id);
                        status.setTextColor(Color.RED);
                        trustButton.setEnabled(false);
                        break;
                    case Messaging.AUTH_STOP:
                        status.setText(R.string.stopped_auth);
                        status.setTextColor(Color.BLACK);
                        trustButton.setEnabled(true);
                        trustButton.stopAuthenticate();
                        break;
                    case Messaging.AUTH_DONE:
                        serverWaitEnd = System.currentTimeMillis();
                        Log.d(tag, "TestAuth: Server response wait time: "+(serverWaitEnd-serverWaitStart)+" ms");
                        Log.d(tag, "TestAuth: Total roundTime:"+((clientGatherEnd-clientGatherStart)+(clientSendEnd-clientSendStart)+(serverWaitEnd-serverWaitStart))+" ms");
                        if (trustButton.isAuthenticating()) {
                            clientGatherStart = System.currentTimeMillis();
                            trustButton.authenticate(myId());
                        }
                        break;
                    case Messaging.AUTH_FAILED:
                        status.setText(String.format(Locale.US, "%s:%s", getString(R.string.authentication_failed), message));
                        status.setTextColor(Color.RED);
                        trustButton.setEnabled(true);
                        serverWaitEnd = System.currentTimeMillis();
                        Log.d(tag, "TestAuth: Server response wait time: "+(serverWaitEnd-serverWaitStart)+" ms");
                        Log.d(tag, "TestAuth: Total roundTime:"+((clientGatherEnd-clientGatherStart)+(clientSendEnd-clientSendStart)+(serverWaitEnd-serverWaitStart))+" ms");
                        break;
                    case Messaging.READY_TO_AUTH:
                        status.setText(R.string.all_ok);
                        status.setTextColor(Color.BLACK);
                        trustButton.setEnabled(true);
                        break;
                    case Messaging.POSSUM_MESSAGE:
                        break;
                    case Messaging.FACE_FOUND:
                        break;
                    case Messaging.SENDING_RESULT:
                        status.setText(getContext().getString(R.string.sending_result));
                        status.setTextColor(Color.BLACK);
                        trustButton.setEnabled(true);
                        break;
                    case Messaging.START_SERVER_DATA_SEND:
                        clientGatherEnd = System.currentTimeMillis();
                        Log.d(tag, "TestAuth: Time spent gathering data:"+(clientGatherEnd-clientGatherStart)+" ms");
                        clientSendStart = Long.parseLong(message);
                        status.setText(getString(R.string.sending_data_to_server));
                        status.setTextColor(Color.BLACK);
                        trustButton.setEnabled(true);
                        break;
                    case Messaging.VERIFICATION_SUCCESS:
                        status.setText(getContext().getString(R.string.verification_success));
                        status.setTextColor(Color.BLACK);
                        trustButton.setEnabled(true);
                        break;
                    case Messaging.WAITING_FOR_SERVER_RESPONSE:
                        clientSendEnd = System.currentTimeMillis();
                        status.setText(getString(R.string.waiting_for_server_response));
                        status.setTextColor(Color.BLACK);
                        serverWaitStart = Long.parseLong(message);
                        Log.d(tag, "TestAuth: Time spent processing and sending: "+(clientSendEnd-clientSendStart)+" ms");
                        trustButton.setEnabled(true);
                        break;
                    case Messaging.DETECTORS_STATUS:
                    case Messaging.POSSUM_PREVIEWS:
                    case Messaging.POSSUM_PREVIEWS_INVALID:
                    case Messaging.REQUEST_DETECTORS:
                        break;
                    default:
//                        status.setText(message);
//                        status.setTextColor(Color.RED);
//                        trustButton.setEnabled(false);
                        Log.e(tag, "Sending data: Unhandled possum message:" + msgType + ":" + message);
                }
            }
        });
    }

    @Override
    public void possumFaceFound(byte[] dataReceived) {
    }

    @Override
    public void possumImageSnapped(byte[] dataReceived) {

    }

    @Override
    public void possumFaceCoordsReceived(int[] xCoords, int[] yCoords) {
    }*/

    private class MyPagerAdapter extends FragmentPagerAdapter {
        private SparseArray<Fragment> myPages = new SparseArray<>();
        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (myPages.get(position) == null) {
                Fragment fragment;
                switch (position) {
                    case 0:
                        fragment = Fragment.instantiate(getContext(), AllSensorsChartFragment.class.getName());
                        break;
                    case 1:
                        fragment = Fragment.instantiate(getContext(), CameraFragment.class.getName());
                        break;
                    default:
                        fragment = Fragment.instantiate(getContext(), CombinedTrustChart.class.getName());
                }
                myPages.put(position, fragment);
            }
            return myPages.get(position);
        }

        @Override
        public int getCount() {
            return 3;
        }
    }
}