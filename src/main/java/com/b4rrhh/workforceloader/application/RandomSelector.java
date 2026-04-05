package com.b4rrhh.workforceloader.application;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomSelector {

    private RandomSelector() {
    }

    public static <T> T pickRandom(List<T> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Cannot pick random element from empty list");
        }
        int index = ThreadLocalRandom.current().nextInt(values.size());
        return values.get(index);
    }
}
