package org.example;

import static org.junit.Assert.assertTrue;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.Test;
import org.xjx.common.utils.cipher.AESUtil;
import org.xjx.common.utils.cipher.RSAUtil;
import org.xjx.common.utils.redis.JedisUtil;

import java.util.Date;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test
    public void jwt() {
        // secretKey、signature
        String security = "xjx123456";
        // create token
        String token = Jwts.builder()
                .setSubject("1314520")
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, security)
                .compact();
        System.out.println("create token is " + token);

        // parse token
        Jwt jwt = Jwts.parser().setSigningKey(security).parse(token);
        System.out.println(jwt);

        String subject = ((DefaultClaims)jwt.getBody()).getSubject();
        System.out.println(subject);
    }

    @Test
    public void redisTest() {
        JedisUtil jedisUtil = new JedisUtil();
        jedisUtil.setString("test", "aaa");
    }

    @Test
    public void testRSA() {
        RSAUtil.KeyPairInfo keyPair = RSAUtil.getKeyPair();
        // Replace with your symmetric key
        String symmetricKey = "zhangjinbiao6666";

        String encryptSymmetric = RSAUtil.encryptPublicKey(symmetricKey, keyPair.getPublicKey());
        System.out.println("EncryptSymmetric："+encryptSymmetric);

        String decryptKey = RSAUtil.decryptPrivateKey(encryptSymmetric, keyPair.getPrivateKey());
        System.out.println(symmetricKey.equals(decryptKey));
    }
}
