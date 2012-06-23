package de.christl.smsoip.activities.settings.preferences.model;

public class AccountModel implements Cloneable {
    private String userName;
    private String passWord;

    public AccountModel(String userName, String passWord) {
        this.userName = userName;
        this.passWord = passWord;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getUserName() {
        return userName;
    }

    public String getPass() {
        return passWord;
    }


}