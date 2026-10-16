package models;

/**
 * AppSettings - holds all configurable application settings with defaults.
 * Settings are role-gated: each role sees a subset of keys.
 */
public class AppSettings {
    public enum Theme { DARK, LIGHT, AUTO }

    private Theme theme;
    private int defaultPomodoroDuration;
    private boolean notificationSound;
    private boolean autoStartNextSession;
    private int autoSaveInterval;
    private boolean showAllTasks;
    private boolean teamReportAccess;
    private int dailyFocusGoal;
    private boolean userManagement;
    private boolean roleManagement;

    public AppSettings() {
        this.theme = Theme.DARK;
        this.defaultPomodoroDuration = 25;
        this.notificationSound = true;
        this.autoStartNextSession = false;
        this.autoSaveInterval = 30;
        this.showAllTasks = false;
        this.teamReportAccess = false;
        this.dailyFocusGoal = 120;
        this.userManagement = true;
        this.roleManagement = true;
    }

    public Theme getTheme() { return theme; }
    public void setTheme(Theme theme) { this.theme = theme; }
    public int getDefaultPomodoroDuration() { return defaultPomodoroDuration; }
    public void setDefaultPomodoroDuration(int defaultPomodoroDuration) { this.defaultPomodoroDuration = defaultPomodoroDuration; }
    public boolean isNotificationSound() { return notificationSound; }
    public void setNotificationSound(boolean notificationSound) { this.notificationSound = notificationSound; }
    public boolean isAutoStartNextSession() { return autoStartNextSession; }
    public void setAutoStartNextSession(boolean autoStartNextSession) { this.autoStartNextSession = autoStartNextSession; }
    public int getAutoSaveInterval() { return autoSaveInterval; }
    public void setAutoSaveInterval(int autoSaveInterval) { this.autoSaveInterval = autoSaveInterval; }
    public boolean isShowAllTasks() { return showAllTasks; }
    public void setShowAllTasks(boolean showAllTasks) { this.showAllTasks = showAllTasks; }
    public boolean isTeamReportAccess() { return teamReportAccess; }
    public void setTeamReportAccess(boolean teamReportAccess) { this.teamReportAccess = teamReportAccess; }
    public int getDailyFocusGoal() { return dailyFocusGoal; }
    public void setDailyFocusGoal(int dailyFocusGoal) { this.dailyFocusGoal = dailyFocusGoal; }
    public boolean isUserManagement() { return userManagement; }
    public void setUserManagement(boolean userManagement) { this.userManagement = userManagement; }
    public boolean isRoleManagement() { return roleManagement; }
    public void setRoleManagement(boolean roleManagement) { this.roleManagement = roleManagement; }
}
