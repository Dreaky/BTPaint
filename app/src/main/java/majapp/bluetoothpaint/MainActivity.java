package majapp.bluetoothpaint;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

//https://github.com/jaredrummler/ColorPicker
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements ColorPickerDialogListener {
    private static final int REQUEST_CONNECT_DEVICE = 10;
    private static final int STROKE_DIALOG_ID = 0;
    private static final int FILL_DIALOG_ID = 1;
    private static final float factor = 1/255.0f;
    private EditText fileNameEditText;
    private TextView errorMessageTextView;
    private DrawingView drawingView;
    private LinearLayout sidePanelLinearLayout;
    private LinearLayout upPanelLinearLayout;
    private LinearLayout strokeWidthLinearLayout;
    private LinearLayout saveFileLinearLayout;
    private RelativeLayout canvasLayout;
    private FloatingActionButton toolsActionButton;
    private FloatingActionButton undoActionButton;
    private FloatingActionButton createPolygonActionButton;
    private FloatingActionButton confirmSavingActionButton;
    private FloatingActionButton cancelSavingActionButton;
    private ImageButton drawLineButton;
    private ImageButton drawRectangleButton;
    private ImageButton drawCircleButton;
    private ImageButton drawPathButton;
    private ImageButton drawPolygonButton;
    private ImageButton strokeColorButton;
    private ImageButton fillColorButton;
    private ImageButton strokeWidthButton;
    private ImageButton strokeWidth1Button;
    private ImageButton strokeWidth2Button;
    private ImageButton strokeWidth3Button;
    private ImageButton strokeWidth4Button;
    private ImageButton strokeWidth5Button;
    private boolean isStrokeWidthButtonClicked = false;
    private boolean isToolsActionButtonClicked = true;
    private String rootDirectory;
    private String selectedDirectory = "";

    //Name of the connected device
    private String connectedDeviceName = null;
     //String buffer for outgoing messages
    private StringBuffer outStringBuffer;
    private BluetoothAdapter bluetoothAdapter = null;
    public static BluetoothService btService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        InitializeSettings();
        InitializeViews();
        InitializeUserSettings();

        fileNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(IsAlphanumeric(s.toString())){
                    errorMessageTextView.setText("");
                }
                else{
                    errorMessageTextView.setText(R.string.filename_error_message);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        if (btService == null) {
            SetupService();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btService != null) {
            btService.stop();
        }
    }

//    public static void TmpResume(){
//        if (btService == null) {
//            SetupService();
//        }
//
//        if (btService != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (btService.getState() == BluetoothService.STATE_NONE) {
//                // Start the Bluetooth chat services
//                btService.start();
//            }
//        }
//    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when REQUEST_CONNECT_DEVICE activity returns.
        if (btService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (btService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth services
                if(SettingsHolder.getInstance().getSettings().getIsTurnedOn())
                    btService.start();
            }
        }
    }

    private void SetupService(){
        // Initialize the BluetoothService to perform bluetooth connections
        if (btService == null)
            btService = new BluetoothService(this, mHandler);
        drawingView.SetBluetoothService(btService);
    }

    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getSupportActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    private String receivedXmlElement = "";
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, connectedDeviceName));
//                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(getString(R.string.title_connecting));
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(getString(R.string.title_not_connected));
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    break;
                case Constants.MESSAGE_READ:
                    if(!SettingsHolder.getInstance().getSettings().getSendData()){
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        if(!readMessage.endsWith("/>"))
                            receivedXmlElement += readMessage;
                        else{
                            receivedXmlElement += readMessage;
                            if(receivedXmlElement.equals(Constants.UNDO))
                                drawingView.Undo();
                            else{
                                drawingView.AddSvgElement(receivedXmlElement);
                                drawingView.invalidate();
                            }
                            receivedXmlElement = "";
                        }
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                        Toast.makeText(MainActivity.this, "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                        Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        return true;
    }

    public void toolsActionButton_Click(View view) {
        if(sidePanelLinearLayout.getVisibility() == View.VISIBLE) {
            isToolsActionButtonClicked = false;
            sidePanelLinearLayout.setVisibility(View.GONE);
            upPanelLinearLayout.setVisibility(View.GONE);
            strokeWidthLinearLayout.setVisibility(View.GONE);
        }
        else{
            isToolsActionButtonClicked = true;
            sidePanelLinearLayout.setVisibility(View.VISIBLE);
            upPanelLinearLayout.setVisibility(View.VISIBLE);
            if(isStrokeWidthButtonClicked)
                strokeWidthLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    public void undoActionButton_Click(View view){
        drawingView.Undo();
    }

    public void createPolygonActionButton_Click(View view){
        drawingView.CreatePolygon();
    }

    public void confirmSavingActionButton_Click(View view){
        String fileName = fileNameEditText.getText().toString();

        if(IsAlphanumeric(fileName)){
            if(fileName.length() <= 0){
                errorMessageTextView.setText(R.string.empty_filename);
            }
            else{
                SaveSvgFile(fileName);
                saveFileLinearLayout.setVisibility(View.GONE);
                drawingView.setEnabled(true);
                if(isToolsActionButtonClicked){
                    sidePanelLinearLayout.setVisibility(View.VISIBLE);
                    upPanelLinearLayout.setVisibility(View.VISIBLE);
                    if(isStrokeWidthButtonClicked)
                        strokeWidthLinearLayout.setVisibility(View.VISIBLE);
                }
            }
        }
        else{
            errorMessageTextView.setText(R.string.filename_error_message);
        }
    }

    public void cancelSavingActionButton_Click(View view){
        selectedDirectory = "";
        saveFileLinearLayout.setVisibility(View.GONE);
        drawingView.setEnabled(true);
        if(isToolsActionButtonClicked){
            sidePanelLinearLayout.setVisibility(View.VISIBLE);
            upPanelLinearLayout.setVisibility(View.VISIBLE);
            if(isStrokeWidthButtonClicked)
                strokeWidthLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    public void drawPathButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        SetButtonsBackground(drawPathButton);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.PATH);
        toolsActionButton.setImageResource(R.drawable.custom_path);
        createPolygonActionButton.setVisibility(View.INVISIBLE);
    }

    public void drawLineButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        SetButtonsBackground(drawLineButton);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.LINE);
        toolsActionButton.setImageResource(R.drawable.custom_diagonal_line);
        createPolygonActionButton.setVisibility(View.INVISIBLE);
    }

    public void drawRectangleButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        SetButtonsBackground(drawRectangleButton);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.RECTAGLE);
        toolsActionButton.setImageResource(R.drawable.custom_rectangle);
        createPolygonActionButton.setVisibility(View.INVISIBLE);
    }

    public void drawCircleButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        SetButtonsBackground(drawCircleButton);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.CIRCLE);
        toolsActionButton.setImageResource(R.drawable.custom_circle);
        createPolygonActionButton.setVisibility(View.INVISIBLE);
    }

    public void drawPolygonButton_Click(View view) {
        SetButtonsBackground(drawPolygonButton);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.POLYGON);
        toolsActionButton.setImageResource(R.drawable.custom_polygon);
        createPolygonActionButton.setVisibility(View.VISIBLE);
    }

    public void strokeColorButton_Click(View view) {
        String hexColor = SettingsHolder.getInstance().getSettings().getStrokeWithOpacity();
        int color = Color.parseColor(hexColor);
        ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowPresets(false)
                .setDialogId(STROKE_DIALOG_ID)
                .setColor(color)
                .setShowAlphaSlider(true)
                .show(this);
    }

    public void fillColorButton_Click(View view) {
        String hexColor = SettingsHolder.getInstance().getSettings().getFillWithOpacity();
        int color = Color.parseColor(hexColor);
        ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowPresets(false)
                .setDialogId(FILL_DIALOG_ID)
                .setColor(color)
                .setShowAlphaSlider(true)
                .show(this);
        fillColorButton.setBackgroundColor(color);
    }

    public void strokeWidthButton_Click(View view) {
        if(strokeWidthLinearLayout.getVisibility() == View.GONE){
            strokeWidthLinearLayout.setVisibility(View.VISIBLE);
            strokeWidthButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonClicked));
            isStrokeWidthButtonClicked = true;
        }
        else{
            strokeWidthLinearLayout.setVisibility(View.GONE);
            strokeWidthButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonNotClicked));
            isStrokeWidthButtonClicked = false;
        }
    }

    public void strokeWidth1Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth1Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(1.0f);
    }
    public void strokeWidth2Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth2Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(3.0f);
    }
    public void strokeWidth3Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth3Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(5.0f);
    }
    public void strokeWidth4Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth4Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(7.0f);
    }
    public void strokeWidth5Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth5Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(9.0f);
    }

    private void InitializeSettings() {
        rootDirectory = this.getFilesDir() + "//SvgFiles//";
        if(SettingsHolder.getInstance().getSettings() == null){
            Settings settings = new Settings();
            SettingsHolder.getInstance().setSettings(settings);
        }
    }

    private void InitializeViews()
    {
        drawingView = (DrawingView)findViewById(R.id.drawingView);
        fileNameEditText = (EditText)findViewById(R.id.fileNameEditText);
        errorMessageTextView = (TextView)findViewById(R.id.errorMessageTextView);

        //Layouts
        sidePanelLinearLayout = (LinearLayout) findViewById(R.id.sidePanelLinearLayout);
        upPanelLinearLayout = (LinearLayout) findViewById(R.id.upPanelLinearLayout);
        strokeWidthLinearLayout = (LinearLayout) findViewById(R.id.strokeWidthLinearLayout);
        saveFileLinearLayout = (LinearLayout) findViewById(R.id.saveFileLinearLayout);
        canvasLayout = (RelativeLayout) findViewById(R.id.canvasLayout);

        //Image buttons
        drawLineButton = (ImageButton)findViewById(R.id.drawLineButton);
        drawRectangleButton = (ImageButton)findViewById(R.id.drawRectangleButton);
        drawCircleButton = (ImageButton)findViewById(R.id.drawCircleButton);
        drawPathButton = (ImageButton)findViewById(R.id.drawPathButton);
        drawPolygonButton= (ImageButton)findViewById(R.id.drawPolygonButton);
        strokeColorButton= (ImageButton)findViewById(R.id.strokeColorButton);
        strokeWidthButton= (ImageButton)findViewById(R.id.strokeWidthButton);
        fillColorButton= (ImageButton)findViewById(R.id.fillColorButton);
        strokeWidth1Button= (ImageButton)findViewById(R.id.strokeWidth1Button);
        strokeWidth2Button= (ImageButton)findViewById(R.id.strokeWidth2Button);
        strokeWidth3Button= (ImageButton)findViewById(R.id.strokeWidth3Button);
        strokeWidth4Button= (ImageButton)findViewById(R.id.strokeWidth4Button);
        strokeWidth5Button= (ImageButton)findViewById(R.id.strokeWidth5Button);

        //Action buttons
        toolsActionButton = (FloatingActionButton) findViewById(R.id.toolsActionButton);
        createPolygonActionButton = (FloatingActionButton) findViewById(R.id.createPolygonActionButton);
        undoActionButton = (FloatingActionButton) findViewById(R.id.undoActionButton);
        confirmSavingActionButton = (FloatingActionButton) findViewById(R.id.confirmSavingActionButton);
        cancelSavingActionButton = (FloatingActionButton) findViewById(R.id.cancelSavingActionButton);

        createPolygonActionButton.setVisibility(View.INVISIBLE);
        strokeWidthLinearLayout.setVisibility(View.GONE);
        saveFileLinearLayout.setVisibility(View.GONE);
        strokeColorButton.setBackgroundColor(Color.parseColor(SettingsHolder.getInstance().getSettings().getStrokeWithOpacity()));
        fillColorButton.setBackgroundColor(Color.parseColor(SettingsHolder.getInstance().getSettings().getFillWithOpacity()));
    }

    private void SetButtonsBackground(ImageButton button) {
        //drawCircleButton.setBackgroundColor(getResources().getColor(R.color.colorDrawButtonNotClicked)); -> deprecated
        drawLineButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonNotClicked));
        drawRectangleButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonNotClicked));
        drawCircleButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonNotClicked));
        drawPathButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonNotClicked));
        drawPolygonButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonNotClicked));

        button.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonClicked));
    }

    private void SetStrokeWidthButtonsBackground(ImageButton button) {
        strokeWidth1Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth2Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth3Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth4Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth5Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));

        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonClicked));
    }

    //override methods for custom color picker

    @Override
    public void onColorSelected(int dialogId, int color) {
        String hexColorWithOpacity = "#" + Integer.toHexString(color);
        if(hexColorWithOpacity.length() == 8)
            hexColorWithOpacity = hexColorWithOpacity.replace("#", "#0");
        else if(hexColorWithOpacity.length() == 7)
            hexColorWithOpacity = hexColorWithOpacity.replace("#", "#00");
        String hexColor = "#" + hexColorWithOpacity.substring(3, hexColorWithOpacity.length());
        float opacity = Color.alpha(color) * factor;
        switch (dialogId) {
            case STROKE_DIALOG_ID:
                SettingsHolder.getInstance().getSettings().setStrokeWithOpacity(hexColorWithOpacity);
                SettingsHolder.getInstance().getSettings().setStroke(hexColor);
                SettingsHolder.getInstance().getSettings().setStrokeOpacity(opacity);
                strokeColorButton.setBackgroundColor(color);
                break;
            case FILL_DIALOG_ID:
                SettingsHolder.getInstance().getSettings().setFillWithOpacity(hexColorWithOpacity);
                SettingsHolder.getInstance().getSettings().setFill(hexColor);
                SettingsHolder.getInstance().getSettings().setFillOpacity(opacity);
                fillColorButton.setBackgroundColor(color);
                break;
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.save_svg:
                SaveSvgFile();
                return true;
            case R.id.open_svg:
                OpenSvgFile();
                return true;
            case R.id.open_bt_settings:
                Intent intent = new Intent(this, BTSettingsActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void SaveSvgFile(){
        File mPath = new File(rootDirectory);
        FileDialog fileDialog = new FileDialog(this, mPath, ".sss");
        fileDialog.setSelectDirectoryOption(true);
        fileDialog.addDirectoryListener(new FileDialog.DirectorySelectedListener() {
            public void directorySelected(File directory) {
                if(isToolsActionButtonClicked){
                    sidePanelLinearLayout.setVisibility(View.GONE);
                    upPanelLinearLayout.setVisibility(View.GONE);
                }
                if(isStrokeWidthButtonClicked)
                    strokeWidthLinearLayout.setVisibility(View.GONE);
                saveFileLinearLayout.setVisibility(View.VISIBLE);
                drawingView.setEnabled(false);
                selectedDirectory = directory.toString();
                Log.d(getClass().getName(), "selected dir " + directory.toString());
            }
        });
        fileDialog.showDialog();
    }

    private void SaveSvgFile(String fileName){
        fileName = selectedDirectory + "/" + fileName + ".svg";
        String content = drawingView.GetSvgString();
        FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(new File(fileName), false);
            outputStream.write(content.getBytes());
            outputStream.close();

            Toast.makeText(this, R.string.saving_successful, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.saving_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void OpenSvgFile(){
        File mPath = new File(rootDirectory);
        FileDialog fileDialog = new FileDialog(this, mPath);
        fileDialog.setSelectDirectoryOption(false);
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                Log.d(getClass().getName(), "selected file " + file.toString());
                OpenSvgFile(file);
            }
        });
        fileDialog.showDialog();
    }

    private void OpenSvgFile(File file){
        if(file.exists())
        {
            String extension = GetFileExtension(file);
            if(!extension.equals(".svg".toLowerCase())){
                new AlertDialog.Builder(this)
                        .setTitle(R.string.error)
                        .setMessage(R.string.unsupported_file_format)
                        .setCancelable(false)
                        .setPositiveButton("beriem na vedomie", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        }).show();
            }
            else{
                try {
                    drawingView.Restart();
                    FileInputStream inputStream = new FileInputStream(file);
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if(line.startsWith("<path") || line.startsWith("<line") || line.startsWith("<rect") ||
                                line.startsWith("<circle") || line.startsWith("<polygon")) {
                            drawingView.AddSvgElement(line);
                        }
                    }
                    inputStream.close();
                    drawingView.invalidate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String GetFileExtension(File file){
        String fileName = file.toString();
        String result = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        return result;
    }

    private boolean IsAlphanumeric(String string){
        Pattern p = Pattern.compile("[^a-zA-Z0-9]");
        return !p.matcher(string).find();
    }

    private void TurnOnBTMode()
    {
        if(
                SettingsHolder.getInstance().getSettings().getIsTurnedOn() &&
                        !SettingsHolder.getInstance().getSettings().getSendData()
                )
            TurnOnIWantToStareMode();
        else
            TurnOnIWantToDrawMode();
    }

    private void TurnOnIWantToStareMode(){
        sidePanelLinearLayout.setVisibility(View.GONE);
        upPanelLinearLayout.setVisibility(View.GONE);
        undoActionButton.setVisibility(View.GONE);
        if(SettingsHolder.getInstance().getSettings().getShape() == ShapesEnum.POLYGON)
            createPolygonActionButton.setVisibility(View.GONE);
        toolsActionButton.setVisibility(View.GONE);
    }

    private void TurnOnIWantToDrawMode(){
        sidePanelLinearLayout.setVisibility(View.VISIBLE);
        upPanelLinearLayout.setVisibility(View.VISIBLE);
        undoActionButton.setVisibility(View.VISIBLE);
        if(SettingsHolder.getInstance().getSettings().getShape() == ShapesEnum.POLYGON)
            createPolygonActionButton.setVisibility(View.VISIBLE);
        toolsActionButton.setVisibility(View.VISIBLE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When BTSettingsActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    SetupService();
                    connectDevice(data, true);
                    TurnOnBTMode();
                }
                else{
                    TurnOnBTMode();
                }
                break;
        }
    }

    //Establish connection with other device
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(BTSettingsActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        btService.connect(device, secure);
    }

    //pomocne funkcie, aby som pri rotacii nestracal data a zvolene nastavenia
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(SettingsHolder.getInstance().getSettings() == null)
            return;
        SettingsHolder.getInstance().getSettings().setSvgElements(drawingView.GetSvgElements());
    }

    private void InitializeUserSettings(){
        if(SettingsHolder.getInstance().getSettings() == null)
            return;

        switch(SettingsHolder.getInstance().getSettings().getShape()){
            case PATH:
                SetButtonsBackground(drawPathButton);
                break;
            case LINE:
                SetButtonsBackground(drawLineButton);
                break;
            case RECTAGLE:
                SetButtonsBackground(drawRectangleButton);
                break;
            case CIRCLE:
                SetButtonsBackground(drawCircleButton);
                break;
            case POLYGON:
                SetButtonsBackground(drawPolygonButton);
                createPolygonActionButton.setVisibility(View.VISIBLE);
                break;
        }

        float strokeWidth = SettingsHolder.getInstance().getSettings().getStrokeWidth();
        if(strokeWidth == 1.0f)
            SetStrokeWidthButtonsBackground(strokeWidth1Button);
        else if(strokeWidth == 3.0f)
            SetStrokeWidthButtonsBackground(strokeWidth2Button);
        else if(strokeWidth == 5.0f)
            SetStrokeWidthButtonsBackground(strokeWidth3Button);
        else if(strokeWidth == 7.0f)
            SetStrokeWidthButtonsBackground(strokeWidth4Button);
        else if(strokeWidth == 9.0f)
            SetStrokeWidthButtonsBackground(strokeWidth5Button);


        drawingView.SetSvgElements(SettingsHolder.getInstance().getSettings().getSvgElements());
    }
}
