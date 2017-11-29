package majapp.bluetoothpaint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuBuilder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.regex.Pattern;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class MainActivity extends AppCompatActivity implements ColorPickerDialogListener {
    private static final int REQUEST_CONNECT_DEVICE = 10;
    private static final int STROKE_DIALOG_ID = 0;
    private static final int FILL_DIALOG_ID = 1;
    private static final float factor = 1/255.0f;
    final Context context = this;

    EditText fileNameEditText;
    private TextView errorMessageTextView;
    private DrawingView drawingView;

    private LinearLayout menuDrawingItems;
    private LinearLayout menuSettingsItems;
    private LinearLayout strokeWidthLinearLayout;

    private FloatingActionButton settingsActionButton;
    private FloatingActionButton toolsActionButton;
    private FloatingActionButton createPolygonActionButton;

    // right side menu
    private FloatingActionButton buttonPencil;
    private FloatingActionButton buttonLine;
    private FloatingActionButton buttonShape;
    private FloatingActionButton buttonCircle;
    private FloatingActionButton buttonPolygon;

    // left side menu
    private FloatingActionButton buttonFill;
    private FloatingActionButton buttonColor;
    private FloatingActionButton buttonWidth;

    // inner left side menu
    private ImageButton strokeWidth1Button;
    private ImageButton strokeWidth2Button;
    private ImageButton strokeWidth3Button;
    private ImageButton strokeWidth4Button;
    private ImageButton strokeWidth5Button;

    private boolean isFillableElement = false;
    private boolean isPolygon = false;
    private boolean isStrokeWidthButtonClicked = false;
    private boolean isSettingMenuOpened = false;
    private boolean isDrawingMenuOpened = false;
    private boolean isFullscreen = false;
    private int drawingItemIcon = R.drawable.custom_path;
    private ArrayList<String> savedElementInstance = null;

    private String rootDirectory;

    //Name of the connected device
    private String connectedDeviceName = null;
    private BluetoothAdapter bluetoothAdapter = null;
    public static BluetoothService btService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (savedInstanceState != null) {
            isFillableElement = savedInstanceState.getBoolean("isFillableElement");
            isStrokeWidthButtonClicked = savedInstanceState.getBoolean("isStrokeWidthButtonClicked");
            isDrawingMenuOpened = savedInstanceState.getBoolean("isDrawingMenuOpened");
            isSettingMenuOpened = savedInstanceState.getBoolean("isSettingMenuOpened");
            isPolygon = savedInstanceState.getBoolean("isPolygon");
            isFullscreen = savedInstanceState.getBoolean("isFullscreen");
            savedElementInstance = savedInstanceState.getStringArrayList("savedElementInstance");
            drawingItemIcon = savedInstanceState.getInt("drawingItemIcon");
        }

        InitializeSettings();
        InitializeViews();
        InitializeUserSettings();
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
    // region BT handler
    private String receivedXmlElement = "";
    @SuppressLint("HandlerLeak")
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
                            switch (receivedXmlElement) {
                                case Constants.UNDO:
                                    drawingView.Undo();
                                    break;
                                case Constants.REDO:
                                    drawingView.Redo();
                                    break;
                                default:
                                    drawingView.AddSvgElement(receivedXmlElement);
                                    drawingView.invalidate();
                                    break;
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
    // endregion

    @SuppressLint("RestrictedApi")
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

    //region toolsActionButtons
    public void toolsActionButton_Click(View view) {
        if (menuDrawingItems.getVisibility() == View.VISIBLE) {
            menuDrawingItems.setVisibility(View.GONE);
            isDrawingMenuOpened = false;
        }
        else {
            menuDrawingItems.setVisibility(View.VISIBLE);
            isDrawingMenuOpened = true;
        }
    }

    public void createPolygonActionButton_Click(View view){
        drawingView.CreatePolygon();
    }

    public void drawPathButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        isFillableElement = false;
        drawingItemIcon = R.drawable.custom_path;
        SetClickedButtonsBackground(buttonPencil);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.PATH);
        toolsActionButton.setImageResource(R.drawable.custom_path);
        createPolygonActionButton.setVisibility(View.GONE);
        menuDrawingItems.setVisibility(View.GONE);
        isDrawingMenuOpened = false;
        if (menuSettingsItems.getVisibility() == View.VISIBLE) {
            buttonFill.setVisibility(View.INVISIBLE);
        }

    }

    public void drawLineButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        isFillableElement = false;
        drawingItemIcon = R.drawable.custom_diagonal_line;
        SetClickedButtonsBackground(buttonLine);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.LINE);
        toolsActionButton.setImageResource(R.drawable.custom_diagonal_line);
        createPolygonActionButton.setVisibility(View.GONE);
        menuDrawingItems.setVisibility(View.GONE);
        isDrawingMenuOpened = false;
        if (menuSettingsItems.getVisibility() == View.VISIBLE) {
            buttonFill.setVisibility(View.INVISIBLE);
        }
    }

    public void drawRectangleButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        isFillableElement = true;
        drawingItemIcon = R.drawable.custom_rectangle;
        SetClickedButtonsBackground(buttonShape);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.RECTANGLE);
        toolsActionButton.setImageResource(R.drawable.custom_rectangle);
        createPolygonActionButton.setVisibility(View.GONE);
        menuDrawingItems.setVisibility(View.GONE);
        isDrawingMenuOpened = false;
        if (menuSettingsItems.getVisibility() == View.VISIBLE) {
            buttonFill.setVisibility(View.VISIBLE);
        }
    }

    public void drawCircleButton_Click(View view) {
        drawingView.ClearPolygonPointsList();
        isFillableElement = true;
        drawingItemIcon = R.drawable.custom_circle;
        SetClickedButtonsBackground(buttonCircle);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.CIRCLE);
        toolsActionButton.setImageResource(R.drawable.custom_circle);
        createPolygonActionButton.setVisibility(View.GONE);
        menuDrawingItems.setVisibility(View.GONE);
        isDrawingMenuOpened = false;
        if (menuSettingsItems.getVisibility() == View.VISIBLE) {
            buttonFill.setVisibility(View.VISIBLE);
        }
    }

    public void drawPolygonButton_Click(View view) {
        isFillableElement = true;
        drawingItemIcon = R.drawable.custom_polygon;
        SetClickedButtonsBackground(buttonPolygon);
        SettingsHolder.getInstance().getSettings().setShape(ShapesEnum.POLYGON);
        toolsActionButton.setImageResource(R.drawable.custom_polygon);
        createPolygonActionButton.setVisibility(View.VISIBLE);
        menuDrawingItems.setVisibility(View.GONE);
        isDrawingMenuOpened = false;
        if (menuSettingsItems.getVisibility() == View.VISIBLE) {
            buttonFill.setVisibility(View.VISIBLE);
        }
    }
    //endregion

    //region settingsActionButtons
    public void settingsActionButtons_Click(View view) {
        if (menuSettingsItems.getVisibility() == View.VISIBLE) {
            menuSettingsItems.setVisibility(View.GONE);
            isSettingMenuOpened = false;
            if (isStrokeWidthButtonClicked)
                strokeWidthLinearLayout.setVisibility(View.GONE);
        }
        else {
            menuSettingsItems.setVisibility(View.VISIBLE);
            isSettingMenuOpened = true;
            if (isFillableElement) {
                buttonFill.setVisibility(View.VISIBLE);
            }
            else {
                buttonFill.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void undoActionButton_Click(View view){
        drawingView.Undo();
    }

    public void redoActionButton_Click(View view){
        drawingView.Redo();
    }

    public void fullscreenActionButton_Click(View view) {
        switchFullscreen();
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
    }

    public void strokeWidthButton_Click(View view) {
        if(strokeWidthLinearLayout.getVisibility() == View.GONE){
            strokeWidthLinearLayout.setVisibility(View.VISIBLE);
            buttonWidth.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonClicked));
            isStrokeWidthButtonClicked = true;
        }
        else{
            strokeWidthLinearLayout.setVisibility(View.GONE);
            buttonWidth.setBackgroundColor(ContextCompat.getColor(this, R.color.colorDrawButtonNotClicked));
            isStrokeWidthButtonClicked = false;
        }
    }

    public void strokeWidth1Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth1Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(1.0f);
        strokeWidthLinearLayout.setVisibility(View.GONE);
    }
    public void strokeWidth2Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth2Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(3.0f);
        strokeWidthLinearLayout.setVisibility(View.GONE);
    }
    public void strokeWidth3Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth3Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(5.0f);
        strokeWidthLinearLayout.setVisibility(View.GONE);
    }
    public void strokeWidth4Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth4Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(7.0f);
        strokeWidthLinearLayout.setVisibility(View.GONE);
    }
    public void strokeWidth5Button_Click(View view){
        SetStrokeWidthButtonsBackground(strokeWidth5Button);
        SettingsHolder.getInstance().getSettings().setStrokeWidth(9.0f);
        strokeWidthLinearLayout.setVisibility(View.GONE);
    }
    //endregion

    // region initialization
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

        //Layouts
        menuDrawingItems = (LinearLayout) findViewById(R.id.menuDrawingItems);
        menuSettingsItems = (LinearLayout) findViewById(R.id.menuSettingsItems);
        strokeWidthLinearLayout = (LinearLayout) findViewById(R.id.strokeWidthLinearLayout);

        //Image buttons
        strokeWidth1Button= (ImageButton)findViewById(R.id.strokeWidth1Button);
        strokeWidth2Button= (ImageButton)findViewById(R.id.strokeWidth2Button);
        strokeWidth3Button= (ImageButton)findViewById(R.id.strokeWidth3Button);
        strokeWidth4Button= (ImageButton)findViewById(R.id.strokeWidth4Button);
        strokeWidth5Button= (ImageButton)findViewById(R.id.strokeWidth5Button);

        //Action buttons
        settingsActionButton = (FloatingActionButton) findViewById(R.id.settingsActionButton);
        toolsActionButton = (FloatingActionButton) findViewById(R.id.toolsActionButton);
        buttonPencil = (FloatingActionButton) findViewById(R.id.buttonPencil);
        buttonLine = (FloatingActionButton) findViewById(R.id.buttonLine);
        buttonShape = (FloatingActionButton) findViewById(R.id.buttonShape);
        buttonCircle = (FloatingActionButton) findViewById(R.id.buttonCircle);
        buttonPolygon = (FloatingActionButton) findViewById(R.id.buttonPolygon);
        createPolygonActionButton = (FloatingActionButton) findViewById(R.id.createPolygonActionButton);

        buttonFill = (FloatingActionButton) findViewById(R.id.buttonFill);
        buttonColor = (FloatingActionButton) findViewById(R.id.buttonColor);
        buttonWidth = (FloatingActionButton) findViewById(R.id.buttonWidth);

        createPolygonActionButton.setVisibility(View.GONE);
        strokeWidthLinearLayout.setVisibility(View.GONE);
        menuSettingsItems.setVisibility(View.GONE);
        menuDrawingItems.setVisibility(View.GONE);
        settingsActionButton.setVisibility(View.VISIBLE);
        toolsActionButton.setVisibility(View.VISIBLE);
        toolsActionButton.setImageResource(drawingItemIcon);
        //showSystemUI();
        if (isPolygon) createPolygonActionButton.setVisibility(View.VISIBLE);
        if (isSettingMenuOpened) menuSettingsItems.setVisibility(View.VISIBLE);
        if (isDrawingMenuOpened) menuDrawingItems.setVisibility(View.VISIBLE);
        if (isStrokeWidthButtonClicked && isSettingMenuOpened) strokeWidthLinearLayout.setVisibility(View.VISIBLE);
        if (isFullscreen) hideSystemUI();
        buttonColor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(SettingsHolder.getInstance().getSettings().getStrokeWithOpacity())));
        buttonFill.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(SettingsHolder.getInstance().getSettings().getFillWithOpacity())));
    }
    // endregion

    // region color settings
    private void SetClickedButtonsBackground(FloatingActionButton button) {
        buttonPencil.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        buttonLine.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        buttonShape.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        buttonCircle.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        buttonPolygon.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));

        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorActionButton));
    }

    private void SetStrokeWidthButtonsBackground(ImageButton button) {
        strokeWidth1Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth2Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth3Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth4Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));
        strokeWidth5Button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorDrawButtonNotClicked));

        button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorActionButton));
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
                buttonColor.setBackgroundTintList(ColorStateList.valueOf(color));
                setIconColor(buttonColor, hexColorWithOpacity, R.drawable.ic_custom_strokecolor_black, R.drawable.ic_custom_strokecolor_white);
                break;
            case FILL_DIALOG_ID:
                SettingsHolder.getInstance().getSettings().setFillWithOpacity(hexColorWithOpacity);
                SettingsHolder.getInstance().getSettings().setFill(hexColor);
                SettingsHolder.getInstance().getSettings().setFillOpacity(opacity);
                buttonFill.setBackgroundTintList(ColorStateList.valueOf(color));
                setIconColor(buttonFill, hexColorWithOpacity, R.drawable.ic_custom_fillcolor_black, R.drawable.ic_custom_fillcolor_white);
                break;
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }

    private void setIconColor(FloatingActionButton button, String hexColorWithOpacity, @DrawableRes int black, @DrawableRes int white) {
        // convert hex to RGB int values
        String tmp;
        if(hexColorWithOpacity.length() == 9) {
            tmp = hexColorWithOpacity.substring(3);
        }
        else if (hexColorWithOpacity.length() == 9) {
            tmp = hexColorWithOpacity.substring(2);
        }
        else {
            tmp = hexColorWithOpacity.substring(1);
        }

        int c_R = Integer.parseInt(tmp.substring(0,2), 16);
        int c_G = Integer.parseInt(tmp.substring(2,4), 16);
        int c_B = Integer.parseInt(tmp.substring(4,6), 16);

        // if lower then 128 black otherwise white
        if ((c_R + c_G + c_B)/3 > 128) {
            button.setImageResource(black);
        }
        else {
            button.setImageResource(white);
        }
    }
    // endregion

    // region menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_svg:
                NewSvgFile();
                return true;
            case R.id.save_svg:
                SaveSvgFile(false);
                return true;
            case R.id.open_svg:
                CheckedOpenSvgFile();
                return true;
            case R.id.open_bt_settings:
                Intent intent = new Intent(this, BTSettingsActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.exit:
                ExitApp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void NewSvgFile(){
        if ((savedElementInstance == null && !drawingView.GetSvgElements().isEmpty()) ||
                (savedElementInstance != null &&
                        !drawingView.GetSvgElements().isEmpty() &&
                        !savedElementInstance.equals(drawingView.GetSvgElements()))) {
            // Your changes will be lost if you don’t save them.
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    context);
            alertDialogBuilder.setMessage(R.string.lbl_saveOrDiscard);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton(R.string.btn_createNew, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    try {
                        drawingView.Restart();
                        drawingView.invalidate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            alertDialogBuilder.setNeutralButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //saveFileLinearLayout.setVisibility(View.GONE);
                    drawingView.setEnabled(true);
                    if (isStrokeWidthButtonClicked)
                        strokeWidthLinearLayout.setVisibility(View.VISIBLE);
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    SaveSvgFile(true);
                }
            });

            final AlertDialog dialog = alertDialogBuilder.create();
            dialog.show();
        }
        else {
            try {
                drawingView.Restart();
                drawingView.invalidate();
                savedElementInstance = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void SaveSvgFile(final boolean clean){
        // custom dialog
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_save, null);
        alertDialogBuilder.setTitle(R.string.title_saving);
        alertDialogBuilder.setMessage(R.string.lbl_fileName);
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton(R.string.btn_save,  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { }
        });
        alertDialogBuilder.setNegativeButton(R.string.btn_cancel,  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //saveFileLinearLayout.setVisibility(View.GONE);
                drawingView.setEnabled(true);
                if (isStrokeWidthButtonClicked)
                    strokeWidthLinearLayout.setVisibility(View.VISIBLE);
            }
        });

        // set the custom dialog components - editText and text
        fileNameEditText = (EditText)view.findViewById(R.id.fileNameEditText);
        errorMessageTextView = (TextView)view.findViewById(R.id.errorMessageTextView);
        fileNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (IsAlphanumeric(s.toString())) errorMessageTextView.setText("");
                else errorMessageTextView.setText(R.string.filename_error_message);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Send the positive button event back to the host activity
                String fileName = fileNameEditText.getText().toString();

                if(IsAlphanumeric(fileName)){
                    if(fileName.length() <= 0){
                        errorMessageTextView.setText(R.string.empty_filename);
                    }
                    else{
                        SaveSvgFile(fileName);
                        drawingView.setEnabled(true);
                        if (clean) {
                            drawingView.Restart();
                            drawingView.invalidate();
                            savedElementInstance = null;
                        }
                        else {
                            savedElementInstance = new ArrayList<>();
                            savedElementInstance.addAll(drawingView.GetSvgElements());
                        }
                        dialog.dismiss();
                    }
                }
                else{
                    errorMessageTextView.setText(R.string.filename_error_message);
                }
            }
        });
    }

    private void SaveSvgFile(String fileName){
        File mPath = new File(rootDirectory);
        fileName = mPath + "/" + fileName + ".svg";
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

    private void CheckedOpenSvgFile() {
        if ((savedElementInstance == null && !drawingView.GetSvgElements().isEmpty()) ||
                (savedElementInstance != null &&
                        !drawingView.GetSvgElements().isEmpty() &&
                        !savedElementInstance.equals(drawingView.GetSvgElements()))) {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    context);
            // Your changes will be lost if you don’t save them.
            alertDialogBuilder.setMessage(R.string.lbl_saveOrDiscard);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton(R.string.btn_open, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    try {
                        OpenSvgFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            alertDialogBuilder.setNeutralButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //saveFileLinearLayout.setVisibility(View.GONE);
                    drawingView.setEnabled(true);
                    if (isStrokeWidthButtonClicked)
                        strokeWidthLinearLayout.setVisibility(View.VISIBLE);
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    SaveSvgFile(true);
                }
            });

            final AlertDialog dialog = alertDialogBuilder.create();
            dialog.show();
        }
        else {
            try {
                OpenSvgFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                savedElementInstance = new ArrayList<>();
                savedElementInstance.addAll(drawingView.GetSvgElements());
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
                        .setPositiveButton(R.string.approval, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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

    private void ExitApp() {
        if ((savedElementInstance == null && !drawingView.GetSvgElements().isEmpty()) ||
                (savedElementInstance != null &&
                        !drawingView.GetSvgElements().isEmpty() &&
                        !savedElementInstance.equals(drawingView.GetSvgElements()))) {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    context);
            // Your changes will be lost if you don’t save them.
            alertDialogBuilder.setMessage(R.string.lbl_saveOrDiscard);
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton(R.string.btn_exit, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    try {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            alertDialogBuilder.setNeutralButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    //saveFileLinearLayout.setVisibility(View.GONE);
                    drawingView.setEnabled(true);
                    if (isStrokeWidthButtonClicked)
                        strokeWidthLinearLayout.setVisibility(View.VISIBLE);
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    SaveSvgFile(true);
                }
            });

            final AlertDialog dialog = alertDialogBuilder.create();
            dialog.show();
        }
        else {
            try {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String GetFileExtension(File file){
        String fileName = file.toString();
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
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
        createPolygonActionButton.setVisibility(View.GONE);
        strokeWidthLinearLayout.setVisibility(View.GONE);
        menuSettingsItems.setVisibility(View.GONE);
        menuDrawingItems.setVisibility(View.GONE);
        settingsActionButton.setVisibility(View.GONE);
        toolsActionButton.setVisibility(View.GONE);
    }

    private void TurnOnIWantToDrawMode(){
        InitializeViews();
        /*if(SettingsHolder.getInstance().getSettings().getShape() == ShapesEnum.POLYGON)
            createPolygonActionButton.setVisibility(View.VISIBLE);
        toolsActionButton.setVisibility(View.VISIBLE);*/
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
    // endregion

    //pomocne funkcie, aby som pri rotacii nestracal data a zvolene nastavenia
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isFillableElement", isFillableElement);
        outState.putBoolean("isStrokeWidthButtonClicked", isStrokeWidthButtonClicked);
        outState.putBoolean("isDrawingMenuOpened", isDrawingMenuOpened);
        outState.putBoolean("isSettingMenuOpened", isSettingMenuOpened);
        outState.putBoolean("isPolygon", isPolygon);
        outState.putBoolean("isFullscreen", isFullscreen);
        outState.putStringArrayList("savedElementInstance", savedElementInstance);
        outState.putInt("drawingItemIcon", drawingItemIcon);

        if(SettingsHolder.getInstance().getSettings() == null)
            return;
        SettingsHolder.getInstance().getSettings().setSvgElements(drawingView.GetSvgElements());
    }

    // region fullscreen
    private void switchFullscreen() {
        if (isFullscreen) {
            showSystemUI();
            isFullscreen = false;
        }
        else {
            hideSystemUI();
            isFullscreen = true;
        }
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }
    // endregion

    private void InitializeUserSettings(){
        if(SettingsHolder.getInstance().getSettings() == null)
            return;

        switch(SettingsHolder.getInstance().getSettings().getShape()){
            case PATH:
                SetClickedButtonsBackground(buttonPencil);
                break;
            case LINE:
                SetClickedButtonsBackground(buttonLine);
                break;
            case RECTANGLE:
                SetClickedButtonsBackground(buttonShape);
                break;
            case CIRCLE:
                SetClickedButtonsBackground(buttonCircle);
                break;
            case POLYGON:
                SetClickedButtonsBackground(buttonPolygon);
                createPolygonActionButton.setVisibility(View.VISIBLE);
                break;
        }

        if (isFillableElement) buttonFill.setVisibility(View.VISIBLE);
        else buttonFill.setVisibility(View.GONE);

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
