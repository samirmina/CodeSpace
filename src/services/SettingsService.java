package services;

import models.AppSettings;

import org.springframework.stereotype.Service;

/**
 * SettingsService - manages application settings with role-based access control.
 * Provides validation and permission checks for each setting key.
 */
@Service
public class SettingsService {
    private AppSettings settings;

    public SettingsService() {
        this.settings = new AppSettings();
    }

    public SettingsService(AppSettings settings) {
        this.settings = settings != null ? settings : new AppSettings();
    }

    public AppSettings getSettings() { return settings; }

    public void setSettings(AppSettings settings) {
        this.settings = settings != null ? settings : new AppSettings();
    }

    public boolean canEdit(int settingNumber, models.Role role) {
        return role != null && role.canEditSetting(settingNumber);
    }

    public boolean canView(int settingNumber, models.Role role) {
        return role != null && role.canViewSetting(settingNumber);
    }

    public int getVisibleCountForRole(models.Role role) {
        if (role == null) return 2;
        switch (role) {
            case DEVELOPER: return 10;
            case MANAGER: return 6;
            case ADMIN: return 4;
            default: return 2;
        }
    }

    public boolean validate(int settingNumber, Object value) {
        switch (settingNumber) {
            case 1:
                return value instanceof AppSettings.Theme;
            case 2:
                if (value instanceof Integer) {
                    int v = (Integer) value;
                    return v >= 1 && v <= 180;
                }
                return false;
            case 3:
            case 4:
            case 6:
            case 7:
            case 9:
            case 10:
                return value instanceof Boolean;
            case 5:
                if (value instanceof Integer) {
                    int v = (Integer) value;
                    return v >= 5 && v <= 120;
                }
                return false;
            case 8:
                if (value instanceof Integer) {
                    int v = (Integer) value;
                    return v >= 30 && v <= 480;
                }
                return false;
            default:
                return false;
        }
    }
}
