package majapp.bluetoothpaint;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

//https://developer.android.com/guide/topics/connectivity/bluetooth.html#HDP

public class BTSettingsActivity extends AppCompatActivity {
    // Tags for Bundle
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;

    //Views
    private Switch btSwitch;
    private LinearLayout modesLinearLayout;
    private LinearLayout listViewsLinearLayout;
    private Button scanButton;
    private RadioGroup radioGroup;
    private RadioButton drawOptionRadioButton;
    private RadioButton stareOptionRadioButton;

    //Other
    private BluetoothAdapter bluetoothAdapter = null;
    private ArrayAdapter<String> newDevicesArrayAdapter;
    private boolean isLoaded = false;

    public BTSettingsActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btsettings);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bt_notSupported), Toast.LENGTH_LONG).show();
            this.finish();
        }

        InitializeViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            //handle back button click
            case android.R.id.home:
                onBackPressed();
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
//        Intent intent = new Intent();
//        setResult(Constants.RESULT_CODE_BACK_PRESSED, intent);
//        finish();
    }

    private void StartBluetoothServie(){
        if (MainActivity.btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (MainActivity.btService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                if(bluetoothAdapter.isEnabled())
                    MainActivity.btService.start();
            }
        }
    }

    private void InitializeViews(){
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        scanButton = (Button)findViewById(R.id.scanButton);
        modesLinearLayout = (LinearLayout)findViewById(R.id.modesLinearLayout);
        modesLinearLayout.setVisibility(View.GONE);
        listViewsLinearLayout = (LinearLayout)findViewById(R.id.listViewsLinearLayout);
        listViewsLinearLayout.setVisibility(View.GONE);


        drawOptionRadioButton = (RadioButton) findViewById(R.id.drawOptionRadioButton);
        stareOptionRadioButton = (RadioButton) findViewById(R.id.stareOptionRadioButton);
        radioGroup = (RadioGroup)findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if(isLoaded){
                    SettingsHolder.getInstance().getSettings().setSendData(checkedId == R.id.drawOptionRadioButton);
                }
            }
        });
        if(SettingsHolder.getInstance().getSettings().getSendData()){
            radioGroup.check(R.id.drawOptionRadioButton);
        }
        else{
            radioGroup.check(R.id.stareOptionRadioButton);
        }

        btSwitch = (Switch)findViewById(R.id.btSwitch);
        btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isLoaded) {
                    if (isChecked) {
                        EnsureDiscoverable();
                        EnableBtUIElements();
                        InitializeDevicesLists();
                        SettingsHolder.getInstance().getSettings().setIsTurnedOn(true);
                        StartBluetoothServie();
                    } else {
                        DisableBtUIElements();
                        SettingsHolder.getInstance().getSettings().setIsTurnedOn(false);
                        if (MainActivity.btService != null) {
                            MainActivity.btService.stop();
                        }
                    }
                }
            }
        });
        btSwitch.setChecked(SettingsHolder.getInstance().getSettings().getIsTurnedOn());
        if(SettingsHolder.getInstance().getSettings().getIsTurnedOn()) {
            EnableBtUIElements();
            InitializeDevicesLists();
        }

        isLoaded = true;
    }

    //Makes this device discoverable for 120 seconds (2 minutes).
    private void EnsureDiscoverable() {
        if (bluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            this.startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
        }
    }

    private void EnableBtUIElements(){
        modesLinearLayout.setVisibility(View.VISIBLE);
        listViewsLinearLayout.setVisibility(View.VISIBLE);
        scanButton.setEnabled(true);
    }

    private void DisableBtUIElements(){
        modesLinearLayout.setVisibility(View.GONE);
        listViewsLinearLayout.setVisibility(View.GONE);
        scanButton.setEnabled(false);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_CANCELED) {
                    btSwitch.setChecked(false);
                }
                else {
                    StartBluetoothServie();
                }
                break;
        }
    }

    private void InitializeDevicesLists(){
        // Initialize array adapters. One for already paired devices...
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<>(this, R.layout.device_list_item);

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.pairedDevicesTextBox).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.bt_noPairedDevices).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }

        // Set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.pairedDevicesListView);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(listViewItemClickListener);

        //... and one for newly discovered devices
        newDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_list_item);
        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.newDevicesListView);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(listViewItemClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener listViewItemClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            bluetoothAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (newDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.bt_noNewDevices).toString();
                    newDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }

        try{
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);}
        catch(IllegalArgumentException e){
            Log.e("UNREGISTER_RECEIVER", e.toString());
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void DoDiscovery() {
        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();
    }

    public void scanButton_Click(View view){
        DoDiscovery();
    }
}
