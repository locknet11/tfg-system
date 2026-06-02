package com.spulido.tfg;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires MongoDB running on localhost:27017")
class WsApplicationTests {

    @Test
    void contextLoads() {
    }
}
