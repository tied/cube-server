package com.mesilat.cube;

import java.util.UUID;
import net.java.ao.EntityManager;
import net.java.ao.ValueGenerator;

public class UUIDValueGenerator implements ValueGenerator<String> {

    @Override
    public String generateValue(EntityManager em) {
        return UUID.randomUUID().toString();
    }
}