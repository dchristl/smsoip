package de.christl.smsoip.activities.settings.preferences.model;

public class AccountModel {
    private final String userName;
    private final String passWord;

    public AccountModel(String userName, String passWord) {
        this.userName = userName;
        this.passWord = passWord;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassWord() {
        return passWord;
    }
}