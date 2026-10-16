package models;

/**
 * Role enum - defines user permission levels in the application.
 * Hierarchy: DEVELOPER > MANAGER > ADMIN > GUEST
 */
public enum Role {
    DEVELOPER, MANAGER, ADMIN;

    public boolean canEditSetting(int settingNumber) {
        switch (this) {
            case DEVELOPER:
                return settingNumber >= 1 && settingNumber <= 10;
            case MANAGER:
                return settingNumber >= 1 && settingNumber <= 6;
            case ADMIN:
                return settingNumber >= 1 && settingNumber <= 4;
            default:
                return settingNumber >= 1 && settingNumber <= 2;
        }
    }

    public boolean canViewSetting(int settingNumber) {
        return canEditSetting(settingNumber);
    }

    public boolean canAssignTask() {
        return this == Role.DEVELOPER || this == Role.MANAGER;
    }
}
