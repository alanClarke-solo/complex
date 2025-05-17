package com.workflow.util;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class Receiver {

    private AtomicInteger counter = new AtomicInteger();

    public void receiveMessage(String message) {
        log.info("Received <" + message + ">");
        counter.incrementAndGet();
    }

    public int getCount() {
        return counter.get();
    }
}