package com.gjermundbjaanes.beaconmqtt.newbeacon;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.gjermundbjaanes.beaconmqtt.R;
import com.gjermundbjaanes.beaconmqtt.beacondb.BeaconPersistence;
import com.gjermundbjaanes.beaconmqtt.beacondb.BeaconResult;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewBeaconActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String TAG = NewBeaconActivity.class.getName();
    private static final String REGION_ID_FOR_RANGING = "myRangingUniqueId";

    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);

    private ListView beaconSearchListView;
    private List<BeaconResult> persistedBeaconList = new ArrayList<>();
    private BeaconListAdapter beaconListAdapter;
    private BeaconPersistence beaconPersistence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_beacon);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }


        beaconSearchListView = (ListView) findViewById(R.id.beacon_search_list);

        beaconListAdapter = new BeaconListAdapter(this);
        beaconSearchListView.setAdapter(beaconListAdapter);

        beaconPersistence = new BeaconPersistence(this);
        persistedBeaconList = beaconPersistence.getBeacons();

        beaconSearchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(NewBeaconActivity.this);
                LayoutInflater inflater = NewBeaconActivity.this.getLayoutInflater();
                final View dialogLayout = inflater.inflate(R.layout.dialog_new_beacon, null);
                builder.setView(dialogLayout)
                        .setPositiveButton(R.string.dialog_save_beacon, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TextView newBeaconNameTextView = (TextView) dialogLayout.findViewById(R.id.dailog_new_beacon_name);
                                String informalBeaconName = newBeaconNameTextView.getText().toString();
                                BeaconListElement beaconListElement = (BeaconListElement) beaconSearchListView.getItemAtPosition(position);

                                beaconPersistence.saveBeacon(beaconListElement.getBeacon(), informalBeaconName);
                                persistedBeaconList = beaconPersistence.getBeacons();
                            }
                        })
                        .setNegativeButton(R.string.dialog_cancel_beacon, null)
                        .show();



            }
        });

        beaconManager.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager.isBound(this)) beaconManager.setBackgroundMode(false);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                final List<BeaconListElement> beaconsList = new ArrayList<>();

                for (Beacon beacon : beacons) {
                    beaconsList.add(beaconToBeaconListElement(beacon));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        beaconListAdapter.updateBeacons(beaconsList);
                    }
                });
            }

            private BeaconListElement beaconToBeaconListElement(Beacon beacon) {
                BeaconListElement beaconListElement = new BeaconListElement(beacon);

                if (beaconSaved(beacon)) {
                    beaconListElement.setSaved(true);
                }

                return beaconListElement;
            }

            private boolean beaconSaved(Beacon beacon) {
                for (BeaconResult beaconResult : persistedBeaconList) {
                    if (beacon.getId1().toString().equals(beaconResult.getUuid()) &&
                            beacon.getId2().toString().equals(beaconResult.getMajor()) &&
                            beacon.getId3().toString().equals(beaconResult.getMinor())) {
                        return true;
                    }
                }

                return false;
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region(REGION_ID_FOR_RANGING, null, null, null));
        } catch (RemoteException e) {
            String errorMessage = "Not able to start ranging beacons";
            Log.e(TAG, errorMessage, e);
            Snackbar.make(this.beaconSearchListView, errorMessage, Snackbar.LENGTH_LONG).show();
        }
    }

}
