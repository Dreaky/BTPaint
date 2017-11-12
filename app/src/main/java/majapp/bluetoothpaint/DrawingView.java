package majapp.bluetoothpaint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import java.util.ArrayList;
import java.util.List;

// -----
// Source: https://code.tutsplus.com/tutorials/android-sdk-create-a-drawing-app-interface-creation--mobile-19021
//         https://code.tutsplus.com/tutorials/android-sdk-create-a-drawing-app-touch-interaction--mobile-19202
//         https://code.google.com/archive/p/svg-android/wikis/Tutorial.wiki
// -----

public class DrawingView extends View
{
    private float startX;
    private float startY;
    private float endX;
    private float endY;
    private String points;
    private List<String> svgElements;
    private List<PointF> polygonPoints;
    private Paint drawPaint;
    private int paintColor;
    private BluetoothService btService = null;


    private String svgHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?><!-- Generator: Adobe Illustrator 13.0.0, SVG Export Plug-In . SVG Version: 6.00 Build 14948)  --><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg version=\"1.1\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\"\t width=\"0px\" height=\"0px\" viewBox=\"0 0 0 0\" enable-background=\"new 0 0 0 0\" xml:space=\"preserve\"><g id=\"android\">";
    private String svgElement = "";
    private String svgFoot = "\n</g></svg>";

    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        setupDrawing();
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    private void setupDrawing(){
        //https://stackoverflow.com/questions/10384613/android-canvas-drawpicture-not-working-in-devices-with-ice-cream-sandwich
        if(android.os.Build.VERSION.SDK_INT <= 22)
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        svgElements = new ArrayList<String>();
        polygonPoints = new ArrayList<PointF>();
        drawPaint = new Paint();
        paintColor = 0xFF000000;
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(5);
        drawPaint.setStyle(Paint.Style.FILL);
        drawPaint.setColor(paintColor);

    }

    public void SetBluetoothService(BluetoothService service){
        btService = service;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFFFFF);

        // Parse the SVG file from the resource
        String svgBody = "";
        for (String element: svgElements) {
            if(element.endsWith("/>"))
                svgBody += "\n" + element;
            else
                Log.e("CHYBNY ELEMENT:", "CHYBNY ELEMENT: " + element);
        }

        Log.e("SVGBODY", svgHeader + svgBody + svgElement + svgFoot);

        SVG svg;
        try {
            svg = SVGParser.getSVGFromString(svgHeader + svgBody + svgElement + svgFoot);
            // Get the picture
            Picture picture = svg.getPicture();
            // Draw picture in canvas
            // Note: use transforms such as translate, scale and rotate to position the picture correctly
            canvas.drawPicture(picture);

            DrawPolygonPoints(canvas);
        }
        catch(Exception ex) {
            Log.e("CHYBA", "svgString :-> " + svgHeader + svgBody + svgElement + svgFoot);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isEnabled())
            return true;

        if(SettingsHolder.getInstance().getSettings().getShape() != ShapesEnum.POLYGON) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    points = "M" + startX + "," + startY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    endX = event.getX();
                    endY = event.getY();
                    points += "L" + endX + "," + endY;
                    UpdateSvgBody();
                    break;
                case MotionEvent.ACTION_UP:
                    AddSvgElement();
                    break;
                default:
                    return false;
            }
        }
        // zadavanie bodov polygonu
        else{
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getX();
                    startY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    polygonPoints.add(new PointF(startX, startY));
                    break;
                default:
                    return false;
            }
        }

        //calls the onDraw method
        invalidate();
        return true;
    }

    private void UpdateSvgBody() {
        switch(SettingsHolder.getInstance().getSettings().getShape()) {
            case LINE:
                CreateLine();
                break;
            case CIRCLE:
                CreateCircle();
                break;
            case RECTAGLE:
                CreateRectangle();
                break;
            case PATH:
                CreatePath();
                break;
            case POLYGON:
                CreatePolygon();
                break;
        }
    }

    private void CreatePath(){
        svgElement = "<path d=\"" + points + "\"";
        AddStroke();
        AddStrokeWidth();
        AddStrokeOpacity();
        AddEndTag();
    }

    private void CreateLine(){
        svgElement = "<line x1=\"" + startX + "\" y1=\"" + startY + "\" x2=\"" + endX +"\" y2=\"" + endY + "\" ";
        AddStroke();
        AddStrokeWidth();
        AddStrokeOpacity();
        AddEndTag();
    }

    private void CreateCircle() {
        svgElement = "<circle cx=\"" + startX + "\" cy=\"" + startY + "\" r=\"" + GetDistance() + "\"";
        AddStroke();
        AddStrokeWidth();
        AddStrokeOpacity();
        AddFill();
        AddFillOpacity();
        AddEndTag();
    }

    private void CreateRectangle() {
        if(startX <= endX){
            if(startY <= endY) {
                svgElement = "<rect x=\"" + startX + "\" y=\"" + startY + "\"";
                AddWidth(Math.abs(startX - endX));
                AddHeight(Math.abs(startY - endY));
                AddStroke();
                AddStrokeWidth();
                AddStrokeOpacity();
                AddFill();
                AddFillOpacity();
                AddEndTag();
            }
            else{
                svgElement = "<rect x=\"" + startX + "\" y=\"" + endY + "\"";
                AddWidth(Math.abs(startX - endX));
                AddHeight(Math.abs(startY - endY));
                AddStroke();
                AddStrokeWidth();
                AddStrokeOpacity();
                AddFill();
                AddFillOpacity();
                AddEndTag();
            }
        }
        else{
            if(startY <= endY) {
                svgElement = "<rect x=\"" + endX + "\" y=\"" + startY + "\"";
                AddWidth(Math.abs(startX - endX));
                AddHeight(Math.abs(startY - endY));
                AddStroke();
                AddStrokeWidth();
                AddStrokeOpacity();
                AddFill();
                AddFillOpacity();
                AddEndTag();
            }
            else{
                svgElement = "<rect x=\"" + endX + "\" y=\"" + endY + "\"";
                AddWidth(Math.abs(startX - endX));
                AddHeight(Math.abs(startY - endY));
                AddStroke();
                AddStrokeWidth();
                AddStrokeOpacity();
                AddFill();
                AddFillOpacity();
                AddEndTag();
            }
        }
    }

    public void CreatePolygon(){
        if(polygonPoints.size() <= 0)
            return;

        points = "";
        for (PointF point:polygonPoints) {
            points += point.x + "," + point.y + " ";
        }

        svgElement = "<polygon points=\"" + points + "\"";
        AddStroke();
        AddStrokeWidth();
        AddStrokeOpacity();
        AddFill();
        AddFillOpacity();
        AddEndTag();

        AddSvgElement();

        polygonPoints.clear();
        invalidate();
    }

    public void ClearPolygonPointsList(){
        polygonPoints.clear();
        invalidate();
    }

    public void Undo(){
        if(polygonPoints.size() > 0) {
            polygonPoints.remove(polygonPoints.size() - 1);
        }
        else{
            int lastItemIndex = svgElements.size() - 1;
            if(lastItemIndex >= 0) {
                svgElements.remove(lastItemIndex);
                svgElement = "";
            }
        }

        if(CanSendData()){
            btService.write(Constants.UNDO.getBytes());
        }

        invalidate();
    }

    public String GetSvgString(){
        StringBuilder sb = new StringBuilder();
        for (String element: svgElements) {
            sb.append(System.lineSeparator());
            sb.append(element);
        }
        return svgHeader + sb.toString() + svgElement + svgFoot;
    }

    public void Restart(){
        svgElements.clear();
        svgElement = "";
    }

    public void AddSvgElement(String line){
        svgElements.add(line);
    }

    public void SetSvgElements(List<String> svgElements){
        this.svgElements = svgElements;
        invalidate();
    }

    public List<String> GetSvgElements(){
        return svgElements;
    }

    private void DrawPolygonPoints(Canvas canvas){
        for (PointF point: polygonPoints) {
            canvas.drawCircle(point.x, point.y, 8, drawPaint);
        }
    }

    private void AddWidth(float value) {
        svgElement += " width=\"" + value + "\"";
    }

    private void AddHeight(float value) {
        svgElement += " height=\"" + value + "\"";
    }

    private void AddFill() {
        svgElement += " fill=\"" + SettingsHolder.getInstance().getSettings().getFill() + "\"";
    }

    private void AddFillOpacity() {
        svgElement += " fill-opacity=\"" + SettingsHolder.getInstance().getSettings().getFillOpacity() + "\"";
    }

    private void AddStrokeOpacity() {
        svgElement += " stroke-opacity=\"" + SettingsHolder.getInstance().getSettings().getStrokeOpacity() + "\"";
    }

    private void AddFill(String value) {
        svgElement += " fill=\"" + value + "\"";
    }

    private void AddEndTag(){
        svgElement += "/>";
    }

    private void AddStrokeWidth() {
        svgElement += " stroke-width=\"" + SettingsHolder.getInstance().getSettings().getStrokeWidth() + "\"";
    }

    private void AddStroke() {
        svgElement += " stroke=\"" + SettingsHolder.getInstance().getSettings().getStroke() + "\"";
    }

    private float GetDistance(){
        return (float)Math.sqrt(Math.pow(startX - endX, 2) + Math.pow(startY - endY, 2));
    }

    private boolean CanSendData(){
        return
                (btService != null) &&
                SettingsHolder.getInstance().getSettings().getIsTurnedOn() &&
                SettingsHolder.getInstance().getSettings().getSendData();
    }

    private void AddSvgElement(){
        svgElements.add(svgElement);
        if(CanSendData()){
            btService.write(svgElement.getBytes());
        }
    }
}
