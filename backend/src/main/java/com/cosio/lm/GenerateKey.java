package com.cosio.lm;

import java.util.Base64;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;

public class GenerateKey {
    public static void main(String[] args) {
        SecretKey key = Jwts.SIG.HS256.key().build();
        System.out.println("Here it is: " + Base64.getEncoder().encodeToString(key.getEncoded()));
    }
}