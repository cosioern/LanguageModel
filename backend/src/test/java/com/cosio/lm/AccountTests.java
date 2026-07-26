package com.cosio.lm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class AccountTests {

    @Test
    void userConstructorTest() {
        User user = new User("Bernie", "fake@email.com", "null", "Ernesto", LocalDate.now());
        assertNotEquals(null, user);
        assertEquals("Ernesto", user.getName());
        assertEquals("Bernie", user.getUsername());

        assertNotEquals(null, user.getID());
        assertEquals(false, user.isVerified());
        user.setVerified();
        assertEquals(true, user.isVerified());

        assertNotEquals(null, user.getVerificationToken());

        Guest guest = new Guest();
        assertNotEquals(null, guest);
    }
 


}
