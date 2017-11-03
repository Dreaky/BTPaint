package majapp.bluetoothpaint;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    private float strokeWidth;
    private String stroke;
    private String fill;
    private String strokeWithOpacity;
    private String fillWithOpacity;
    private ShapesEnum shape;
    private float strokeOpacity;
    private float fillOpacity;
    private List<String> svgElements;

    public Settings() {
        strokeWidth = 1.0f;
        stroke = "#FF0000";
        fill = "#00FF00";
        strokeWithOpacity = "#FFFF0000";
        fillWithOpacity = "#FF00FF00";
        shape = ShapesEnum.PATH;
        strokeOpacity = 1.0f;
        fillOpacity= 1.0f;
        svgElements = new ArrayList<String>();
    }

    public void setStrokeWidth(float value){strokeWidth = value;}
    public float getStrokeWidth(){return strokeWidth;}

    public void setStroke(String value){stroke = value;}
    public String getStroke(){return stroke;}

    public void setFill(String value){fill = value;}
    public String getFill(){return fill;}

    public void setStrokeWithOpacity(String value){strokeWithOpacity = value;}
    public String getStrokeWithOpacity(){return strokeWithOpacity;}

    public void setFillWithOpacity(String value){fillWithOpacity = value;}
    public String getFillWithOpacity(){return fillWithOpacity;}

    public void setShape(ShapesEnum value){shape = value;}
    public ShapesEnum getShape(){return shape;}

    public void setStrokeOpacity(float value){strokeOpacity = value;}
    public float getStrokeOpacity(){return strokeOpacity;}

    public void setFillOpacity(float value){fillOpacity = value;}
    public float getFillOpacity(){return fillOpacity;}

    public void setSvgElements(List<String> value){svgElements = value;}
    public List<String> getSvgElements(){return svgElements;}
}
