package majapp.bluetoothpaint;

public class SettingsHolder {
    private static SettingsHolder dataObject = null;
    private Settings settings;

    private SettingsHolder() {
        // left blank intentionally
    }

    public static SettingsHolder getInstance() {
        if (dataObject == null)
            dataObject = new SettingsHolder();
        return dataObject;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}
