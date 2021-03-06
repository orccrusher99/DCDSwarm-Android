package com.orctech.dcdswarm1.Models;

/**
 * Created by Justin Lee on 5/30/2017.
 */
public class Login {
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    private String username, password;
    
    public Login(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public boolean isEmpty() {
        if(username == null || password == null)
            return true;
        return (getPassword().equals("") && getUsername().equals(""));
    }
}