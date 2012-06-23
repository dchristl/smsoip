package de.christl.smsoip.activities.settings.preferences.model;

public class AccountModel {
    private int index;
    private final String userName;
    private final String passWord;

    public AccountModel(int i, String userName, String passWord) {
        index = i;
        this.userName = userName;
        this.passWord = passWord;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public int getIndex() {
        return index;
    }
}