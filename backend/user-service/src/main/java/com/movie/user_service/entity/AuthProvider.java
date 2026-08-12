package com.movie.user_service.entity;


public enum AuthProvider {
    LOCAL,GOOGLE;

    public static AuthProvider getAuthProvider(String registrationId){
        if(registrationId==null || registrationId.trim().isEmpty()){
            return LOCAL;
        }
        try{
            return AuthProvider.valueOf(registrationId.toUpperCase());
        }catch(IllegalArgumentException ex){
            throw new IllegalArgumentException("Unsupported authentication provider");
        }
    }
}
